import { isBinary } from "../../helper/binaryHelper.ts";
import { Ipc, IpcBody, IpcHeaders, IpcResponse, IpcServerRequest } from "../ipc/index.ts";
import { $bodyInitToIpcBodyArgs, isWebSocket } from "./ipcRequestHelper.ts";
import { type $PromiseMaybe } from "./types.ts";

export type $OnFetchReturn = Response | IpcResponse | $FetchResponse | void;

/**
 * fetch 处理函数
 * 如果标记成 中间件 模式，那么该函数会执行之前函数
 */
export type $OnFetch = (event: IpcFetchEvent) => $PromiseMaybe<$OnFetchReturn>;

export type $OnFetchMid = (respose: IpcResponse, event: IpcFetchEvent) => $PromiseMaybe<$OnFetchReturn>;
/**
 * 对即将要进行的响应内容，作出额外的处理
 */
export const fetchMid = (handler: $OnFetchMid) => Object.assign(handler, { [FETCH_MID_SYMBOL]: true } as const);
export const FETCH_MID_SYMBOL = Symbol("fetch.middleware");

export type $OnFetchEnd = (event: IpcFetchEvent, respose: IpcResponse | undefined) => $PromiseMaybe<$OnFetchReturn>;
/**
 * 对即将要进行的响应内容，做出最后的处理
 *
 * 如果没有返回值，那么就不会执行 ipc.postMessage
 */
export const fetchEnd = (handler: $OnFetchEnd) => Object.assign(handler, { [FETCH_END_SYMBOL]: true } as const);
export const FETCH_END_SYMBOL = Symbol("fetch.end");

export type $OnWebSocket = $OnFetch;
/**
 * 响应 WebSocket
 * @param handler
 * @returns
 */
export const fetchWs = (handler: $OnWebSocket) =>
  Object.assign(
    ((event) => {
      if (isWebSocket(event.method, event.headers)) {
        return handler(event);
      }
    }) satisfies $OnFetch,
    { [FETCH_WS_SYMBOL]: true } as const
  );
export const FETCH_WS_SYMBOL = Symbol("fetch.websocket");

/**
 * 目前对于响应，有三种角色
 *
 * 响应器：在前面没有做出响应内容的情况下，会执行响应器来获取响应内容。直到有一个响应器做出响应，那么后续的响应器就不会被执行。
 * 中间件：在前面拥有响应内容的情况下，基于该响应 做出修改原有的响应内容 或者 替换新的响应内容
 * 终止符：不论前面有无响应内容，都会执行它，它既可以像响应器和中间件一样去创建修改响应内容，也可以取消内容。（如果取消，那么后面的响应器就会因为 当前没有响应内容 而继续运作。所以你可以用它来切分组合你的响应流）
 */
export type $AnyFetchHanlder =
  | $OnFetch
  | ReturnType<typeof fetchMid>
  | ReturnType<typeof fetchEnd>
  | ReturnType<typeof fetchWs>;

const $throw = (err: Error) => {
  throw err;
};

export const fetchHanlderFactory = {
  NoFound: () => fetchEnd((_event, res) => res ?? $throw(new FetchError("No Found", { status: 404 }))),
  Forbidden: () => fetchEnd((_event, res) => res ?? $throw(new FetchError("Forbidden", { status: 403 }))),
  BadRequest: () => fetchEnd((_event, res) => res ?? $throw(new FetchError("Bad Request", { status: 400 }))),
  InternalServerError: (message = "Internal Server Error") =>
    fetchEnd((_event, res) => res ?? $throw(new FetchError(message, { status: 500 }))),
  // deno-lint-ignore no-explicit-any
} satisfies Record<string, (...args: any[]) => $AnyFetchHanlder>;
/**
 * 一个通用的 ipcRequest 处理器
 * 开发不需要面对 ipcRequest，而是面对 web 标准的 Request、Response 即可
 */
