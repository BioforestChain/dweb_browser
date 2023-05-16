var __create = Object.create;
var __defProp = Object.defineProperty;
var __getOwnPropDesc = Object.getOwnPropertyDescriptor;
var __getOwnPropNames = Object.getOwnPropertyNames;
var __getProtoOf = Object.getPrototypeOf;
var __hasOwnProp = Object.prototype.hasOwnProperty;
var __commonJS = (cb, mod) => function __require() {
  return mod || (0, cb[__getOwnPropNames(cb)[0]])((mod = { exports: {} }).exports, mod), mod.exports;
};
var __copyProps = (to, from, except, desc) => {
  if (from && typeof from === "object" || typeof from === "function") {
    for (let key of __getOwnPropNames(from))
      if (!__hasOwnProp.call(to, key) && key !== except)
        __defProp(to, key, { get: () => from[key], enumerable: !(desc = __getOwnPropDesc(from, key)) || desc.enumerable });
  }
  return to;
};
var __toESM = (mod, isNodeMode, target) => (target = mod != null ? __create(__getProtoOf(mod)) : {}, __copyProps(
  // If the importer is in node compatibility mode or this is not an ESM
  // file that has been converted to a CommonJS file using a Babel-
  // compatible transform (i.e. "__esModule" has not been set), then set
  // "default" to the CommonJS "module.exports" for node compatibility.
  isNodeMode || !mod || !mod.__esModule ? __defProp(target, "default", { value: mod, enumerable: true }) : target,
  mod
));
var __decorateClass = (decorators, target, key, kind) => {
  var result = kind > 1 ? void 0 : kind ? __getOwnPropDesc(target, key) : target;
  for (var i = decorators.length - 1, decorator; i >= 0; i--)
    if (decorator = decorators[i])
      result = (kind ? decorator(target, key, result) : decorator(result)) || result;
  if (kind && result)
    __defProp(target, key, result);
  return result;
};
var __accessCheck = (obj, member, msg) => {
  if (!member.has(obj))
    throw TypeError("Cannot " + msg);
};
var __privateGet = (obj, member, getter) => {
  __accessCheck(obj, member, "read from private field");
  return getter ? getter.call(obj) : member.get(obj);
};
var __privateAdd = (obj, member, value) => {
  if (member.has(obj))
    throw TypeError("Cannot add the same private member more than once");
  member instanceof WeakSet ? member.add(obj) : member.set(obj, value);
};
var __privateSet = (obj, member, value, setter) => {
  __accessCheck(obj, member, "write to private field");
  setter ? setter.call(obj, value) : member.set(obj, value);
  return value;
};

// node_modules/.pnpm/lodash@4.17.21/node_modules/lodash/_trimmedEndIndex.js
var require_trimmedEndIndex = __commonJS({
  "node_modules/.pnpm/lodash@4.17.21/node_modules/lodash/_trimmedEndIndex.js"(exports, module) {
    var reWhitespace = /\s/;
    function trimmedEndIndex(string) {
      var index = string.length;
      while (index-- && reWhitespace.test(string.charAt(index))) {
      }
      return index;
    }
    module.exports = trimmedEndIndex;
  }
});

// node_modules/.pnpm/lodash@4.17.21/node_modules/lodash/_baseTrim.js
var require_baseTrim = __commonJS({
  "node_modules/.pnpm/lodash@4.17.21/node_modules/lodash/_baseTrim.js"(exports, module) {
    var trimmedEndIndex = require_trimmedEndIndex();
    var reTrimStart = /^\s+/;
    function baseTrim(string) {
      return string ? string.slice(0, trimmedEndIndex(string) + 1).replace(reTrimStart, "") : string;
    }
    module.exports = baseTrim;
  }
});

// node_modules/.pnpm/lodash@4.17.21/node_modules/lodash/isObject.js
var require_isObject = __commonJS({
  "node_modules/.pnpm/lodash@4.17.21/node_modules/lodash/isObject.js"(exports, module) {
    function isObject2(value) {
      var type = typeof value;
      return value != null && (type == "object" || type == "function");
    }
    module.exports = isObject2;
  }
});

// node_modules/.pnpm/lodash@4.17.21/node_modules/lodash/_freeGlobal.js
var require_freeGlobal = __commonJS({
  "node_modules/.pnpm/lodash@4.17.21/node_modules/lodash/_freeGlobal.js"(exports, module) {
    var freeGlobal = typeof global == "object" && global && global.Object === Object && global;
    module.exports = freeGlobal;
  }
});

// node_modules/.pnpm/lodash@4.17.21/node_modules/lodash/_root.js
var require_root = __commonJS({
  "node_modules/.pnpm/lodash@4.17.21/node_modules/lodash/_root.js"(exports, module) {
    var freeGlobal = require_freeGlobal();
    var freeSelf = typeof self == "object" && self && self.Object === Object && self;
    var root = freeGlobal || freeSelf || Function("return this")();
    module.exports = root;
  }
});

// node_modules/.pnpm/lodash@4.17.21/node_modules/lodash/_Symbol.js
var require_Symbol = __commonJS({
  "node_modules/.pnpm/lodash@4.17.21/node_modules/lodash/_Symbol.js"(exports, module) {
    var root = require_root();
    var Symbol2 = root.Symbol;
    module.exports = Symbol2;
  }
});

// node_modules/.pnpm/lodash@4.17.21/node_modules/lodash/_getRawTag.js
var require_getRawTag = __commonJS({
  "node_modules/.pnpm/lodash@4.17.21/node_modules/lodash/_getRawTag.js"(exports, module) {
    var Symbol2 = require_Symbol();
    var objectProto = Object.prototype;
    var hasOwnProperty2 = objectProto.hasOwnProperty;
    var nativeObjectToString = objectProto.toString;
    var symToStringTag = Symbol2 ? Symbol2.toStringTag : void 0;
    function getRawTag(value) {
      var isOwn = hasOwnProperty2.call(value, symToStringTag), tag = value[symToStringTag];
      try {
        value[symToStringTag] = void 0;
        var unmasked = true;
      } catch (e) {
      }
      var result = nativeObjectToString.call(value);
      if (unmasked) {
        if (isOwn) {
          value[symToStringTag] = tag;
        } else {
          delete value[symToStringTag];
        }
      }
      return result;
    }
    module.exports = getRawTag;
  }
});

// node_modules/.pnpm/lodash@4.17.21/node_modules/lodash/_objectToString.js
var require_objectToString = __commonJS({
  "node_modules/.pnpm/lodash@4.17.21/node_modules/lodash/_objectToString.js"(exports, module) {
    var objectProto = Object.prototype;
    var nativeObjectToString = objectProto.toString;
    function objectToString(value) {
      return nativeObjectToString.call(value);
    }
    module.exports = objectToString;
  }
});

// node_modules/.pnpm/lodash@4.17.21/node_modules/lodash/_baseGetTag.js
var require_baseGetTag = __commonJS({
  "node_modules/.pnpm/lodash@4.17.21/node_modules/lodash/_baseGetTag.js"(exports, module) {
    var Symbol2 = require_Symbol();
    var getRawTag = require_getRawTag();
    var objectToString = require_objectToString();
    var nullTag = "[object Null]";
    var undefinedTag = "[object Undefined]";
    var symToStringTag = Symbol2 ? Symbol2.toStringTag : void 0;
    function baseGetTag(value) {
      if (value == null) {
        return value === void 0 ? undefinedTag : nullTag;
      }
      return symToStringTag && symToStringTag in Object(value) ? getRawTag(value) : objectToString(value);
    }
    module.exports = baseGetTag;
  }
});

// node_modules/.pnpm/lodash@4.17.21/node_modules/lodash/isObjectLike.js
var require_isObjectLike = __commonJS({
  "node_modules/.pnpm/lodash@4.17.21/node_modules/lodash/isObjectLike.js"(exports, module) {
    function isObjectLike(value) {
      return value != null && typeof value == "object";
    }
    module.exports = isObjectLike;
  }
});

// node_modules/.pnpm/lodash@4.17.21/node_modules/lodash/isSymbol.js
var require_isSymbol = __commonJS({
  "node_modules/.pnpm/lodash@4.17.21/node_modules/lodash/isSymbol.js"(exports, module) {
    var baseGetTag = require_baseGetTag();
    var isObjectLike = require_isObjectLike();
    var symbolTag = "[object Symbol]";
    function isSymbol(value) {
      return typeof value == "symbol" || isObjectLike(value) && baseGetTag(value) == symbolTag;
    }
    module.exports = isSymbol;
  }
});

// node_modules/.pnpm/lodash@4.17.21/node_modules/lodash/toNumber.js
var require_toNumber = __commonJS({
  "node_modules/.pnpm/lodash@4.17.21/node_modules/lodash/toNumber.js"(exports, module) {
    var baseTrim = require_baseTrim();
    var isObject2 = require_isObject();
    var isSymbol = require_isSymbol();
    var NAN = 0 / 0;
    var reIsBadHex = /^[-+]0x[0-9a-f]+$/i;
    var reIsBinary = /^0b[01]+$/i;
    var reIsOctal = /^0o[0-7]+$/i;
    var freeParseInt = parseInt;
    function toNumber(value) {
      if (typeof value == "number") {
        return value;
      }
      if (isSymbol(value)) {
        return NAN;
      }
      if (isObject2(value)) {
        var other = typeof value.valueOf == "function" ? value.valueOf() : value;
        value = isObject2(other) ? other + "" : other;
      }
      if (typeof value != "string") {
        return value === 0 ? value : +value;
      }
      value = baseTrim(value);
      var isBinary2 = reIsBinary.test(value);
      return isBinary2 || reIsOctal.test(value) ? freeParseInt(value.slice(2), isBinary2 ? 2 : 8) : reIsBadHex.test(value) ? NAN : +value;
    }
    module.exports = toNumber;
  }
});

// node_modules/.pnpm/lodash@4.17.21/node_modules/lodash/toFinite.js
var require_toFinite = __commonJS({
  "node_modules/.pnpm/lodash@4.17.21/node_modules/lodash/toFinite.js"(exports, module) {
    var toNumber = require_toNumber();
    var INFINITY = 1 / 0;
    var MAX_INTEGER = 17976931348623157e292;
    function toFinite(value) {
      if (!value) {
        return value === 0 ? value : 0;
      }
      value = toNumber(value);
      if (value === INFINITY || value === -INFINITY) {
        var sign = value < 0 ? -1 : 1;
        return sign * MAX_INTEGER;
      }
      return value === value ? value : 0;
    }
    module.exports = toFinite;
  }
});

// node_modules/.pnpm/lodash@4.17.21/node_modules/lodash/toInteger.js
var require_toInteger = __commonJS({
  "node_modules/.pnpm/lodash@4.17.21/node_modules/lodash/toInteger.js"(exports, module) {
    var toFinite = require_toFinite();
    function toInteger(value) {
      var result = toFinite(value), remainder = result % 1;
      return result === result ? remainder ? result - remainder : result : 0;
    }
    module.exports = toInteger;
  }
});

// node_modules/.pnpm/lodash@4.17.21/node_modules/lodash/before.js
var require_before = __commonJS({
  "node_modules/.pnpm/lodash@4.17.21/node_modules/lodash/before.js"(exports, module) {
    var toInteger = require_toInteger();
    var FUNC_ERROR_TEXT = "Expected a function";
    function before(n, func) {
      var result;
      if (typeof func != "function") {
        throw new TypeError(FUNC_ERROR_TEXT);
      }
      n = toInteger(n);
      return function() {
        if (--n > 0) {
          result = func.apply(this, arguments);
        }
        if (n <= 1) {
          func = void 0;
        }
        return result;
      };
    }
    module.exports = before;
  }
});

// node_modules/.pnpm/lodash@4.17.21/node_modules/lodash/once.js
var require_once = __commonJS({
  "node_modules/.pnpm/lodash@4.17.21/node_modules/lodash/once.js"(exports, module) {
    var before = require_before();
    function once4(func) {
      return before(2, func);
    }
    module.exports = once4;
  }
});

// node_modules/.pnpm/deep-object-diff@1.1.9/node_modules/deep-object-diff/mjs/utils.js
var isDate = (d) => d instanceof Date;
var isEmpty = (o) => Object.keys(o).length === 0;
var isObject = (o) => o != null && typeof o === "object";
var hasOwnProperty = (o, ...args) => Object.prototype.hasOwnProperty.call(o, ...args);
var isEmptyObject = (o) => isObject(o) && isEmpty(o);
var makeObjectWithoutPrototype = () => /* @__PURE__ */ Object.create(null);

// node_modules/.pnpm/deep-object-diff@1.1.9/node_modules/deep-object-diff/mjs/added.js
var addedDiff = (lhs, rhs) => {
  if (lhs === rhs || !isObject(lhs) || !isObject(rhs))
    return {};
  return Object.keys(rhs).reduce((acc, key) => {
    if (hasOwnProperty(lhs, key)) {
      const difference = addedDiff(lhs[key], rhs[key]);
      if (isObject(difference) && isEmpty(difference))
        return acc;
      acc[key] = difference;
      return acc;
    }
    acc[key] = rhs[key];
    return acc;
  }, makeObjectWithoutPrototype());
};
var added_default = addedDiff;

// node_modules/.pnpm/deep-object-diff@1.1.9/node_modules/deep-object-diff/mjs/deleted.js
var deletedDiff = (lhs, rhs) => {
  if (lhs === rhs || !isObject(lhs) || !isObject(rhs))
    return {};
  return Object.keys(lhs).reduce((acc, key) => {
    if (hasOwnProperty(rhs, key)) {
      const difference = deletedDiff(lhs[key], rhs[key]);
      if (isObject(difference) && isEmpty(difference))
        return acc;
      acc[key] = difference;
      return acc;
    }
    acc[key] = void 0;
    return acc;
  }, makeObjectWithoutPrototype());
};
var deleted_default = deletedDiff;

// node_modules/.pnpm/deep-object-diff@1.1.9/node_modules/deep-object-diff/mjs/updated.js
var updatedDiff = (lhs, rhs) => {
  if (lhs === rhs)
    return {};
  if (!isObject(lhs) || !isObject(rhs))
    return rhs;
  if (isDate(lhs) || isDate(rhs)) {
    if (lhs.valueOf() == rhs.valueOf())
      return {};
    return rhs;
  }
  return Object.keys(rhs).reduce((acc, key) => {
    if (hasOwnProperty(lhs, key)) {
      const difference = updatedDiff(lhs[key], rhs[key]);
      if (isEmptyObject(difference) && !isDate(difference) && (isEmptyObject(lhs[key]) || !isEmptyObject(rhs[key])))
        return acc;
      acc[key] = difference;
      return acc;
    }
    return acc;
  }, makeObjectWithoutPrototype());
};
var updated_default = updatedDiff;

// node_modules/.pnpm/deep-object-diff@1.1.9/node_modules/deep-object-diff/mjs/detailed.js
var detailedDiff = (lhs, rhs) => ({
  added: added_default(lhs, rhs),
  deleted: deleted_default(lhs, rhs),
  updated: updated_default(lhs, rhs)
});
var detailed_default = detailedDiff;

// src/helper/PromiseOut.mts
var isPromiseLike = (value) => {
  return value instanceof Object && typeof value.then === "function";
};
var PromiseOut = class {
  constructor() {
    this.is_resolved = false;
    this.is_rejected = false;
    this.is_finished = false;
    this.promise = new Promise((resolve, reject) => {
      this.resolve = (value) => {
        try {
          if (isPromiseLike(value)) {
            value.then(this.resolve, this.reject);
          } else {
            this.is_resolved = true;
            this.is_finished = true;
            resolve(this.value = value);
            this._runThen();
            this._innerFinallyArg = Object.freeze({
              status: "resolved",
              result: this.value
            });
            this._runFinally();
          }
        } catch (err) {
          this.reject(err);
        }
      };
      this.reject = (reason) => {
        this.is_rejected = true;
        this.is_finished = true;
        reject(this.reason = reason);
        this._runCatch();
        this._innerFinallyArg = Object.freeze({
          status: "rejected",
          reason: this.reason
        });
        this._runFinally();
      };
    });
  }
  onSuccess(innerThen) {
    if (this.is_resolved) {
      this.__callInnerThen(innerThen);
    } else {
      (this._innerThen || (this._innerThen = [])).push(innerThen);
    }
  }
  onError(innerCatch) {
    if (this.is_rejected) {
      this.__callInnerCatch(innerCatch);
    } else {
      (this._innerCatch || (this._innerCatch = [])).push(innerCatch);
    }
  }
  onFinished(innerFinally) {
    if (this.is_finished) {
      this.__callInnerFinally(innerFinally);
    } else {
      (this._innerFinally || (this._innerFinally = [])).push(innerFinally);
    }
  }
  _runFinally() {
    if (this._innerFinally) {
      for (const innerFinally of this._innerFinally) {
        this.__callInnerFinally(innerFinally);
      }
      this._innerFinally = void 0;
    }
  }
  __callInnerFinally(innerFinally) {
    queueMicrotask(async () => {
      try {
        await innerFinally(this._innerFinallyArg);
      } catch (err) {
        console.error(
          "Unhandled promise rejection when running onFinished",
          innerFinally,
          err
        );
      }
    });
  }
  _runThen() {
    if (this._innerThen) {
      for (const innerThen of this._innerThen) {
        this.__callInnerThen(innerThen);
      }
      this._innerThen = void 0;
    }
  }
  _runCatch() {
    if (this._innerCatch) {
      for (const innerCatch of this._innerCatch) {
        this.__callInnerCatch(innerCatch);
      }
      this._innerCatch = void 0;
    }
  }
  __callInnerThen(innerThen) {
    queueMicrotask(async () => {
      try {
        await innerThen(this.value);
      } catch (err) {
        console.error(
          "Unhandled promise rejection when running onSuccess",
          innerThen,
          err
        );
      }
    });
  }
  __callInnerCatch(innerCatch) {
    queueMicrotask(async () => {
      try {
        await innerCatch(this.value);
      } catch (err) {
        console.error(
          "Unhandled promise rejection when running onError",
          innerCatch,
          err
        );
      }
    });
  }
};

