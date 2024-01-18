import { bindThis } from "../../helper/bindThis.ts";
import { BasePlugin } from "../base/BasePlugin.ts";

/**
 * 文件标准插件
 */
export class FilePlugin extends BasePlugin {
  constructor() {
    super("file.std.dweb");
  }

  @bindThis
  async open() {
    return await this.fetchApi("/open", {}).text();
  }
}

export const filePlugin = new FilePlugin();
