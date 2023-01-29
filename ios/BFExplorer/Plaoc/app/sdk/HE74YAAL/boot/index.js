class MetaData {
    constructor(metaData) {
        Object.defineProperty(this, "manifest", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: void 0
        });
        Object.defineProperty(this, "dwebview", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: void 0
        });
        Object.defineProperty(this, "whitelist", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: void 0
        });
        this.manifest = metaData.manifest;
        this.dwebview = metaData.dwebview;
        this.whitelist = metaData.whitelist;
    }
}
function metaConfig(metaData) {
    return new MetaData(metaData);
}

// import "./node_modules/index.html";
var metaData = metaConfig({
    manifest: {
        version: "1.4.0",
        name: "ar扫码",
        icon: "/vite.svg",
        appType: "",
        url: "https://objectjson.waterbang.top/",
        engines: {
            dwebview: "~1.0.0",
        },
        // 应用所属链的名称（系统应用的链名为通配符“*”，其合法性由节点程序自身决定，不跟随链上数据）
        origin: "bfchain",
        // 开发者
        author: ["waterbang,water_bang@163.com"],
        // 应用搜索的描述
        description: "Awasome DWeb",
        maxAge: 1,
        // 应用搜索的关键字
        keywords: ["demo"],
        // 私钥文件，用于最终的应用签名
        privateKey: "bmr9vohvtvbvwrs3p4bwgzsmolhtphsvvj",
        homepage: "docs.plaoc.com",
        // 应用入口，可以配置多个，其中index为缺省名称。
        // 外部可以使用 DWEB_ID.bfchain (等价同于index.DWEB_ID.bfchain)、admin.DWEB_ID.bfchain 来启动其它页面
        enters: ["index.html", "aboult.html", "https://objectjson.waterbang.top/"],
        //本次发布的信息，一般存放更新信息
        releaseNotes: "xxx",
        //  本次发布的标题，用于展示更新信息时的标题
        releaseName: "xxx",
        // 发布日期
        releaseDate: "xxx",
    },
    //  这里配置的白名单将不被拦截
    whitelist: ["https://unpkg.com", "https://cn.vitejs.dev"],
    // 定义路由，这里与enter是完全独立的存在。
    // 外部可以使用 admin.DWEB_ID.bfchain/routeA 来传入参数
    dwebview: {
        importmap: [
            {
                url: "/getBlockInfo",
                response: "https://62b94efd41bf319d22797acd.mockapi.io/bfchain/v1/getBlockInfo",
            },
            {
                url: "/getBlockHigh",
                response: "https://62b94efd41bf319d22797acd.mockapi.io/bfchain/v1/getBlockInfo",
            },
            {
                url: "/app/bfchain.dev/index.html",
                response: "/app/bfchain.dev/index.html",
            },
            {
                url: "/api/*",
                response: "./api/*",
            },
            {
                url: "/api/upload",
                response: "/api/update",
            },
        ],
    },
});
// // web
// await fetch("./api/z.ts?a=1");
// // z.bfsapi.ts
// export default (req, res) => {
//   return { echo: queryData };
// };
// dwebview -> nodejs
// importMap assert
/// dwebview index.ts

/*! *****************************************************************************
Copyright (c) Microsoft Corporation.

Permission to use, copy, modify, and/or distribute this software for any
purpose with or without fee is hereby granted.

THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH
REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY
AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT,
INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM
LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR
OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
PERFORMANCE OF THIS SOFTWARE.
***************************************************************************** */

function __decorate(decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
}

function __metadata(metadataKey, metadataValue) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(metadataKey, metadataValue);
}

/**存储已经执行过bindThis的属性，避免原型链上的重复bind */
const BINDED_THIS_PROPS = Symbol("bindThisProps");
function bindThis(target, propertyKey, descriptor) {
    if (!descriptor || typeof descriptor.value !== "function") {
        throw new TypeError(`Only methods can be decorated with @bind. <${propertyKey}> is not a method!`);
    }
    return {
        configurable: true,
        get() {
            let props = this[BINDED_THIS_PROPS];
            /// 父级原型链上已经有执行过bindThis了，这里就直接跟随父级进行返回
            if (props && props.has(propertyKey)) {
                return descriptor.value;
            }
            const bound = descriptor.value.bind(this);
            Object.defineProperty(this, propertyKey, {
                value: bound,
                configurable: true,
                writable: true,
            });
            props || (props = this[BINDED_THIS_PROPS] = new Set());
            props.add(propertyKey);
            return bound;
        },
    };
}

const CACHE_KEYS_SYMBOL = Symbol("CACHE_GETTER_KEYS_STORE");
function getCacheKeys(protoTarget) {
    let CACHE_KEYS = Reflect.get(protoTarget, CACHE_KEYS_SYMBOL);
    if (!CACHE_KEYS) {
        CACHE_KEYS = new Map();
        /// 写入原型链
        Reflect.set(protoTarget, CACHE_KEYS_SYMBOL, CACHE_KEYS);
    }
    return CACHE_KEYS;
}
/**
 * 缓存Key生成器，这里与构造函数的原型链进行绑定
 * 以确保构造函数存在的清空下，缓存的key就会有存在的必要。
 * 这样可以避免动态生成class的情况，但这些class被释放，那么对应的CACHE_KEYS也能被释放
 *
 * 值得注意的是，根据代码的执行顺序，这里只会绑定到最底层的那么class上，其它继承于它的class与它贡献同一个CACHE_KEYS
 */
function keyGenerator(protoTarget, prop) {
    const CACHE_KEYS = getCacheKeys(protoTarget);
    let symbol = CACHE_KEYS.get(prop);
    if (!symbol) {
        symbol = Symbol(`[${typeof prop}]${String(prop)}`);
        CACHE_KEYS.set(prop, symbol);
    }
    return symbol;
}
function cacheGetter(propTarget, prop, descriptor) {
    if (typeof descriptor.get !== "function") {
        throw new TypeError(`property ${String(prop)} must has an getter function.`);
    }
    const source_fun = descriptor.get;
    /**缓存结果用的key */
    const CACHE_VALUE_SYMBOL = keyGenerator(propTarget, prop);
    const getter = function () {
        if (CACHE_VALUE_SYMBOL in this) {
            // 可能无法成功 Object.defineProperty，那么直接获取缓存的数据
            return this[CACHE_VALUE_SYMBOL].value;
        }
        else {
            const value = source_fun.call(this);
            /**
             * 使用原型链来进行缓存绑定，最符合使用直觉
             * 同时这里需要记录原型链中的位置，才能快速删除
             */
            const cacheValue = {
                // propTarget,
                target: this,
                value,
                sourceFun: source_fun,
            };
            this[CACHE_VALUE_SYMBOL] = cacheValue;
            /// 如果没有自定义getter，那么可以尝试进行重写，直接写成值，无需再是函数的模式
            if (descriptor.set === undefined) {
                try {
                    /// 注意，这里不会修改到 propTarget 对象，所以不干扰其它实例
                    Object.defineProperty(this, prop, {
                        value,
                        writable: false,
                        configurable: true,
                        enumerable: descriptor.enumerable,
                    });
                }
                catch (err) {
                    console.error(err);
                }
            }
            return value;
        }
    };
    Reflect.set(getter, "source_fun", source_fun);
    descriptor.get = getter;
    return descriptor;
}
/*
// node .\packages\decorator\build\cjs\cacheGetter.js
class A {
  b = 1;
  @cacheGetter
  get a() {
    return this.b;
  }
}
const a = new A();
a.b = 2;
console.assert(a.a === 2);
a.b = 3;
console.assert(a.a === 2);

cleanGetterCache(a, "a");
console.assert(a.a === 3);

a.b = 4;

const a1 = Object.create(a);
console.assert(a1.a === 3);
cleanGetterCache(a1, "a");
console.assert(a1.a === 4);
a.b = 5;
console.assert(a1.a === 4);
console.assert(a.a === 5);
 */

// 存储在原型链上的数据（字符串）集合
class PropArrayHelper {
    constructor(pid = Math.random().toString(36).slice(2)) {
        Object.defineProperty(this, "pid", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: pid
        });
        Object.defineProperty(this, "PA_ID_KEY", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: void 0
        });
        Object.defineProperty(this, "CLASS_PROTO_ARRAYDATA_POOL", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: new Map()
        });
        Object.defineProperty(this, "PA_ID_VALUE", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: 0
        });
        this.PA_ID_KEY = Symbol(`@PAID:${pid}`);
    }
    get(target, key) {
        const res = new Set();
        const CLASS_PROTO_ARRAYDATA = this.CLASS_PROTO_ARRAYDATA_POOL.get(key);
        if (CLASS_PROTO_ARRAYDATA) {
            do {
                if (target.hasOwnProperty(this.PA_ID_KEY)) {
                    const arr_data = CLASS_PROTO_ARRAYDATA.get(target[this.PA_ID_KEY]);
                    if (arr_data) {
                        for (const item of arr_data) {
                            res.add(item);
                        }
                    }
                }
            } while ((target = Object.getPrototypeOf(target)));
        }
        return res;
    }
    add(target, key, value) {
        let CLASS_PROTO_ARRAYDATA = this.CLASS_PROTO_ARRAYDATA_POOL.get(key);
        if (!CLASS_PROTO_ARRAYDATA) {
            CLASS_PROTO_ARRAYDATA = new Map();
            this.CLASS_PROTO_ARRAYDATA_POOL.set(key, CLASS_PROTO_ARRAYDATA);
        }
        const pa_id = target.hasOwnProperty(this.PA_ID_KEY)
            ? target[this.PA_ID_KEY]
            : (target[this.PA_ID_KEY] = Symbol(`@PAID:${this.pid}#${this.PA_ID_VALUE++}`));
        let arr_data = CLASS_PROTO_ARRAYDATA.get(pa_id);
        if (!arr_data) {
            arr_data = [value];
            CLASS_PROTO_ARRAYDATA.set(pa_id, arr_data);
        }
        else {
            arr_data.push(value);
        }
    }
    remove(target, key, value) {
        const CLASS_PROTO_ARRAYDATA = this.CLASS_PROTO_ARRAYDATA_POOL.get(key);
        if (!CLASS_PROTO_ARRAYDATA) {
            return;
        }
        do {
            if (!target.hasOwnProperty(this.PA_ID_KEY)) {
                break;
            }
            const pa_id = target[this.PA_ID_KEY];
            const arr_data = CLASS_PROTO_ARRAYDATA.get(pa_id);
            if (!arr_data) {
                return;
            }
            const index = arr_data.indexOf(value);
            if (index !== -1) {
                arr_data.splice(index, 1);
                return;
            }
        } while ((target = Object.getPrototypeOf(target)));
    }
}
__decorate([
    bindThis,
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object, Object]),
    __metadata("design:returntype", void 0)
], PropArrayHelper.prototype, "get", null);
__decorate([
    bindThis,
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object, Object, Object]),
    __metadata("design:returntype", void 0)
], PropArrayHelper.prototype, "add", null);
__decorate([
    bindThis,
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object, Object, Object]),
    __metadata("design:returntype", void 0)
], PropArrayHelper.prototype, "remove", null);

const dntGlobals = {};
const dntGlobalThis = createMergeProxy(globalThis, dntGlobals);
// deno-lint-ignore ban-types
function createMergeProxy(baseObj, extObj) {
    return new Proxy(baseObj, {
        get(_target, prop, _receiver) {
            if (prop in extObj) {
                return extObj[prop];
            }
            else {
                return baseObj[prop];
            }
        },
        set(_target, prop, value) {
            if (prop in extObj) {
                delete extObj[prop];
            }
            baseObj[prop] = value;
            return true;
        },
        deleteProperty(_target, prop) {
            let success = false;
            if (prop in extObj) {
                delete extObj[prop];
                success = true;
            }
            if (prop in baseObj) {
                delete baseObj[prop];
                success = true;
            }
            return success;
        },
        ownKeys(_target) {
            const baseKeys = Reflect.ownKeys(baseObj);
            const extKeys = Reflect.ownKeys(extObj);
            const extKeysSet = new Set(extKeys);
            return [...baseKeys.filter((k) => !extKeysSet.has(k)), ...extKeys];
        },
        defineProperty(_target, prop, desc) {
            if (prop in extObj) {
                delete extObj[prop];
            }
            Reflect.defineProperty(baseObj, prop, desc);
            return true;
        },
        getOwnPropertyDescriptor(_target, prop) {
            if (prop in extObj) {
                return Reflect.getOwnPropertyDescriptor(extObj, prop);
            }
            else {
                return Reflect.getOwnPropertyDescriptor(baseObj, prop);
            }
        },
        has(_target, prop) {
            return prop in extObj || prop in baseObj;
        },
    });
}

