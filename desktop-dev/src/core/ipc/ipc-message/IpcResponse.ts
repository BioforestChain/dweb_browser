import { once } from "../../../helper/$once.ts";
import { binaryToU8a, type $Binary } from "../../../helper/binaryHelper.ts";
import { IpcHeaders } from "../helper/IpcHeaders.ts";
import type { Ipc } from "../ipc.ts";
import { IPC_MESSAGE_TYPE, ipcMessageBase } from "./internal/IpcMessage.ts";
import type { IpcBody } from "./stream/IpcBody.ts";
import { IpcBodySender, setStreamId } from "./stream/IpcBodySender.ts";
import type { MetaBody } from "./stream/MetaBody.ts";

export type $IpcResponse = ReturnType<typeof ipcResponse>;
export const ipcResponse = (reqId: number, statusCode: number, headers: Record<string, string>, metaBody: MetaBody) =>
  ({
    ...ipcMessageBase(IPC_MESSAGE_TYPE.RESPONSE),
    reqId,
    statusCode,
    headers,
    metaBody,
  } as const);

export class IpcResponse {
  readonly type = IPC_MESSAGE_TYPE.RESPONSE;
  constructor(
    readonly reqId: number,
    readonly statusCode: number,
    readonly headers: IpcHeaders,
    readonly body: IpcBody,
    readonly ipc: Ipc
  ) {
    if (body instanceof IpcBodySender) {
      IpcBodySender.$usableByIpc(ipc, body);
    }
  }

  #ipcHeaders?: IpcHeaders;
  get ipcHeaders() {
    return (this.#ipcHeaders ??= new IpcHeaders(this.headers));
  }

  toResponse(url?: string) {
    const body = this.body.raw;
    if (body instanceof Uint8Array) {
      this.headers.init("Content-Length", body.length + "");
    }
    let response: Response;
    if (this.statusCode < 200 || this.statusCode > 599) {
      response = new Response(body, {
        headers: this.headers,
        status: 200,
      });
      Object.defineProperty(response, "status", {
        value: this.statusCode,
        enumerable: true,
        configurable: true,
        writable: false,
      });
    } else {
      response = new Response(body, {
        headers: this.headers,
        status: this.statusCode,
      });
    }
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
  static async fromResponse(
    reqId: number,
    response: Response,
    ipc: Ipc,
    /// 如果有 content-length，说明大小是明确的，不要走流，直接传输就好，减少 IPC 的触发次数. TODO 需要注意大小是否过大，过大的话还是要分片传输。不过这种大二进制情况下一般是请求文件，应该直接使用句柄转发
    asBinary = false // response.headers.get("content-length") !== null
  ) {
    if (response.bodyUsed) {
      throw new Error("body used");
    }
    let ipcBody: IpcBody;
    if (
      asBinary ||
      response.body == undefined ||
      parseInt(response.headers.get("Content-Length") || "NaN") < 16 * 1024 * 1024
    ) {
      ipcBody = IpcBodySender.fromBinary(binaryToU8a(await response.arrayBuffer()), ipc);
    } else {
      setStreamId(response.body, response.url);
      ipcBody = IpcBodySender.fromStream(response.body, ipc);
    }

    const ipcHeaders = new IpcHeaders(response.headers);

    return new IpcResponse(reqId, response.status, ipcHeaders, ipcBody, ipc);
  }
  static fromJson(reqId: number, statusCode: number, headers = new IpcHeaders(), jsonable: unknown, ipc: Ipc) {
    headers.init("Content-Type", "application/json");
    return this.fromText(reqId, statusCode, headers, JSON.stringify(jsonable), ipc);
  }
  static fromText(reqId: number, statusCode: number, headers = new IpcHeaders(), text: string, ipc: Ipc) {
    headers.init("Content-Type", "text/plain");
    // 这里 content-length 默认不写，因为这是要算二进制的长度，我们这里只有在字符串的长度，不是一个东西
    return new IpcResponse(reqId, statusCode, headers, IpcBodySender.fromText(text, ipc), ipc);
  }
  static fromBinary(reqId: number, statusCode: number, headers = new IpcHeaders(), binary: $Binary, ipc: Ipc) {
    headers.init("Content-Type", "application/octet-stream");
    headers.set("Content-Length", binary.byteLength + "");
    return new IpcResponse(reqId, statusCode, headers, IpcBodySender.fromBinary(binaryToU8a(binary), ipc), ipc);
  }
  static fromStream(
    reqId: number,
    statusCode: number,
    headers = new IpcHeaders(),
    stream: ReadableStream<Uint8Array>,
    ipc: Ipc
  ) {
    headers.init("Content-Type", "application/octet-stream");
    const ipcResponse = new IpcResponse(reqId, statusCode, headers, IpcBodySender.fromStream(stream, ipc), ipc);
    return ipcResponse;
  }

  readonly ipcResMessage = once(() =>
    ipcResponse(this.reqId, this.statusCode, this.headers.toJSON(), this.body.metaBody)
  );
  toJSON() {
    return this.ipcResMessage();
  }
}
