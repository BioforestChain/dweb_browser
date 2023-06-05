// browserWindow 
import Electron from "electron";
import type { $Details, $Callback } from "./types.ts";
import type { BrowserNMM } from "./browser.ts";

export function createBrowserWindow(
  this: BrowserNMM
): $BW{
  const { x, y, width, height} = getInitSize()
  // 放到页面居中的位置
  const options = {
    x: x, 
    y: y,
    width: width,
    height: height,
    movable: true,
    webPreferences: {
      devTools: true,
      webSecurity: false,
      safeDialogs: true,
    },
  }
  const bw = new Electron.BrowserWindow(options) as Electron.BrowserWindow;
  bw.on('close', () => {
    // 关闭browser就是关闭全部的进程
    Electron.app.quit()
  })
  Reflect.set(
    bw, 
    "getTitleBarHeight",
    getTitleBarHeight
  )

  const session = bw.webContents.session;
  const filter = {
    // 拦截全部 devtools:// 协议发起的请求
    // urls: ["devtools://*/*"]
    urls: [
      "http://browser.dweb/*",
      "https://shop.plaoc.com/*.json"
    ]
  }
  
  session.webRequest.onBeforeRequest(filter, async (details: $Details, callback: $Callback) => {
    const _url = new URL(details.url)
    // console.always("browser.content.bv.ts 拦截到了请求", _url)
    // 不能够直接返回只能够重新定向 broser.dweb 的 apiServer 服务器
    if(_url.hostname === "browser.dweb" && details.method === "GET"){
      relayGerRequest.bind(this)(details, callback)
      return;
    }

    if(_url.hostname === "shop.plaoc.com" && details.method === 'GET'){
      relayExternalGetRequest.bind(this)(details, callback)
      return;
    }

    throw new Error(`还有被拦截但没有转发的请求 ${details.url}`)
  })

  return bw;
}

function getInitSize(){
  const size = Electron.screen.getPrimaryDisplay().size
  const width = parseInt(size.width * 0.8 + "");
  const height = parseInt(size.height * 0.8 + "")
  const x = 0;
  const y = (size.height - height) / 2
  return {width, height, x, y}
}

function getTitleBarHeight(this: Electron.BrowserWindow){
  return this.getBounds().height - this.getContentBounds().height;
}

async function relayGerRequest(this: BrowserNMM,details: $Details, callback: $Callback){
  const _url = new URL(details.url)
  const url = `${this.apiServer?.startResult.urlInfo.internal_origin}${_url.pathname}${_url.search}`;
  callback({ cancel: false, redirectURL: url })
}

/** 用来转发特定的外部 get 请求 */
async function relayExternalGetRequest(this: BrowserNMM, details: $Details, callback: $Callback){
  const url = `${this.apiServer?.startResult.urlInfo.internal_origin}/external?url=${details.url}`;
  callback({ cancel: false, redirectURL: url })
}

export type $BW = Electron.BrowserWindow & $ExtendsBrowserWindow;

export interface $ExtendsBrowserWindow{
  getTitleBarHeight(): number;
}