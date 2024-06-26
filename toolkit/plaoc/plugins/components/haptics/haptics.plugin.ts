import "@dweb-browser/polyfill";
import { bindThis } from "../../helper/bindThis.ts";
import type { $BuildRequestInit } from "../../helper/request.ts";
import { BasePlugin } from "../base/base.plugin.ts";
import type { ImpactOptions, NotificationOptions, VibrateOptions } from "./haptics.type.ts";

export class HapticsPlugin extends BasePlugin {
  private static isIOSDWebBrowser: boolean = typeof self?.webkit?.messageHandlers?.haptics?.postMessage === "function";

  constructor() {
    super(HapticsPlugin.isIOSDWebBrowser ? "haptics.browser.dweb" : "haptics.sys.dweb");
  }
  /**
   * 触碰轻质量物体
   *@Platform android/ios only
   *  */
  @bindThis
  async impactLight(options: ImpactOptions) {
    return await this.fetchApi("/impactLight", {
      search: {
        style: options.style,
      },
    });
  }

  /**
   * 振动通知
   * @Platform android/ios only
   */
  @bindThis
  async notification(options: NotificationOptions) {
    return await this.fetchApi("/notification", {
      search: {
        style: options.type,
      },
    });
  }

  /**
   * 单击手势的反馈振动
   * @Platform android/ios only
   *  */
  @bindThis
  async vibrateClick() {
    return await this.fetchApi("/vibrateClick");
  }

  /** 禁用手势的反馈振动
   * 与 headShak 特效一致, 详见 ripple-button.animation.ts
   * headShak 是一段抖动特效, 前面抖动增强然后衰退
   * 这里只针对抖动增强阶段提供同步的振动反馈
   * @Platform android/ios only
   */
  @bindThis
  async vibrateDisabled() {
    return await this.fetchApi("/vibrateDisabled");
  }

  /**
   * 双击手势的反馈振动
   * @Platform android/ios only
   */
  @bindThis
  async vibrateDoubleClick() {
    return await this.fetchApi("/vibrateDoubleClick");
  }
  /**
   * 重击手势的反馈振动, 比如菜单键/惨案/3Dtouch
   * @Platform android/ios only
   *  */
  @bindThis
  async vibrateHeavyClick() {
    return await this.fetchApi("/vibrateHeavyClick");
  }

  /**
   * 滴答
   * @Platform android/ios only
   *  */
  @bindThis
  async vibrateTick() {
    return await this.fetchApi("/vibrateTick");
  }
  /**
   * 自定义效果
   * @param VibrateOptions
   * @Platform android/ios only
   */
  @bindThis
  async vibrate(options: VibrateOptions) {
    const duration = options?.duration || [300];
    return await this.fetchApi("/customize", {
      search: {
        duration: JSON.stringify(duration),
      },
    });
  }

  override fetchApi(url: string, init?: $BuildRequestInit) {
    if (HapticsPlugin.isIOSDWebBrowser) {
      // deno-lint-ignore no-explicit-any
      return (globalThis as any).webkit.messageHandlers.haptics.postMessage({
        path: url,
        search: init?.search,
      });
    }

    return this.buildApiRequest(url, init).fetch();
  }
}

export const hapticsPlugin = new HapticsPlugin();
