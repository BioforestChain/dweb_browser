"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.MicroModule = void 0;
const helper_cjs_1 = require("./helper.cjs");
class MicroModule {
    constructor() {
        this.running = false;
    }
    before_bootstrap() {
        if (this.running) {
            throw new Error(`module ${this.mmid} alreay running`);
        }
        this.running = true;
    }
    after_bootstrap() { }
    async bootstrap() {
        this.before_bootstrap();
        const bootstrap_lock = new helper_cjs_1.PromiseOut();
        this._bootstrap_lock = bootstrap_lock.promise;
        try {
            await this._bootstrap();
        }
        finally {
            bootstrap_lock.resolve();
            this._bootstrap_lock = undefined;
            this.after_bootstrap();
        }
    }
    before_shutdown() {
        if (this.running === false) {
            throw new Error(`module ${this.mmid} alreay shutdown`);
        }
        this.running = false;
    }
    after_shutdown() { }
    async shutdown() {
        if (this._bootstrap_lock) {
            await this._bootstrap_lock;
        }
        const shutdown_lock = new helper_cjs_1.PromiseOut();
        this._shutdown_lock = shutdown_lock.promise;
        this.before_shutdown();
        try {
            await this._shutdown();
        }
        finally {
            shutdown_lock.resolve();
            this._shutdown_lock = undefined;
            this.after_shutdown();
        }
    }
    async connect(from) {
        if (this.running === false) {
            throw new Error("module no running");
        }
        await this._shutdown_lock;
        return this._connect(from);
    }
    fetch(url, init) {
        /// 强制注入上下文
        return Object.assign(fetch.call(this, url, init), helper_cjs_1.fetch_helpers);
    }
}
exports.MicroModule = MicroModule;