export const createFetchHandler = (onFetchs: Iterable<$OnFetch> = []) => {
  const onFetchHanlders: $AnyFetchHanlder[] = [...onFetchs];

  // deno-lint-ignore ban-types
  const extendsTo = <T extends {}>(_to: T) => {
    // deno-lint-ignore no-explicit-any
    const wrapFactory = <T extends (...args: any[]) => $AnyFetchHanlder>(factory: T) => {
      return (...args: Parameters<T>) => {
        onFetchHanlders.push(factory(...args));
        return to;
      };
    };

    const EXT = {
      onFetch: (handler: $OnFetch) => {
        onFetchHanlders.push(handler);
        return to;
      },
      onWebSocket: (hanlder: $OnWebSocket) => {
        onFetchHanlders.push(hanlder);
        return to;
      },
      mid: (handler: $OnFetchMid) => {
        onFetchHanlders.push(fetchMid(handler));
        return to;
      },
      end: (handler: $OnFetchEnd) => {
        onFetchHanlders.push(fetchEnd(handler));
        return to;
      },
      /**
       * 配置跨域，一般是最后调用
       * @param config
       */
      cors: (config: { origin?: string; headers?: string; methods?: string } = {}) => {
        /// options 请求一般是跨域时，询问能否post，这里统一返回空就行，后面再加上 Access-Control-Allow-Methods
        onFetchHanlders.unshift(((event) => {
          if (event.method === "OPTIONS") {
            return { body: "" };
          }
        }) satisfies $OnFetch);

        /// 如果有响应，统一加上响应头
        onFetchHanlders.push(
          fetchMid((res) => {
            res?.headers
              .init("Access-Control-Allow-Origin", config.origin ?? "*")
              .init("Access-Control-Allow-Headers", config.headers ?? "*")
              .init("Access-Control-Allow-Methods", config.methods ?? "*");
            return res;
          })
        );
        return to;
      },
      noFound: wrapFactory(fetchHanlderFactory.NoFound),
      forbidden: wrapFactory(fetchHanlderFactory.Forbidden),
      badRequest: wrapFactory(fetchHanlderFactory.BadRequest),
      internalServerError: wrapFactory(fetchHanlderFactory.InternalServerError),
      extendsTo,
    };
    const to = _to as unknown as typeof EXT & T;
    Object.assign(to, EXT);
    return to;
  };

  const onRequest = async (request: IpcServerRequest) => {
    const ipc = request.ipc;
    const event = new IpcFetchEvent(request, ipc);
    let res: IpcResponse | undefined;

    for (const handler of onFetchHanlders) {
      try {
        let result: $OnFetchReturn = undefined;
        if (FETCH_MID_SYMBOL in handler) {
          if (res !== undefined) {
            result = await handler(res, event);
          }
        } else if (FETCH_END_SYMBOL in handler) {
          result = await handler(event, res);
        } else {
          if (res === undefined) {
            result = await handler(event);
          }
        }
        if (result instanceof IpcResponse) {
          res = result;
        } else if (result instanceof Response) {
          /// TODO 需要加入对 Response.error() 的支持，这需要新增 IpcError { message:string, reqId?:number } 的消息
          res = await IpcResponse.fromResponse(request.reqId, result, ipc);
        } else if (typeof result === "object") {
          /// 尝试构建出 IpcResponse
          const reqId = request.reqId;
          const status = result.status ?? 200;
          const headers = new IpcHeaders(result.headers);
          if (result.body instanceof IpcBody) {
            res = new IpcResponse(reqId, status, headers, result.body, ipc);
          } else {
            const body = await $bodyInitToIpcBodyArgs(result.body, (bodyInit) => {
              /// 尝试使用 JSON 解码
              if (
                headers.has("Content-Type") === false ||
                headers.get("Content-Type")!.startsWith("application/javascript")
              ) {
                headers.init("Content-Type", "application/javascript;charset=utf8");
                return JSON.stringify(bodyInit);
              }
              // 否则直接处理成字符串
              return String(bodyInit);
            });
            if (typeof body === "string") {
              res = IpcResponse.fromText(reqId, status, headers, body, ipc);
            } else if (isBinary(body)) {
              res = IpcResponse.fromBinary(reqId, status, headers, body, ipc);
            } else if (body instanceof ReadableStream) {
              res = IpcResponse.fromStream(reqId, status, headers, body, ipc);
            }
          }
        }
      } catch (err) {
        if (err instanceof Response) {
          res = await IpcResponse.fromResponse(request.reqId, err, ipc);
        } else {
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
              request.reqId,
              err_code,
              new IpcHeaders().init("Content-Type", "text/html;charset=utf8"),
              { message: err_message, detail: err_detail },
              ipc
            );
          } else {
            res = IpcResponse.fromText(
              request.reqId,
              err_code,
              new IpcHeaders().init("Content-Type", "text/html;charset=utf8"),
              err instanceof Error ? `<h1>${err.message}</h1><hr/><pre>${err.stack}</pre>` : String(err),
              ipc
            );
          }
        }
      }
    }
    /// 发送
    if (res) {
      ipc.postMessage(res);
      return res;
    }
  };

  return extendsTo(onRequest);
};

export type $FetchHandler = ReturnType<typeof createFetchHandler>;
export interface $FetchResponse extends ResponseInit {
  body?: BodyInit | null | IpcBody;
}

export class IpcFetchEvent {
  constructor(readonly ipcRequest: IpcServerRequest, readonly ipc: Ipc) {}
  get url() {
    return this.ipcRequest.parsed_url;
  }
  get pathname() {
    return this.url.pathname;
  }
  get search() {
    return this.url.search;
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
  async typedArray() {
    return new Uint8Array(await this.request.arrayBuffer());
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
  // deno-lint-ignore no-explicit-any
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
  get reqId() {
    return this.ipcRequest.reqId;
  }
}

export class FetchError extends Error {
  constructor(message: string, options?: $FetchErrorOptions) {
    super(message);
    this.code = options?.status ?? 500;
  }
  readonly code: number;
}
export interface $FetchErrorOptions {
  status?: number;
  cause?: unknown;
}
