import type { HttpDwebServer } from "dweb/sys/http-server/$createHttpDwebServer.ts";
import { DetailedDiff, detailedDiff } from "npm:deep-object-diff";
import { EVENT, WebViewState } from "./tool.event.ts";
import { closeDwebView, nativeActivate } from "./tool.native.ts";

// 管理webView
export const webViewMap = new Map<string, WebViewState>();
// 管理webview的状态，因为当前webview是通过状态判断操作的，比如激活，关闭
let oldWebviewState: WebViewState[] = [];

/**
 * 重启前后端
 * @param url
 * @param servers
 * @param ipcs
 */
export const closeApp = async (servers: HttpDwebServer[]) => {
  // 关闭api和文件的http服务
  const serverOp = servers.map(async (server) => {
    await server.close();
  });
  await Promise.all(serverOp);
};

/**
 * 关闭前端
 */
export const closeFront = (): Promise<string> => {
  return new Promise((resolve, _reject) => {
    // 关闭所有的DwebView
    webViewMap.forEach(async (state) => {
      await closeDwebView(state.webviewId);
      if (webViewMap.size === 0) {
        resolve("closeFront ok");
      }
    });
  });
};

/// 同步 mwebview 的状态机
navigator.dweb.jsProcess.onDwebViewState((event, ipc) => {
  if (event.name === EVENT.State && typeof event.data === "string") {
    const newState = JSON.parse(event.data);
    const diff = detailedDiff(oldWebviewState, newState);
    oldWebviewState = newState;
    diffFactory(diff);
  }
});

/**
 * 对比状态的更新
 * @param diff
 */
const diffFactory = async (diff: DetailedDiff) => {
  //  是否有新增
  for (const id in diff.added) {
    webViewMap.set(id, JSON.parse(diff.added[id as keyof typeof diff.added]));
  }
  // 是否有删除
  for (const id in diff.deleted) {
    webViewMap.delete(id);
    await closeDwebView(id);
  }
  // 是否有更新
  for (const id in diff.updated) {
    webViewMap.set(
      id,
      JSON.parse(diff.updated[id as keyof typeof diff.updated])
    );
    await nativeActivate(id);
  }
};
