import { NativeMicroModule } from "../../core/micro-module.native.js";
import { log } from "../../helper/devtools.js";
import { WWWServer } from "./www-server.js";
// 提供下载的 UI 
export class DownloadNMM extends NativeMicroModule {
    constructor() {
        super(...arguments);
        Object.defineProperty(this, "mmid", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: "download.sys.dweb"
        });
        Object.defineProperty(this, "httpNMM", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: void 0
        });
        Object.defineProperty(this, "waitForOperationResMap", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: new Map()
        });
    }
    async _bootstrap(context) {
        log.green(`[${this.mmid}] _bootstrap`);
        this.httpNMM = (await context.dns.query('http.sys.dweb'));
        if (this.httpNMM === undefined)
            throw new Error(`[${this.mmid}] this.httpNMM === undefined`);
        {
            new WWWServer(this);
        }
    }
    _shutdown() {
        throw new Error("Method not implemented.");
    }
}
