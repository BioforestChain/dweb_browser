import { bindThis } from "../../helper/bindThis.ts";
import { BasePlugin } from "../base/BasePlugin.ts";
import type { ShortcutOption } from "./shortcut.type.ts";

export class ShortcutPlugin extends BasePlugin {
  constructor() {
    super("shortcut.sys.dweb");
  }

  /**
   * 注册
   * @param option ShortcutOption
   * @returns boolean
   * @since 2.0.0
   */
  @bindThis
  async registry(option: ShortcutOption) {
    return await this.fetchApi(`/registry`, {
      method: "POST",
      search: {
        title: option.title,
        url: option.url,
      },
      body: option.icon,
    }).boolean();
  }
}
export const shortcutPlugin = new ShortcutPlugin();