// src/helper/createSignal.mts
var createSignal = () => {
  return new Signal();
};
var Signal = class {
  constructor() {
    this._cbs = /* @__PURE__ */ new Set();
    this.listen = (cb) => {
      this._cbs.add(cb);
      return () => this._cbs.delete(cb);
    };
    this.emit = (...args) => {
      for (const cb of this._cbs) {
        cb.apply(null, args);
      }
    };
  }
};

// src/user/tool/tool.native.mts
var cros = (headers) => {
  headers.init("Access-Control-Allow-Origin", "*");
  headers.init("Access-Control-Allow-Headers", "*");
  headers.init("Access-Control-Allow-Methods", "*");
  return headers;
};
var nativeOpen = async (url) => {
  return await jsProcess.nativeFetch(
    `file://mwebview.sys.dweb/open?url=${encodeURIComponent(url)}`
  ).text();
};
var nativeActivate = async (webview_id) => {
  return await jsProcess.nativeFetch(
    `file://mwebview.sys.dweb/activate?webview_id=${encodeURIComponent(webview_id)}`
  ).text();
};
var closeDwebView = async (webview_id) => {
  return await jsProcess.nativeFetch(
    `file://mwebview.sys.dweb/close?webview_id=${encodeURIComponent(webview_id)}`
  ).text();
};

// src/user/tool/app.handle.mts
var webViewMap = /* @__PURE__ */ new Map();
var restartApp = async (servers, ipcs) => {
  const serverOp = servers.map(async (server) => {
    await server.close();
  });
  const opcOp = ipcs.map((ipc2) => {
    ipc2.close();
  });
  await Promise.all([serverOp, opcOp]);
  webViewMap.forEach(async (state) => {
    await closeDwebView(state.webviewId);
  });
  jsProcess.restart();
  return "ok";
};
var closeFront = () => {
  webViewMap.forEach(async (state) => {
    await closeDwebView(state.webviewId);
  });
  return "ok";
};

// src/helper/binaryHelper.cts
var isBinary = (data) => data instanceof ArrayBuffer || ArrayBuffer.isView(data);
var binaryToU8a = (binary) => {
  if (binary instanceof ArrayBuffer) {
    return new Uint8Array(binary);
  }
  if (binary instanceof Uint8Array) {
    return binary;
  }
  return new Uint8Array(binary.buffer, binary.byteOffset, binary.byteLength);
};
var u8aConcat = (binaryList) => {
  let totalLength = 0;
  for (const binary of binaryList) {
    totalLength += binary.byteLength;
  }
  const result = new Uint8Array(totalLength);
  let offset = 0;
  for (const binary of binaryList) {
    result.set(binary, offset);
    offset += binary.byteLength;
  }
  return result;
};

// src/helper/encoding.cts
var textEncoder = new TextEncoder();
var simpleEncoder = (data, encoding) => {
  if (encoding === "base64") {
    const byteCharacters = atob(data);
    const binary = new Uint8Array(byteCharacters.length);
    for (let i = 0; i < byteCharacters.length; i++) {
      binary[i] = byteCharacters.charCodeAt(i);
    }
    return binary;
  }
  return textEncoder.encode(data);
};
var textDecoder = new TextDecoder();
var simpleDecoder = (data, encoding) => {
  if (encoding === "base64") {
    let binary = "";
    const bytes = binaryToU8a(data);
    for (const byte of bytes) {
      binary += String.fromCharCode(byte);
    }
    return btoa(binary);
  }
  return textDecoder.decode(data);
};

// src/core/ipc/const.cts
var toIpcMethod = (method) => {
  if (method == null) {
    return "GET" /* GET */;
  }
  switch (method.toUpperCase()) {
    case "GET" /* GET */: {
      return "GET" /* GET */;
    }
    case "POST" /* POST */: {
      return "POST" /* POST */;
    }
    case "PUT" /* PUT */: {
      return "PUT" /* PUT */;
    }
    case "DELETE" /* DELETE */: {
      return "DELETE" /* DELETE */;
    }
    case "OPTIONS" /* OPTIONS */: {
      return "OPTIONS" /* OPTIONS */;
    }
    case "TRACE" /* TRACE */: {
      return "TRACE" /* TRACE */;
    }
    case "PATCH" /* PATCH */: {
      return "PATCH" /* PATCH */;
    }
    case "PURGE" /* PURGE */: {
      return "PURGE" /* PURGE */;
    }
    case "HEAD" /* HEAD */: {
      return "HEAD" /* HEAD */;
    }
  }
  throw new Error(`invalid method: ${method}`);
};
var IpcMessage = class {
  constructor(type) {
    this.type = type;
  }
};
var $dataToBinary = (data, encoding) => {
  switch (encoding) {
    case 8 /* BINARY */: {
      return data;
    }
    case 4 /* BASE64 */: {
      return simpleEncoder(data, "base64");
    }
    case 2 /* UTF8 */: {
      return simpleEncoder(data, "utf8");
    }
  }
  throw new Error(`unknown encoding: ${encoding}`);
};
var $dataToText = (data, encoding) => {
  switch (encoding) {
    case 8 /* BINARY */: {
      return simpleDecoder(data, "utf8");
    }
    case 4 /* BASE64 */: {
      return simpleDecoder(simpleEncoder(data, "base64"), "utf8");
    }
    case 2 /* UTF8 */: {
      return data;
    }
  }
  throw new Error(`unknown encoding: ${encoding}`);
};

// src/helper/cacheGetter.cts
var cacheGetter = () => {
  return (target, prop, desp) => {
    const source_fun = desp.get;
    if (source_fun === void 0) {
      throw new Error(`${target}.${prop} should has getter`);
    }
    desp.get = function() {
      const result = source_fun.call(this);
      if (desp.set) {
        desp.get = () => result;
      } else {
        delete desp.set;
        delete desp.get;
        desp.value = result;
        desp.writable = false;
      }
      Object.defineProperty(this, prop, desp);
      return result;
    };
    return desp;
  };
};

// src/helper/createSignal.cts
var createSignal2 = (autoStart) => {
  return new Signal2(autoStart);
};
var Signal2 = class {
  constructor(autoStart = true) {
    this._cbs = /* @__PURE__ */ new Set();
    this._started = false;
    this.start = () => {
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
    };
    this.listen = (cb) => {
      this._cbs.add(cb);
      this.start();
      return () => this._cbs.delete(cb);
    };
    this.emit = (...args) => {
      if (this._started) {
        this._emit(args);
      } else {
        this._cachedEmits.push(args);
      }
    };
    this._emit = (args) => {
      for (const cb of this._cbs) {
        cb.apply(null, args);
      }
    };
    this.clear = () => {
      this._cbs.clear();
    };
    if (autoStart) {
      this.start();
    }
  }
  get _cachedEmits() {
    return [];
  }
};
__decorateClass([
  cacheGetter()
], Signal2.prototype, "_cachedEmits", 1);

// src/helper/PromiseOut.cts
var isPromiseLike2 = (value) => {
  return value instanceof Object && typeof value.then === "function";
};
var PromiseOut2 = class {
  constructor() {
    this.is_resolved = false;
    this.is_rejected = false;
    this.is_finished = false;
    this.promise = new Promise((resolve, reject) => {
      this.resolve = (value) => {
        try {
          if (isPromiseLike2(value)) {
            value.then(this.resolve, this.reject);
          } else {
            this.is_resolved = true;
            this.is_finished = true;
            resolve(this.value = value);
            this._runThen();
            this._innerFinallyArg = Object.freeze({
              status: "resolved",
              result: this.value
            });
            this._runFinally();
          }
        } catch (err) {
          this.reject(err);
        }
      };
      this.reject = (reason) => {
        this.is_rejected = true;
        this.is_finished = true;
        reject(this.reason = reason);
        this._runCatch();
        this._innerFinallyArg = Object.freeze({
          status: "rejected",
          reason: this.reason
        });
        this._runFinally();
      };
    });
  }
  static resolve(v) {
    const po = new PromiseOut2();
    po.resolve(v);
    return po;
  }
  static sleep(ms) {
    const po = new PromiseOut2();
    let ti = setTimeout(() => {
      ti = void 0;
      po.resolve();
    }, ms);
    po.onFinished(() => ti !== void 0 && clearTimeout(ti));
    return po;
  }
  onSuccess(innerThen) {
    if (this.is_resolved) {
      this.__callInnerThen(innerThen);
    } else {
      (this._innerThen || (this._innerThen = [])).push(innerThen);
    }
  }
  onError(innerCatch) {
    if (this.is_rejected) {
      this.__callInnerCatch(innerCatch);
    } else {
      (this._innerCatch || (this._innerCatch = [])).push(innerCatch);
    }
  }
  onFinished(innerFinally) {
    if (this.is_finished) {
      this.__callInnerFinally(innerFinally);
    } else {
      (this._innerFinally || (this._innerFinally = [])).push(innerFinally);
    }
  }
  _runFinally() {
    if (this._innerFinally) {
      for (const innerFinally of this._innerFinally) {
        this.__callInnerFinally(innerFinally);
      }
      this._innerFinally = void 0;
    }
  }
  __callInnerFinally(innerFinally) {
    queueMicrotask(async () => {
      try {
        await innerFinally(this._innerFinallyArg);
      } catch (err) {
        console.error(
          "Unhandled promise rejection when running onFinished",
          innerFinally,
          err
        );
      }
    });
  }
  _runThen() {
    if (this._innerThen) {
      for (const innerThen of this._innerThen) {
        this.__callInnerThen(innerThen);
      }
      this._innerThen = void 0;
    }
  }
  _runCatch() {
    if (this._innerCatch) {
      for (const innerCatch of this._innerCatch) {
        this.__callInnerCatch(innerCatch);
      }
      this._innerCatch = void 0;
    }
  }
  __callInnerThen(innerThen) {
    queueMicrotask(async () => {
      try {
        await innerThen(this.value);
      } catch (err) {
        console.error(
          "Unhandled promise rejection when running onSuccess",
          innerThen,
          err
        );
      }
    });
  }
  __callInnerCatch(innerCatch) {
    queueMicrotask(async () => {
      try {
        await innerCatch(this.value);
      } catch (err) {
        console.error(
          "Unhandled promise rejection when running onError",
          innerCatch,
          err
        );
      }
    });
  }
};

// src/helper/$makeFetchBaseExtends.cts
var $makeFetchExtends = (exts) => {
  return exts;
};
var fetchBaseExtends = $makeFetchExtends({
  async number() {
    const text = await this.text();
    return +text;
  },
  async ok() {
    const response = await this;
    if (response.status >= 400) {
      throw response.statusText || await response.text();
    } else {
      return response;
    }
  },
  async text() {
    const ok = await this.ok();
    return ok.text();
  },
  async binary() {
    const ok = await this.ok();
    return ok.arrayBuffer();
  },
  async boolean() {
    const text = await this.text();
    return text === "true";
  },
  async object() {
    const ok = await this.ok();
    try {
      return await ok.json();
    } catch (err) {
      debugger;
      throw err;
    }
  }
});

// src/helper/JsonlinesStream.cts
var JsonlinesStream = class extends TransformStream {
  constructor() {
    let json = "";
    const try_enqueue = (controller, jsonline) => {
      try {
        controller.enqueue(JSON.parse(jsonline));
      } catch (err) {
        controller.error(err);
        return true;
      }
    };
    super({
      transform: (chunk, controller) => {
        json += chunk;
        let line_break_index;
        while ((line_break_index = json.indexOf("\n")) !== -1) {
          const jsonline = json.slice(0, line_break_index);
          json = json.slice(jsonline.length + 1);
          if (try_enqueue(controller, jsonline)) {
            break;
          }
        }
      },
      flush: (controller) => {
        json = json.trim();
        if (json.length > 0) {
          try_enqueue(controller, json);
        }
        controller.terminate();
      }
    });
  }
};

// src/helper/$makeFetchStreamExtends.cts
var $makeFetchExtends2 = (exts) => {
  return exts;
};
var fetchStreamExtends = $makeFetchExtends2({
  /** 将响应的内容解码成 jsonlines 格式 */
  async jsonlines() {
    return (
      // 首先要能拿到数据流
      (await this.stream()).pipeThrough(new TextDecoderStream()).pipeThrough(new JsonlinesStream())
    );
  },
  /** 获取 Response 的 body 为 ReadableStream */
  stream() {
    return this.then((res) => {
      const stream = res.body;
      if (stream == null) {
        throw new Error(`request ${res.url} could not by stream.`);
      }
      return stream;
    });
  }
});

// src/helper/$makeFetchExtends.cts
var fetchExtends = {
  ...fetchBaseExtends,
  ...fetchStreamExtends
};

// src/helper/urlHelper.cts
var URL_BASE = "document" in globalThis ? document.baseURI : "location" in globalThis && (location.protocol === "http:" || location.protocol === "https:" || location.protocol === "file:" || location.protocol === "chrome-extension:") ? location.href : "file:///";
var parseUrl = (url, base = URL_BASE) => {
  return new URL(url, base);
};

// src/helper/normalizeFetchArgs.cts
var normalizeFetchArgs = (url, init) => {
  let _parsed_url2;
  let _request_init = init;
  if (typeof url === "string") {
    _parsed_url2 = parseUrl(url);
  } else if (url instanceof Request) {
    _parsed_url2 = parseUrl(url.url);
    _request_init = url;
  } else if (url instanceof URL) {
    _parsed_url2 = url;
  }
  if (_parsed_url2 === void 0) {
    throw new Error(`no found url for fetch`);
  }
  const parsed_url = _parsed_url2;
  const request_init = _request_init ?? {};
  return {
    parsed_url,
    request_init
  };
};

// src/helper/AdaptersManager.cts
var AdaptersManager = class {
  constructor() {
    this.adapterOrderMap = /* @__PURE__ */ new Map();
    this.orderdAdapters = [];
  }
  _reorder() {
    this.orderdAdapters = [...this.adapterOrderMap].sort((a, b) => a[1] - b[1]).map((a) => a[0]);
  }
  get adapters() {
    return this.orderdAdapters;
  }
  append(adapter, order = 0) {
    this.adapterOrderMap.set(adapter, order);
    this._reorder();
    return () => this.remove(adapter);
  }
  remove(adapter) {
    if (this.adapterOrderMap.delete(adapter) != null) {
      this._reorder();
      return true;
    }
    return false;
  }
};

// src/sys/dns/nativeFetch.cts
var nativeFetchAdaptersManager = new AdaptersManager();

// src/core/micro-module.cts
var MicroModule = class {
  constructor() {
    this._running_state_lock = PromiseOut2.resolve(false);
    this._after_shutdown_signal = createSignal2();
    this._ipcSet = /* @__PURE__ */ new Set();
    /**
     * 内部程序与外部程序通讯的方法
     * TODO 这里应该是可以是多个
     */
    this._connectSignal = createSignal2();
  }
  get isRunning() {
    return this._running_state_lock.promise;
  }
  async before_bootstrap(context) {
    if (await this._running_state_lock.promise) {
      throw new Error(`module ${this.mmid} alreay running`);
    }
    this._running_state_lock = new PromiseOut2();
  }
  async after_bootstrap(context) {
    this._running_state_lock.resolve(true);
  }
  async bootstrap(context) {
    await this.before_bootstrap(context);
    try {
      await this._bootstrap(context);
    } finally {
      this.after_bootstrap(context);
    }
  }
  async before_shutdown() {
    if (false === await this._running_state_lock.promise) {
      throw new Error(`module ${this.mmid} already shutdown`);
    }
    this._running_state_lock = new PromiseOut2();
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
    } finally {
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
  async beConnect(ipc2, reason) {
    this._ipcSet.add(ipc2);
    ipc2.onClose(() => {
      this._ipcSet.delete(ipc2);
    });
    this._connectSignal.emit(ipc2, reason);
  }
  async _nativeFetch(url, init) {
    const args = normalizeFetchArgs(url, init);
    for (const adapter of nativeFetchAdaptersManager.adapters) {
      const response = await adapter(this, args.parsed_url, args.request_init);
      if (response !== void 0) {
        return response;
      }
    }
    return fetch(args.parsed_url, args.request_init);
  }
  nativeFetch(url, init) {
    if (init?.body instanceof ReadableStream) {
      Reflect.set(init, "duplex", "half");
    }
    return Object.assign(this._nativeFetch(url, init), fetchExtends);
  }
};

// src/core/ipc/IpcRequest.cts
var import_once = __toESM(require_once());

// src/helper/readableStreamHelper.cts
async function* _doRead(reader) {
  try {
    while (true) {
      const item = await reader.read();
      if (item.done) {
        break;
      }
      yield item.value;
    }
  } finally {
    reader.releaseLock();
  }
}
var streamRead = (stream, options = {}) => {
  return _doRead(stream.getReader());
};
var binaryStreamRead = (stream, options = {}) => {
  const reader = streamRead(stream, options);
  var done = false;
  var cache = new Uint8Array(0);
  const appendToCache = async () => {
    const item = await reader.next();
    if (item.done) {
      done = true;
      return false;
    } else {
      cache = u8aConcat([cache, item.value]);
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
    } else {
      throw new Error(
        `fail to read bytes(${cache.length}/${size} byte) in stream`
      );
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
    readInt
  });
};
var streamReadAll = async (stream, options = {}) => {
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
    result
  };
};
var streamReadAllBuffer = async (stream) => {
  return (await streamReadAll(stream, {
    complete(items) {
      return u8aConcat(items);
    }
  })).result;
};
var ReadableStreamOut = class {
  constructor(strategy) {
    this.strategy = strategy;
    this.stream = new ReadableStream(
      {
        cancel: (reason) => {
          this._on_cancel_signal?.emit(reason);
        },
        start: (controller) => {
          this.controller = controller;
        },
        pull: () => {
          this._on_pull_signal?.emit();
        }
      },
      this.strategy
    );
  }
  get onCancel() {
    return (this._on_cancel_signal ??= createSignal2()).listen;
  }
  get onPull() {
    return (this._on_pull_signal ??= createSignal2()).listen;
  }
};

