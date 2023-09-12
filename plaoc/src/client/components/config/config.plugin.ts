import { X_PLAOC_QUERY } from "../../../server/const.ts";
import { bindThis } from "../../helper/bindThis.ts";
import { BasePlugin } from "../base/BasePlugin.ts";
import { dwebServiceWorker } from "../index.ts";

export class ConfigPlugin extends BasePlugin {
  constructor() {
    super("config.sys.dweb");
    if (typeof location === "object") {
      this.initConfig();
    }
  }
  async initConfig() {
    const internalUrl = await BasePlugin.getInternalUrl(X_PLAOC_QUERY.API_INTERNAL_URL);
    internalUrl && this.setInternalUrl(internalUrl);
  }

  getInternalUrl() {
    return BasePlugin.internal_url;
  }
  setInternalUrl(url: string) {
    try {
      return (BasePlugin.internal_url = url);
    } finally {
      // this.init_public_url();
    }
  }
  get public_url() {
    return BasePlugin.public_url;
  }

  /**
   * 设置语言
   * @param lang 语言
   * @param isReload 是否重新加载
   * @returns 
   */
  @bindThis
  async setLang(lang: string,isReload = true) {
    const res = await this.fetchApi(`/setLang`, {
      base:location.href,
      search: {
        lang: lang,
      },
    }).boolean();
    if(res && isReload) {
      dwebServiceWorker.restart()
    }
    return res
  }

  @bindThis
  async getLang() {
    return this.fetchApi("/getLang",{base:location.href}).text();
  }
}
export const configPlugin = new ConfigPlugin();
