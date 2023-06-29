import { binaryToU8a, isBinary } from "../../helper/binaryHelper.ts";
import {
  headersToRecord,
  httpMethodCanOwnBody,
} from "../../helper/httpHelper.ts";

/**
 * 将 RequestInit 解构成 ipcRequest 的构造参数
 */
export const $normalizeRequestInitAsIpcRequestArgs = async (
  request_init: RequestInit
) => {
  const method = request_init.method ?? "GET";

  /// 读取 body
  const body = httpMethodCanOwnBody(method)
    ? await $bodyInitToIpcBodyArgs(request_init.body)
    : "";

  /// 读取 headers
  const headers = headersToRecord(request_init.headers);

  return { method, body, headers };
};

export const $bodyInitToIpcBodyArgs = async (
  bodyInit?: BodyInit | null,
  onUnknown?: (
    bodyInit: unknown
  ) => Uint8Array | ReadableStream<Uint8Array> | string
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
      throw new Error(
        `unsupport body type: ${(bodyInit as any)?.constructor.name}`
      );
    }
  }
  return body;
};
