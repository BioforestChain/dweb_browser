import { once } from "lodash";
import { $Binary, binaryToU8a } from "../../helper/binaryHelper.ts";
import { IpcMessage, IPC_MESSAGE_TYPE } from "./const.ts";
import type { Ipc } from "./ipc.ts";
import type { IpcBody } from "./IpcBody.ts";
import { IpcBodySender, setStreamId } from "./IpcBodySender.ts";
import { IpcHeaders } from "./IpcHeaders.ts";
import type { MetaBody } from "./MetaBody.ts";

export class IpcResponse extends IpcMessage<IPC_MESSAGE_TYPE.RESPONSE> {
  constructor(
    readonly req_id: number,
    readonly statusCode: number,
    readonly headers: IpcHeaders,
    readonly body: IpcBody,
    readonly ipc: Ipc
  ) {
    super(IPC_MESSAGE_TYPE.RESPONSE);
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
  static async fromResponse(
    req_id: number,
    response: Response,
    ipc: Ipc,
    /// 如果有 content-length，说明大小是明确的，不要走流，直接传输就好，减少 IPC 的触发次数. TODO 需要注意大小是否过大，过大的话还是要分片传输。不过这种大二进制情况下一般是请求文件，应该直接使用句柄转发
    asBinary = false // response.headers.get("content-length") !== null
  ) {
    if (response.bodyUsed) {
      throw new Error("body used");
    }
    let ipcBody: IpcBody;
    if (asBinary || response.body == undefined) {
      ipcBody = IpcBodySender.from(
        binaryToU8a(await response.arrayBuffer()),
        ipc
      );
    } else {
      setStreamId(response.body, response.url);
      ipcBody = IpcBodySender.from(response.body, ipc);
    }

    return new IpcResponse(
      req_id,
      response.status,
      new IpcHeaders(response.headers),
      ipcBody,
      ipc
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
      IpcBodySender.from(text, ipc),
      ipc
    );
  }
  static fromBinary(
    req_id: number,
    statusCode: number,
    headers = new IpcHeaders(),
    binary: $Binary,
    ipc: Ipc
  ) {
    headers.init("Content-Type", "application/octet-stream");
    headers.init("Content-Length", binary.byteLength + "");
    return new IpcResponse(
      req_id,
      statusCode,
      headers,
      IpcBodySender.from(binaryToU8a(binary), ipc),
      ipc
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
      IpcBodySender.from(stream, ipc),
      ipc
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

export class IpcResMessage extends IpcMessage<IPC_MESSAGE_TYPE.RESPONSE> {
  constructor(
    readonly req_id: number,
    readonly statusCode: number,
    readonly headers: Record<string, string>,
    readonly metaBody: MetaBody
  ) {
    super(IPC_MESSAGE_TYPE.RESPONSE);
  }
}
