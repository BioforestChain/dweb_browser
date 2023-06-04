import process from "node:process";
import "./sys/dns/localeFileFetch.ts";

import { BrowserNMM } from "./browser/browser/browser.ts";
import { JsProcessNMM } from "./browser/js-process/js-process.ts";
import { MultiWebviewNMM } from "./browser/multi-webview/multi-webview.mobile.ts";
import { NavigationBarNMM } from "./browser/native-ui/navigation-bar/navigation-bar.main.ts";
import { SafeAreaNMM } from "./browser/native-ui/safe-area/safe-area.main.ts";
import { StatusbarNativeUiNMM } from "./browser/native-ui/status-bar/status-bar.main.ts";
import { TorchNMM } from "./browser/native-ui/torch/torch.main.ts";
import { VirtualKeyboardNMM } from "./browser/native-ui/virtual-keyboard/virtual-keyboard.main.ts";
import { setFilter } from "./helper/devtools.ts";
import { BarcodeScanningNativeUiNMM } from "./sys/barcode-scanning/barcode-scanning.main.ts";
import { BiometricsNMM } from "./sys/biometrics/biometrics.main.ts";
import { BootNMM } from "./sys/boot/boot.ts";
import { DnsNMM } from "./sys/dns/dns.ts";
import { HapticsNMM } from "./sys/haptics/haptics.main.ts";
import { HttpServerNMM } from "./sys/http-server/http-server.ts";
import { ShareNMM } from "./sys/share/share.main.ts";
import { ToastNMM } from "./sys/toast/toast.main.ts";

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
setFilter([
  "error",
  "browser",
  "mwebveiw"
]);

export const dns = new DnsNMM();
dns.install(new MultiWebviewNMM());
dns.install(new JsProcessNMM());
dns.install(new HttpServerNMM());
const dwebBrowser = new BrowserNMM();
dns.install(dwebBrowser);
dns.install(new StatusbarNativeUiNMM());
dns.install(new NavigationBarNMM());
dns.install(new SafeAreaNMM());
dns.install(new VirtualKeyboardNMM());
dns.install(new ToastNMM());
dns.install(new TorchNMM());
dns.install(new BarcodeScanningNativeUiNMM());
dns.install(new BiometricsNMM());
dns.install(new HapticsNMM());
dns.install(new ShareNMM());

// 安装 file.sys.dweb
import { FileNMM } from "./sys/file/file.ts";
dns.install(new FileNMM());

import { JmmNMM } from "./browser/jmm/jmm.ts";
dns.install(new JmmNMM());

// import { jmmtestconnectJMM } from "./user/jmm-test-connect/jmmtestconnect.main.ts";
// import { jmmtestconnectJMM2 } from "./user/jmm-test-connect2/jmmtestconnect2.main.ts";
// dns.install(jmmtestconnectJMM)
// dns.install(jmmtestconnectJMM2)

dns.install(
  new BootNMM([
    //
    dwebBrowser.mmid,
  ])
);

// 载入安装测试的代码
// import { TestFromNMM } from "./test/test.from.nmm.ts";
// import { TestToNMM } from "./test/test.to.nmm.ts";
// dns.install(new TestFromNMM())
// dns.install(new TestToNMM())

Object.assign(globalThis, { dns: dns });
process.on("unhandledRejection", (error) => {
  console.error("????", error);
});

import { webcrypto } from "node:crypto";
if (typeof crypto === "undefined") {
  Object.assign(globalThis, { crypto: webcrypto });
}
