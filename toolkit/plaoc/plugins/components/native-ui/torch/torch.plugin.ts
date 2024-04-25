import { bindThis } from "../../../helper/bindThis.ts";
import { BasePlugin } from "../../base/base.plugin.ts";
/**
 * TODO 挪到 nativeui 中控制
 */
export class TorchPlugin extends BasePlugin {
  constructor() {
    super("torch.nativeui.browser.dweb");
  }

  /**
   * 打开/关闭手电筒
   * @Platform android/ios only
   */
  @bindThis
  async toggleTorch() {
    return await this.fetchApi("/toggleTorch").boolean();
  }

  /**
   * 手电筒状态
   * @Platform android/ios only
   */
  @bindThis
  async getTorchState(): Promise<boolean> {
    return await this.fetchApi("/torchState").boolean();
  }
}

export const torchPlugin = new TorchPlugin();
