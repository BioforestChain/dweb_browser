/// <reference lib="dom" />
import { encodeUri } from "../helper/binary.ts";
import { createSignal } from "../helper/createSignal.ts";

export class BasePlugin extends HTMLElement {
  // mmid:为对应组件的名称，proxy:为劫持对象的属性
  constructor(readonly mmid: string, readonly proxy: string) {
    super();
  }

  protected nativeFetch(
    url: RequestInfo,
    init?: RequestInit & {
      search?: string | URLSearchParams | Record<string, unknown> | {};
    }
  ): Promise<Response> {
    if (url instanceof Request) {
      return fetch(url, init);
    }
    const host = window.location.host.replace("www", "api");

    const uri = new URL(`${this.mmid}${url}`, host);
    const search = init?.search;
    if (search) {
      if (search instanceof URLSearchParams) {
        uri.search = search.toString();
      } else if (typeof search === "string") {
        uri.search = search;
      } else {
        uri.search = new URLSearchParams(
          Object.entries(search).map(([key, value]) => {
            return [
              key,
              typeof value === "string" ? value : JSON.stringify(value),
            ] as [string, string];
          })
        ).toString();
      }
    }
    console.log("nativeFetch=>", uri);
    return fetch(uri, init);
  }

  protected createSignal = createSignal;
}
