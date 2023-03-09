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

// node_modules/lodash/_trimmedEndIndex.js
var require_trimmedEndIndex = __commonJS({
  "node_modules/lodash/_trimmedEndIndex.js"(exports, module) {
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

// node_modules/lodash/_baseTrim.js
var require_baseTrim = __commonJS({
  "node_modules/lodash/_baseTrim.js"(exports, module) {
    var trimmedEndIndex = require_trimmedEndIndex();
    var reTrimStart = /^\s+/;
    function baseTrim(string) {
      return string ? string.slice(0, trimmedEndIndex(string) + 1).replace(reTrimStart, "") : string;
    }
    module.exports = baseTrim;
  }
});

// node_modules/lodash/isObject.js
var require_isObject = __commonJS({
  "node_modules/lodash/isObject.js"(exports, module) {
    function isObject(value) {
      var type = typeof value;
      return value != null && (type == "object" || type == "function");
    }
    module.exports = isObject;
  }
});

// node_modules/lodash/_freeGlobal.js
var require_freeGlobal = __commonJS({
  "node_modules/lodash/_freeGlobal.js"(exports, module) {
    var freeGlobal = typeof global == "object" && global && global.Object === Object && global;
    module.exports = freeGlobal;
  }
});

// node_modules/lodash/_root.js
var require_root = __commonJS({
  "node_modules/lodash/_root.js"(exports, module) {
    var freeGlobal = require_freeGlobal();
    var freeSelf = typeof self == "object" && self && self.Object === Object && self;
    var root = freeGlobal || freeSelf || Function("return this")();
    module.exports = root;
  }
});

// node_modules/lodash/_Symbol.js
var require_Symbol = __commonJS({
  "node_modules/lodash/_Symbol.js"(exports, module) {
    var root = require_root();
    var Symbol2 = root.Symbol;
    module.exports = Symbol2;
  }
});

// node_modules/lodash/_getRawTag.js
var require_getRawTag = __commonJS({
  "node_modules/lodash/_getRawTag.js"(exports, module) {
    var Symbol2 = require_Symbol();
    var objectProto = Object.prototype;
    var hasOwnProperty = objectProto.hasOwnProperty;
    var nativeObjectToString = objectProto.toString;
    var symToStringTag = Symbol2 ? Symbol2.toStringTag : void 0;
    function getRawTag(value) {
      var isOwn = hasOwnProperty.call(value, symToStringTag), tag = value[symToStringTag];
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

// node_modules/lodash/_objectToString.js
var require_objectToString = __commonJS({
  "node_modules/lodash/_objectToString.js"(exports, module) {
    var objectProto = Object.prototype;
    var nativeObjectToString = objectProto.toString;
    function objectToString(value) {
      return nativeObjectToString.call(value);
    }
    module.exports = objectToString;
  }
});

// node_modules/lodash/_baseGetTag.js
var require_baseGetTag = __commonJS({
  "node_modules/lodash/_baseGetTag.js"(exports, module) {
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

// node_modules/lodash/isObjectLike.js
var require_isObjectLike = __commonJS({
  "node_modules/lodash/isObjectLike.js"(exports, module) {
    function isObjectLike(value) {
      return value != null && typeof value == "object";
    }
    module.exports = isObjectLike;
  }
});

// node_modules/lodash/isSymbol.js
var require_isSymbol = __commonJS({
  "node_modules/lodash/isSymbol.js"(exports, module) {
    var baseGetTag = require_baseGetTag();
    var isObjectLike = require_isObjectLike();
    var symbolTag = "[object Symbol]";
    function isSymbol(value) {
      return typeof value == "symbol" || isObjectLike(value) && baseGetTag(value) == symbolTag;
    }
    module.exports = isSymbol;
  }
});

// node_modules/lodash/toNumber.js
var require_toNumber = __commonJS({
  "node_modules/lodash/toNumber.js"(exports, module) {
    var baseTrim = require_baseTrim();
    var isObject = require_isObject();
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
      if (isObject(value)) {
        var other = typeof value.valueOf == "function" ? value.valueOf() : value;
        value = isObject(other) ? other + "" : other;
      }
      if (typeof value != "string") {
        return value === 0 ? value : +value;
      }
      value = baseTrim(value);
      var isBinary = reIsBinary.test(value);
      return isBinary || reIsOctal.test(value) ? freeParseInt(value.slice(2), isBinary ? 2 : 8) : reIsBadHex.test(value) ? NAN : +value;
    }
    module.exports = toNumber;
  }
});

// node_modules/lodash/toFinite.js
var require_toFinite = __commonJS({
  "node_modules/lodash/toFinite.js"(exports, module) {
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

// node_modules/lodash/toInteger.js
var require_toInteger = __commonJS({
  "node_modules/lodash/toInteger.js"(exports, module) {
    var toFinite = require_toFinite();
    function toInteger(value) {
      var result = toFinite(value), remainder = result % 1;
      return result === result ? remainder ? result - remainder : result : 0;
    }
    module.exports = toInteger;
  }
});

// node_modules/lodash/before.js
var require_before = __commonJS({
  "node_modules/lodash/before.js"(exports, module) {
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

// node_modules/lodash/once.js
var require_once = __commonJS({
  "node_modules/lodash/once.js"(exports, module) {
    var before = require_before();
    function once2(func) {
      return before(2, func);
    }
    module.exports = once2;
  }
});

// src/core/ipc/IpcResponse.cts
var import_once = __toESM(require_once());

// src/helper/binaryHelper.cts
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

// src/helper/createSignal.cts
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
    this.clear = () => {
      this._cbs.clear();
    };
  }
};

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

// src/core/ipc/IpcStreamAbort.cts
var IpcStreamAbort = class extends IpcMessage {
  constructor(stream_id) {
    super(5 /* STREAM_ABORT */);
    this.stream_id = stream_id;
  }
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
    super(4 /* STREAM_END */);
    this.stream_id = stream_id;
  }
};

// src/core/ipc/IpcStreamPull.cts
var IpcStreamPull = class extends IpcMessage {
  constructor(stream_id, desiredSize) {
    super(3 /* STREAM_PULL */);
    this.stream_id = stream_id;
    if (desiredSize == null) {
      desiredSize = 1;
    } else if (Number.isFinite(desiredSize) === false) {
      desiredSize = 1;
    } else if (desiredSize < 1) {
      desiredSize = 1;
    }
    this.desiredSize = desiredSize;
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
    return (this.type & IPC_META_BODY_TYPE.INLINE) !== 0;
  }
  get type_isStream() {
    return (this.type & IPC_META_BODY_TYPE.INLINE) === 0;
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
    this.pullSignal = createSignal();
    this.abortSignal = createSignal();
    /**
     * 被哪些 ipc 所真正使用，使用的进度分别是多少
     *
     * 这个进度 用于 类似流的 多发
     */
    this.usedIpcMap = /* @__PURE__ */ new Map();
    /**
     * 当前收到拉取的请求数
     */
    this.curPulledTimes = 0;
    this.closeSignal = createSignal();
    this.openSignal = createSignal();
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
    if (this.usedIpcMap.has(ipc2)) {
      return true;
    }
    if (this.isStream && !this._isStreamOpened) {
      this.usedIpcMap.set(ipc2, 0);
      this.closeSignal.listen(() => {
        this.unuseByIpc(ipc2);
      });
      return true;
    }
    return false;
  }
  /**
   * 拉取数据
   */
  emitStreamPull(message, ipc2) {
    const pulledSize = this.usedIpcMap.get(ipc2) + message.desiredSize;
    this.usedIpcMap.set(ipc2, pulledSize);
    this.pullSignal.emit();
  }
  /**
   * 解绑使用
   */
  unuseByIpc(ipc2) {
    if (this.usedIpcMap.delete(ipc2) != null) {
      if (this.usedIpcMap.size === 0) {
        this.abortSignal.emit();
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
    const sender = async () => {
      if (this.curPulledTimes++ > 0) {
        return;
      }
      while (this.curPulledTimes > 0) {
        const availableLen = await reader.available();
        switch (availableLen) {
          case -1:
          case 0:
            {
              const message = new IpcStreamEnd(stream_id);
              for (const ipc3 of this.usedIpcMap.keys()) {
                ipc3.postMessage(message);
              }
              this.emitStreamClose();
            }
            break;
          default: {
            this.isStreamOpened = true;
            const data = await reader.readBinary(availableLen);
            const message = IpcStreamData.fromBinary(stream_id, data);
            for (const ipc3 of this.usedIpcMap.keys()) {
              ipc3.postMessage(message);
            }
          }
        }
        this.curPulledTimes = 0;
      }
    };
    this.pullSignal.listen(() => {
      void sender();
    });
    this.abortSignal.listen(() => {
      reader.return();
      this.emitStreamClose();
    });
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
        ipc2.onMessage((message) => {
          if (message instanceof IpcStreamPull) {
            const ipcBody2 = mapper.get(message.stream_id);
            if (ipcBody2?.useByIpc(ipc2)) {
              ipcBody2.emitStreamPull(message, ipc2);
            }
          } else if (message instanceof IpcStreamAbort) {
            const ipcBody2 = mapper.get(message.stream_id);
            ipcBody2?.unuseByIpc(ipc2);
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
var UsableIpcBodyMapper = class {
  constructor() {
    this.map = /* @__PURE__ */ new Map();
    this.destroySignal = createSignal();
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
      record[key] = value;
    });
    return record;
  }
};

// src/core/ipc/IpcResponse.cts
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
    this.ipcResMessage = (0, import_once.default)(
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
  static async fromResponse(req_id, response, ipc2) {
    let ipcBody;
    if (response.body) {
      ipcBody = IpcBodySender.from(response.body, ipc2);
    } else {
      ipcBody = IpcBodySender.from(
        binaryToU8a(await response.arrayBuffer()),
        ipc2
      );
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

// src/user/cot-demo/cotDemo.request.mts
async function onRequestToastShow(request, httpServerIpc) {
  const url = `file://toast.sys.dweb${request.url}`;
  console.log("onRequestToastShow: ", url);
  const result = await jsProcess.nativeFetch(url).then(async (res) => res).catch((err) => {
    console.log("\u8BF7\u6C42\u5931\u8D25\uFF1A ", err);
    return err;
  });
  return result;
}

// src/user/cot-demo/cotDemo.worker.mts
var main = async () => {
  console.log("start cot-demo");
  const { IpcResponse: IpcResponse3, IpcHeaders: IpcHeaders2 } = ipc;
  const wwwServer = await http.createHttpDwebServer(jsProcess, {
    subdomain: "www",
    port: 443
  });
  const apiServer = await http.createHttpDwebServer(jsProcess, {
    subdomain: "api",
    port: 443
  });
  console.log("will do listen!!", wwwServer.startResult.urlInfo.host, apiServer.startResult.urlInfo.host);
  (await apiServer.listen()).onRequest(async (request, ipc2) => {
    console.log("\u63A5\u53D7\u5230\u4E86\u8BF7\u6C42 apiServer\uFF1A request.parsed_url.pathname\uFF1A ", request.parsed_url.pathname);
    onRequest(request, ipc2);
  });
  async function onRequest(request, httpServerIpc) {
    let res = new Response();
    switch (request.parsed_url.pathname) {
      case "/":
      case "/show":
        res = await onRequestToastShow(request, httpServerIpc);
        break;
      default:
        break;
    }
    httpServerIpc.postMessage(
      await IpcResponse3.fromJson(
        request.req_id,
        200,
        new IpcHeaders2({
          "content-type": "text/html",
          "Access-Control-Allow-Origin": "*",
          "Access-Control-Allow-Headers": "*",
          // 要支持 X-Dweb-Host
          "Access-Control-Allow-Methods": "*"
        }),
        await res.text(),
        httpServerIpc
      )
    );
  }
  (await wwwServer.listen()).onRequest(async (request, ipc2) => {
    console.log("\u63A5\u53D7\u5230\u4E86\u8BF7\u6C42 wwwServer request.parsed_url.pathname\uFF1A ", request.parsed_url.pathname);
    let pathname = request.parsed_url.pathname;
    if (pathname === "/") {
      pathname = "/index.html";
    }
    console.time(`open file ${pathname}`);
    const remoteIpcResponse = await jsProcess.nativeRequest(
      `file:///cot-demo${pathname}?mode=stream`
    );
    console.timeEnd(`open file ${pathname}`);
    ipc2.postMessage(
      new IpcResponse3(
        request.req_id,
        remoteIpcResponse.statusCode,
        remoteIpcResponse.headers,
        remoteIpcResponse.body,
        ipc2
      )
    );
  });
  {
    const view_id = await jsProcess.nativeFetch(
      `file://mwebview.sys.dweb/open?url=${encodeURIComponent(
        wwwServer.startResult.urlInfo.buildInternalUrl((url) => {
          url.pathname = "/index.html";
        }).href
      )}`
    ).text();
  }
  {
  }
};
main();
