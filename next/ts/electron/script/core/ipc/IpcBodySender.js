"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.setStreamId = exports.IpcBodySender = void 0;
const createSignal_js_1 = require("../../helper/createSignal.js");
const PromiseOut_js_1 = require("../../helper/PromiseOut.js");
const readableStreamHelper_js_1 = require("../../helper/readableStreamHelper.js");
const const_js_1 = require("./const.js");
const IpcBody_js_1 = require("./IpcBody.js");
const IpcStreamData_js_1 = require("./IpcStreamData.js");
const IpcStreamEnd_js_1 = require("./IpcStreamEnd.js");
const MetaBody_js_1 = require("./MetaBody.js");
/**
 * 控制信号
 */
var STREAM_CTOR_SIGNAL;
(function (STREAM_CTOR_SIGNAL) {
    STREAM_CTOR_SIGNAL[STREAM_CTOR_SIGNAL["PULLING"] = 0] = "PULLING";
    STREAM_CTOR_SIGNAL[STREAM_CTOR_SIGNAL["PAUSED"] = 1] = "PAUSED";
    STREAM_CTOR_SIGNAL[STREAM_CTOR_SIGNAL["ABORTED"] = 2] = "ABORTED";
})(STREAM_CTOR_SIGNAL || (STREAM_CTOR_SIGNAL = {}));
class IpcBodySender extends IpcBody_js_1.IpcBody {
    static from(data, ipc) {
        if (typeof data !== "string") {
            const cache = IpcBody_js_1.IpcBody.wm.get(data);
            if (cache !== undefined) {
                return cache;
            }
        }
        return new IpcBodySender(data, ipc);
    }
    constructor(data, ipc) {
        super();
        Object.defineProperty(this, "data", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: data
        });
        Object.defineProperty(this, "ipc", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: ipc
        });
        Object.defineProperty(this, "isStream", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: this.data instanceof ReadableStream
        });
        Object.defineProperty(this, "streamCtorSignal", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: (0, createSignal_js_1.createSignal)()
        });
        /**
         * 被哪些 ipc 所真正使用，使用的进度分别是多少
         *
         * 这个进度 用于 类似流的 多发
         */
        Object.defineProperty(this, "usedIpcMap", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: new Map()
        });
        Object.defineProperty(this, "UsedIpcInfo", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: class UsedIpcInfo {
                constructor(ipcBody, ipc, bandwidth = 0, fuse = 0) {
                    Object.defineProperty(this, "ipcBody", {
                        enumerable: true,
                        configurable: true,
                        writable: true,
                        value: ipcBody
                    });
                    Object.defineProperty(this, "ipc", {
                        enumerable: true,
                        configurable: true,
                        writable: true,
                        value: ipc
                    });
                    Object.defineProperty(this, "bandwidth", {
                        enumerable: true,
                        configurable: true,
                        writable: true,
                        value: bandwidth
                    });
                    Object.defineProperty(this, "fuse", {
                        enumerable: true,
                        configurable: true,
                        writable: true,
                        value: fuse
                    });
                }
                emitStreamPull(message) {
                    return this.ipcBody.emitStreamPull(this, message);
                }
                emitStreamPaused(message) {
                    return this.ipcBody.emitStreamPaused(this, message);
                }
                emitStreamAborted() {
                    return this.ipcBody.emitStreamAborted(this);
                }
            }
        });
        Object.defineProperty(this, "closeSignal", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: (0, createSignal_js_1.createSignal)()
        });
        Object.defineProperty(this, "openSignal", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: (0, createSignal_js_1.createSignal)()
        });
        Object.defineProperty(this, "_isStreamOpened", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: false
        });
        Object.defineProperty(this, "_isStreamClosed", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: false
        });
        /// bodyAsMeta
        Object.defineProperty(this, "_bodyHub", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: new IpcBody_js_1.BodyHub(this.data)
        });
        Object.defineProperty(this, "metaBody", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: this.$bodyAsMeta(this.data, this.ipc)
        });
        if (typeof data !== "string") {
            IpcBody_js_1.IpcBody.wm.set(data, this);
        }
        /// 作为 "生产者"，第一持有这个 IpcBodySender
        IpcBodySender.$usableByIpc(ipc, this);
    }
    /**
     * 绑定使用
     */
    useByIpc(ipc) {
        const info = this.usedIpcMap.get(ipc);
        if (info !== undefined) {
            return info;
        }
        /// 如果是未开启的流，插入
        if (this.isStream && !this._isStreamOpened) {
            const info = new this.UsedIpcInfo(this, ipc);
            this.usedIpcMap.set(ipc, info);
            this.closeSignal.listen(() => {
                this.emitStreamAborted(info);
            });
            return info;
        }
    }
    /**
     * 拉取数据
     */
    emitStreamPull(info, message) {
        /// desiredSize 仅作参考，我们以发过来的拉取次数为准
        info.bandwidth = message.bandwidth;
        // 只要有一个开始读取，那么就可以开始
        this.streamCtorSignal.emit(STREAM_CTOR_SIGNAL.PULLING);
    }
    /**
     * 暂停数据
     */
    emitStreamPaused(info, message) {
        /// 更新保险限制
        info.bandwidth = -1;
        info.fuse = message.fuse;
        /// 如果所有的读取者都暂停了，那么就触发暂停
        let paused = true;
        for (const info of this.usedIpcMap.values()) {
            if (info.bandwidth >= 0) {
                paused = false;
                break;
            }
        }
        if (paused) {
            this.streamCtorSignal.emit(STREAM_CTOR_SIGNAL.PAUSED);
        }
    }
    /**
     * 解绑使用
     */
    emitStreamAborted(info) {
        if (this.usedIpcMap.delete(info.ipc) != null) {
            /// 如果没有任何消费者了，那么真正意义上触发 abort
            if (this.usedIpcMap.size === 0) {
                this.streamCtorSignal.emit(STREAM_CTOR_SIGNAL.ABORTED);
            }
        }
    }
    onStreamClose(cb) {
        return this.closeSignal.listen(cb);
    }
    onStreamOpen(cb) {
        return this.openSignal.listen(cb);
    }
    get isStreamOpened() {
        return this._isStreamOpened;
    }
    set isStreamOpened(value) {
        if (this._isStreamOpened !== value) {
            this._isStreamOpened = value;
            if (value) {
                this.openSignal.emit();
                this.openSignal.clear();
            }
        }
    }
    get isStreamClosed() {
        return this._isStreamClosed;
    }
    set isStreamClosed(value) {
        if (this._isStreamClosed !== value) {
            this._isStreamClosed = value;
            if (value) {
                this.closeSignal.emit();
                this.closeSignal.clear();
            }
        }
    }
    emitStreamClose() {
        this.isStreamOpened = true;
        this.isStreamClosed = true;
    }
    $bodyAsMeta(body, ipc) {
        if (typeof body === "string") {
            return MetaBody_js_1.MetaBody.fromText(ipc.uid, body);
        }
        if (body instanceof ReadableStream) {
            return this.$streamAsMeta(body, ipc);
        }
        return MetaBody_js_1.MetaBody.fromBinary(ipc, body);
    }
    /**
     * 如果 rawData 是流模式，需要提供数据发送服务
     *
     * 这里不会一直无脑发，而是对方有需要的时候才发
     * @param stream_id
     * @param stream
     * @param ipc
     */
    $streamAsMeta(stream, ipc) {
        const stream_id = getStreamId(stream);
        const reader = (0, readableStreamHelper_js_1.binaryStreamRead)(stream);
        (async () => {
            /**
             * 流的使用锁(Future 锁)
             * 只有等到 Pulling 指令的时候才能读取并发送
             */
            let pullingLock = new PromiseOut_js_1.PromiseOut();
            this.streamCtorSignal.listen(async (signal) => {
                switch (signal) {
                    case STREAM_CTOR_SIGNAL.PULLING: {
                        pullingLock.resolve();
                        break;
                    }
                    case STREAM_CTOR_SIGNAL.PAUSED: {
                        if (pullingLock.is_finished) {
                            pullingLock = new PromiseOut_js_1.PromiseOut();
                        }
                        break;
                    }
                    case STREAM_CTOR_SIGNAL.ABORTED: {
                        /// stream 现在在 locked 状态，binaryStreamRead 的 reutrn 可以释放它的 locked
                        await reader.return();
                        /// 然后取消流的读取
                        await stream.cancel();
                        this.emitStreamClose();
                    }
                }
            });
            /// 持续发送数据
            while (true) {
                // 等待流开始被拉取
                await pullingLock.promise;
                // const desiredSize = this.maxPulledSize - this.curPulledSize;
                const availableLen = await reader.available();
                if (availableLen > 0) {
                    // 开光了，流已经开始被读取
                    this.isStreamOpened = true;
                    const message = IpcStreamData_js_1.IpcStreamData.fromBinary(stream_id, await reader.readBinary(availableLen));
                    for (const ipc of this.usedIpcMap.keys()) {
                        ipc.postMessage(message);
                    }
                }
                else if (availableLen === -1) {
                    /// 不论是不是被 aborted，都发送结束信号
                    const message = new IpcStreamEnd_js_1.IpcStreamEnd(stream_id);
                    for (const ipc of this.usedIpcMap.keys()) {
                        ipc.postMessage(message);
                    }
                    this.emitStreamClose();
                    break;
                }
            }
        })().catch(console.error);
        let streamType = MetaBody_js_1.IPC_META_BODY_TYPE.STREAM_ID;
        let streamFirstData = "";
        if ("preReadableSize" in stream &&
            typeof stream.preReadableSize === "number" &&
            stream.preReadableSize > 0) {
            // js的不支持输出预读取帧
        }
        return new MetaBody_js_1.MetaBody(streamType, ipc.uid, streamFirstData, stream_id);
    }
}
/**
 * ipc 将会使用它
 */
