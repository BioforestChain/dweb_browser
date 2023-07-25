import { jsProcess } from "./deps.ts";

/**开启新页面 */
export const mwebview_open = async (url: string) => {
  return await jsProcess.nativeFetch(`file://mwebview.browser.dweb/open?url=${encodeURIComponent(url)}`).text();
};

/**
 * 激活窗口
 * TODO 未来，激活窗口需要消耗token，而token必须基于用户的touchend手势，一个touchend手势在只能创建一个token
 * @returns
 */
export const mwebview_activate = async () => {
  return await jsProcess.nativeFetch(`file://mwebview.browser.dweb/activate`).text();
};

/**关闭app */
export const mwebview_close = async (webview_id: string) => {
  return await jsProcess
    .nativeFetch(`file://mwebview.browser.dweb/close?webview_id=${encodeURIComponent(webview_id)}`)
    .text();
};

/**
 * 关闭window
 *
 */
export const mwebview_destroy = async () => {
  return await jsProcess.nativeFetch(`file://mwebview.browser.dweb/close/app`).boolean();
};

import { DetailedDiff, detailedDiff } from "npm:deep-object-diff";
export type $WebViewState = {
  isActivated: boolean;
  webviewId: string;
};
export interface $AllWebviewState {
  [key: number]: $WebViewState;
}

// 管理webView
export const all_webview_status = new (class extends Map<string, $WebViewState> {
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
export const sync_mwebview_status = async () => {
  if (_false === false) {
    return;
  }
  _false = false;

  const ipc = await navigator.dweb.jsProcess.connect("mwebview.browser.dweb");
  // 管理webview的状态，因为当前webview是通过状态判断操作的，比如激活，关闭
  let oldWebviewState: $WebViewState[] = [];

  ipc.onEvent((ipcEvent) => {
    /// 同步 mwebview 的状态机
    if (ipcEvent.name === "state") {
      const newState = JSON.parse(ipcEvent.text);
      const diff = detailedDiff(oldWebviewState, newState);
      oldWebviewState = newState;
      all_webview_status.diffFactory(diff);
    } else if (ipcEvent.name === "diff-state") {
      throw new Error("no implement");
    }
  });
  // TODO 这里应该进行一次主动同步全部状态
};
