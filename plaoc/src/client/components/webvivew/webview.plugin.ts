import { bindThis } from "../../helper/bindThis.ts";
import { BasePlugin } from "../base/BasePlugin.ts";

export class WebviewPlugin extends BasePlugin {
  constructor() {
    super("mwebview.browser.dweb");
  }

  @bindThis
  async open(url: string) {
    const res = await (
      await this.fetchApi(`/open`, {
        search: {
          url,
        },
      })
    ).json();

    return res;
  }

  /**
   * 销毁指定的 webview
   * @param host
   * @returns
   */
  @bindThis
  async close(host: string) {
    const res = await (
      await this.fetchApi(`/close`, {
        search: {
          host,
        },
      })
    ).json();

    return res;
  }

  @bindThis
  async activate() {
    const res = await (await this.fetchApi(`/activate`)).json();

    return res;
  }

  @bindThis
  async closeWindow() {
    const res = await (await this.fetchApi(`/close/window`)).json();
    return res;
  }
}

export const webviewPlugin = new WebviewPlugin();
