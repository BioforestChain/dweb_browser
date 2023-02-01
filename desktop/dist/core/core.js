"use strict";
// class DwebDNS {
//    readonly map = new Map<string, new () => MicroModule>();
//    async query(domain: string) {
//       return this.map.get(domain);
//    }
// }
var __classPrivateFieldGet = (this && this.__classPrivateFieldGet) || function (receiver, state, kind, f) {
    if (kind === "a" && !f) throw new TypeError("Private accessor was defined without a getter");
    if (typeof state === "function" ? receiver !== state || !f : !state.has(receiver)) throw new TypeError("Cannot read private member from an object whose class did not declare it");
    return kind === "m" ? f : kind === "a" ? f.call(receiver) : f ? f.value : state.get(receiver);
};
var __classPrivateFieldSet = (this && this.__classPrivateFieldSet) || function (receiver, state, value, kind, f) {
    if (kind === "m") throw new TypeError("Private method is not writable");
    if (kind === "a" && !f) throw new TypeError("Private accessor was defined without a setter");
    if (typeof state === "function" ? receiver !== state || !f : !state.has(receiver)) throw new TypeError("Cannot write private member to an object whose class did not declare it");
    return (kind === "a" ? f.call(receiver, value) : f ? f.value = value : state.set(receiver, value)), value;
};
var _IpcRequest_parsed_url, _IpcResponse_request;
const $typeNameParser = (key, typeName2, value) => {
    let param;
    if (value === null) {
        if (typeName2.endsWith("?")) {
            throw new Error(`param type error: '${key}'.`);
        }
        else {
            param = undefined;
        }
    }
    else {
        const typeName1 = (typeName2.endsWith("?") ? typeName2.slice(0, -1) : typeName2);
        switch (typeName1) {
            case "number": {
                param = +value;
            }
            case "boolean": {
                param = value === "" ? false : Boolean(value.toLowerCase());
            }
            case "mmid": {
                if (value.endsWith(".dweb") === false) {
                    throw new Error(`param mmid type error: '${key}':'${value}'`);
                }
                param = value;
            }
            case "string": {
                param = value;
            }
            default:
                param = void 0;
        }
    }
    return param;
};
const $deserializeRequestToParams = (schema) => {
    return (request) => {
        const url = request.parsed_url;
        const params = {};
        for (const [key, typeName2] of Object.entries(schema)) {
            params[key] = $typeNameParser(key, typeName2, url.searchParams.get(key));
        }
        return params;
    };
};
/**
 * @TODO 实现模式匹配
 */
