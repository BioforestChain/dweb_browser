// 工具函数用来打开 js-process 的window
import { Remote, wrap } from "comlink";
import Electron from "electron";
import { PromiseOut } from "../../helper/PromiseOut.ts";

export async function jsProcessOpenWindow(
  url: string,
  options: Electron.BrowserWindowConstructorOptions = {},
  webContentsConfig: { userAgent?: (userAgent: string) => string } = {}
): Promise<$NWW> {
  const { MainPortToRenderPort } = await import(
    "../../helper/electronPortMessage.ts"
  );
  options.webPreferences = {
    ...options.webPreferences,
    sandbox: false,
    devTools: true,
    webSecurity: false,
    nodeIntegration: true, // 注入 ipcRenderer 对象
    contextIsolation: false,
  };
  const devToolsBV = new Electron.BrowserView();
  const contentBV = new Electron.BrowserView(options);
  contentBV.webContents.setDevToolsWebContents(devToolsBV.webContents);
  contentBV.webContents.openDevTools({ mode: "detach" });
  contentBV.webContents.setWindowOpenHandler((_detail: unknown) => {
    return { action: "deny" };
  });
  const show_po = new PromiseOut<void>();
  const ports_po = new PromiseOut<{
    import_port: MessagePort;
    export_port: MessagePort;
  }>();

  contentBV.webContents.on("dom-ready", () => {
    show_po.resolve();
  });
  contentBV.webContents.ipc.once("renderPort", (event) => {
    const [import_port, export_port] = event.ports;
    ports_po.resolve({
      import_port: MainPortToRenderPort(import_port),
      export_port: MainPortToRenderPort(export_port),
    });
  });

  const bw = new Electron.BrowserWindow({ ...options, show: true });
  bw.setTitle(`for: ${url}`);
  const bounds = bw.getBounds();
  const contentBounds = bw.getContentBounds();
  const titleBarHeight = bounds.height - contentBounds.height;
  bw.addBrowserView(contentBV);
  bw.addBrowserView(devToolsBV);

  if (webContentsConfig.userAgent) {
    contentBV.webContents.setUserAgent(
      webContentsConfig.userAgent(contentBV.webContents.userAgent)
    );
  }

  contentBV.setBounds({
    x: 0,
    y: titleBarHeight,
    width: options.show ? contentBounds.width / 2 : 0,
    height: options.show ? contentBounds.height : 0,
  });

  devToolsBV.setBounds({
    x: options.show ? contentBounds.width / 2 : 0,
    y: titleBarHeight,
    width: options.show ? contentBounds.width / 2 : contentBounds.width,
    height: contentBounds.height,
  });

  await contentBV.webContents.loadURL(url);
  await show_po.promise;
  const { import_port, export_port } = await ports_po.promise;
  // js-process ui 没有获取主进程方法的需求
  // expose(new ForRenderApi(win), export_port);
  return Object.assign(bw, {
    getApis<T>() {
      return wrap<T>(import_port);
    },
  });
}

export type $NWW = Electron.BrowserWindow & $ExtendsBrowserWindow;
export interface $ExtendsBrowserWindow {
  getApis<T>(): Remote<T>;
}