const freeGlobalThis = typeof dntGlobalThis !== "undefined" &&
    dntGlobalThis !== null &&
    globalThis.Object === Object &&
    dntGlobalThis;
const freeGlobal = typeof global !== "undefined" &&
    global !== null &&
    global.Object === Object &&
    global;
const freeSelf = typeof self !== "undefined" &&
    self !== null &&
    self.Object === Object &&
    self;
const $global = freeGlobalThis || freeGlobal || freeSelf || Function("return this")();
if (Reflect.get($global, "globalThis") === undefined) {
    Reflect.set($global, "globalThis", $global);
}

/**队列策略 */
var THROTTLE_WRAP_PLOT;
(function (THROTTLE_WRAP_PLOT) {
    /**等待任务执行完成才开始下一个任务 */
    THROTTLE_WRAP_PLOT[THROTTLE_WRAP_PLOT["WAIT_RESULT_RETURN"] = 0] = "WAIT_RESULT_RETURN";
    /**不等待任务执行完成，只要一开始执行，就能开始执行下一个任务 */
    THROTTLE_WRAP_PLOT[THROTTLE_WRAP_PLOT["NO_WAIT_EXEC_TIME"] = 1] = "NO_WAIT_EXEC_TIME";
})(THROTTLE_WRAP_PLOT || (THROTTLE_WRAP_PLOT = {}));

/**
 * 从堆栈中一个函数的获取调用者的信息
 * @param caller 如果支持`Error.captureStackTrace`，则使用caller定位
 * @param deep 否则直接使用手动计数定位
 */
const GetCallerInfo = Error.captureStackTrace
    ? (caller) => {
        const stackInfo = { stack: "" };
        Error.captureStackTrace(stackInfo, caller);
        return stackInfo.stack;
    }
    : /**使用Function动态生成来规避严格模式的代码解析 */
        Function("f", `
    let deep = 0;
    let caller = arguments.callee;
    do {
      if (caller.caller === f) {
        break;
      }
      deep += 1;
      caller = caller.caller;
      if (caller === null) {
        break;
      }
    } while (true);
    const stack = new Error().stack || "";
    const stackLineLine = stack.split('\\n');
    stackLineLine.splice(1, deep);
    return stackLineLine.join('\\n');
  `);

function renameFunction(fun, newName) {
    const hanlder_name_des = Object.getOwnPropertyDescriptor(fun, "name");
    if (hanlder_name_des && hanlder_name_des.configurable) {
        Object.defineProperty(fun, "name", {
            value: newName,
            configurable: true,
        });
    }
}

var messageStyle = (message, style) => {
    return [message];
};

const EVENT_DESCRIPTION_SYMBOL = Symbol("eventemitter.description");
const eventDebugStyle = {
    head: messageStyle,
    MIDNIGHTBLUE_BOLD_UNDERLINE: "color:midnightblue;text-decoration: underline;font-weight: bold;",
    DARKVIOLET_BOLD_UNDERLINE: "color:darkviolet;text-decoration: underline;font-weight: bold;",
};

const isNodejs = Boolean(typeof process !== "undefined" &&
    process &&
    process.versions &&
    process.versions.node);
typeof process !== "undefined" &&
    (process.platform === "win32" ||
        /^(msys|cygwin)$/.test(process.env && process.env.OSTYPE));

const checkType$1 = (name, type) => {
    try {
        return new Function(`return typeof ${name} === "${type}"`)();
    }
    catch (_) {
        return false;
    }
};
const isCordova = checkType$1("cordova", "object");
/**web worker and main thread all has location as navigator */
const isWebView = checkType$1("navigator", "object");
const isDenoRuntime$1 = checkType$1("Deno", "object");
const isAndroid = isWebView && /Android/i.test(navigator.userAgent);
const isIOS = isWebView && /iPhone|iPod|iPad/i.test(navigator.userAgent);
const isWebMainThread = isWebView && checkType$1("document", "object");
const isWebWorker = isWebView && !isWebMainThread;
const platformInfo = {
    getGlobalFlag(flag_name, defaultValue = "") {
        const g = isDenoRuntime$1 ? this : this.global();
        return (g[flag_name] ||
            (g.process && g.process.env && g.process.env[flag_name]) ||
            (g.location &&
                g.location.href &&
                new URL(g.location.href).searchParams.get(flag_name)) ||
            (g.localStorage && g.localStorage.getItem(flag_name)) ||
            defaultValue);
    },
    global() {
        return $global;
    },
    platformName() {
        if (isNodejs) {
            return "Nodejs";
        }
        const device_name = isAndroid ? "Android" : isIOS ? "IOS" : "unknown";
        if (isCordova) {
            return "Cordova-" + device_name;
        }
        if (isWebMainThread) {
            return "WebMaster-" + device_name;
        }
        if (isWebWorker) {
            return "WebWorker-" + device_name;
        }
        return "UNKNOWN";
    },
    getChannel() {
        return "UNKNOWN";
    },
    getBusiness() {
        return "UNKNOWN";
    },
};

const ENV = platformInfo.getGlobalFlag("dev-flag", "development");
ENV.split(",").map((flag) => flag.trim());
const _envFlags = new Map();
for (const flag of ENV.split(",")) {
    const [_flagKey, flagValue] = flag.split("=").map((item) => item.trim());
    let flagKey = _flagKey;
    let remove = false;
    if (flagKey.startsWith("- ")) {
        remove = true;
        flagKey = flagKey.substr(2);
    }
    if (remove) {
        _envFlags.delete(flagKey);
    }
    else {
        _envFlags.set(flagKey, flagValue);
    }
}
function isFlagInDev(flag) {
    return _envFlags.has(flag);
}

const isDev = isFlagInDev("eventemitter") && isFlagInDev("browser");

class MapEventEmitter {
    constructor() {
        /**导出类型 */
        Object.defineProperty(this, "TYPE", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: void 0
        });
        Object.defineProperty(this, "_e", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: Object.create(null)
        });
        /**是否由过自定义异常处理 */
        Object.defineProperty(this, "_hasEmitErrorHandlerSet", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: void 0
        });
        //#endregion
    }
    on(eventname, handler, opts = {}) {
        this._on(eventname, handler, opts.taskname, opts.once);
    }
    /** on函数的具体实现 */
    _on(eventname, handler, taskname, once) {
        const eventHanldersMap = this._e;
        let eventSet = eventHanldersMap[eventname];
        if (!eventSet) {
            eventSet = eventHanldersMap[eventname] = new Map();
        }
        else if (eventSet.has(handler)) {
            console.warn(`hanlder '${handler.name}' already exits in event set ${String(eventname)}.`);
        }
        if (taskname === undefined) {
            taskname = GetCallerInfo(this.constructor);
        }
        eventSet.set(handler, {
            taskname,
            once,
        });
    }
    once(eventname, handler, opts = {}) {
        this._on(eventname, handler, opts.taskname, true);
    }
    off(eventname, handler) {
        return this._off(eventname, handler);
    }
    _off(eventname, handler) {
        const eventMap = this._e[eventname];
        let res = true;
        if (eventMap) {
            if (handler) {
                const res = eventMap.delete(handler);
                if (res && eventMap.size === 0) {
                    delete this._e[eventname];
                }
            }
            else {
                eventMap.clear();
                delete this._e[eventname];
            }
        }
        else {
            res = false;
        }
        return res;
    }
    get [EVENT_DESCRIPTION_SYMBOL]() {
        return "";
    }
    emit(eventname, ...args) {
        this._emit(eventname, args);
    }
    _emit(eventname, args) {
        /**
         * 触发针对性的监听任务
         */
        const eventMap = this._e[eventname];
        if (isDev) {
            console.group(...eventDebugStyle.head("%s EMIT [%s]", eventDebugStyle.MIDNIGHTBLUE_BOLD_UNDERLINE), this[EVENT_DESCRIPTION_SYMBOL] || this, eventname);
            console.log(...eventDebugStyle.head("%s ARGS:", eventDebugStyle.MIDNIGHTBLUE_BOLD_UNDERLINE), ...args);
        }
        if (eventMap) {
            for (const [handler, opts] of eventMap.entries()) {
                try {
                    if (isDev) {
                        const { taskname = handler.name } = opts;
                        console.log(...eventDebugStyle.head("%s RUN [%s]", eventDebugStyle.MIDNIGHTBLUE_BOLD_UNDERLINE), this[EVENT_DESCRIPTION_SYMBOL] || this, taskname);
                    }
                    const res = handler(...args);
                    if (res instanceof Promise) {
                        res.catch((err) => this._emitErrorHanlder(err, eventname, args));
                    }
                }
                catch (err) {
                    this._emitErrorHanlder(err, eventname, args);
                }
                finally {
                    if (opts.once) {
                        eventMap.delete(handler);
                    }
                }
            }
        }
        isDev && console.groupEnd();
    }
    //#region on emit error
    /**
     * 触发内部的异常处理函数
     * @param err
     * @param han
     * @param name
     */
    _emitErrorHanlder(err, eventname, args) {
        if (this._hasEmitErrorHandlerSet) {
            for (const errorHandler of this._emitErrorHandlerSet) {
                /// 这里如果还是异常就不作处理了，直接抛到未捕获异常中就好
                errorHandler(err, {
                    // hanlder: hanlder ,//as $MutArgEventHandler<EM[keyof EM]>,
                    eventname,
                    args,
                });
            }
        }
        else {
            isDev &&
                console.error(`EventEmitter '${this.constructor.name}' emit '${eventname.toString()}' fail:`, err);
            throw err;
        }
    }
    get _emitErrorHandlerSet() {
        this._hasEmitErrorHandlerSet = true;
        return new Set();
    }
    /**
     * 自定义函数执行异常处理器
     * @param errorHandler
     */
    onError(errorHandler, taskname) {
        if (typeof taskname === "string") {
            renameFunction(errorHandler, taskname);
        }
        if (this._emitErrorHandlerSet.has(errorHandler)) {
            console.warn(`hanlder '${errorHandler.name}' already exits in custom error hanlder event set.`);
        }
        this._emitErrorHandlerSet.add(errorHandler);
    }
    /**
     * 移除自定义函数的执行异常处理器
     * @param errorHandler
     */
    offError(errorHandler) {
        if (!this._hasEmitErrorHandlerSet) {
            return false;
        }
        if (errorHandler) {
            return this._emitErrorHandlerSet.delete(errorHandler);
        }
        this._emitErrorHandlerSet.clear();
        return true;
    }
    //#endregion
    /**
     * 移除所有的事件
     */
    clear(opts = {}) {
        /// 直接清理掉
        this._e = Object.create(null);
        const { ignoreCustomErrorHanlder } = opts;
        /// 默认清理掉自定义错误的回调合集
        if (!ignoreCustomErrorHanlder && this._hasEmitErrorHandlerSet) {
            this._emitErrorHandlerSet.clear();
        }
    }
    //#region 同名拓展
    get removeAllListeners() {
        return this.clear;
    }
    get addListener() {
        return this.on;
    }
    get removeListener() {
        return this.off;
    }
}
__decorate([
    cacheGetter,
    __metadata("design:type", Object),
    __metadata("design:paramtypes", [])
], MapEventEmitter.prototype, "_emitErrorHandlerSet", null);
__decorate([
    cacheGetter,
    __metadata("design:type", Object),
    __metadata("design:paramtypes", [])
], MapEventEmitter.prototype, "removeAllListeners", null);
__decorate([
    cacheGetter,
    __metadata("design:type", Object),
    __metadata("design:paramtypes", [])
], MapEventEmitter.prototype, "addListener", null);
__decorate([
    cacheGetter,
    __metadata("design:type", Object),
    __metadata("design:paramtypes", [])
], MapEventEmitter.prototype, "removeListener", null);