const $serializeResultToResponse = (schema) => {
    return (request, result) => {
        return new IpcResponse(request, 200, JSON.stringify(result), {
            "Content-Type": "application/json",
        });
    };
};
class MicroModule {
    constructor() {
        this._commmon_ipc_on_message_hanlders = new Set();
        this._inited_commmon_ipc_on_message = false;
    }
    _initCommmonIpcOnMessage() {
        if (this._inited_commmon_ipc_on_message) {
            return;
        }
        this._inited_commmon_ipc_on_message = true;
        this.onConnect((ipc) => {
            ipc.onMessage(async (request) => {
                if (request.type !== 0 /* IPC_DATA_TYPE.REQUEST */) {
                    return;
                }
                const { pathname } = request.parsed_url;
                let response;
                for (const hanlder_schema of this
                    ._commmon_ipc_on_message_hanlders) {
                    if (hanlder_schema.matchMode === "full"
                        ? pathname === hanlder_schema.pathname
                        : hanlder_schema.matchMode === "prefix"
                            ? pathname.startsWith(hanlder_schema.pathname)
                            : false) {
                        try {
                            const result = await hanlder_schema.hanlder(hanlder_schema.input(request));
                            if (result instanceof IpcResponse) {
                                response = result;
                            }
                            else {
                                response = hanlder_schema.output(request, result);
                            }
                        }
                        catch (err) {
                            let body;
                            if (err instanceof Error) {
                                body = err.message;
                            }
                            else {
                                body = String(err);
                            }
                            response = new IpcResponse(request, 500, body, {
                                "Content-Type": "text/plain",
                            });
                        }
                    }
                }
                if (response === undefined) {
                    response = response = new IpcResponse(request, 404, `no found hanlder for '${pathname}'`, {
                        "Content-Type": "text/plain",
                    });
                }
                ipc.postMessage(response);
            });
        });
    }
    registerCommonIpcOnMessageHanlder(common_hanlder_schema) {
        this._initCommmonIpcOnMessage();
        const hanlders = this._commmon_ipc_on_message_hanlders;
        const custom_handler_schema = {
            ...common_hanlder_schema,
            input: $deserializeRequestToParams(common_hanlder_schema.input),
            output: $serializeResultToResponse(common_hanlder_schema.output),
        };
        /// 初始化
        hanlders.add(custom_handler_schema);
        return () => hanlders.delete(custom_handler_schema);
    }
}
class IpcRequest {
    constructor(req_id, method, url, body, headers) {
        this.req_id = req_id;
        this.method = method;
        this.url = url;
        this.body = body;
        this.headers = headers;
        this.type = 0 /* IPC_DATA_TYPE.REQUEST */;
        _IpcRequest_parsed_url.set(this, void 0);
    }
    get parsed_url() {
        return (__classPrivateFieldSet(this, _IpcRequest_parsed_url, __classPrivateFieldGet(this, _IpcRequest_parsed_url, "f") ?? new URL(this.url), "f"));
    }
}
_IpcRequest_parsed_url = new WeakMap();
class IpcResponse {
    constructor(request, statusCode, body, headers) {
        this.statusCode = statusCode;
        this.body = body;
        this.headers = headers;
        this.type = 1 /* IPC_DATA_TYPE.RESPONSE */;
        _IpcResponse_request.set(this, void 0);
        __classPrivateFieldSet(this, _IpcResponse_request, request, "f");
        this.req_id = request.req_id;
    }
}
_IpcResponse_request = new WeakMap();
class Ipc {
}
class NativeIpc extends Ipc {
    constructor(port) {
        super();
        this.port = port;
        this._cbs = new Set();
        port.addEventListener("message", (event) => {
            const { req_id, method, url, body, headers } = event.data;
            const request = new IpcRequest(req_id, method, url, body, headers);
            for (const cb of this._cbs) {
                cb(request);
            }
        });
    }
    postMessage(request) {
        this.port.postMessage(JSON.stringify(request));
    }
    onMessage(cb) {
        this._cbs.add(cb);
        return () => this._cbs.delete(cb);
    }
}
class NativeMicroModule extends MicroModule {
    constructor() {
        super(...arguments);
        this._on_connect_cbs = new Set();
        this._channel = new MessageChannel();
        this.ipc = new NativeIpc(this._channel.port1);
        this.inner_ipc = new NativeIpc(this._channel.port2);
    }
    connect() {
        const channel = new MessageChannel();
        const { port1, port2 } = channel;
        for (const cb of this._on_connect_cbs) {
            const inner_ipc = new NativeIpc(port2);
            cb(inner_ipc);
        }
        return new NativeIpc(port1);
    }
    onConnect(cb) {
        this._on_connect_cbs.add(cb);
        return () => this._on_connect_cbs.delete(cb);
    }
}
/** 内核，原 DNS 服务 */
class CoreNMM extends NativeMicroModule {
    constructor() {
        super(...arguments);
        this.mmid = "core.sys.dweb";
        this.apps = new Map();
        this.running = new Map();
    }
    bootstrap() {
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
    }
    async destroy() {
        for (const mmid of this.running.keys()) {
            await this.close(mmid);
        }
    }
    /** 查询应用 */
    async query(mmid) {
        return this.apps.get(mmid);
    }
    /** 打开应用 */
    async open(mmid) {
        let app = this.running.get(mmid);
        if (app === undefined) {
            const MM = await this.query(mmid);
            if (MM === undefined) {
                throw new Error(`no found app: ${mmid}`);
            }
            app = new MM();
            this.running.get(mmid);
        }
        return app;
    }
    /** 关闭应用 */
    async close(mmid) {
        const app = this.running.get(mmid);
        if (app === undefined) {
            return -1;
        }
        try {
            await app.destroy();
            return 0;
        }
        catch {
            return 1;
        }
    }
}
const core = new CoreNMM();
class BootNMM extends NativeMicroModule {
    constructor() {
        super(...arguments);
        this.mmid = "boot.sys.dweb";
        this.registeredMmids = new Set(["desktop.sys.dweb"]);
        // $Routers: {
        //    "/register": IO<mmid, boolean>;
        //    "/unregister": IO<mmid, boolean>;
        // };
    }
    async bootstrap() {
        for (const mmid of this.registeredMmids) {
            /// TODO 这里应该使用总线进行通讯，而不是拿到core直接调用。在未来分布式系统中，core模块可能是远程模块
            await core.open(mmid);
        }
        this.registerCommonIpcOnMessageHanlder({
            pathname: "/register",
            matchMode: "full",
            input: { app_id: "mmid" },
            output: "boolean",
            hanlder: async (args) => {
                return await this.register(args.app_id);
            },
        });
        this.registerCommonIpcOnMessageHanlder({
            pathname: "/unregister",
            matchMode: "full",
            input: { app_id: "mmid" },
            output: "boolean",
            hanlder: async (args) => {
                return await this.unregister(args.app_id);
            },
        });
    }
    destroy() { }
    register(mmid) {
        /// TODO 这里应该有用户授权，允许开机启动
        this.registeredMmids.add(mmid);
        return true;
    }
    unregister(mmid) {
        /// TODO 这里应该有用户授权，取消开机启动
        return this.registeredMmids.delete(mmid);
    }
}
class JsMicroModule extends MicroModule {
    bootstrap() {
        throw new Error("Method not implemented.");
    }
    destroy() {
        throw new Error("Method not implemented.");
    }
    connect() {
        throw new Error("Method not implemented.");
    }
    onConnect(cb) {
        throw new Error("Method not implemented.");
    }
}
/**
 * > 在NW.js里，JsIpc几乎等价于 NativeIPC，都是使用原生的 MessagePort 即可
 * 差别只在于 JsIpc 需要传入到运作JS线程池的 WebDocument 中，传递给指定的Worker
 */
class JsIpc extends Ipc {
    constructor(port) {
        super();
        this.port = port;
        this._cbs = new Set();
        port.addEventListener("message", (event) => {
            const { req_id, method, url, body, headers } = event.data;
            const request = new IpcRequest(req_id, method, url, body, headers);
            for (const cb of this._cbs) {
                cb(request);
            }
        });
    }
    postMessage(request) {
        this.port.postMessage(JSON.stringify(request));
    }
    onMessage(cb) {
        this._cbs.add(cb);
        return () => this._cbs.delete(cb);
    }
}
class DesktopJMM extends JsMicroModule {
    constructor() {
        super(...arguments);
        this.mmid = "desktop.sys.dweb";
    }
}
console.log("qaq");
// JsProcessManager.singleton();
// setTimeout(() => {
//    nw.Window.open("index.html", {}, function (win) {});
// }, 1000);
