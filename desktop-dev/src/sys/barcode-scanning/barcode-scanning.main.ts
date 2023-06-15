// 模拟状态栏模块-用来提供状态UI的模块

import Jimp from "npm:jimp";
import jsQR from "npm:jsqr";
 
import { NativeMicroModule } from "../../core/micro-module.native.ts";
import type { HttpServerNMM } from "../http-server/http-server.ts";
import type { Ipc } from "../../core/ipc/ipc.ts";

export class BarcodeScanningNativeUiNMM extends NativeMicroModule {
  mmid = "barcode-scanning.sys.dweb" as const;
  httpIpc: Ipc | undefined;
  httpNMM: HttpServerNMM | undefined;
  encoder = new TextEncoder();
  allocId = 0;

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
        return await Jimp.read(Buffer.from(await ipcRequest.body.u8a()))
        .then(({ bitmap }: Jimp) => {
            const result = jsQR(bitmap.data as unknown as Uint8ClampedArray, bitmap.width, bitmap.height);
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
