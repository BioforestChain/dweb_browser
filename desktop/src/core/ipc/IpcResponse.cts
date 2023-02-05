import { simpleDecoder } from "../../helper/encoding.cjs";
import { headersToRecord } from "../../helper/headersToRecord.cjs";
import { $rawDataToBody } from "./$rawDataToBody.cjs";
import { $streamAsRawData } from "./$streamAsRawData.cjs";
import { IPC_DATA_TYPE, IPC_RAW_BODY_TYPE, type $RawData } from "./const.cjs";
import type { Ipc } from "./ipc.cjs";

export class IpcResponse {
  readonly type = IPC_DATA_TYPE.RESPONSE;
  constructor(
    readonly req_id: number,
    readonly statusCode: number,
    readonly rawBody: $RawData,
    readonly headers: Record<string, string>,
    ipc: Ipc
  ) {
    this.#ipc = ipc;
  }
  #ipc: Ipc;
  #body?: ReturnType<typeof $rawDataToBody>;
  get body() {
    return (this.#body ??= $rawDataToBody(this.rawBody, this.#ipc));
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
    const contentLength = response.headers.get("Content-Length") ?? 0;

    /// 6kb 为分水岭，超过 6kb 就改用流传输，或者如果原本就是流对象
    /// TODO 这里是否使用流模式，应该引入更加只能的判断方式，比如先读取一部分数据，如果能读取完成，那么就直接吧 end 符号一起带过去
    if (contentLength > 6144 || response.body) {
      const stream_id = `res/${req_id}/${contentLength}`;
      ipcResponse = new IpcResponse(
        req_id,
        response.status,
        [IPC_RAW_BODY_TYPE.BASE64_STREAM_ID, stream_id],
        headersToRecord(response.headers),
        ipc
      );
      $streamAsRawData(
        stream_id,
        response.body ?? (await response.blob()).stream(),
        ipc
      );
    } else {
      ipcResponse = new IpcResponse(
        req_id,
        response.status,
        [
          IPC_RAW_BODY_TYPE.BASE64,
          simpleDecoder(await response.arrayBuffer(), "base64"),
        ],
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
    headers["Content-Type"] = "application/json";
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
    headers["Content-Type"] = "text/plain";
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
    headers: Record<string, string> = {}
  ) {
    headers["Content-Type"] = "application/octet-stream";
    return new IpcResponse(
      req_id,
      statusCode,
      [IPC_RAW_BODY_TYPE.BASE64, simpleDecoder(binary, "base64")],
      headers,
      void 0 as never
    );
  }
  // static fromBinaryStream(
  //   req_id: number,
  //   statusCode: number,
  //   binary: Uint8Array | ReadableStream<Uint8Array>,
  //   headers: Record<string, string> = {},
  //   ipc: Ipc
  // ) {}
}
