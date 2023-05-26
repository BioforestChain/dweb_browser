import { once } from "lodash";
import { $Binary, binaryToU8a, isBinary } from "../../helper/binaryHelper.ts";
import { parseUrl } from "../../helper/urlHelper.ts";
import {
  IpcMessage,
  IPC_MESSAGE_TYPE,
  IPC_METHOD,
  toIpcMethod,
} from "./const.ts";
import type { Ipc } from "./ipc.ts";
import type { $BodyData, IpcBody } from "./IpcBody.ts";
import { IpcBodySender } from "./IpcBodySender.ts";
import { IpcHeaders } from "./IpcHeaders.ts";
import type { MetaBody } from "./MetaBody.ts";

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

  #parsed_url?: URL;
  get parsed_url() {
    return (this.#parsed_url ??= parseUrl(this.url));
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
    return new IpcRequest(
      req_id,
      url,
      method,
      headers,
      IpcBodySender.from(text, ipc),
      ipc
    );
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

    return new IpcRequest(
      req_id,
      url,
      method,
      headers,
      IpcBodySender.from(binaryToU8a(binary), ipc),
      ipc
    );
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

    return new IpcRequest(
      req_id,
      url,
      method,
      headers,
      IpcBodySender.from(stream, ipc),
      ipc
    );
  }

  static fromRequest(
    req_id: number,
    ipc: Ipc,
    url: string,
    init: {
      method?: string;
      body?: /* json+text */
      | string
        /* base64 */
        | Uint8Array
        /* stream+base64 */
        | ReadableStream<Uint8Array>;
      headers?: IpcHeaders | HeadersInit;
    } = {}
  ) {
    const method = toIpcMethod(init.method);
    const headers =
      init.headers instanceof IpcHeaders
        ? init.headers
        : new IpcHeaders(init.headers);

    let ipcBody: IpcBody;
    if (isBinary(init.body)) {
      ipcBody = IpcBodySender.from(init.body, ipc);
    } else if (init.body instanceof ReadableStream) {
      ipcBody = IpcBodySender.from(init.body, ipc);
    } else {
      ipcBody = IpcBodySender.from(init.body ?? "", ipc);
    }

    return new IpcRequest(req_id, url, method, headers, ipcBody, ipc);
  }

  toRequest() {
    const { method } = this;
    let body: undefined | $BodyData;
    if ((method === IPC_METHOD.GET || method === IPC_METHOD.HEAD) === false) {
      body = this.body.raw;
    }
    const init = {
      method,
      headers: this.headers,
      body,
    };
    if (body) {
      Reflect.set(init, "duplex", "half");
    }
    return new Request(this.url, init);
  }

  readonly ipcReqMessage = once(
    () =>
      new IpcReqMessage(
        this.req_id,
        this.method,
        this.url,
        this.headers.toJSON(),
        this.body.metaBody
      )
  );

  toJSON() {
    const { method } = this;
    let body: undefined | $BodyData;
    if ((method === IPC_METHOD.GET || method === IPC_METHOD.HEAD) === false) {
      body = this.body.raw;
      return new IpcReqMessage(
        this.req_id,
        this.method,
        this.url,
        this.headers.toJSON(),
        this.body.metaBody
      );
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
