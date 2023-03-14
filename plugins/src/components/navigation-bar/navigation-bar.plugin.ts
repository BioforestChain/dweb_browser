import { bindThis } from "../../helper/bindThis.ts";
import { BasePlugin } from "../basePlugin.ts";
import type { ColorParameters } from "./navigation-bar.type.ts";
// navigator-bar 插件
export class NavigatorBarPlugin extends BasePlugin {
  readonly tagName = "dweb-navigator-bar";

  constructor() {
    super("navigationbar.sys.dweb");
  }

  connectedCallback() {
    // 发起一个监听请求
    // this._addClickNavigatorbarItem()
  }

  /**
   * 显示导航栏。
   */
  @bindThis
  async show(): Promise<void> {
    await this.nativeFetch("/setVisible", {
      search: {
        visible: true,
      },
    });
  }

  /**
   * 隐藏导航栏。
   */
  @bindThis
  async hide(): Promise<void> {
    await this.nativeFetch("/setVisible", {
      search: {
        visible: false,
      },
    });
  }

  /**
   * 获取导航栏是否可见。
   */
  @bindThis
  async getVisible(): Promise<Response> {
    return await this.nativeFetch("/getVisible");
  }

  /**
   * 更改导航栏的颜色。
   *支持 alpha 十六进制数。
   * @param options
   */
  @bindThis
  async setColor(options: ColorParameters): Promise<void> {
    await this.nativeFetch("/setBackgroundColor", {
      search: {
        color: options.color,
        darkButtons: options.darkButtons,
      },
    });
  }

  /**
   * 以十六进制获取导航栏的当前颜色。
   */
  @bindThis
  async getColor(): Promise<{ color: string }> {
    const color = await this.nativeFetch("/getBackgroundColor").then((res) =>
      res.text()
    );
    return { color: color };
  }

  /**
   * 设置透明度
   * @param isTransparent
   */
  @bindThis
  async setTransparency(options: { isTransparent: boolean }): Promise<void> {
    await this.nativeFetch("/setTransparency", {
      search: {
        isTransparency: options.isTransparent,
      },
    });
  }
  /**
   * 获取导航栏是否透明度
   * @param isTransparent
   */
  @bindThis
  async getTransparency(): Promise<Response> {
    return await this.nativeFetch("/getTransparency");
  }

  /**
   * 设置导航栏是否覆盖内容
   * @param isOverlay
   */
  @bindThis
  async setOverlay(options: { isOverlay: boolean }): Promise<void> {
    await this.nativeFetch("/setOverlay", {
      search: {
        isOverlay: options.isOverlay,
      },
    });
  }
  /**
   * 获取导航栏是否覆盖内容
   * @param isOverlay
   */
  @bindThis
  async getOverlay(): Promise<Response> {
    return await this.nativeFetch("/getOverlay");
  }

  private _signalShow = this.createSignal();
  readonly onShow = this._signalShow.listen;
  private _signalHide = this.createSignal();
  readonly onHide = this._signalHide.listen;
  private _signalChange = this.createSignal();
  readonly onChange = this._signalChange.listen;
}
export const navigatorBarPlugin = new NavigatorBarPlugin();
