import type { IpcHeaders } from "../../core/ipc/IpcHeaders.cjs";

type $IpcHeaders = InstanceType<typeof IpcHeaders>;


export const cros = (headers: $IpcHeaders) => {
  headers.init("Access-Control-Allow-Origin", "*");
  headers.init("Access-Control-Allow-Headers", "*"); // 要支持 X-Dweb-Host
  headers.init("Access-Control-Allow-Methods", "*");
  // headers.init("Connection", "keep-alive");
  // headers.init("Transfer-Encoding", "chunked");
  return headers;
};

export const nativeOpen = async (url: string) => {
  return await jsProcess
    .nativeFetch(
      `file://mwebview.sys.dweb/open?url=${encodeURIComponent(url)}`
    )
    .text();
}


export const nativeActivate = async (webview_id: string) => {
  return await jsProcess
    .nativeFetch(
      `file://mwebview.sys.dweb/activate?webview_id=${encodeURIComponent(webview_id)}`
    )
    .text();
}


