import { $Binary, binaryToU8a } from "../../helper/binaryHelper.cjs";
import { simpleDecoder } from "../../helper/encoding.cjs";
import { parseUrl } from "../../helper/urlHelper.cjs";
import { $streamAsRawData } from "./$streamAsRawData.cjs";
import { IPC_DATA_TYPE, IPC_RAW_BODY_TYPE, type $RawData } from "./const.cjs";
import type { Ipc } from "./ipc.cjs";
import { IpcBody } from "./IpcBody.cjs";
import { IpcHeaders } from "./IpcHeaders.cjs";

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
    headers = new IpcHeaders()
    // ipc: Ipc
  ) {
    // 这里 content-length 默认不写，因为这是要算二进制的长度，我们这里只有在字符串的长度，不是一个东西
    return new IpcRequest(
      req_id,
      method,
      url,
      [IPC_RAW_BODY_TYPE.TEXT, text],
      headers.toJSON(),
      void 0 as never
    );
  }
  static fromBinary(
    binary: $Binary,
    req_id: number,
    method: string,
    url: string,
    headers = new IpcHeaders(),
    ipc: Ipc
  ) {
    headers.init("Content-Type", "application/octet-stream");
    headers.init("Content-Length", binary.byteLength + "");
    const rawBody: $RawData = ipc.support_message_pack
      ? [IPC_RAW_BODY_TYPE.BINARY, binaryToU8a(binary)]
      : [IPC_RAW_BODY_TYPE.BASE64, simpleDecoder(binary, "base64")];
    return new IpcRequest(req_id, method, url, rawBody, headers.toJSON(), ipc);
  }
  static fromStream(
    stream: ReadableStream<Uint8Array>,
    req_id: number,
    method: string,
    url: string,
    headers = new IpcHeaders(),
    ipc: Ipc
  ) {
    headers.init("Content-Type", "application/octet-stream");
    // headers["transfer-encoding"] ??= ipc.support_message_pack
    //   ? "base64"
    //   : "binary";
    const stream_id = `res/${req_id}/${headers.get("content-length") ?? "-"}`;
    $streamAsRawData(stream_id, stream, ipc);
    return new IpcRequest(
      req_id,
      method,
      url,
      ipc.support_message_pack
        ? [IPC_RAW_BODY_TYPE.BINARY_STREAM_ID, stream_id]
        : [IPC_RAW_BODY_TYPE.BASE64_STREAM_ID, stream_id],
      headers.toJSON(),
      ipc
    );
  }
}
