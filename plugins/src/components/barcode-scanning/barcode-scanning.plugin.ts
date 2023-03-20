import { bindThis } from "../../helper/bindThis.ts";
import { BasePlugin } from "../base/BasePlugin.ts";
import {
  SupportedFormat,
} from "./barcode-scanning.type.ts";

export class BarcodeScannerPlugin extends BasePlugin {
  readonly tagName = "dweb-barcode-scanning";

  constructor() {
    super("barcode-scanning.sys.dweb");
  }

  /**
   *  识别二维码
   * @param blob 
   * @param rotation 
   * @param formats 
   * @returns 
   */
  @bindThis
  async process(blob: Blob, rotation = 0, formats = SupportedFormat.QR_CODE) {
    return await this.buildApiRequest("/process", {
      search: {
        rotation,
        formats,
      },
      method: "POST",
      body: blob,
      base: await BasePlugin.public_url,
    }).fetch()
  }
  /**
   * 停止扫码
   * @returns 
   */
  @bindThis
  async stop() {
    return await this.fetchApi(`/stop`)
  }

}

export const barcodeScannerPlugin = new BarcodeScannerPlugin();