// src/core/ipc/IpcBody.cts
var _IpcBody = class {
  get raw() {
    return this._bodyHub.data;
  }
  async u8a() {
    const bodyHub = this._bodyHub;
    let body_u8a = bodyHub.u8a;
    if (body_u8a === void 0) {
      if (bodyHub.stream) {
        body_u8a = await streamReadAllBuffer(bodyHub.stream);
      } else if (bodyHub.text !== void 0) {
        body_u8a = simpleEncoder(bodyHub.text, "utf8");
      } else {
        throw new Error(`invalid body type`);
      }
      bodyHub.u8a = body_u8a;
      _IpcBody.wm.set(body_u8a, this);
    }
    return body_u8a;
  }
  async stream() {
    const bodyHub = this._bodyHub;
    let body_stream = bodyHub.stream;
    if (body_stream === void 0) {
      body_stream = new Blob([await this.u8a()]).stream();
      bodyHub.stream = body_stream;
      _IpcBody.wm.set(body_stream, this);
    }
    return body_stream;
  }
  async text() {
    const bodyHub = await this._bodyHub;
    let body_text = bodyHub.text;
    if (body_text === void 0) {
      body_text = simpleDecoder(await this.u8a(), "utf8");
      bodyHub.text = body_text;
    }
    return body_text;
  }
};
var IpcBody = _IpcBody;
IpcBody.wm = /* @__PURE__ */ new WeakMap();
var BodyHub = class {
  constructor(data) {
    this.data = data;
    if (typeof data === "string") {
      this.text = data;
    } else if (data instanceof ReadableStream) {
      this.stream = data;
    } else {
      this.u8a = data;
    }
  }
};

// src/core/ipc/IpcStreamData.cts
var _IpcStreamData = class extends IpcMessage {
  constructor(stream_id, data, encoding) {
    super(2 /* STREAM_DATA */);
    this.stream_id = stream_id;
    this.data = data;
    this.encoding = encoding;
  }
  static fromBase64(stream_id, data) {
    return new _IpcStreamData(
      stream_id,
      simpleDecoder(data, "base64"),
      4 /* BASE64 */
    );
  }
  static fromBinary(stream_id, data) {
    return new _IpcStreamData(stream_id, data, 8 /* BINARY */);
  }
  static fromUtf8(stream_id, data) {
    return new _IpcStreamData(
      stream_id,
      simpleDecoder(data, "utf8"),
      2 /* UTF8 */
    );
  }
  get binary() {
    return $dataToBinary(this.data, this.encoding);
  }
  get text() {
    return $dataToText(this.data, this.encoding);
  }
  get jsonAble() {
    if (this.encoding === 8 /* BINARY */) {
      return _IpcStreamData.fromBase64(this.stream_id, this.data);
    }
    return this;
  }
  toJSON() {
    return { ...this.jsonAble };
  }
};
var IpcStreamData = _IpcStreamData;
__decorateClass([
  cacheGetter()
], IpcStreamData.prototype, "binary", 1);
__decorateClass([
  cacheGetter()
], IpcStreamData.prototype, "text", 1);
__decorateClass([
  cacheGetter()
], IpcStreamData.prototype, "jsonAble", 1);

// src/core/ipc/IpcStreamEnd.cts
var IpcStreamEnd = class extends IpcMessage {
  constructor(stream_id) {
    super(5 /* STREAM_END */);
    this.stream_id = stream_id;
  }
};

// src/core/ipc/MetaBody.cts
var _MetaBody = class {
  constructor(type, senderUid, data, streamId, receiverUid, metaId = simpleDecoder(
    crypto.getRandomValues(new Uint8Array(8)),
    "base64"
  )) {
    this.type = type;
    this.senderUid = senderUid;
    this.data = data;
    this.streamId = streamId;
    this.receiverUid = receiverUid;
    this.metaId = metaId;
  }
  static fromJSON(metaBody) {
    if (metaBody instanceof _MetaBody === false) {
      metaBody = new _MetaBody(
        metaBody.type,
        metaBody.senderUid,
        metaBody.data,
        metaBody.streamId,
        metaBody.receiverUid,
        metaBody.metaId
      );
    }
    return metaBody;
  }
  static fromText(senderUid, data, streamId, receiverUid) {
    return new _MetaBody(
      streamId == null ? IPC_META_BODY_TYPE.INLINE_TEXT : IPC_META_BODY_TYPE.STREAM_WITH_TEXT,
      senderUid,
      data,
      streamId,
      receiverUid
    );
  }
  static fromBase64(senderUid, data, streamId, receiverUid) {
    return new _MetaBody(
      streamId == null ? IPC_META_BODY_TYPE.INLINE_BASE64 : IPC_META_BODY_TYPE.STREAM_WITH_BASE64,
      senderUid,
      data,
      streamId,
      receiverUid
    );
  }
  static fromBinary(sender, data, streamId, receiverUid) {
    if (typeof sender === "number") {
      return new _MetaBody(
        streamId == null ? IPC_META_BODY_TYPE.INLINE_BINARY : IPC_META_BODY_TYPE.STREAM_WITH_BINARY,
        sender,
        data,
        streamId,
        receiverUid
      );
    }
    if (sender.support_binary) {
      return this.fromBinary(sender.uid, data, streamId, receiverUid);
    }
    return this.fromBase64(
      sender.uid,
      simpleDecoder(data, "base64"),
      streamId,
      receiverUid
    );
  }
  get type_encoding() {
    const encoding = this.type & 254;
    switch (encoding) {
      case 2 /* UTF8 */:
        return 2 /* UTF8 */;
      case 4 /* BASE64 */:
        return 4 /* BASE64 */;
      case 8 /* BINARY */:
        return 8 /* BINARY */;
    }
  }
  get type_isInline() {
    return (this.type & 1 /* INLINE */) !== 0;
  }
  get type_isStream() {
    return (this.type & 1 /* INLINE */) === 0;
  }
  get jsonAble() {
    if (this.type_encoding === 8 /* BINARY */) {
      return _MetaBody.fromBase64(
        this.senderUid,
        simpleDecoder(this.data, "base64"),
        this.streamId,
        this.receiverUid
      );
    }
    return this;
  }
  toJSON() {
    return { ...this.jsonAble };
  }
};
var MetaBody = _MetaBody;
__decorateClass([
  cacheGetter()
], MetaBody.prototype, "type_encoding", 1);
__decorateClass([
  cacheGetter()
], MetaBody.prototype, "type_isInline", 1);
__decorateClass([
  cacheGetter()
], MetaBody.prototype, "type_isStream", 1);
__decorateClass([
  cacheGetter()
], MetaBody.prototype, "jsonAble", 1);
var IPC_META_BODY_TYPE = ((IPC_META_BODY_TYPE2) => {
  IPC_META_BODY_TYPE2[IPC_META_BODY_TYPE2["STREAM_ID"] = 0] = "STREAM_ID";
  IPC_META_BODY_TYPE2[IPC_META_BODY_TYPE2["INLINE"] = 1] = "INLINE";
  IPC_META_BODY_TYPE2[IPC_META_BODY_TYPE2["STREAM_WITH_TEXT"] = 0 /* STREAM_ID */ | 2 /* UTF8 */] = "STREAM_WITH_TEXT";
  IPC_META_BODY_TYPE2[IPC_META_BODY_TYPE2["STREAM_WITH_BASE64"] = 0 /* STREAM_ID */ | 4 /* BASE64 */] = "STREAM_WITH_BASE64";
  IPC_META_BODY_TYPE2[IPC_META_BODY_TYPE2["STREAM_WITH_BINARY"] = 0 /* STREAM_ID */ | 8 /* BINARY */] = "STREAM_WITH_BINARY";
  IPC_META_BODY_TYPE2[IPC_META_BODY_TYPE2["INLINE_TEXT"] = 1 /* INLINE */ | 2 /* UTF8 */] = "INLINE_TEXT";
  IPC_META_BODY_TYPE2[IPC_META_BODY_TYPE2["INLINE_BASE64"] = 1 /* INLINE */ | 4 /* BASE64 */] = "INLINE_BASE64";
  IPC_META_BODY_TYPE2[IPC_META_BODY_TYPE2["INLINE_BINARY"] = 1 /* INLINE */ | 8 /* BINARY */] = "INLINE_BINARY";
  return IPC_META_BODY_TYPE2;
})(IPC_META_BODY_TYPE || {});

// src/core/ipc/IpcBodySender.cts
var _IpcBodySender = class extends IpcBody {
  constructor(data, ipc2) {
    super();
    this.data = data;
    this.ipc = ipc2;
    this.isStream = this.data instanceof ReadableStream;
    this.streamCtorSignal = createSignal2();
    /**
     * 被哪些 ipc 所真正使用，使用的进度分别是多少
     *
     * 这个进度 用于 类似流的 多发
     */
    this.usedIpcMap = /* @__PURE__ */ new Map();
    this.UsedIpcInfo = class UsedIpcInfo {
      constructor(ipcBody, ipc2, bandwidth = 0, fuse = 0) {
        this.ipcBody = ipcBody;
        this.ipc = ipc2;
        this.bandwidth = bandwidth;
        this.fuse = fuse;
      }
      emitStreamPull(message) {
        return this.ipcBody.emitStreamPull(this, message);
      }
      emitStreamPaused(message) {
        return this.ipcBody.emitStreamPaused(this, message);
      }
      emitStreamAborted() {
        return this.ipcBody.emitStreamAborted(this);
      }
    };
    this.closeSignal = createSignal2();
    this.openSignal = createSignal2();
    this._isStreamOpened = false;
    this._isStreamClosed = false;
    /// bodyAsMeta
    this._bodyHub = new BodyHub(this.data);
    this.metaBody = this.$bodyAsMeta(this.data, this.ipc);
    if (typeof data !== "string") {
      IpcBody.wm.set(data, this);
    }
    _IpcBodySender.$usableByIpc(ipc2, this);
  }
  static from(data, ipc2) {
    if (typeof data !== "string") {
      const cache = IpcBody.wm.get(data);
      if (cache !== void 0) {
        return cache;
      }
    }
    return new _IpcBodySender(data, ipc2);
  }
  /**
   * 绑定使用
   */
  useByIpc(ipc2) {
    const info = this.usedIpcMap.get(ipc2);
    if (info !== void 0) {
      return info;
    }
    if (this.isStream && !this._isStreamOpened) {
      const info2 = new this.UsedIpcInfo(this, ipc2);
      this.usedIpcMap.set(ipc2, info2);
      this.closeSignal.listen(() => {
        this.emitStreamAborted(info2);
      });
      return info2;
    }
  }
  /**
   * 拉取数据
   */
  emitStreamPull(info, message) {
    info.bandwidth = message.bandwidth;
    this.streamCtorSignal.emit(0 /* PULLING */);
  }
  /**
   * 暂停数据
   */
  emitStreamPaused(info, message) {
    info.bandwidth = -1;
    info.fuse = message.fuse;
    let paused = true;
    for (const info2 of this.usedIpcMap.values()) {
      if (info2.bandwidth >= 0) {
        paused = false;
        break;
      }
    }
    if (paused) {
      this.streamCtorSignal.emit(1 /* PAUSED */);
    }
  }
  /**
   * 解绑使用
   */
  emitStreamAborted(info) {
    if (this.usedIpcMap.delete(info.ipc) != null) {
      if (this.usedIpcMap.size === 0) {
        this.streamCtorSignal.emit(2 /* ABORTED */);
      }
    }
  }
  onStreamClose(cb) {
    return this.closeSignal.listen(cb);
  }
  onStreamOpen(cb) {
    return this.openSignal.listen(cb);
  }
  get isStreamOpened() {
    return this._isStreamOpened;
  }
  set isStreamOpened(value) {
    if (this._isStreamOpened !== value) {
      this._isStreamOpened = value;
      if (value) {
        this.openSignal.emit();
        this.openSignal.clear();
      }
    }
  }
  get isStreamClosed() {
    return this._isStreamClosed;
  }
  set isStreamClosed(value) {
    if (this._isStreamClosed !== value) {
      this._isStreamClosed = value;
      if (value) {
        this.closeSignal.emit();
        this.closeSignal.clear();
      }
    }
  }
  emitStreamClose() {
    this.isStreamOpened = true;
    this.isStreamClosed = true;
  }
  $bodyAsMeta(body, ipc2) {
    if (typeof body === "string") {
      return MetaBody.fromText(ipc2.uid, body);
    }
    if (body instanceof ReadableStream) {
      return this.$streamAsMeta(body, ipc2);
    }
    return MetaBody.fromBinary(ipc2, body);
  }
  /**
   * 如果 rawData 是流模式，需要提供数据发送服务
   *
   * 这里不会一直无脑发，而是对方有需要的时候才发
   * @param stream_id
   * @param stream
   * @param ipc
   */
  $streamAsMeta(stream, ipc2) {
    const stream_id = getStreamId(stream);
    const reader = binaryStreamRead(stream);
    (async () => {
      let pullingLock = new PromiseOut2();
      this.streamCtorSignal.listen(async (signal) => {
        switch (signal) {
          case 0 /* PULLING */: {
            pullingLock.resolve();
            break;
          }
          case 1 /* PAUSED */: {
            if (pullingLock.is_finished) {
              pullingLock = new PromiseOut2();
            }
            break;
          }
          case 2 /* ABORTED */: {
            await reader.return();
            await stream.cancel();
            this.emitStreamClose();
          }
        }
      });
      while (true) {
        await pullingLock.promise;
        const availableLen = await reader.available();
        if (availableLen > 0) {
          this.isStreamOpened = true;
          const message = IpcStreamData.fromBinary(
            stream_id,
            await reader.readBinary(availableLen)
          );
          for (const ipc3 of this.usedIpcMap.keys()) {
            ipc3.postMessage(message);
          }
        } else if (availableLen === -1) {
          const message = new IpcStreamEnd(stream_id);
          for (const ipc3 of this.usedIpcMap.keys()) {
            ipc3.postMessage(message);
          }
          this.emitStreamClose();
          break;
        }
      }
    })().catch(console.error);
    let streamType = 0 /* STREAM_ID */;
    let streamFirstData = "";
    if ("preReadableSize" in stream && typeof stream.preReadableSize === "number" && stream.preReadableSize > 0) {
    }
    return new MetaBody(streamType, ipc2.uid, streamFirstData, stream_id);
  }
};
var IpcBodySender = _IpcBodySender;
/**
 * ipc 将会使用它
 */
IpcBodySender.$usableByIpc = (ipc2, ipcBody) => {
  if (ipcBody.isStream && !ipcBody._isStreamOpened) {
    const streamId = ipcBody.metaBody.streamId;
    let usableIpcBodyMapper = IpcUsableIpcBodyMap.get(ipc2);
    if (usableIpcBodyMapper === void 0) {
      const mapper = new UsableIpcBodyMapper();
      mapper.onDestroy(
        ipc2.onStream((message) => {
          switch (message.type) {
            case 3 /* STREAM_PULLING */:
              mapper.get(message.stream_id)?.useByIpc(ipc2)?.emitStreamPull(message);
              break;
            case 4 /* STREAM_PAUSED */:
              mapper.get(message.stream_id)?.useByIpc(ipc2)?.emitStreamPaused(message);
              break;
            case 6 /* STREAM_ABORT */:
              mapper.get(message.stream_id)?.useByIpc(ipc2)?.emitStreamAborted();
              break;
          }
        })
      );
      mapper.onDestroy(() => IpcUsableIpcBodyMap.delete(ipc2));
      usableIpcBodyMapper = mapper;
    }
    if (usableIpcBodyMapper.add(streamId, ipcBody)) {
      ipcBody.onStreamClose(() => usableIpcBodyMapper.remove(streamId));
    }
  }
};
var streamIdWM = /* @__PURE__ */ new WeakMap();
var stream_id_acc = 0;
var getStreamId = (stream) => {
  let id = streamIdWM.get(stream);
  if (id === void 0) {
    id = `rs-${stream_id_acc++}`;
    streamIdWM.set(stream, id);
  }
  return id;
};
var setStreamId = (stream, cid) => {
  let id = streamIdWM.get(stream);
  if (id === void 0) {
    streamIdWM.set(stream, id = `rs-${stream_id_acc++}[${cid}]`);
  }
  return id;
};
var UsableIpcBodyMapper = class {
  constructor() {
    this.map = /* @__PURE__ */ new Map();
    this.destroySignal = createSignal2();
  }
  add(streamId, ipcBody) {
    if (this.map.has(streamId)) {
      return true;
    }
    this.map.set(streamId, ipcBody);
    return false;
  }
  get(streamId) {
    return this.map.get(streamId);
  }
  remove(streamId) {
    const ipcBody = this.map.get(streamId);
    if (ipcBody !== void 0) {
      this.map.delete(streamId);
      if (this.map.size === 0) {
        this.destroySignal.emit();
        this.destroySignal.clear();
      }
    }
  }
  onDestroy(cb) {
    this.destroySignal.listen(cb);
  }
};
var IpcUsableIpcBodyMap = /* @__PURE__ */ new WeakMap();

