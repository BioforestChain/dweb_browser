// 工具函数用来打开 js-process 的window
import { Remote, wrap } from "comlink";
import { PromiseOut } from "../../helper/PromiseOut.ts";
import { ElectronConfig } from "../../helper/electron-config.ts";

declare global {
  interface ElectronConfig {
    "js-process-bounds": Electron.Rectangle;
  }
}

export async function jsProcessOpenWindow(
  url: string,
  options: Electron.BrowserWindowConstructorOptions = {},
  webContentsConfig: { userAgent?: (userAgent: string) => string } = {}
): Promise<$NWW> {
  const { MainPortToRenderPort } = await import(
    "../../helper/electronPortMessage.ts"
  );
  const browserWindow = new Electron.BrowserWindow({
    ...options,
    ...ElectronConfig.get("js-process-bounds"),
  });

  options.webPreferences = {
    ...options.webPreferences,
    sandbox: false,
    devTools: true,
    webSecurity: true, // 跨域限制
    nodeIntegration: true, // 注入 ipcRenderer 对象
    contextIsolation: false,
  };
  browserWindow.webContents.openDevTools();
  browserWindow.webContents.once("devtools-opened", () => {
    browserWindow.loadURL(url);
    browserWindow.webContents.disableDeviceEmulation();
    browserWindow.webContents.once("dom-ready", () => {
      show_po.resolve();
    });
  });
  browserWindow.webContents.setWindowOpenHandler((_detail) => {
    return { action: "allow" };
  });
  browserWindow.webContents.on("will-prevent-unload", async (event) => {
    if (allowClose) {
      /// 来自 close 的指令
      allowClose = false;
      /// 取消 unload 的 prevent（拦截）
      event.preventDefault();
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
  let allowClose = false;
  browserWindow.on("close", async (event) => {
    allowClose = false; // 重置

    const res = await Electron.dialog.showMessageBox({
      type: "warning",
      message: "关闭该页面，将使得所有的 js 进程被强制关停，确定？",
      buttons: ["取消", "确定关闭"],
    });
    /// 确定关闭
    if (res.response === 1) {
      ElectronConfig.set("js-process-bounds", browserWindow.getBounds());

      /// 同意关闭，将会冒泡到 will-prevent-unload
      allowClose = true;
    }
    /// 取消关闭
    else {
      /// 阻止 close 的默认行为发生
      event.preventDefault();
    }
  });
  const show_po = new PromiseOut<void>();
  const ports_po = new PromiseOut<{
    import_port: MessagePort;
    export_port: MessagePort;
  }>();

  browserWindow.webContents.ipc.once("renderPort", (event) => {
    const [import_port, export_port] = event.ports;
    ports_po.resolve({
      import_port: MainPortToRenderPort(import_port),
      export_port: MainPortToRenderPort(export_port),
    });
  });

  if (webContentsConfig.userAgent) {
    browserWindow.webContents.setUserAgent(
      webContentsConfig.userAgent(browserWindow.webContents.userAgent)
    );
  }

  await show_po.promise;
  const { import_port, export_port } = await ports_po.promise;

  return Object.assign(browserWindow, {
    getApis<T>() {
      return wrap<T>(import_port);
    },
  });
}

export type $NWW = Electron.BrowserWindow & $ExtendsBrowserWindow;
export interface $ExtendsBrowserWindow {
  getApis<T>(): Remote<T>;
}
