"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.Webview = void 0;
const PromiseOut_js_1 = require("../../../helper/PromiseOut.js");
class Webview {
    constructor(id, src) {
        Object.defineProperty(this, "id", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: id
        });
        Object.defineProperty(this, "src", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: src
        });
        Object.defineProperty(this, "webContentId", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: -1
        });
        Object.defineProperty(this, "webContentId_devTools", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: -1
        });
        Object.defineProperty(this, "_api", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: void 0
        });
        Object.defineProperty(this, "_api_po", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: new PromiseOut_js_1.PromiseOut()
        });
        Object.defineProperty(this, "closing", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: false
        });
        Object.defineProperty(this, "state", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: {
                zIndex: 0,
                openingIndex: 0,
                closingIndex: 0,
                scale: 1,
                opacity: 1,
                // translateY: 0,
            }
        });
    }
    get api() {
        return this._api;
    }
    doReady(value) {
        this._api = value;
        this._api_po.resolve(value);
        console.log('执行了 doReady');
    }
    ready() {
        return this._api_po.promise;
    }
}
exports.Webview = Webview;
