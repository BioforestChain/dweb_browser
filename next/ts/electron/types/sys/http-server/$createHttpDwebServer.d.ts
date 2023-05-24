import { ReadableStreamIpc } from "../../core/ipc-web/ReadableStreamIpc.js";
import type { $ReqMatcher } from "../../helper/$ReqMatcher.js";
import type { $MicroModule } from "../../helper/types.js";
import { ServerStartResult } from "./const.js";
import type { $DwebHttpServerOptions } from "./net/createNetServer.js";
/** 创建一个网络服务 */
export declare const createHttpDwebServer: (microModule: $MicroModule, options: $DwebHttpServerOptions) => Promise<HttpDwebServer>;
export declare class HttpDwebServer {
    private readonly nmm;
    private readonly options;
    readonly startResult: ServerStartResult;
    constructor(nmm: $MicroModule, options: $DwebHttpServerOptions, startResult: ServerStartResult);
    /** 开始处理请求 */
    listen: (routes?: $ReqMatcher[]) => Promise<ReadableStreamIpc>;
    /** 关闭监听 */
    close: any;
}
/** 开始处理请求 */
export declare const listenHttpDwebServer: (microModule: $MicroModule, startResult: ServerStartResult, routes?: $ReqMatcher[]) => Promise<ReadableStreamIpc>;
/** 开始监听端口和域名 */
export declare const startHttpDwebServer: (microModule: $MicroModule, options: $DwebHttpServerOptions) => Promise<ServerStartResult>;
/** 停止监听端口和域名 */
export declare const closeHttpDwebServer: (microModule: $MicroModule, options: $DwebHttpServerOptions) => Promise<boolean>;
