import { $Callback } from "../../helper/createSignal.js";
import type { Ipc } from "./ipc.js";
import { BodyHub, IpcBody, type $BodyData } from "./IpcBody.js";
import { MetaBody } from "./MetaBody.js";
export declare class IpcBodySender extends IpcBody {
    readonly data: $BodyData;
    private readonly ipc;
    static from(data: $BodyData, ipc: Ipc): IpcBody;
    constructor(data: $BodyData, ipc: Ipc);
    readonly isStream: boolean;
    private streamCtorSignal;
    /**
     * 被哪些 ipc 所真正使用，使用的进度分别是多少
     *
     * 这个进度 用于 类似流的 多发
     */
    private readonly usedIpcMap;
    private UsedIpcInfo;
    /**
     * 绑定使用
     */
    private useByIpc;
    /**
     * 拉取数据
     */
    private emitStreamPull;
    /**
     * 暂停数据
     */
    private emitStreamPaused;
    /**
     * 解绑使用
     */
    private emitStreamAborted;
    private readonly closeSignal;
    onStreamClose(cb: $Callback): import("../../helper/createSignal.js").$OffListener;
    private readonly openSignal;
    onStreamOpen(cb: $Callback): import("../../helper/createSignal.js").$OffListener;
    private _isStreamOpened;
    get isStreamOpened(): boolean;
    set isStreamOpened(value: boolean);
    private _isStreamClosed;
    get isStreamClosed(): boolean;
    set isStreamClosed(value: boolean);
    private emitStreamClose;
    protected _bodyHub: BodyHub;
    readonly metaBody: MetaBody;
    private $bodyAsMeta;
    /**
     * 如果 rawData 是流模式，需要提供数据发送服务
     *
     * 这里不会一直无脑发，而是对方有需要的时候才发
     * @param stream_id
     * @param stream
     * @param ipc
     */
    private $streamAsMeta;
    /**
     * ipc 将会使用它
     */
    static $usableByIpc: (ipc: Ipc, ipcBody: IpcBodySender) => void;
}
export declare const setStreamId: (stream: ReadableStream<Uint8Array>, cid: string) => string;
