"use strict";
var __classPrivateFieldGet = (this && this.__classPrivateFieldGet) || function (receiver, state, kind, f) {
    if (kind === "a" && !f) throw new TypeError("Private accessor was defined without a getter");
    if (typeof state === "function" ? receiver !== state || !f : !state.has(receiver)) throw new TypeError("Cannot read private member from an object whose class did not declare it");
    return kind === "m" ? f : kind === "a" ? f.call(receiver) : f ? f.value : state.get(receiver);
};
var _LockManager_queues;
Object.defineProperty(exports, "__esModule", { value: true });
exports.locks = exports.LockManager = void 0;
const PromiseOut_js_1 = require("./PromiseOut.js");
class LockManager {
    constructor() {
        _LockManager_queues.set(this, new Map());
    }
    request(name, callback) {
        let lock = __classPrivateFieldGet(this, _LockManager_queues, "f").get(name) ?? Promise.resolve();
        const r = new PromiseOut_js_1.PromiseOut();
        lock = lock.finally(async () => {
            try {
                r.resolve(await callback());
            }
            catch (err) {
                r.reject(err);
            }
            return r.promise;
        });
        __classPrivateFieldGet(this, _LockManager_queues, "f").set(name, lock);
        return r.promise;
    }
}
exports.LockManager = LockManager;
_LockManager_queues = new WeakMap();
exports.locks = new LockManager();
