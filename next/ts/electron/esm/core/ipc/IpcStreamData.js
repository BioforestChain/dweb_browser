var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
import { cacheGetter } from "../../helper/cacheGetter.js";
import { simpleDecoder } from "../../helper/encoding.js";
import { $dataToBinary, $dataToText, IpcMessage, IPC_DATA_ENCODING, IPC_MESSAGE_TYPE, } from "./const.js";
export class IpcStreamData extends IpcMessage {
    constructor(stream_id, data, encoding) {
        super(IPC_MESSAGE_TYPE.STREAM_DATA);
        Object.defineProperty(this, "stream_id", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: stream_id
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
    static fromBase64(stream_id, data) {
        return new IpcStreamData(stream_id, simpleDecoder(data, "base64"), IPC_DATA_ENCODING.BASE64);
    }
    static fromBinary(stream_id, data) {
        return new IpcStreamData(stream_id, data, IPC_DATA_ENCODING.BINARY);
    }
    static fromUtf8(stream_id, data) {
        return new IpcStreamData(stream_id, simpleDecoder(data, "utf8"), IPC_DATA_ENCODING.UTF8);
    }
    get binary() {
        return $dataToBinary(this.data, this.encoding);
    }
    get text() {
        return $dataToText(this.data, this.encoding);
    }
    get jsonAble() {
        if (this.encoding === IPC_DATA_ENCODING.BINARY) {
            return IpcStreamData.fromBase64(this.stream_id, this.data);
        }
        return this;
    }
    toJSON() {
        return { ...this.jsonAble };
    }
}
__decorate([
    cacheGetter()
], IpcStreamData.prototype, "binary", null);
__decorate([
    cacheGetter()
], IpcStreamData.prototype, "text", null);
__decorate([
    cacheGetter()
], IpcStreamData.prototype, "jsonAble", null);
