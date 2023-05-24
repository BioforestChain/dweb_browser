import { IpcMessage, IPC_MESSAGE_TYPE } from "./const.js";
export declare class IpcStreamPulling extends IpcMessage<IPC_MESSAGE_TYPE.STREAM_PULLING> {
    readonly stream_id: string;
    /**
     * 带宽限制, 0 代表不限速。
     * 负数代表暂停，但对于数据暂停，一般使用 Paused 指令。
     * 如果出现负数，往往是代表对方的数据处理出现了阻塞，与 Paused 不同，Paused 代表的是逻辑上的暂停，可能是挂起去处理其它事情去了，
     * 而负数的带宽代表物理意义上的阻塞，此时更不该再发送更多的数据过去
     */
    readonly bandwidth: number;
    constructor(stream_id: string, bandwidth?: number | null);
}
