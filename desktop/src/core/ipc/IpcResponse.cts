import once from "lodash/once";
import { $Binary, binaryToU8a } from "../../helper/binaryHelper.cjs";
import { $MetaBody, IpcMessage, IPC_DATA_TYPE } from "./const.cjs";
import type { Ipc } from "./ipc.cjs";
import type { IpcBody } from "./IpcBody.cjs";
import { IpcBodySender } from "./IpcBodySender.cjs";
import { IpcHeaders } from "./IpcHeaders.cjs";

export class IpcResponse extends IpcMessage<IPC_DATA_TYPE.RESPONSE> {
  constructor(
    readonly req_id: number,
    readonly statusCode: number,
    readonly headers: IpcHeaders,
    readonly body: IpcBody
  ) {
    super(IPC_DATA_TYPE.RESPONSE);
  }

  #ipcHeaders?: IpcHeaders;
  get ipcHeaders() {
    return (this.#ipcHeaders ??= new IpcHeaders(this.headers));
  }

  toResponse(url?: string) {
    const body = this.body.body;
    if (body instanceof Uint8Array) {
      this.headers.init("Content-Length", body.length + "");
    }
    const response = new Response(body, {
      headers: this.headers,
      status: this.statusCode,
    });
    if (url) {
      Object.defineProperty(response, "url", {
        value: url,
        enumerable: true,
        configurable: true,
        writable: false,
      });
    }
    return response;
  }

  /** 将 response 对象进行转码变成 ipcResponse */
  static async fromResponse(req_id: number, response: Response, ipc: Ipc) {
    let ipcBody: IpcBody;
    if (
      response.body /* &&
      /// 如果有 content-length，说明大小是明确的，不要走流，直接传输就好，减少 IPC 的触发次数
      response.headers.get("content-length") === null */
    ) {
      ipcBody = new IpcBodySender(response.body, ipc);
    } else {
      ipcBody = new IpcBodySender(
        binaryToU8a(await response.arrayBuffer()),
        ipc
      );
    }

    return new IpcResponse(
      req_id,
      response.status,
      new IpcHeaders(response.headers),
      ipcBody
    );
  }
  static fromJson(
    req_id: number,
    statusCode: number,
    headers = new IpcHeaders(),
    jsonable: unknown,
    ipc: Ipc
  ) {
    headers.init("Content-Type", "application/json");
    return this.fromText(
      req_id,
      statusCode,
      headers,
      JSON.stringify(jsonable),
      ipc
    );
  }
  static fromText(
    req_id: number,
    statusCode: number,
    headers = new IpcHeaders(),
    text: string,
    ipc: Ipc
  ) {
    headers.init("Content-Type", "text/plain");
    // 这里 content-length 默认不写，因为这是要算二进制的长度，我们这里只有在字符串的长度，不是一个东西
    return new IpcResponse(
      req_id,
      statusCode,
      headers,
      new IpcBodySender(text, ipc)
    );
  }
  static fromBinary(
    req_id: number,
    statusCode: number,
    headers: IpcHeaders,
    binary: $Binary,
    ipc: Ipc
  ) {
    headers.init("Content-Type", "application/octet-stream");
    headers.init("Content-Length", binary.byteLength + "");
    return new IpcResponse(
      req_id,
      statusCode,
      headers,
      new IpcBodySender(binaryToU8a(binary), ipc)
    );
  }
  static fromStream(
    req_id: number,
    statusCode: number,
    headers = new IpcHeaders(),
    stream: ReadableStream<Uint8Array>,
    ipc: Ipc
  ) {
    headers.init("Content-Type", "application/octet-stream");
    const ipcResponse = new IpcResponse(
      req_id,
      statusCode,
      headers,
      new IpcBodySender(stream, ipc)
    );
    return ipcResponse;
  }

  readonly ipcResMessage = once(
    () =>
      new IpcResMessage(
        this.req_id,
        this.statusCode,
        this.headers.toJSON(),
        this.body.metaBody
      )
  );
  toJSON() {
    return this.ipcResMessage();
  }
}

export class IpcResMessage extends IpcMessage<IPC_DATA_TYPE.RESPONSE> {
  constructor(
    readonly req_id: number,
    readonly statusCode: number,
    readonly headers: Record<string, string>,
    readonly metaBody: $MetaBody
  ) {
    super(IPC_DATA_TYPE.RESPONSE);
  }
}
