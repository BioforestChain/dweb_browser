import { bindThis } from "../../helper/bindThis.ts";
import { BasePlugin } from "../base/BasePlugin.ts";
// import { cameraPlugin } from "../camera/camera.plugin.ts";
// import type { ImageOptions } from "../camera/camera.type.ts";
import { SupportedFormat } from "./barcode-scanning.type.ts";

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
  async process(
    blob: Blob,
    rotation = 0,
    formats = SupportedFormat.QR_CODE
  ): Promise<string[]> {
    const value = await this.buildApiRequest("/process", {
      search: {
        rotation,
        formats,
      },
      method: "POST",
      body: blob,
      base: await BasePlugin.public_url,
    })
      .fetch()
      .object<string[]>();
    const result = Array.from(value ?? []);
    return result;
  }
  /**
   * 停止扫码
   * @returns
   */
  @bindThis
  async stop() {
    return await this.fetchApi(`/stop`).boolean();
  }

  // /**
  //  * 打开相册
  //  */
  // @bindThis
  // getPhoto(options: ImageOptions = {}) {
  //   return cameraPlugin.getPhoto(options);
  // }
}

export const barcodeScannerPlugin = new BarcodeScannerPlugin();
