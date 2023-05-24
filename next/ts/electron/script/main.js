"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || function (mod) {
    if (mod && mod.__esModule) return mod;
    var result = {};
    if (mod != null) for (var k in mod) if (k !== "default" && Object.prototype.hasOwnProperty.call(mod, k)) __createBinding(result, mod, k);
    __setModuleDefault(result, mod);
    return result;
};
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.dns = void 0;
const dntShim = __importStar(require("./_dnt.shims.js"));
const boot_js_1 = require("./sys/boot.js");
const dns_js_1 = require("./sys/dns/dns.js");
require("./sys/dns/localeFileFetch.js");
const download_js_1 = require("./sys/download/download.js");
const http_server_js_1 = require("./sys/http-server/http-server.js");
const js_process_js_1 = require("./sys/js-process/js-process.js");
const multi_webview_mobile_js_1 = require("./sys/multi-webview/multi-webview.mobile.js");
const navigation_bar_main_js_1 = require("./sys/plugins/native-ui/navigation-bar/navigation-bar.main.js");
const safe_area_main_js_1 = require("./sys/plugins/native-ui/safe-area/safe-area.main.js");
const status_bar_main_js_1 = require("./sys/plugins/native-ui/status-bar/status-bar.main.js");
const torch_main_js_1 = require("./sys/plugins/native-ui/torch/torch.main.js");
const virtual_keyboard_main_js_1 = require("./sys/plugins/native-ui/virtual-keyboard/virtual-keyboard.main.js");
const barcode_scanning_main_js_1 = require("./sys/plugins/sys/barcode-scanning/barcode-scanning.main.js");
const biometrics_main_js_1 = require("./sys/plugins/sys/biometrics/biometrics.main.js");
const haptics_main_js_1 = require("./sys/plugins/sys/haptics/haptics.main.js");
const share_main_js_1 = require("./sys/plugins/sys/share/share.main.js");
const toast_main_js_1 = require("./sys/plugins/sys/toast/toast.main.js");
const process_1 = __importDefault(require("process"));
exports.dns = new dns_js_1.DnsNMM();
exports.dns.install(new multi_webview_mobile_js_1.MultiWebviewNMM());
exports.dns.install(new js_process_js_1.JsProcessNMM());
exports.dns.install(new http_server_js_1.HttpServerNMM());
exports.dns.install(new download_js_1.DownloadNMM());
exports.dns.install(new status_bar_main_js_1.StatusbarNativeUiNMM());
exports.dns.install(new navigation_bar_main_js_1.NavigationBarNMM());
exports.dns.install(new safe_area_main_js_1.SafeAreaNMM());
exports.dns.install(new virtual_keyboard_main_js_1.VirtualKeyboardNMM());
exports.dns.install(new toast_main_js_1.ToastNMM());
exports.dns.install(new torch_main_js_1.TorchNMM());
exports.dns.install(new barcode_scanning_main_js_1.BarcodeScanningNativeUiNMM());
exports.dns.install(new biometrics_main_js_1.BiometricsNMM());
exports.dns.install(new haptics_main_js_1.HapticsNMM());
exports.dns.install(new share_main_js_1.ShareNMM());
const desktop_main_js_1 = require("./user/desktop/desktop.main.js");
exports.dns.install(desktop_main_js_1.desktopJmm);
const browser_main_js_1 = require("./user/browser/browser.main.js");
exports.dns.install(browser_main_js_1.browserJMM);
const public_service_main_js_1 = require("./user/public-service/public.service.main.js");
exports.dns.install(public_service_main_js_1.publicServiceJMM);
// 安装 file.sys.dweb
const file_js_1 = require("./sys/file/file.js");
exports.dns.install(new file_js_1.FileNMM());
const jmm_js_1 = require("./sys/jmm/jmm.js");
exports.dns.install(new jmm_js_1.JmmNMM());
// import { jmmtestconnectJMM } from "./user/jmm-test-connect/jmmtestconnect.main.ts";
// import { jmmtestconnectJMM2 } from "./user/jmm-test-connect2/jmmtestconnect2.main.ts";
// dns.install(jmmtestconnectJMM)
// dns.install(jmmtestconnectJMM2)
exports.dns.install(new boot_js_1.BootNMM([
    // 核心模块
    "http.sys.dweb",
    "mwebview.sys.dweb",
    "jmm.sys.dweb",
    "js.sys.dweb",
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
Object.assign(dntShim.dntGlobalThis, { dns: exports.dns });
process_1.default.on("unhandledRejection", (error) => {
    console.error("????", error);
});
const crypto_1 = require("crypto");
if (typeof crypto === "undefined") {
    Object.assign(dntShim.dntGlobalThis, { crypto: crypto_1.webcrypto });
}
