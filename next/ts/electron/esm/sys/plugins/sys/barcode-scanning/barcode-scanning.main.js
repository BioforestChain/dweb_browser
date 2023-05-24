// 模拟状态栏模块-用来提供状态UI的模块
import { NativeMicroModule } from "../../../../core/micro-module.native.js";
import { log } from "../../../../helper/devtools.js";
import Jimp from "jimp";
import jsQR from "jsqr";
export class BarcodeScanningNativeUiNMM extends NativeMicroModule {
    constructor() {
        super(...arguments);
        Object.defineProperty(this, "mmid", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: "barcode-scanning.sys.dweb"
        });
        Object.defineProperty(this, "httpIpc", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: void 0
        });
        Object.defineProperty(this, "httpNMM", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: void 0
        });
        Object.defineProperty(this, "observe", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: new Map()
        });
        Object.defineProperty(this, "waitForOperationRes", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: new Map()
        });
        Object.defineProperty(this, "reqResMap", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: new Map()
        });
        Object.defineProperty(this, "observeMap", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: new Map()
        });
        Object.defineProperty(this, "encoder", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: new TextEncoder()
        });
        Object.defineProperty(this, "allocId", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: 0
        });
        Object.defineProperty(this, "_bootstrap", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: async (context) => {
                log.green(`[${this.mmid} _bootstrap]`);
                let isStop = false;
                this.registerCommonIpcOnMessageHandler({
                    method: "POST",
                    pathname: "/process",
                    matchMode: "full",
                    input: {},
                    output: "string",
                    handler: async (args, client_ipc, ipcRequest) => {
                        // const host: string = ipcRequest.parsed_url.host;
                        // const pathname = ipcRequest.parsed_url.pathname;
                        // const search = ipcRequest.parsed_url.search;
                        // const url = `file://mwebview.sys.dweb/plugin/${host}${pathname}${search}`
                        // const result = await this.nativeFetch(url)
                        console.log(ipcRequest.body);
                        // 直接解析二维码
                        return await Jimp.read(await ipcRequest.body.u8a()).then(({ bitmap }) => {
                            const result = jsQR(bitmap.data, bitmap.width, bitmap.height);
                            console.log("result: ", result);
                            return JSON.stringify(result === null ? [] : [result.data]);
                        });
                    },
                });
                this.registerCommonIpcOnMessageHandler({
                    method: "GET",
                    pathname: "/stop",
                    matchMode: "full",
                    input: {},
                    output: "boolean",
                    handler: async (args, client_ipc, ipcRequest) => {
                        // 停止及解析
                        isStop = true;
                        return true;
                    },
                });
            }
        });
        Object.defineProperty(this, "_process", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: async (req, res) => {
                switch (req.method) {
                    case "OPTIONS":
                        this._processOPTIONS(req, res);
                        break;
                    case "POST":
                        this._processPOST(req, res);
                        break;
                    default:
                        new Error(`没有处理的方法 ${req.method}`);
                }
            }
        });
        Object.defineProperty(this, "_processOPTIONS", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: async (req, res) => {
                res.end();
            }
        });
        Object.defineProperty(this, "_processPOST", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: async (req, res) => {
                // const buffer = Buffer.from(data.body)
                let chunks = Buffer.alloc(0);
                req.on("data", (chunk) => (chunks = Buffer.concat([chunks, chunk])));
                req.on("end", () => {
                    Jimp.read(chunks).then(({ bitmap }) => {
                        const result = jsQR(bitmap.data, bitmap.width, bitmap.height);
                        res.end(JSON.stringify(result === null ? [] : [result.data]));
                    });
                });
            }
        });
        Object.defineProperty(this, "_stop", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: async (req, res) => {
                throw new Error(`_stop 还没有处理`);
            }
        });
        Object.defineProperty(this, "_getPhoto", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: async (req, res) => {
                const id = this.allocId++;
                const origin = req.headers.origin;
                if (origin === undefined)
                    throw new Error(`origin === null`);
                const waitForRes = this.waitForOperationRes.get(origin);
                if (waitForRes === undefined)
                    throw new Error(`waitForRes ===  undefined`);
                waitForRes.write(this.encoder.encode(`${JSON.stringify({
                    operationName: "getPhoto",
                    value: "",
                    from: origin,
                    id: id,
                })}\n`));
                this.reqResMap.set(id, { req, res });
            }
        });
        Object.defineProperty(this, "_waitForOperation", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: async (req, res) => {
                const appUrl = new URL(req.url, req.headers.referer).searchParams.get("app_url");
                if (appUrl === null)
                    throw new Error(`${this.mmid} _waiForOperation appUrl === null`);
                this.waitForOperationRes.set(appUrl, res);
            }
        });
        Object.defineProperty(this, "_operationReturn", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: async (req, res) => {
                const id = req.headers.id;
                if (typeof id !== "string")
                    throw new Error(`${this.mmid} typeof id !== string`);
                if (Object.prototype.toString.call(id).slice(8, -1) === "Array")
                    throw new Error(`id === Array`);
                let chunks = Buffer.alloc(0);
                req.on("data", (chunk) => {
                    chunks = Buffer.concat([chunks, chunk]);
                });
                req.on("end", () => {
                    res.end();
                    const key = parseInt(id);
                    const reqRes = this.reqResMap.get(key);
                    if (reqRes === undefined)
                        throw new Error(`reqRes === undefined`);
                    reqRes.res.end(Buffer.from(chunks));
                    this.reqResMap.delete(key);
                });
            }
        });
    }
    _shutdown() { }
}