let EasyMap$1 = class EasyMap extends Map {
    // private _map: Map<F, V>;
    constructor(creater, entries, transformKey = (v) => v, _afterDelete) {
        super(entries);
        Object.defineProperty(this, "creater", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: creater
        });
        Object.defineProperty(this, "transformKey", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: transformKey
        });
        Object.defineProperty(this, "_afterDelete", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: _afterDelete
        });
    }
    static from(args) {
        return new EasyMap$1(args.creater, args.entries, args.transformKey, args.afterDelete);
    }
    forceGet(key, creater = this.creater) {
        const k = this.transformKey(key);
        if (super.has(k)) {
            return super.get(k);
        }
        const res = creater(key, k);
        super.set(k, res);
        return res;
    }
    tryGet(key) {
        return this.get(this.transformKey(key));
    }
    trySet(key, val) {
        return this.set(this.transformKey(key), val);
    }
    tryDelete(key) {
        return this.delete(this.transformKey(key));
    }
    tryHas(key) {
        return this.has(this.transformKey(key));
    }
    delete(key) {
        const res = super.delete(key);
        if (res && this._afterDelete) {
            this._afterDelete(key);
        }
        return res;
    }
    get [Symbol.toStringTag]() {
        return "EasyMap";
    }
    static call(_this, creater, entries, transformKey, _afterDelete) {
        if (!(_this instanceof EasyMap$1)) {
            throw new TypeError("please use new keyword to create EasyMap instance.");
        }
        const protoMap = new EasyMap$1(creater, entries, transformKey, _afterDelete);
        const protoMap_PROTO = Object.getPrototypeOf(protoMap);
        const protoMap_PROTO_PROTO = Object.getPrototypeOf(protoMap_PROTO);
        const mapProps = Object.getOwnPropertyDescriptors(protoMap_PROTO_PROTO);
        for (const key in mapProps) {
            if (key !== "constructor") {
                const propDes = mapProps[key];
                if (typeof propDes.value === "function") {
                    propDes.value = propDes.value.bind(protoMap);
                }
                else {
                    if (typeof propDes.get === "function") {
                        propDes.get = propDes.get.bind(protoMap);
                    }
                    if (typeof propDes.set === "function") {
                        propDes.set = propDes.set.bind(protoMap);
                    }
                }
                Object.defineProperty(_this, key, propDes);
            }
        }
        const easymapProps = Object.getOwnPropertyDescriptors(protoMap_PROTO);
        for (const key in easymapProps) {
            if (key !== "constructor") {
                const propDes = easymapProps[key];
                if (typeof propDes.value === "function") {
                    propDes.value = propDes.value.bind(protoMap);
                }
                else {
                    if (typeof propDes.get === "function") {
                        propDes.get = propDes.get.bind(protoMap);
                    }
                    if (typeof propDes.set === "function") {
                        propDes.set = propDes.set.bind(protoMap);
                    }
                }
                Object.defineProperty(_this, key, propDes);
            }
        }
        const thisProps = Object.getOwnPropertyDescriptors(protoMap);
        for (const key in thisProps) {
            if (key !== "constructor")
                Object.defineProperty(_this, key, {
                    enumerable: true,
                    configurable: true,
                    get() {
                        return Reflect.get(protoMap, key);
                    },
                    set(v) {
                        Reflect.set(protoMap, key, v);
                    },
                });
        }
        return _this;
    }
};

/**
 * @param value
 * @returns
 * @inline
 */
const isPromiseLike = (value) => {
    return (value instanceof Object &&
        typeof value.then === "function");
};

class PromiseOut {
    constructor() {
        Object.defineProperty(this, "promise", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: void 0
        });
        Object.defineProperty(this, "is_resolved", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: false
        });
        Object.defineProperty(this, "is_rejected", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: false
        });
        Object.defineProperty(this, "is_finished", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: false
        });
        Object.defineProperty(this, "value", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: void 0
        });
        Object.defineProperty(this, "reason", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: void 0
        });
        Object.defineProperty(this, "resolve", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: void 0
        });
        Object.defineProperty(this, "reject", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: void 0
        });
        Object.defineProperty(this, "_innerFinally", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: void 0
        });
        Object.defineProperty(this, "_innerFinallyArg", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: void 0
        });
        Object.defineProperty(this, "_innerThen", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: void 0
        });
        Object.defineProperty(this, "_innerCatch", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: void 0
        });
        this.promise = new Promise((resolve, reject) => {
            this.resolve = (value) => {
                try {
                    if (isPromiseLike(value)) {
                        value.then(this.resolve, this.reject);
                    }
                    else {
                        this.is_resolved = true;
                        this.is_finished = true;
                        resolve((this.value = value));
                        this._runThen();
                        this._innerFinallyArg = Object.freeze({
                            status: "resolved",
                            result: this.value,
                        });
                        this._runFinally();
                    }
                }
                catch (err) {
                    this.reject(err);
                }
            };
            this.reject = (reason) => {
                this.is_rejected = true;
                this.is_finished = true;
                reject((this.reason = reason));
                this._runCatch();
                this._innerFinallyArg = Object.freeze({
                    status: "rejected",
                    reason: this.reason,
                });
                this._runFinally();
            };
        });
    }
    onSuccess(innerThen) {
        if (this.is_resolved) {
            this.__callInnerThen(innerThen);
        }
        else {
            (this._innerThen || (this._innerThen = [])).push(innerThen);
        }
    }
    onError(innerCatch) {
        if (this.is_rejected) {
            this.__callInnerCatch(innerCatch);
        }
        else {
            (this._innerCatch || (this._innerCatch = [])).push(innerCatch);
        }
    }
    onFinished(innerFinally) {
        if (this.is_finished) {
            this.__callInnerFinally(innerFinally);
        }
        else {
            (this._innerFinally || (this._innerFinally = [])).push(innerFinally);
        }
    }
    _runFinally() {
        if (this._innerFinally) {
            for (const innerFinally of this._innerFinally) {
                this.__callInnerFinally(innerFinally);
            }
            this._innerFinally = undefined;
        }
    }
    __callInnerFinally(innerFinally) {
        queueMicrotask(async () => {
            try {
                await innerFinally(this._innerFinallyArg);
            }
            catch (err) {
                console.error("Unhandled promise rejection when running onFinished", innerFinally, err);
            }
        });
    }
    _runThen() {
        if (this._innerThen) {
            for (const innerThen of this._innerThen) {
                this.__callInnerThen(innerThen);
            }
            this._innerThen = undefined;
        }
    }
    _runCatch() {
        if (this._innerCatch) {
            for (const innerCatch of this._innerCatch) {
                this.__callInnerCatch(innerCatch);
            }
            this._innerCatch = undefined;
        }
    }
    __callInnerThen(innerThen) {
        queueMicrotask(async () => {
            try {
                await innerThen(this.value);
            }
            catch (err) {
                console.error("Unhandled promise rejection when running onSuccess", innerThen, err);
            }
        });
    }
    __callInnerCatch(innerCatch) {
        queueMicrotask(async () => {
            try {
                await innerCatch(this.value);
            }
            catch (err) {
                console.error("Unhandled promise rejection when running onError", innerCatch, err);
            }
        });
    }
}

new TextEncoder();
const _decoder = new TextDecoder("utf-8");
const stringToByte = (s) => {
    const res = new Uint16Array(s.length);
    for (let i = 0; i < s.length; i += 1) {
        const u = s.codePointAt(i);
        if (u) {
            res[i] = u;
        }
    }
    return res;
};
/**
 * arrayBuffer to String
 * @param buffer
 * @returns
 */
const bufferToString = (buffer) => {
    if (ArrayBuffer.isView(buffer)) {
        // return String.fromCharCode.apply(null, buffer as number[])
        return _decoder.decode(buffer.buffer);
    }
    return _decoder.decode(buffer);
};
/**
 * 合并Uint16array
 * @param arrs
 * @returns
 */
const contactUint16 = (...arrs) => {
    const length = arrs.reduce((l, a) => l += a.length, 0);
    const r = new Uint16Array(length);
    let walk = 0;
    for (const arr of arrs) {
        r.set(arr, walk);
        walk += arr.length;
    }
    return r;
};
/**
 * 合并Uint16array
 * @param arrs
 * @returns
 */
const contactUint8 = (...arrs) => {
    const length = arrs.reduce((l, a) => l += a.length, 0);
    const r = new Uint8Array(length);
    let walk = 0;
    for (const arr of arrs) {
        r.set(arr, walk);
        walk += arr.length;
    }
    return r;
};
/**
 * hex string to Uint8Array
 * @param hex string
 * @returns Uint8Array
 */
const hexToBinary = (hex) => {
    return Uint8Array.from(hex.split(",").map(v => +v));
};

/////////////////////////////
const checkType = (name, type) => {
    try {
        return new Function(`return typeof ${name} === "${type}"`)();
    }
    catch (_) {
        return false;
    }
};

var EChannelMode;
(function (EChannelMode) {
    EChannelMode["static"] = "static";
    EChannelMode["pattern"] = "pattern";
})(EChannelMode || (EChannelMode = {}));
var ECommand;
(function (ECommand) {
    ECommand["openBackPressure"] = "openBackPressure";
    ECommand["openChannel"] = "openChannel";
    ECommand["openMessageChannel"] = "openMessageChannel";
    ECommand["registerChannelId"] = "registerChannelId"; // 注册一个channel id
})(ECommand || (ECommand = {}));

// your OS.
/**js 到rust的消息 */
function js_to_rust_buffer(zerocopybuffer) {
    Deno.core.opSync("op_js_to_rust_buffer", zerocopybuffer);
}
/**js 到rust的消息： 传递零拷贝消息 */
function send_zero_copy_buffer(req_id, zerocopybuffer) {
    let buffer;
    // 需要解析成Uint8
    if (zerocopybuffer.buffer.byteLength % 2 !== 0) {
        buffer = contactUint8(new Uint8Array(req_id.buffer), zerocopybuffer);
    }
    else {
        buffer = contactUint16(req_id, new Uint16Array(zerocopybuffer.buffer));
    }
    Deno.core.opSync("op_send_zero_copy_buffer", buffer);
}
/**
 * 循环从rust里拿数据
 * 这里拿的是service worker 构建的 chunk的数据
 */
async function getRustChunk() {
    const buffer = await Deno.core.opAsync("op_rust_to_js_buffer"); // backDataToRust
    // 没得数据回来
    if (buffer[0] === 0) {
        return {
            value: buffer,
            done: true,
        };
    }
    return {
        value: buffer,
        done: false,
    };
}
/**循环从rust里拿数据 */
function getRustBuffer(ex_head_view) {
    const uint8_head = new Uint8Array(ex_head_view.buffer);
    const data = `${uint8_head[0]}-${uint8_head[1]}`;
    const buffer = Deno.core.opSync("op_rust_to_js_system_buffer", data); // backSystemDataToRust
    if (buffer[0] === 0 && buffer.length === 1) {
        return {
            value: buffer,
            done: true,
        };
    }
    console.log("getRustBuffer2: -->  ", buffer);
    // 如果是普通消息,versionID == 1
    if (buffer[0] === 1) {
        buffer.splice(0, 2); //拿到版本号
        buffer.splice(0, 2); // 拿到头部标记
    }
    const buff = new Uint8Array(buffer);
    return {
        value: buff,
        done: false,
    };
}

class EasyMap extends Map {
    // private _map: Map<F, V>;
    constructor(creater, entries, transformKey = (v) => v, _afterDelete) {
        super(entries);
        Object.defineProperty(this, "creater", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: creater
        });
        Object.defineProperty(this, "transformKey", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: transformKey
        });
        Object.defineProperty(this, "_afterDelete", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: _afterDelete
        });
    }
    static from(args) {
        return new EasyMap(args.creater, args.entries, args.transformKey, args.afterDelete);
    }
    forceGet(key, creater = this.creater) {
        const k = this.transformKey(key);
        if (super.has(k)) {
            return super.get(k);
        }
        const res = creater(key, k);
        super.set(k, res);
        return res;
    }
    tryGet(key) {
        return this.get(this.transformKey(key));
    }
    trySet(key, val) {
        return this.set(this.transformKey(key), val);
    }
    tryDelete(key) {
        return this.delete(this.transformKey(key));
    }
    tryHas(key) {
        return this.has(this.transformKey(key));
    }
    delete(key) {
        const res = super.delete(key);
        if (res && this._afterDelete) {
            this._afterDelete(key);
        }
        return res;
    }
    get [Symbol.toStringTag]() {
        return "EasyMap";
    }
    static call(_this, creater, entries, transformKey, _afterDelete) {
        if (!(_this instanceof EasyMap)) {
            throw new TypeError("please use new keyword to create EasyMap instance.");
        }
        const protoMap = new EasyMap(creater, entries, transformKey, _afterDelete);
        const protoMap_PROTO = Object.getPrototypeOf(protoMap);
        const protoMap_PROTO_PROTO = Object.getPrototypeOf(protoMap_PROTO);
        const mapProps = Object.getOwnPropertyDescriptors(protoMap_PROTO_PROTO);
        for (const key in mapProps) {
            if (key !== "constructor") {
                const propDes = mapProps[key];
                if (typeof propDes.value === "function") {
                    propDes.value = propDes.value.bind(protoMap);
                }
                else {
                    if (typeof propDes.get === "function") {
                        propDes.get = propDes.get.bind(protoMap);
                    }
                    if (typeof propDes.set === "function") {
                        propDes.set = propDes.set.bind(protoMap);
                    }
                }
                Object.defineProperty(_this, key, propDes);
            }
        }
        const easymapProps = Object.getOwnPropertyDescriptors(protoMap_PROTO);
        for (const key in easymapProps) {
            if (key !== "constructor") {
                const propDes = easymapProps[key];
                if (typeof propDes.value === "function") {
                    propDes.value = propDes.value.bind(protoMap);
                }
                else {
                    if (typeof propDes.get === "function") {
                        propDes.get = propDes.get.bind(protoMap);
                    }
                    if (typeof propDes.set === "function") {
                        propDes.set = propDes.set.bind(protoMap);
                    }
                }
                Object.defineProperty(_this, key, propDes);
            }
        }
        const thisProps = Object.getOwnPropertyDescriptors(protoMap);
        for (const key in thisProps) {
            if (key !== "constructor")
                Object.defineProperty(_this, key, {
                    enumerable: true,
                    configurable: true,
                    get() {
                        return Reflect.get(protoMap, key);
                    },
                    set(v) {
                        Reflect.set(protoMap, key, v);
                    },
                });
        }
        return _this;
    }
}

