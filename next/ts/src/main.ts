import { BootNMM } from "./sys/boot.ts";
import { DnsNMM } from "./sys/dns/dns.ts";
import "./sys/dns/localeFileFetch.ts";
import { DownloadNMM } from "./sys/download/download.ts";
import { HttpServerNMM } from "./sys/http-server/http-server.ts";
import { JsProcessNMM } from "./sys/js-process/js-process.ts";
import { MultiWebviewNMM } from "./sys/multi-webview/multi-webview.mobile.ts";
import { NavigationBarNMM } from "./sys/plugins/native-ui/navigation-bar/navigation-bar.main.ts";
import { SafeAreaNMM } from "./sys/plugins/native-ui/safe-area/safe-area.main.ts";
import { StatusbarNativeUiNMM } from "./sys/plugins/native-ui/status-bar/status-bar.main.ts";
import { TorchNMM } from "./sys/plugins/native-ui/torch/torch.main.ts";
import { VirtualKeyboardNMM } from "./sys/plugins/native-ui/virtual-keyboard/virtual-keyboard.main.ts";
import { BarcodeScanningNativeUiNMM } from "./sys/plugins/sys/barcode-scanning/barcode-scanning.main.ts";
import { BiometricsNMM } from "./sys/plugins/sys/biometrics/biometrics.main.ts";
import { HapticsNMM } from "./sys/plugins/sys/haptics/haptics.main.ts";
import { ShareNMM } from "./sys/plugins/sys/share/share.main.ts";
import { ToastNMM } from "./sys/plugins/sys/toast/toast.main.ts";
import process from "node:process"

export const dns = new DnsNMM();
dns.install(new MultiWebviewNMM());
dns.install(new JsProcessNMM());
dns.install(new HttpServerNMM());
dns.install(new DownloadNMM());
dns.install(new StatusbarNativeUiNMM());
dns.install(new NavigationBarNMM());
dns.install(new SafeAreaNMM())
dns.install(new VirtualKeyboardNMM())
dns.install(new ToastNMM())
dns.install(new TorchNMM())
dns.install(new BarcodeScanningNativeUiNMM())
dns.install(new BiometricsNMM());
dns.install(new HapticsNMM());
dns.install(new ShareNMM());

import { desktopJmm } from "./user/desktop/desktop.main.ts";
dns.install(desktopJmm);

import { browserJMM } from "./user/browser/browser.main.ts";
dns.install(browserJMM);

import { publicServiceJMM } from "./user/public-service/public.service.main.ts";
dns.install(publicServiceJMM);

// 安装 file.sys.dweb
import { FileNMM } from "./sys/file/file.ts";
dns.install(new FileNMM());

import { JmmNMM } from "./sys/jmm/jmm.ts";
dns.install(new JmmNMM());


// import { jmmtestconnectJMM } from "./user/jmm-test-connect/jmmtestconnect.main.ts";
// import { jmmtestconnectJMM2 } from "./user/jmm-test-connect2/jmmtestconnect2.main.ts";
// dns.install(jmmtestconnectJMM)
// dns.install(jmmtestconnectJMM2)


dns.install(new BootNMM([
  // 核心模块
  // "http.sys.dweb",
  // "mwebview.sys.dweb",
  // "jmm.sys.dweb",
  // "js.sys.dweb",

  // 插件模块
  // "download.sys.dweb",
  // "status-bar.nativeui.sys.dweb",
  // "navigation-bar.nativeui.sys.dweb",
  // "safe-area.nativeui.sys.dweb",
  // "virtual-keyboard.nativeui.sys.dweb",
  // "barcode-scanning.sys.dweb",
  // "toast.sys.dweb",
  // "torch.nativeui.sys.dweb",
  // "biometrics.sys.dweb",
  // "haptics.sys.dweb",
  // "share.sys.dweb",
  "browser.sys.dweb",
  // cotDemoJMM.mmid
  // 下面是专门用来测是 connect 的模块
  // "test-from.sys.dweb",

  // "js.sys.dweb",
  // jmmtestconnectJMM.mmid,
  // jmmtestconnectJMM2.mmid
]));

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