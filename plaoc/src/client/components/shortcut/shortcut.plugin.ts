import { encode } from "cbor-x";
import { bindThis } from "../../helper/bindThis.ts";
import { BasePlugin } from "../base/base.plugin.ts";
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
   * @Platform android/ios only
   */
  @bindThis
   registry(option: ShortcutOption) {
    const body = encode({
      title: option.title,
      data: option.data,
      icon: option.icon,
    });
    return this.fetchApi(`/registry`, {
      method: "POST",
      body: body,
      headers: {
        "Content-Type": "application/cbor",
        "Content-Length": body.length,
      },
    }).boolean();
  }
}
export const shortcutPlugin = new ShortcutPlugin();