let _L = 0;
var Transform_Type;
(function (Transform_Type) {
    /**不需要返回值的消息 */
    Transform_Type[Transform_Type["NOT_RETURN"] = 1 << _L++] = "NOT_RETURN";
    /**通用的消息 */
    Transform_Type[Transform_Type["HAS_RETURN"] = 1 << _L++] = "HAS_RETURN";
    /**传递buffer的消息 */
    Transform_Type[Transform_Type["IS_ALL_BUFFER"] = 1 << _L++] = "IS_ALL_BUFFER";
    // IS_ALL_JSON = 1 >> L++,
    // IS_ALL_STRING = 1 >> L++,
    // IS_ALL_U32 = 1 >> L++,
    // IS_ALL_BOOLEAN = 1 >> L++,
})(Transform_Type || (Transform_Type = {}));

/////////////////////////////
const REQ_CATCH = EasyMap.from({
    creater(_req_id) {
        return {
            po: new PromiseOut()
        };
    },
});
let Deno$1 = class Deno {
    constructor() {
        Object.defineProperty(this, "version_id", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: new Uint16Array([1])
        });
        Object.defineProperty(this, "reqId", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: new Uint16Array([0])
        }); // 初始化头部标记
    }
    async request(cmd, input, type) {
        const zerocopybuffer_list = [];
        const transferable_metadata = [];
        let z_acc_id = 0;
        // 处理 buffer view
        const copy_list = input.map((value, index) => {
            if (ArrayBuffer.isView(value)) {
                console.log("deno#zerocopybuffer_list:", index, value);
                zerocopybuffer_list.push(value);
                transferable_metadata.push(index, z_acc_id++);
                return z_acc_id;
            }
            return value;
        });
        this.postMessageToKotlin(this.reqId, cmd, type, JSON.stringify(copy_list), zerocopybuffer_list, transferable_metadata);
        // 如果不需要返回值
        if ((type & Transform_Type.NOT_RETURN) === Transform_Type.NOT_RETURN) {
            console.log("deno#request,不需要返回值:", cmd);
            return new ArrayBuffer(1);
        }
        return await REQ_CATCH.forceGet(this.reqId).po.promise;
    }
    /** 发送请求 */
    postMessageToKotlin(req_id, cmd, type, data_string, zerocopybuffer_list, transferable_metadata) {
        this.headViewAdd();
        console.log("deno#postMessageToKotlin#🚓cmd： %s, data_string:%s，req_id:%s", cmd, data_string, req_id[0]);
        // 发送bufferview
        if (zerocopybuffer_list.length !== 0) {
            zerocopybuffer_list.map((zerocopybuffer) => {
                send_zero_copy_buffer(req_id, zerocopybuffer);
            });
        }
        // 发送具体操作消息
        this.callFunction(cmd, type, data_string, transferable_metadata);
        // 需要返回值的才需要等待
        if ((type & Transform_Type.NOT_RETURN) !== Transform_Type.NOT_RETURN) {
            this.loopGetKotlinReturn(req_id, cmd);
        }
    }
    headViewAdd() {
        this.reqId[0]++;
    }
    /**
     * 调用deno的函数
     * @param handleFn
     * @param data
     */
    callFunction(handleFn, type, data = "''", transferable_metadata) {
        const body = this.structureBinary(handleFn, type, data, transferable_metadata);
        // 发送消息
        js_to_rust_buffer(body); // android - denoOp
    }
    /**
     * 循环获取kotlin system 返回的数据
     * @returns
     */
    async loopGetKotlinReturn(reqId, cmd) {
        do {
            const result = await getRustBuffer(reqId); // backSystemDataToRust
            if (result.done) {
                continue;
            }
            console.log(`deno#loopGetKotlinReturn ✅:${cmd},req_id,当前请求的：${this.reqId[0]},是否存在请求：${REQ_CATCH.has(this.reqId)}`);
            REQ_CATCH.get(this.reqId)?.po.resolve(result.value);
            REQ_CATCH.delete(this.reqId);
            break;
        } while (true);
    }
    /** 针对64位
     * 第一块分区：版本号 2^8 8位，一个字节 1：表示消息，2：表示广播，4：心跳检测
     * 第二块分区：头部标记 2^16 16位 两个字节  根据版本号这里各有不同，假如是消息，就是0，1；如果是广播则是组
     * 第三块分区：数据主体 动态创建
     */
    structureBinary(fn, type, data = "", transferable_metadata) {
        // op(send , version:number, cmd:string, reqId:number, type:number, data:string, transferable_metadata:number[])
        const message = `{"cmd":"${fn}","type":${type},"data":${data},"transferable_metadata":[${transferable_metadata.join()}]}`;
        // 字符 转 Uint16Array
        const body = stringToByte(message);
        return contactUint16(this.version_id, this.reqId, body);
    }
};
const deno = new Deno$1();

// dnt-shim-ignore
// deno-lint-ignore no-explicit-any
var jscore = globalThis
    .PlaocJavascriptBridge;

// 记得值需要大写开头，跟Native enum  保持一直
var callNative;
(function (callNative) {
    /**打开dwebView */
    callNative["openDWebView"] = "OpenDWebView";
    /**二维码 */
    callNative["openQrScanner"] = "OpenQrScanner";
    /**条形码 */
    callNative["openBarcodeScanner"] = "BarcodeScanner";
    /**初始化app数据 */
    callNative["initMetaData"] = "InitMetaData";
    /**初始化运行时 */
    callNative["denoRuntime"] = "DenoRuntime";
    /**获取appid */
    callNative["getBfsAppId"] = "GetBfsAppId";
    /**传递给前端消息 */
    callNative["evalJsRuntime"] = "EvalJsRuntime";
    /**获取设备信息 */
    callNative["getDeviceInfo"] = "GetDeviceInfo";
    /**发送消息 */
    callNative["sendNotification"] = "SendNotification";
    /**申请权限 */
    callNative["applyPermissions"] = "ApplyPermissions";
    /**检查是否有摄像头权限，如果没有或者被拒绝，那么会强制请求打开权限（设置） */
    callNative["checkCameraPermission"] = "CheckCameraPermission";
    /**获取权限信息 */
    callNative["getPermissions"] = "GetPermissions";
    /** serviceworker 告知已经准备好 */
    callNative["serviceWorkerReady"] = "ServiceWorkerReady";
    /**设置dwebview的ui */
    callNative["setDWebViewUI"] = "SetDWebViewUI";
    /** 剪切板 */
    callNative["writeClipboardContent"] = "WriteClipboardContent";
    callNative["readClipboardContent"] = "ReadClipboardContent";
    /** 获取网络状态 */
    callNative["getNetworkStatus"] = "GetNetworkStatus";
    /** camera */
    callNative["takeCameraPhoto"] = "TakeCameraPhoto";
    callNative["pickCameraPhoto"] = "PickCameraPhoto";
    callNative["pickCameraPhotos"] = "PickCameraPhotos";
})(callNative || (callNative = {}));
/**不需要返回的命令 */
var callNotReturnNative;
(function (callNotReturnNative) {
    /**退出app */
    callNotReturnNative["exitApp"] = "ExitApp";
    /** toast 提示 */
    callNotReturnNative["showToast"] = "ShowToast";
    /** share 系统分享 */
    callNotReturnNative["systemShare"] = "SystemShare";
    /** haptics 交互 */
    callNotReturnNative["hapticsImpact"] = "HapticsImpact";
    callNotReturnNative["hapticsNotification"] = "HapticsNotification";
    callNotReturnNative["hapticsVibrate"] = "HapticsVibrate";
})(callNotReturnNative || (callNotReturnNative = {}));
// 回调到对应的组件
var callDVebView;
(function (callDVebView) {
    callDVebView["BarcodeScanner"] = "dweb-scanner";
    callDVebView["OpenQrScanner"] = "dweb-scanner";
    callDVebView["OpenDWebView"] = "dweb-view";
    callDVebView["ExitApp"] = "dweb-app";
    callDVebView["ApplyPermissions"] = "dweb-permission";
    callDVebView["CheckCameraPermission"] = "dweb-permission";
    callDVebView["GetPermissions"] = "dweb-permission";
    callDVebView["ShowToast"] = "dweb-app";
    callDVebView["SystemShare"] = "dweb-app";
    callDVebView["GetNetworkStatus"] = "dweb-app";
    callDVebView["HapticsImpact"] = "dweb-app";
    callDVebView["HapticsNotification"] = "dweb-app";
    callDVebView["HapticsVibrate"] = "dweb-app";
    callDVebView["ReadClipboardContent"] = "dweb-app";
    callDVebView["WriteClipboardContent"] = "dweb-app";
    callDVebView["TakeCameraPhoto"] = "dweb-camera";
    callDVebView["PickCameraPhoto"] = "dweb-camera";
    callDVebView["PickCameraPhotos"] = "dweb-camera";
})(callDVebView || (callDVebView = {}));
// const callDeno
// 需要ios异步返回结果方法
var callIOSAsyncFunc;
(function (callIOSAsyncFunc) {
    callIOSAsyncFunc["ApplyPermissions"] = "ApplyPermissions";
    callIOSAsyncFunc["OpenQrScanner"] = "OpenQrScanner";
    callIOSAsyncFunc["BarcodeScanner"] = "BarcodeScanner";
    callIOSAsyncFunc["TakeCameraPhoto"] = "TakeCameraPhoto";
    callIOSAsyncFunc["PickCameraPhoto"] = "PickCameraPhoto";
    callIOSAsyncFunc["PickCameraPhotos"] = "PickCameraPhotos";
})(callIOSAsyncFunc || (callIOSAsyncFunc = {}));

function netCallNativeService(fn, data = "") {
    console.log("🥳deno#netCallNativeService:", fn, data);
    const uint8 = jscore.callJavaScriptWithFunctionNameParam(fn, data);
    if (!uint8)
        return new Uint8Array(0);
    console.log("netCallNativeService:==>", fn, uint8);
    return uint8;
}

/**判断是不是denoRuntime环境 */
function isDenoRuntime() {
    return checkType("Deno", "object");
}
var EDeviceModule;
(function (EDeviceModule) {
    EDeviceModule["default"] = "default";
    EDeviceModule["silentMode"] = "silentMode";
    EDeviceModule["doNotDisturb"] = "doNotDisturb";
})(EDeviceModule || (EDeviceModule = {}));

/** 判断当前属于哪个平台 */
function currentPlatform() {
    let platform = "";
    if (jscore) {
        platform = "ios";
    }
    else if (isDenoRuntime()) {
        platform = "android";
    }
    else {
        platform = "desktop";
    }
    return platform;
}
var EPlatform;
(function (EPlatform) {
    EPlatform["ios"] = "ios";
    EPlatform["android"] = "android";
    EPlatform["desktop"] = "desktop";
})(EPlatform || (EPlatform = {}));

