import { expose, proxy, wrap } from "comlink";
import { app, BrowserWindow, BrowserWindowConstructorOptions } from "electron";
import * as Electron from "electron/main";
import { createResolveTo } from "./createResolveTo.cjs";
import { PromiseOut } from "./PromiseOut.cjs";
const resolveTo = createResolveTo(__dirname);

export const openNativeWindow = async (
  url: string,
  options: BrowserWindowConstructorOptions = {}
) => {
  const { MainPortToRenderPort } = await import("./electronPortMessage.mjs");
  await app.whenReady();
  options.webPreferences = {
    ...options.webPreferences,
    // preload: resolveTo("./openNativeWindow.preload.cjs"),
    sandbox: false,
    devTools: true,
    webSecurity: false,
    nodeIntegration: true,
    contextIsolation: false,
  };

  const win = new BrowserWindow(options);

  const show_po = new PromiseOut<void>();
  win.once("ready-to-show", () => {
    win.show();
    // win.webContents.openDevTools();
    show_po.resolve();
  });

  win.webContents.setWindowOpenHandler((detail) => {
    debugger;
    return { action: "deny" };
  });

  const ports_po = new PromiseOut<{
    import_port: MessagePort;
    export_port: MessagePort;
  }>();
  win.webContents.ipc.once("renderPort", (event) => {
    const [import_port, export_port] = event.ports;

    ports_po.resolve({
      import_port: MainPortToRenderPort(import_port),
      export_port: MainPortToRenderPort(export_port),
    });
  });
  console.log("url", url);
  await win.loadURL(url);
  await show_po.promise;

  const { import_port, export_port } = await ports_po.promise;

  expose(new ForRenderApi(win), export_port);
  return Object.assign(win, {
    getApis<T>() {
      return wrap<T>(import_port);
    },
  });
};
export class ForRenderApi {
  constructor(private win: BrowserWindow) {}
  openDevTools(
    webContentsId: number,
    options?: Electron.OpenDevToolsOptions,
    devToolsId?: number
  ) {
    const content_wcs = Electron.webContents.fromId(webContentsId);
    if (devToolsId) {
      const devTools_wcs = Electron.webContents.fromId(devToolsId);
      content_wcs.setDevToolsWebContents(devTools_wcs);
      queueMicrotask(() => {
        devTools_wcs.executeJavaScript("window.location.reload()");
      });
    }
    content_wcs.openDevTools(options);
  }
  denyWindowOpenHandler(
    webContentsId: number,
    onDeny: (details: Electron.HandlerDetails) => unknown
  ) {
    return Electron.webContents
      .fromId(webContentsId)
      .setWindowOpenHandler((detail) => {
        onDeny(detail);
        return { action: "deny" };
      });
  }
  destroy(webContentsId: number, options?: Electron.CloseOpts) {
    return Electron.webContents.fromId(webContentsId).close(options);
  }
  onDestroy(webContentsId: number, onDestroy: () => unknown) {
    Electron.webContents.fromId(webContentsId).addListener("destroyed", () => {
      onDestroy();
    });
  }
  getWenContents(webContentsId: number) {
    return proxy(Electron.webContents.fromId(webContentsId));
  }
}

// export const wrapNativeWindowAsApis = <T>(win: $NativeWindow) => {
//   wrap({
//     addEventListener(type, listener: (...args: any[]) => void) {
//       win.webContents.ipc.on(type, listener);
//     },
//     removeEventListener(type, listener: (...args: any[]) => void) {
//       win.webContents.ipc.off(type, listener);
//     },
//     postMessage(messaeg, transfer) {
//       win.webContents.ipc.;
//     },
//   }) as T;
// };

export type $NativeWindow = Awaited<ReturnType<typeof openNativeWindow>>;
