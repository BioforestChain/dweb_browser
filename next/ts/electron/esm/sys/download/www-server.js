import { log } from "../../helper/devtools.js";
import { BaseWWWServer } from "../plugins/base/base_www_server.js";
export class WWWServer extends BaseWWWServer {
    constructor(nmm) {
        super(nmm);
        Object.defineProperty(this, "_onRequestMore", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: async (message, ipc) => {
                log.red(`${this.nmm.mmid} www-server.cts.cts _onMessage 还有消息没有处理 ${JSON.stringify(message)}`);
            }
        });
    }
}
