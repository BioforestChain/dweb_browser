// 地址栏 browsrVeiw
import process from "node:process";
import { resolveToRoot } from "../../helper/createResolveTo.ts";
import { $BW } from "./browser.bw.ts";
import type { BrowserNMM } from "./browser.ts";

export function createAddressBrowserVeiw(
  this: BrowserNMM,
  bw: $BW,
  barHeight: number
) {
  const fileanme = resolveToRoot("assets/browser/newtab/address.html");
  const options = {
    webPreferences: {
      devTools: true,
      webSecurity: false,
      safeDialogs: true,
    },
  };

  const bv = new Electron.BrowserView(options);
  bv.setAutoResize({
    width: true,
    height: true,
    horizontal: true,
    vertical: true,
  });
  bw.addBrowserView(bv);
  bv.webContents.loadFile(fileanme);
  const [width] = bw.getContentSize();
  bv.setBounds({
    x: 0,
    y: bw.getTitleBarHeight(),
    width: width,
    height: barHeight,
  });

  // 调试状态显示 开发工具栏
  process.argv.includes("--inspect") ? bv.webContents.openDevTools() : "";
  return bv;
}
