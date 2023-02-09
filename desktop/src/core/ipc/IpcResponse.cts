import { $Binary, binaryToU8a } from "../../helper/binaryHelper.cjs";
import { simpleDecoder } from "../../helper/encoding.cjs";
import { $streamAsRawData } from "./$streamAsRawData.cjs";
import { IPC_DATA_TYPE, IPC_RAW_BODY_TYPE, type $RawData } from "./const.cjs";
import type { Ipc } from "./ipc.cjs";
import { IpcBody } from "./IpcBody.cjs";
import { IpcHeaders } from "./IpcHeaders.cjs";

export class IpcResponse extends IpcBody {
  readonly type = IPC_DATA_TYPE.RESPONSE;
  constructor(
    readonly req_id: number,
    readonly statusCode: number,
    rawBody: $RawData,
    readonly headers: Record<string, string>,
    ipc: Ipc
  ) {
    super(rawBody, ipc);
  }

  asResponse(url?: string) {
    const body = this.body;
    if (body instanceof Uint8Array) {
      this.headers["content-length"] ??= body.length + "";
    }
    const response = new Response(this.body, {
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
    let ipcResponse: IpcResponse;

    if (
      response.body /* &&
      /// 如果有 content-length，说明大小是明确的，不要走流，直接传输就好，减少 IPC 的触发次数
      response.headers.get("content-length") === null */
    ) {
      ipcResponse = this.fromStream(
        req_id,
        response.status,
        response.body,
        new IpcHeaders(response.headers),
        ipc
      );
    } else {
      ipcResponse = this.fromBinary(
        req_id,
        response.status,
        await response.arrayBuffer(),
        new IpcHeaders(response.headers),
        ipc
      );
    }

    return ipcResponse;
  }
  static fromJson(
    req_id: number,
    statusCode: number,
    jsonable: unknown,
    headers = new IpcHeaders()
  ) {
    headers.init("Content-Type", "application/json");
    return this.fromText(req_id, statusCode, JSON.stringify(jsonable), headers);
  }
  static fromText(
    req_id: number,
    statusCode: number,
    text: string,
    headers = new IpcHeaders()
  ) {
    headers.init("Content-Type", "text/plain");
    // 这里 content-length 默认不写，因为这是要算二进制的长度，我们这里只有在字符串的长度，不是一个东西
    return new IpcResponse(
      req_id,
      statusCode,
      [IPC_RAW_BODY_TYPE.TEXT, text],
      headers.toJSON(),
      void 0 as never
    );
  }
  static fromBinary(
    req_id: number,
    statusCode: number,
    binary: $Binary,
    headers: IpcHeaders,
    ipc: Ipc
  ) {
    headers.init("Content-Type", "application/octet-stream");
    headers.init("Content-Length", binary.byteLength + "");
    return new IpcResponse(
      req_id,
      statusCode,
      ipc.support_message_pack
        ? [IPC_RAW_BODY_TYPE.BINARY, binaryToU8a(binary)]
        : [IPC_RAW_BODY_TYPE.BASE64, simpleDecoder(binary, "base64")],
      headers.toJSON(),
      void 0 as never
    );
  }
  static fromStream(
    req_id: number,
    statusCode: number,
    stream: ReadableStream<Uint8Array>,
    headers = new IpcHeaders(),
    ipc: Ipc
  ) {
    headers.init("Content-Type", "application/octet-stream");
    // headers["transfer-encoding"] ??= ipc.support_message_pack
    //   ? "base64"
    //   : "binary";
    const stream_id = `res/${req_id}/${headers.get("Content-Length") ?? "-"}`;
    const ipcResponse = new IpcResponse(
      req_id,
      statusCode,
      ipc.support_message_pack
        ? [IPC_RAW_BODY_TYPE.BINARY_STREAM_ID, stream_id]
        : [IPC_RAW_BODY_TYPE.BASE64_STREAM_ID, stream_id],
      headers.toJSON(),
      ipc
    );
    $streamAsRawData(stream_id, stream, ipc);
    return ipcResponse;
  }
  // static fromBinaryStream(
  //   req_id: number,
  //   statusCode: number,
  //   binary: Uint8Array | ReadableStream<Uint8Array>,
  //   headers=new IpcHeaders(),
  //   ipc: Ipc
  // ) {}
}
