import { jsProcess } from "../deps.ts";

//申请模块窗口
export const apply_window = () => {
  return jsProcess.nativeFetch("file://window.sys.dweb/mainWindow").text();
};

export const open_main_window = () => {
  return jsProcess.nativeFetch("file://window.sys.dweb/openMainWindow").text();
};

// 彻底删除模块窗口
export const close_window = (wid: string) => {
  return jsProcess.nativeFetch(`file://window.sys.dweb/closeWindow?wid=${wid}`);
};

/**开启新页面 */
export const mwebview_open = async (wid: string, url: string) => {
  const openUrl = new URL(`file://mwebview.browser.dweb/open`);
  openUrl.searchParams.set("wid", wid);
  openUrl.searchParams.set("url", url);
  await jsProcess.nativeFetch(openUrl);
};

/**开启或者激活 */
export const mwebview_open_activate = async (wid: string, url: string) => {
  const openUrl = new URL(`file://mwebview.browser.dweb/openOrActivate`);
  openUrl.searchParams.set("wid", wid);
  openUrl.searchParams.set("url", url);
  await jsProcess.nativeFetch(openUrl);
};

/**
 * 激活窗口
 * TODO 未来，激活窗口需要消耗token，而token必须基于用户的touchend手势，一个touchend手势在只能创建一个token
 * @returns
 */
export const mwebview_activate = (wid: string) => {
  const activateUrl = new URL(`file://mwebview.browser.dweb/activate`);
  activateUrl.searchParams.set("wid", wid);
  return jsProcess.nativeFetch(activateUrl).text();
};

/**关闭app */
export const mwebview_close = (webview_id: string) => {
  return jsProcess
    .nativeFetch(`file://mwebview.browser.dweb/close?webview_id=${encodeURIComponent(webview_id)}`)
    .text();
};

/**
 * 关闭window
 *
 */
export const mwebview_destroy = () => {
  return jsProcess.nativeFetch(`file://mwebview.browser.dweb/close/app`).boolean();
};

// import { detailedDiff, type DetailedDiff } from "deep-object-diff";
// export type $WebViewState = {
//   isActivated: boolean;
//   webviewId: string;
//   index: number;
//   mmid: string;
// };

// export interface $AllWebviewState {
//   wid: string;
//   views: { [key: string]: $WebViewState };
// }

// export type DiffFn = (size: number) => unknown;

// 管理webView
// class AllWebviewStatus extends Map<string, $WebViewState> {
//   last() {
//     return [...this.entries()].at(-1)!;
//   }

//   signal = createSignal<DiffFn>();
//   /**
//    * 对比状态的更新
//    * @param diff
//    */
//   diffFactory(diff: DetailedDiff) {
//     //  是否有新增
//     for (const id in diff.added) {
//       this.set(id, diff.added[id as keyof typeof diff.added]);
//     }
//     // 是否有删除
//     for (const id in diff.deleted) {
//       this.delete(id);
//     }
//     // 是否有更新
//     for (const id in diff.updated) {
//       this.set(id, diff.updated[id as keyof typeof diff.updated]);
//     }
//     this.signal.emit(this.size);
//   }

//   private oldWebviewState: $AllWebviewState["views"] = {};

//   diffState(newState: $AllWebviewState) {
//     const diff = detailedDiff(this.oldWebviewState, newState.views);
//     this.oldWebviewState = newState.views;
//     this.diffFactory(diff);
//   }
// }
// export const all_webview_status = new AllWebviewStatus();

// let _false = true;
// export const sync_mwebview_status = async () => {
//   if (_false === false) {
//     return;
//   }
//   _false = false;

//   const ipc = await navigator.dweb.jsProcess.connect("mwebview.browser.dweb");

//   ipc.onEvent("ovserver-mwebiew-state").collect((ipcEvent) => {
//     const event = ipcEvent.data;
//     /// 同步 mwebview 的状态机
//     if (event.name === "state") {
//       const newState = JSON.parse(IpcEvent.text(event)) as $AllWebviewState;
//       all_webview_status.diffState(newState);
//     }
//   });
//   // TODO 这里应该进行一次主动同步全部状态
// };