// src/core/ipc/IpcHeaders.cts
var IpcHeaders = class extends Headers {
  init(key, value) {
    if (this.has(key)) {
      return;
    }
    this.set(key, value);
  }
  toJSON() {
    const record = {};
    this.forEach((value, key) => {
      record[key.replace(/\w+/g, (w) => w[0].toUpperCase() + w.slice(1))] = value;
    });
    return record;
  }
};

// src/core/ipc/IpcRequest.cts
var _parsed_url;
var _IpcRequest = class extends IpcMessage {
  constructor(req_id, url, method, headers, body, ipc2) {
    super(0 /* REQUEST */);
    this.req_id = req_id;
    this.url = url;
    this.method = method;
    this.headers = headers;
    this.body = body;
    this.ipc = ipc2;
    __privateAdd(this, _parsed_url, void 0);
    this.ipcReqMessage = (0, import_once.default)(
      () => new IpcReqMessage(
        this.req_id,
        this.method,
        this.url,
        this.headers.toJSON(),
        this.body.metaBody
      )
    );
    if (body instanceof IpcBodySender) {
      IpcBodySender.$usableByIpc(ipc2, body);
    }
  }
  get parsed_url() {
    return __privateGet(this, _parsed_url) ?? __privateSet(this, _parsed_url, parseUrl(this.url));
  }
  static fromText(req_id, url, method = "GET" /* GET */, headers = new IpcHeaders(), text, ipc2) {
    return new _IpcRequest(
      req_id,
      url,
      method,
      headers,
      IpcBodySender.from(text, ipc2),
      ipc2
    );
  }
  static fromBinary(req_id, url, method = "GET" /* GET */, headers = new IpcHeaders(), binary, ipc2) {
    headers.init("Content-Type", "application/octet-stream");
    headers.init("Content-Length", binary.byteLength + "");
    return new _IpcRequest(
      req_id,
      url,
      method,
      headers,
      IpcBodySender.from(binaryToU8a(binary), ipc2),
      ipc2
    );
  }
  static fromStream(req_id, url, method = "GET" /* GET */, headers = new IpcHeaders(), stream, ipc2) {
    headers.init("Content-Type", "application/octet-stream");
    return new _IpcRequest(
      req_id,
      url,
      method,
      headers,
      IpcBodySender.from(stream, ipc2),
      ipc2
    );
  }
  static fromRequest(req_id, ipc2, url, init = {}) {
    const method = toIpcMethod(init.method);
    const headers = init.headers instanceof IpcHeaders ? init.headers : new IpcHeaders(init.headers);
    let ipcBody;
    if (isBinary(init.body)) {
      ipcBody = IpcBodySender.from(init.body, ipc2);
    } else if (init.body instanceof ReadableStream) {
      ipcBody = IpcBodySender.from(init.body, ipc2);
    } else {
      ipcBody = IpcBodySender.from(init.body ?? "", ipc2);
    }
    return new _IpcRequest(req_id, url, method, headers, ipcBody, ipc2);
  }
  toRequest() {
    const { method } = this;
    let body;
    if ((method === "GET" /* GET */ || method === "HEAD" /* HEAD */) === false) {
      body = this.body.raw;
    }
    const init = {
      method,
      headers: this.headers,
      body
    };
    if (body) {
      Reflect.set(init, "duplex", "half");
    }
    return new Request(this.url, init);
  }
  toJSON() {
    const { method } = this;
    let body;
    if ((method === "GET" /* GET */ || method === "HEAD" /* HEAD */) === false) {
      body = this.body.raw;
      return new IpcReqMessage(
        this.req_id,
        this.method,
        this.url,
        this.headers.toJSON(),
        this.body.metaBody
      );
    }
    return this.ipcReqMessage();
  }
};
var IpcRequest = _IpcRequest;
_parsed_url = new WeakMap();
var IpcReqMessage = class extends IpcMessage {
  constructor(req_id, method, url, headers, metaBody) {
    super(0 /* REQUEST */);
    this.req_id = req_id;
    this.method = method;
    this.url = url;
    this.headers = headers;
    this.metaBody = metaBody;
  }
};

// src/core/ipc/ipc.cts
var ipc_uid_acc = 0;
var Ipc = class {
  constructor() {
    this.uid = ipc_uid_acc++;
    this._support_message_pack = false;
    this._support_protobuf = false;
    this._support_raw = false;
    this._support_binary = false;
    this._messageSignal = createSignal2(false);
    this.onMessage = this._messageSignal.listen;
    this._closed = false;
    this._closeSignal = createSignal2(false);
    this.onClose = this._closeSignal.listen;
    this._req_id_acc = 0;
  }
  /**
   * 是否支持使用 MessagePack 直接传输二进制
   * 在一些特殊的场景下支持字符串传输，比如与webview的通讯
   * 二进制传输在网络相关的服务里被支持，里效率会更高，但前提是对方有 MessagePack 的编解码能力
   * 否则 JSON 是通用的传输协议
   */
  get support_message_pack() {
    return this._support_message_pack;
  }
  /**
   * 是否支持使用 Protobuf 直接传输二进制
   * 在网络环境里，protobuf 是更加高效的协议
   */
  get support_protobuf() {
    return this._support_protobuf;
  }
  /**
   * 是否支持结构化内存协议传输：
   * 就是说不需要对数据手动序列化反序列化，可以直接传输内存对象
   */
  get support_raw() {
    return this._support_raw;
  }
  /**
   * 是否支持二进制传输
   */
  get support_binary() {
    return this._support_binary ?? (this.support_message_pack || this.support_protobuf || this.support_raw);
  }
  asRemoteInstance() {
    if (this.remote instanceof MicroModule) {
      return this.remote;
    }
  }
  postMessage(message) {
    if (this._closed) {
      return;
    }
    this._doPostMessage(message);
  }
  get _onRequestSignal() {
    const signal = createSignal2(false);
    this.onMessage((request, ipc2) => {
      if (request.type === 0 /* REQUEST */) {
        signal.emit(request, ipc2);
      }
    });
    return signal;
  }
  onRequest(cb) {
    return this._onRequestSignal.listen(cb);
  }
  get _onStreamSignal() {
    const signal = createSignal2(false);
    this.onMessage((request, ipc2) => {
      if ("stream_id" in request) {
        signal.emit(request, ipc2);
      }
    });
    return signal;
  }
  onStream(cb) {
    return this._onStreamSignal.listen(cb);
  }
  get _onEventSignal() {
    const signal = createSignal2(false);
    this.onMessage((event, ipc2) => {
      if (event.type === 7 /* EVENT */) {
        signal.emit(event, ipc2);
      }
    });
    return signal;
  }
  onEvent(cb) {
    return this._onEventSignal.listen(cb);
  }
  close() {
    if (this._closed) {
      return;
    }
    this._closed = true;
    this._doClose();
    this._closeSignal.emit();
    this._closeSignal.clear();
  }
  allocReqId(url) {
    return this._req_id_acc++;
  }
  get _reqresMap() {
    const reqresMap = /* @__PURE__ */ new Map();
    this.onMessage((message) => {
      if (message.type === 1 /* RESPONSE */) {
        const response_po = reqresMap.get(message.req_id);
        if (response_po) {
          reqresMap.delete(message.req_id);
          response_po.resolve(message);
        } else {
          throw new Error(`no found response by req_id: ${message.req_id}`);
        }
      }
    });
    return reqresMap;
  }
  /** 发起请求并等待响应 */
  request(url, init) {
    const req_id = this.allocReqId();
    const ipcRequest = IpcRequest.fromRequest(req_id, this, url, init);
    const result = this.registerReqId(req_id);
    this.postMessage(ipcRequest);
    return result.promise;
  }
  /** 自定义注册 请求与响应 的id */
  registerReqId(req_id = this.allocReqId()) {
    const response_po = new PromiseOut2();
    this._reqresMap.set(req_id, response_po);
    return response_po;
  }
};
__decorateClass([
  cacheGetter()
], Ipc.prototype, "_onRequestSignal", 1);
__decorateClass([
  cacheGetter()
], Ipc.prototype, "_onStreamSignal", 1);
__decorateClass([
  cacheGetter()
], Ipc.prototype, "_onEventSignal", 1);
__decorateClass([
  cacheGetter()
], Ipc.prototype, "_reqresMap", 1);

// src/core/ipc/IpcStreamAbort.cts
var IpcStreamAbort = class extends IpcMessage {
  constructor(stream_id) {
    super(6 /* STREAM_ABORT */);
    this.stream_id = stream_id;
  }
};

// src/core/ipc/IpcStreamPulling.cts
var IpcStreamPulling = class extends IpcMessage {
  constructor(stream_id, bandwidth) {
    super(3 /* STREAM_PULLING */);
    this.stream_id = stream_id;
    this.bandwidth = bandwidth ?? 0;
  }
};

// src/core/ipc/IpcBodyReceiver.cts
var _IpcBodyReceiver = class extends IpcBody {
  constructor(metaBody, ipc2) {
    super();
    this.metaBody = metaBody;
    if (metaBody.type_isStream) {
      const streamId = metaBody.streamId;
      const senderIpcUid = metaBody.senderUid;
      const metaId = `${senderIpcUid}/${streamId}`;
      if (_IpcBodyReceiver.metaIdIpcMap.has(metaId) === false) {
        ipc2.onClose(() => {
          _IpcBodyReceiver.metaIdIpcMap.delete(metaId);
        });
        _IpcBodyReceiver.metaIdIpcMap.set(metaId, ipc2);
        metaBody.receiverUid = ipc2.uid;
      }
      const receiver = _IpcBodyReceiver.metaIdIpcMap.get(metaId);
      if (receiver === void 0) {
        throw new Error(`no found ipc by metaId:${metaId}`);
      }
      ipc2 = receiver;
      this._bodyHub = new BodyHub($metaToStream(this.metaBody, ipc2));
    } else
      switch (metaBody.type_encoding) {
        case 2 /* UTF8 */:
          this._bodyHub = new BodyHub(metaBody.data);
          break;
        case 4 /* BASE64 */:
          this._bodyHub = new BodyHub(
            simpleEncoder(metaBody.data, "base64")
          );
          break;
        case 8 /* BINARY */:
          this._bodyHub = new BodyHub(metaBody.data);
          break;
        default:
          throw new Error(`invalid metaBody type: ${metaBody.type}`);
      }
  }
};
var IpcBodyReceiver = _IpcBodyReceiver;
IpcBodyReceiver.metaIdIpcMap = /* @__PURE__ */ new Map();
var $metaToStream = (metaBody, ipc2) => {
  if (ipc2 == null) {
    throw new Error(`miss ipc when ipc-response has stream-body`);
  }
  const stream_ipc = ipc2;
  const stream_id = metaBody.streamId;
  let paused = true;
  const stream = new ReadableStream(
    {
      start(controller) {
        let firstData;
        switch (metaBody.type_encoding) {
          case 2 /* UTF8 */:
            firstData = simpleEncoder(metaBody.data, "utf8");
            break;
          case 4 /* BASE64 */:
            firstData = simpleEncoder(metaBody.data, "base64");
            break;
          case 8 /* BINARY */:
            firstData = metaBody.data;
            break;
        }
        if (firstData) {
          controller.enqueue(firstData);
        }
        const off = ipc2.onStream((message) => {
          if (message.stream_id === stream_id) {
            switch (message.type) {
              case 2 /* STREAM_DATA */:
                controller.enqueue(message.binary);
                break;
              case 5 /* STREAM_END */:
                controller.close();
                off();
                break;
            }
          }
        });
      },
      pull(controller) {
        if (paused) {
          paused = false;
          stream_ipc.postMessage(new IpcStreamPulling(stream_id));
        }
      },
      cancel() {
        stream_ipc.postMessage(new IpcStreamAbort(stream_id));
      }
    },
    {
      /// 按需 pull, 不可以0以上。否则一开始的时候就会发送pull指令，会导致远方直接把流给读取出来。
      /// 这会导致一些优化的行为异常，有些时候流一旦开始读取了，其他读取者就不能再进入读取了。那么流转发就不能工作了
      highWaterMark: 0
    }
  );
  return stream;
};
new WritableStream({});

// src/core/ipc/IpcResponse.cts
var import_once2 = __toESM(require_once());
var _ipcHeaders;
var _IpcResponse = class extends IpcMessage {
  constructor(req_id, statusCode, headers, body, ipc2) {
    super(1 /* RESPONSE */);
    this.req_id = req_id;
    this.statusCode = statusCode;
    this.headers = headers;
    this.body = body;
    this.ipc = ipc2;
    __privateAdd(this, _ipcHeaders, void 0);
    this.ipcResMessage = (0, import_once2.default)(
      () => new IpcResMessage(
        this.req_id,
        this.statusCode,
        this.headers.toJSON(),
        this.body.metaBody
      )
    );
    if (body instanceof IpcBodySender) {
      IpcBodySender.$usableByIpc(ipc2, body);
    }
  }
  get ipcHeaders() {
    return __privateGet(this, _ipcHeaders) ?? __privateSet(this, _ipcHeaders, new IpcHeaders(this.headers));
  }
  toResponse(url) {
    const body = this.body.raw;
    if (body instanceof Uint8Array) {
      this.headers.init("Content-Length", body.length + "");
    }
    const response = new Response(body, {
      headers: this.headers,
      status: this.statusCode
    });
    if (url) {
      Object.defineProperty(response, "url", {
        value: url,
        enumerable: true,
        configurable: true,
        writable: false
      });
    }
    return response;
  }
  /** 将 response 对象进行转码变成 ipcResponse */
  static async fromResponse(req_id, response, ipc2, asBinary = false) {
    if (response.bodyUsed) {
      throw new Error("body used");
    }
    let ipcBody;
    if (asBinary || response.body == void 0) {
      ipcBody = IpcBodySender.from(
        binaryToU8a(await response.arrayBuffer()),
        ipc2
      );
    } else {
      setStreamId(response.body, response.url);
      ipcBody = IpcBodySender.from(response.body, ipc2);
    }
    return new _IpcResponse(
      req_id,
      response.status,
      new IpcHeaders(response.headers),
      ipcBody,
      ipc2
    );
  }
  static fromJson(req_id, statusCode, headers = new IpcHeaders(), jsonable, ipc2) {
    headers.init("Content-Type", "application/json");
    return this.fromText(
      req_id,
      statusCode,
      headers,
      JSON.stringify(jsonable),
      ipc2
    );
  }
  static fromText(req_id, statusCode, headers = new IpcHeaders(), text, ipc2) {
    headers.init("Content-Type", "text/plain");
    return new _IpcResponse(
      req_id,
      statusCode,
      headers,
      IpcBodySender.from(text, ipc2),
      ipc2
    );
  }
  static fromBinary(req_id, statusCode, headers = new IpcHeaders(), binary, ipc2) {
    headers.init("Content-Type", "application/octet-stream");
    headers.init("Content-Length", binary.byteLength + "");
    return new _IpcResponse(
      req_id,
      statusCode,
      headers,
      IpcBodySender.from(binaryToU8a(binary), ipc2),
      ipc2
    );
  }
  static fromStream(req_id, statusCode, headers = new IpcHeaders(), stream, ipc2) {
    headers.init("Content-Type", "application/octet-stream");
    const ipcResponse = new _IpcResponse(
      req_id,
      statusCode,
      headers,
      IpcBodySender.from(stream, ipc2),
      ipc2
    );
    return ipcResponse;
  }
  toJSON() {
    return this.ipcResMessage();
  }
};
var IpcResponse = _IpcResponse;
_ipcHeaders = new WeakMap();
var IpcResMessage = class extends IpcMessage {
  constructor(req_id, statusCode, headers, metaBody) {
    super(1 /* RESPONSE */);
    this.req_id = req_id;
    this.statusCode = statusCode;
    this.headers = headers;
    this.metaBody = metaBody;
  }
};

// src/core/ipc/IpcStreamPaused.cts
var IpcStreamPaused = class extends IpcMessage {
  constructor(stream_id, fuse) {
    super(4 /* STREAM_PAUSED */);
    this.stream_id = stream_id;
    this.fuse = fuse ?? 1;
  }
};

