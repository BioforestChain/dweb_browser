// 内容的 browserView
import path from "node:path";
import { $BW } from "./browser.bw.ts";
import Electron from "electron";
import type { $Details, $Callback } from "./types.ts";

export function createCBV(
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
  
  // const session = cbv.webContents.session;
  // session.webRequest.onBeforeRequest((details: $Details, callback: $Callback) => {
  //   console.always('拦截到了 请求： ', details.url)
  //   callback({ cancel: false })
  // })

  Reflect.set(
    cbv,
    "updatedContentByUrl",
    updatedContentByUrl.bind(cbv)
  )


  return cbv
}

function updatedContentByUrl(this: Electron.BrowserView){

}

export type $CBV = Electron.BrowserView & $ExtendsBrowserView

export interface $ExtendsBrowserView{
  updatedContentByUrl(url: string): void;
}

// export interface $Details{
//   id: number;
//   url: string;
//   method: string;
//   webContentsId: number;
//   webContents: Electron.WebContents;
//   frame: Electron.WebFrameMain;
//   resourceType: string //  - 可以是 mainFrame， subFrame，stylesheet，script，image，font，object，xhr，ping，cspReport，media，webSocket 或 other。
//   referrer: string
//   timestamp: number
//   uploadData: Electron.UploadData[]
// }

// export interface $Callback{
//   (arg: $Callback.$Response): void;
// }

// export declare namespace $Callback{
//   export interface $Response{
//     cancel?: boolean;
//     redirectURL?: string;
//   }
// }