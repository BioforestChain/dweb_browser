import { bindThis } from "../../helper/bindThis.ts";
import { BasePlugin } from "../base/BasePlugin.ts";
import { BioetricsResult } from "./biometrics.type.ts";

export class BiometricsPlugin extends BasePlugin {
  constructor() {
    super("biometrics.sys.dweb");
  }

  /**
   * 检查是否支持生物识别
   * @returns boolean
   */
  @bindThis
  async check(): Promise<boolean> {
    return await this.fetchApi("/check").boolean();
  }
  /**
   * 生物识别
   * @returns
   */
  @bindThis
  async biometrics(): Promise<BioetricsResult> {
    return await this.fetchApi("/biometrics").object();
  }
}

export const biometricsPlugin = new BiometricsPlugin();
