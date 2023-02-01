"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.AppNMM = void 0;
const helper_1 = require("../core/helper");
const ipc_1 = require("../core/ipc");
const micro_module_1 = require("../core/micro-module");
const micro_module_native_1 = require("../core/micro-module.native");
/** 内核，原 DNS 服务 */
class AppNMM extends micro_module_native_1.NativeMicroModule {
    constructor() {
        super(...arguments);
        this.mmid = "core.sys.dweb";
        this.apps = new Map();
        this.running_apps = new Map();
    }
    _bootstrap() {
        // const parseArgs_open = $parseRequestToParams({ app_id: "mmid" });
        // const parseArgs_close = $parseRequestToParams({ app_id: "mmid" });
        this.registerCommonIpcOnMessageHanlder({
            pathname: "/install-js",
            matchMode: "full",
            input: {},
            output: "void",
            hanlder: () => {
                /// TODO 动态创建 JsMicroModule
            },
        });
        this.registerCommonIpcOnMessageHanlder({
            pathname: "/open",
            matchMode: "full",
            input: { app_id: "mmid" },
            output: "boolean",
            hanlder: async (args) => {
                /// TODO 询问用户是否授权该行为
                await this.open(args.app_id);
                return true;
            },
        });
        this.registerCommonIpcOnMessageHanlder({
            pathname: "/close",
            matchMode: "full",
            input: { app_id: "mmid" },
            output: "boolean",
            hanlder: async (args) => {
                /// TODO 关闭应用首先要确保该应用的 parentProcessId 在 processTree 中
                await this.close(args.app_id);
                return true;
            },
        });
        const native_fetch = globalThis.fetch;
        const app_mm = this;
        const connects = new WeakMap();
        globalThis.fetch = function fetch(url, init) {
            /// 如果上下文是 MicroModule，那么进入特殊的解析模式
            if (this instanceof micro_module_1.MicroModule) {
                const from_app = this;
                let _parsed_url;
                let _request_init = init;
                if (typeof url === "string") {
                    _parsed_url = new URL(url);
                }
                else if (url instanceof Request) {
                    _parsed_url = new URL(url.url);
                    _request_init = url;
                }
                else if (url instanceof URL) {
                    _parsed_url = url;
                }
                if (_parsed_url !== undefined) {
                    const parsed_url = _parsed_url;
                    const request_init = _request_init ?? {};
                    if (parsed_url.protocol === "file:" &&
                        parsed_url.hostname.endsWith(".dweb")) {
                        const mmid = parsed_url.hostname;
                        /// 拦截到了，走自定义总线
                        let from_app_ipcs = connects.get(from_app);
                        if (from_app_ipcs === undefined) {
                            from_app_ipcs = new Map();
                            connects.set(from_app, from_app_ipcs);
                        }
                        let ipc_promise = from_app_ipcs.get(mmid);
                        if (ipc_promise === undefined) {
                            /// 初始化互联
                            ipc_promise = (async () => {
                                const app = await app_mm.open(parsed_url.hostname);
                                let req_id = 0;
                                const allocReqId = () => req_id++;
                                const ipc = await app.connect(from_app);
                                const reqresMap = new Map();
                                /// 监听回调
                                ipc.onMessage((message) => {
                                    if (message instanceof ipc_1.IpcResponse) {
                                        const response_po = reqresMap.get(message.req_id);
                                        if (response_po) {
                                            reqresMap.delete(message.req_id);
                                            response_po.resolve(message);
                                        }
                                    }
                                });
                                ipc.onClose(() => {
                                    from_app_ipcs?.delete(mmid);
                                });
                                return {
                                    ipc,
                                    reqresMap,
                                    allocReqId,
                                };
                            })();
                            from_app_ipcs.set(mmid, ipc_promise);
                        }
                        return (async () => {
                            const { ipc, reqresMap, allocReqId } = await ipc_promise;
                            let body = "";
                            const method = request_init.method ?? "GET";
                            /// 读取 body
                            if (method === "POST" || method === "PUT") {
                                let buffer;
                                if (request_init.body instanceof ReadableStream) {
                                    const reader = request_init.body.getReader();
                                    const chunks = [];
                                    while (true) {
                                        const item = await reader.read();
                                        if (item.done) {
                                            break;
                                        }
                                        chunks.push(item.value);
                                    }
                                    buffer = Buffer.concat(chunks);
                                }
                                else if (request_init.body instanceof Blob) {
                                    buffer = Buffer.from(await request_init.body.arrayBuffer());
                                }
                                else if (ArrayBuffer.isView(request_init.body)) {
                                    buffer = Buffer.from(request_init.body.buffer, request_init.body.byteOffset, request_init.body.byteLength);
                                }
                                else if (request_init.body instanceof ArrayBuffer) {
                                    buffer = Buffer.from(request_init.body);
                                }
                                else if (typeof request_init.body === "string") {
                                    body = request_init.body;
                                }
                                else if (request_init.body) {
                                    throw new Error(`unsupport body type: ${request_init.body.constructor.name}`);
                                }
                                if (buffer !== undefined) {
                                    body = buffer.toString("base64");
                                }
                            }
                            /// 读取 headers
                            let headers = Object.create(null);
                            if (request_init.headers) {
                                let req_headers;
                                if (request_init.headers instanceof Array) {
                                    req_headers = new Headers(request_init.headers);
                                }
                                else if (request_init.headers instanceof Headers) {
                                    req_headers = request_init.headers;
                                }
                                else {
                                    headers = request_init.headers;
                                }
                                if (req_headers !== undefined) {
                                    req_headers.forEach((value, key) => {
                                        headers[key] = value;
                                    });
                                }
                            }
                            /// 注册回调
                            const req_id = allocReqId();
                            const response_po = new helper_1.PromiseOut();
                            reqresMap.set(req_id, response_po);
                            /// 发送
                            ipc.postMessage(new ipc_1.IpcRequest(req_id, method, parsed_url.href, body, headers));
                            const ipc_response = await response_po.promise;
                            return new Response(ipc_response.body, {
                                headers: ipc_response.headers,
                                status: ipc_response.statusCode,
                            });
                        })();
                    }
                }
            }
            return native_fetch(url, init);
        };
    }
    async _shutdown() {
        for (const mmid of this.running_apps.keys()) {
            await this.close(mmid);
        }
    }
    /** 安装应用 */
    install(mm) {
        this.apps.set(mm.mmid, mm);
    }
    /** 查询应用 */
    async query(mmid) {
        return this.apps.get(mmid);
    }
    /** 打开应用 */
    async open(mmid) {
        let app = this.running_apps.get(mmid);
        if (app === undefined) {
            const mm = await this.query(mmid);
            if (mm === undefined) {
                throw new Error(`no found app: ${mmid}`);
            }
            this.running_apps.set(mmid, mm);
            // @TODO bootstrap 函数应该是 $singleton 修饰
            await mm.bootstrap();
            app = mm;
        }
        return app;
    }
    /** 关闭应用 */
    async close(mmid) {
        const app = this.running_apps.get(mmid);
        if (app === undefined) {
            return -1;
        }
        try {
            this.running_apps.delete(mmid);
            await app.shutdown();
            return 0;
        }
        catch {
            return 1;
        }
    }
}
exports.AppNMM = AppNMM;
