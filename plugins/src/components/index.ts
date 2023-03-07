// 全部的插件入口文件
import { registerWebPlugin } from "./registerPlugin.ts";
import { BarcodeScanner } from "./barcode-scanner/index.ts";
import { NavigationBarPluginEvents, Navigatorbar } from "./navigator-bar/index.ts"
import "./statusbar/statusbar.plugin.ts";
import "./toast/toast.plugin.ts";


registerWebPlugin(new Navigatorbar())
registerWebPlugin(new BarcodeScanner())


export {
  NavigationBarPluginEvents,
  Navigatorbar,
}
