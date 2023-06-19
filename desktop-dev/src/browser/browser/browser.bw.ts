// browserWindow
import type { BrowserNMM } from "./browser.ts";
import type { $Callback, $Details } from "./types.ts";

export function createBrowserWindow(this: BrowserNMM): $BW {
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
      webSecurity: false,
      safeDialogs: true,
    },
  };
  const bw = new Electron.BrowserWindow(options) as Electron.BrowserWindow;
  bw.on("close", () => {
    // 关闭browser就是关闭全部的进程
    Electron.app.quit();
  });

  const session = bw.webContents.session;
  const filter = {
    // 拦截全部 devtools:// 协议发起的请求
    // urls: ["devtools://*/*"]
    urls: ["http://localhost/*", "https://shop.plaoc.com/*.json"],
  };

  session.webRequest.onBeforeRequest(filter, (details: { url: string|URL; method: string; }, callback: $Callback) => {
    const _url = new URL(details.url);
    console.always("browser.content.bv.ts 拦截到了请求", _url)
    // 不能够直接返回只能够重新定向 broser.dweb 的 apiServer 服务器
    if (_url.hostname === "localhost" && details.method === "GET") {
      relayGerRequest.bind(this)(details, callback);
      return;
    }

    throw new Error(`还有被拦截但没有转发的请求 ${details.url}`);
  });

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

async function relayGerRequest(
  browser: BrowserNMM,
  details: $Details,
  callback: $Callback
) {
  const _url = new URL(details.url);
  // const url = `${this.apiServer?.startResult.urlInfo.internal_origin}${_url.pathname}${_url.search}`;
  let pathname = _url.pathname;
  let response = null
  // dweb_deeplink 请求
  if (_url.protocol === "dweb:") {
   response =  await browser.nativeFetch(_url.href)
  }

  const mmid = _url.searchParams.get("mmid")
  if (mmid)  {
    pathname = pathname.replace("browser.dweb",mmid)
  }
  console.always("browser.content.bv.ts 拦截到了请求xx", `file:/${pathname}`)
  response = await browser.nativeFetch(`file:/${pathname}`)
  // callback({ cancel: false, redirectURL: response });
  callback({ cancel: false, redirectURL: response });
}


export type $BW = Electron.BrowserWindow & $ExtendsBrowserWindow;

export interface $ExtendsBrowserWindow {
  getTitleBarHeight(): number;
}
