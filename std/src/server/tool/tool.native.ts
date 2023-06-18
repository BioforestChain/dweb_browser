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

export const nativeActivate = async (webview_id: string) => {
  return await jsProcess
    .nativeFetch(
      `file://mwebview.browser.dweb/activate?webview_id=${encodeURIComponent(
        webview_id
      )}`
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
