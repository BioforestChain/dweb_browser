"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.MicroModule = void 0;
const helper_1 = require("./helper");
const ipc_1 = require("./ipc");
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
                            if (result instanceof ipc_1.IpcResponse) {
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
                            response = new ipc_1.IpcResponse(request, 500, body, {
                                "Content-Type": "text/plain",
                            });
                        }
                    }
                }
                if (response === undefined) {
                    response = response = new ipc_1.IpcResponse(request, 404, `no found hanlder for '${pathname}'`, {
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
            input: (0, helper_1.$deserializeRequestToParams)(common_hanlder_schema.input),
            output: (0, helper_1.$serializeResultToResponse)(common_hanlder_schema.output),
        };
        /// 初始化
        hanlders.add(custom_handler_schema);
        return () => hanlders.delete(custom_handler_schema);
    }
}
exports.MicroModule = MicroModule;
