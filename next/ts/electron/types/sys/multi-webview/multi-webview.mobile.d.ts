/// <reference types="node" />
import { NativeMicroModule } from "../../core/micro-module.native.js";
import { $NativeWindow } from "../../helper/openNativeWindow.js";
import type { $BootstrapContext } from "../../core/bootstrapContext.js";
import type { Remote } from "comlink";
import type { Ipc } from "../../core/ipc/ipc.js";
import type { OutgoingMessage } from "http";
/**
 * 构建一个视图树
 * 如果是桌面版，所以不用去管树的概念，直接生成生成就行了
 * 但这里是模拟手机版，所以还是构建一个层级视图
 */
export declare class MultiWebviewNMM extends NativeMicroModule {
    mmid: "mwebview.sys.dweb";
    observeMap: $ObserveMapNww;
    encoder: TextEncoder;
    _uid_wapis_map: Map<number, {
        nww: $NativeWindow;
        apis: Remote<$APIS>;
    }>;
    _bootstrap(context: $BootstrapContext): Promise<void>;
    /**
     * 获取当前激活的 browserWindow 的 apis
     */
    apisGetFromFocused(): any;
    browserWindowGetFocused(): (Electron.CrossProcessExports.BrowserWindow & {
        getApis<T>(): Remote<T>;
    }) | undefined;
    _shutdown(): void;
    forceGetWapis(ipc: Ipc, root_url: string): Promise<{
        nww: Electron.CrossProcessExports.BrowserWindow & {
            getApis<T>(): Remote<T>;
        };
        apis: any;
    }>;
    getWapisByUid(uid: number): {
        nww: Electron.CrossProcessExports.BrowserWindow & {
            getApis<T>(): Remote<T>;
        };
        apis: any;
    } | undefined;
}
export interface $ObserveItem {
    res: OutgoingMessage | undefined;
    isObserve: boolean;
}
type $ObserveMapNwwItem = Map<string /** mmid */, $ObserveItem>;
type $ObserveMapNww = Map<Electron.CrossProcessExports.BrowserWindow, $ObserveMapNwwItem>;
export {};
