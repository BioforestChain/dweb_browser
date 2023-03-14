// <reference types="https://esm.sh/v111/@types/web@0.0.96/index.d.ts" />
/// <reference lib="dom"/>
import { createEvt } from "../helper/createEvt.ts";

export abstract class BasePlugin {
  abstract tagName: string;

  // mmid:为对应组件的名称，proxy:为劫持对象的属性
  constructor(readonly mmid: string) { }

  protected static internal_url: string =
    globalThis.location?.href ?? "http://localhost";
  protected static public_url: Promise<string> | string = "";

  protected nativeFetch(url: string, init?: $NativeFetchInit) {
    return Object.assign(this._nativeFetch(url, init), fetchBaseExtends);
  }
  protected async _nativeFetch(url: string, init?: $NativeFetchInit) {
    const uri = new URL(`${this.mmid}${url}`, await BasePlugin.internal_url);
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

  protected createSignal = createEvt;
}
type $NativeFetchInit = RequestInit & {
  // deno-lint-ignore ban-types
  search?: string | URLSearchParams | Record<string, unknown> | {};
};
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
      // deno-lint-ignore no-debugger
      debugger;
      throw err;
    }
  },
});
