"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.BodyHub = exports.IpcBody = void 0;
const encoding_js_1 = require("../../helper/encoding.js");
const readableStreamHelper_js_1 = require("../../helper/readableStreamHelper.js");
class IpcBody {
    get raw() {
        return this._bodyHub.data;
    }
    async u8a() {
        /// 首先要确保 body 已经被绑定上去
        const bodyHub = this._bodyHub;
        let body_u8a = bodyHub.u8a;
        if (body_u8a === undefined) {
            if (bodyHub.stream) {
                body_u8a = await (0, readableStreamHelper_js_1.streamReadAllBuffer)(bodyHub.stream);
            }
            else if (bodyHub.text !== undefined) {
                body_u8a = (0, encoding_js_1.simpleEncoder)(bodyHub.text, "utf8");
            }
            else {
                throw new Error(`invalid body type`);
            }
            bodyHub.u8a = body_u8a;
            IpcBody.wm.set(body_u8a, this);
        }
        return body_u8a;
    }
    async stream() {
        /// 首先要确保 body 已经被绑定上去
        const bodyHub = this._bodyHub;
        let body_stream = bodyHub.stream;
        if (body_stream === undefined) {
            body_stream = new Blob([await this.u8a()]).stream();
            bodyHub.stream = body_stream;
            IpcBody.wm.set(body_stream, this);
        }
        return body_stream;
    }
    async text() {
        const bodyHub = await this._bodyHub;
        let body_text = bodyHub.text;
        if (body_text === undefined) {
            body_text = (0, encoding_js_1.simpleDecoder)(await this.u8a(), "utf8");
            bodyHub.text = body_text;
        }
        return body_text;
    }
}
Object.defineProperty(IpcBody, "wm", {
    enumerable: true,
    configurable: true,
    writable: true,
    value: new WeakMap()
});
exports.IpcBody = IpcBody;
class BodyHub {
    constructor(data) {
        Object.defineProperty(this, "data", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: data
        });
        Object.defineProperty(this, "u8a", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: void 0
        });
        Object.defineProperty(this, "stream", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: void 0
        });
        Object.defineProperty(this, "text", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: void 0
        });
        if (typeof data === "string") {
            this.text = data;
        }
        else if (data instanceof ReadableStream) {
            this.stream = data;
        }
        else {
            this.u8a = data;
        }
    }
}
exports.BodyHub = BodyHub;
