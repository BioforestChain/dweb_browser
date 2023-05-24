"use strict";
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.IPC_META_BODY_TYPE = exports.MetaBody = void 0;
const cacheGetter_js_1 = require("../../helper/cacheGetter.js");
const encoding_js_1 = require("../../helper/encoding.js");
const const_js_1 = require("./const.js");
class MetaBody {
    constructor(type, senderUid, data, streamId, receiverUid, metaId = (0, encoding_js_1.simpleDecoder)(crypto.getRandomValues(new Uint8Array(8)), "base64")) {
        Object.defineProperty(this, "type", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: type
        });
        Object.defineProperty(this, "senderUid", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: senderUid
        });
        Object.defineProperty(this, "data", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: data
        });
        Object.defineProperty(this, "streamId", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: streamId
        });
        Object.defineProperty(this, "receiverUid", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: receiverUid
        });
        Object.defineProperty(this, "metaId", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: metaId
        });
    }
    static fromJSON(metaBody) {
        if (metaBody instanceof MetaBody === false) {
            metaBody = new MetaBody(metaBody.type, metaBody.senderUid, metaBody.data, metaBody.streamId, metaBody.receiverUid, metaBody.metaId);
        }
        return metaBody;
    }
    static fromText(senderUid, data, streamId, receiverUid) {
        return new MetaBody(streamId == null
            ? IPC_META_BODY_TYPE.INLINE_TEXT
            : IPC_META_BODY_TYPE.STREAM_WITH_TEXT, senderUid, data, streamId, receiverUid);
    }
    static fromBase64(senderUid, data, streamId, receiverUid) {
        return new MetaBody(streamId == null
            ? IPC_META_BODY_TYPE.INLINE_BASE64
            : IPC_META_BODY_TYPE.STREAM_WITH_BASE64, senderUid, data, streamId, receiverUid);
    }
    static fromBinary(sender, data, streamId, receiverUid) {
        if (typeof sender === "number") {
            return new MetaBody(streamId == null
                ? IPC_META_BODY_TYPE.INLINE_BINARY
                : IPC_META_BODY_TYPE.STREAM_WITH_BINARY, sender, data, streamId, receiverUid);
        }
        if (sender.support_binary) {
            return this.fromBinary(sender.uid, data, streamId, receiverUid);
        }
        return this.fromBase64(sender.uid, (0, encoding_js_1.simpleDecoder)(data, "base64"), streamId, receiverUid);
    }
    get type_encoding() {
        const encoding = this.type & 0b11111110;
        switch (encoding) {
            case const_js_1.IPC_DATA_ENCODING.UTF8:
                return const_js_1.IPC_DATA_ENCODING.UTF8;
            case const_js_1.IPC_DATA_ENCODING.BASE64:
                return const_js_1.IPC_DATA_ENCODING.BASE64;
            case const_js_1.IPC_DATA_ENCODING.BINARY:
                return const_js_1.IPC_DATA_ENCODING.BINARY;
        }
    }
    get type_isInline() {
        return (this.type & IPC_META_BODY_TYPE.INLINE) !== 0;
    }
    get type_isStream() {
        return (this.type & IPC_META_BODY_TYPE.INLINE) === 0;
    }
    get jsonAble() {
        if (this.type_encoding === const_js_1.IPC_DATA_ENCODING.BINARY) {
            return MetaBody.fromBase64(this.senderUid, (0, encoding_js_1.simpleDecoder)(this.data, "base64"), this.streamId, this.receiverUid);
        }
        return this;
    }
    toJSON() {
        return { ...this.jsonAble };
    }
}
__decorate([
    (0, cacheGetter_js_1.cacheGetter)()
], MetaBody.prototype, "type_encoding", null);
__decorate([
    (0, cacheGetter_js_1.cacheGetter)()
], MetaBody.prototype, "type_isInline", null);
__decorate([
    (0, cacheGetter_js_1.cacheGetter)()
], MetaBody.prototype, "type_isStream", null);
__decorate([
    (0, cacheGetter_js_1.cacheGetter)()
], MetaBody.prototype, "jsonAble", null);
exports.MetaBody = MetaBody;
var IPC_META_BODY_TYPE;
(function (IPC_META_BODY_TYPE) {
    /** 流 */
    IPC_META_BODY_TYPE[IPC_META_BODY_TYPE["STREAM_ID"] = 0] = "STREAM_ID";
    /** 内联数据 */
    IPC_META_BODY_TYPE[IPC_META_BODY_TYPE["INLINE"] = 1] = "INLINE";
    /** 流，但是携带一帧的 UTF8 数据 */
    IPC_META_BODY_TYPE[IPC_META_BODY_TYPE["STREAM_WITH_TEXT"] = 2] = "STREAM_WITH_TEXT";
    /** 流，但是携带一帧的 BASE64 数据 */
    IPC_META_BODY_TYPE[IPC_META_BODY_TYPE["STREAM_WITH_BASE64"] = 4] = "STREAM_WITH_BASE64";
    /** 流，但是携带一帧的 BINARY 数据 */
    IPC_META_BODY_TYPE[IPC_META_BODY_TYPE["STREAM_WITH_BINARY"] = 8] = "STREAM_WITH_BINARY";
    /** 内联 UTF8 数据 */
    IPC_META_BODY_TYPE[IPC_META_BODY_TYPE["INLINE_TEXT"] = 3] = "INLINE_TEXT";
    /** 内联 BASE64 数据 */
    IPC_META_BODY_TYPE[IPC_META_BODY_TYPE["INLINE_BASE64"] = 5] = "INLINE_BASE64";
    /** 内联 BINARY 数据 */
    IPC_META_BODY_TYPE[IPC_META_BODY_TYPE["INLINE_BINARY"] = 9] = "INLINE_BINARY";
})(IPC_META_BODY_TYPE = exports.IPC_META_BODY_TYPE || (exports.IPC_META_BODY_TYPE = {}));
