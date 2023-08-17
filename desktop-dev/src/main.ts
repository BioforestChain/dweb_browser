/// <reference lib="dom"/>
import { webcrypto } from "node:crypto";
import os from "node:os";
import process from "node:process";
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

import { DeskNMM } from "./browser/desk/desk.nmm.ts";
import { JmmNMM } from "./browser/jmm/jmm.ts";
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

// task bar
if (os.platform() === "win32") {
  Electron.app.setUserTasks([
    {
      program: process.execPath,
      arguments: "--quit",
      iconPath: process.execPath,
      iconIndex: 0,
      title: "Quit",
      description: "exit Dweb Browser",
    },
  ]);
}

Electron.app.on('ready', () => {
  // 退出应用
  if(process.argv.includes('--quit')){
    Electron.app.quit()
  }
})