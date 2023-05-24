import { log } from "../../../helper/devtools.js";
import { IPC_MESSAGE_TYPE } from "../../../core/ipc/const.js";
import { createHttpDwebServer } from "../../http-server/$createHttpDwebServer.js";
import { IpcResponse } from "../../../core/ipc/IpcResponse.js";
/**
 * 提供静态资源服务
 */
export class BaseWWWServer {
    constructor(nmm) {
        Object.defineProperty(this, "nmm", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: nmm
        });
        Object.defineProperty(this, "server", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: void 0
        });
        Object.defineProperty(this, "_int", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: async () => {
                this.server = await createHttpDwebServer(this.nmm, {});
                log.green(`[${this.nmm.mmid}] ${this.server.startResult.urlInfo.internal_origin}`);
                (await this.server.listen()).onMessage(this._onMessage);
            }
        });
        Object.defineProperty(this, "_onMessage", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: (message, ipc) => {
                switch (message.type) {
                    case IPC_MESSAGE_TYPE.REQUEST:
                        this._onRequest(message, ipc);
                        break;
                    default: this._onRequestMore(message, ipc);
                    // default: 
                }
            }
        });
        Object.defineProperty(this, "_onRequest", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: async (request, ipc) => {
                let pathname = request.parsed_url.pathname;
                if (pathname === "/" || pathname === "/index.html") {
                    pathname = `/html/${this.nmm.mmid}.html`;
                }
                const url = `file:///assets/${pathname}?mode=stream`;
                const remoteIpcResponse = await this.nmm.nativeFetch(url);
                ipc.postMessage(await IpcResponse.fromResponse(request.req_id, remoteIpcResponse, ipc));
            }
        });
        this._int();
    }
}
