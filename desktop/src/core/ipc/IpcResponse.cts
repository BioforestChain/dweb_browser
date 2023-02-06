import { binaryToU8a } from "../../helper/binaryHelper.cjs";
import { simpleDecoder } from "../../helper/encoding.cjs";
import { headersToRecord } from "../../helper/headersToRecord.cjs";
import { $streamAsRawData } from "./$streamAsRawData.cjs";
import { IPC_DATA_TYPE, IPC_RAW_BODY_TYPE, type $RawData } from "./const.cjs";
import type { Ipc } from "./ipc.cjs";
import { IpcBody } from "./IpcBody.cjs";

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

  asResponse() {
    const response = new Response(this.body, {
      headers: this.headers,
      status: this.statusCode,
    });
    return response;
  }

  /** 将 response 对象进行转码变成 ipcResponse */
  static async fromResponse(req_id: number, response: Response, ipc: Ipc) {
    let ipcResponse: IpcResponse;

    if (response.body) {
      ipcResponse = this.fromStream(
        req_id,
        response.status,
        response.body,
        headersToRecord(response.headers),
        ipc
      );
    } else {
      ipcResponse = this.fromBinary(
        req_id,
        response.status,
        await response.arrayBuffer(),
        headersToRecord(response.headers),
        ipc
      );
    }

    return ipcResponse;
  }
  static fromJson(
    req_id: number,
    statusCode: number,
    jsonable: unknown,
    headers: Record<string, string> = {}
  ) {
    headers["Content-Type"] ??= "application/json";
    return new IpcResponse(
      req_id,
      statusCode,
      [IPC_RAW_BODY_TYPE.TEXT, JSON.stringify(jsonable)],
      headers,
      void 0 as never
    );
  }
  static fromText(
    req_id: number,
    statusCode: number,
    text: string,
    headers: Record<string, string> = {}
  ) {
    headers["Content-Type"] ??= "text/plain";
    return new IpcResponse(
      req_id,
      statusCode,
      [IPC_RAW_BODY_TYPE.TEXT, text],
      headers,
      void 0 as never
    );
  }
  static fromBinary(
    req_id: number,
    statusCode: number,
    binary: ArrayBufferView | ArrayBuffer,
    headers: Record<string, string> = {},
    ipc: Ipc
  ) {
    headers["Content-Type"] ??= "application/octet-stream";
    return new IpcResponse(
      req_id,
      statusCode,
      ipc.support_message_pack
        ? [IPC_RAW_BODY_TYPE.BINARY, binaryToU8a(binary)]
        : [IPC_RAW_BODY_TYPE.BASE64, simpleDecoder(binary, "base64")],
      headers,
      void 0 as never
    );
  }
  static fromStream(
    req_id: number,
    statusCode: number,
    stream: ReadableStream<Uint8Array>,
    headers: Record<string, string> = {},
    ipc: Ipc
  ) {
    headers["Content-Type"] ??= "application/octet-stream";
    const contentLength = headers["Content-Length"] ?? 0;
    const stream_id = `res/${req_id}/${contentLength}`;
    const ipcResponse = new IpcResponse(
      req_id,
      statusCode,
      [IPC_RAW_BODY_TYPE.BASE64_STREAM_ID, stream_id],
      headersToRecord(headers),
      ipc
    );
    $streamAsRawData(stream_id, stream, ipc);
    return ipcResponse;
  }
  // static fromBinaryStream(
  //   req_id: number,
  //   statusCode: number,
  //   binary: Uint8Array | ReadableStream<Uint8Array>,
  //   headers: Record<string, string> = {},
  //   ipc: Ipc
  // ) {}
}
