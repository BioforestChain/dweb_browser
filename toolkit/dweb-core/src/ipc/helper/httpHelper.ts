import { isWebSocket } from "./ipcRequestHelper.ts";

export const headersToRecord = (headers?: HeadersInit | null) => {
  let record: Record<string, string> = Object.create(null);
  if (headers) {
    let req_headers: Headers | undefined;
    if (headers instanceof Array) {
      req_headers = new Headers(headers);
    } else if (headers instanceof Headers) {
      req_headers = headers;
    } else {
      record = headers as Record<string, string>;
    }
    if (req_headers !== undefined) {
      req_headers.forEach((value, key) => {
        record[key] = value;
      });
    }
  }
  return record;
};

export type $Method =
  | "GET" // 查
  | "POST" // 增
  | "PUT" // 改：替换
  | "PATCH" // 改：局部更新
  | "DELETE" // 删
  | "OPTIONS" //  嗅探
  | "HEAD" // 预查
  | "CONNECT" // 双工
  | "TRACE"; // 调试

// deno-lint-ignore ban-types
export const httpMethodCanOwnBody = (method: $Method | (string & {}), headers?: HeadersInit) => {
  if (headers !== undefined && method === "GET") {
    return isWebSocket(method, headers instanceof Headers ? headers : new Headers(headers));
  }
  return method !== "GET" && method !== "HEAD" && method !== "TRACE" && method !== "OPTIONS";
};

/**
 * 获得一个资源对象的总大小，如果使用了分片下载，那么也返回总大小而不是分片的大小
 * @param headers
 * @returns
 */
export const headersGetTotalLength = (headers: Headers) => {
  const totalLength =
    /**
     * Content-Length: 2
     * Accept-Ranges: bytes
     * Content-Range: bytes 0-1/4300047
     */
    headers.get("Content-Range")?.split("/").pop() ??
    /**
     * Content-Length: 4300047
     */
    headers.get("Content-Length");

  if (totalLength) {
    const len = parseInt(totalLength);
    if (Number.isFinite(len)) {
      return len;
    }
  }
};
