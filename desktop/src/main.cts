import { BootNMM } from "./sys/boot.cjs";
import { DnsNMM } from "./sys/dns/dns.cjs";
import "./sys/dns/localeFileFetch.cjs";
import { HttpServerNMM } from "./sys/http-server/http-server.cjs";
import { JsProcessNMM } from "./sys/js-process/js-process.cjs";
import { MultiWebviewNMM } from "./sys/multi-webview/multi-webview.mobile.cjs";

export const dns = new DnsNMM();
dns.install(new MultiWebviewNMM());
dns.install(new JsProcessNMM());
dns.install(new HttpServerNMM());

import { desktopJmm } from "./user/desktop/desktop.main.cjs";
dns.install(desktopJmm);

// 安装 browser
// import { BrowserNMM } from "./sys/browser/browser.main.cjs"
// dns.install(new BrowserNMM())
import { browserJMM } from "./user/browser/browser.main.cjs";
dns.install(browserJMM);
// 安装 cot
import { cotJMM } from "./user/cot/cot.main.cjs";
dns.install(cotJMM);

import { cotDemoJMM } from "./user/cot-demo/cotDemo.main.cjs";
dns.install(cotDemoJMM);

// 安装 file.sys.dweb
import { FileNMM } from "./sys/file/file.cjs";
dns.install(new FileNMM());

import { JmmNMM } from "./sys/jmm/jmm.cjs";
dns.install(new JmmNMM());

import { WWWNMM } from "./sys/www/www.cjs";
dns.install(new WWWNMM());

import { ApiNMM } from "./sys/api/api.cjs";
dns.install(new ApiNMM());

// 安装 statusbar.sys.dweb
import { StatusbarNMM } from "./sys/statusbar/statusbar.main.cjs";
dns.install(new StatusbarNMM());

// 安装 navigatorbar.sys.dweb
import { NavigatorbarNMM } from "./sys/navigator-bar/navigator-bar.cjs";
dns.install(new NavigatorbarNMM());

// 安装 plugins.sys.dweb 服务
import { PluginsNMM } from "./sys/plugin/plugins.main.cjs";
dns.install(new PluginsNMM());

// 安装 jmmMetadata.sys.dweb
import { JMMMetadata } from "./sys/jmm-metadata/jmm-metadata.cjs";
dns.install(new JMMMetadata());

dns.install(new BootNMM([
  "http.sys.dweb",
  "mwebview.sys.dweb",
  "js.sys.dweb",
  "statusbar.sys.dweb",
  "browser.sys.dweb"
  // cotDemoJMM.mmid
]));

Object.assign(globalThis, { dns: dns });

process.on("unhandledRejection", (error) => {
  console.error("????", error);
  debugger;
});

import { webcrypto } from "node:crypto";
if (typeof crypto === "undefined") {
  Object.assign(globalThis, { crypto: webcrypto });
}
