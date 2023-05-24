/// <reference types="node" />
import type { ReadableStreamIpc } from "../../core/ipc-web/ReadableStreamIpc.js";
import type { Ipc } from "../../core/ipc/ipc.js";
import { $ReqMatcher } from "../../helper/$ReqMatcher.js";
import type { WebServerRequest, WebServerResponse } from "./types.js";
export interface $Router {
    routes: readonly $ReqMatcher[];
    streamIpc: ReadableStreamIpc;
}
/**
 * > 目前只允许端口独占，未来会开放共享监听以及对应的路由策略（比如允许开发WASM版本的路由策略）
 */
export declare class PortListener {
    readonly ipc: Ipc;
    readonly host: string;
    readonly origin: string;
    constructor(ipc: Ipc, host: string, origin: string);
    private _routers;
    addRouter(router: $Router): () => void;
    /**
     * 判断是否有绑定的请求
     * @param pathname
     * @param method
     * @returns
     */
    private _isBindMatchReq;
    /**
     * 接收 nodejs-web 请求
     * 将之转发给 IPC 处理，等待远端处理完成再代理响应回去
     */
    hookHttpRequest(req: WebServerRequest, res: WebServerResponse): Promise<void>;
    private _on_destroy_signal;
    /** 监听 destroy 时间 */
    onDestroy: (cb: () => unknown) => import("../../helper/createSignal.js").$OffListener;
    /** 销毁监听器内产生的引用 */
    destroy(): void;
}
