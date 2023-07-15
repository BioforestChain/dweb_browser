// 工具函数用来打开 js-process 的window
import { Remote } from "comlink";
import { $CreateNativeWindowOptions, createComlinkNativeWindow } from "../../helper/openNativeWindow.ts";

declare global {
  interface ElectronConfig {
    "js-process-bounds"?: Electron.Rectangle;
  }
}

export async function jsProcessOpenWindow(url: string, _options: $CreateNativeWindowOptions = {}): Promise<$NWW> {
  const { MainPortToRenderPort } = await import("../../helper/electronPortMessage.ts");
  const browserWindow = await createComlinkNativeWindow(url, _options);
  /**
   * https://github.com/electron/electron/issues/24994
   * will-prevent-unload 必须同步进行，渲染进程会被暂停。
   * 但是我们的进程都在Worker中，不会被完全影响，但是会影响到进程创建的任务、消息通讯也会被停滞。
   * 这可能会导致在这期间创建的任务被取消
   * 
   * @TODO 监听webContents的生命周期，确保createProcess等任务能抛出中断异常
   */
  browserWindow.webContents.on("will-prevent-unload", (event) => {
    let allowUnload = false;
    if (browserWindow.isVisible()) {
      const res = Electron.dialog.showMessageBoxSync({
        type: "warning",
        message: "这将使得所有的 js 进程被强制关停，确定？",
        buttons: ["保持原状", "关停"],
      });
      /// 确定重载
      if (res === 1) {
        allowUnload = true;
      }
    } else {
      allowUnload = true;
    }

    if (allowUnload) {
      /// 取消 unload 的 prevent（拦截）
      event.preventDefault();
    }
  });

  return browserWindow;
}

export type $NWW = Electron.BrowserWindow & $ExtendsBrowserWindow;
export interface $ExtendsBrowserWindow {
  getRenderApi<T>(): Remote<T>;
}
