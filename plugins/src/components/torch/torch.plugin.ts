import { bindThis } from "../../helper/bindThis.ts";
import { BasePlugin } from "../basePlugin.ts";
/**
 * TODO 挪到 nativeui 中控制
 */
export class TorchPlugin extends BasePlugin {
  readonly tagName = "dweb-torch";
  constructor() {
    super("torch.sys.dweb");
  }

  /**
   * 打开/关闭手电筒
   */
  @bindThis
  async toggleTorch() {
    return await this.nativeFetch("/toggleTorch");
  }

  /**
   * 手电筒状态
   */
  @bindThis
  async getTorchState() {
    return await this.nativeFetch("/torchState");
  }
}

export const torchPlugin = new TorchPlugin();
