import { createSignal } from "../../helper/createSignal.ts";
import { $BuildRequestInit, buildRequest } from "../../helper/request.ts";

export abstract class BasePlugin {
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

  fetchApi(url: string, init?: $BuildRequestInit) {
    return this.buildApiRequest(url, init).fetch();
  }
  buildApiRequest(pathname: string, init?: $BuildRequestWithBaseInit) {
    const url = new URL(init?.base ?? BasePlugin.url);
    url.pathname = `${this.mmid}${pathname}`;
    return buildRequest(url, init);
  }

  buildExternalApiRequest(pathname: string, init?: $BuildRequestWithBaseInit) {
    const url = new URL(init?.base ?? BasePlugin.url);
    url.pathname = pathname;
    return buildRequest(url, init);
  }

  protected createSignal = createSignal;
}

export interface $BuildRequestWithBaseInit extends $BuildRequestInit {
  base?: string;
}