// src/core/ipc/IpcEvent.cts
var _IpcEvent = class extends IpcMessage {
  constructor(name, data, encoding) {
    super(7 /* EVENT */);
    this.name = name;
    this.data = data;
    this.encoding = encoding;
  }
  static fromBase64(name, data) {
    return new _IpcEvent(
      name,
      simpleDecoder(data, "base64"),
      4 /* BASE64 */
    );
  }
  static fromBinary(name, data) {
    return new _IpcEvent(name, data, 8 /* BINARY */);
  }
  static fromUtf8(name, data) {
    return new _IpcEvent(
      name,
      simpleDecoder(data, "utf8"),
      2 /* UTF8 */
    );
  }
  static fromText(name, data) {
    return new _IpcEvent(name, data, 2 /* UTF8 */);
  }
  get binary() {
    return $dataToBinary(this.data, this.encoding);
  }
  get text() {
    return $dataToText(this.data, this.encoding);
  }
  get jsonAble() {
    if (this.encoding === 8 /* BINARY */) {
      return _IpcEvent.fromBase64(this.name, this.data);
    }
    return this;
  }
  toJSON() {
    return { ...this.jsonAble };
  }
};
var IpcEvent = _IpcEvent;
__decorateClass([
  cacheGetter()
], IpcEvent.prototype, "binary", 1);
__decorateClass([
  cacheGetter()
], IpcEvent.prototype, "text", 1);
__decorateClass([
  cacheGetter()
], IpcEvent.prototype, "jsonAble", 1);

// src/helper/mapHelper.cts
var mapHelper = new class {
  getOrPut(map, key, putter) {
    if (map.has(key)) {
      return map.get(key);
    }
    const put = putter(key);
    map.set(key, put);
    return put;
  }
}();

// node_modules/.pnpm/@msgpack+msgpack@2.8.0/node_modules/@msgpack/msgpack/dist.es5+esm/utils/int.mjs
var UINT32_MAX = 4294967295;
function setUint64(view, offset, value) {
  var high = value / 4294967296;
  var low = value;
  view.setUint32(offset, high);
  view.setUint32(offset + 4, low);
}
function setInt64(view, offset, value) {
  var high = Math.floor(value / 4294967296);
  var low = value;
  view.setUint32(offset, high);
  view.setUint32(offset + 4, low);
}
function getInt64(view, offset) {
  var high = view.getInt32(offset);
  var low = view.getUint32(offset + 4);
  return high * 4294967296 + low;
}
function getUint64(view, offset) {
  var high = view.getUint32(offset);
  var low = view.getUint32(offset + 4);
  return high * 4294967296 + low;
}

// node_modules/.pnpm/@msgpack+msgpack@2.8.0/node_modules/@msgpack/msgpack/dist.es5+esm/utils/utf8.mjs
var _a;
var _b;
var _c;
var TEXT_ENCODING_AVAILABLE = (typeof process === "undefined" || ((_a = process === null || process === void 0 ? void 0 : process.env) === null || _a === void 0 ? void 0 : _a["TEXT_ENCODING"]) !== "never") && typeof TextEncoder !== "undefined" && typeof TextDecoder !== "undefined";
function utf8Count(str) {
  var strLength = str.length;
  var byteLength = 0;
  var pos = 0;
  while (pos < strLength) {
    var value = str.charCodeAt(pos++);
    if ((value & 4294967168) === 0) {
      byteLength++;
      continue;
    } else if ((value & 4294965248) === 0) {
      byteLength += 2;
    } else {
      if (value >= 55296 && value <= 56319) {
        if (pos < strLength) {
          var extra = str.charCodeAt(pos);
          if ((extra & 64512) === 56320) {
            ++pos;
            value = ((value & 1023) << 10) + (extra & 1023) + 65536;
          }
        }
      }
      if ((value & 4294901760) === 0) {
        byteLength += 3;
      } else {
        byteLength += 4;
      }
    }
  }
  return byteLength;
}
function utf8EncodeJs(str, output, outputOffset) {
  var strLength = str.length;
  var offset = outputOffset;
  var pos = 0;
  while (pos < strLength) {
    var value = str.charCodeAt(pos++);
    if ((value & 4294967168) === 0) {
      output[offset++] = value;
      continue;
    } else if ((value & 4294965248) === 0) {
      output[offset++] = value >> 6 & 31 | 192;
    } else {
      if (value >= 55296 && value <= 56319) {
        if (pos < strLength) {
          var extra = str.charCodeAt(pos);
          if ((extra & 64512) === 56320) {
            ++pos;
            value = ((value & 1023) << 10) + (extra & 1023) + 65536;
          }
        }
      }
      if ((value & 4294901760) === 0) {
        output[offset++] = value >> 12 & 15 | 224;
        output[offset++] = value >> 6 & 63 | 128;
      } else {
        output[offset++] = value >> 18 & 7 | 240;
        output[offset++] = value >> 12 & 63 | 128;
        output[offset++] = value >> 6 & 63 | 128;
      }
    }
    output[offset++] = value & 63 | 128;
  }
}
var sharedTextEncoder = TEXT_ENCODING_AVAILABLE ? new TextEncoder() : void 0;
var TEXT_ENCODER_THRESHOLD = !TEXT_ENCODING_AVAILABLE ? UINT32_MAX : typeof process !== "undefined" && ((_b = process === null || process === void 0 ? void 0 : process.env) === null || _b === void 0 ? void 0 : _b["TEXT_ENCODING"]) !== "force" ? 200 : 0;
function utf8EncodeTEencode(str, output, outputOffset) {
  output.set(sharedTextEncoder.encode(str), outputOffset);
}
function utf8EncodeTEencodeInto(str, output, outputOffset) {
  sharedTextEncoder.encodeInto(str, output.subarray(outputOffset));
}
var utf8EncodeTE = (sharedTextEncoder === null || sharedTextEncoder === void 0 ? void 0 : sharedTextEncoder.encodeInto) ? utf8EncodeTEencodeInto : utf8EncodeTEencode;
var CHUNK_SIZE = 4096;
function utf8DecodeJs(bytes, inputOffset, byteLength) {
  var offset = inputOffset;
  var end = offset + byteLength;
  var units = [];
  var result = "";
  while (offset < end) {
    var byte1 = bytes[offset++];
    if ((byte1 & 128) === 0) {
      units.push(byte1);
    } else if ((byte1 & 224) === 192) {
      var byte2 = bytes[offset++] & 63;
      units.push((byte1 & 31) << 6 | byte2);
    } else if ((byte1 & 240) === 224) {
      var byte2 = bytes[offset++] & 63;
      var byte3 = bytes[offset++] & 63;
      units.push((byte1 & 31) << 12 | byte2 << 6 | byte3);
    } else if ((byte1 & 248) === 240) {
      var byte2 = bytes[offset++] & 63;
      var byte3 = bytes[offset++] & 63;
      var byte4 = bytes[offset++] & 63;
      var unit = (byte1 & 7) << 18 | byte2 << 12 | byte3 << 6 | byte4;
      if (unit > 65535) {
        unit -= 65536;
        units.push(unit >>> 10 & 1023 | 55296);
        unit = 56320 | unit & 1023;
      }
      units.push(unit);
    } else {
      units.push(byte1);
    }
    if (units.length >= CHUNK_SIZE) {
      result += String.fromCharCode.apply(String, units);
      units.length = 0;
    }
  }
  if (units.length > 0) {
    result += String.fromCharCode.apply(String, units);
  }
  return result;
}
var sharedTextDecoder = TEXT_ENCODING_AVAILABLE ? new TextDecoder() : null;
var TEXT_DECODER_THRESHOLD = !TEXT_ENCODING_AVAILABLE ? UINT32_MAX : typeof process !== "undefined" && ((_c = process === null || process === void 0 ? void 0 : process.env) === null || _c === void 0 ? void 0 : _c["TEXT_DECODER"]) !== "force" ? 200 : 0;
function utf8DecodeTD(bytes, inputOffset, byteLength) {
  var stringBytes = bytes.subarray(inputOffset, inputOffset + byteLength);
  return sharedTextDecoder.decode(stringBytes);
}

// node_modules/.pnpm/@msgpack+msgpack@2.8.0/node_modules/@msgpack/msgpack/dist.es5+esm/ExtData.mjs
var ExtData = (
  /** @class */
  function() {
    function ExtData2(type, data) {
      this.type = type;
      this.data = data;
    }
    return ExtData2;
  }()
);

// node_modules/.pnpm/@msgpack+msgpack@2.8.0/node_modules/@msgpack/msgpack/dist.es5+esm/DecodeError.mjs
var __extends = function() {
  var extendStatics = function(d, b) {
    extendStatics = Object.setPrototypeOf || { __proto__: [] } instanceof Array && function(d2, b2) {
      d2.__proto__ = b2;
    } || function(d2, b2) {
      for (var p in b2)
        if (Object.prototype.hasOwnProperty.call(b2, p))
          d2[p] = b2[p];
    };
    return extendStatics(d, b);
  };
  return function(d, b) {
    if (typeof b !== "function" && b !== null)
      throw new TypeError("Class extends value " + String(b) + " is not a constructor or null");
    extendStatics(d, b);
    function __() {
      this.constructor = d;
    }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
  };
}();
var DecodeError = (
  /** @class */
  function(_super) {
    __extends(DecodeError2, _super);
    function DecodeError2(message) {
      var _this = _super.call(this, message) || this;
      var proto = Object.create(DecodeError2.prototype);
      Object.setPrototypeOf(_this, proto);
      Object.defineProperty(_this, "name", {
        configurable: true,
        enumerable: false,
        value: DecodeError2.name
      });
      return _this;
    }
    return DecodeError2;
  }(Error)
);

// node_modules/.pnpm/@msgpack+msgpack@2.8.0/node_modules/@msgpack/msgpack/dist.es5+esm/timestamp.mjs
var EXT_TIMESTAMP = -1;
var TIMESTAMP32_MAX_SEC = 4294967296 - 1;
var TIMESTAMP64_MAX_SEC = 17179869184 - 1;
function encodeTimeSpecToTimestamp(_a2) {
  var sec = _a2.sec, nsec = _a2.nsec;
  if (sec >= 0 && nsec >= 0 && sec <= TIMESTAMP64_MAX_SEC) {
    if (nsec === 0 && sec <= TIMESTAMP32_MAX_SEC) {
      var rv = new Uint8Array(4);
      var view = new DataView(rv.buffer);
      view.setUint32(0, sec);
      return rv;
    } else {
      var secHigh = sec / 4294967296;
      var secLow = sec & 4294967295;
      var rv = new Uint8Array(8);
      var view = new DataView(rv.buffer);
      view.setUint32(0, nsec << 2 | secHigh & 3);
      view.setUint32(4, secLow);
      return rv;
    }
  } else {
    var rv = new Uint8Array(12);
    var view = new DataView(rv.buffer);
    view.setUint32(0, nsec);
    setInt64(view, 4, sec);
    return rv;
  }
}
function encodeDateToTimeSpec(date) {
  var msec = date.getTime();
  var sec = Math.floor(msec / 1e3);
  var nsec = (msec - sec * 1e3) * 1e6;
  var nsecInSec = Math.floor(nsec / 1e9);
  return {
    sec: sec + nsecInSec,
    nsec: nsec - nsecInSec * 1e9
  };
}
function encodeTimestampExtension(object) {
  if (object instanceof Date) {
    var timeSpec = encodeDateToTimeSpec(object);
    return encodeTimeSpecToTimestamp(timeSpec);
  } else {
    return null;
  }
}
function decodeTimestampToTimeSpec(data) {
  var view = new DataView(data.buffer, data.byteOffset, data.byteLength);
  switch (data.byteLength) {
    case 4: {
      var sec = view.getUint32(0);
      var nsec = 0;
      return { sec, nsec };
    }
    case 8: {
      var nsec30AndSecHigh2 = view.getUint32(0);
      var secLow32 = view.getUint32(4);
      var sec = (nsec30AndSecHigh2 & 3) * 4294967296 + secLow32;
      var nsec = nsec30AndSecHigh2 >>> 2;
      return { sec, nsec };
    }
    case 12: {
      var sec = getInt64(view, 4);
      var nsec = view.getUint32(0);
      return { sec, nsec };
    }
    default:
      throw new DecodeError("Unrecognized data size for timestamp (expected 4, 8, or 12): ".concat(data.length));
  }
}
function decodeTimestampExtension(data) {
  var timeSpec = decodeTimestampToTimeSpec(data);
  return new Date(timeSpec.sec * 1e3 + timeSpec.nsec / 1e6);
}
var timestampExtension = {
  type: EXT_TIMESTAMP,
  encode: encodeTimestampExtension,
  decode: decodeTimestampExtension
};

// node_modules/.pnpm/@msgpack+msgpack@2.8.0/node_modules/@msgpack/msgpack/dist.es5+esm/ExtensionCodec.mjs
var ExtensionCodec = (
  /** @class */
  function() {
    function ExtensionCodec2() {
      this.builtInEncoders = [];
      this.builtInDecoders = [];
      this.encoders = [];
      this.decoders = [];
      this.register(timestampExtension);
    }
    ExtensionCodec2.prototype.register = function(_a2) {
      var type = _a2.type, encode2 = _a2.encode, decode2 = _a2.decode;
      if (type >= 0) {
        this.encoders[type] = encode2;
        this.decoders[type] = decode2;
      } else {
        var index = 1 + type;
        this.builtInEncoders[index] = encode2;
        this.builtInDecoders[index] = decode2;
      }
    };
    ExtensionCodec2.prototype.tryToEncode = function(object, context) {
      for (var i = 0; i < this.builtInEncoders.length; i++) {
        var encodeExt = this.builtInEncoders[i];
        if (encodeExt != null) {
          var data = encodeExt(object, context);
          if (data != null) {
            var type = -1 - i;
            return new ExtData(type, data);
          }
        }
      }
      for (var i = 0; i < this.encoders.length; i++) {
        var encodeExt = this.encoders[i];
        if (encodeExt != null) {
          var data = encodeExt(object, context);
          if (data != null) {
            var type = i;
            return new ExtData(type, data);
          }
        }
      }
      if (object instanceof ExtData) {
        return object;
      }
      return null;
    };
    ExtensionCodec2.prototype.decode = function(data, type, context) {
      var decodeExt = type < 0 ? this.builtInDecoders[-1 - type] : this.decoders[type];
      if (decodeExt) {
        return decodeExt(data, type, context);
      } else {
        return new ExtData(type, data);
      }
    };
    ExtensionCodec2.defaultCodec = new ExtensionCodec2();
    return ExtensionCodec2;
  }()
);

// node_modules/.pnpm/@msgpack+msgpack@2.8.0/node_modules/@msgpack/msgpack/dist.es5+esm/utils/typedArrays.mjs
function ensureUint8Array(buffer) {
  if (buffer instanceof Uint8Array) {
    return buffer;
  } else if (ArrayBuffer.isView(buffer)) {
    return new Uint8Array(buffer.buffer, buffer.byteOffset, buffer.byteLength);
  } else if (buffer instanceof ArrayBuffer) {
    return new Uint8Array(buffer);
  } else {
    return Uint8Array.from(buffer);
  }
}
function createDataView(buffer) {
  if (buffer instanceof ArrayBuffer) {
    return new DataView(buffer);
  }
  var bufferView = ensureUint8Array(buffer);
  return new DataView(bufferView.buffer, bufferView.byteOffset, bufferView.byteLength);
}

