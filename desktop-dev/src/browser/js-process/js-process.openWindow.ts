// 工具函数用来打开 js-process 的window
import { Remote } from "comlink";
import { electronConfig } from "../../helper/electronConfig.ts";
import { openNativeWindow } from "../../helper/openNativeWindow.ts";

declare global {
  interface ElectronConfig {
    "js-process-bounds"?: Electron.Rectangle;
  }
}

export async function jsProcessOpenWindow(
  url: string,
  _options: Electron.BrowserWindowConstructorOptions = {},
  webContentsConfig: { userAgent?: (userAgent: string) => string } = {}
): Promise<$NWW> {
  const { MainPortToRenderPort } = await import(
    "../../helper/electronPortMessage.ts"
  );
  const options = {
    ..._options,
    webPreferences: {
      ..._options.webPreferences,
      sandbox: false,
      devTools: true,
      webSecurity: true, // 跨域限制
      nodeIntegration: true, // 注入 ipcRenderer 对象
      contextIsolation: false,
    },
  };
  const browserWindow = await openNativeWindow(url, _options);

  browserWindow.webContents.on("will-prevent-unload", async (event) => {
    if (allowClose !== undefined) {
      if (allowClose) {
        /// 来自 close 的指令
        allowClose = false;
        /// 取消 unload 的 prevent（拦截）
        event.preventDefault();
      }

      /// 重置状态
      allowClose = undefined;
      return;
    }
    console.always("unload event", event, event.returnValue);
    // browserWindow.webContents
    const res = await Electron.dialog.showMessageBox({
      type: "warning",
      message: "刷新该页面，将使得所有的 js 进程被强制关停，确定？",
      buttons: ["取消", "确定重载"],
    });
    /// 确定重载
    if (res.response === 1) {
      /// 取消 unload 的 prevent（拦截）
      event.preventDefault();
    }
  });
  let allowClose: undefined | boolean;
  browserWindow.on("close", async (event) => {
    allowClose = false; // 重置

    const res = await Electron.dialog.showMessageBox({
      type: "warning",
      message: "关闭该页面，将使得所有的 js 进程被强制关停，确定？",
      buttons: ["取消", "确定关闭"],
    });
    /// 确定关闭
    if (res.response === 1) {
      electronConfig.set("js-process-bounds", browserWindow.getBounds());

      /// 同意关闭，将会冒泡到 will-prevent-unload
      allowClose = true;
    }
    /// 取消关闭
    else {
      allowClose = false;
      /// 阻止 close 的默认行为发生
      event.preventDefault();
    }
  });

  return browserWindow;
}

export type $NWW = Electron.BrowserWindow & $ExtendsBrowserWindow;
export interface $ExtendsBrowserWindow {
  getApis<T>(): Remote<T>;
}
