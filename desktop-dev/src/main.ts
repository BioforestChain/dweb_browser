/// <reference lib="dom"/>
import { isWindows } from "https://deno.land/std@0.193.0/_util/os.ts";
import { webcrypto } from "node:crypto";
import path from "node:path";
import process from "node:process";
import { DeskNMM } from "./browser/desk/desk.nmm.ts";
import { DownloadNMM } from "./browser/download/download.nmm.ts";
import { JmmNMM } from "./browser/jmm/jmm.ts";
import { JsProcessNMM } from "./browser/js-process/js-process.ts";
import { MultiWebviewNMM } from "./browser/multi-webview/multi-webview.nmm.ts";
import { WebBrowserNMM } from "./browser/web/web.nmm.ts";
import { setFilter } from "./helper/devtools.ts";
import { isElectronDev } from "./helper/electronIsDev.ts";
import { BluetoothNMM } from "./std/bluetooth/bluetooth.main.ts";
import { HttpServerNMM } from "./std/http/http.nmm.ts";
import { BarcodeScanningNMM } from "./sys/barcode-scanning/barcode-scanning.main.ts";
import { BootNMM } from "./sys/boot/boot.ts";
import { ConfigNMM } from "./sys/config/config.ts";
import { DeviceNMM } from "./sys/device/device.main.ts";
import { DnsNMM } from "./sys/dns/dns.ts";
import "./sys/dns/localeFileFetch.ts";
import { HapticsNMM } from "./sys/haptics/haptics.ts";
import { ToastNMM } from "./sys/toast/toast.ts";
import { WindowNMM } from "./sys/window/window.ts";

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
dns.install(new ConfigNMM())
const webBrowser = new WebBrowserNMM();
dns.install(webBrowser);
dns.install(new BarcodeScanningNMM());
dns.install(new HapticsNMM())
dns.install(new WindowNMM())
dns.install(new ToastNMM())
dns.install(new DownloadNMM())
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

// 允许自签名证书
app.commandLine.appendSwitch('ignore-certificate-errors');

app.whenReady().then(async () => {
  const devIcon = isWindows ? "./electron/icons/win/icon.ico" : "./electron/icons/png/16x16.png";
  const depsIcon = isWindows ? "./resources/icons/win/icon.ico": "./resources/icons/png/16x16.png";
  const iconPath = isElectronDev
    ? path.join(path.dirname(app.getAppPath()), devIcon)
    : path.join(path.dirname(app.getPath("exe")), depsIcon);
  let img = Electron.nativeImage.createFromPath(iconPath)
  appIcon = new Tray(img);
  img = img.resize({width:16,height:16})
  const contextMenu = menu.buildFromTemplate([
    {
      icon:img,
      label: "quit",
      type: "normal",
      click: () => {
        app.quit();
      },
    },
  ]);

  // 对于 Linux 再次调用此命令，因为我们修改了上下文菜单
  appIcon.setToolTip("dweb_browser");
  appIcon.setContextMenu(contextMenu);

  Electron.protocol.handle("dweb", (req) => {
    return dns.nativeFetch(req.url.replace(/^dweb:\/\//, "dweb:"));
  });
  Electron.session.defaultSession.setProxy({
    proxyRules: `https=${await HttpServerNMM.proxyHost.promise}`
  });
});

// 设置deeplink
app.setAsDefaultProtocolClient("dweb");
const gotTheLock = app.requestSingleInstanceLock()
if(gotTheLock) {
  app.on('second-instance', (event, argv, workingDirectory) => {
    event.preventDefault();

    if(argv.length > 0) {
      try {
        let url = argv.pop()!;
        if(url.startsWith("dweb:")) {
          url = url.replace(/\:{1}\/{2}/, ":")
        }
        dns.nativeFetch(url); 
      } catch {
        //
      }
    }
  });
} else {
  app.quit();
}
app.on('will-finish-launching', () => {
  // macOS only
  app.on('open-url', (event, url) => {
    event.preventDefault();
    if(url.startsWith("dweb:")) {
      url = url.replace(/\:{1}\/{2}/, ":")
    }
    dns.nativeFetch(url);
  })
});

app.on("window-all-closed", () => {
  if (process.platform !== "darwin") {
    app.quit();
  }
});
