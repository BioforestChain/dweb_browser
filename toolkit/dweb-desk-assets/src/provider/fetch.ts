/// about:blank

import { jsonlinesStreamReadText } from "@dweb-browser/helper/stream/jsonlinesStreamHelper.ts";

/// about:newtab

const apiUrl = new URL(location.origin);
/**
 * 默认网关是自己
 */
const baseMmid = apiUrl.searchParams.get("mmid") ?? "desk.browser.dweb";
/**
 * 缓存那些不支持的请求
 */
const noImplementedCaches = new Map<string, string>();
/** 发起请求，响应JSON */
interface $NativeFetch {
  <T extends Blob = Blob>(pathname: string, init?: $BuildRequestInit & { responseType: "blob" }): Promise<T>;
  <T extends string = string>(pathname: string, init?: $BuildRequestInit & { responseType: "text" }): Promise<T>;
  <T extends unknown>(pathname: string, init?: $BuildRequestInit): Promise<T>;
}
export const nativeFetch: $NativeFetch = async <T extends unknown>(pathname: string, init?: $BuildRequestInit) => {
  if (noImplementedCaches.has(pathname)) {
    throw noImplementedCaches.get(pathname);
  }
  const res = await fetch(...buildApiRequestArgs(pathname, init));
  if (res.ok) {
    const responseType = init?.responseType ?? "json";

    let data: T;
    switch (responseType) {
      case "json":
        data = (await res.json()) as T;
        break;
      case "blob":
        data = (await res.blob()) as T;
        break;
      case "text":
        data = (await res.text()) as T;
        break;
    }
    return data;
  } else {
    const errorCache = await res.text();
    /// 501 Not Implemented
    if (res.status === 501) {
      noImplementedCaches.set(init?.mmid ?? baseMmid, errorCache);
    }
    throw errorCache;
  }
};

const wsMap = new Map<string, WebSocket>();

/** 发起请求，响应JSON流 */
export const nativeFetchStream = <T>(
  pathname: string,
  init?: $BuildRequestInit,
  options?: { signal?: AbortSignal },
) => {
  const [url] = buildApiRequestArgs(pathname, init);
  const wsUrl = url.href.replace("http", "ws").replace(/^dweb\+ws/, "ws");

  return jsonlinesStreamReadText<T>(
    new ReadableStream<string>({
      pull(controller) {
        // console.log("pull", wsUrl);
        pullMessage(wsUrl, controller);
      },
      cancel() {
        wsMap.get(wsUrl)?.close();
      },
    }),
    options,
  );
};
// 断开重连机制
function pullMessage(wsUrl: string, controller: ReadableStreamDefaultController<string>) {
  let ws = wsMap.get(wsUrl);
  if (ws == null || ws.readyState === WebSocket.CLOSING) {
    console.log(`re-create=>${wsUrl}`, ws?.readyState);
    ws = new WebSocket(wsUrl);
    wsMap.set(wsUrl, ws);
    ws.onmessage = (msgEvent) => {
      const data = msgEvent.data;
      // console.log("ws on message", data, typeof data, wsUrl);
      if (typeof data === "string") {
        controller.enqueue(data);
      }
    };
  }
  return ws;
}

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
            ([_, value]) => value != undefined /* null undefined 都不传输*/,
          )
          .map(([key, value]) => {
            return [key, typeof value === "object" ? JSON.stringify(value) : String(value)] as [string, string];
          }),
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
  responseType?: "json" | "blob" | "text";
}
