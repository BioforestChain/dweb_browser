import { bindThis } from "../../helper/bindThis.ts";
import { BasePlugin } from "../base/BasePlugin.ts";
import { WebViewItem } from "./mwebview.type.ts";

export class WebviewPlugin extends BasePlugin {
  constructor() {
    super("mwebview.browser.dweb");
  }

  @bindThis
  open(url: string) {
    return this.fetchApi(`/open`, {
      search: {
        url,
      },
    }).object<WebViewItem>();
  }

  /**
   * 销毁指定的 webview
   * @param host
   * @returns
   */
  @bindThis
  close(webview_id: string) {
    return this.fetchApi(`/close`, {
      search: {
        webview_id,
      },
    }).boolean();
  }

  @bindThis
  activate() {
    return this.fetchApi(`/activate`).boolean();
  }

  @bindThis
  closeApp() {
    return this.fetchApi(`/close/app`).boolean();
  }
}

export const webviewPlugin = new WebviewPlugin();
