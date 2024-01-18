import { bindThis } from "../../helper/bindThis.ts";
import { BasePlugin } from "../base/BasePlugin.ts";

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
