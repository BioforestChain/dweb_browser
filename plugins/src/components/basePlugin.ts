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

  protected buildRequest(url: URL, init?: $BuildRequestInit) {
    const search = init?.search;
    if (search) {
      let extendsSearch: URLSearchParams;
      if (search instanceof URLSearchParams) {
        extendsSearch = search;
      } else if (typeof search === "string") {
        extendsSearch = new URLSearchParams(search);
      } else {
        extendsSearch = new URLSearchParams(
          Object.entries(search).map(([key, value]) => {
            return [
              key,
              typeof value === "string" ? value : JSON.stringify(value),
            ] as [string, string];
          })
        );
      }
      extendsSearch.forEach((value, key) => {
        url.searchParams.append(key, value);
      });
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
  protected fetchApi(url: string, init?: $BuildRequestInit) {
    return this.buildApiRequest(url, init).fetch();
  }
  protected buildApiRequest(
    pathname: string,
    init?: $BuildRequestWithBaseInit
  ) {
    const url = new URL(init?.base ?? BasePlugin.internal_url);
    url.pathname = `${this.mmid}${pathname}`;
    return this.buildRequest(url, init);
  }
  protected buildInternalApiRequest(
    pathname: string,
    init?: $BuildRequestWithBaseInit
  ) {
    const url = new URL(init?.base ?? BasePlugin.internal_url);
    url.pathname = `/internal${pathname}`;
    return this.buildRequest(url, init);
  }

  protected createSignal = createSignal;
}
interface $BuildRequestInit extends RequestInit {
  search?:
    | ConstructorParameters<typeof URLSearchParams>[0]
    | Record<string, unknown>;
  base?: string;
}
interface $BuildRequestWithBaseInit extends $BuildRequestInit {
  base?: string;
}
