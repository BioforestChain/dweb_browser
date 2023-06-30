import { isBinary } from "../../helper/binaryHelper.ts";
import {
  $OnIpcRequestMessage,
  Ipc,
  IpcHeaders,
  IpcRequest,
  IpcResponse,
} from "../ipc/index.ts";
import { $bodyInitToIpcBodyArgs } from "./ipcRequestHelper.ts";
import { $PromiseMaybe } from "./types.ts";

export type $OnFetch = (
  request: FetchEvent
) => $PromiseMaybe<Response | IpcResponse | $FetchResponse | void>;

/**
 * 对即将要进行的响应内容，作出最后的处理
 *
 *
 * 调用 next，可以得到后来者对response对象的处理完毕的内容
 * 如果不调用，就不会
 */
export type $BeforeResponse = (
  respose: IpcResponse | undefined,
  next: $BeforeResponse
) => $PromiseMaybe<IpcResponse | void>;
/**
 * 一个通用的 ipcRequest 处理器
 * 开发不需要面对 ipcRequest，而是面对 web 标准的 Request、Response 即可
 */
export const createFetchHandler = (
  onFetchs: Iterable<$OnFetch>,
  beforeResponses: Iterable<$BeforeResponse> = []
) => {
  return (async (request, ipc) => {
    const event = new FetchEvent(request, ipc);
    let res: IpcResponse | undefined;

    for (const onFetch of onFetchs) {
      try {
        const result = await onFetch(event);
        if (result instanceof IpcResponse) {
          res = result;
        } else if (result instanceof Response) {
          /// TODO 需要加入对 Response.error() 的支持，这需要新增 IpcError { message:string, reqId?:number } 的消息
          res = await IpcResponse.fromResponse(request.req_id, result, ipc);
        } else if (typeof result === "object") {
          /// 尝试构建出 IpcResponse
          const req_id = request.req_id;
          const status = result.status ?? 200;
          const headers = new IpcHeaders(result.headers);
          const body = await $bodyInitToIpcBodyArgs(result.body, (bodyInit) => {
            /// 尝试使用 JSON 解码
            if (
              headers.has("Content-Type") === false ||
              headers.get("Content-Type")!.startsWith("application/javascript")
            ) {
              headers.init(
                "Content-Type",
                "application/javascript,charset=utf8"
              );
              return JSON.stringify(bodyInit);
            }
            // 否则直接处理成字符串
            return String(bodyInit);
          });
          if (typeof body === "string") {
            res = IpcResponse.fromText(req_id, status, headers, body, ipc);
          } else if (isBinary(body)) {
            res = IpcResponse.fromBinary(req_id, status, headers, body, ipc);
          } else if (body instanceof ReadableStream) {
            res = IpcResponse.fromStream(req_id, status, headers, body, ipc);
          }
        }
      } catch (err) {
        /// 处理异常，尝试返回
        let err_code = 500;
        let err_message = "";
        let err_detail = "";
        if (err instanceof Error) {
          err_message = err.message;
          err_detail = err.stack ?? err.name;
          if (err instanceof FetchError) {
            err_code = err.code;
          }
        } else {
          err_message = String(err);
        }
        /// 根据对方的接收需求，尝试返回 JSON
        if (request.headers.get("Accept") === "application/json") {
          res = IpcResponse.fromJson(
            request.req_id,
            err_code,
            new IpcHeaders().init("Content-Type", "text/html,charset=utf8"),
            { message: err_message, detail: err_detail },
            ipc
          );
        } else {
          res = IpcResponse.fromText(
            request.req_id,
            err_code,
            new IpcHeaders().init("Content-Type", "text/html,charset=utf8"),
            err instanceof Error
              ? `<h1>${err.message}</h1><hr/><pre>${err.stack}</pre>`
              : String(err),
            ipc
          );
        }
      }

      /// 返回
      if (res) {
        ipc.postMessage(res);
        return res;
      }
    }
  }) satisfies $OnIpcRequestMessage;
};

export interface $FetchResponse extends ResponseInit {
  body: BodyInit;
}

export class FetchEvent {
  constructor(readonly ipcRequest: IpcRequest, readonly ipc: Ipc) {}
  get url() {
    return this.ipcRequest.parsed_url;
  }
  get pathname() {
    return this.url.pathname;
  }
  get searchParams() {
    return this.url.searchParams;
  }
  #request?: Request;
  get request() {
    return (this.#request ??= this.ipcRequest.toRequest());
  }

  //#region Body 相关的属性与方法

  /** A simple getter used to expose a `ReadableStream` of the body contents. */
  get body() {
    return this.request.body;
  }
  /** Stores a `Boolean` that declares whether the body has been used in a
   * response yet.
   */
  get bodyUsed() {
    return this.request.bodyUsed;
  }
  /** Takes a `Response` stream and reads it to completion. It returns a promise
   * that resolves with an `ArrayBuffer`.
   */
  arrayBuffer() {
    return this.request.arrayBuffer();
  }
  /** Takes a `Response` stream and reads it to completion. It returns a promise
   * that resolves with a `Blob`.
   */
  blob() {
    return this.request.blob();
  }
  /** Takes a `Response` stream and reads it to completion. It returns a promise
   * that resolves with a `FormData` object.
   */
  formData() {
    return this.request.formData();
  }
  /** Takes a `Response` stream and reads it to completion. It returns a promise
   * that resolves with the result of parsing the body text as JSON.
   */
  json<T = any>() {
    return this.request.json() as Promise<T>;
  }
  /** Takes a `Response` stream and reads it to completion. It returns a promise
   * that resolves with a `USVString` (text).
   */
  text() {
    return this.request.text();
  }
  //#endregion

  /** Returns a Headers object consisting of the headers associated with request. Note that headers added in the network layer by the user agent will not be accounted for in this object, e.g., the "Host" header. */
  get headers() {
    return this.ipcRequest.headers;
  }
  /** Returns request's HTTP method, which is "GET" by default. */
  get method() {
    return this.ipcRequest.method;
  }
  /** Returns the URL of request as a string. */
  get href() {
    return this.url.href;
  }
}

export class FetchError extends Error {
  constructor(message: string, options?: $FetchErrorOptions) {
    super(message, options);
    this.code = options?.code ?? 500;
  }
  readonly code: number;
}
export interface $FetchErrorOptions {
  code?: number;
  cause?: unknown;
}
