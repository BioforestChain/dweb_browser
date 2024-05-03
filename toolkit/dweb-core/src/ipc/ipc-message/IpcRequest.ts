import { CacheGetter } from "@dweb-browser/helper/cacheGetter.ts";
import {
  binaryToU8a,
  isBinary,
  type $Binary,
} from "@dweb-browser/helper/fun/binaryHelper.ts";
import { buildRequestX } from "../helper/ipcRequestHelper.ts";
import { parseUrl } from "@dweb-browser/helper/fun/urlHelper.ts";
import { IpcHeaders } from "../helper/IpcHeaders.ts";
import { PureChannel, pureChannelToIpcEvent } from "../helper/PureChannel.ts";
import { PURE_METHOD, toPureMethod } from "../helper/PureMethod.ts";
import type { $IpcRequestInit, Ipc } from "../ipc.ts";
import { IPC_MESSAGE_TYPE, ipcMessageBase } from "./internal/IpcMessage.ts";
import type { IpcBody } from "./stream/IpcBody.ts";
import { IpcBodySender } from "./stream/IpcBodySender.ts";
import type { MetaBody } from "./stream/MetaBody.ts";

export const PURE_CHANNEL_EVENT_PREFIX = "§-";
export const X_IPC_UPGRADE_KEY = "X-Dweb-Ipc-Upgrade-Key";

export type $IpcRawRequest = ReturnType<typeof IpcRawRequest>;
export const IpcRawRequest = (
  reqId: number,
  method: PURE_METHOD,
  url: string,
  headers: Record<string, string>,
  metaBody: MetaBody
) =>
  ({
    ...ipcMessageBase(IPC_MESSAGE_TYPE.REQUEST),
    reqId,
    method,
    url,
    headers,
    metaBody,
  } as const);

export abstract class IpcRequest {
  readonly type = IPC_MESSAGE_TYPE.REQUEST;
  constructor(
    readonly reqId: number,
    readonly url: string,
    readonly method: PURE_METHOD,
    readonly headers: IpcHeaders,
    readonly body: IpcBody,
    readonly ipc: Ipc
  ) {
    if (body instanceof IpcBodySender) {
      IpcBodySender.$usableByIpc(ipc, body);
    }
  }

  private _parsed_url?: URL;
  get parsed_url() {
    return (this._parsed_url ??= parseUrl(this.url));
  }

  /**
   * 判断是否是双工协议
   *
   * 比如目前双工协议可以由 WebSocket 来提供支持
   */
  get hasDuplex() {
    return this.duplexIpcId !== undefined;
  }
  private lazyDuplexIpcId = new CacheGetter(() => {
    const upgrade_key = this.headers.get(X_IPC_UPGRADE_KEY);
    if (upgrade_key?.startsWith(PURE_CHANNEL_EVENT_PREFIX)) {
      const forkedIpcId = +upgrade_key.slice(PURE_CHANNEL_EVENT_PREFIX.length);
      if (Number.isFinite(forkedIpcId)) {
        return forkedIpcId;
      }
    }
  });
  get duplexIpcId() {
    return this.lazyDuplexIpcId.value;
  }

  protected abstract channel: CacheGetter<PureChannel>;
  getChannel() {
    return this.channel.value;
  }

  toRequest() {
    return buildRequestX(this.url, {
      method: this.method,
      headers: this.headers,
      body: this.body.raw,
    });
  }

  toSerializable() {
    return IpcRawRequest(
      this.reqId,
      this.method,
      this.url,
      this.headers.toJSON(),
      this.body.metaBody
    );
  }

  toJSON() {
    return this.toSerializable();
  }
}

export class IpcClientRequest extends IpcRequest {
  private server?: IpcServerRequest;
  toServer(serverIpc: Ipc) {
    return (this.server ??= new IpcServerRequest(this, serverIpc));
  }

