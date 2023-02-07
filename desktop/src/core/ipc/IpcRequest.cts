import { $Binary, binaryToU8a } from "../../helper/binaryHelper.cjs";
import { simpleDecoder } from "../../helper/encoding.cjs";
import { parseUrl } from "../../helper/urlHelper.cjs";
import { $streamAsRawData } from "./$streamAsRawData.cjs";
import { IPC_DATA_TYPE, IPC_RAW_BODY_TYPE, type $RawData } from "./const.cjs";
import type { Ipc } from "./ipc.cjs";
import { IpcBody } from "./IpcBody.cjs";

export class IpcRequest extends IpcBody {
  readonly type = IPC_DATA_TYPE.REQUEST;
  constructor(
    readonly req_id: number,
    readonly method: string,
    readonly url: string,
    rawBody: $RawData,
    readonly headers: Record<string, string>,
    ipc: Ipc
  ) {
    super(rawBody, ipc);
  }

  #parsed_url?: URL;
  get parsed_url() {
    return (this.#parsed_url ??= parseUrl(this.url));
  }

  static fromText(
    text: string,
    req_id: number,
    method: string,
    url: string,
    headers: Record<string, string>
    // ipc: Ipc
  ) {
    const rawBody: $RawData = [IPC_RAW_BODY_TYPE.TEXT, text];
    return new IpcRequest(
      req_id,
      method,
      url,
      rawBody,
      headers,
      void 0 as never
    );
  }
  static fromBinary(
    binary: $Binary,
    req_id: number,
    method: string,
    url: string,
    headers: Record<string, string>,
    ipc: Ipc
  ) {
    const rawBody: $RawData = ipc.support_message_pack
      ? [IPC_RAW_BODY_TYPE.BINARY, binaryToU8a(binary)]
      : [IPC_RAW_BODY_TYPE.BASE64, simpleDecoder(binary, "base64")];
    return new IpcRequest(req_id, method, url, rawBody, headers, ipc);
  }
  static fromStream(
    stream: ReadableStream<Uint8Array>,
    req_id: number,
    method: string,
    url: string,
    headers: Record<string, string>,
    ipc: Ipc
  ) {
    const contentLength = headers?.["Content-Length"] ?? 0;
    const stream_id = `req/${req_id}/${contentLength}`;
    const rawBody: $RawData = [IPC_RAW_BODY_TYPE.BASE64_STREAM_ID, stream_id];
    $streamAsRawData(stream_id, stream, ipc);
    return new IpcRequest(req_id, method, url, rawBody, headers, ipc);
  }
}
