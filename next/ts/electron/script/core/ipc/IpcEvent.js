"use strict";
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.IpcEvent = void 0;
const cacheGetter_js_1 = require("../../helper/cacheGetter.js");
const encoding_js_1 = require("../../helper/encoding.js");
const const_js_1 = require("./const.js");
class IpcEvent extends const_js_1.IpcMessage {
    constructor(name, data, encoding) {
        super(const_js_1.IPC_MESSAGE_TYPE.EVENT);
        Object.defineProperty(this, "name", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: name
        });
        Object.defineProperty(this, "data", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: data
        });
        Object.defineProperty(this, "encoding", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: encoding
        });
    }
    static fromBase64(name, data) {
        return new IpcEvent(name, (0, encoding_js_1.simpleDecoder)(data, "base64"), const_js_1.IPC_DATA_ENCODING.BASE64);
    }
    static fromBinary(name, data) {
        return new IpcEvent(name, data, const_js_1.IPC_DATA_ENCODING.BINARY);
    }
    static fromUtf8(name, data) {
        return new IpcEvent(name, (0, encoding_js_1.simpleDecoder)(data, "utf8"), const_js_1.IPC_DATA_ENCODING.UTF8);
    }
    static fromText(name, data) {
        return new IpcEvent(name, data, const_js_1.IPC_DATA_ENCODING.UTF8);
    }
    get binary() {
        return (0, const_js_1.$dataToBinary)(this.data, this.encoding);
    }
    get text() {
        return (0, const_js_1.$dataToText)(this.data, this.encoding);
    }
    get jsonAble() {
        if (this.encoding === const_js_1.IPC_DATA_ENCODING.BINARY) {
            return IpcEvent.fromBase64(this.name, this.data);
        }
        return this;
    }
    toJSON() {
        return { ...this.jsonAble };
    }
}
__decorate([
    (0, cacheGetter_js_1.cacheGetter)()
], IpcEvent.prototype, "binary", null);
__decorate([
    (0, cacheGetter_js_1.cacheGetter)()
], IpcEvent.prototype, "text", null);
__decorate([
    (0, cacheGetter_js_1.cacheGetter)()
], IpcEvent.prototype, "jsonAble", null);
exports.IpcEvent = IpcEvent;
