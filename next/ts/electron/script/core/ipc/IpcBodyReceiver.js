"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.IpcBodyReceiver = void 0;
const IpcBody_js_1 = require("./IpcBody.js");
const encoding_js_1 = require("../../helper/encoding.js");
const const_js_1 = require("./const.js");
const IpcStreamAbort_js_1 = require("./IpcStreamAbort.js");
const IpcStreamPulling_js_1 = require("./IpcStreamPulling.js");
class IpcBodyReceiver extends IpcBody_js_1.IpcBody {
    constructor(metaBody, ipc) {
        super();
        Object.defineProperty(this, "metaBody", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: metaBody
        });
        Object.defineProperty(this, "_bodyHub", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: void 0
        });
        if (metaBody.type_isStream) {
            const streamId = metaBody.streamId;
            const senderIpcUid = metaBody.senderUid;
            const metaId = `${senderIpcUid}/${streamId}`;
            /// 将第一次得到这个metaBody的 ipc 保存起来，这个ipc将用于接收
            if (IpcBodyReceiver.metaIdIpcMap.has(metaId) === false) {
                ipc.onClose(() => {
                    IpcBodyReceiver.metaIdIpcMap.delete(metaId);
                });
                IpcBodyReceiver.metaIdIpcMap.set(metaId, ipc);
                metaBody.receiverUid = ipc.uid;
            }
            const receiver = IpcBodyReceiver.metaIdIpcMap.get(metaId);
            if (receiver === undefined) {
                throw new Error(`no found ipc by metaId:${metaId}`);
            }
            ipc = receiver;
            this._bodyHub = new IpcBody_js_1.BodyHub($metaToStream(this.metaBody, ipc));
        }
        else
            switch (metaBody.type_encoding) {
                case const_js_1.IPC_DATA_ENCODING.UTF8:
                    /// 文本模式，直接返回即可，因为 RequestInit/Response 支持支持传入 utf8 字符串
                    this._bodyHub = new IpcBody_js_1.BodyHub(metaBody.data);
                    break;
                case const_js_1.IPC_DATA_ENCODING.BASE64:
                    this._bodyHub = new IpcBody_js_1.BodyHub((0, encoding_js_1.simpleEncoder)(metaBody.data, "base64"));
                    break;
                case const_js_1.IPC_DATA_ENCODING.BINARY:
                    this._bodyHub = new IpcBody_js_1.BodyHub(metaBody.data);
                    break;
                default:
                    throw new Error(`invalid metaBody type: ${metaBody.type}`);
            }
    }
}
Object.defineProperty(IpcBodyReceiver, "metaIdIpcMap", {
    enumerable: true,
    configurable: true,
    writable: true,
    value: new Map()
});
exports.IpcBodyReceiver = IpcBodyReceiver;
const $metaToStream = (metaBody, ipc) => {
    if (ipc == null) {
        throw new Error(`miss ipc when ipc-response has stream-body`);
    }
    const stream_ipc = ipc;
    const stream_id = metaBody.streamId;
    /**
     * 默认是暂停状态
     */
    let paused = true;
    const stream = new ReadableStream({
        start(controller) {
            /// 如果有初始帧，直接存起来
            let firstData;
            switch (metaBody.type_encoding) {
                case const_js_1.IPC_DATA_ENCODING.UTF8:
                    firstData = (0, encoding_js_1.simpleEncoder)(metaBody.data, "utf8");
                    break;
                case const_js_1.IPC_DATA_ENCODING.BASE64:
                    firstData = (0, encoding_js_1.simpleEncoder)(metaBody.data, "base64");
                    break;
                case const_js_1.IPC_DATA_ENCODING.BINARY:
                    firstData = metaBody.data;
                    break;
            }
            if (firstData) {
                controller.enqueue(firstData);
            }
            /// 监听事件
            const off = ipc.onStream((message) => {
                if (message.stream_id === stream_id) {
                    // STREAM_DATA || STREAM_END
                    switch (message.type) {
                        case const_js_1.IPC_MESSAGE_TYPE.STREAM_DATA:
                            controller.enqueue(message.binary);
                            break;
                        case const_js_1.IPC_MESSAGE_TYPE.STREAM_END:
                            controller.close();
                            off();
                            break;
                    }
                }
            });
        },
        pull(controller) {
            if (paused) {
                paused = false;
                stream_ipc.postMessage(new IpcStreamPulling_js_1.IpcStreamPulling(stream_id));
            }
        },
        cancel() {
            stream_ipc.postMessage(new IpcStreamAbort_js_1.IpcStreamAbort(stream_id));
        },
    }, {
        /// 按需 pull, 不可以0以上。否则一开始的时候就会发送pull指令，会导致远方直接把流给读取出来。
        /// 这会导致一些优化的行为异常，有些时候流一旦开始读取了，其他读取者就不能再进入读取了。那么流转发就不能工作了
        highWaterMark: 0,
    });
    return stream;
};
new WritableStream({});
