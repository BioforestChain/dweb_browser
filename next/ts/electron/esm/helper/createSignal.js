var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
import { cacheGetter } from "./cacheGetter.js";
export const createSignal = (autoStart) => {
    return new Signal(autoStart);
};
export class Signal {
    constructor(autoStart = true) {
        Object.defineProperty(this, "_cbs", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: new Set()
        });
        Object.defineProperty(this, "_started", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: false
        });
        Object.defineProperty(this, "start", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: () => {
                if (this._started) {
                    return;
                }
                this._started = true;
                if (this._cachedEmits.length) {
                    for (const args of this._cachedEmits) {
                        this._emit(args);
                    }
                    this._cachedEmits.length = 0;
                }
            }
        });
        Object.defineProperty(this, "listen", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: (cb) => {
                this._cbs.add(cb);
                this.start();
                return () => this._cbs.delete(cb);
            }
        });
        Object.defineProperty(this, "emit", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: (...args) => {
                if (this._started) {
                    this._emit(args);
                }
                else {
                    this._cachedEmits.push(args);
                }
            }
        });
        Object.defineProperty(this, "_emit", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: (args) => {
                for (const cb of this._cbs) {
                    cb.apply(null, args);
                }
            }
        });
        Object.defineProperty(this, "clear", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: () => {
                this._cbs.clear();
            }
        });
        if (autoStart) {
            this.start();
        }
    }
    get _cachedEmits() {
        return [];
    }
}
__decorate([
    cacheGetter()
], Signal.prototype, "_cachedEmits", null);
