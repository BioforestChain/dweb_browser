// 模拟状态栏模块-用来提供状态UI的模块

import Jimp from "jimp";
import jsQR from "jsqr";
import { Buffer } from "node:buffer";
import { NativeMicroModule } from "../../core/micro-module.native.ts";

export class BarcodeScanningNativeUiNMM extends NativeMicroModule {
  mmid = "barcode-scanning.sys.dweb" as const;

  _bootstrap = () => {
    console.always(`[${this.mmid} _bootstrap]`);
    let isStop = false;
    this.registerCommonIpcOnMessageHandler({
      method: "POST",
      pathname: "/process",
      matchMode: "full",
      input: {},
      output: "object",
      handler: async (_args, _client_ipc, ipcRequest) => {
        // 直接解析二维码
        return await Jimp.read(Buffer.from(await ipcRequest.body.u8a())).then(
          ({ bitmap }: Jimp) => {
            const result = jsQR(
              bitmap.data as unknown as Uint8ClampedArray,
              bitmap.width,
              bitmap.height
            );
            return result === null ? [] : [result.data];
          }
        );
      },
    });

    this.registerCommonIpcOnMessageHandler({
      method: "GET",
      pathname: "/stop",
      matchMode: "full",
      input: {},
      output: "boolean",
      handler: (_args, _client_ipc, _ipcRequest) => {
        // 停止及解析
        isStop = true;
        return true;
      },
    });
  };

  _shutdown() {}
}
