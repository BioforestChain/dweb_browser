// 模拟状态栏模块-用来提供状态UI的模块
import { NativeMicroModule } from "../../core/micro-module.native.ts";

export class BarcodeScanningNMM extends NativeMicroModule {
  mmid = "barcode-scanning.sys.dweb" as const;
  // barcodeDetector = new BarcodeDetectorDelegate([
  //   Html5QrcodeSupportedFormats.QR_CODE,
  //   Html5QrcodeSupportedFormats.CODE_39,
  //   Html5QrcodeSupportedFormats.CODABAR,
  //   Html5QrcodeSupportedFormats.EAN_13,
  // ]);
  _bootstrap = () => {
    this.registerCommonIpcOnMessageHandler({
      method: "POST",
      pathname: "/process",
      matchMode: "full",
      input: {},
      output: "object",
      handler: async (_args, _client_ipc, ipcRequest) => {},
    });
    this.registerCommonIpcOnMessageHandler({
      method: "GET",
      pathname: "/getSupportedFormats",
      matchMode: "full",
      input: {},
      output: "boolean",
      handler: () => {
        return true;
      },
    });
  };
  _shutdown() {}
}
