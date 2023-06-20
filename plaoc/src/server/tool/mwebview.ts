import { DetailedDiff, detailedDiff } from "npm:deep-object-diff";
import { WebViewState } from "./tool.event.ts";

// 管理webView
export const webViewMap = new (class extends Map<string, WebViewState> {
  last() {
    return [...this.entries()].at(-1)!;
  }
  /**
   * 对比状态的更新
   * @param diff
   */
  diffFactory(diff: DetailedDiff) {
    //  是否有新增
    for (const id in diff.added) {
      this.set(id, diff.added[id as keyof typeof diff.added]);
    }
    // 是否有删除
    for (const id in diff.deleted) {
      this.delete(id);
    }
    // 是否有更新
    for (const id in diff.updated) {
      this.set(id, diff.updated[id as keyof typeof diff.updated]);
    }
  }
})();

let _false = true;
export const init = async () => {
  if (_false === false) {
    return;
  }
  _false = false;

  const ipc = await navigator.dweb.jsProcess.connect("mwebview.browser.dweb");
  // 管理webview的状态，因为当前webview是通过状态判断操作的，比如激活，关闭
  let oldWebviewState: WebViewState[] = [];

  ipc.onEvent((ipcEvent) => {
    /// 同步 mwebview 的状态机
    if (ipcEvent.name === "state") {
      const newState = JSON.parse(ipcEvent.text);
      const diff = detailedDiff(oldWebviewState, newState);
      oldWebviewState = newState;
      webViewMap.diffFactory(diff);
    } else if (ipcEvent.name === "diff-state") {
      throw new Error("no implement");
    }
  });
};
