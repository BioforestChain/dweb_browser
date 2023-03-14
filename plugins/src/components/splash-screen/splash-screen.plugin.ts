import { bindThis } from "../../helper/bindThis.ts";
import { BasePlugin } from "../basePlugin.ts";
import type {
  HideOptions,
  ShowOptions,
  ISplashScreenPlugin,
} from "./splash-screen.type.ts";

export class SplashScreenPlugin
  extends BasePlugin
  implements ISplashScreenPlugin
{
  readonly tagName = "dweb-splash-screen";
  constructor(readonly mmid = "splash-screen.sys.dweb") {
    super("splash-screen.sys.dweb");
  }

  /**
   * 显示启动页
   * @param options
   */
  @bindThis
  async show(options?: ShowOptions): Promise<Response> {
    return await this.nativeFetch(`/show`, {
      search: {
        autoHide: options?.autoHide,
        showDuration: options?.showDuration,
        fadeOutDuration: options?.fadeOutDuration,
        fadeInDuration: options?.fadeInDuration,
      },
    });
  }

  /**
   * 隐藏启动页
   * @param options
   */
  @bindThis
  async hide(options?: HideOptions): Promise<Response> {
    return await this.nativeFetch(`/hide`, {
      search: {
        fadeOutDuration: options?.fadeOutDuration,
      },
    });
  }
}
export const splashScreenPlugin = new SplashScreenPlugin();
