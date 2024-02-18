import { bindThis } from "../../helper/bindThis.ts";
import { BasePlugin } from "../base/base.plugin.ts";
import type { SplashScreenHideOptions, SplashScreenShowOptions } from "./splash-screen.type.ts";

export class SplashScreenPlugin extends BasePlugin {
  constructor() {
    super("splash-screen.nativeui.browser.dweb");
  }

  /**
   * 显示启动页
   * @param options
   */
  @bindThis
  async show(options?: SplashScreenShowOptions): Promise<boolean> {
    return await this.fetchApi(`/show`, {
      search: {
        autoHide: options?.autoHide,
        showDuration: options?.showDuration,
        fadeOutDuration: options?.fadeOutDuration,
        fadeInDuration: options?.fadeInDuration,
      },
    }).boolean();
  }

  /**
   * 隐藏启动页
   * @param options
   */
  @bindThis
  async hide(options?: SplashScreenHideOptions): Promise<boolean> {
    return await this.fetchApi(`/hide`, {
      search: {
        fadeOutDuration: options?.fadeOutDuration,
      },
    }).boolean();
  }
}
export const splashScreenPlugin = new SplashScreenPlugin();
