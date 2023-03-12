// <reference types="https://esm.sh/v111/@types/web@0.0.96/index.d.ts" />
/// <reference lib="dom"/>
import { createSignal } from "../helper/createSignal.ts";

export class BasePlugin extends HTMLElement {
  // mmid:为对应组件的名称，proxy:为劫持对象的属性
  constructor(readonly mmid: string, readonly proxy: string) {
    super();
  }

  protected nativeFetch(
    url: RequestInfo,
    init?: RequestInit & {
      // deno-lint-ignore ban-types
      search?: string | URLSearchParams | Record<string, unknown> | {};
    }
  ): Promise<Response> {
    if (url instanceof Request) {
      return fetch(url, init);
    }
    const host = window.location.host.replace("www", "api");

    const uri = new URL(`${this.mmid}${url}`, `https://${host}`);
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
    return fetch(uri, init);
  }

  protected createSignal = createSignal;
}
