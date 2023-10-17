import { X_PLAOC_QUERY } from "../../../common/const.ts";
import { $MMID } from "../../../common/types.ts";
import { createSignal } from "../../helper/createSignal.ts";
import { $BuildRequestInit, buildRequest } from "../../helper/request.ts";

export abstract class BasePlugin {
  static internal_url: string = location?.href ?? "http://localhost";
  private static urlData = new URLSearchParams(location.search);
  static public_url = BasePlugin.getUrl(X_PLAOC_QUERY.API_PUBLIC_URL);
  static external_url = BasePlugin.getUrl(X_PLAOC_QUERY.EXTERNAL_URL);
  static internal_url_useable = false;
  /** internal_url or public_url */
  static get url() {
    if (this.internal_url_useable) {
      return this.internal_url;
    }
    return this.public_url;
  }

  constructor(readonly mmid: $MMID) {}

  fetchApi(url: string, init?: $BuildRequestInit) {
    return this.buildApiRequest(url, init).fetch();
  }
  buildApiRequest(pathname: string, init?: $BuildRequestWithBaseInit) {
    const url = new URL(init?.base ?? BasePlugin.url);
    url.pathname = `${init?.pathPrefix ?? this.mmid}${pathname}`;
    return buildRequest(url, init);
  }

  protected createSignal = createSignal;
  /**
   * 获取指定的url
   * @param urlType
   * @returns
   */
  static getUrl(urlType: X_PLAOC_QUERY) {
    const url = this.urlData.get(urlType) || localStorage.getItem("url:" + urlType);
    if (url === null) {
      throw new Error(`unconfig url: ${urlType}`);
    }
    localStorage.setItem("url:" + urlType, url);
    return url;
  }
  /**获取plaoc内置url */
  static async getBaseUrl() {
    if (typeof location === "undefined") {
      return "file:///";
    }
    const url = new URL(location.href);
    url.pathname = `/${X_PLAOC_QUERY.GET_CONFIG_URL}`;
    return await buildRequest(url).fetch().object<{ [key in X_PLAOC_QUERY]: string }>();
  }
}

export interface $BuildRequestWithBaseInit extends $BuildRequestInit {
  base?: string;
}

if (typeof HTMLElement !== "function") {
  Object.assign(globalThis, { HTMLElement: class HTMLElement {}, customElements: { define: () => {} } });
}
