import { $ReqMatcher } from "../helper/$ReqMatcher.js";
import type { $IpcSupportProtocols, $PromiseMaybe, $Schema1, $Schema1ToType, $Schema2, $Schema2ToType } from "../helper/types.js";
import { Ipc, IpcRequest, IpcResponse } from "./ipc/index.js";
import { MicroModule } from "./micro-module.js";
export declare abstract class NativeMicroModule extends MicroModule {
    readonly ipc_support_protocols: $IpcSupportProtocols;
    abstract mmid: `${string}.${"sys" | "std"}.dweb`;
    _onConnect(ipc: Ipc): void;
    private _commmon_ipc_on_message_hanlders;
    private _inited_commmon_ipc_on_message;
    private _initCommmonIpcOnMessage;
    protected registerCommonIpcOnMessageHandler<I extends $Schema1, O extends $Schema2>(common_hanlder_schema: $RequestCommonHanlderSchema<I, O>): () => boolean;
}
interface $RequestHanlderSchema<ARGS, RES> extends $ReqMatcher {
    readonly handler: (args: ARGS, client_ipc: Ipc, ipc_request: IpcRequest) => $PromiseMaybe<RES | IpcResponse>;
}
export interface $RequestCommonHanlderSchema<I extends $Schema1, O extends $Schema2> extends $RequestHanlderSchema<$Schema1ToType<I>, $Schema2ToType<O>> {
    readonly input: I;
    readonly output: O;
}
export interface $RequestCustomHanlderSchema<ARGS = unknown, RES = unknown> extends $RequestHanlderSchema<ARGS, RES> {
    readonly input: (request: IpcRequest) => ARGS;
    readonly output: (request: IpcRequest, result: RES, ipc: Ipc) => $PromiseMaybe<IpcResponse>;
}
export {};
