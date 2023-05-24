"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.MessagePortIpc = void 0;
const msgpack_1 = require("@msgpack/msgpack");
const const_js_1 = require("../ipc/const.js");
const ipc_js_1 = require("../ipc/ipc.js");
const IpcRequest_js_1 = require("../ipc/IpcRequest.js");
const IpcResponse_js_1 = require("../ipc/IpcResponse.js");
const _messagePackToIpcMessage_js_1 = require("./$messagePackToIpcMessage.js");
const _messageToIpcMessage_js_1 = require("./$messageToIpcMessage.js");
class MessagePortIpc extends ipc_js_1.Ipc {
    constructor(port, remote, role = const_js_1.IPC_ROLE.CLIENT, self_support_protocols = {
        raw: true,
        message_pack: true,
        protobuf: false,
    }) {
        super();
        Object.defineProperty(this, "port", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: port
        });
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
        /** messageport内置JS对象解码，但也要看对方是否支持接受，比如Android层就只能接受String类型的数据 */
        this._support_raw =
            self_support_protocols.raw && this.remote.ipc_support_protocols.raw;
        /** JS 环境里支持 message_pack 协议，但也要看对等方是否支持 */
        this._support_message_pack =
            self_support_protocols.message_pack &&
                this.remote.ipc_support_protocols.message_pack;
        port.addEventListener("message", (event) => {
            const message = this.support_raw
                ? (0, _messageToIpcMessage_js_1.$messageToIpcMessage)(event.data, this)
                : this.support_message_pack
                    ? (0, _messagePackToIpcMessage_js_1.$messagePackToIpcMessage)(event.data, this)
                    : (0, _messageToIpcMessage_js_1.$jsonToIpcMessage)(event.data, this);
            if (message === undefined) {
                console.error("MessagePortIpc.cts unkonwn message", event.data);
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
                this.port.postMessage("pong");
                return;
            }
            // console.log("web-message-port-ipc", "onmessage", message);
            this._messageSignal.emit(message, this);
        });
        port.start();
    }
    _doPostMessage(message) {
        var message_data;
        var message_raw;
        if (message instanceof IpcRequest_js_1.IpcRequest) {
            message_raw = message.ipcReqMessage();
        }
        else if (message instanceof IpcResponse_js_1.IpcResponse) {
            message_raw = message.ipcResMessage();
        }
        else {
            message_raw = message;
        }
        if (this.support_raw) {
            message_data = message_raw;
        }
        else if (this.support_message_pack) {
            message_data = (0, msgpack_1.encode)(message_raw);
        }
        else {
            message_data = JSON.stringify(message_raw);
        }
        this.port.postMessage(message_data);
    }
    _doClose() {
        console.log("web-message-port-ipc", "onclose");
        this.port.postMessage("close");
        this.port.close();
    }
}
exports.MessagePortIpc = MessagePortIpc;
