import { simpleDecoder, simpleEncoder } from "../../helper/encoding.js";
import { streamReadAllBuffer } from "../../helper/readableStreamHelper.js";
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
                body_u8a = await streamReadAllBuffer(bodyHub.stream);
            }
            else if (bodyHub.text !== undefined) {
                body_u8a = simpleEncoder(bodyHub.text, "utf8");
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
            body_text = simpleDecoder(await this.u8a(), "utf8");
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
export { IpcBody };
export class BodyHub {
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
