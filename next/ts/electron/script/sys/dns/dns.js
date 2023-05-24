"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || function (mod) {
    if (mod && mod.__esModule) return mod;
    var result = {};
    if (mod != null) for (var k in mod) if (k !== "default" && Object.prototype.hasOwnProperty.call(mod, k)) __createBinding(result, mod, k);
    __setModuleDefault(result, mod);
    return result;
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.DnsNMM = void 0;
const IpcHeaders_js_1 = require("../../core/ipc/IpcHeaders.js");
const IpcResponse_js_1 = require("../../core/ipc/IpcResponse.js");
const micro_module_native_js_1 = require("../../core/micro-module.native.js");
const nativeConnect_js_1 = require("../../core/nativeConnect.js");
const _readRequestAsIpcRequest_js_1 = require("../../helper/$readRequestAsIpcRequest.js");
const devtools_js_1 = require("../../helper/devtools.js");
const mapHelper_js_1 = require("../../helper/mapHelper.js");
const PromiseOut_js_1 = require("../../helper/PromiseOut.js");
const nativeFetch_js_1 = require("./nativeFetch.js");
class MyDnsMicroModule {
    constructor(dnsNN, fromMM) {
        Object.defineProperty(this, "dnsNN", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: dnsNN
        });
        Object.defineProperty(this, "fromMM", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: fromMM
        });
    }
    install(mm) {
        this.dnsNN.install(mm);
    }
    uninstall(mm) {
        this.dnsNN.uninstall(mm);
    }
    connect(mmid, reason) {
        return this.dnsNN[connectTo_symbol](this.fromMM, mmid, reason ?? new Request(`file://${mmid}`));
    }
    query(mmid) {
        return this.dnsNN.query(mmid);
    }
}
class MyBootstrapContext {
    constructor(dns) {
        Object.defineProperty(this, "dns", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: dns
        });
    }
}
const connectTo_symbol = Symbol("connectTo");
/** DNS 服务，内核！
 * 整个系统都围绕这个 DNS 服务来展开互联
 */
