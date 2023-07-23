/// about:blank

import { JsonlinesStream } from "../../../../../helper/JsonlinesStream.ts";
import { streamRead } from "../../../../../helper/readableStreamHelper.ts";

/// about:newtab
const BASE_URL =
  location.protocol === "about:" || location.protocol === "chrome:"
    ? new URL(`http://browser.dweb.localhost/${location.href.replace(new RegExp(location.protocol + "/+"), "")}`)
    : new URL(location.href);
const apiUrl = new URL(BASE_URL.searchParams.get("api-base") ?? BASE_URL);
/**
 * 默认网关是自己
 */
const baseMmid = apiUrl.searchParams.get("mmid") ?? "desktop.browser.dweb";
export const nativeFetch = (pathname: string, init?: $BuildRequestInit) => {
  return fetch(...buildApiRequestArgs(pathname, init));
};

export const nativeFetchStream = <T>(
  pathname: string,
  init?: $BuildRequestInit,
  options?: { signal?: AbortSignal }
) => {
  const [url] = buildApiRequestArgs(pathname, init);
  const wsUrl = url.href.replace("http", "ws");
  const ws = new WebSocket(wsUrl);
  ws.binaryType = "arraybuffer";

  return streamRead(
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
    })
      .pipeThrough(new TextDecoderStream())
      .pipeThrough(new JsonlinesStream<T>()),
    options
  );
};

export function buildApiRequestArgs(pathname: string, init?: $BuildRequestInit) {
  const mmid = init?.mmid ?? baseMmid;
  const url = new URL(`api/${mmid}${pathname}`, apiUrl);
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
