import {
  $SimpleEncoding,
  headersToRecord,
  simpleDecoder,
  simpleEncoder,
} from "./helper.cjs";
import type { $MicroModule } from "./types.cjs";

export const enum IPC_DATA_TYPE {
  // /** 特殊位：结束符 */
  // END = 1,
  /** 类型：请求 */
  REQUEST,
  /** 类型：相应 */
  RESPONSE,
  /** 类型：流 */
  STREAM,
  /** 类型：流关闭 */
  STREAM_END,
}
export class IpcRequest {
  readonly type = IPC_DATA_TYPE.REQUEST;
  constructor(
    readonly req_id: number,
    readonly method: string,
    readonly url: string,
    readonly body: string,
    readonly headers: Record<string, string>
  ) {}
  #parsed_url?: URL;
  get parsed_url() {
    return (this.#parsed_url ??= new URL(this.url));
  }
}

export class IpcStream {
  readonly type = IPC_DATA_TYPE.STREAM;
  constructor(readonly stream_id: string, readonly data: string) {}
}
export class IpcStreamEnd {
  readonly type = IPC_DATA_TYPE.STREAM_END;
  constructor(readonly stream_id: string) {}
}

export const enum IPC_RESPONSE_RAW_BODY_TYPE {
  /** 文本 json html 等 */
  TEXT = 1 << 1,
  /** 二进制 */
  BASE64 = 1 << 2,
  /** 流 */
  STREAM_ID = 1 << 3,
  /** 文本流 */
  TEXT_STREAM_ID = IPC_RESPONSE_RAW_BODY_TYPE.STREAM_ID |
    IPC_RESPONSE_RAW_BODY_TYPE.TEXT,
  /** 二进制流 */
  BASE64_STREAM_ID = IPC_RESPONSE_RAW_BODY_TYPE.STREAM_ID |
    IPC_RESPONSE_RAW_BODY_TYPE.BASE64,
}

export class IpcResponse {
  readonly type = IPC_DATA_TYPE.RESPONSE;
  constructor(
    readonly req_id: number,
    readonly statusCode: number,
    readonly rawBody: readonly [IPC_RESPONSE_RAW_BODY_TYPE, string],
    readonly headers: Record<string, string>,
    ipc?: Ipc
  ) {
    let body: string | Uint8Array | ReadableStream<Uint8Array | string>;

    const raw_body_type = rawBody[0];
    const encoding: $SimpleEncoding =
      raw_body_type & IPC_RESPONSE_RAW_BODY_TYPE.BASE64 ? "base64" : "utf8";

    if (raw_body_type === IPC_RESPONSE_RAW_BODY_TYPE.STREAM_ID) {
      if (ipc == null) {
        throw new Error(`miss ipc when ipc-response has stream-body`);
      }
      const stream_id = rawBody[1];
      body = new ReadableStream<Uint8Array>({
        start(controller) {
          const off = ipc.onMessage((message) => {
            if ("stream_id" in message && message.stream_id === stream_id) {
              if (message.type === IPC_DATA_TYPE.STREAM) {
                controller.enqueue(simpleEncoder(message.data, encoding));
              } else if (message.type === IPC_DATA_TYPE.STREAM_END) {
                controller.close();
                off();
              }
            }
          });
        },
      });
    } else {
      body = simpleEncoder(rawBody[1], encoding);
    }
    this.#body = body;
  }
  #body: string | Uint8Array | ReadableStream<Uint8Array | string>;
  get body() {
    return this.#body;
  }

  static ORIGIN = Symbol.for("origin-ipc-response");
  asResponse() {
    const response = new Response(this.body, {
      headers: this.headers,
      status: this.statusCode,
    });
    (response as any)[IpcResponse.ORIGIN] = this;
    return response;
  }

  /** 将 response 对象进行转码变成 ipcResponse */
  static async formResponse(req_id: number, response: Response, ipc: Ipc) {
    let ipcResponse = (response as any)[IpcResponse.ORIGIN] as
      | IpcResponse
      | undefined;
    if (ipcResponse === undefined) {
      const contentLength = response.headers.get("Content-Length") ?? 0;

      /// 6kb 为分水岭，超过 6kb 就改用流传输，或者如果原本就是流对象
      /// TODO 这里是否使用流模式，应该引入更加只能的判断方式，比如先读取一部分数据，如果能读取完成，那么就直接吧 end 符号一起带过去
      if (contentLength > 6144 || response.body) {
        const stream_id = `${req_id}/${contentLength}`;
        ipcResponse = new IpcResponse(
          req_id,
          response.status,
          [IPC_RESPONSE_RAW_BODY_TYPE.BASE64_STREAM_ID, stream_id],
          headersToRecord(response.headers)
        );
        (async (stream) => {
          const reader = stream.getReader();
          while (true) {
            const item = await reader.read();
            if (item.done) {
              ipc.postMessage(new IpcStreamEnd(stream_id));
            } else {
              ipc.postMessage(
                new IpcStream(stream_id, simpleDecoder(item.value, "base64"))
              );
            }
          }
        })(response.body ?? (await response.blob()).stream());
      } else {
        ipcResponse = new IpcResponse(
          req_id,
          response.status,
          [
            IPC_RESPONSE_RAW_BODY_TYPE.BASE64,
            simpleDecoder(await response.arrayBuffer(), "base64"),
          ],
          headersToRecord(response.headers)
        );
      }

      (response as any)[IpcResponse.ORIGIN] = ipcResponse;
    }

    return ipcResponse;
  }
  static fromJson(
    req_id: number,
    statusCode: number,
    json: unknown,
    headers: Record<string, string> = {}
  ) {
    headers["Content-Type"] = "application/json";
    return new IpcResponse(
      req_id,
      statusCode,
      [IPC_RESPONSE_RAW_BODY_TYPE.TEXT, JSON.stringify(json)],
      headers
    );
  }
  static fromText(
    req_id: number,
    statusCode: number,
    json: unknown,
    headers: Record<string, string> = {}
  ) {
    headers["Content-Type"] = "text/plain";
    return new IpcResponse(
      req_id,
      statusCode,
      [IPC_RESPONSE_RAW_BODY_TYPE.TEXT, JSON.stringify(json)],
      headers
    );
  }
  static fromBinary(
    req_id: number,
    statusCode: number,
    binary: Uint8Array,
    headers: Record<string, string> = {}
  ) {
    headers["Content-Type"] = "application/octet-stream";
    return new IpcResponse(
      req_id,
      statusCode,
      [IPC_RESPONSE_RAW_BODY_TYPE.BASE64, simpleDecoder(binary, "base64")],
      headers
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

export const enum IPC_ROLE {
  SERVER = "server",
  CLIENT = "client",
}
let ipc_uid_acc = 0;
export abstract class Ipc {
  readonly uid = ipc_uid_acc++;
  abstract readonly module: $MicroModule;
  abstract readonly role: IPC_ROLE;
  abstract postMessage(data: $IpcMessage): void;
  abstract onMessage(cb: $IpcOnMessage): () => boolean;
  abstract close(): void;
  abstract onClose(cb: () => unknown): () => boolean;
}

export type $IpcMessage = IpcRequest | IpcResponse | IpcStream | IpcStreamEnd;
export type $IpcOnMessage = (message: $IpcMessage) => unknown;