class Network {
    /**
     * 异步调用方法,这个是给后端调用的方法，不会传递数据到前端
     * @param handleFn
     * @param data
     * @returns
     */
    async asyncCallDenoFunction(handleFn, data = "") {
        return await this.asyncSendMsgNative(handleFn, data).then((data) => {
            if (currentPlatform() === EPlatform.ios) {
                return data;
            }
            const result = _decoder.decode(data);
            return result;
        }).catch((err) => {
            console.log("deno#asyncCallDenoFunction err", err);
            return err;
        });
    }
    /**
     * 异步调用方法,这个是给后端调用的方法，不会传递数据到前端
     * @param handleFn
     * @param data
     * @returns  Buffer
     */
    async asyncCallbackBuffer(handleFn, data = "") {
        return await this.asyncSendMsgNative(handleFn, data);
    }
    /**
     * 异步发送消息到android/ios
     * @param handleFn
     * @param data
     * @returns
     */
    async asyncSendMsgNative(handleFn, data = "") {
        // 发送消息的类型（标记为需要消息返回）
        const type = Transform_Type.HAS_RETURN;
        if (data instanceof Object && !ArrayBuffer.isView(data)) {
            data = JSON.stringify(data); // stringify 两次转义一下双引号
        }
        // console.log("deno#asyncSendMsgNative request: ", handleFn, data)
        // 处理IOS，可以不转buffer就不转，少了一道工序
        if (currentPlatform() === EPlatform.ios) {
            const msg = await netCallNativeService(handleFn, data);
            return msg;
        }
        // 发送请求
        const buffer = await deno.request(handleFn, [data], type);
        // console.log("deno#asyncSendMsgNative Response: ", buffer)
        return buffer;
    }
    /**
     * 同步调用方法没返回值
     * @param handleFn
     * @param data
     */
    async syncSendMsgNative(handleFn, data = "") {
        // 发送消息的类型 （标记为不需要返回）
        const type = Transform_Type.NOT_RETURN;
        if (data instanceof Object) {
            data = JSON.stringify(data); // stringify 两次转义一下双引号
        }
        // 处理IOS，
        if (currentPlatform() === EPlatform.ios) {
            netCallNativeService(handleFn, data);
            return;
        }
        console.log("syncSendMsgNative#request: ", handleFn, data);
        await deno.request(handleFn, [data], type); // 发送请求
    }
    /**
     * 分段发送buffer请求到native
     * @param handleFn
     * @param data
     * @returns
     */
    async asyncSendBufferNative(handleFn, data) {
        // 发送消息的类型（标记为需要消息返回，并且是二进制传输）
        const type = Transform_Type.HAS_RETURN | Transform_Type.IS_ALL_BUFFER;
        // 处理IOS，
        if (currentPlatform() === EPlatform.ios) {
            netCallNativeService(handleFn, data);
        }
        // 发送请求
        const buffer = await deno.request(handleFn, data, type);
        console.log("deno#asyncSendBufferNative Response: ", buffer);
        return buffer;
    }
}
const network = new Network();

// /getBlockInfo 
// [{ "url": "/getBlockInfo", "response": "https://62b94efd41bf319d22797acd.mockapi.io/bfchain/v1/getBlockInfo" }, { "url": "/getBlockHigh", "response": "https://62b94efd41bf319d22797acd.mockapi.io/bfchain/v1/getBlockInfo" }, { "url": "/app/bfchain.dev/index.html", "response": "/app/bfchain.dev/index.html" }, { "url": "/api/*", "response": "./api/*" }, { "url": "/api/upload", "response": "/api/update" }]
/**
 * 代理数据请求
 * @param path
 * @param importMap
 */
async function parseNetData(event, pathname, importMap) {
    let url = "";
    const request = event.request;
    // 匹配bfsa-betadata.ts importMap 里面映射的
    importMap.map((obj) => {
        if (obj.url.includes(pathname)) {
            url = obj.response;
            return;
        }
    });
    // 
    // 如果没有在bfsa-metadata.ts里
    if (!url) {
        event.response.write("Not Found importMap in bfsa-metadata.ts !!!");
        event.response.end();
        return url;
    }
    let res;
    if (request.method.toUpperCase() === "GET") {
        //不含body
        res = await fetch(url, {
            headers: request.headers,
            method: request.method,
            mode: request.mode
        });
    }
    else {
        // 包含body
        res = await fetch(url, {
            headers: request.headers,
            method: request.method,
            mode: request.mode,
            body: request.body,
        });
    }
    // const buffer = await res.arrayBuffer(); // ⚠️考虑使用ReadableStream
    // headers
    res.headers.forEach((val, key) => {
        event.response.setHeaders(key, val);
    });
    // statusCode
    event.response.statusCode = res.status;
    if (res.ok && res.body) {
        const buff = res.body.getReader();
        while (true) {
            const { value, done } = await buff.read();
            if (done) {
                event.response.end();
                break;
            }
            console.log("bodyStringValue:", value, ArrayBuffer.isView(value));
            event.response.write(value);
        }
    }
}

async function warpPermissions(cmd, permissions) {
    console.log("deno#warpPermissions 权限申请:", cmd, permissions);
    if (cmd === callNative.applyPermissions) {
        return await applyPermissions(permissions);
    }
    if (cmd === callNative.checkCameraPermission) {
        return await applyPermissions(EPermissions.CAMERA);
    }
    if (cmd === callNative.getPermissions) {
        return await getPermissions();
    }
    return "";
}
/**
 * 申请权限(如果没有或者被拒绝，那么会强制请求打开权限（设置）)
 * @param permissions
 * @returns boolean
 */
async function applyPermissions(permissions) {
    console.log("deno#applyPermissions：", permissions, currentPlatform());
    return await network.asyncCallDenoFunction(callNative.applyPermissions, permissions);
}
/**
 * 获取权限信息
 * @returns Promsie<EPermissions>
 */
async function getPermissions() {
    return await network.asyncCallDenoFunction(callNative.getPermissions);
}
var EPermissions;
(function (EPermissions) {
    /**相机 */
    EPermissions["CAMERA"] = "PERMISSION_CAMERA";
    /**位置 */
    EPermissions["LOCATION"] = "PERMISSION_LOCATION";
    /**联系人 */
    EPermissions["CONTACTS"] = "PERMISSION_CONTACTS";
    /**录音 */
    EPermissions["RECORD_AUDIO"] = "PERMISSION_RECORD_AUDIO";
    /**相册(ios only) */
    EPermissions["PHOTO"] = "PERMISSION_PHOTO";
    /**网络(ios only) */
    EPermissions["NETWORK"] = "PERMISSION_NETWORK";
    /**媒体库(ios only) */
    EPermissions["MEDIA"] = "PERMISSION_MEDIA";
    /**通知(ios only) */
    EPermissions["NOTIFICATION"] = "PERMISSION_NOTIFICATION";
    /**蓝牙(ios only) */
    EPermissions["BLUETOOTH"] = "PERMISSION_BLUETOOTH";
    /**日历(android only) */
    EPermissions["CALENDAR"] = "PERMISSION_CALENDAR";
    /**传感器（重力，陀螺仪）(android only) */
    EPermissions["BODY_SENSORS"] = "PERMISSION_BODY_SENSORS";
    /**存储(android only) */
    EPermissions["STORAGE"] = "PERMISSION_STORAGE";
    /**短信(android only) */
    EPermissions["SMS"] = "PERMISSION_SMS";
    /**电话(android only) */
    EPermissions["CALL"] = "PERMISSION_CALL";
    /**手机状态(android only) */
    EPermissions["DEVICE"] = "PERMISSION_DEVICE";
})(EPermissions || (EPermissions = {}));

class RequestEvent {
    constructor(request, response, channelId, bodyId) {
        Object.defineProperty(this, "request", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: request
        });
        Object.defineProperty(this, "response", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: response
        });
        Object.defineProperty(this, "channelId", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: channelId
        });
        Object.defineProperty(this, "bodyId", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: bodyId
        });
    }
    // @cacheGetter
    get url() {
        return new URL(this.request.url, 'https://localhost');
    }
}
class RequestResponse {
    constructor(_bodyCtrl, _onClose) {
        Object.defineProperty(this, "_bodyCtrl", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: _bodyCtrl
        });
        Object.defineProperty(this, "_onClose", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: _onClose
        });
        Object.defineProperty(this, "statusCode", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: 200
        });
        Object.defineProperty(this, "headers", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: {}
        });
        Object.defineProperty(this, "_closed", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: false
        });
    }
    setHeaders(key, value) {
        this.headers[key] = value;
    }
    getHeaders(key) {
        return this.headers[key];
    }
    write(data) {
        if (this._closed) {
            throw new Error('closed');
        }
        // if (typeof data === 'string') {
        //   data = stringToByte(data)
        // }
        this._bodyCtrl.enqueue(data);
    }
    end() {
        console.log("deno#end:", this._closed);
        if (this._closed) {
            return;
        }
        this._closed = true;
        this._bodyCtrl.close();
        this._onClose(this.statusCode, this.headers);
    }
}
/**
 * 发送component UI 的样式请求
 * @param event
 * @returns string
 */
async function setUiHandle(event) {
    const { url } = event;
    const searchParams = url.searchParams.get("data");
    // 处理GET
    if (searchParams) {
        const data = await network.asyncCallbackBuffer(callNative.setDWebViewUI, searchParams);
        console.log("resolveSetUiHandleData:", data);
        event.response.write(data);
        event.response.end();
        return;
    }
    const body = event.request.body;
    // 如果没有get请求参数，又没有携带body
    if (!body) {
        console.log(`deno#setUiHandle Parameter passing cannot be empty！${body}`);
        return "Parameter passing cannot be empty！";
    }
    // console.log("deno#body 获取数据等待🚥:", event.bodyId)
    // await request_body_cache.forceGet(event.bodyId).op.promise; // 等待body的填充
    console.log("deno#body 准备获取数据📚:", event.bodyId);
    const buff = body.getReader();
    while (true) {
        const { value, done } = await buff.read();
        if (done) {
            console.log(`deno#body  传递数据结束`);
            break;
        }
        console.log(`deno#body  传递数据, body:`, value.length, ArrayBuffer.isView(value));
        const data = await network.asyncSendBufferNative(callNative.setDWebViewUI, [value]);
        event.response.write(data);
    }
    request_body_cache.delete(event.bodyId);
    // console.log("deno#body 删除了🏵", event.bodyId)
    event.response.end();
}
/**
 * 请求一些系统函数(扫码，手机信息)
 * @param event
 * @returns
 */
async function setPollHandle(event) {
    const { url } = event;
    const bufferData = url.searchParams.get("data");
    let buffer;
    // 如果是get
    if (bufferData) {
        buffer = hexToBinary(bufferData);
    }
    else {
        // 处理post 
        if (!event.request.body) {
            throw new Error("Parameter passing cannot be empty！"); // 如果没有任何请求体
        }
        buffer = await event.request.arrayBuffer();
    }
    const stringData = bufferToString(buffer);
    /// 如果是操作对象，拿出对象的操作函数和数据,传递给Kotlin
    const handler = JSON.parse(stringData);
    console.log("deno#setPollHandlestring Data:", stringData);
    // 看看是不是serviceWorekr准备好了
    if (getServiceWorkerReady(handler.function)) {
        return true;
    }
    basePollHandle(handler.function, handler.data);
}
/**
 * systemAPI逻辑相关操作
 * @param cmd
 * @param data
 * @returns
 */
async function basePollHandle(cmd, data) {
    console.log("deno#basePollHandle need return?:", cmd, Object.values(callNative).includes(cmd));
    if (!Object.values(callNative).includes(cmd)) {
        // 不需要返回值的调用
        network.syncSendMsgNative(cmd, data);
        return true;
    }
    let result = "";
    // 权限相关
    if (/Permission/.test(cmd)) {
        result = await warpPermissions(cmd, data);
    }
    else {
        result = await network.asyncCallDenoFunction(cmd, data);
    }
    // 需要ios异步返回结果，直接返回，在ios端通过jscore主动调用 callDwebViewFactory
    if (currentPlatform() === EPlatform.ios && cmd in callIOSAsyncFunc) {
        return;
    }
    console.log("deno#basePollHandle result: ", result);
    callDwebViewFactory(cmd, result);
}
/**
 * 数据传递到DwebView
 * @param data
 * @returns
 */
function callDwebViewFactory(func, data) {
    console.log("func: " + func);
    const handler = func;
    if (handler && callDVebView[handler]) {
        handlerEvalJs(handler, callDVebView[handler], data);
    }
}
/**
 * 传递消息给DwebView-js,路径为：deno-js-(op)->rust-(ffi)->kotlin-(evaljs)->dwebView-js
 * @param wb
 * @param data
 * @returns
 */
