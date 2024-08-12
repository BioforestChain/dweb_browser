import { bindThis } from "../../helper/bindThis.ts";
import { BasePlugin } from "../base/base.plugin.ts";

export class ConfigPlugin extends BasePlugin {
  constructor() {
    super("config.sys.dweb");
  }

  /**
   * 设置语言
   * @param lang 语言
   * @param isReload 是否重新加载
   * @returns boolean
   * @since 1.0.0
   */
  @bindThis
  async setLang(lang: string, isReload = true) {
    const res = await this.fetchApi(`/setLang`, {
      search: {
        lang: lang,
      },
      base: location.href,
    }).boolean();
    if (res && isReload) {
      location.reload();
    }
    return res;
  }
  /**
   * 获取当前语言
   * @returns string
   * @since 1.0.0
   */
  @bindThis
  getLang() {
    return this.fetchApi("/getLang", { base: location.href }).text();
  }
}
export const configPlugin = new ConfigPlugin();
