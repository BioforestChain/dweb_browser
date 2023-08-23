/// <reference lib="dom"/>
import { webcrypto } from "node:crypto";
import process from "node:process";
import { DeskNMM } from "./browser/desk/desk.nmm.ts";
import { JmmNMM } from "./browser/jmm/jmm.ts";
import { JsProcessNMM } from "./browser/js-process/js-process.ts";
import { MultiWebviewNMM } from "./browser/multi-webview/multi-webview.nmm.ts";
import { WebBrowserNMM } from "./browser/web/web.nmm.ts";
import { setFilter } from "./helper/devtools.ts";
import { BluetoothNMM } from "./std/bluetooth/bluetooth.main.ts";
import { HttpServerNMM } from "./std/http/http.nmm.ts";
import { BarcodeScanningNMM } from "./sys/barcode-scanning/barcode-scanning.main.ts";
import { BootNMM } from "./sys/boot/boot.ts";
import { DeviceNMM } from "./sys/device/device.main.ts";
import { DnsNMM } from "./sys/dns/dns.ts";
import "./sys/dns/localeFileFetch.ts";

/**
 * 设置 debugger 过滤条件
 * 预设的值
 * "micro-module/native"
 * "http"
 * "http/dweb-server",
 * 'http/port-listener'
 * "mm"
 * "jmm"
 * "jsmm"
 * "mwebview"
 * "dns"
 * "browser"
 * "error"
 *
 * "jsProcess"
 *
 * "biometrices"
 *
 *
 * "sender/init"
 * "sender/pulling"
 * "sender/read"
 * "sender/end"
 * "sender/pull-end"
 * "sender/use-by"
 *
 * "receiver/data"
 * "receiver/end"
 * "receiver/pull"
 * "receiver/start"
 * "maphelper"
 */
// 设置 console 过滤条件
setFilter(["error", "browser", "mwebveiw", ""]);

export const dns = new DnsNMM();
dns.install(new MultiWebviewNMM());
dns.install(new JsProcessNMM());
dns.install(new HttpServerNMM());
dns.install(new DeviceNMM());
const webBrowser = new WebBrowserNMM();
dns.install(webBrowser);
dns.install(new BarcodeScanningNMM());
// dns.install(new BiometricsNMM());
dns.install(new BluetoothNMM());
const jmm = new JmmNMM();
dns.install(jmm);
const desk = new DeskNMM();
dns.install(desk);

const custom_boot = process.argv.find((arg) => arg.startsWith("--boot="))?.slice("--boot=".length) as "*.dweb";

dns.install(
  new BootNMM([
    /// 一定要直接启动jmm，这样js应用才会被注册安装
    jmm.mmid,
    /// 启动自定义模块，或者桌面模块
    custom_boot ?? desk.mmid,
  ])
);

Object.assign(globalThis, { dns: dns });
process.on("unhandledRejection", (error: Error) => {
  console.error("on unhandledRejection=>", error);
});

if (typeof crypto === "undefined") {
  Object.assign(globalThis, { crypto: webcrypto });
}

const app = Electron.app;
const menu = Electron.Menu;
const Tray = Electron.Tray;
let appIcon = null;
app.whenReady().then(() => {
  appIcon = new Tray("logo.png");
  const contextMenu = menu.buildFromTemplate([
    {
      label: "quit",
      type: "normal",
      click: () => {
        app.quit();
      },
    },
  ]);

  // 对于 Linux 再次调用此命令，因为我们修改了上下文菜单
  appIcon.setContextMenu(contextMenu);
});

app.on("window-all-closed", () => {
  if (process.platform !== "darwin") {
    app.quit();
  }
});
