"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.MicroModule = void 0;
const _makeFetchExtends_js_1 = require("../helper/$makeFetchExtends.js");
const createSignal_js_1 = require("../helper/createSignal.js");
const normalizeFetchArgs_js_1 = require("../helper/normalizeFetchArgs.js");
const PromiseOut_js_1 = require("../helper/PromiseOut.js");
const nativeFetch_js_1 = require("../sys/dns/nativeFetch.js");
class MicroModule {
    constructor() {
        Object.defineProperty(this, "_running_state_lock", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: PromiseOut_js_1.PromiseOut.resolve(false)
        });
        Object.defineProperty(this, "_after_shutdown_signal", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: (0, createSignal_js_1.createSignal)()
        });
        Object.defineProperty(this, "_ipcSet", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: new Set()
        });
        /**
         * 内部程序与外部程序通讯的方法
         * TODO 这里应该是可以是多个
         */
        Object.defineProperty(this, "_connectSignal", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: (0, createSignal_js_1.createSignal)()
        });
    }
    get isRunning() {
        return this._running_state_lock.promise;
    }
    async before_bootstrap(context) {
        if (await this._running_state_lock.promise) {
            throw new Error(`module ${this.mmid} alreay running`);
        }
        this._running_state_lock = new PromiseOut_js_1.PromiseOut();
    }
    async after_bootstrap(context) {
        this._running_state_lock.resolve(true);
    }
    async bootstrap(context) {
        await this.before_bootstrap(context);
        try {
            await this._bootstrap(context);
        }
        finally {
            this.after_bootstrap(context);
        }
    }
    async before_shutdown() {
        if (false === (await this._running_state_lock.promise)) {
            throw new Error(`module ${this.mmid} already shutdown`);
        }
        this._running_state_lock = new PromiseOut_js_1.PromiseOut();
    }
    after_shutdown() {
        this._after_shutdown_signal.emit();
        this._after_shutdown_signal.clear();
        this._running_state_lock.resolve(false);
    }
    async shutdown() {
        await this.before_shutdown();
        try {
            await this._shutdown();
        }
        finally {
            this.after_shutdown();
        }
    }
    /**
     * 给内部程序自己使用的 onConnect，外部与内部建立连接时使用
     * 因为 NativeMicroModule 的内部程序在这里编写代码，所以这里会提供 onConnect 方法
     * 如果时 JsMicroModule 这个 onConnect 就是写在 WebWorker 那边了
     */
    onConnect(cb) {
        return this._connectSignal.listen(cb);
    }
    async beConnect(ipc, reason) {
        this._ipcSet.add(ipc);
        ipc.onClose(() => {
            this._ipcSet.delete(ipc);
        });
        this._connectSignal.emit(ipc, reason);
    }
    async _nativeFetch(url, init) {
        const args = (0, normalizeFetchArgs_js_1.normalizeFetchArgs)(url, init);
        for (const adapter of nativeFetch_js_1.nativeFetchAdaptersManager.adapters) {
            const response = await adapter(this, args.parsed_url, args.request_init);
            if (response !== undefined) {
                return response;
            }
        }
        return fetch(args.parsed_url, args.request_init);
    }
    nativeFetch(url, init) {
        if (init?.body instanceof ReadableStream) {
            Reflect.set(init, "duplex", "half");
        }
        return Object.assign(this._nativeFetch(url, init), _makeFetchExtends_js_1.fetchExtends);
    }
}
exports.MicroModule = MicroModule;
