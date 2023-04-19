import type { ReadableStreamIpc } from "../../core/ipc-web/ReadableStreamIpc.cjs";
import type { HttpDwebServer } from "../../sys/http-server/$createHttpDwebServer.cjs";
import type { WebViewState } from "./tool.event.mjs";
import { closeDwebView, emitUpdateFoundEvent } from "./tool.native.mjs";

// 管理webView
export const webViewMap = new Map<string, WebViewState>()


/**
 * 重启前后端
 * @param url 
 * @param servers 
 * @param ipcs 
 */
export const restartApp = async (
  url: URL,
  servers: HttpDwebServer[],
  ipcs: ReadableStreamIpc[]
) => {
  // 关闭api和文件的http服务
  servers.forEach((server) => {
    server.close();
  });
  // 关闭ipc信道
  ipcs.forEach((ipc) => {
    ipc.close();
  });
  // 关闭所有的DwebView
  webViewMap.forEach(async (state) => {
    await closeDwebView(state.webviewId);
  });

  // 转发file请求到目标NMM 并且重启服务
  const path = `file:/${url.pathname}${url.search}`;
  // 这里只需要把请求发送过去，因为app已经被关闭，已经无法拿到返回值
  jsProcess.nativeFetch(path);
  return "ok"
};

/**
 * 关闭前端
 */
export const closeFront = () => {
  // 关闭所有的DwebView
  webViewMap.forEach(async (state) => {
    await closeDwebView(state.webviewId);
  });
  return "ok"
}
