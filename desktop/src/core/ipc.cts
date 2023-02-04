import {
  $SimpleEncoding,
  headersToRecord,
  PromiseOut,
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
  /** 类型：流数据 */
  STREAM_DATA,
  /** 类型：流拉取 */
  STREAM_PULL,
  /** 类型：流关闭 */
  STREAM_END,
}
export class IpcRequest {
  readonly type = IPC_DATA_TYPE.REQUEST;
  constructor(
    readonly req_id: number,
    readonly method: string,
    readonly url: string,
    readonly rawBody: $RawBody,
    readonly headers: Record<string, string>,
    ipc: Ipc
  ) {
    this.#ipc = ipc;
  }
  #ipc: Ipc;
  #body?: ReturnType<typeof $RawBodyToBody>;
  get body() {
    return (this.#body ??= $RawBodyToBody(this.rawBody, this.#ipc));
  }
  #parsed_url?: URL;
  get parsed_url() {
    return (this.#parsed_url ??= new URL(this.url));
  }
}

export class IpcStreamData {
  readonly type = IPC_DATA_TYPE.STREAM_DATA;
  constructor(readonly stream_id: string, readonly data: string) {}
}
export class IpcStreamPull {
  readonly type = IPC_DATA_TYPE.STREAM_PULL;
  readonly desiredSize: number;
  constructor(readonly stream_id: string, desiredSize?: number | null) {
    if (desiredSize == null) {
      desiredSize = 1;
    } else if (Number.isFinite(desiredSize) === false) {
      desiredSize = 1;
    } else if (desiredSize < 1) {
      desiredSize = 1;
    }
    this.desiredSize = desiredSize;
  }
}
export class IpcStreamEnd {
  readonly type = IPC_DATA_TYPE.STREAM_END;
  constructor(readonly stream_id: string) {}
}

type $RawBody = readonly [IPC_RAW_BODY_TYPE, string];

const $RawBodyToBody = (rawBody: $RawBody, ipc?: Ipc) => {
  let body: string | Uint8Array | ReadableStream<Uint8Array>;

  const raw_body_type = rawBody[0];
  const encoding: $SimpleEncoding =
    raw_body_type & IPC_RAW_BODY_TYPE.BASE64 ? "base64" : "utf8";

  if (raw_body_type & IPC_RAW_BODY_TYPE.STREAM_ID) {
    if (ipc == null) {
      throw new Error(`miss ipc when ipc-response has stream-body`);
    }
    const stream_ipc = ipc;
    const stream_id = rawBody[1];
    body = new ReadableStream<Uint8Array>({
      start(controller) {
        const off = ipc.onMessage((message) => {
          if ("stream_id" in message && message.stream_id === stream_id) {
            if (message.type === IPC_DATA_TYPE.STREAM_DATA) {
              controller.enqueue(simpleEncoder(message.data, encoding));
            } else if (message.type === IPC_DATA_TYPE.STREAM_END) {
              controller.close();
              off();
            }
          }
        });
      },
      pull(controller) {
        stream_ipc.postMessage(
          new IpcStreamPull(stream_id, controller.desiredSize)
        );
      },
    });
  } else {
    body =
      encoding == "utf8" ? rawBody[1] : simpleEncoder(rawBody[1], encoding);
  }
  return body;
};
/**
 * @TODO 这里将加入反压的行为，不能一直无脑发，而是对方有需要的时候才发
 * @param stream_id
 * @param stream
 * @param ipc
 */
const $streamAsBodyRaw = (
  stream_id: string,
  stream: ReadableStream<Uint8Array>,
  ipc: Ipc
) => {
  const reader = stream.getReader();
  /// 这里的数据发送是按需迭代，而不是马上发
  /// 马上发会有一定的问题，需要确保对方收到 IpcResponse 对象后，并且开始接收数据时才能开始
  /// 否则发过去的数据 IpcResponse 如果还没构建完，就导致 IpcStreamData 无法认领，为了内存安全必然要被抛弃
  /// 所以整体上来说，我们使用 pull 的逻辑，让远端来要求我们去发送数据
  const off = ipc.onMessage(async (message) => {
    if (
      message.type === IPC_DATA_TYPE.STREAM_PULL &&
      message.stream_id === stream_id
    ) {
      // let desiredSize = message.desiredSize;
      // while (desiredSize-- > 0) {}

      /// 预期值仅供参考
      console.log("desiredSize:", message.desiredSize);

      await sender.next();
    }
  });
  const sender = (async function* () {
    while (true) {
      yield;
      const item = await reader.read();
      if (item.done) {
        ipc.postMessage(new IpcStreamEnd(stream_id));
        break;
      } else {
        ipc.postMessage(
          new IpcStreamData(stream_id, simpleDecoder(item.value, "base64"))
        );
      }
    }

    /// 解除pull绑定
    off();
  })();
};

export const enum IPC_RAW_BODY_TYPE {
  /** 文本 json html 等 */
  TEXT = 1 << 1,
  /** 二进制 */
  BASE64 = 1 << 2,
  /** 流 */
  STREAM_ID = 1 << 3,
  /** 文本流 */
  TEXT_STREAM_ID = IPC_RAW_BODY_TYPE.STREAM_ID | IPC_RAW_BODY_TYPE.TEXT,
  /** 二进制流 */
  BASE64_STREAM_ID = IPC_RAW_BODY_TYPE.STREAM_ID | IPC_RAW_BODY_TYPE.BASE64,
}

export class IpcResponse {
  readonly type = IPC_DATA_TYPE.RESPONSE;
  constructor(
    readonly req_id: number,
    readonly statusCode: number,
    readonly rawBody: $RawBody,
    readonly headers: Record<string, string>,
    ipc: Ipc
  ) {
    this.#ipc = ipc;
  }
  #ipc: Ipc;
  #body?: ReturnType<typeof $RawBodyToBody>;
  get body() {
    return (this.#body ??= $RawBodyToBody(this.rawBody, this.#ipc));
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
      $streamAsBodyRaw(
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

export const enum IPC_ROLE {
  SERVER = "server",
  CLIENT = "client",
}
let ipc_uid_acc = 0;
export abstract class Ipc {
  readonly uid = ipc_uid_acc++;
  abstract readonly remote: $MicroModule;
  abstract readonly role: IPC_ROLE;
  abstract postMessage(data: $IpcMessage): void;
  abstract onMessage(cb: $IpcOnMessage): () => boolean;
  abstract close(): void;
  abstract onClose(cb: () => unknown): () => boolean;

  private readonly _reqresMap = new Map<number, PromiseOut<IpcResponse>>();
  private _req_id_acc = 0;
  allocReqId() {
    return this._req_id_acc++;
  }

  private _inited_req_res = false;
  private _initReqRes() {
    if (this._inited_req_res) {
      return;
    }
    this._inited_req_res = true;
    this.onMessage((message) => {
      if (message.type === IPC_DATA_TYPE.RESPONSE) {
        const response_po = this._reqresMap.get(message.req_id);
        if (response_po) {
          this._reqresMap.delete(message.req_id);
          response_po.resolve(message);
        } else {
          throw new Error(`no found response by req_id: ${message.req_id}`);
        }
      }
    });
  }

  /** 发起请求并等待响应 */
  request(
    url: string,
    init: {
      method?: string;
      body?: /* json+text */
      | string
        /* base64 */
        | Uint8Array
        /* stream+base64 */
        | ReadableStream<Uint8Array>;
      headers?: Record<string, string>;
    } = {}
  ) {
    const req_id = this.allocReqId();
    let rawBody: $RawBody;
    if (ArrayBuffer.isView(init.body)) {
      rawBody = [IPC_RAW_BODY_TYPE.BASE64, simpleDecoder(init.body, "base64")];
    } else if (init.body instanceof ReadableStream) {
      const contentLength = init.headers?.["Content-Length"] ?? 0;
      const stream_id = `req/${req_id}/${contentLength}`;
      rawBody = [IPC_RAW_BODY_TYPE.BASE64_STREAM_ID, stream_id];
      $streamAsBodyRaw(stream_id, init.body, this);
    } else {
      rawBody = [IPC_RAW_BODY_TYPE.TEXT, init.body ?? ""];
    }

    const ipcRequest = new IpcRequest(
      req_id,
      init.method ?? "GET",
      url,
      rawBody,
      init.headers ?? {},
      this
    );
    this.postMessage(ipcRequest);
    return this.registerReqId(req_id).promise;
  }
  /** 自定义注册 请求与响应 的id */
  registerReqId(req_id = this.allocReqId()) {
    const response_po = new PromiseOut<IpcResponse>();
    this._reqresMap.set(req_id, response_po);
    this._initReqRes();
    return response_po;
  }
}

export type $IpcMessage =
  | IpcRequest
  | IpcResponse
  | IpcStreamData
  | IpcStreamPull
  | IpcStreamEnd;
export type $IpcOnMessage = (message: $IpcMessage, ipc: Ipc) => unknown;
