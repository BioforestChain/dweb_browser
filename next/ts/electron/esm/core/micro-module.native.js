import chalk from "chalk";
import { $deserializeRequestToParams } from "../helper/$deserializeRequestToParams.js";
import { $isMatchReq } from "../helper/$ReqMatcher.js";
import { $serializeResultToResponse } from "../helper/$serializeResultToResponse.js";
import { IpcResponse } from "./ipc/index.js";
import { MicroModule } from "./micro-module.js";
// import { connectAdapterManager } from "./nativeConnect.ts";
// connectAdapterManager.append((fromMM, toMM, reason) => {
//   // // 原始代码
//   // if (toMM instanceof NativeMicroModule) {
//   //   const channel = new MessageChannel();
//   //   const { port1, port2 } = channel;
//   //   const toNativeIpc = new NativeIpc(port1, fromMM, IPC_ROLE.SERVER);
//   //   const fromNativeIpc = new NativeIpc(port2, toMM, IPC_ROLE.CLIENT);
//   //   fromMM.beConnect(fromNativeIpc, reason); // 通知发起连接者作为Client
//   //   toMM.beConnect(toNativeIpc, reason); // 通知接收者作为Server
//   //   return [fromNativeIpc, toNativeIpc];
//   // }
//   // 测试代码
//   const channel = new MessageChannel();
//   const { port1, port2 } = channel;
//   const toNativeIpc = new NativeIpc(port1, fromMM, IPC_ROLE.SERVER);
//   const fromNativeIpc = new NativeIpc(port2, toMM, IPC_ROLE.CLIENT);
//   fromMM.beConnect(fromNativeIpc, reason); // 通知发起连接者作为Client
//   toMM.beConnect(toNativeIpc, reason); // 通知接收者作为Server
//   return [fromNativeIpc, toNativeIpc];
// });
export class NativeMicroModule extends MicroModule {
    constructor() {
        super(...arguments);
        Object.defineProperty(this, "ipc_support_protocols", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: {
                message_pack: true,
                protobuf: true,
                raw: true,
            }
        });
        Object.defineProperty(this, "_commmon_ipc_on_message_hanlders", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: new Set()
        });
        Object.defineProperty(this, "_inited_commmon_ipc_on_message", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: false
        });
    }
    _onConnect(ipc) { }
    ;
    _initCommmonIpcOnMessage() {
        if (this._inited_commmon_ipc_on_message) {
            return;
        }
        this._inited_commmon_ipc_on_message = true;
        this.onConnect((client_ipc) => {
            this._onConnect(client_ipc);
            client_ipc.onRequest(async (request) => {
                const { pathname } = request.parsed_url;
                let response;
                // 添加了一个判断 如果没有注册匹配请求的监听器会有信息弹出到 终端;
                let has = false;
                for (const hanlder_schema of this._commmon_ipc_on_message_hanlders) {
                    if ($isMatchReq(hanlder_schema, pathname, request.method)) {
                        has = true;
                        try {
                            const result = await hanlder_schema.handler(hanlder_schema.input(request), client_ipc, request);
                            if (result instanceof IpcResponse) {
                                response = result;
                            }
                            else {
                                response = await hanlder_schema.output(request, result, client_ipc);
                            }
                        }
                        catch (err) {
                            let body;
                            if (err instanceof Error) {
                                body = err.stack ?? err.message;
                            }
                            else {
                                body = String(err);
                            }
                            response = IpcResponse.fromJson(request.req_id, 500, undefined, body, client_ipc);
                        }
                        break;
                    }
                }
                if (!has) {
                    /** 没有匹配的事件处理器 弹出终端 优化了开发体验 */
                    console.log(chalk.red("[micro-module.native.cts 没有匹配的注册方法 mmid===]", this.mmid), "请求的方法是", request);
                }
                if (response === undefined) {
                    response = IpcResponse.fromText(request.req_id, 404, undefined, `no found hanlder for '${pathname}'`, client_ipc);
                }
                client_ipc.postMessage(response);
            });
        });
    }
    registerCommonIpcOnMessageHandler(common_hanlder_schema) {
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
