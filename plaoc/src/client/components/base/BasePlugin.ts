import { X_PLAOC_QUERY } from "../../../common/const.ts";
import { $MMID } from "../../../common/types.ts";
import { createSignal } from "../../helper/createSignal.ts";
import { $BuildRequestInit, buildRequest } from "../../helper/request.ts";

export abstract class BasePlugin {
  static internal_url: string = location?.href ?? "http://localhost";
  private static urlData: Promise<"file:///" | { [key in X_PLAOC_QUERY]: string }> = BasePlugin.getBaseUrl();
  static public_url: Promise<string> | string = BasePlugin.getInternalUrl(X_PLAOC_QUERY.API_PUBLIC_URL);
  static external_url = BasePlugin.getInternalUrl(X_PLAOC_QUERY.EXTERNAL_URL);
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
  static async getInternalUrl(urlType: X_PLAOC_QUERY): Promise<string> {
    const data = await this.urlData
    if (data === "file:///") {
      return "file:///";
    }
    if (data) {
      return data[urlType];
    }
    return data[urlType];
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
