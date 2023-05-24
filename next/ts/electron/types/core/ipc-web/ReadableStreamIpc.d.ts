import type { $IpcMicroModuleInfo, $IpcSupportProtocols, $PromiseMaybe } from "../../helper/types.js";
import type { $IpcMessage, IPC_ROLE } from "../ipc/const.js";
import { Ipc } from "../ipc/ipc.js";
/**
 * 基于 WebReadableStream 的IPC
 *
 * 它会默认构建出一个输出流，
 * 以及需要手动绑定输入流 {@link bindIncomeStream}
 */
export declare class ReadableStreamIpc extends Ipc {
    #private;
    readonly remote: $IpcMicroModuleInfo;
    readonly role: IPC_ROLE;
    readonly self_support_protocols: $IpcSupportProtocols;
    constructor(remote: $IpcMicroModuleInfo, role: IPC_ROLE, self_support_protocols?: $IpcSupportProtocols);
    /** 这是输出流，给外部读取用的 */
    get stream(): ReadableStream<Uint8Array>;
    get controller(): ReadableStreamDefaultController<Uint8Array>;
    private PONG_DATA;
    private _incomne_stream?;
    /**
     * 输入流要额外绑定
     * 注意，非必要不要 await 这个promise
     */
    bindIncomeStream(stream: $PromiseMaybe<ReadableStream<Uint8Array>>): Promise<void>;
    private _len;
    private _len_u8a;
    _doPostMessage(message: $IpcMessage): void;
    _doClose(): void;
}
