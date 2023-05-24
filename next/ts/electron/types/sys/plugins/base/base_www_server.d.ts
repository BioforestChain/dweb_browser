import type { HttpDwebServer } from "../../http-server/$createHttpDwebServer.js";
import type { $IpcMessage } from "../../../core/ipc/const.js";
import type { Ipc } from "../../../core/ipc/ipc.js";
import type { NativeMicroModule } from "../../../core/micro-module.native.js";
/**
 * 提供静态资源服务
 */
export declare abstract class BaseWWWServer<T extends NativeMicroModule> {
    readonly nmm: T;
    server: HttpDwebServer | undefined;
    constructor(nmm: T);
    private _int;
    private _onMessage;
    private _onRequest;
    abstract _onRequestMore(message: $IpcMessage, ipc: Ipc): void;
}
