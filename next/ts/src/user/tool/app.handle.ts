import type { ReadableStreamIpc } from "../../core/ipc-web/ReadableStreamIpc.ts";
import type { HttpDwebServer } from "../../sys/http-server/$createHttpDwebServer.ts";
import type { WebViewState } from "./tool.event.ts";
import { closeDwebView } from "./tool.native.ts";
import { hashConnentMap } from "./tool.request.ts";

// 管理webView
export const webViewMap = new Map<string, WebViewState>();

/**
 * 重启前后端
 * @param url
 * @param servers
 * @param ipcs
 */
export const closeApp = async (
  servers: HttpDwebServer[],
  ipcs: ReadableStreamIpc[],
) => {
  // 清空建立的连接
  hashConnentMap.clear();
  // 关闭api和文件的http服务
  const serverOp = servers.map(async (server) => {
    await server.close();
  });
  // 关闭ipc信道
  const opcOp = ipcs.map((ipc) => {
    ipc.close();
  });
  await Promise.all([serverOp, opcOp]);
  // 关闭所有的DwebView
  closeFront();
};

/**
 * 关闭前端
 */
export const closeFront = () => {
  // 关闭所有的DwebView
  webViewMap.forEach(async (state) => {
    await closeDwebView(state.webviewId);
  });
  webViewMap.clear();
  return "closeFront ok";
};