function handlerEvalJs(handler, wb, data) {
    console.log("handlerEvalJs:", wb, data);
    network.syncSendMsgNative(callNative.evalJsRuntime, `javascript:document.querySelector('${wb}').dispatchStringMessage('${handler}','${data}')`);
}
/**
 * 看看是不是serviceworker准备好了
 * @param fun
 * @returns
 */
function getServiceWorkerReady(fun) {
    console.log(`getServiceWorkerReady: ${fun} , ${fun === callNative.serviceWorkerReady}`);
    if (fun !== callNative.serviceWorkerReady) {
        return false;
    }
    // 执行事件
    for (const data of EventPollQueue) {
        openChannel(data);
    }
    callDwebViewFactory(fun, "true");
    return true;
}
/**
 * 打开一个channel通道
 * @param data
 * @returns
 */
function openChannel(config) {
    callSWPostMessage({ cmd: ECommand.openChannel, data: config });
}
/**
 * 申请一个channelId
 * @param channelId
 */
function applyChannelId(channelId) {
    callSWPostMessage({ cmd: ECommand.registerChannelId, data: channelId });
}
/**
* 发送消息给serviceWorker message
* @param hexResult
*/
function callSWPostMessage(result) {
    network.syncSendMsgNative(callNative.evalJsRuntime, `navigator.serviceWorker.controller.postMessage('${JSON.stringify(result)}')`);
}

/**
 *
 */
class snowflakeIdv1 {
    /**
         *Creates an instance of Genid.
         * @author zhupengfeivip
         * @param {{
         *     BaseTime: 1577836800000,  // 基础时间（ms 单位），默认2020年1月1日，不能超过当前系统时间，一旦投入使用就不能再更改，更改后产生的ID可能会和以前的重复
         *     WorkerId: Number, // 机器码，必须由外部设定，最大值 2^WorkerIdBitLength-1
         *     WorkerIdBitLength: 6,   // 机器码位长，默认值 6，取值范围 [1, 15](要求：序列数位长+机器码位长不超过 22)
         *     SeqBitLength: 6,   // 序列数位长，默认值 6，取值范围 [3, 21](要求：序列数位长+机器码位长不超过 22)
         *     MaxSeqNumber: 5, // 最大序列数（含），设置范围 [MinSeqNumber, 2^SeqBitLength-1]，默认值 0，表示最大序列数取最大值（2^SeqBitLength-1]）
         *     MinSeqNumber: 5, // 最小序列数（含），默认值 5，取值范围 [5, MaxSeqNumber]，每毫秒的前 5 个序列数对应编号 0-4 是保留位，其中 1-4 是时间回拨相应预留位，0 是手工新值预留位
         *     TopOverCostCount: 2000// 最大漂移次数（含），默认 2000，推荐范围 500-10000（与计算能力有关）
         * }} options
         * @memberof Genid
         */
    constructor(options) {
        /**
         * 雪花计算方法，（1-漂移算法|2-传统算法），默认 1
         */
        Object.defineProperty(this, "Method", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: void 0
        });
        /**
         * 基础时间（ms 单位），不能超过当前系统时间
         */
        Object.defineProperty(this, "BaseTime", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: void 0
        });
        /**
         * 机器码，必须由外部设定，最大值 2^WorkerIdBitLength-1
         */
        Object.defineProperty(this, "WorkerId", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: void 0
        });
        /**
         * 机器码位长，默认值 6，取值范围 [1, 15](要求：序列数位长+机器码位长不超过 22)
         */
        Object.defineProperty(this, "WorkerIdBitLength", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: void 0
        });
        /**
         * 序列数位长，默认值 6，取值范围 [3, 21](要求：序列数位长+机器码位长不超过 22)
         */
        Object.defineProperty(this, "SeqBitLength", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: void 0
        });
        /**
         * 最大序列数（含），设置范围 [MinSeqNumber, 2^SeqBitLength-1]，默认值 0，表示最大序列数取最大值（2^SeqBitLength-1]）
         */
        Object.defineProperty(this, "MaxSeqNumber", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: void 0
        });
        /**
         * 最小序列数（含），默认值 5，取值范围 [5, MaxSeqNumber]，每毫秒的前 5 个序列数对应编号 0-4 是保留位，其中 1-4 是时间回拨相应预留位，0 是手工新值预留位
         */
        Object.defineProperty(this, "MinSeqNumber", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: void 0
        });
        /**
         * 最大漂移次数（含），默认 2000，推荐范围 500-10000（与计算能力有关）
         */
        Object.defineProperty(this, "TopOverCostCount", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: void 0
        });
        /**
         *
         */
        Object.defineProperty(this, "_TimestampShift", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: void 0
        });
        /**
         *
         */
        Object.defineProperty(this, "_CurrentSeqNumber", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: void 0
        });
        /**
         *
         */
        Object.defineProperty(this, "_LastTimeTick", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: void 0
        });
        /**
         * 回拨次序, 支持 4 次回拨次序（避免回拨重叠导致 ID 重复）
         */
        Object.defineProperty(this, "_TurnBackTimeTick", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: void 0
        });
        /**
         *
         */
        Object.defineProperty(this, "_TurnBackIndex", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: void 0
        });
        /**
         *
         */
        Object.defineProperty(this, "_IsOverCost", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: void 0
        });
        /**
         *
         */
        Object.defineProperty(this, "_OverCostCountInOneTerm", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: void 0
        });
        if (options.workerId === undefined)
            throw new Error("lost WorkerId");
        // 1.BaseTime 2020年1月1日 Wed, 01 Jan 2020 00:00:00 GMT 0时区的2020年1月1日
        const BaseTime = 1577836800000;
        if (!options.baseTime || options.baseTime < 0)
            options.baseTime = BaseTime;
        // 2.WorkerIdBitLength
        const WorkerIdBitLength = 6;
        if (!options.workerIdBitLength || options.workerIdBitLength < 0)
            options.workerIdBitLength = WorkerIdBitLength;
        // 4.SeqBitLength
        const SeqBitLength = 6;
        if (!options.seqBitLength || options.seqBitLength < 0)
            options.seqBitLength = SeqBitLength;
        // 5.MaxSeqNumber
        if (options.maxSeqNumber == undefined || options.maxSeqNumber <= 0)
            options.maxSeqNumber = (1 << SeqBitLength) - 1;
        // 6.MinSeqNumber
        const MinSeqNumber = 5;
        if (options.minSeqNumber == undefined || options.minSeqNumber < 0)
            options.minSeqNumber = MinSeqNumber;
        // 7.Others
        const topOverCostCount = 2000;
        if (options.topOverCostCount == undefined || options.topOverCostCount < 0)
            options.topOverCostCount = topOverCostCount;
        if (options.method !== 2)
            options.method = 1;
        else
            options.method = 2;
        this.Method = BigInt(options.method);
        this.BaseTime = BigInt(options.baseTime);
        this.WorkerId = BigInt(options.workerId);
        this.WorkerIdBitLength = BigInt(options.workerIdBitLength);
        this.SeqBitLength = BigInt(options.seqBitLength);
        this.MaxSeqNumber = BigInt(options.maxSeqNumber);
        this.MinSeqNumber = BigInt(options.minSeqNumber);
        this.TopOverCostCount = BigInt(options.topOverCostCount);
        const timestampShift = this.WorkerIdBitLength + this.SeqBitLength;
        const currentSeqNumber = this.MinSeqNumber;
        this._TimestampShift = timestampShift;
        this._CurrentSeqNumber = currentSeqNumber;
        this._LastTimeTick = BigInt(0);
        this._TurnBackTimeTick = BigInt(0);
        this._TurnBackIndex = 0;
        this._IsOverCost = false;
        this._OverCostCountInOneTerm = 0;
    }
    /**
     * 雪花漂移算法
     * @returns
     */
    NextOverCostId() {
        const currentTimeTick = this.GetCurrentTimeTick();
        if (currentTimeTick > this._LastTimeTick) {
            //当前时间大于上次时间，说明是时间是递增的，这是正常情况
            this._LastTimeTick = currentTimeTick;
            this._CurrentSeqNumber = this.MinSeqNumber;
            this._IsOverCost = false;
            this._OverCostCountInOneTerm = 0;
            // this._GenCountInOneTerm = 0
            return this.CalcId(this._LastTimeTick);
        }
        if (this._OverCostCountInOneTerm >= this.TopOverCostCount) {
            //当前漂移次数超过最大限制
            // TODO: 在漂移终止，等待时间对齐时，如果发生时间回拨较长，则此处可能等待较长时间。可优化为：在漂移终止时增加时间回拨应对逻辑。（该情况发生概率很低）
            this._LastTimeTick = this.GetNextTimeTick();
            this._CurrentSeqNumber = this.MinSeqNumber;
            this._IsOverCost = false;
            this._OverCostCountInOneTerm = 0;
            // this._GenCountInOneTerm = 0
            return this.CalcId(this._LastTimeTick);
        }
        if (this._CurrentSeqNumber > this.MaxSeqNumber) {
            //当前序列数超过最大限制，则要提前透支
            this._LastTimeTick++;
            this._CurrentSeqNumber = this.MinSeqNumber;
            this._IsOverCost = true;
            this._OverCostCountInOneTerm++;
            // this._GenCountInOneTerm++
            return this.CalcId(this._LastTimeTick);
        }
        // this._GenCountInOneTerm++
        return this.CalcId(this._LastTimeTick);
    }
    /**
     * 常规雪花算法
     * @returns
     */
    NextNormalId() {
        const currentTimeTick = this.GetCurrentTimeTick();
        if (currentTimeTick < this._LastTimeTick) {
            if (this._TurnBackTimeTick < 1) {
                this._TurnBackTimeTick = this._LastTimeTick - BigInt(1);
                this._TurnBackIndex++;
                // 每毫秒序列数的前 5 位是预留位，0 用于手工新值，1-4 是时间回拨次序
                // 支持 4 次回拨次序（避免回拨重叠导致 ID 重复），可无限次回拨（次序循环使用）。
                if (this._TurnBackIndex > 4)
                    this._TurnBackIndex = 1;
            }
            return this.CalcTurnBackId(this._TurnBackTimeTick);
        }
        // 时间追平时，_TurnBackTimeTick 清零
        if (this._TurnBackTimeTick > 0) {
            this._TurnBackTimeTick = BigInt(0);
        }
        if (currentTimeTick > this._LastTimeTick) {
            this._LastTimeTick = currentTimeTick;
            this._CurrentSeqNumber = this.MinSeqNumber;
            return this.CalcId(this._LastTimeTick);
        }
        if (this._CurrentSeqNumber > this.MaxSeqNumber) {
            // this._TermIndex++
            this._LastTimeTick++;
            this._CurrentSeqNumber = this.MinSeqNumber;
            this._IsOverCost = true;
            this._OverCostCountInOneTerm = 1;
            // this._GenCountInOneTerm = 1
            return this.CalcId(this._LastTimeTick);
        }
        return this.CalcId(this._LastTimeTick);
    }
    /**
     * 生成ID
     * @param useTimeTick 时间戳
     * @returns
     */
    CalcId(useTimeTick) {
        //ID组成 1.相对基础时间的时间差 | 2.WorkerId | 3.序列数
        //时间差，是生成ID时的系统时间减去 BaseTime 的总时间差（毫秒单位）
        const result = BigInt(useTimeTick << this._TimestampShift) + BigInt(this.WorkerId << this.SeqBitLength) + BigInt(this._CurrentSeqNumber);
        this._CurrentSeqNumber++;
        return result;
    }
    /**
     * 生成时间回拨ID
     * @returns
     */
    CalcTurnBackId(useTimeTick) {
        const result = BigInt(useTimeTick << this._TimestampShift) + BigInt(this.WorkerId << this.SeqBitLength) + BigInt(this._TurnBackIndex);
        this._TurnBackTimeTick--;
        return result;
    }
    /**
     *
     * @returns
     */
    GetCurrentTimeTick() {
        const millis = BigInt((new Date()).valueOf());
        return millis - this.BaseTime;
    }
    /**
     *
     * @returns
     */
    GetNextTimeTick() {
        let tempTimeTicker = this.GetCurrentTimeTick();
        while (tempTimeTicker <= this._LastTimeTick) {
            tempTimeTicker = this.GetCurrentTimeTick();
        }
        return tempTimeTicker;
    }
    /**
     * 生成ID
     * @returns 始终输出number类型，超过时throw error
     */
    NextNumber() {
        if (this._IsOverCost) {
            const id = this.NextOverCostId();
            if (id >= 9007199254740992n)
                throw Error(`${id.toString()} over max of Number 9007199254740992`);
            return parseInt(id.toString());
        }
        else {
            const id = this.NextNormalId();
            if (id >= 9007199254740992n)
                throw Error(`${id.toString()} over max of Number 9007199254740992`);
            return parseInt(id.toString());
        }
    }
    /**
     * 生成ID
     * @returns 根据输出数值判断，小于number最大值时输出number类型，大于时输出bigint
     */
    NextId() {
        if (this._IsOverCost) {
            const id = this.NextOverCostId();
            if (id >= 9007199254740992n)
                return id;
            else
                return parseInt(id.toString());
        }
        else {
            const id = this.NextNormalId();
            if (id >= 9007199254740992n)
                return id;
            else
                return parseInt(id.toString());
        }
    }
    /**
     * 生成ID
     * @returns 始终输出bigint类型
     */
    NextBigId() {
        if (this._IsOverCost) {
            //
            return this.NextOverCostId();
        }
        else {
            //
            return this.NextNormalId();
        }
    }
}

