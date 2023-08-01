/// about:blank

import { jsonlinesStreamRead } from "helper/stream/jsonlinesStreamHelper.ts";

/// about:newtab
const BASE_URL =
  location.protocol === "about:" || location.protocol === "chrome:"
    ? new URL(`http://browser.dweb.localhost/${location.href.replace(new RegExp(location.protocol + "/+"), "")}`)
    : new URL(location.href);
const apiUrl = new URL(BASE_URL.searchParams.get("api-base") ?? BASE_URL);
/**
 * 默认网关是自己
 */
const baseMmid = apiUrl.searchParams.get("mmid") ?? "desk.browser.dweb";
/**
 * 缓存那些不支持的请求
 */
const noImplementedCaches = new Map<string, string>();
/** 发起请求，响应JSON */
export const nativeFetch = async <T extends unknown>(pathname: string, init?: $BuildRequestInit) => {
  if (noImplementedCaches.has(pathname)) {
    throw noImplementedCaches.get(pathname);
  }
  const res = await fetch(...buildApiRequestArgs(pathname, init));
  if (res.ok) {
    return (await res.json()) as T;
  } else {
    const errorCache = await res.text();
    /// 501 Not Implemented
    if (res.status === 501) {
      noImplementedCaches.set(init?.mmid ?? baseMmid, errorCache);
    }
    throw errorCache;
  }
};

/** 发起请求，响应JSON流 */
export const nativeFetchStream = <T>(
  pathname: string,
  init?: $BuildRequestInit,
  options?: { signal?: AbortSignal }
) => {
  const [url] = buildApiRequestArgs(pathname, init);
  const wsUrl = url.href.replace("http", "ws");
  const ws = new WebSocket(wsUrl);
  ws.binaryType = "arraybuffer";

  return jsonlinesStreamRead<T>(
    new ReadableStream<Uint8Array>({
      start(controller) {
        ws.onerror = (e) => {
          controller.error(e);
        };
        ws.onclose = () => {
          controller.close();
        };
        ws.onmessage = (msgEvent) => {
          controller.enqueue(msgEvent.data);
        };
      },
      cancel() {
        ws.close();
      },
    }),
    options
  );
};

export function buildApiRequestArgs(pathname: string, init?: $BuildRequestInit) {
  const mmid = init?.mmid ?? baseMmid;
  const url = new URL(`api/${mmid}${pathname}`, apiUrl); // 复制一份，这样可以保留 search 信息
  {
    url.search = apiUrl.search;
    url.hash = apiUrl.hash;
  }

  const search = init?.search;
  if (search) {
    let extendsSearch: URLSearchParams;
    if (search instanceof URLSearchParams) {
      extendsSearch = search;
    } else if (typeof search === "string") {
      extendsSearch = new URLSearchParams(search);
    } else {
      extendsSearch = new URLSearchParams(
        Object.entries(search)
          .filter(
            // eslint-disable-next-line @typescript-eslint/no-unused-vars
            ([_, value]) => value != undefined /* null undefined 都不传输*/
          )
          .map(([key, value]) => {
            return [key, typeof value === "object" ? JSON.stringify(value) : String(value)] as [string, string];
          })
      );
    }
    extendsSearch.forEach((value, key) => {
      url.searchParams.append(key, value);
    });
  }
  return [url, init] as const;
}

interface $BuildRequestInit extends RequestInit {
  search?:
    | ConstructorParameters<typeof URLSearchParams>[0]
    // deno-lint-ignore no-explicit-any
    | Record<string, any>;
  base?: string;
  mmid?: `${string}.dweb`;
}
