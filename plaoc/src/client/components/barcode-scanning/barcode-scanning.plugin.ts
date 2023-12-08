import { bindThis } from "../../helper/bindThis.ts";
import { BasePlugin } from "../base/BasePlugin.ts";
import { SupportedFormat } from "./barcode-scanning.type.ts";

export interface BarcodeResult {
  data: string;
  boundingBox: Rect;
  topLeft: Point;
  topRight: Point;
  bottomLeft: Point;
  bottomRight: Point;
}
interface Rect {
  x: number;
  y: number;
  width: number;
  height: number;
}
interface Point {
  x: number;
  y: number;
}
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
  async process(formats = SupportedFormat.QR_CODE) {
    const wsUrl = await this.buildApiRequest("/process", {
      search: {
        formats,
      },
      method: "GET",
    }).url.replace("http", "ws");
    const ws = new WebSocket(wsUrl);
    ws.binaryType = "blob";

    const rotation_ab = new ArrayBuffer(4);
    const rotation_i32 = new Int32Array(rotation_ab);
    const controller = {
      setRotation(rotation: number) {
        rotation_i32[0] = rotation;
        ws.send(rotation_ab);
      },
      sendImageData(data: Uint8Array | Blob) {
        ws.send(data);
      },
      onmessage: undefined as ((message: BarcodeResult[]) => void) | undefined,
      stop() {
        ws.close();
      },
    };
    ws.onmessage = async (ev) => {
      if (controller.onmessage) {
        controller.onmessage(JSON.parse(await (ev.data as Blob).text()) as BarcodeResult[]);
      }
    };

    return controller;
  }
  /**
   * 停止扫码
   * @returns
   */
  @bindThis
  async stop() {
    return await this.fetchApi(`/stop`).boolean();
  }
}

export type DeCodeType = {
  rawValue: string;
  format: string;
};
export const barcodeScannerPlugin = new BarcodeScannerPlugin();