  static fromText(
    reqId: number,
    url: string,
    method: PURE_METHOD = PURE_METHOD.GET,
    headers = new IpcHeaders(),
    text: string,
    ipc: Ipc
  ) {
    // 这里 content-length 默认不写，因为这是要算二进制的长度，我们这里只有在字符串的长度，不是一个东西
    return new IpcClientRequest(
      reqId,
      url,
      method,
      headers,
      IpcBodySender.fromText(text, ipc),
      ipc
    );
  }
  static fromBinary(
    reqId: number,
    url: string,
    method: PURE_METHOD = PURE_METHOD.GET,
    headers = new IpcHeaders(),
    binary: $Binary,
    ipc: Ipc
  ) {
    headers.init("Content-Type", "application/octet-stream");
    headers.init("Content-Length", binary.byteLength + "");

    return new IpcClientRequest(
      reqId,
      url,
      method,
      headers,
      IpcBodySender.fromBinary(binaryToU8a(binary), ipc),
      ipc
    );
  }
  // 如果需要发送stream数据 一定要使用这个方法才可以传递数据否则数据无法传递
  static fromStream(
    reqId: number,
    url: string,
    method: PURE_METHOD = PURE_METHOD.GET,
    headers = new IpcHeaders(),
    stream: ReadableStream<Uint8Array>,
    ipc: Ipc
  ) {
    headers.init("Content-Type", "application/octet-stream");

    return new IpcClientRequest(
      reqId,
      url,
      method,
      headers,
      IpcBodySender.fromStream(stream, ipc),
      ipc
    );
  }

  static fromRequest(
    reqId: number,
    ipc: Ipc,
    url: string,
    init: $IpcRequestInit = {}
  ) {
    const method = toPureMethod(init.method);
    const headers =
      init.headers instanceof IpcHeaders
        ? init.headers
        : new IpcHeaders(init.headers);

    let ipcBody: IpcBody;
    if (isBinary(init.body)) {
      let u8aBody: Uint8Array;
      if (init.body instanceof Uint8Array) {
        u8aBody = init.body;
      } else if (init.body instanceof ArrayBuffer) {
        u8aBody = new Uint8Array(init.body);
      } else {
        u8aBody = new Uint8Array(
          init.body.buffer,
          init.body.byteOffset,
          init.body.byteLength
        );
      }
      ipcBody = IpcBodySender.fromBinary(u8aBody, ipc);
    } else if (init.body instanceof ReadableStream) {
      ipcBody = IpcBodySender.fromStream(init.body, ipc);
    } else if (init.body instanceof Blob) {
      ipcBody = IpcBodySender.fromStream(init.body.stream(), ipc);
    } else {
      ipcBody = IpcBodySender.fromText(init.body ?? "", ipc);
    }

    return new IpcClientRequest(reqId, url, method, headers, ipcBody, ipc);
  }

  protected channel = new CacheGetter(() => {
    const channelIpc = this._channelIpc;
    if (channelIpc === undefined) {
      throw new Error("no channel");
    }
    const channel = new PureChannel();
    void (async () => {
      const forkedIpc = await channelIpc;
      await pureChannelToIpcEvent(forkedIpc, channel, "IpcClientRequest");
    })();

    return channel;
  });

  private _channelIpc?: Promise<Ipc>;

  async enableChannel() {
    this._channelIpc ??= this._channelIpc = this.ipc.fork().then((ipc) => {
      this.headers.set(
        X_IPC_UPGRADE_KEY,
        `${PURE_CHANNEL_EVENT_PREFIX}${ipc.pid}`
      );
      return ipc;
    });
  }
}

export class IpcServerRequest extends IpcRequest {
  constructor(readonly client: IpcClientRequest, ipc: Ipc) {
    super(
      client.reqId,
      client.url,
      client.method,
      client.headers,
      client.body,
      ipc
    );
  }

  protected channel = new CacheGetter(() => {
    const pid = this.duplexIpcId!;
    const channel = new PureChannel();
    void (async () => {
      const forkedIpc = await this.ipc.waitForkedIpc(pid);
      await pureChannelToIpcEvent(forkedIpc, channel, "IpcServerRequest");
    })();
    return channel;
  });
}
