import { expose, proxy, wrap } from "comlink";
import { app, BrowserWindow, protocol, } from "electron";
import * as Electron from "electron";
import { PromiseOut } from "./PromiseOut.js";
export const openNativeWindow = async (url, options = {}, webContentsConfig = {}) => {
    const { MainPortToRenderPort } = await import("./electronPortMessage.js");
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
        win.webContents.setUserAgent(webContentsConfig.userAgent(win.webContents.userAgent));
    }
    const show_po = new PromiseOut();
    win.once("ready-to-show", () => {
        win.show();
        win.webContents.openDevTools();
        show_po.resolve();
    });
    win.webContents.setWindowOpenHandler((detail) => {
        debugger;
        return { action: "deny" };
    });
    const ports_po = new PromiseOut();
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
        getApis() {
            return wrap(import_port);
        },
    });
};
export class ForRenderApi {
    constructor(win) {
        Object.defineProperty(this, "win", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: win
        });
    }
    openDevTools(webContentsId, options, devToolsId) {
        const content_wcs = Electron.webContents.fromId(webContentsId);
        if (content_wcs === undefined)
            throw new Error(`content_wcs === undefined`);
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
    denyWindowOpenHandler(webContentsId, onDeny) {
        const contents = Electron.webContents.fromId(webContentsId);
        if (contents === undefined)
            throw new Error(`contents === undefined`);
        return contents.setWindowOpenHandler((detail) => {
            onDeny(detail);
            return { action: "deny" };
        });
    }
    destroy(webContentsId, options) {
        const contents = Electron.webContents.fromId(webContentsId);
        if (contents === undefined)
            throw new Error(`contents === undefined`);
        return contents.close(options);
    }
    onDestroy(webContentsId, onDestroy) {
        const contents = Electron.webContents.fromId(webContentsId);
        if (contents === undefined)
            throw new Error(`contents === undefined`);
        contents.addListener("destroyed", () => {
            onDestroy();
        });
    }
    getWenContents(webContentsId) {
        const contents = Electron.webContents.fromId(webContentsId);
        if (contents === undefined)
            throw new Error(`contents === undefined`);
        return proxy(contents);
    }
    // 关闭 Browserwindow
    closedBrowserWindow() {
        this.win.close();
    }
}
