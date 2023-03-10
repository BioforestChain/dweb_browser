// 全部的插件入口文件
// import { registerWebPlugin } from "./registerPlugin.ts";
// import { BarcodeScanner } from "./barcode-scanner/index.ts";
// import { Navigatorbar } from "./navigator-bar/index.ts"
// import { StatusbarPlugin } from "./statusbar/index.ts";
// import { ToastPlugin } from "./toast/index.ts";
// import { TorchPlugin } from "./torch/index.ts"

// hijack Capacitor
// registerWebPlugin(new Navigatorbar())
// registerWebPlugin(new BarcodeScanner())
// registerWebPlugin(new StatusbarPlugin())
// registerWebPlugin(new ToastPlugin())
// registerWebPlugin(new TorchPlugin())


export * from "./barcode-scanner/index.ts"
export * from "./navigator-bar/index.ts"
export * from "./toast/index.ts";
export * from "./torch/index.ts"
export * from "./statusbar/index.ts";
export * from "./splash-screen/index.ts"
export * from "./share/index.ts"
