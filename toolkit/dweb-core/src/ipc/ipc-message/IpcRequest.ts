import { CacheGetter } from "@dweb-browser/helper/cacheGetter.ts";
import { binaryToU8a, isBinary, type $Binary } from "@dweb-browser/helper/fun/binaryHelper.ts";
import { parseUrl } from "@dweb-browser/helper/fun/urlHelper.ts";
import { IpcHeaders } from "../helper/IpcHeaders.ts";
import { PURE_METHOD, toPureMethod } from "../helper/PureMethod.ts";
import { httpMethodCanOwnBody } from "../helper/httpHelper.ts";
import type { $IpcRequestInit, Ipc } from "../ipc.ts";
import { X_IPC_UPGRADE_KEY, getIpcChannel } from "./channel/IpcChannel.ts";
import { PURE_CHANNEL_EVENT_PREFIX, PureChannel, pureChannelToIpcEvent } from "./channel/PureChannel.ts";
import { IPC_MESSAGE_TYPE, ipcMessageBase } from "./internal/IpcMessage.ts";
import type { IpcBody } from "./stream/IpcBody.ts";
import { IpcBodySender } from "./stream/IpcBodySender.ts";
import type { MetaBody } from "./stream/MetaBody.ts";

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

  toSerializable() {
    return IpcRawRequest(this.reqId, this.method, this.url, this.headers.toJSON(), this.body.metaBody);
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
    return new IpcClientRequest(reqId, url, method, headers, IpcBodySender.fromText(text, ipc), ipc);
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

    return new IpcClientRequest(reqId, url, method, headers, IpcBodySender.fromBinary(binaryToU8a(binary), ipc), ipc);
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

    return new IpcClientRequest(reqId, url, method, headers, IpcBodySender.fromStream(stream, ipc), ipc);
  }

  static async fromRequest(reqId: number, ipc: Ipc, url: string, init: $IpcRequestInit = {}) {
    const method = toPureMethod(init.method);
    const headers = init.headers instanceof IpcHeaders ? init.headers : new IpcHeaders(init.headers);

    let ipcBody: IpcBody;
    if (isBinary(init.body)) {
      let u8aBody: Uint8Array;
      if (init.body instanceof Uint8Array) {
        u8aBody = init.body;
      } else if (init.body instanceof ArrayBuffer) {
        u8aBody = new Uint8Array(init.body);
      } else {
        u8aBody = new Uint8Array(init.body.buffer, init.body.byteOffset, init.body.byteLength);
      }
      ipcBody = IpcBodySender.fromBinary(u8aBody, ipc);
    } else if (init.body instanceof ReadableStream) {
      ipcBody = IpcBodySender.fromStream(init.body, ipc);
    } else if (init.body instanceof Blob) {
      ipcBody = IpcBodySender.fromStream(init.body.stream(), ipc);
    } else {
      ipcBody = IpcBodySender.fromText(init.body ?? "", ipc);
    }

    const clientIpcRequest = new IpcClientRequest(reqId, url, method, headers, ipcBody, ipc);

    /// 如果外部 定义了 clientPureChannel，说明外部不是使用 channelIpc 来发送数据的，
    // 而是使用 pureChannel 来发送数据，于是为他们进行手动的绑定。
    const clientPureChannel =
      init.headers instanceof Headers ? client_headers_pure_channel_wm.get(init.headers) : undefined;
    if (clientPureChannel !== undefined) {
      /// 强制初始化 channelIpc
      await ipc.prepareChannel(headers);
      /// 强制初始化 channelIpc 与 clientPureChannel 的关联
      clientIpcRequest.pureChannel = clientPureChannel;
      /// 强制初始化channel
      void clientIpcRequest.getChannel();
    }

    return clientIpcRequest;
  }

  private pureChannel?: PureChannel;

  protected channel = new CacheGetter(() => {
    const channelIpc = getIpcChannel(this.headers);
    if (channelIpc === undefined) {
      throw new Error("no channel");
    }
    const channel = (this.pureChannel ??= new PureChannel());
    void (async () => {
      const forkedIpc = await channelIpc;
      await pureChannelToIpcEvent(forkedIpc, channel, "IpcClientRequest");
    })();

    return channel;
  });
}

export class IpcServerRequest extends IpcRequest {
  constructor(readonly client: IpcClientRequest, ipc: Ipc) {
    super(client.reqId, client.url, client.method, client.headers, client.body, ipc);
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

  toPureClientRequest() {
    const url = this.url;
    const method = this.method;
    const body = httpMethodCanOwnBody(method) ? this.body.raw : undefined;
    const request_init: RequestInit = {
      method,
      headers: new Headers(this.headers), // 复制一份全新的
      body,
    };
    if (body instanceof ReadableStream) {
      Reflect.set(request_init, "duplex", "half");
    }
    const request = new PureRequest(url, request_init);

    if (this.hasDuplex) {
      const serverChannel = this.getChannel();
      // 拿到channel转换输入输出,意味着输出变成输入，那么也就自然的继续进行转发的流程
      const clientChannel = serverChannel.reverse();
      client_headers_pure_channel_wm.set(request.headers, clientChannel);
    }
    return request;
  }
}

export class PureRequest extends Request implements Body {
  stream?: BodyInit | null;
  constructor(input: URL | RequestInfo, init?: RequestInit | undefined) {
    super(input, init);
    this.stream = init?.body;
  }

  override get body() {
    if (this.stream instanceof ReadableStream) return this.stream;
    return super.body;
  }

  override get bodyUsed() {
    return super.bodyUsed;
  }

  override async arrayBuffer(): Promise<ArrayBuffer> {
    if (!this.stream) {
      return new ArrayBuffer(0);
    }

    return await new Response(this.stream).arrayBuffer();
  }

  override async blob(): Promise<Blob> {
    if (!this.stream) {
      return new Blob();
    }

    return await new Response(this.stream).blob();
  }

  override async formData(): Promise<FormData> {
    if (!this.stream) {
      return new FormData();
    }

    return await new Response(this.stream).formData();
  }

  // deno-lint-ignore no-explicit-any
  override async json(): Promise<any> {
    if (!this.stream) {
      return null;
    }
    return await new Response(this.stream).json();
  }

  // deno-lint-ignore require-await
  override async text(): Promise<string> {
    if (!this.stream) {
      return "";
    }
    return new Response(this.stream).text();
  }
}

const client_headers_pure_channel_wm = new WeakMap<Headers, PureChannel>();
