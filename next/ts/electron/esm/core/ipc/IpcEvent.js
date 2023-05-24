var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
import { cacheGetter } from "../../helper/cacheGetter.js";
import { simpleDecoder } from "../../helper/encoding.js";
import { $dataToBinary, $dataToText, IpcMessage, IPC_DATA_ENCODING, IPC_MESSAGE_TYPE, } from "./const.js";
export class IpcEvent extends IpcMessage {
    constructor(name, data, encoding) {
        super(IPC_MESSAGE_TYPE.EVENT);
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
        return new IpcEvent(name, simpleDecoder(data, "base64"), IPC_DATA_ENCODING.BASE64);
    }
    static fromBinary(name, data) {
        return new IpcEvent(name, data, IPC_DATA_ENCODING.BINARY);
    }
    static fromUtf8(name, data) {
        return new IpcEvent(name, simpleDecoder(data, "utf8"), IPC_DATA_ENCODING.UTF8);
    }
    static fromText(name, data) {
        return new IpcEvent(name, data, IPC_DATA_ENCODING.UTF8);
    }
    get binary() {
        return $dataToBinary(this.data, this.encoding);
    }
    get text() {
        return $dataToText(this.data, this.encoding);
    }
    get jsonAble() {
        if (this.encoding === IPC_DATA_ENCODING.BINARY) {
            return IpcEvent.fromBase64(this.name, this.data);
        }
        return this;
    }
    toJSON() {
        return { ...this.jsonAble };
    }
}
__decorate([
    cacheGetter()
], IpcEvent.prototype, "binary", null);
__decorate([
    cacheGetter()
], IpcEvent.prototype, "text", null);
__decorate([
    cacheGetter()
], IpcEvent.prototype, "jsonAble", null);