// node_modules/.pnpm/@msgpack+msgpack@2.8.0/node_modules/@msgpack/msgpack/dist.es5+esm/Encoder.mjs
var DEFAULT_MAX_DEPTH = 100;
var DEFAULT_INITIAL_BUFFER_SIZE = 2048;
var Encoder = (
  /** @class */
  function() {
    function Encoder2(extensionCodec, context, maxDepth, initialBufferSize, sortKeys, forceFloat32, ignoreUndefined, forceIntegerToFloat) {
      if (extensionCodec === void 0) {
        extensionCodec = ExtensionCodec.defaultCodec;
      }
      if (context === void 0) {
        context = void 0;
      }
      if (maxDepth === void 0) {
        maxDepth = DEFAULT_MAX_DEPTH;
      }
      if (initialBufferSize === void 0) {
        initialBufferSize = DEFAULT_INITIAL_BUFFER_SIZE;
      }
      if (sortKeys === void 0) {
        sortKeys = false;
      }
      if (forceFloat32 === void 0) {
        forceFloat32 = false;
      }
      if (ignoreUndefined === void 0) {
        ignoreUndefined = false;
      }
      if (forceIntegerToFloat === void 0) {
        forceIntegerToFloat = false;
      }
      this.extensionCodec = extensionCodec;
      this.context = context;
      this.maxDepth = maxDepth;
      this.initialBufferSize = initialBufferSize;
      this.sortKeys = sortKeys;
      this.forceFloat32 = forceFloat32;
      this.ignoreUndefined = ignoreUndefined;
      this.forceIntegerToFloat = forceIntegerToFloat;
      this.pos = 0;
      this.view = new DataView(new ArrayBuffer(this.initialBufferSize));
      this.bytes = new Uint8Array(this.view.buffer);
    }
    Encoder2.prototype.reinitializeState = function() {
      this.pos = 0;
    };
    Encoder2.prototype.encodeSharedRef = function(object) {
      this.reinitializeState();
      this.doEncode(object, 1);
      return this.bytes.subarray(0, this.pos);
    };
    Encoder2.prototype.encode = function(object) {
      this.reinitializeState();
      this.doEncode(object, 1);
      return this.bytes.slice(0, this.pos);
    };
    Encoder2.prototype.doEncode = function(object, depth) {
      if (depth > this.maxDepth) {
        throw new Error("Too deep objects in depth ".concat(depth));
      }
      if (object == null) {
        this.encodeNil();
      } else if (typeof object === "boolean") {
        this.encodeBoolean(object);
      } else if (typeof object === "number") {
        this.encodeNumber(object);
      } else if (typeof object === "string") {
        this.encodeString(object);
      } else {
        this.encodeObject(object, depth);
      }
    };
    Encoder2.prototype.ensureBufferSizeToWrite = function(sizeToWrite) {
      var requiredSize = this.pos + sizeToWrite;
      if (this.view.byteLength < requiredSize) {
        this.resizeBuffer(requiredSize * 2);
      }
    };
    Encoder2.prototype.resizeBuffer = function(newSize) {
      var newBuffer = new ArrayBuffer(newSize);
      var newBytes = new Uint8Array(newBuffer);
      var newView = new DataView(newBuffer);
      newBytes.set(this.bytes);
      this.view = newView;
      this.bytes = newBytes;
    };
    Encoder2.prototype.encodeNil = function() {
      this.writeU8(192);
    };
    Encoder2.prototype.encodeBoolean = function(object) {
      if (object === false) {
        this.writeU8(194);
      } else {
        this.writeU8(195);
      }
    };
    Encoder2.prototype.encodeNumber = function(object) {
      if (Number.isSafeInteger(object) && !this.forceIntegerToFloat) {
        if (object >= 0) {
          if (object < 128) {
            this.writeU8(object);
          } else if (object < 256) {
            this.writeU8(204);
            this.writeU8(object);
          } else if (object < 65536) {
            this.writeU8(205);
            this.writeU16(object);
          } else if (object < 4294967296) {
            this.writeU8(206);
            this.writeU32(object);
          } else {
            this.writeU8(207);
            this.writeU64(object);
          }
        } else {
          if (object >= -32) {
            this.writeU8(224 | object + 32);
          } else if (object >= -128) {
            this.writeU8(208);
            this.writeI8(object);
          } else if (object >= -32768) {
            this.writeU8(209);
            this.writeI16(object);
          } else if (object >= -2147483648) {
            this.writeU8(210);
            this.writeI32(object);
          } else {
            this.writeU8(211);
            this.writeI64(object);
          }
        }
      } else {
        if (this.forceFloat32) {
          this.writeU8(202);
          this.writeF32(object);
        } else {
          this.writeU8(203);
          this.writeF64(object);
        }
      }
    };
    Encoder2.prototype.writeStringHeader = function(byteLength) {
      if (byteLength < 32) {
        this.writeU8(160 + byteLength);
      } else if (byteLength < 256) {
        this.writeU8(217);
        this.writeU8(byteLength);
      } else if (byteLength < 65536) {
        this.writeU8(218);
        this.writeU16(byteLength);
      } else if (byteLength < 4294967296) {
        this.writeU8(219);
        this.writeU32(byteLength);
      } else {
        throw new Error("Too long string: ".concat(byteLength, " bytes in UTF-8"));
      }
    };
    Encoder2.prototype.encodeString = function(object) {
      var maxHeaderSize = 1 + 4;
      var strLength = object.length;
      if (strLength > TEXT_ENCODER_THRESHOLD) {
        var byteLength = utf8Count(object);
        this.ensureBufferSizeToWrite(maxHeaderSize + byteLength);
        this.writeStringHeader(byteLength);
        utf8EncodeTE(object, this.bytes, this.pos);
        this.pos += byteLength;
      } else {
        var byteLength = utf8Count(object);
        this.ensureBufferSizeToWrite(maxHeaderSize + byteLength);
        this.writeStringHeader(byteLength);
        utf8EncodeJs(object, this.bytes, this.pos);
        this.pos += byteLength;
      }
    };
    Encoder2.prototype.encodeObject = function(object, depth) {
      var ext = this.extensionCodec.tryToEncode(object, this.context);
      if (ext != null) {
        this.encodeExtension(ext);
      } else if (Array.isArray(object)) {
        this.encodeArray(object, depth);
      } else if (ArrayBuffer.isView(object)) {
        this.encodeBinary(object);
      } else if (typeof object === "object") {
        this.encodeMap(object, depth);
      } else {
        throw new Error("Unrecognized object: ".concat(Object.prototype.toString.apply(object)));
      }
    };
    Encoder2.prototype.encodeBinary = function(object) {
      var size = object.byteLength;
      if (size < 256) {
        this.writeU8(196);
        this.writeU8(size);
      } else if (size < 65536) {
        this.writeU8(197);
        this.writeU16(size);
      } else if (size < 4294967296) {
        this.writeU8(198);
        this.writeU32(size);
      } else {
        throw new Error("Too large binary: ".concat(size));
      }
      var bytes = ensureUint8Array(object);
      this.writeU8a(bytes);
    };
    Encoder2.prototype.encodeArray = function(object, depth) {
      var size = object.length;
      if (size < 16) {
        this.writeU8(144 + size);
      } else if (size < 65536) {
        this.writeU8(220);
        this.writeU16(size);
      } else if (size < 4294967296) {
        this.writeU8(221);
        this.writeU32(size);
      } else {
        throw new Error("Too large array: ".concat(size));
      }
      for (var _i = 0, object_1 = object; _i < object_1.length; _i++) {
        var item = object_1[_i];
        this.doEncode(item, depth + 1);
      }
    };
    Encoder2.prototype.countWithoutUndefined = function(object, keys) {
      var count = 0;
      for (var _i = 0, keys_1 = keys; _i < keys_1.length; _i++) {
        var key = keys_1[_i];
        if (object[key] !== void 0) {
          count++;
        }
      }
      return count;
    };
    Encoder2.prototype.encodeMap = function(object, depth) {
      var keys = Object.keys(object);
      if (this.sortKeys) {
        keys.sort();
      }
      var size = this.ignoreUndefined ? this.countWithoutUndefined(object, keys) : keys.length;
      if (size < 16) {
        this.writeU8(128 + size);
      } else if (size < 65536) {
        this.writeU8(222);
        this.writeU16(size);
      } else if (size < 4294967296) {
        this.writeU8(223);
        this.writeU32(size);
      } else {
        throw new Error("Too large map object: ".concat(size));
      }
      for (var _i = 0, keys_2 = keys; _i < keys_2.length; _i++) {
        var key = keys_2[_i];
        var value = object[key];
        if (!(this.ignoreUndefined && value === void 0)) {
          this.encodeString(key);
          this.doEncode(value, depth + 1);
        }
      }
    };
    Encoder2.prototype.encodeExtension = function(ext) {
      var size = ext.data.length;
      if (size === 1) {
        this.writeU8(212);
      } else if (size === 2) {
        this.writeU8(213);
      } else if (size === 4) {
        this.writeU8(214);
      } else if (size === 8) {
        this.writeU8(215);
      } else if (size === 16) {
        this.writeU8(216);
      } else if (size < 256) {
        this.writeU8(199);
        this.writeU8(size);
      } else if (size < 65536) {
        this.writeU8(200);
        this.writeU16(size);
      } else if (size < 4294967296) {
        this.writeU8(201);
        this.writeU32(size);
      } else {
        throw new Error("Too large extension object: ".concat(size));
      }
      this.writeI8(ext.type);
      this.writeU8a(ext.data);
    };
    Encoder2.prototype.writeU8 = function(value) {
      this.ensureBufferSizeToWrite(1);
      this.view.setUint8(this.pos, value);
      this.pos++;
    };
    Encoder2.prototype.writeU8a = function(values) {
      var size = values.length;
      this.ensureBufferSizeToWrite(size);
      this.bytes.set(values, this.pos);
      this.pos += size;
    };
    Encoder2.prototype.writeI8 = function(value) {
      this.ensureBufferSizeToWrite(1);
      this.view.setInt8(this.pos, value);
      this.pos++;
    };
    Encoder2.prototype.writeU16 = function(value) {
      this.ensureBufferSizeToWrite(2);
      this.view.setUint16(this.pos, value);
      this.pos += 2;
    };
    Encoder2.prototype.writeI16 = function(value) {
      this.ensureBufferSizeToWrite(2);
      this.view.setInt16(this.pos, value);
      this.pos += 2;
    };
    Encoder2.prototype.writeU32 = function(value) {
      this.ensureBufferSizeToWrite(4);
      this.view.setUint32(this.pos, value);
      this.pos += 4;
    };
    Encoder2.prototype.writeI32 = function(value) {
      this.ensureBufferSizeToWrite(4);
      this.view.setInt32(this.pos, value);
      this.pos += 4;
    };
    Encoder2.prototype.writeF32 = function(value) {
      this.ensureBufferSizeToWrite(4);
      this.view.setFloat32(this.pos, value);
      this.pos += 4;
    };
    Encoder2.prototype.writeF64 = function(value) {
      this.ensureBufferSizeToWrite(8);
      this.view.setFloat64(this.pos, value);
      this.pos += 8;
    };
    Encoder2.prototype.writeU64 = function(value) {
      this.ensureBufferSizeToWrite(8);
      setUint64(this.view, this.pos, value);
      this.pos += 8;
    };
    Encoder2.prototype.writeI64 = function(value) {
      this.ensureBufferSizeToWrite(8);
      setInt64(this.view, this.pos, value);
      this.pos += 8;
    };
    return Encoder2;
  }()
);

// node_modules/.pnpm/@msgpack+msgpack@2.8.0/node_modules/@msgpack/msgpack/dist.es5+esm/encode.mjs
var defaultEncodeOptions = {};
function encode(value, options) {
  if (options === void 0) {
    options = defaultEncodeOptions;
  }
  var encoder = new Encoder(options.extensionCodec, options.context, options.maxDepth, options.initialBufferSize, options.sortKeys, options.forceFloat32, options.ignoreUndefined, options.forceIntegerToFloat);
  return encoder.encodeSharedRef(value);
}

// node_modules/.pnpm/@msgpack+msgpack@2.8.0/node_modules/@msgpack/msgpack/dist.es5+esm/utils/prettyByte.mjs
function prettyByte(byte) {
  return "".concat(byte < 0 ? "-" : "", "0x").concat(Math.abs(byte).toString(16).padStart(2, "0"));
}

// node_modules/.pnpm/@msgpack+msgpack@2.8.0/node_modules/@msgpack/msgpack/dist.es5+esm/CachedKeyDecoder.mjs
var DEFAULT_MAX_KEY_LENGTH = 16;
var DEFAULT_MAX_LENGTH_PER_KEY = 16;
var CachedKeyDecoder = (
  /** @class */
  function() {
    function CachedKeyDecoder2(maxKeyLength, maxLengthPerKey) {
      if (maxKeyLength === void 0) {
        maxKeyLength = DEFAULT_MAX_KEY_LENGTH;
      }
      if (maxLengthPerKey === void 0) {
        maxLengthPerKey = DEFAULT_MAX_LENGTH_PER_KEY;
      }
      this.maxKeyLength = maxKeyLength;
      this.maxLengthPerKey = maxLengthPerKey;
      this.hit = 0;
      this.miss = 0;
      this.caches = [];
      for (var i = 0; i < this.maxKeyLength; i++) {
        this.caches.push([]);
      }
    }
    CachedKeyDecoder2.prototype.canBeCached = function(byteLength) {
      return byteLength > 0 && byteLength <= this.maxKeyLength;
    };
    CachedKeyDecoder2.prototype.find = function(bytes, inputOffset, byteLength) {
      var records = this.caches[byteLength - 1];
      FIND_CHUNK:
        for (var _i = 0, records_1 = records; _i < records_1.length; _i++) {
          var record = records_1[_i];
          var recordBytes = record.bytes;
          for (var j = 0; j < byteLength; j++) {
            if (recordBytes[j] !== bytes[inputOffset + j]) {
              continue FIND_CHUNK;
            }
          }
          return record.str;
        }
      return null;
    };
    CachedKeyDecoder2.prototype.store = function(bytes, value) {
      var records = this.caches[bytes.length - 1];
      var record = { bytes, str: value };
      if (records.length >= this.maxLengthPerKey) {
        records[Math.random() * records.length | 0] = record;
      } else {
        records.push(record);
      }
    };
    CachedKeyDecoder2.prototype.decode = function(bytes, inputOffset, byteLength) {
      var cachedValue = this.find(bytes, inputOffset, byteLength);
      if (cachedValue != null) {
        this.hit++;
        return cachedValue;
      }
      this.miss++;
      var str = utf8DecodeJs(bytes, inputOffset, byteLength);
      var slicedCopyOfBytes = Uint8Array.prototype.slice.call(bytes, inputOffset, inputOffset + byteLength);
      this.store(slicedCopyOfBytes, str);
      return str;
    };
    return CachedKeyDecoder2;
  }()
);