class DnsNMM extends micro_module_native_js_1.NativeMicroModule {
    constructor() {
        super(...arguments);
        Object.defineProperty(this, "mmid", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: "dns.sys.dweb"
        });
        Object.defineProperty(this, "apps", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: new Map()
        });
        // 拦截 nativeFetch
        Object.defineProperty(this, "mmConnectsMap", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: new WeakMap()
        });
        Object.defineProperty(this, "running_apps", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: new Map()
        });
    }
    bootstrap(ctx = new MyBootstrapContext(new MyDnsMicroModule(this, this))) {
        return super.bootstrap(ctx);
    }
    bootstrapMicroModule(fromMM) {
        return fromMM.bootstrap(new MyBootstrapContext(new MyDnsMicroModule(this, fromMM)));
    }
    /**
     * 创建通过 MessageChannel 实现同行的 ipc
     * @param fromMM
     * @param toMmid
     * @param reason
     * @returns
     */
    async [connectTo_symbol](fromMM, toMmid, reason) {
        // v2.0
        // 创建连接
        const fromMMconnectsMap = mapHelper_js_1.mapHelper.getOrPut(this.mmConnectsMap, fromMM, () => new Map());
        const po = mapHelper_js_1.mapHelper.getOrPut(fromMMconnectsMap, toMmid, () => {
            const po = new PromiseOut_js_1.PromiseOut();
            (async () => {
                /// 与指定应用建立通讯
                const toMM = await this.open(toMmid);
                const result = await (0, nativeConnect_js_1.connectMicroModules)(fromMM, toMM, reason);
                const [ipcForFromMM, ipcForToMM] = result;
                // 监听生命周期 释放引用
                ipcForFromMM.onClose(() => {
                    fromMMconnectsMap?.delete(toMmid);
                });
                // 反向存储 toMM
                if (ipcForToMM) {
                    const result2 = [ipcForToMM, ipcForFromMM];
                    const toMMconnectsMap = mapHelper_js_1.mapHelper.getOrPut(this.mmConnectsMap, toMM, () => new Map());
                    mapHelper_js_1.mapHelper.getOrPut(toMMconnectsMap, fromMM.mmid, () => {
                        const toMMPromise = new PromiseOut_js_1.PromiseOut();
                        ipcForToMM.onClose(() => {
                            toMMconnectsMap?.delete(fromMM.mmid);
                        });
                        toMMPromise.resolve(result2);
                        return toMMPromise;
                    });
                }
                po.resolve(result);
            })();
            return po;
        });
        return po.promise;
    }
    _bootstrap() {
        devtools_js_1.log.green(`${this.mmid} _bootstrap`);
        this.install(this);
        this.running_apps.set(this.mmid, this);
        this.registerCommonIpcOnMessageHandler({
            pathname: "/open",
            matchMode: "full",
            input: { app_id: "mmid" },
            output: "boolean",
            handler: async (args, client_ipc, request) => {
                /// TODO 询问用户是否授权该行为
                const app = await this.open(args.app_id);
                return IpcResponse_js_1.IpcResponse.fromJson(request.req_id, 200, new IpcHeaders_js_1.IpcHeaders({
                    "Content-Type": "application/json; charset=UTF-8",
                }), JSON.stringify(app), client_ipc);
            },
        });
        this.registerCommonIpcOnMessageHandler({
            pathname: "/open_browser",
            matchMode: "full",
            input: { mmid: "mmid", root: "string", entry: "string" },
            output: "boolean",
            handler: async (args, client_ipc, request) => {
                const { JsMicroModule } = await Promise.resolve().then(() => __importStar(require("../jmm/micro-module.js.js")));
                const { JmmMetadata } = await Promise.resolve().then(() => __importStar(require("../jmm/JmmMetadata.js")));
                const metadata = new JmmMetadata({
                    id: args.mmid,
                    server: { root: args.root, entry: args.entry },
                });
                console.log("metadata: ", metadata);
                // 实例化
                // 安装
                this.install(new JsMicroModule(metadata));
                /// TODO 询问用户是否授权该行为
                const app = await this.open(args.mmid);
                return IpcResponse_js_1.IpcResponse.fromJson(request.req_id, 200, new IpcHeaders_js_1.IpcHeaders({
                    "Content-Type": "application/json; charset=UTF-8",
                }), JSON.stringify(app), client_ipc);
            },
        });
        this.registerCommonIpcOnMessageHandler({
            pathname: "/close",
            matchMode: "full",
            input: { app_id: "mmid" },
            output: "number",
            handler: async (args) => {
                /// TODO 关闭应用首先要确保该应用的 parentProcessId 在 processTree 中
                const n = await this.close(args.app_id);
                const result = await this.nativeFetch(`file://mwebview.sys.dweb/close/focused_window`);
                return n;
            },
        });
        // 检查工具 提供查询 mmConnectsMap  的结果
        this.registerCommonIpcOnMessageHandler({
            pathname: "/query/mm_connects_map",
            matchMode: "full",
            input: { app_id: "mmid" },
            output: "object",
            handler: async (args) => {
                const mm = await this.query(args.app_id);
                if (mm === undefined) {
                    throw new Error(`mm === undefined`);
                }
                const _map = this.mmConnectsMap.get(mm);
                return {};
            },
        });
        this.registerCommonIpcOnMessageHandler({
            pathname: "/restart",
            matchMode: "full",
            input: { app_id: "mmid" },
            output: "boolean",
            handler: async (args, ipc, request) => {
                // 需要停止匹配的 jsMicroModule
                const mm = await this.query(args.app_id);
                if (mm === undefined)
                    return false;
                this.close(args.app_id);
                // 关闭当前window对象
                const result = await this.nativeFetch(`file://mwebview.sys.dweb/close/focused_window`);
                this.install(mm);
                this.open(args.app_id);
                return true;
            },
        });
        this._after_shutdown_signal.listen(nativeFetch_js_1.nativeFetchAdaptersManager.append(async (fromMM, parsedUrl, requestInit) => {
            // 测试代码
            // Reflect.set(requestInit, "duplex", "half")
            if (parsedUrl.protocol === "file:" &&
                parsedUrl.hostname.endsWith(".dweb")) {
                const mmid = parsedUrl.hostname;
                const [ipc] = await this[connectTo_symbol](fromMM, mmid, new Request(parsedUrl, requestInit));
                const ipc_req_init = await (0, _readRequestAsIpcRequest_js_1.$readRequestAsIpcRequest)(requestInit);
                const ipc_response = await ipc.request(parsedUrl.href, ipc_req_init);
                return ipc_response.toResponse(parsedUrl.href);
            }
        }));
        //#region 启动引导程序
        return this.open(`boot.sys.dweb`);
        //#endregion
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
    /** 卸载应用 */
    uninstall(mm) {
        this.apps.delete(mm.mmid);
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
            await this.bootstrapMicroModule(mm);
            app = mm;
        }
        return app;
    }
    /** 关闭应用 */
    async close(mmid) {
        const app = this.running_apps.get(mmid);
        if (app === undefined) {
            // 关闭失败没有匹配的 microModule 运行
            return -1;
        }
        try {
            this.running_apps.delete(mmid);
            await app.shutdown();
            this.uninstall(app);
            // 关闭成功
            return 0;
        }
        catch {
            // 关闭失败
            return 1;
        }
    }
}
exports.DnsNMM = DnsNMM;
