// 全部的插件入口文件
import { registerWebPlugin } from "./registerPlugin.ts";
import { BarcodeScanner } from "./barcode-scanner/index.ts";
import { NavigationBarPluginEvents, Navigatorbar } from "./navigator-bar/index.ts"
import { StatusbarPlugin } from "./statusbar/index.ts";
import { ToastPlugin } from "./toast/index.ts";
import { TorchPlugin } from "./torch/index.ts"

// hijack Capacitor
registerWebPlugin(new Navigatorbar())
registerWebPlugin(new BarcodeScanner())
registerWebPlugin(new StatusbarPlugin())
registerWebPlugin(new ToastPlugin())
registerWebPlugin(new TorchPlugin())


export {
  NavigationBarPluginEvents,
  Navigatorbar,
  StatusbarPlugin,
  ToastPlugin,
  TorchPlugin
}
