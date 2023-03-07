import { BasePlugin } from '../basePlugin.ts';
export class TorchPlugin extends BasePlugin {
  constructor(readonly mmid = "torch.sys.dweb") {
    super(mmid, "Torch");
  }

  /**
   * 打开/关闭手电筒
   */
  async toggleTorch() {
    return await this.nativeFetch("/toggleTorch")
  }

  /**
   * 手电筒状态
   */
  async getTorchState() {
    return await this.nativeFetch("/torchState")
  }
}
