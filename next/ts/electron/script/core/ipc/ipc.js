"use strict";
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.Ipc = void 0;
const cacheGetter_js_1 = require("../../helper/cacheGetter.js");
const createSignal_js_1 = require("../../helper/createSignal.js");
const PromiseOut_js_1 = require("../../helper/PromiseOut.js");
const micro_module_js_1 = require("../micro-module.js");
const const_js_1 = require("./const.js");
const IpcRequest_js_1 = require("./IpcRequest.js");
let ipc_uid_acc = 0;
class Ipc {
    constructor() {
        Object.defineProperty(this, "uid", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: ipc_uid_acc++
        });
        Object.defineProperty(this, "_support_message_pack", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: false
        });
        Object.defineProperty(this, "_support_protobuf", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: false
        });
        Object.defineProperty(this, "_support_raw", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: false
        });
        Object.defineProperty(this, "_support_binary", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: false
        });
        Object.defineProperty(this, "_messageSignal", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: (0, createSignal_js_1.createSignal)(false)
        });
        Object.defineProperty(this, "onMessage", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: this._messageSignal.listen
        });
        Object.defineProperty(this, "_closed", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: false
        });
        Object.defineProperty(this, "_closeSignal", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: (0, createSignal_js_1.createSignal)(false)
        });
        Object.defineProperty(this, "onClose", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: this._closeSignal.listen
        });
        Object.defineProperty(this, "_req_id_acc", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: 0
        });
    }
    /**
     * 是否支持使用 MessagePack 直接传输二进制
     * 在一些特殊的场景下支持字符串传输，比如与webview的通讯
     * 二进制传输在网络相关的服务里被支持，里效率会更高，但前提是对方有 MessagePack 的编解码能力
     * 否则 JSON 是通用的传输协议
     */
    get support_message_pack() {
        return this._support_message_pack;
    }
    /**
     * 是否支持使用 Protobuf 直接传输二进制
     * 在网络环境里，protobuf 是更加高效的协议
     */
    get support_protobuf() {
        return this._support_protobuf;
    }
    /**
     * 是否支持结构化内存协议传输：
     * 就是说不需要对数据手动序列化反序列化，可以直接传输内存对象
     */
    get support_raw() {
        return this._support_raw;
    }
    /**
     * 是否支持二进制传输
     */
    get support_binary() {
        return (this._support_binary ??
            (this.support_message_pack || this.support_protobuf || this.support_raw));
    }
    asRemoteInstance() {
        if (this.remote instanceof micro_module_js_1.MicroModule) {
            return this.remote;
        }
    }
    postMessage(message) {
        if (this._closed) {
            return;
        }
        this._doPostMessage(message);
    }
    get _onRequestSignal() {
        const signal = (0, createSignal_js_1.createSignal)(false);
        this.onMessage((request, ipc) => {
            if (request.type === const_js_1.IPC_MESSAGE_TYPE.REQUEST) {
                signal.emit(request, ipc);
            }
        });
        return signal;
    }
    onRequest(cb) {
        return this._onRequestSignal.listen(cb);
    }
    get _onStreamSignal() {
        const signal = (0, createSignal_js_1.createSignal)(false);
        this.onMessage((request, ipc) => {
            if ("stream_id" in request) {
                signal.emit(request, ipc);
            }
        });
        return signal;
    }
    onStream(cb) {
        return this._onStreamSignal.listen(cb);
    }
    get _onEventSignal() {
        const signal = (0, createSignal_js_1.createSignal)(false);
        this.onMessage((event, ipc) => {
            if (event.type === const_js_1.IPC_MESSAGE_TYPE.EVENT) {
                signal.emit(event, ipc);
            }
        });
        return signal;
    }
    onEvent(cb) {
        return this._onEventSignal.listen(cb);
    }
    close() {
        if (this._closed) {
            return;
        }
        this._closed = true;
        this._doClose();
        this._closeSignal.emit();
        this._closeSignal.clear();
    }
    allocReqId(url) {
        return this._req_id_acc++;
    }
    get _reqresMap() {
        const reqresMap = new Map();
        this.onMessage((message) => {
            if (message.type === const_js_1.IPC_MESSAGE_TYPE.RESPONSE) {
                const response_po = reqresMap.get(message.req_id);
                if (response_po) {
                    reqresMap.delete(message.req_id);
                    response_po.resolve(message);
                }
                else {
                    throw new Error(`no found response by req_id: ${message.req_id}`);
                }
            }
        });
        return reqresMap;
    }
    /** 发起请求并等待响应 */
    request(url, init) {
        const req_id = this.allocReqId();
        const ipcRequest = IpcRequest_js_1.IpcRequest.fromRequest(req_id, this, url, init);
        const result = this.registerReqId(req_id);
        this.postMessage(ipcRequest);
        return result.promise;
    }
    /** 自定义注册 请求与响应 的id */
    registerReqId(req_id = this.allocReqId()) {
        const response_po = new PromiseOut_js_1.PromiseOut();
        this._reqresMap.set(req_id, response_po);
        return response_po;
    }
}
__decorate([
    (0, cacheGetter_js_1.cacheGetter)()
], Ipc.prototype, "_onRequestSignal", null);
__decorate([
    (0, cacheGetter_js_1.cacheGetter)()
], Ipc.prototype, "_onStreamSignal", null);
__decorate([
    (0, cacheGetter_js_1.cacheGetter)()
], Ipc.prototype, "_onEventSignal", null);
__decorate([
    (0, cacheGetter_js_1.cacheGetter)()
], Ipc.prototype, "_reqresMap", null);
exports.Ipc = Ipc;