class Channels {
    constructor() {
        Object.defineProperty(this, "gen", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: void 0
        });
        this.gen = new snowflakeIdv1({ workerId: 213, seqBitLength: 6 });
    }
    getChannelId() {
        return this.gen.NextId() + "";
    }
}

/**
 * 处理ios请求
 * @param url
 * @param buffer
 */
async function iosRequestFanctory(url, buffer) {
    const pathname = url.pathname;
    // console.log("deno#iosRequestFanctory:", pathname)
    if (pathname.endsWith("/setUi")) {
        return setIosUiHandle(url, buffer); // 处理 system ui
    }
    if (pathname.startsWith("/poll")) {
        await setIosPollHandle(url, buffer); // 处理真正的请求
    }
}
/**
 * ios ui相关操作
 * @param url
 * @returns
 */
async function setIosUiHandle(url, hexBuffer) {
    const searchParams = url.searchParams.get("data");
    // console.log("deno#setIosUiHandle:", searchParams, hexBuffer)
    if (searchParams) {
        const result = await network.asyncCallDenoFunction(callNative.setDWebViewUI, searchParams);
        console.log("deno#setIosUiHandle result:", result);
        const { cmd, data } = JSON.parse(result);
        callWKWebView(cmd, data);
        return result;
    }
    if (!hexBuffer) {
        console.error("Parameter passing cannot be empty！"); // 如果没有任何请求体
        throw new Error("Parameter passing cannot be empty！");
    }
    const data = await network.asyncCallDenoFunction(callNative.setDWebViewUI, 
    // [new Uint8Array(hexToBinary(hexBuffer))]
    hexBuffer);
    return data;
}
/**
 * ios 系统api逻辑相关操作
 * @param url
 * @param hexBuffer
 */
function setIosPollHandle(url, hexBuffer) {
    const bufferData = url.searchParams.get("data");
    console.log("deno#setIosPollHandle:", bufferData, hexBuffer);
    let buffer;
    // 如果是get
    if (bufferData) {
        buffer = hexToBinary(bufferData);
    }
    else {
        // 处理post
        if (!hexBuffer) {
            console.error("Parameter passing cannot be empty！");
            throw new Error("Parameter passing cannot be empty！"); // 如果没有任何请求体
        }
        buffer = hexToBinary(hexBuffer);
    }
    const stringData = bufferToString(buffer);
    const handler = JSON.parse(stringData);
    console.log("deno#setIosPollHandle Data:", stringData);
    basePollHandle(handler.function, handler.data);
}
/**
* 发送消息给serviceWorker message
* @param hexResult
*/
function callWKWebView(cmd, data) {
    network.syncSendMsgNative(callNative.evalJsRuntime, `iosListen.listerIosSetUiCallback('${cmd}','${data}')`);
}

// 存储需要触发前端的事件，需要等待serviceworekr准备好
// deno-lint-ignore no-explicit-any
const EventPollQueue = [];
const request_body_cache = EasyMap$1.from({
    // deno-lint-ignore no-unused-vars
    creater(boydId) {
        let bodyStreamController;
        const bodyStream = new ReadableStream({ start(controller) { bodyStreamController = controller; } });
        // deno-lint-ignore no-explicit-any
        const op = null;
        return {
            bodyStream,
            bodyStreamController: bodyStreamController,
            op
        };
    }
});
class DWebView extends MapEventEmitter {
    constructor(metaData) {
        super();
        Object.defineProperty(this, "entrys", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: void 0
        });
        Object.defineProperty(this, "importMap", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: void 0
        });
        Object.defineProperty(this, "channel", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: void 0
        });
        this.entrys = metaData.manifest.enters;
        this.importMap = metaData.dwebview.importmap;
        this.channel = new Channels();
        this.initAppMetaData(metaData);
        this.dwebviewToDeno(); // 挂载轮询操作， 这里会自动处理来自前端的请求，并且处理操作返回到前端
        this.on("request", async (event) => {
            const { url } = event;
            // 是不是资源文件 （index.html,xxx.js）
            const isAssetsFile = url.pathname.lastIndexOf(".") !== -1;
            console.log(`deno#request: method:${event.request.method},channelId:${event.channelId}`, event.request.url);
            // headers
            event.request.headers.forEach((val, key) => {
                event.response.setHeaders(key, val);
            });
            if (url.pathname.endsWith("/setUi")) {
                return setUiHandle(event); // 处理 system ui
            }
            if (url.pathname.startsWith("/poll")) {
                await setPollHandle(event); // 处理真正的请求
                event.response.end(); // 操作成功直接返回
            }
            // 如果是需要转发的数据请求 pathname: "/getBlockInfo"
            if (!isAssetsFile) {
                return parseNetData(event, url.pathname, this.importMap);
            }
        });
    }
    /**
     * ios转发信息都会到这里
     * @param strPath
     * @returns
     */
    getIosMessage(strPath, buffer) {
        if (!strPath)
            return;
        const url = new URL(`https://a${strPath}`);
        // console.log(`deno#getIosMessage url:${url}`)
        iosRequestFanctory(url, buffer);
    }
    /**
   * 轮询向rust拿数据，路径为：dwebView-js-(fetch)->kotlin-(ffi)->rust-(op)->deno-js->kotlin(eventJs)->dwebView-js
   * 这里是接收dwebView-js操作系统API转发到后端的请求
   */
    async dwebviewToDeno() {
        do {
            const data = await getRustChunk();
            if (data.done) {
                continue;
            }
            // console.log("dwebviewToDeno====>", data.value);
            const strPath = bufferToString(Uint8Array.from(data.value));
            this.chunkGateway(strPath);
            /// 这里是重点，使用 do-while ，替代 finally，可以避免堆栈溢出。
        } while (true);
    }
    /**
     * 解析网络请求
     * @param strBits
     */
    async chunkGateway(strPath) {
        console.log("strPath :", strPath);
        // 注册channelID
        if (strPath.startsWith("/chunk/registryChannelId")) {
            const channelId = this.channel.getChannelId();
            return applyChannelId(channelId);
        }
        // 转发请求
        if (strPath.startsWith("/channel")) { // /channel/349512662458373/chunk=0002,104,116,116,112,115,58,1
            // 拿到channelId
            const channelId = strPath.substring(strPath.lastIndexOf("/channel/") + 9, strPath.lastIndexOf("/chunk"));
            const stringHex = strPath.substring(strPath.lastIndexOf("=") + 1);
            // const buffers = stringHex.split(",").map(v => Number(v))
            const buffers = hexToBinary(stringHex);
            // const chunk = (new Uint8Array(buffers))
            await this.chunkHanlder(channelId, buffers);
        }
    }
    /**
     * 处理chunk
     * @param channelId
     * @param chunk
     */
    async chunkHanlder(channelId, chunk) {
        // 拿到头部
        const headers_body_id = chunk.slice(0, 1)[0];
        // 是否结束
        const isEnd = chunk.slice(-1)[0] === 1; // 1为发送结束，0为还没结束
        console.log(`deno#chunkHanlder headerId:${headers_body_id},isEnd:${isEnd}`);
        // 拿到请求题
        const contentBytes = chunk.slice(1, -1);
        // 如果是headers请求，解析请求头
        if (headers_body_id % 2 === 0) {
            const headersId = headers_body_id;
            const { url, headers, method } = JSON.parse(bufferToString(contentBytes));
            let req;
            if (method === 'POST' || method === 'PUT' || method === 'DELETE') {
                const body = request_body_cache.forceGet(headersId + 1);
                console.log("deno#body 第一次存储 🎬", headers_body_id + 1);
                // body.op = new PromiseOut();
                console.log("deno#chunkHanlder:", method, url);
                req = new Request(url, { method, headers, body: body.bodyStream });
            }
            else {
                req = new Request(url, { method, headers });
            }
            let responseBodyCtrl;
            const responseBody = new ReadableStream({ start: (ctrl) => responseBodyCtrl = ctrl });
            const postBodyDone = new PromiseOut();
            // create request head
            const event = new RequestEvent(req, new RequestResponse(responseBodyCtrl, (statusCode, headers) => {
                postBodyDone.resolve();
                // 发送header头到serviceworker
                callSWPostMessage({
                    returnId: headersId,
                    channelId: channelId,
                    chunk: stringToByte(JSON.stringify({ statusCode, headers })).join(",") + ",0" // 后面加0 表示发送未结束
                });
            }), channelId, headersId + 1);
            // 触发到kotlin的真正请求
            this.emit("request", event);
            // 等待请求数据填充,保证responseBodyReader有数据
            await postBodyDone.promise;
            const responseBodyReader = responseBody.getReader();
            // 填充真正的数据发送到serviceworker
            do {
                const { value: chunk, done } = await responseBodyReader.read();
                if (done) {
                    console.log("dwebView#responseBodyReader:啊我结束了", headersId + 1, chunk, done);
                    callSWPostMessage({
                        returnId: headersId + 1,
                        channelId: channelId,
                        chunk: "1" // 后面加1 表示发送结束
                    });
                    break;
                }
                console.log("dwebView#responseBodyReader:", headersId + 1, chunk, done);
                callSWPostMessage({
                    returnId: headersId + 1,
                    channelId: channelId,
                    chunk: chunk.join(",") + ",0" // 后面加0 表示发送未结束
                });
                console.log("dwebView#responseBodyReader:222");
            } while (true);
            return;
        }
        // 如果是body 需要填充Request body
        this.resolveNetworkBodyRequest(headers_body_id, contentBytes, isEnd);
    }
    /**
     * 分发body数据
     * @param path  数据
     * @param isEnd  如果是true就是消息结束了，如果是false 就是消息未结束
     */
    resolveNetworkBodyRequest(body_id, contentBytes, isEnd) {
        const body = request_body_cache.get(body_id); // 获取body
        if (!body) {
            console.log("deno#body Not Found", body_id, body, contentBytes.length);
            return;
        }
        // body 流结束
        if (isEnd) {
            body.bodyStreamController.close();
            console.log("deno#body 推入完成✅:", body_id);
            return;
        }
        console.log("deno#body 推入:", body_id, isEnd, contentBytes.length);
        body.bodyStreamController.enqueue(new Uint8Array(contentBytes)); // 在需要传递二进制数据的时候再转换Uint8
    }
    /**
     * 打开请求通道
     * @param url  api/user/*, api/:method,api/chunkInfo
     * @param mode  pattern | static
     */
    openRequest(url, mode) {
        EventPollQueue.push({ url, mode });
        // await this.openChannel({ url, mode })
    }
    /**
    * 初始化app元数据
    * @param metaData  元数据
    * @returns void
    */
    initAppMetaData(metaData) {
        if (Object.keys(metaData).length === 0)
            return;
        network.syncSendMsgNative(callNative.initMetaData, metaData);
    }
    /**
     * 激活DwebView
     * @param entry // DwebView入口
     */
    activity(entry) {
        console.log("this.entrys:", this.entrys.toString(), entry, this.entrys.toString().match(RegExp(`${entry}`)));
        if (this.entrys.toString().match(RegExp(`${entry}`))) {
            network.syncSendMsgNative(callNative.openDWebView, entry);
            return;
        }
        console.error("您传递的入口不在配置的入口内，需要在配置文件里配置入口");
        throw new Error("not found entry");
    }
}

var EFilterType;
(function (EFilterType) {
    EFilterType["file"] = "file";
    EFilterType["directory"] = "directory";
})(EFilterType || (EFilterType = {}));

