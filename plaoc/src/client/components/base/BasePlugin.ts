// <reference types="https://esm.sh/v111/@types/web@0.0.96/index.d.ts" />
/// <reference lib="dom"/>
import { $makeExtends } from "../../helper/$makeExtends.ts";
import { fetchExtends } from "../../helper/$makeFetchExtends.ts";
import { createSignal } from "../../helper/createSignal.ts";

export abstract class BasePlugin {
  abstract tagName: string;
  static internal_url: string = globalThis.location?.href ?? "http://localhost";
  static public_url: Promise<string> | string = "";
  static internal_url_useable?: boolean;
  /** internal_url or public_url */
  static get url() {
    if (
      typeof this.public_url === "string" &&
      this.public_url !== "" &&
      /// 如果不能明确知道 internal_url 可用，则使用 public_url
      this.internal_url_useable !== true
    ) {
      return this.public_url;
    }
    return this.internal_url;
  }

  constructor(readonly mmid: string) {}

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
          Object.entries(search)
            .filter(
              ([_, value]) => value != undefined /* null undefined 都不传输*/
            )
            .map(([key, value]) => {
              return [
                key,
                typeof value === "object"
                  ? JSON.stringify(value)
                  : String(value),
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
  fetchApi(url: string, init?: $BuildRequestInit) {
    return this.buildApiRequest(url, init).fetch();
  }
  buildApiRequest(pathname: string, init?: $BuildRequestWithBaseInit) {
    const url = new URL(init?.base ?? BasePlugin.url);
    url.pathname = `${this.mmid}${pathname}`;
    return this.buildRequest(url, init);
  }
  buildInternalApiRequest(pathname: string, init?: $BuildRequestWithBaseInit) {
    const url = new URL(init?.base ?? BasePlugin.url);
    url.pathname = `/internal${pathname}`;
    return this.buildRequest(url, init);
  }

  buildExternalApiRequest(pathname: string, init?: $BuildRequestWithBaseInit) {
    const url = new URL(init?.base ?? BasePlugin.url);
    url.pathname = pathname;
    return this.buildRequest(url, init);
  }

  protected createSignal = createSignal;
}
interface $BuildRequestInit extends RequestInit {
  search?:
    | ConstructorParameters<typeof URLSearchParams>[0]
    // deno-lint-ignore no-explicit-any
    | Record<string, any>;
  base?: string;
}
export interface $BuildRequestWithBaseInit extends $BuildRequestInit {
  base?: string;
}
