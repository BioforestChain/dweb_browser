export const headersToRecord = (headers?: HeadersInit | null) => {
  let record: Record<string, string> = Object.create(null);
  if (headers) {
    let req_headers: Headers | undefined;
    if (headers instanceof Array) {
      req_headers = new Headers(headers);
    } else if (headers instanceof Headers) {
      req_headers = headers;
    } else {
      record = headers;
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

export const httpMethodCanOwnBody = (method: $Method | (string & {})) => {
  return (
    method !== "GET" &&
    method !== "HEAD" &&
    method !== "TRACE" &&
    method !== "OPTIONS"
  );
};