// node_modules/.pnpm/@msgpack+msgpack@2.8.0/node_modules/@msgpack/msgpack/dist.es5+esm/Decoder.mjs
var __awaiter = function(thisArg, _arguments, P, generator) {
  function adopt(value) {
    return value instanceof P ? value : new P(function(resolve) {
      resolve(value);
    });
  }
  return new (P || (P = Promise))(function(resolve, reject) {
    function fulfilled(value) {
      try {
        step(generator.next(value));
      } catch (e) {
        reject(e);
      }
    }
    function rejected(value) {
      try {
        step(generator["throw"](value));
      } catch (e) {
        reject(e);
      }
    }
    function step(result) {
      result.done ? resolve(result.value) : adopt(result.value).then(fulfilled, rejected);
    }
    step((generator = generator.apply(thisArg, _arguments || [])).next());
  });
};
var __generator = function(thisArg, body) {
  var _ = { label: 0, sent: function() {
    if (t[0] & 1)
      throw t[1];
    return t[1];
  }, trys: [], ops: [] }, f, y, t, g;
  return g = { next: verb(0), "throw": verb(1), "return": verb(2) }, typeof Symbol === "function" && (g[Symbol.iterator] = function() {
    return this;
  }), g;
  function verb(n) {
    return function(v) {
      return step([n, v]);
    };
  }
  function step(op) {
    if (f)
      throw new TypeError("Generator is already executing.");
    while (_)
      try {
        if (f = 1, y && (t = op[0] & 2 ? y["return"] : op[0] ? y["throw"] || ((t = y["return"]) && t.call(y), 0) : y.next) && !(t = t.call(y, op[1])).done)
          return t;
        if (y = 0, t)
          op = [op[0] & 2, t.value];
        switch (op[0]) {
          case 0:
          case 1:
            t = op;
            break;
          case 4:
            _.label++;
            return { value: op[1], done: false };
          case 5:
            _.label++;
            y = op[1];
            op = [0];
            continue;
          case 7:
            op = _.ops.pop();
            _.trys.pop();
            continue;
          default:
            if (!(t = _.trys, t = t.length > 0 && t[t.length - 1]) && (op[0] === 6 || op[0] === 2)) {
              _ = 0;
              continue;
            }
            if (op[0] === 3 && (!t || op[1] > t[0] && op[1] < t[3])) {
              _.label = op[1];
              break;
            }
            if (op[0] === 6 && _.label < t[1]) {
              _.label = t[1];
              t = op;
              break;
            }
            if (t && _.label < t[2]) {
              _.label = t[2];
              _.ops.push(op);
              break;
            }
            if (t[2])
              _.ops.pop();
            _.trys.pop();
            continue;
        }
        op = body.call(thisArg, _);
      } catch (e) {
        op = [6, e];
        y = 0;
      } finally {
        f = t = 0;
      }
    if (op[0] & 5)
      throw op[1];
    return { value: op[0] ? op[1] : void 0, done: true };
  }
};
var __asyncValues = function(o) {
  if (!Symbol.asyncIterator)
    throw new TypeError("Symbol.asyncIterator is not defined.");
  var m = o[Symbol.asyncIterator], i;
  return m ? m.call(o) : (o = typeof __values === "function" ? __values(o) : o[Symbol.iterator](), i = {}, verb("next"), verb("throw"), verb("return"), i[Symbol.asyncIterator] = function() {
    return this;
  }, i);
  function verb(n) {
    i[n] = o[n] && function(v) {
      return new Promise(function(resolve, reject) {
        v = o[n](v), settle(resolve, reject, v.done, v.value);
      });
    };
  }
  function settle(resolve, reject, d, v) {
    Promise.resolve(v).then(function(v2) {
      resolve({ value: v2, done: d });
    }, reject);
  }
};
var __await = function(v) {
  return this instanceof __await ? (this.v = v, this) : new __await(v);
};
var __asyncGenerator = function(thisArg, _arguments, generator) {
  if (!Symbol.asyncIterator)
    throw new TypeError("Symbol.asyncIterator is not defined.");
  var g = generator.apply(thisArg, _arguments || []), i, q = [];
  return i = {}, verb("next"), verb("throw"), verb("return"), i[Symbol.asyncIterator] = function() {
    return this;
  }, i;
  function verb(n) {
    if (g[n])
      i[n] = function(v) {
        return new Promise(function(a, b) {
          q.push([n, v, a, b]) > 1 || resume(n, v);
        });
      };
  }
  function resume(n, v) {
    try {
      step(g[n](v));
    } catch (e) {
      settle(q[0][3], e);
    }
  }
  function step(r) {
    r.value instanceof __await ? Promise.resolve(r.value.v).then(fulfill, reject) : settle(q[0][2], r);
  }
  function fulfill(value) {
    resume("next", value);
  }
  function reject(value) {
    resume("throw", value);
  }
  function settle(f, v) {
    if (f(v), q.shift(), q.length)
      resume(q[0][0], q[0][1]);
  }
};
var isValidMapKeyType = function(key) {
  var keyType = typeof key;
  return keyType === "string" || keyType === "number";
};
var HEAD_BYTE_REQUIRED = -1;
var EMPTY_VIEW = new DataView(new ArrayBuffer(0));
var EMPTY_BYTES = new Uint8Array(EMPTY_VIEW.buffer);
var DataViewIndexOutOfBoundsError = function() {
  try {
    EMPTY_VIEW.getInt8(0);
  } catch (e) {
    return e.constructor;
  }
  throw new Error("never reached");
}();
var MORE_DATA = new DataViewIndexOutOfBoundsError("Insufficient data");
var sharedCachedKeyDecoder = new CachedKeyDecoder();
var Decoder = (
  /** @class */
  function() {
    function Decoder2(extensionCodec, context, maxStrLength, maxBinLength, maxArrayLength, maxMapLength, maxExtLength, keyDecoder) {
      if (extensionCodec === void 0) {
        extensionCodec = ExtensionCodec.defaultCodec;
      }
      if (context === void 0) {
        context = void 0;
      }
      if (maxStrLength === void 0) {
        maxStrLength = UINT32_MAX;
      }
      if (maxBinLength === void 0) {
        maxBinLength = UINT32_MAX;
      }
      if (maxArrayLength === void 0) {
        maxArrayLength = UINT32_MAX;
      }
      if (maxMapLength === void 0) {
        maxMapLength = UINT32_MAX;
      }
      if (maxExtLength === void 0) {
        maxExtLength = UINT32_MAX;
      }
      if (keyDecoder === void 0) {
        keyDecoder = sharedCachedKeyDecoder;
      }
      this.extensionCodec = extensionCodec;
      this.context = context;
      this.maxStrLength = maxStrLength;
      this.maxBinLength = maxBinLength;
      this.maxArrayLength = maxArrayLength;
      this.maxMapLength = maxMapLength;
      this.maxExtLength = maxExtLength;
      this.keyDecoder = keyDecoder;
      this.totalPos = 0;
      this.pos = 0;
      this.view = EMPTY_VIEW;
      this.bytes = EMPTY_BYTES;
      this.headByte = HEAD_BYTE_REQUIRED;
      this.stack = [];
    }
    Decoder2.prototype.reinitializeState = function() {
      this.totalPos = 0;
      this.headByte = HEAD_BYTE_REQUIRED;
      this.stack.length = 0;
    };
    Decoder2.prototype.setBuffer = function(buffer) {
      this.bytes = ensureUint8Array(buffer);
      this.view = createDataView(this.bytes);
      this.pos = 0;
    };
    Decoder2.prototype.appendBuffer = function(buffer) {
      if (this.headByte === HEAD_BYTE_REQUIRED && !this.hasRemaining(1)) {
        this.setBuffer(buffer);
      } else {
        var remainingData = this.bytes.subarray(this.pos);
        var newData = ensureUint8Array(buffer);
        var newBuffer = new Uint8Array(remainingData.length + newData.length);
        newBuffer.set(remainingData);
        newBuffer.set(newData, remainingData.length);
        this.setBuffer(newBuffer);
      }
    };
    Decoder2.prototype.hasRemaining = function(size) {
      return this.view.byteLength - this.pos >= size;
    };
    Decoder2.prototype.createExtraByteError = function(posToShow) {
      var _a2 = this, view = _a2.view, pos = _a2.pos;
      return new RangeError("Extra ".concat(view.byteLength - pos, " of ").concat(view.byteLength, " byte(s) found at buffer[").concat(posToShow, "]"));
    };
    Decoder2.prototype.decode = function(buffer) {
      this.reinitializeState();
      this.setBuffer(buffer);
      var object = this.doDecodeSync();
      if (this.hasRemaining(1)) {
        throw this.createExtraByteError(this.pos);
      }
      return object;
    };
    Decoder2.prototype.decodeMulti = function(buffer) {
      return __generator(this, function(_a2) {
        switch (_a2.label) {
          case 0:
            this.reinitializeState();
            this.setBuffer(buffer);
            _a2.label = 1;
          case 1:
            if (!this.hasRemaining(1))
              return [3, 3];
            return [4, this.doDecodeSync()];
          case 2:
            _a2.sent();
            return [3, 1];
          case 3:
            return [
              2
              /*return*/
            ];
        }
      });
    };
    Decoder2.prototype.decodeAsync = function(stream) {
      var stream_1, stream_1_1;
      var e_1, _a2;
      return __awaiter(this, void 0, void 0, function() {
        var decoded, object, buffer, e_1_1, _b2, headByte, pos, totalPos;
        return __generator(this, function(_c2) {
          switch (_c2.label) {
            case 0:
              decoded = false;
              _c2.label = 1;
            case 1:
              _c2.trys.push([1, 6, 7, 12]);
              stream_1 = __asyncValues(stream);
              _c2.label = 2;
            case 2:
              return [4, stream_1.next()];
            case 3:
              if (!(stream_1_1 = _c2.sent(), !stream_1_1.done))
                return [3, 5];
              buffer = stream_1_1.value;
              if (decoded) {
                throw this.createExtraByteError(this.totalPos);
              }
              this.appendBuffer(buffer);
              try {
                object = this.doDecodeSync();
                decoded = true;
              } catch (e) {
                if (!(e instanceof DataViewIndexOutOfBoundsError)) {
                  throw e;
                }
              }
              this.totalPos += this.pos;
              _c2.label = 4;
            case 4:
              return [3, 2];
            case 5:
              return [3, 12];
            case 6:
              e_1_1 = _c2.sent();
              e_1 = { error: e_1_1 };
              return [3, 12];
            case 7:
              _c2.trys.push([7, , 10, 11]);
              if (!(stream_1_1 && !stream_1_1.done && (_a2 = stream_1.return)))
                return [3, 9];
              return [4, _a2.call(stream_1)];
            case 8:
              _c2.sent();
              _c2.label = 9;
            case 9:
              return [3, 11];
            case 10:
              if (e_1)
                throw e_1.error;
              return [
                7
                /*endfinally*/
              ];
            case 11:
              return [
                7
                /*endfinally*/
              ];
            case 12:
              if (decoded) {
                if (this.hasRemaining(1)) {
                  throw this.createExtraByteError(this.totalPos);
                }
                return [2, object];
              }
              _b2 = this, headByte = _b2.headByte, pos = _b2.pos, totalPos = _b2.totalPos;
              throw new RangeError("Insufficient data in parsing ".concat(prettyByte(headByte), " at ").concat(totalPos, " (").concat(pos, " in the current buffer)"));
          }
        });
      });
    };
    Decoder2.prototype.decodeArrayStream = function(stream) {
      return this.decodeMultiAsync(stream, true);
    };
    Decoder2.prototype.decodeStream = function(stream) {
      return this.decodeMultiAsync(stream, false);
    };
    Decoder2.prototype.decodeMultiAsync = function(stream, isArray) {
      return __asyncGenerator(this, arguments, function decodeMultiAsync_1() {
        var isArrayHeaderRequired, arrayItemsLeft, stream_2, stream_2_1, buffer, e_2, e_3_1;
        var e_3, _a2;
        return __generator(this, function(_b2) {
          switch (_b2.label) {
            case 0:
              isArrayHeaderRequired = isArray;
              arrayItemsLeft = -1;
              _b2.label = 1;
            case 1:
              _b2.trys.push([1, 13, 14, 19]);
              stream_2 = __asyncValues(stream);
              _b2.label = 2;
            case 2:
              return [4, __await(stream_2.next())];
            case 3:
              if (!(stream_2_1 = _b2.sent(), !stream_2_1.done))
                return [3, 12];
              buffer = stream_2_1.value;
              if (isArray && arrayItemsLeft === 0) {
                throw this.createExtraByteError(this.totalPos);
              }
              this.appendBuffer(buffer);
              if (isArrayHeaderRequired) {
                arrayItemsLeft = this.readArraySize();
                isArrayHeaderRequired = false;
                this.complete();
              }
              _b2.label = 4;
            case 4:
              _b2.trys.push([4, 9, , 10]);
              _b2.label = 5;
            case 5:
              if (false)
                return [3, 8];
              return [4, __await(this.doDecodeSync())];
            case 6:
              return [4, _b2.sent()];
            case 7:
              _b2.sent();
              if (--arrayItemsLeft === 0) {
                return [3, 8];
              }
              return [3, 5];
            case 8:
              return [3, 10];
            case 9:
              e_2 = _b2.sent();
              if (!(e_2 instanceof DataViewIndexOutOfBoundsError)) {
                throw e_2;
              }
              return [3, 10];
            case 10:
              this.totalPos += this.pos;
              _b2.label = 11;
            case 11:
              return [3, 2];
            case 12:
              return [3, 19];
            case 13:
              e_3_1 = _b2.sent();
              e_3 = { error: e_3_1 };
              return [3, 19];
            case 14:
              _b2.trys.push([14, , 17, 18]);
              if (!(stream_2_1 && !stream_2_1.done && (_a2 = stream_2.return)))
                return [3, 16];
              return [4, __await(_a2.call(stream_2))];
            case 15:
              _b2.sent();
              _b2.label = 16;
            case 16:
              return [3, 18];
            case 17:
              if (e_3)
                throw e_3.error;
              return [
                7
                /*endfinally*/
              ];
            case 18:
              return [
                7
                /*endfinally*/
              ];
            case 19:
              return [
                2
                /*return*/
              ];
          }
        });
      });
    };
    Decoder2.prototype.doDecodeSync = function() {
      DECODE:
        while (true) {
          var headByte = this.readHeadByte();
          var object = void 0;
          if (headByte >= 224) {
            object = headByte - 256;
          } else if (headByte < 192) {
            if (headByte < 128) {
              object = headByte;
            } else if (headByte < 144) {
              var size = headByte - 128;
              if (size !== 0) {
                this.pushMapState(size);
                this.complete();
                continue DECODE;
              } else {
                object = {};
              }
            } else if (headByte < 160) {
              var size = headByte - 144;
              if (size !== 0) {
                this.pushArrayState(size);
                this.complete();
                continue DECODE;
              } else {
                object = [];
              }
            } else {
              var byteLength = headByte - 160;
              object = this.decodeUtf8String(byteLength, 0);
            }
          } else if (headByte === 192) {
            object = null;
          } else if (headByte === 194) {
            object = false;
          } else if (headByte === 195) {
            object = true;
          } else if (headByte === 202) {
            object = this.readF32();
          } else if (headByte === 203) {
            object = this.readF64();
          } else if (headByte === 204) {
            object = this.readU8();
          } else if (headByte === 205) {
            object = this.readU16();
          } else if (headByte === 206) {
            object = this.readU32();
          } else if (headByte === 207) {
            object = this.readU64();
          } else if (headByte === 208) {
            object = this.readI8();
          } else if (headByte === 209) {
            object = this.readI16();
          } else if (headByte === 210) {
            object = this.readI32();
          } else if (headByte === 211) {
            object = this.readI64();
          } else if (headByte === 217) {
            var byteLength = this.lookU8();
            object = this.decodeUtf8String(byteLength, 1);
          } else if (headByte === 218) {
            var byteLength = this.lookU16();
            object = this.decodeUtf8String(byteLength, 2);
          } else if (headByte === 219) {
            var byteLength = this.lookU32();
            object = this.decodeUtf8String(byteLength, 4);
          } else if (headByte === 220) {
            var size = this.readU16();
            if (size !== 0) {
              this.pushArrayState(size);
              this.complete();
              continue DECODE;
            } else {
              object = [];
            }
          } else if (headByte === 221) {
            var size = this.readU32();
            if (size !== 0) {
              this.pushArrayState(size);
              this.complete();
              continue DECODE;
            } else {
              object = [];
            }
          } else if (headByte === 222) {
            var size = this.readU16();
            if (size !== 0) {
              this.pushMapState(size);
              this.complete();
              continue DECODE;
            } else {
              object = {};
            }
          } else if (headByte === 223) {
            var size = this.readU32();
            if (size !== 0) {
              this.pushMapState(size);
              this.complete();
              continue DECODE;
            } else {
              object = {};
            }
          } else if (headByte === 196) {
            var size = this.lookU8();
            object = this.decodeBinary(size, 1);
          } else if (headByte === 197) {
            var size = this.lookU16();
            object = this.decodeBinary(size, 2);
          } else if (headByte === 198) {
            var size = this.lookU32();
            object = this.decodeBinary(size, 4);
          } else if (headByte === 212) {
            object = this.decodeExtension(1, 0);
          } else if (headByte === 213) {
            object = this.decodeExtension(2, 0);
          } else if (headByte === 214) {
            object = this.decodeExtension(4, 0);
          } else if (headByte === 215) {
            object = this.decodeExtension(8, 0);
          } else if (headByte === 216) {
            object = this.decodeExtension(16, 0);
          } else if (headByte === 199) {
            var size = this.lookU8();
            object = this.decodeExtension(size, 1);
          } else if (headByte === 200) {
            var size = this.lookU16();
            object = this.decodeExtension(size, 2);
          } else if (headByte === 201) {
            var size = this.lookU32();
            object = this.decodeExtension(size, 4);
          } else {
            throw new DecodeError("Unrecognized type byte: ".concat(prettyByte(headByte)));
          }
          this.complete();
          var stack = this.stack;
          while (stack.length > 0) {
            var state = stack[stack.length - 1];
            if (state.type === 0) {
              state.array[state.position] = object;
              state.position++;
              if (state.position === state.size) {
                stack.pop();
                object = state.array;
              } else {
                continue DECODE;
              }
            } else if (state.type === 1) {
              if (!isValidMapKeyType(object)) {
                throw new DecodeError("The type of key must be string or number but " + typeof object);
              }
              if (object === "__proto__") {
                throw new DecodeError("The key __proto__ is not allowed");
              }
              state.key = object;
              state.type = 2;
              continue DECODE;
            } else {
              state.map[state.key] = object;
              state.readCount++;
              if (state.readCount === state.size) {
                stack.pop();
                object = state.map;
              } else {
                state.key = null;
                state.type = 1;
                continue DECODE;
              }
            }
          }
          return object;
        }
    };
    Decoder2.prototype.readHeadByte = function() {
      if (this.headByte === HEAD_BYTE_REQUIRED) {
        this.headByte = this.readU8();
      }
      return this.headByte;
    };
    Decoder2.prototype.complete = function() {
      this.headByte = HEAD_BYTE_REQUIRED;
    };
    Decoder2.prototype.readArraySize = function() {
      var headByte = this.readHeadByte();
      switch (headByte) {
        case 220:
          return this.readU16();
        case 221:
          return this.readU32();
        default: {
          if (headByte < 160) {
            return headByte - 144;
          } else {
            throw new DecodeError("Unrecognized array type byte: ".concat(prettyByte(headByte)));
          }
        }
      }
    };
    Decoder2.prototype.pushMapState = function(size) {
      if (size > this.maxMapLength) {
        throw new DecodeError("Max length exceeded: map length (".concat(size, ") > maxMapLengthLength (").concat(this.maxMapLength, ")"));
      }
      this.stack.push({
        type: 1,
        size,
        key: null,
        readCount: 0,
        map: {}
      });
    };
    Decoder2.prototype.pushArrayState = function(size) {
      if (size > this.maxArrayLength) {
        throw new DecodeError("Max length exceeded: array length (".concat(size, ") > maxArrayLength (").concat(this.maxArrayLength, ")"));
      }
      this.stack.push({
        type: 0,
        size,
        array: new Array(size),
        position: 0
      });
    };
    Decoder2.prototype.decodeUtf8String = function(byteLength, headerOffset) {
      var _a2;
      if (byteLength > this.maxStrLength) {
        throw new DecodeError("Max length exceeded: UTF-8 byte length (".concat(byteLength, ") > maxStrLength (").concat(this.maxStrLength, ")"));
      }
      if (this.bytes.byteLength < this.pos + headerOffset + byteLength) {
        throw MORE_DATA;
      }
      var offset = this.pos + headerOffset;
      var object;
      if (this.stateIsMapKey() && ((_a2 = this.keyDecoder) === null || _a2 === void 0 ? void 0 : _a2.canBeCached(byteLength))) {
        object = this.keyDecoder.decode(this.bytes, offset, byteLength);
      } else if (byteLength > TEXT_DECODER_THRESHOLD) {
        object = utf8DecodeTD(this.bytes, offset, byteLength);
      } else {
        object = utf8DecodeJs(this.bytes, offset, byteLength);
      }
      this.pos += headerOffset + byteLength;
      return object;
    };
    Decoder2.prototype.stateIsMapKey = function() {
      if (this.stack.length > 0) {
        var state = this.stack[this.stack.length - 1];
        return state.type === 1;
      }
      return false;
    };
    Decoder2.prototype.decodeBinary = function(byteLength, headOffset) {
      if (byteLength > this.maxBinLength) {
        throw new DecodeError("Max length exceeded: bin length (".concat(byteLength, ") > maxBinLength (").concat(this.maxBinLength, ")"));
      }
      if (!this.hasRemaining(byteLength + headOffset)) {
        throw MORE_DATA;
      }
      var offset = this.pos + headOffset;
      var object = this.bytes.subarray(offset, offset + byteLength);
      this.pos += headOffset + byteLength;
      return object;
    };
    Decoder2.prototype.decodeExtension = function(size, headOffset) {
      if (size > this.maxExtLength) {
        throw new DecodeError("Max length exceeded: ext length (".concat(size, ") > maxExtLength (").concat(this.maxExtLength, ")"));
      }
      var extType = this.view.getInt8(this.pos + headOffset);
      var data = this.decodeBinary(
        size,
        headOffset + 1
        /* extType */
      );
      return this.extensionCodec.decode(data, extType, this.context);
    };
    Decoder2.prototype.lookU8 = function() {
      return this.view.getUint8(this.pos);
    };
    Decoder2.prototype.lookU16 = function() {
      return this.view.getUint16(this.pos);
    };
    Decoder2.prototype.lookU32 = function() {
      return this.view.getUint32(this.pos);
    };
    Decoder2.prototype.readU8 = function() {
      var value = this.view.getUint8(this.pos);
      this.pos++;
      return value;
    };
    Decoder2.prototype.readI8 = function() {
      var value = this.view.getInt8(this.pos);
      this.pos++;
      return value;
    };
    Decoder2.prototype.readU16 = function() {
      var value = this.view.getUint16(this.pos);
      this.pos += 2;
      return value;
    };
    Decoder2.prototype.readI16 = function() {
      var value = this.view.getInt16(this.pos);
      this.pos += 2;
      return value;
    };
    Decoder2.prototype.readU32 = function() {
      var value = this.view.getUint32(this.pos);
      this.pos += 4;
      return value;
    };
    Decoder2.prototype.readI32 = function() {
      var value = this.view.getInt32(this.pos);
      this.pos += 4;
      return value;
    };
    Decoder2.prototype.readU64 = function() {
      var value = getUint64(this.view, this.pos);
      this.pos += 8;
      return value;
    };
    Decoder2.prototype.readI64 = function() {
      var value = getInt64(this.view, this.pos);
      this.pos += 8;
      return value;
    };
    Decoder2.prototype.readF32 = function() {
      var value = this.view.getFloat32(this.pos);
      this.pos += 4;
      return value;
    };
    Decoder2.prototype.readF64 = function() {
      var value = this.view.getFloat64(this.pos);
      this.pos += 8;
      return value;
    };
    return Decoder2;
  }()
);

