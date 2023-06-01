import { DetailedDiff, detailedDiff } from "deep-object-diff";
import { EVENT, WebViewState } from "./tool.event.ts";

// 管理webView
export const webViewMap = new Map<string, WebViewState>();
// 管理webview的状态，因为当前webview是通过状态判断操作的，比如激活，关闭
let oldWebviewState: WebViewState[] = [];

/// 同步 mwebview 的状态机
navigator.dweb.jsProcess.onDwebViewState((event, _ipc) => {
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
const diffFactory = (diff: DetailedDiff) => {
  //  是否有新增
  for (const id in diff.added) {
    webViewMap.set(id, diff.added[id as keyof typeof diff.added]);
  }
  // 是否有删除
  for (const id in diff.deleted) {
    webViewMap.delete(id);
  }
  // 是否有更新
  for (const id in diff.updated) {
    webViewMap.set(id, diff.updated[id as keyof typeof diff.updated]);
  }
};
