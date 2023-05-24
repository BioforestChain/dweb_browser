import { expose, proxy, wrap } from "comlink";
import {
  app,
  BrowserWindow,
  BrowserWindowConstructorOptions,
  protocol,
} from "electron";
import * as Electron from "electron/main";
import { createResolveTo } from "./createResolveTo.cjs";
import { PromiseOut } from "./PromiseOut.cjs";
import HTTP from "node:http";
import URL from "node:url";

export const openNativeWindow = async (
  url: string,
  options: BrowserWindowConstructorOptions = {},
  webContentsConfig: { userAgent?: (userAgent: string) => string } = {}
) => {
  const { MainPortToRenderPort } = await import("./electronPortMessage.mjs");
  await app.whenReady();

  protocol.registerHttpProtocol("http", (request, callback) => {
    callback({
      url: request.url,
      method: request.method,
      session: undefined,
    });
  });

  protocol.registerHttpProtocol("https", (request, callback) => {
    console.log("被转发了的请求request: ", request.url);
    // 把 https 的请求转为 http 发送
    callback({
      url: request.url.replace("https://", "http://"),
      method: request.method,
      session: undefined,
    });
  });

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
  if (webContentsConfig.userAgent) {
    win.webContents.setUserAgent(
      webContentsConfig.userAgent(win.webContents.userAgent)
    );
  }

  const show_po = new PromiseOut<void>();
  win.once("ready-to-show", () => {
    win.show();
    win.webContents.openDevTools();
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

  console.log("[openNativeWindow.cts]url", url);
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
    if (content_wcs === undefined) throw new Error(`content_wcs === undefined`);
    if (devToolsId) {
      const devTools_wcs = Electron.webContents.fromId(devToolsId);
      if (devTools_wcs === undefined)
        throw new Error(`content_wcs === undefined`);
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
    const contents = Electron.webContents.fromId(webContentsId);
    if (contents === undefined) throw new Error(`contents === undefined`);
    return contents.setWindowOpenHandler((detail) => {
      onDeny(detail);
      return { action: "deny" };
    });
  }
  destroy(webContentsId: number, options?: Electron.CloseOpts) {
    const contents = Electron.webContents.fromId(webContentsId);
    if (contents === undefined) throw new Error(`contents === undefined`);
    return contents.close(options);
  }
  onDestroy(webContentsId: number, onDestroy: () => unknown) {
    const contents = Electron.webContents.fromId(webContentsId);
    if (contents === undefined) throw new Error(`contents === undefined`);
    contents.addListener("destroyed", () => {
      onDestroy();
    });
  }
  getWenContents(webContentsId: number) {
    const contents = Electron.webContents.fromId(webContentsId);
    if (contents === undefined) throw new Error(`contents === undefined`);
    return proxy(contents);
  }

  // 关闭 Browserwindow
  closedBrowserWindow() {
    this.win.close();
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