Object.defineProperty(IpcBodySender, "$usableByIpc", {
    enumerable: true,
    configurable: true,
    writable: true,
    value: (ipc, ipcBody) => {
        if (ipcBody.isStream && !ipcBody._isStreamOpened) {
            const streamId = ipcBody.metaBody.streamId;
            let usableIpcBodyMapper = IpcUsableIpcBodyMap.get(ipc);
            if (usableIpcBodyMapper === undefined) {
                const mapper = new UsableIpcBodyMapper();
                mapper.onDestroy(ipc.onStream((message) => {
                    switch (message.type) {
                        case const_js_1.IPC_MESSAGE_TYPE.STREAM_PULLING:
                            mapper
                                .get(message.stream_id)
                                ?.useByIpc(ipc)
                                ?.emitStreamPull(message);
                            break;
                        case const_js_1.IPC_MESSAGE_TYPE.STREAM_PAUSED:
                            mapper
                                .get(message.stream_id)
                                ?.useByIpc(ipc)
                                ?.emitStreamPaused(message);
                            break;
                        case const_js_1.IPC_MESSAGE_TYPE.STREAM_ABORT:
                            mapper
                                .get(message.stream_id)
                                ?.useByIpc(ipc)
                                ?.emitStreamAborted();
                            break;
                    }
                }));
                mapper.onDestroy(() => IpcUsableIpcBodyMap.delete(ipc));
                usableIpcBodyMapper = mapper;
            }
            if (usableIpcBodyMapper.add(streamId, ipcBody)) {
                // 一个流一旦关闭，那么就将不再会与它有主动通讯上的可能
                ipcBody.onStreamClose(() => usableIpcBodyMapper.remove(streamId));
            }
        }
    }
});
exports.IpcBodySender = IpcBodySender;
const streamIdWM = new WeakMap();
let stream_id_acc = 0;
const getStreamId = (stream) => {
    let id = streamIdWM.get(stream);
    if (id === undefined) {
        id = `rs-${stream_id_acc++}`;
        streamIdWM.set(stream, id);
    }
    return id;
};
const setStreamId = (stream, cid) => {
    let id = streamIdWM.get(stream);
    if (id === undefined) {
        streamIdWM.set(stream, (id = `rs-${stream_id_acc++}[${cid}]`));
    }
    return id;
};
exports.setStreamId = setStreamId;
class UsableIpcBodyMapper {
    constructor() {
        Object.defineProperty(this, "map", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: new Map()
        });
        Object.defineProperty(this, "destroySignal", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: (0, createSignal_js_1.createSignal)()
        });
    }
    add(streamId, ipcBody) {
        if (this.map.has(streamId)) {
            return true;
        }
        this.map.set(streamId, ipcBody);
        return false;
    }
    get(streamId) {
        return this.map.get(streamId);
    }
    remove(streamId) {
        const ipcBody = this.map.get(streamId);
        if (ipcBody !== undefined) {
            this.map.delete(streamId);
            /// 如果都删除完了，那么就触发事件解绑
            if (this.map.size === 0) {
                this.destroySignal.emit();
                this.destroySignal.clear();
            }
        }
    }
    onDestroy(cb) {
        this.destroySignal.listen(cb);
    }
}
const IpcUsableIpcBodyMap = new WeakMap();
