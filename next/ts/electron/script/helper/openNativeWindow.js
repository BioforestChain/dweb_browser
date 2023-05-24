"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || function (mod) {
    if (mod && mod.__esModule) return mod;
    var result = {};
    if (mod != null) for (var k in mod) if (k !== "default" && Object.prototype.hasOwnProperty.call(mod, k)) __createBinding(result, mod, k);
    __setModuleDefault(result, mod);
    return result;
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.ForRenderApi = exports.openNativeWindow = void 0;
const comlink_1 = require("comlink");
const electron_1 = require("electron");
const Electron = __importStar(require("electron"));
const PromiseOut_js_1 = require("./PromiseOut.js");
const openNativeWindow = async (url, options = {}, webContentsConfig = {}) => {
    const { MainPortToRenderPort } = await Promise.resolve().then(() => __importStar(require("./electronPortMessage.js")));
    await electron_1.app.whenReady();
    electron_1.protocol.registerHttpProtocol("http", (request, callback) => {
        callback({
            url: request.url,
            method: request.method,
            session: undefined,
        });
    });
    electron_1.protocol.registerHttpProtocol("https", (request, callback) => {
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
    const win = new electron_1.BrowserWindow(options);
    if (webContentsConfig.userAgent) {
        win.webContents.setUserAgent(webContentsConfig.userAgent(win.webContents.userAgent));
    }
    const show_po = new PromiseOut_js_1.PromiseOut();
    win.once("ready-to-show", () => {
        win.show();
        win.webContents.openDevTools();
        show_po.resolve();
    });
    win.webContents.setWindowOpenHandler((detail) => {
        debugger;
        return { action: "deny" };
    });
    const ports_po = new PromiseOut_js_1.PromiseOut();
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
    (0, comlink_1.expose)(new ForRenderApi(win), export_port);
    return Object.assign(win, {
        getApis() {
            return (0, comlink_1.wrap)(import_port);
        },
    });
};
exports.openNativeWindow = openNativeWindow;
class ForRenderApi {
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
        return (0, comlink_1.proxy)(contents);
    }
    // 关闭 Browserwindow
    closedBrowserWindow() {
        this.win.close();
    }
}
exports.ForRenderApi = ForRenderApi;
