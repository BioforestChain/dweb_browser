// 内容的 browserView
import path from "node:path";
import { $BW } from "./browser.bw.ts";
import Electron from "electron";
import type { $Details, $Callback } from "./types.ts";
import type { BrowserNMM } from "./browser.ts";

export function createCBV(
  this: BrowserNMM,
  bw: $BW,
  barHeight: number
): $CBV{
  const index_html = path.resolve(
    Electron.app.getAppPath(),
    "assets/browser/newtab/index.html"
  );
  const options = {
    webPreferences: {
      devTools: true,
      webSecurity: false,
      safeDialogs: true,
    }
  }
  
  const cbv = new Electron.BrowserView(options)
  console.always("cbv: ", cbv, cbv)
  cbv.setAutoResize({
    width: true,
    height: true,
    horizontal: true,
    vertical: true
  })
  cbv.webContents.loadFile(index_html)
  bw.addBrowserView(cbv)
  const [width, height] = bw.getContentSize();
  cbv.setBounds({
    x: 0,
    y: bw.getTitleBarHeight() + barHeight ,
    width:  width,
    height: height - barHeight
  })
  cbv.webContents.openDevTools();

  return cbv
}
 
export type $CBV = Electron.BrowserView