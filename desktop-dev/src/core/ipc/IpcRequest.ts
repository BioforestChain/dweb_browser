import { IpcEvent } from "@dweb-browser/desktop/core/ipc/IpcEvent.ts";
import { PromiseOut } from "@dweb-browser/desktop/helper/PromiseOut.ts";
import { once } from "../../helper/$once.ts";
import { $Binary, binaryToU8a, isBinary } from "../../helper/binaryHelper.ts";
import { CacheGetter } from "../../helper/cacheGetter.ts";
import { ReadableStreamOut, streamRead } from "../../helper/stream/readableStreamHelper.ts";
import { parseUrl } from "../../helper/urlHelper.ts";
import { buildRequestX } from "../helper/ipcRequestHelper.ts";
import type { IpcBody } from "./IpcBody.ts";
import { IpcBodySender } from "./IpcBodySender.ts";
import { IpcHeaders } from "./IpcHeaders.ts";
import type { MetaBody } from "./MetaBody.ts";
import { $PureFrame, PureChannel, ipcEventToPureFrame, pureFrameToIpcEvent } from "./PureChannel.ts";
import { IPC_MESSAGE_TYPE, IPC_METHOD, IpcMessage, toIpcMethod } from "./const.ts";
import type { Ipc } from "./ipc.ts";

const PURE_CHANNEL_EVENT_PREFIX = "§";
const X_IPC_UPGRADE_KEY = "X-Dweb-Ipc-Upgrade-Key";

export class IpcRequest extends IpcMessage<IPC_MESSAGE_TYPE.REQUEST> {
  constructor(
    readonly req_id: number,
    readonly url: string,
    readonly method: IPC_METHOD,
    readonly headers: IpcHeaders,
    readonly body: IpcBody,
    readonly ipc: Ipc
  ) {
    super(IPC_MESSAGE_TYPE.REQUEST);
    if (body instanceof IpcBodySender) {
      IpcBodySender.$usableByIpc(ipc, body);
    }
  }

  private _parsed_url?: URL;
  get parsed_url() {
    return (this._parsed_url ??= parseUrl(this.url));
  }

  static fromText(
    req_id: number,
    url: string,
    method: IPC_METHOD = IPC_METHOD.GET,
    headers = new IpcHeaders(),
    text: string,
    ipc: Ipc
  ) {
    // 这里 content-length 默认不写，因为这是要算二进制的长度，我们这里只有在字符串的长度，不是一个东西
    return new IpcRequest(req_id, url, method, headers, IpcBodySender.fromText(text, ipc), ipc);
  }
  static fromBinary(
    req_id: number,
    url: string,
    method: IPC_METHOD = IPC_METHOD.GET,
    headers = new IpcHeaders(),
    binary: $Binary,
    ipc: Ipc
  ) {
    headers.init("Content-Type", "application/octet-stream");
    headers.init("Content-Length", binary.byteLength + "");

    return new IpcRequest(req_id, url, method, headers, IpcBodySender.fromBinary(binaryToU8a(binary), ipc), ipc);
  }
  // 如果需要发送stream数据 一定要使用这个方法才可以传递数据否则数据无法传递
  static fromStream(
    req_id: number,
    url: string,
    method: IPC_METHOD = IPC_METHOD.GET,
    headers = new IpcHeaders(),
    stream: ReadableStream<Uint8Array>,
    ipc: Ipc
  ) {
    headers.init("Content-Type", "application/octet-stream");

    return new IpcRequest(req_id, url, method, headers, IpcBodySender.fromStream(stream, ipc), ipc);
  }

  static fromRequest(
    req_id: number,
    ipc: Ipc,
    url: string,
    init: {
      method?: string;
      body?: /* json+text */
      | null
        | string
        /* base64 */
        | Uint8Array
        /* stream+base64 */
        | Blob
        | ReadableStream<Uint8Array>;
      headers?: IpcHeaders | HeadersInit;
    } = {}
  ) {
    const method = toIpcMethod(init.method);
    const headers = init.headers instanceof IpcHeaders ? init.headers : new IpcHeaders(init.headers);

    let ipcBody: IpcBody;
    if (isBinary(init.body)) {
      ipcBody = IpcBodySender.fromBinary(init.body, ipc);
    } else if (init.body instanceof ReadableStream) {
      ipcBody = IpcBodySender.fromStream(init.body, ipc);
    } else if (init.body instanceof Blob) {
      ipcBody = IpcBodySender.fromStream(init.body.stream(), ipc);
    } else {
      ipcBody = IpcBodySender.fromText(init.body ?? "", ipc);
    }

    return new IpcRequest(req_id, url, method, headers, ipcBody, ipc);
  }

  /**
   * 判断是否是双工协议
   *
   * 比如目前双工协议可以由 WebSocket 来提供支持
   */
  get hasDuplex() {
    return this.duplexEventBaseName.value !== undefined;
  }
  private duplexEventBaseName = new CacheGetter(() => {
    let eventNameBase: string | undefined;
    const upgrade_key = this.headers.get(X_IPC_UPGRADE_KEY);
    if (upgrade_key?.startsWith(PURE_CHANNEL_EVENT_PREFIX)) {
      eventNameBase = upgrade_key;
    }
    return eventNameBase;
  });

  private channel = new CacheGetter(async () => {
    const channelId = this.duplexEventBaseName.value!;
    const eventStart = `${channelId}/start`;
    const eventData = `${channelId}/data`;
    const eventClose = `${channelId}/close`;
    const income = new ReadableStreamOut<$PureFrame>();
    const outgoing = new ReadableStreamOut<$PureFrame>();
    const channel = new PureChannel(income, outgoing);
    const started = new PromiseOut<IpcEvent>();

    this.ipc.onEvent((ipcEvent, ipc) => {
      switch (ipcEvent.name) {
        case eventStart:
          started.resolve(ipcEvent);
          break;
        case eventData:
          income.controller.enqueue(ipcEventToPureFrame(ipcEvent));
          break;
        case eventClose:
          channel.close();
          break;
      }
    });
    this.ipc.postMessage(IpcEvent.fromText(eventStart, ""));
    this.ipc.postMessage(await started.promise);

    void (async () => {
      for await (const pureFrame of streamRead(income.stream)) {
        this.ipc.postMessage(pureFrameToIpcEvent(eventData, pureFrame));
      }
      // 关闭的时候，发一个信号给对面
      const ipcCloseEvent = IpcEvent.fromText(eventClose, "");
      this.ipc.postMessage(ipcCloseEvent);
    })();

    return channel;
  });
  getChannel() {
    return this.channel.value;
  }

  toRequest() {
    return buildRequestX(this.url, { method: this.method, headers: this.headers, body: this.body.raw });
  }

  readonly ipcReqMessage = once(
    () => new IpcReqMessage(this.req_id, this.method, this.url, this.headers.toJSON(), this.body.metaBody)
  );

  toJSON() {
    const { method } = this;
    // let body: undefined | $BodyData;
    if ((method === IPC_METHOD.GET || method === IPC_METHOD.HEAD) === false) {
      // body = this.body.raw;
      return new IpcReqMessage(this.req_id, this.method, this.url, this.headers.toJSON(), this.body.metaBody);
    }
    return this.ipcReqMessage();
  }
}

export class IpcReqMessage extends IpcMessage<IPC_MESSAGE_TYPE.REQUEST> {
  constructor(
    readonly req_id: number,
    readonly method: IPC_METHOD,
    readonly url: string,
    readonly headers: Record<string, string>,
    readonly metaBody: MetaBody
  ) {
    super(IPC_MESSAGE_TYPE.REQUEST);
  }
}
