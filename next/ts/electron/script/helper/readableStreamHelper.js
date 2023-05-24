"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.streamFromCallback = exports.ReadableStreamOut = exports.streamReadAllBuffer = exports.streamReadAll = exports.binaryStreamRead = exports.streamRead = void 0;
const binaryHelper_js_1 = require("./binaryHelper.js");
const createSignal_js_1 = require("./createSignal.js");
async function* _doRead(reader) {
    try {
        while (true) {
            const item = await reader.read();
            if (item.done) {
                break;
            }
            yield item.value;
        }
    }
    finally {
        reader.releaseLock();
    }
}
const streamRead = (stream, options = {}) => {
    return _doRead(stream.getReader());
};
exports.streamRead = streamRead;
const binaryStreamRead = (stream, options = {}) => {
    const reader = (0, exports.streamRead)(stream, options);
    var done = false;
    var cache = new Uint8Array(0);
    const appendToCache = async () => {
        const item = await reader.next();
        if (item.done) {
            done = true;
            return false;
        }
        else {
            cache = (0, binaryHelper_js_1.u8aConcat)([cache, item.value]);
            return true;
        }
    };
    const available = async () => {
        if (cache.length > 0) {
            return cache.length;
        }
        if (done) {
            return -1;
        }
        await appendToCache();
        return available();
    };
    const readBinary = async (size) => {
        if (cache.length >= size) {
            const result = cache.subarray(0, size);
            cache = cache.subarray(size);
            return result;
        }
        if (await appendToCache()) {
            return readBinary(size);
        }
        else {
            throw new Error(`fail to read bytes(${cache.length}/${size} byte) in stream`);
        }
    };
    const u32 = new Uint32Array(1);
    const u32_u8 = new Uint8Array(u32.buffer);
    const readInt = async () => {
        const intBuf = await readBinary(4);
        u32_u8.set(intBuf);
        return u32[0];
    };
    return Object.assign(reader, {
        available,
        readBinary,
        readInt,
    });
};
exports.binaryStreamRead = binaryStreamRead;
const streamReadAll = async (stream, options = {}) => {
    const items = [];
    const maps = [];
    for await (const item of _doRead(stream.getReader())) {
        items.push(item);
        if (options.map) {
            maps.push(options.map(item));
        }
    }
    const result = options.complete?.(items, maps);
    return {
        items,
        maps,
        result,
    };
};
exports.streamReadAll = streamReadAll;
const streamReadAllBuffer = async (stream) => {
    return (await (0, exports.streamReadAll)(stream, {
        complete(items) {
            return (0, binaryHelper_js_1.u8aConcat)(items);
        },
    })).result;
};
exports.streamReadAllBuffer = streamReadAllBuffer;
class ReadableStreamOut {
    constructor(strategy) {
        Object.defineProperty(this, "strategy", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: strategy
        });
        Object.defineProperty(this, "controller", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: void 0
        });
        Object.defineProperty(this, "stream", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: new ReadableStream({
                cancel: (reason) => {
                    this._on_cancel_signal?.emit(reason);
                },
                start: (controller) => {
                    this.controller = controller;
                },
                pull: () => {
                    this._on_pull_signal?.emit();
                },
            }, this.strategy)
        });
        Object.defineProperty(this, "_on_cancel_signal", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: void 0
        });
        Object.defineProperty(this, "_on_pull_signal", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: void 0
        });
    }
    get onCancel() {
        return (this._on_cancel_signal ??= (0, createSignal_js_1.createSignal)()).listen;
    }
    get onPull() {
        return (this._on_pull_signal ??= (0, createSignal_js_1.createSignal)()).listen;
    }
}
exports.ReadableStreamOut = ReadableStreamOut;
const streamFromCallback = (cb, onCancel) => {
    const stream = new ReadableStream({
        start(controller) {
            onCancel?.then(() => controller.close());
            cb((...args) => {
                controller.enqueue(args);
            });
        },
    });
    return stream;
};
exports.streamFromCallback = streamFromCallback;
