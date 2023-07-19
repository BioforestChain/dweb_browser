import { bindThis } from "../../helper/bindThis.ts";
import { BasePlugin } from "../base/BasePlugin.ts";
import { SupportedFormat } from "./barcode-scanning.type.ts";

export class BarcodeScannerPlugin extends BasePlugin {
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
    // const userAgent = navigator.userAgent.toLowerCase();
    // if (userAgent.indexOf(" electron/") > -1 && "BarcodeDetector" in window) {
    //   // Electron-specific code
    //   // deno-lint-ignore no-explicit-any
    //   const barcodeDetector = new (window as any).BarcodeDetector({
    //     formats: ["qr_code", "code_39", "codabar", "ean_13"],
    //   });
    //   const po = new PromiseOut<string[]>();
    //   const img = new Image();
    //   img.src = URL.createObjectURL(blob);
    //   img.onload = function () {
    //     // 在图像加载完成后执行以下步骤
    //     const canvas = document.createElement("canvas");
    //     const ctx = canvas.getContext("2d")!;
    //     canvas.width = img.width;
    //     canvas.height = img.height;
    //     ctx.drawImage(img, 0, 0);
    //     const imageData = ctx.getImageData(0, 0, img.width, img.height);
    //     barcodeDetector
    //       .detect(imageData)
    //       .then((barcodes: DeCodeType[]) => {
    //         const data = barcodes.map((barcode) => {
    //           return barcode.rawValue;
    //         });
    //         po.resolve(data);
    //         return barcodes;
    //       })
    //       .catch((err: Error) => {
    //         console.log(err);
    //         po.reject(err);
    //       });
    //   };
    //   return await po.promise;
    // }
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

  @bindThis
  async getSupportedFormats() {
    return (await this.fetchApi("/get_supported_formats")).json();
  }
}

export type DeCodeType = {
  rawValue: string;
  format: string;
};
export const barcodeScannerPlugin = new BarcodeScannerPlugin();
