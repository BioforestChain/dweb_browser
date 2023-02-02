"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.NativeMicroModule = void 0;
const helper_cjs_1 = require("./helper.cjs");
const ipc_cjs_1 = require("./ipc.cjs");
const ipc_native_cjs_1 = require("./ipc.native.cjs");
const micro_module_cjs_1 = require("./micro-module.cjs");
class NativeMicroModule extends micro_module_cjs_1.MicroModule {
    constructor() {
        super(...arguments);
        this._connectting_ipcs = new Set();
        /**
         * 内部程序与外部程序通讯的方法
         * TODO 这里应该是可以是多个
         */
        this._on_connect_cbs = new Set();
        ///
        this._commmon_ipc_on_message_hanlders = new Set();
        this._inited_commmon_ipc_on_message = false;
    }
    _connect() {
        const channel = new MessageChannel();
        const { port1, port2 } = channel;
        const inner_ipc = new ipc_native_cjs_1.NativeIpc(port2, this, "server" /* IPC_ROLE.SERVER */);
        this._connectting_ipcs.add(inner_ipc);
        inner_ipc.onClose(() => {
            this._connectting_ipcs.delete(inner_ipc);
        });
        this._emitConnect(inner_ipc);
        return new ipc_native_cjs_1.NativeIpc(port1, this, "client" /* IPC_ROLE.CLIENT */);
    }
    /**
     * 给内部程序自己使用的 onConnect，外部与内部建立连接时使用
     * 因为 NativeMicroModule 的内部程序在这里编写代码，所以这里会提供 onConnect 方法
     * 如果时 JsMicroModule 这个 onConnect 就是写在 WebWorker 那边了
     */
    onConnect(cb) {
        this._on_connect_cbs.add(cb);
        return () => this._on_connect_cbs.delete(cb);
    }
    _emitConnect(ipc) {
        for (const cb of this._on_connect_cbs) {
            cb(ipc);
        }
    }
    after_shutdown() {
        super.after_shutdown();
        for (const inner_ipc of this._connectting_ipcs) {
            inner_ipc.close();
        }
        this._connectting_ipcs.clear();
    }
    _initCommmonIpcOnMessage() {
        if (this._inited_commmon_ipc_on_message) {
            return;
        }
        this._inited_commmon_ipc_on_message = true;
        this.onConnect((client_ipc) => {
            client_ipc.onMessage(async (request) => {
                if (request.type !== 0 /* IPC_DATA_TYPE.REQUEST */) {
                    return;
                }
                const { pathname } = request.parsed_url;
                let response;
                for (const hanlder_schema of this._commmon_ipc_on_message_hanlders) {
                    if (hanlder_schema.matchMode === "full"
                        ? pathname === hanlder_schema.pathname
                        : hanlder_schema.matchMode === "prefix"
                            ? pathname.startsWith(hanlder_schema.pathname)
                            : false) {
                        try {
                            const result = await hanlder_schema.hanlder(hanlder_schema.input(request), client_ipc);
                            if (result instanceof ipc_cjs_1.IpcResponse) {
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
                            response = ipc_cjs_1.IpcResponse.fromJson(request.req_id, 500, body);
                        }
                        break;
                    }
                }
                if (response === undefined) {
                    response = ipc_cjs_1.IpcResponse.fromText(request.req_id, 404, `no found hanlder for '${pathname}'`);
                }
                client_ipc.postMessage(response);
            });
        });
    }
    registerCommonIpcOnMessageHanlder(common_hanlder_schema) {
        this._initCommmonIpcOnMessage();
        const hanlders = this._commmon_ipc_on_message_hanlders;
        const custom_handler_schema = {
            ...common_hanlder_schema,
            input: (0, helper_cjs_1.$deserializeRequestToParams)(common_hanlder_schema.input),
            output: (0, helper_cjs_1.$serializeResultToResponse)(common_hanlder_schema.output),
        };
        /// 初始化
        hanlders.add(custom_handler_schema);
        return () => hanlders.delete(custom_handler_schema);
    }
}
exports.NativeMicroModule = NativeMicroModule;