var vfsHandle;
(function (vfsHandle) {
    vfsHandle["FileSystemLs"] = "FileSystemLs";
    vfsHandle["FileSystemList"] = "FileSystemList";
    vfsHandle["FileSystemMkdir"] = "FileSystemMkdir";
    vfsHandle["FileSystemWrite"] = "FileSystemWrite";
    vfsHandle["FileSystemRead"] = "FileSystemRead";
    vfsHandle["FileSystemReadBuffer"] = "FileSystemReadBuffer";
    vfsHandle["FileSystemRename"] = "FileSystemRename";
    vfsHandle["FileSystemRm"] = "FileSystemRm";
    vfsHandle["FileSystemStat"] = "FileSystemStat";
})(vfsHandle || (vfsHandle = {}));

class BfsPath {
    /**
   * 拼接路径
   * @param cwd
   * @param path
   * @returns
   */
    join(cwd, path) {
        return `${cwd}/${path}`.replace(/(\/+)/, '/');
    }
    resolve(...paths) {
        let resolvePath = '';
        let isAbsolutePath = false;
        for (let i = paths.length - 1; i > -1; i--) {
            const path = paths[i];
            if (isAbsolutePath) {
                break;
            }
            if (!path) {
                continue;
            }
            resolvePath = path + '/' + resolvePath;
            isAbsolutePath = path.charCodeAt(0) === 47;
        }
        if (/^\/+$/.test(resolvePath)) {
            resolvePath = resolvePath.replace(/(\/+)/, '/');
        }
        else {
            resolvePath = resolvePath.replace(/(?!^)\w+\/+\.{2}\//g, '')
                .replace(/(?!^)\.\//g, '')
                .replace(/\/+$/, '');
        }
        return resolvePath;
    }
}
const Path = new BfsPath();

// const fs = await fs.read("/text.text");
/**
 * 读取文件
 * @param filePath 要读取的文件路径
 * @returns fs
 */
async function read(path) {
    const fs = await network.asyncCallDenoFunction(vfsHandle.FileSystemRead, {
        path,
    });
    return fs;
}
/**
 * 读取文件buffer
 * @param path
 * @returns
 */
async function readBuff(path) {
    const fs = await network.asyncCallbackBuffer(vfsHandle.FileSystemReadBuffer, {
        path,
    });
    return fs;
}
/**
 * 重命名文件
 * @param path 源文件
 * @param newPath 需要重命名的文件名
 * @returns
 */
async function rename(path, newName) {
    // 提取文件前缀 /a/b/bfsa.txt -> /a/b/
    const newPath = Path.join(path.slice(0, path.lastIndexOf("/") + 1), newName);
    const fs = await network.asyncCallDenoFunction(vfsHandle.FileSystemRename, {
        path,
        newPath,
    });
    if (fs === "true") {
        return true;
    }
    return fs;
}

/// const list: string[] = await fs.ls("./", { // list
// /   filter: [{ // 声明筛选方式
// /     type: "file",
// /     name: ["*.ts"]
// /   }],
///   recursive: true, // 是否要递归遍历目录，默认是 false
/// });
/**
 * 获取目录下有哪些文件
 * @param path
 * @param option:{filter: [{type: "file", name: ["*.ts"]}],recursive: true // 是否要递归遍历目录，默认是 false}
 * @returns file string[]
 */
async function ls(path, option) {
    const fileList = await network.asyncCallDenoFunction(vfsHandle.FileSystemLs, {
        path,
        option,
    });
    return transStringToArray(fileList);
}
// for await (const entry of fs.list("./")) { // 也可以用异步迭代器来访问，避免列表过大
//   entry.name // 文件或者目录的完整名称
//   entry.extname // 文件的后缀，如果是文件夹则为空
//   entry.basename // 文件的基本名称
//   entry.path // 完整路径
//   entry.cwd // 访问者的源路径
//   entry.relativePath // 相对路径
//   entry.type // "file"或者"directory"
//   entry.isLink // 是否是超链接文件
//   await entry.text() // {string} 当作文本读取
//   await entry.binary() // {ArrayBuffer} 当作二进制读取
//   entry.stream({ threshold...}) // {AsyncGenerator<ArrayBuffer>} 以二进制流的方式进行读取
//   await entry.readAs("json") // {json-instance} 解析成json实例对象。这是开发者可以通过扩展来实现的
//   await entry.checkname("new-name") // {boolean} 检查名字是否符合规范。在一些特定的文件夹中，通过“文件夹守护配置GuardConfig”，可能会有特定的文件名规范
//   await entry.rename("new-name") // {self} 重命名，如果名字不符合规范，会抛出异常
//   await entry.cd("../") // {FileSystem} change-directory 进入其它目录
//   await entry.open("/") // 与FileSystem.open类似，使用绝对路径打开，同时会继承第二参数的部分配置
//   entry.relativeTo("./" | otherEntry) // {string} 获取相对路径
// }
/**
 * 迭代返回文件对象
 * @param path
 * @returns fileSystems
 */
async function* list(path) {
    const fileList = await network.asyncCallDenoFunction(vfsHandle.FileSystemList, { path });
    const list = transStringToJson(fileList);
    for (const fs of list) {
        const files = createFileEntry(fs);
        yield files;
    }
}
/**
 * 返回文件对象
 * @param path
 * @returns fileSystems
 */
async function getList(path) {
    const fileList = await network.asyncCallDenoFunction(vfsHandle.FileSystemList, { path });
    const list = transStringToJson(fileList);
    const fileEntrys = [];
    for (const fs of list) {
        const files = createFileEntry(fs);
        fileEntrys.push(files);
    }
    return fileEntrys;
}
// ["/src/test/vue3/bfsa-service/vfs/index.ts","./src"]
/**
 * 创建文件entry
 * @param filePath
 * @param cwd
 * @returns
 */
function createFileEntry(file) {
    console.log("createFileEntry:", file);
    // 去掉两边的"
    const isFile = file.type === EFilterType.file ? true : false;
    // 文件基本名称，不带文件类型
    file.basename = isFile
        ? file.name.slice(0, file.name.lastIndexOf("."))
        : file.name;
    // 读取文件 以文本方式
    file.text = async function () {
        const readText = await read(file.path);
        return readText;
    };
    // 读取文件 流方式
    file.stream = async function* () {
        // 如果是文件再读取内容
        if (isFile) {
            const fileBuff = new Uint8Array(await readBuff(file.path));
            let index = 0;
            const oneM = 1024 * 512 * 1;
            // 如果数据不是很大，直接返回
            if (fileBuff.byteLength < oneM) {
                yield fileBuff;
            }
            else {
                // 迭代返回
                do {
                    yield fileBuff.subarray(index, index + oneM);
                    index += oneM;
                } while (fileBuff.byteLength > index);
            }
        }
    };
    // 读取文件 二进制流方式
    file.binary = async function () {
        let buff = new ArrayBuffer(1);
        if (isFile) {
            buff = await readBuff(file.path);
        }
        return buff;
    };
    //重命名文件
    file.rename = async function (name) {
        return await rename(file.path, name);
    };
    // file.readAs = function () {
    //   return Promise.resolve(file)
    // }
    // file.checkname = function () {
    //   return Promise.resolve(true)
    // }
    file.cd = async function (path) {
        const fs = await getList(Path.join(file.cwd, path));
        return await fs;
    };
    file.relativeTo = async function (path) {
        if (path) {
            const fs = await list(path).next();
            return fs.value.relativePath;
        }
        return file.relativePath;
    };
    return file;
}
/**
 * 把字符串转换成数组
 * @param string
 * @returns string[]
 */
function transStringToArray(str) {
    if (/^\[.*\]$/i.test(str)) {
        str = str.replace(/^\[/i, "").replace(/\]$/i, "");
    }
    return str.split(",");
}
/**
 * 字符串转换为json
 * @param str
 * @returns
 */
function transStringToJson(str) {
    const fs = JSON.parse(str);
    return fs;
}

// const fs = await fs.open("/"/* 默认值就是根目录 */, {
//   recursive: true, // 自动创建不存在的目录，默认是 false
// });
/**
 *
 * @param path 默认值就是根目录
 * @param option : { recursive: false } // 自动创建不存在的目录，默认是 false
 * @returns fs
 */
async function mkdir(path, option = { recursive: false }) {
    const result = await network.asyncCallDenoFunction(vfsHandle.FileSystemMkdir, { path, option });
    return result;
}

/**
 * 写入内容
 * @param filePath
 * @param option： {content:"",append: false, // 是否追加内容,默认是false autoCreate: true, // 自动创建不存在的目录，默认是 true});
 * @returns
 */
async function write(path, content, option) {
    const fs = await network.asyncCallDenoFunction(vfsHandle.FileSystemWrite, {
        path,
        content,
        option,
    });
    return fs;
}

// const fs = await fs.rm("/", {
/**
 *
 * @param path 默认值就是根目录
 * @param option : { deepDelete: true, // 是否删除包含子目录 true}
 * @returns
 */
async function rm(path, option = { deepDelete: true }) {
    const fs = await network.asyncCallDenoFunction(vfsHandle.FileSystemRm, {
        path,
        option,
    });
    return fs;
}

/**
 * 文件信息
 * @param path
 * @returns
 */
async function stat(path) {
    const fs = await network.asyncCallDenoFunction(vfsHandle.FileSystemStat, {
        path,
    });
    return fs;
}

const fs = {
    ls,
    list,
    mkdir,
    read,
    readBuff,
    write,
    rm,
    stat,
};

(async () => {
    const lsFileList = await ls("/", {
        filter: [
            {
                type: EFilterType.file,
                name: ["*.ts", "index"],
            },
            {
                type: EFilterType.directory,
                name: ["core"],
            },
        ],
        recursive: true,
    });
    console.log("vfs测试：获取ls : ", lsFileList);
    try {
        for await (const entry of fs.list("./")) {
            // 也可以用异步迭代器来访问，避免列表过大
            console.log(`vfs测试：获取${entry.type}的各项信息name->${entry.name}
    ,extname->${entry.extname},cwd->${entry.cwd},basename->${entry.basename},
    path->${entry.path},relativePath->${entry.relativePath}`);
            for await (const buff of entry.stream()) {
                console.log("vfs测试：entry.stream():", buff);
            }
            console.log("vfs测试：entry.binary():", await entry.binary());
            console.log("vfs测试：entry.cd(book):", await entry.cd("book"));
            if (entry.name === "gege.txt") {
                console.log("vfs测试：重命名:", await entry.rename("嘎嘎.txt"));
            }
        }
    }
    catch (e) {
        console.log(e);
    }
    const mkdirFs1 = await fs.mkdir("/water1");
    const mkdirFs2 = await fs.mkdir("/bang1");
    console.log("vfs测试：创建文件: ", mkdirFs1, mkdirFs2);
    const rmDir = Math.random() <= 0.5 ? "/water1" : "/bang1";
    const rmFs1 = await fs.rm(rmDir);
    console.log(`vfs测试：删除${rmDir}:${rmFs1}`);
    const mkdirFs3 = await fs.mkdir("/water/bang", { recursive: true });
    console.log("vfs测试：创建多级文件: ", mkdirFs3);
    const statFs = await fs.stat("/water");
    console.log("vfs测试：目录信息: ", statFs);
    const rmFs2 = await fs.rm("/water", { deepDelete: false });
    const rmFs3 = await fs.rm("/water");
    console.log(`vfs测试：递归删除失败 ${rmFs2}`);
    console.log(`vfs测试：递归删除 ${rmFs3}`);
    const writeFs1 = await fs.write("./gege.txt", "日射纱窗风撼扉，");
    console.log(`vfs测试：写入信息 ${writeFs1}`);
    const statFs2 = await fs.stat("./gege.txt");
    console.log("vfs测试: 文件信息： ", statFs2);
    const writeFs2 = await fs.write("./gege.txt", "香罗拭手春事违。", {
        append: true,
    });
    console.log(`vfs测试：追加写入信息 ${writeFs2}`);
    const readFs1 = await fs.read("./gege.txt");
    console.log(`vfs测试：读取信息 ${readFs1}`);
    const writeFs3 = await fs.write("/book/book.js", "console.log(`十年花骨东风泪，几点螺香素壁尘。`)");
    console.log(`vfs测试：创建不存在的文件写入信息 ${writeFs3}`);
})();

const webView = new DWebView(metaData);
// 打开channel模式
(async () => {
    await webView.openRequest("/api/*", EChannelMode.pattern);
})();
// 多入口指定
webView.activity("https://objectjson.waterbang.top/");
// webView.activity("index.html");
// try {
//   sendNotification({ title: "消息头", body: "今晚打老虎", priority: 1 });
// } catch (error) {
//   console.log(error);
// }
