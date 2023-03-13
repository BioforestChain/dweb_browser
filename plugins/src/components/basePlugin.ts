// <reference types="https://esm.sh/v111/@types/web@0.0.96/index.d.ts" />
/// <reference lib="dom"/>
import { createSignal } from "../helper/createSignal.ts";

export class BasePlugin extends HTMLElement {
  // mmid:为对应组件的名称，proxy:为劫持对象的属性
  constructor(readonly mmid: string, readonly proxy: string) {
    super();
  }

  protected nativeFetch(
    url: string,
    init?: RequestInit & {
      // deno-lint-ignore ban-types
      search?: string | URLSearchParams | Record<string, unknown> | {};
    }
  ) {
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
    return Object.assign(fetch(uri, init), fetchBaseExtends);
  }

  protected createSignal = createSignal;
}

const $makeFetchExtends = <M extends unknown = unknown>(
  exts: $FetchExtends<M>
) => {
  return exts;
};
type $FetchExtends<E> = E & ThisType<Promise<Response> & E>; // Type of 'this' in methods is D & M

export const fetchBaseExtends = $makeFetchExtends({
  async number() {
    const text = await this.text();
    return +text;
  },
  async ok() {
    const response = await this;
    if (response.status >= 400) {
      throw response.statusText || (await response.text());
    } else {
      return response;
    }
  },
  async text() {
    const ok = await this.ok();
    return ok.text();
  },
  async binary() {
    const ok = await this.ok();
    return ok.arrayBuffer();
  },
  async boolean() {
    const text = await this.text();
    return text === "true"; // JSON.stringify(true)
  },
  async object<T>() {
    const ok = await this.ok();
    try {
      return (await ok.json()) as T;
    } catch (err) {
      debugger;
      throw err;
    }
  },
});
