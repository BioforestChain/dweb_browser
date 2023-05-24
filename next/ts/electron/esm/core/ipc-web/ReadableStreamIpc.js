var __classPrivateFieldGet = (this && this.__classPrivateFieldGet) || function (receiver, state, kind, f) {
    if (kind === "a" && !f) throw new TypeError("Private accessor was defined without a getter");
    if (typeof state === "function" ? receiver !== state || !f : !state.has(receiver)) throw new TypeError("Cannot read private member from an object whose class did not declare it");
    return kind === "m" ? f : kind === "a" ? f.call(receiver) : f ? f.value : state.get(receiver);
};
var _ReadableStreamIpc_rso;
import { encode } from "@msgpack/msgpack";
import { once } from "lodash";
import { u8aConcat } from "../../helper/binaryHelper.js";
import { simpleDecoder, simpleEncoder } from "../../helper/encoding.js";
import { binaryStreamRead, ReadableStreamOut, } from "../../helper/readableStreamHelper.js";
import { Ipc } from "../ipc/ipc.js";
import { $messagePackToIpcMessage } from "./$messagePackToIpcMessage.js";
import { $jsonToIpcMessage } from "./$messageToIpcMessage.js";
import { IPC_MESSAGE_TYPE } from "../ipc/const.js";
/**
 * 基于 WebReadableStream 的IPC
 *
 * 它会默认构建出一个输出流，
 * 以及需要手动绑定输入流 {@link bindIncomeStream}
 */
export class ReadableStreamIpc extends Ipc {
    constructor(remote, role, self_support_protocols = {
        raw: false,
        message_pack: true,
        protobuf: false,
    }) {
        super();
        Object.defineProperty(this, "remote", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: remote
        });
        Object.defineProperty(this, "role", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: role
        });
        Object.defineProperty(this, "self_support_protocols", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: self_support_protocols
        });
        _ReadableStreamIpc_rso.set(this, new ReadableStreamOut());
        Object.defineProperty(this, "PONG_DATA", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: once(() => {
                const pong = simpleEncoder("pong", "utf8");
                this._len[0] = pong.length;
                return u8aConcat([this._len_u8a, pong]);
            })
        });
        Object.defineProperty(this, "_incomne_stream", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: void 0
        });
        Object.defineProperty(this, "_len", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: new Uint32Array(1)
        });
        Object.defineProperty(this, "_len_u8a", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: new Uint8Array(this._len.buffer)
        });
        /** JS 环境里支持 message_pack 协议 */
        this._support_message_pack =
            self_support_protocols.message_pack &&
                remote.ipc_support_protocols.message_pack;
    }
    /** 这是输出流，给外部读取用的 */
    get stream() {
        return __classPrivateFieldGet(this, _ReadableStreamIpc_rso, "f").stream;
    }
    get controller() {
        return __classPrivateFieldGet(this, _ReadableStreamIpc_rso, "f").controller;
    }
    /**
     * 输入流要额外绑定
     * 注意，非必要不要 await 这个promise
     */
    async bindIncomeStream(stream) {
        if (this._incomne_stream !== undefined) {
            throw new Error("in come stream alreay binded.");
        }
        this._incomne_stream = await stream;
        const reader = binaryStreamRead(this._incomne_stream);
        while ((await reader.available()) > 0) {
            const size = await reader.readInt();
            const data = await reader.readBinary(size);
            /// 开始处理数据并做响应
            const message = this.support_message_pack
                ? $messagePackToIpcMessage(data, this)
                : $jsonToIpcMessage(simpleDecoder(data, "utf8"), this);
            if (message === undefined) {
                console.error("unkonwn message", data);
                return;
            }
            if (message === "pong") {
                return;
            }
            if (message === "close") {
                this.close();
                return;
            }
            if (message === "ping") {
                this.controller.enqueue(this.PONG_DATA());
                return;
            }
            this._messageSignal.emit(message, this);
        }
    }
    _doPostMessage(message) {
        var message_raw;
        // 源代吗 在 处理 /internal/public-url 的时候无法正确的判断
        // message instanceof IpcResponse === false
        // 所以更改为使用message.type 判断 
        // if (message instanceof IpcRequest) {
        //   message_raw = message.ipcReqMessage();
        // } else if (message instanceof IpcResponse) {
        //   message_raw = message.ipcResMessage();
        // } else {
        //   message_raw = message;
        // }
        // 使用 type 判断
        if (message.type === IPC_MESSAGE_TYPE.REQUEST) {
            message_raw = message.ipcReqMessage();
        }
        else if (message.type === IPC_MESSAGE_TYPE.RESPONSE) {
            message_raw = message.ipcResMessage();
        }
        else {
            message_raw = message;
        }
        const message_data = this.support_message_pack
            ? encode(message_raw)
            : simpleEncoder(JSON.stringify(message_raw), "utf8");
        this._len[0] = message_data.length;
        const chunk = u8aConcat([this._len_u8a, message_data]);
        this.controller.enqueue(chunk);
    }
    _doClose() {
        this.controller.close();
    }
}
_ReadableStreamIpc_rso = new WeakMap();
