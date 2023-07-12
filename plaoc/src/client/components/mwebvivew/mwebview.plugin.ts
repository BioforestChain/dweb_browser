import { bindThis } from "../../helper/bindThis.ts";
import { BasePlugin } from "../base/BasePlugin.ts";
import { WebViewItem } from "./mwebview.type.ts";

export class MWebviewPlugin extends BasePlugin {
  constructor() {
    super("mwebview.browser.dweb");
  }

  /**
   * 打开一个新的webview
   * @param url
   * @returns WebViewItem
   */
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
   * @param webview_id
   * @returns boolean
   */
  @bindThis
  close(webview_id: string) {
    return this.fetchApi(`/close`, {
      search: {
        webview_id,
      },
    }).boolean();
  }

  /**
   * 激活mwebview
   * @returns boolean
   */
  @bindThis
  activate() {
    return this.fetchApi(`/activate`).boolean();
  }

  /**
   * 关闭整个应用，只会关闭前端。
   * @returns boolean
   */
  @bindThis
  closeApp() {
    return this.fetchApi(`/close/app`).boolean();
  }
}

export const mwebviewPlugin = new MWebviewPlugin();
