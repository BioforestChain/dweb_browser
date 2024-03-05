import { binaryToU8a, isBinary } from "../../helper/binaryHelper.ts";
import { headersToRecord, httpMethodCanOwnBody } from "../../helper/httpHelper.ts";
import { IPC_METHOD } from "../ipc/helper/const.ts";

/**
 * 将 RequestInit 解构成 ipcRequest 的构造参数
 */
export const $normalizeRequestInitAsIpcRequestArgs = async (request_init: RequestInit) => {
  const method = request_init.method ?? "GET";

  /// 读取 body
  const body = httpMethodCanOwnBody(method, request_init.headers)
    ? await $bodyInitToIpcBodyArgs(request_init.body)
    : "";

  /// 读取 headers
  const headers = headersToRecord(request_init.headers);

  return { method, body, headers };
};

export const $bodyInitToIpcBodyArgs = async (
  bodyInit?: BodyInit | null,
  onUnknown?: (bodyInit: unknown) => Uint8Array | ReadableStream<Uint8Array> | string
) => {
  let body: Uint8Array | ReadableStream<Uint8Array> | string = "";
  if (bodyInit instanceof FormData || bodyInit instanceof URLSearchParams) {
    bodyInit = await new Request("", {
      body: bodyInit,
    }).blob();
  }

  if (bodyInit instanceof ReadableStream) {
    body = bodyInit;
  } else if (bodyInit instanceof Blob) {
    // 16kb 以上, 就用流传输
    if (bodyInit.size >= 16 * 1024 * 1024) {
      body = bodyInit?.stream() || "";
    }
    // 16kb 以下，直接单帧传输
    if (body === "") {
      body = new Uint8Array(await bodyInit.arrayBuffer());
    }
  } else if (isBinary(bodyInit)) {
    body = binaryToU8a(bodyInit);
  } else if (typeof bodyInit === "string") {
    body = bodyInit;
  } else if (bodyInit) {
    if (onUnknown) {
      bodyInit = onUnknown(bodyInit);
    } else {
      throw new Error(`unsupport body type: ${(bodyInit as any)?.constructor.name}`);
    }
  }
  return body;
};
export const isWebSocket = (method: IPC_METHOD | (string & {}), headers: Headers) => {
  return method === "GET" && headers.get("Upgrade")?.toLowerCase() === "websocket";
};
/**
 * 构建Request对象，和`new Request`类似，允许突破原本Request的一些限制
 * @param toRequest
 */
export const buildRequestX = (url: string | URL, init: RequestInit = {}) => {
  let method = init.method ?? IPC_METHOD.GET;
  const headers = init.headers instanceof Headers ? init.headers : new Headers(init.headers);
  const isWs = isWebSocket(method, headers);
  let body: undefined | BodyInit | null;
  if (isWs) {
    method = IPC_METHOD.POST; // new Request 如果要携带body，method 不可以是 GET
    body = init.body;
  } else if (httpMethodCanOwnBody(method)) {
    body = init.body;
  }
  const request_init = {
    method,
    headers,
    body,
    duplex: body instanceof ReadableStream ? "half" : undefined,
  };
  const request = new Request(url, request_init);
  if (isWs) {
    Object.defineProperty(request, "method", {
      configurable: true,
      enumerable: true,
      writable: false,
      value: "GET",
    });
  }
  // 兼容浏览器不支持的情况chrome < 105
  if(request_init.body instanceof ReadableStream && request.body!=request_init.body){
    Object.defineProperty(request, "body", {
      configurable: true,
      enumerable: true,
      writable: false,
      value: request_init.body,
    });
  }
  return request;
};
