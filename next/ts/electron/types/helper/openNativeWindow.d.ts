import { BrowserWindow, BrowserWindowConstructorOptions } from "electron";
import * as Electron from "electron";
export declare const openNativeWindow: (url: string, options?: BrowserWindowConstructorOptions, webContentsConfig?: {
    userAgent?: ((userAgent: string) => string) | undefined;
}) => Promise<BrowserWindow & {
    getApis<T>(): import("comlink").Remote<T>;
}>;
export declare class ForRenderApi {
    private win;
    constructor(win: BrowserWindow);
    openDevTools(webContentsId: number, options?: Electron.OpenDevToolsOptions, devToolsId?: number): void;
    denyWindowOpenHandler(webContentsId: number, onDeny: (details: Electron.HandlerDetails) => unknown): void;
    destroy(webContentsId: number, options?: Electron.CloseOpts): void;
    onDestroy(webContentsId: number, onDestroy: () => unknown): void;
    getWenContents(webContentsId: number): globalThis.Electron.WebContents & import("comlink").ProxyMarked;
    closedBrowserWindow(): void;
}
export type $NativeWindow = Awaited<ReturnType<typeof openNativeWindow>>;
