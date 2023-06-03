// 地址栏 browsrVeiw
import path from "node:path";
import { $BW } from "./browser.bw.ts";
import Electron from "electron";
import type { $Details, $Callback } from "./types.ts";

export function createAddressBrowserVeiw(
  bw: $BW,
  barHeight: number
){
  const fileanme = path.resolve(
    Electron.app.getAppPath(),
    "assets/browser/newtab/address.html"
  );
  const options = {
    webPreferences: {
      devTools: true,
      webSecurity: false,
      safeDialogs: true,
    }
  }

  const bv = new Electron.BrowserView(options);
  bv.setAutoResize({
    width: true,
    height: true,
    horizontal: true,
    vertical: true
  })
  bw.addBrowserView(bv);
  bv.webContents.loadFile(fileanme)
  const [width] = bw.getContentSize();
  bv.setBounds({
    x: 0,
    y: bw.getTitleBarHeight(),
    width: width,
    height: barHeight,
  })
  bv.webContents.openDevTools();
  // const session = bv.webContents.session;
  // session.webRequest.onBeforeRequest((details: $Details, callback: $Callback) => {
  //   console.always('拦截到了 请求： ', details.url)
  //   callback({ cancel: false })
  // })
}