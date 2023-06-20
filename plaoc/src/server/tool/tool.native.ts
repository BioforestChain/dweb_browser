import { IpcHeaders } from "../deps.ts";

type $IpcHeaders = InstanceType<typeof IpcHeaders>;

export const cros = (headers: $IpcHeaders) => {
  headers.init("Access-Control-Allow-Origin", "*");
  headers.init("Access-Control-Allow-Headers", "*"); // 要支持 X-Dweb-Host
  headers.init("Access-Control-Allow-Methods", "*");
  // headers.init("Connection", "keep-alive");
  // headers.init("Transfer-Encoding", "chunked");
  return headers;
};

const { jsProcess } = navigator.dweb;

/**开启新页面 */
export const nativeOpen = async (url: string) => {
  return await jsProcess
    .nativeFetch(`file://mwebview.browser.dweb/open?url=${encodeURIComponent(url)}`)
    .text();
};

/**
 * 激活窗口
 * @returns 
 */
export const nativeActivate = async () => {
  return await jsProcess
    .nativeFetch(
      `file://mwebview.browser.dweb/activate`
    )
    .text();
};

/**关闭app */
export const closeDwebView = async (webview_id: string) => {
  return await jsProcess
    .nativeFetch(
      `file://mwebview.browser.dweb/close?webview_id=${encodeURIComponent(
        webview_id
      )}`
    )
    .text();
};

/**
 * 关闭window
 *
 * */
export const closeWindow = async () => {
  return await jsProcess
    .nativeFetch(`file://mwebview.browser.dweb/close/app`)
    .boolean();
};

/**
 * 关闭app(这个会伴随后端一起关闭，能否关闭app只能由app自己决定)
 * */
export const closeApp = async () => {
  return await jsProcess
    .nativeFetch(`file://dns.sys.dweb/close?app_id=${jsProcess.mmid}`)
    .boolean();
};
