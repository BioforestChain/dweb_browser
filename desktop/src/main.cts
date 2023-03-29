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


// 安装 status-bar.sys.dweb
import { StatusbarNativeUiNMM } from "./sys/native-ui/status-bar/status-bar.main.cjs";
dns.install(new StatusbarNativeUiNMM());

// 安装 navigatorbar.nativeui.sys.dweb
import { NavigationBarNMM } from "./sys/native-ui/navigation-bar/navigation-bar.cjs";
dns.install(new NavigationBarNMM());

import { SafeAreaNMM } from "./sys/native-ui/safe-area/safe-area.cjs"
dns.install(new SafeAreaNMM())

// 安装 jmmMetadata.sys.dweb
import { JMMMetadata } from "./sys/jmm-metadata/jmm-metadata.cjs";
dns.install(new JMMMetadata());


// 安装 jmm.test.connect.dweb
import { jmmtestconnectJMM } from "./user/jmm-test-connect/jmmtestconnect.main.cjs";
import { jmmtestconnectJMM2 } from "./user/jmm-test-connect2/jmmtestconnect2.main.cjs";
dns.install(jmmtestconnectJMM)
dns.install(jmmtestconnectJMM2)


dns.install(new BootNMM([
  "http.sys.dweb",
  "mwebview.sys.dweb",
  "js.sys.dweb",
  "status-bar.nativeui.sys.dweb",
  "navigation-bar.nativeui.sys.dweb",
  "safe-area.nativeui.sys.dweb",
  "browser.sys.dweb"
  // cotDemoJMM.mmid

  // 下面是专门用来测是 connect
  // "js.sys.dweb",
  // jmmtestconnectJMM.mmid,
  // jmmtestconnectJMM2.mmid
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
