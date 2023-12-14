import { PromiseOut } from "../../helper/PromiseOut.ts";
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
  async createProcesser(formats = SupportedFormat.QR_CODE) {
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
    const locks: PromiseOut<BarcodeResult[]>[] = [];
    const controller = {
      setRotation(rotation: number) {
        rotation_i32[0] = rotation;
        ws.send(rotation_ab);
      },
      process(data: Uint8Array | Blob) {
        const task = new PromiseOut<BarcodeResult[]>();
        locks.push(task);
        ws.send(data);
        return task.promise;
      },
      stop() {
        ws.close();
        for (const lock of locks) {
          lock.reject("stop");
        }
        locks.length = 0;
      },
    };
    ws.onmessage = async (ev) => {
      const lock = locks.shift();
      if (lock) {
        lock.resolve(JSON.parse(await (ev.data as Blob).text()));
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
