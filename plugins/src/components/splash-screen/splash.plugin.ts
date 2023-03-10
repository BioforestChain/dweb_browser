import { BasePlugin } from "../basePlugin.ts";
import { HideOptions, ShowOptions, ISplashScreenPlugin } from "./splash.type.ts";

export class SplashScreenPlugin extends BasePlugin implements ISplashScreenPlugin {

  constructor(readonly mmid = "splash.sys.dweb") {
    super(mmid, "Splash")
  }

  /**
   * 显示启动页
   * @param options
   */
  async show(options?: ShowOptions): Promise<Response> {
    return await this.nativeFetch(`/show?options=${JSON.stringify(options)}`);
  }

  /**
   * 隐藏启动页
   * @param options
   */
  async hide(options?: HideOptions): Promise<Response> {
    return await this.nativeFetch(`/hide?options=${JSON.stringify(options)}`);
  }
}