// node_modules/.pnpm/@msgpack+msgpack@2.8.0/node_modules/@msgpack/msgpack/dist.es5+esm/decode.mjs
var defaultDecodeOptions = {};
function decode(buffer, options) {
  if (options === void 0) {
    options = defaultDecodeOptions;
  }
  var decoder = new Decoder(options.extensionCodec, options.context, options.maxStrLength, options.maxBinLength, options.maxArrayLength, options.maxMapLength, options.maxExtLength);
  return decoder.decode(buffer);
}

// src/core/ipc-web/ReadableStreamIpc.cts
var import_once3 = __toESM(require_once());

// src/core/ipc-web/$messageToIpcMessage.cts
var $isIpcSignalMessage = (msg) => msg === "close" || msg === "ping" || msg === "pong";
var $objectToIpcMessage = (data, ipc2) => {
  let message;
  if (data.type === 0 /* REQUEST */) {
    message = new IpcRequest(
      data.req_id,
      data.url,
      data.method,
      new IpcHeaders(data.headers),
      new IpcBodyReceiver(MetaBody.fromJSON(data.metaBody), ipc2),
      ipc2
    );
  } else if (data.type === 1 /* RESPONSE */) {
    message = new IpcResponse(
      data.req_id,
      data.statusCode,
      new IpcHeaders(data.headers),
      new IpcBodyReceiver(MetaBody.fromJSON(data.metaBody), ipc2),
      ipc2
    );
  } else if (data.type === 7 /* EVENT */) {
    message = new IpcEvent(data.name, data.data, data.encoding);
  } else if (data.type === 2 /* STREAM_DATA */) {
    message = new IpcStreamData(data.stream_id, data.data, data.encoding);
  } else if (data.type === 3 /* STREAM_PULLING */) {
    message = new IpcStreamPulling(data.stream_id, data.bandwidth);
  } else if (data.type === 4 /* STREAM_PAUSED */) {
    message = new IpcStreamPaused(data.stream_id, data.fuse);
  } else if (data.type === 6 /* STREAM_ABORT */) {
    message = new IpcStreamAbort(data.stream_id);
  } else if (data.type === 5 /* STREAM_END */) {
    message = new IpcStreamEnd(data.stream_id);
  }
  return message;
};
var $messageToIpcMessage = (data, ipc2) => {
  if ($isIpcSignalMessage(data)) {
    return data;
  }
  return $objectToIpcMessage(data, ipc2);
};
var $jsonToIpcMessage = (data, ipc2) => {
  if ($isIpcSignalMessage(data)) {
    return data;
  }
  return $objectToIpcMessage(JSON.parse(data), ipc2);
};

// src/core/ipc-web/$messagePackToIpcMessage.cts
var $messagePackToIpcMessage = (data, ipc2) => {
  return $messageToIpcMessage(
    decode(data),
    ipc2
  );
};

// src/core/ipc-web/ReadableStreamIpc.cts
var _rso;
var ReadableStreamIpc = class extends Ipc {
  constructor(remote, role, self_support_protocols = {
    raw: false,
    message_pack: true,
    protobuf: false
  }) {
    super();
    this.remote = remote;
    this.role = role;
    this.self_support_protocols = self_support_protocols;
    __privateAdd(this, _rso, new ReadableStreamOut());
    this.PONG_DATA = (0, import_once3.default)(() => {
      const pong = simpleEncoder("pong", "utf8");
      this._len[0] = pong.length;
      return u8aConcat([this._len_u8a, pong]);
    });
    this._len = new Uint32Array(1);
    this._len_u8a = new Uint8Array(this._len.buffer);
    this._support_message_pack = self_support_protocols.message_pack && remote.ipc_support_protocols.message_pack;
  }
  /** 这是输出流，给外部读取用的 */
  get stream() {
    return __privateGet(this, _rso).stream;
  }
  get controller() {
    return __privateGet(this, _rso).controller;
  }
  /**
   * 输入流要额外绑定
   * 注意，非必要不要 await 这个promise
   */
  async bindIncomeStream(stream) {
    if (this._incomne_stream !== void 0) {
      throw new Error("in come stream alreay binded.");
    }
    this._incomne_stream = await stream;
    const reader = binaryStreamRead(this._incomne_stream);
    while (await reader.available() > 0) {
      const size = await reader.readInt();
      const data = await reader.readBinary(size);
      const message = this.support_message_pack ? $messagePackToIpcMessage(data, this) : $jsonToIpcMessage(simpleDecoder(data, "utf8"), this);
      if (message === void 0) {
        console.error("unkonwn message", data);
        return;
      }
      if (message === "pong") {
        return;
      }
      if (message === "close") {
        this.close();
        return;
      }
      if (message === "ping") {
        this.controller.enqueue(this.PONG_DATA());
        return;
      }
      this._messageSignal.emit(message, this);
    }
  }
  _doPostMessage(message) {
    var message_raw;
    if (message.type === 0 /* REQUEST */) {
      message_raw = message.ipcReqMessage();
    } else if (message.type === 1 /* RESPONSE */) {
      message_raw = message.ipcResMessage();
    } else {
      message_raw = message;
    }
    const message_data = this.support_message_pack ? encode(message_raw) : simpleEncoder(JSON.stringify(message_raw), "utf8");
    this._len[0] = message_data.length;
    const chunk = u8aConcat([this._len_u8a, message_data]);
    this.controller.enqueue(chunk);
  }
  _doClose() {
    this.controller.close();
  }
};
_rso = new WeakMap();

// src/user/browser/api.request.mts
var { IpcResponse: IpcResponse2, Ipc: Ipc2, IpcRequest: IpcRequest2, IpcHeaders: IpcHeaders2, IPC_METHOD: IPC_METHOD2 } = ipc;
var ipcObserversMap = /* @__PURE__ */ new Map();
var INTERNAL_PREFIX = "/internal";
var fetchSignal = createSignal2();
var onFetchSignal = createSignal2();
async function onApiRequest(serverurlInfo, request, httpServerIpc) {
  let ipcResponse;
  try {
    const url = new URL(request.url, serverurlInfo.internal_origin);
    if (url.pathname.startsWith(INTERNAL_PREFIX)) {
      ipcResponse = internalFactory(
        url,
        request.req_id,
        httpServerIpc,
        serverurlInfo
      );
    } else {
      const path = `file:/${url.pathname}${url.search}`;
      const ipcProxyRequest = new IpcRequest2(
        jsProcess.fetchIpc.allocReqId(),
        path,
        request.method,
        request.headers,
        request.body,
        jsProcess.fetchIpc
      );
      jsProcess.fetchIpc.postMessage(ipcProxyRequest);
      const ipcProxyResponse = await jsProcess.fetchIpc.registerReqId(
        ipcProxyRequest.req_id
      ).promise;
      ipcResponse = new IpcResponse2(
        request.req_id,
        ipcProxyResponse.statusCode,
        ipcProxyResponse.headers,
        ipcProxyResponse.body,
        httpServerIpc
      );
    }
    if (!ipcResponse) {
      throw new Error(`unknown gateway: ${url.search}`);
    }
    cros(ipcResponse.headers);
    httpServerIpc.postMessage(ipcResponse);
  } catch (err) {
    if (ipcResponse === void 0) {
      ipcResponse = await IpcResponse2.fromText(
        request.req_id,
        502,
        void 0,
        String(err),
        httpServerIpc
      );
      cros(ipcResponse.headers);
      httpServerIpc.postMessage(ipcResponse);
    } else {
      throw err;
    }
  }
}
var internalFactory = (url, req_id, httpServerIpc, serverurlInfo) => {
  const pathname = url.pathname.slice(INTERNAL_PREFIX.length);
  if (pathname === "/public-url") {
    return IpcResponse2.fromText(
      req_id,
      200,
      void 0,
      serverurlInfo.buildPublicUrl(() => {
      }).href,
      httpServerIpc
    );
  }
  if (pathname === "/observe") {
    const mmid = url.searchParams.get("mmid");
    if (mmid === null) {
      throw new Error("observe require mmid");
    }
    const host = new URL(serverurlInfo.internal_origin).host;
    const streamPo = observeFactory(mmid, host);
    return IpcResponse2.fromStream(
      req_id,
      200,
      void 0,
      streamPo.stream,
      httpServerIpc
    );
  }
  if (pathname === "/fetch") {
    const streamPo = serviceWorkerFetch();
    return IpcResponse2.fromStream(
      req_id,
      200,
      void 0,
      streamPo.stream,
      httpServerIpc
    );
  }
  if (pathname === "/onFetch") {
    const streamPo = serviceWorkerOnFetch();
    return IpcResponse2.fromStream(
      req_id,
      200,
      void 0,
      streamPo.stream,
      httpServerIpc
    );
  }
};
var serviceWorkerFetch = () => {
  const streamPo = new ReadableStreamOut();
  const ob = { controller: streamPo.controller };
  fetchSignal.listen((ipcRequest) => {
    const jsonlineEnd = simpleEncoder("\n", "utf8");
    const json = ipcRequest.toJSON();
    const uint8 = simpleEncoder(JSON.stringify(json), "utf8");
    ob.controller.enqueue(u8aConcat([uint8, jsonlineEnd]));
  });
  return streamPo;
};
var serviceWorkerOnFetch = () => {
  const streamPo = new ReadableStreamOut();
  const ob = { controller: streamPo.controller };
  onFetchSignal.listen((ipcRequest) => {
    const jsonlineEnd = simpleEncoder("\n", "utf8");
    const json = ipcRequest.toJSON();
    const uint8 = simpleEncoder(JSON.stringify(json), "utf8");
    ob.controller.enqueue(u8aConcat([uint8, jsonlineEnd]));
  });
  return streamPo;
};
var observeFactory = (mmid, host) => {
  const streamPo = new ReadableStreamOut();
  const observers = mapHelper.getOrPut(ipcObserversMap, mmid, (mmid2) => {
    const result = { ipc: new PromiseOut2(), obs: /* @__PURE__ */ new Set() };
    result.ipc.resolve(jsProcess.connect(mmid2));
    result.ipc.promise.then((ipc2) => {
      ipc2.postMessage(
        IpcEvent.fromText("observe", host)
      );
      ipc2.onEvent((event) => {
        if (event.name !== "observe" /* State */ && event.name !== "observeUpdateProgress" /* UpdateProgress */) {
          return;
        }
        const observers2 = ipcObserversMap.get(ipc2.remote.mmid);
        const jsonlineEnd = simpleEncoder("\n", "utf8");
        if (observers2 && observers2.obs.size > 0) {
          for (const ob2 of observers2.obs) {
            ob2.controller.enqueue(u8aConcat([event.binary, jsonlineEnd]));
          }
        }
      });
    });
    return result;
  });
  const ob = { controller: streamPo.controller };
  observers.obs.add(ob);
  streamPo.onCancel(() => {
    observers.obs.delete(ob);
  });
  return streamPo;
};

// src/user/browser/www-server-on-request.mts
var { IpcResponse: IpcResponse3, IpcHeaders: IpcHeaders3 } = ipc;
async function wwwServerOnRequest(request, ipc2) {
  let pathname = request.parsed_url.pathname;
  pathname = pathname === "/" ? "/index.html" : pathname;
  const url = `file:///app/cot-demo${pathname}?mode=stream`;
  const response = await jsProcess.nativeRequest(url);
  ipc2.postMessage(
    new IpcResponse3(
      request.req_id,
      response.statusCode,
      response.headers,
      response.body,
      ipc2
    )
  );
}

// src/user/browser/browser.worker.mts
var main = async () => {
  console.log("bootstrap browser.worker.mts");
  const { IpcEvent: IpcEvent2 } = ipc;
  const mainUrl = new PromiseOut();
  let oldWebviewState = [];
  const multiWebViewIpc = await jsProcess.connect("mwebview.sys.dweb");
  const multiWebViewCloseSignal = createSignal();
  const tryOpenView = () => {
    console.log("tryOpenView");
    const newWindowState = new PromiseOut();
    (async () => {
      try {
        if (webViewMap.size === 0) {
          const url = await mainUrl.promise;
          const view_id = await nativeOpen(url);
          webViewMap.set(view_id, {
            isActivated: true,
            webviewId: view_id
          });
          return view_id;
        }
        await Promise.all(
          [...webViewMap.values()].map((item) => {
            return nativeActivate(item.webviewId);
          })
        );
      } finally {
        newWindowState.resolve(true);
      }
    })();
    windowState = newWindowState;
    return newWindowState;
  };
  let windowState;
  windowState = tryOpenView();
  const { IpcResponse: IpcResponse4, IpcHeaders: IpcHeaders4 } = ipc;
  const wwwServer = await http.createHttpDwebServer(jsProcess, {
    subdomain: "www",
    port: 443
  });
  const apiServer = await http.createHttpDwebServer(jsProcess, {
    subdomain: "api",
    port: 443
  });
  const externalServer = await http.createHttpDwebServer(jsProcess, {
    subdomain: "external",
    port: 443
  });
  const apiReadableStreamIpc = await apiServer.listen();
  const wwwReadableStreamIpc = await wwwServer.listen();
  const externalReadableStreamIpc = await externalServer.listen();
  apiReadableStreamIpc.onRequest(async (request, ipc2) => {
    const url = new URL(
      request.url,
      apiServer.startResult.urlInfo.internal_origin
    );
    if (url.pathname.startsWith("/dns.sys.dweb")) {
      const result = await serviceWorkerFactory(url, ipc2);
      const ipcResponse = IpcResponse4.fromText(
        request.req_id,
        200,
        void 0,
        result,
        ipc2
      );
      cros(ipcResponse.headers);
      return ipc2.postMessage(ipcResponse);
    }
    onApiRequest(apiServer.startResult.urlInfo, request, ipc2);
  });
  wwwReadableStreamIpc.onRequest(wwwServerOnRequest);
  externalReadableStreamIpc.onRequest(async (request, ipc2) => {
  });
  const serviceWorkerFactory = async (url, ipc2) => {
    const pathname = url.pathname;
    if (pathname.endsWith("close")) {
      return closeFront();
    }
    if (pathname.endsWith("restart")) {
      return restartApp(
        [apiServer, wwwServer],
        [apiReadableStreamIpc, wwwReadableStreamIpc]
      );
    }
    return "no action for serviceWorker Factory !!!";
  };
  jsProcess.onActivity(async (ipcEvent, ipc2) => {
    if (await windowState.promise === false) {
      await tryOpenView();
    }
    ipc2.postMessage(IpcEvent2.fromText("ready", "activity"));
    if (hasActivityEventIpcs.has(ipc2) === false) {
      hasActivityEventIpcs.add(ipc2);
      multiWebViewCloseSignal.listen(() => {
        ipc2.postMessage(IpcEvent2.fromText("close", ""));
        ipc2.close();
      });
    }
  });
  const hasActivityEventIpcs = /* @__PURE__ */ new Set();
  multiWebViewIpc.onEvent(async (event) => {
    console.log("connectMultiWebView =>", event.name);
    if (event.name === "state" /* State */ && typeof event.data === "string") {
      const newState = JSON.parse(event.data);
      const diff = detailed_default(oldWebviewState, newState);
      oldWebviewState = newState;
      diffFactory(diff);
    }
  });
  const diffFactory = async (diff) => {
    console.log(
      "connectMultiWebView diffFactory=>",
      diff.added,
      diff.deleted,
      diff.updated
    );
    for (const id in diff.added) {
      webViewMap.set(id, JSON.parse(diff.added[id]));
    }
    for (const id in diff.deleted) {
      webViewMap.delete(id);
      await closeDwebView(id);
    }
    for (const id in diff.updated) {
      webViewMap.set(
        id,
        JSON.parse(diff.updated[id])
      );
      await nativeActivate(id);
    }
  };
  {
    const interUrl = wwwServer.startResult.urlInfo.buildInternalUrl((url) => {
      url.pathname = "/index.html";
    }).href;
    mainUrl.resolve(interUrl);
  }
};
main();
