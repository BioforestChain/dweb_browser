// browserWindow
import { createComlinkNativeWindow } from "../../helper/openNativeWindow.ts";
import type { BrowserNMM } from "./browser.ts";

export async function createBrowserWindow(this: BrowserNMM, url: string) {
  const { x, y, width, height } = getInitSize();
  // 放到页面居中的位置
  const options = {
    x: x,
    y: y,
    width: width,
    height: height,
    movable: true,
    webPreferences: {
      devTools: true,
      webSecurity: false, // 跨域限制
      safeDialogs: true,
      sandbox: false,
      nodeIntegration: true, // 注入 ipcRenderer 对象
      contextIsolation: false,
    },
  };
  const bw = await createComlinkNativeWindow(url, options);
  // 获取 BrowserWindow 的 session 对象
  const mainWindowSession = bw.webContents.session;

  // 在 BrowserWindow 的 session 中拦截请求
  // mainWindowSession.webRequest.onBeforeRequest(async (request, callback) => {
  //   const _url = new URL(request.url);
  //   if (_url.hostname === "localhost" && request.method === "GET") {
  //    const pathname = _url.pathname.replace("/browser.dweb", "");
  //    console.always("onBeforeRequest=>",`http://browser.dweb-80.localhost:22605${pathname}${_url.search}`)
  //    callback({cancel:false,redirectURL:`http://browser.dweb-80.localhost:22605${pathname}${_url.search}`});
  //   }
  // });

  // Electron.protocol.registerBufferProtocol(
  //   "http",
  //   async (request, callback) => {
  //     const _url = new URL(request.url);
  //     const response = await relayGerRequest.bind(this)(_url);
  //     callback({
  //       statusCode: response.status,
  //       mimeType: response.type,
  //       data: Buffer.from(await response.arrayBuffer()),
  //     });
  //   }
  // );
  return Object.assign(bw, { getTitleBarHeight });
}

function getInitSize() {
  const size = Electron.screen.getPrimaryDisplay().size;
  const width = parseInt(size.width * 0.8 + "");
  const height = parseInt(size.height * 0.8 + "");
  const x = 0;
  const y = (size.height - height) / 2;
  return { width, height, x, y };
}

function getTitleBarHeight(this: Electron.BrowserWindow) {
  return this.getBounds().height - this.getContentBounds().height;
}

async function relayGerRequest(this: BrowserNMM, _url: URL) {
  let pathname = _url.pathname;
  let response: Response | null = null;
  console.always("browser.content.bv.ts 拦截到了请求xx", _url.href);
  if (_url.hostname !== "localhost") {
    response = await this.nativeFetch(_url.href);
    console.always("xxxxx", response);
    return response;
  }
  // dweb_deeplink 请求
  if (_url.protocol === "dweb:") {
    response = await this.nativeFetch(_url.href);
  }
  const mmid = _url.searchParams.get("mmid");
  if (mmid) {
    pathname = pathname.replace("browser.dweb", mmid);
  }
  response = await this.nativeFetch(`file:/${pathname}${_url.search}`);
  return response;
}

export type $BW = Electron.BrowserWindow & $ExtendsBrowserWindow;

export interface $ExtendsBrowserWindow {
  getTitleBarHeight(): number;
}
