/// <reference types="node" />
import { NativeMicroModule } from "../../core/micro-module.native.js";
import type { IncomingMessage, OutgoingMessage } from "http";
export interface $Listener {
    (req: IncomingMessage, res: OutgoingMessage): void;
}
/**
 * 类似 https.createServer
 * 差别在于服务只在本地运作
 */
export declare class HttpServerNMM extends NativeMicroModule {
    mmid: "http.sys.dweb";
    private _dwebServer;
    private _tokenMap;
    private _gatewayMap;
    private _info;
    private _allRoutes;
    protected _bootstrap(): Promise<void>;
    protected _shutdown(): void;
    private getServerUrlInfo;
    /** 申请监听，获得一个连接地址 */
    private start;
    /** 远端监听请求，将提供一个 ReadableStreamIpc 流 */
    private listen;
    /**
     * 释放监听
     */
    private close;
    getHostByReq: (req: IncomingMessage) => string;
    getHostByURLAndHeaders: (url: string, headers: {}) => string;
}
