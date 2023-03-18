import { bindThis } from "../../helper/bindThis.ts";
import { BasePlugin } from "../base/BasePlugin.ts";
import type { IShowOptions } from "./toast.type.ts";
import { IToastPlugin } from "./toast.type.ts";

/**
 * 访问 toast 能力的插件
 */
export class ToastPlugin extends BasePlugin implements IToastPlugin {
  readonly tagName = "dweb-toast";

  constructor() {
    super("toast.sys.dweb");
  }

  /**
   * toast信息显示
   * @param message 消息
   * @param duration 时长 'long' | 'short'
   * @returns
   */
  @bindThis
  async show(options: IShowOptions) {
    const { text: message, duration = "long", position = "bottom" } = options;

    return await this.fetchApi(`/show`, {
      search: { message, duration, position },
    });
  }
}

export const toastPlugin = new ToastPlugin();
