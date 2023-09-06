import { X_PLAOC_QUERY } from "../../../server/const.ts";
import { bindThis } from "../../helper/bindThis.ts";
import { BasePlugin } from "../base/BasePlugin.ts";
import { $CommonCommands } from "./config.type.ts";

export class ConfigPlugin extends BasePlugin {
  constructor() {
    super("config.std.dweb");
    if (typeof location === "object") {
      this.initConfig();
    }
  }
  async initConfig() {
    const internalUrl = await BasePlugin.getInternalUrl(X_PLAOC_QUERY.API_INTERNAL_URL)
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
   * 设置一些配置指令
   * @param cmd 
   * @returns 
   */
  @bindThis
  async command(cmd: keyof $CommonCommands) {
    return await this.fetchApi(`/${cmd}`,{
      search:{
        local:"locales"
      }
    }).object<$CommonCommands[typeof cmd]>();
  }

}
export const configPlugin = new ConfigPlugin();
