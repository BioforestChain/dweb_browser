// <reference types="https://esm.sh/v111/@types/web@0.0.96/index.d.ts" />
/// <reference lib="dom"/>
import { $makeExtends } from "../helper/$makeExtends.ts";
import { fetchExtends } from "../helper/$makeFetchExtends.ts";
import { createSignal } from "../helper/createSignal.ts";

export abstract class BasePlugin {
  abstract tagName: string;

  // mmid:为对应组件的名称，proxy:为劫持对象的属性
  constructor(readonly mmid: string) {}

  protected static internal_url: string =
    globalThis.location?.href ?? "http://localhost";
  protected static public_url: Promise<string> | string = "";

  protected buildRequest(url: URL, init?: $NativeFetchInit) {
    const search = init?.search;
    if (search) {
      if (search instanceof URLSearchParams) {
        url.search = search.toString();
      } else if (typeof search === "string") {
        url.search = search;
      } else {
        url.search = new URLSearchParams(
          Object.entries(search).map(([key, value]) => {
            return [
              key,
              typeof value === "string" ? value : JSON.stringify(value),
            ] as [string, string];
          })
        ).toString();
      }
    }
    return Object.assign(
      new Request(url, init),
      $makeExtends<Request>()({
        fetch() {
          return Object.assign(fetch(this), fetchExtends);
        },
      })
    );
  }
  protected fetchApi(url: string, init?: $NativeFetchInit) {
    return this.buildApiRequest(url, init).fetch();
  }
  protected buildApiRequest(url: string, init?: $NativeFetchInit) {
    return this.buildRequest(
      new URL(`${this.mmid}${url}`, BasePlugin.internal_url),
      init
    );
  }
  protected buildInternalRequest(url: string, init?: $NativeFetchInit) {
    return this.buildRequest(
      new URL(`/internal${url}`, BasePlugin.internal_url),
      init
    );
  }

  protected createSignal = createSignal;
}
type $NativeFetchInit = RequestInit & {
  // deno-lint-ignore ban-types
  search?: string | URLSearchParams | Record<string, unknown> | {};
};
