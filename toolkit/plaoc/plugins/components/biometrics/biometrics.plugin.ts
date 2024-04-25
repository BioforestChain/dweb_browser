import { bindThis } from "../../helper/bindThis.ts";
import { BaseResult } from "../../util/response.ts";
import { BasePlugin } from "../base/base.plugin.ts";
import { BioetricsCheckResult } from "./biometrics.type.ts";

export class BiometricsPlugin extends BasePlugin {
  constructor() {
    super("biometrics.sys.dweb");
  }

  /**
   * 检查是否支持生物识别
   * (ios始终返回支持人脸识别)
   * @returns BioetricsCheckResult
   */
  @bindThis
  async check(): Promise<BioetricsCheckResult> {
   const res = await this.fetchApi("/check");
    const data = await res.text();
    if (data == "true") {
      return 0;
    }
    if (data == "false") {
      return -1;
    }
    return +data;
  }
  /**
   * 生物识别
   * @returns
   */
  @bindThis
  async biometrics(): Promise<BaseResult> {
    return await this.fetchApi("/biometrics").object();
  }
}

export const biometricsPlugin = new BiometricsPlugin();
