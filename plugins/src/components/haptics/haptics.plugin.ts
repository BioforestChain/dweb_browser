import { bindThis } from "../../helper/bindThis.ts";
import { BasePlugin } from "../base/BasePlugin.ts";
import type { ImpactOptions, NotificationOptions, VibrateOptions } from "./haptics.type.ts";

export class HapticsPlugin extends BasePlugin {
  tagName = "dweb-haptics";
  constructor() {
    super("haptics.sys.dweb");
  }
  /** 触碰轻质量物体 */
  @bindThis
  async impactLight(options: ImpactOptions) {
    return await this.fetchApi("/impactLight", {
      search: {
        style: options.style
      }
    });
  }

  /** 振动通知 */
  @bindThis
  async notification(options: NotificationOptions) {
    return await this.fetchApi("/notification", {
      search: {
        style: options.type
      }
    });
  }

  /** 单击手势的反馈振动 */
  @bindThis
  async vibrateClick() {
    await this.fetchApi("/vibrateClick");
  }

  /** 禁用手势的反馈振动,
   * 与 headShak 特效一致, 详见 ripple-button.animation.ts
   * headShak 是一段抖动特效, 前面抖动增强然后衰退
   * 这里只针对抖动增强阶段提供同步的振动反馈
   */
  @bindThis
  async vibrateDisabled() {
    await this.fetchApi("/vibrateDisabled");
  }

  /** 双击手势的反馈振动 */
  @bindThis
  async vibrateDoubleClick() {
    await this.fetchApi("/vibrateDoubleClick");
  }
  /** 重击手势的反馈振动, 比如菜单键/惨案/3Dtouch */
  @bindThis
  async vibrateHeavyClick() {
    await this.fetchApi("/vibrateHeavyClick");
  }

  /** 滴答 */
  @bindThis
  async vibrateTick() {
    await this.fetchApi("/vibrateTick");
  }
  /**
   * 自定义效果
   * @param VibrateOptions 
   */
  @bindThis
  async vibrate(options: VibrateOptions) {
    const duration = options?.duration || 300;
    await this.fetchApi("/customize", {
      search: {
        duration: duration
      }
    })
  }
}

export const hapticsPlugin = new HapticsPlugin();
