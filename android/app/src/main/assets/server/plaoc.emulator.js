var __defProp = Object.defineProperty;
var __getOwnPropDesc = Object.getOwnPropertyDescriptor;
var __require = /* @__PURE__ */ ((x8) => typeof require !== "undefined" ? require : typeof Proxy !== "undefined" ? new Proxy(x8, {
  get: (a4, b7) => (typeof require !== "undefined" ? require : a4)[b7]
}) : x8)(function(x8) {
  if (typeof require !== "undefined")
    return require.apply(this, arguments);
  throw new Error('Dynamic require of "' + x8 + '" is not supported');
});
var __export = (target, all) => {
  for (var name in all)
    __defProp(target, name, { get: all[name], enumerable: true });
};
var __decorateClass = (decorators, target, key, kind) => {
  var result = kind > 1 ? void 0 : kind ? __getOwnPropDesc(target, key) : target;
  for (var i3 = decorators.length - 1, decorator; i3 >= 0; i3--)
    if (decorator = decorators[i3])
      result = (kind ? decorator(target, key, result) : decorator(result)) || result;
  if (kind && result)
    __defProp(target, key, result);
  return result;
};

// ../desktop-dev/src/helper/PromiseOut.ts
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
  static resolve(v12) {
    const po = new PromiseOut();
    po.resolve(v12);
    return po;
  }
  static reject(reason) {
    const po = new PromiseOut();
    po.reject(reason);
    return po;
  }
  static sleep(ms) {
    const po = new PromiseOut();
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

// ../desktop-dev/src/helper/binaryHelper.ts
var isBinary = (data) => data instanceof ArrayBuffer || ArrayBuffer.isView(data) || typeof SharedArrayBuffer === "function" && data instanceof SharedArrayBuffer;
var binaryToU8a = (binary) => {
  if (binary instanceof Uint8Array) {
    return binary;
  }
  if (ArrayBuffer.isView(binary)) {
    return new Uint8Array(binary.buffer, binary.byteOffset, binary.byteLength);
  }
  return new Uint8Array(binary);
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

// ../desktop-dev/src/helper/color.ts
function hexaToRGBA(str) {
  return {
    red: parseInt(str.slice(1, 3), 16),
    green: parseInt(str.slice(3, 5), 16),
    blue: parseInt(str.slice(5, 7), 16),
    alpha: parseInt(str.slice(7), 16)
  };
}
function colorToHex(color) {
  const rgbaColor = color.alpha === 255 ? [color.red, color.green, color.blue] : [color.red, color.green, color.blue, color.alpha];
  return `#${rgbaColor.map((v12) => (v12 & 255).toString(16).padStart(2, "0")).join("")}`;
}

// ../desktop-dev/src/helper/cacheGetter.ts
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

// ../desktop-dev/src/helper/createSignal.ts
var createSignal = (autoStart) => {
  return new Signal(autoStart);
};
var Signal = class {
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
], Signal.prototype, "_cachedEmits", 1);

// ../desktop-dev/src/helper/encoding.ts
var textEncoder = new TextEncoder();
var simpleEncoder = (data, encoding) => {
  if (encoding === "base64") {
    const byteCharacters = atob(data);
    const binary = new Uint8Array(byteCharacters.length);
    for (let i3 = 0; i3 < byteCharacters.length; i3++) {
      binary[i3] = byteCharacters.charCodeAt(i3);
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

// ../desktop-dev/src/helper/mapHelper.ts
var mapHelper = new class {
  getOrPut(map, key, putter) {
    if (map.has(key)) {
      return map.get(key);
    }
    const put = putter(key);
    map.set(key, put);
    return put;
  }
  getAndRemove(map, key) {
    const val = map.get(key);
    if (map.delete(key)) {
      return val;
    }
  }
}();

// ../desktop-dev/src/helper/readableStreamHelper.ts
async function* _doRead(reader, options) {
  const signal = options?.signal;
  if (signal !== void 0) {
    signal.addEventListener("abort", (reason) => reader.cancel(reason));
  }
  try {
    while (true) {
      const item = await reader.read();
      if (item.done) {
        break;
      }
      yield item.value;
    }
  } catch (err) {
    reader.cancel(err);
  } finally {
    reader.releaseLock();
  }
}
var streamRead = (stream, options) => {
  return _doRead(stream.getReader(), options);
};
var binaryStreamRead = (stream, options) => {
  const reader = streamRead(stream, options);
  let done = false;
  let cache = new Uint8Array(0);
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
  const maps = [];
  for await (const item of _doRead(stream.getReader())) {
    if (options.map) {
      maps.push(options.map(item));
    }
  }
  const result = options.complete?.(maps);
  return {
    maps,
    result
  };
};
var streamReadAllBuffer = async (stream) => {
  return (await streamReadAll(stream, {
    map(chunk) {
      return chunk;
    },
    complete(chunks) {
      return u8aConcat(chunks);
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
    return (this._on_cancel_signal ??= createSignal()).listen;
  }
  get onPull() {
    return (this._on_pull_signal ??= createSignal()).listen;
  }
};

// https://esm.sh/v124/zod@3.21.4/denonext/zod.mjs
var g;
(function(s2) {
  s2.assertEqual = (n4) => n4;
  function e3(n4) {
  }
  s2.assertIs = e3;
  function t3(n4) {
    throw new Error();
  }
  s2.assertNever = t3, s2.arrayToEnum = (n4) => {
    let a4 = {};
    for (let i3 of n4)
      a4[i3] = i3;
    return a4;
  }, s2.getValidEnumValues = (n4) => {
    let a4 = s2.objectKeys(n4).filter((o3) => typeof n4[n4[o3]] != "number"), i3 = {};
    for (let o3 of a4)
      i3[o3] = n4[o3];
    return s2.objectValues(i3);
  }, s2.objectValues = (n4) => s2.objectKeys(n4).map(function(a4) {
    return n4[a4];
  }), s2.objectKeys = typeof Object.keys == "function" ? (n4) => Object.keys(n4) : (n4) => {
    let a4 = [];
    for (let i3 in n4)
      Object.prototype.hasOwnProperty.call(n4, i3) && a4.push(i3);
    return a4;
  }, s2.find = (n4, a4) => {
    for (let i3 of n4)
      if (a4(i3))
        return i3;
  }, s2.isInteger = typeof Number.isInteger == "function" ? (n4) => Number.isInteger(n4) : (n4) => typeof n4 == "number" && isFinite(n4) && Math.floor(n4) === n4;
  function r3(n4, a4 = " | ") {
    return n4.map((i3) => typeof i3 == "string" ? `'${i3}'` : i3).join(a4);
  }
  s2.joinValues = r3, s2.jsonStringifyReplacer = (n4, a4) => typeof a4 == "bigint" ? a4.toString() : a4;
})(g || (g = {}));
var me;
(function(s2) {
  s2.mergeShapes = (e3, t3) => ({ ...e3, ...t3 });
})(me || (me = {}));
var d = g.arrayToEnum(["string", "nan", "number", "integer", "float", "boolean", "date", "bigint", "symbol", "function", "undefined", "null", "array", "object", "unknown", "promise", "void", "never", "map", "set"]);
var P = (s2) => {
  switch (typeof s2) {
    case "undefined":
      return d.undefined;
    case "string":
      return d.string;
    case "number":
      return isNaN(s2) ? d.nan : d.number;
    case "boolean":
      return d.boolean;
    case "function":
      return d.function;
    case "bigint":
      return d.bigint;
    case "symbol":
      return d.symbol;
    case "object":
      return Array.isArray(s2) ? d.array : s2 === null ? d.null : s2.then && typeof s2.then == "function" && s2.catch && typeof s2.catch == "function" ? d.promise : typeof Map < "u" && s2 instanceof Map ? d.map : typeof Set < "u" && s2 instanceof Set ? d.set : typeof Date < "u" && s2 instanceof Date ? d.date : d.object;
    default:
      return d.unknown;
  }
};
var c = g.arrayToEnum(["invalid_type", "invalid_literal", "custom", "invalid_union", "invalid_union_discriminator", "invalid_enum_value", "unrecognized_keys", "invalid_arguments", "invalid_return_type", "invalid_date", "invalid_string", "too_small", "too_big", "invalid_intersection_types", "not_multiple_of", "not_finite"]);
var Ne = (s2) => JSON.stringify(s2, null, 2).replace(/"([^"]+)":/g, "$1:");
var T = class extends Error {
  constructor(e3) {
    super(), this.issues = [], this.addIssue = (r3) => {
      this.issues = [...this.issues, r3];
    }, this.addIssues = (r3 = []) => {
      this.issues = [...this.issues, ...r3];
    };
    let t3 = new.target.prototype;
    Object.setPrototypeOf ? Object.setPrototypeOf(this, t3) : this.__proto__ = t3, this.name = "ZodError", this.issues = e3;
  }
  get errors() {
    return this.issues;
  }
  format(e3) {
    let t3 = e3 || function(a4) {
      return a4.message;
    }, r3 = { _errors: [] }, n4 = (a4) => {
      for (let i3 of a4.issues)
        if (i3.code === "invalid_union")
          i3.unionErrors.map(n4);
        else if (i3.code === "invalid_return_type")
          n4(i3.returnTypeError);
        else if (i3.code === "invalid_arguments")
          n4(i3.argumentsError);
        else if (i3.path.length === 0)
          r3._errors.push(t3(i3));
        else {
          let o3 = r3, f8 = 0;
          for (; f8 < i3.path.length; ) {
            let l5 = i3.path[f8];
            f8 === i3.path.length - 1 ? (o3[l5] = o3[l5] || { _errors: [] }, o3[l5]._errors.push(t3(i3))) : o3[l5] = o3[l5] || { _errors: [] }, o3 = o3[l5], f8++;
          }
        }
    };
    return n4(this), r3;
  }
  toString() {
    return this.message;
  }
  get message() {
    return JSON.stringify(this.issues, g.jsonStringifyReplacer, 2);
  }
  get isEmpty() {
    return this.issues.length === 0;
  }
  flatten(e3 = (t3) => t3.message) {
    let t3 = {}, r3 = [];
    for (let n4 of this.issues)
      n4.path.length > 0 ? (t3[n4.path[0]] = t3[n4.path[0]] || [], t3[n4.path[0]].push(e3(n4))) : r3.push(e3(n4));
    return { formErrors: r3, fieldErrors: t3 };
  }
  get formErrors() {
    return this.flatten();
  }
};
T.create = (s2) => new T(s2);
var oe = (s2, e3) => {
  let t3;
  switch (s2.code) {
    case c.invalid_type:
      s2.received === d.undefined ? t3 = "Required" : t3 = `Expected ${s2.expected}, received ${s2.received}`;
      break;
    case c.invalid_literal:
      t3 = `Invalid literal value, expected ${JSON.stringify(s2.expected, g.jsonStringifyReplacer)}`;
      break;
    case c.unrecognized_keys:
      t3 = `Unrecognized key(s) in object: ${g.joinValues(s2.keys, ", ")}`;
      break;
    case c.invalid_union:
      t3 = "Invalid input";
      break;
    case c.invalid_union_discriminator:
      t3 = `Invalid discriminator value. Expected ${g.joinValues(s2.options)}`;
      break;
    case c.invalid_enum_value:
      t3 = `Invalid enum value. Expected ${g.joinValues(s2.options)}, received '${s2.received}'`;
      break;
    case c.invalid_arguments:
      t3 = "Invalid function arguments";
      break;
    case c.invalid_return_type:
      t3 = "Invalid function return type";
      break;
    case c.invalid_date:
      t3 = "Invalid date";
      break;
    case c.invalid_string:
      typeof s2.validation == "object" ? "includes" in s2.validation ? (t3 = `Invalid input: must include "${s2.validation.includes}"`, typeof s2.validation.position == "number" && (t3 = `${t3} at one or more positions greater than or equal to ${s2.validation.position}`)) : "startsWith" in s2.validation ? t3 = `Invalid input: must start with "${s2.validation.startsWith}"` : "endsWith" in s2.validation ? t3 = `Invalid input: must end with "${s2.validation.endsWith}"` : g.assertNever(s2.validation) : s2.validation !== "regex" ? t3 = `Invalid ${s2.validation}` : t3 = "Invalid";
      break;
    case c.too_small:
      s2.type === "array" ? t3 = `Array must contain ${s2.exact ? "exactly" : s2.inclusive ? "at least" : "more than"} ${s2.minimum} element(s)` : s2.type === "string" ? t3 = `String must contain ${s2.exact ? "exactly" : s2.inclusive ? "at least" : "over"} ${s2.minimum} character(s)` : s2.type === "number" ? t3 = `Number must be ${s2.exact ? "exactly equal to " : s2.inclusive ? "greater than or equal to " : "greater than "}${s2.minimum}` : s2.type === "date" ? t3 = `Date must be ${s2.exact ? "exactly equal to " : s2.inclusive ? "greater than or equal to " : "greater than "}${new Date(Number(s2.minimum))}` : t3 = "Invalid input";
      break;
    case c.too_big:
      s2.type === "array" ? t3 = `Array must contain ${s2.exact ? "exactly" : s2.inclusive ? "at most" : "less than"} ${s2.maximum} element(s)` : s2.type === "string" ? t3 = `String must contain ${s2.exact ? "exactly" : s2.inclusive ? "at most" : "under"} ${s2.maximum} character(s)` : s2.type === "number" ? t3 = `Number must be ${s2.exact ? "exactly" : s2.inclusive ? "less than or equal to" : "less than"} ${s2.maximum}` : s2.type === "bigint" ? t3 = `BigInt must be ${s2.exact ? "exactly" : s2.inclusive ? "less than or equal to" : "less than"} ${s2.maximum}` : s2.type === "date" ? t3 = `Date must be ${s2.exact ? "exactly" : s2.inclusive ? "smaller than or equal to" : "smaller than"} ${new Date(Number(s2.maximum))}` : t3 = "Invalid input";
      break;
    case c.custom:
      t3 = "Invalid input";
      break;
    case c.invalid_intersection_types:
      t3 = "Intersection results could not be merged";
      break;
    case c.not_multiple_of:
      t3 = `Number must be a multiple of ${s2.multipleOf}`;
      break;
    case c.not_finite:
      t3 = "Number must be finite";
      break;
    default:
      t3 = e3.defaultError, g.assertNever(s2);
  }
  return { message: t3 };
};
var ke = oe;
function Ee(s2) {
  ke = s2;
}
function de() {
  return ke;
}
var ue = (s2) => {
  let { data: e3, path: t3, errorMaps: r3, issueData: n4 } = s2, a4 = [...t3, ...n4.path || []], i3 = { ...n4, path: a4 }, o3 = "", f8 = r3.filter((l5) => !!l5).slice().reverse();
  for (let l5 of f8)
    o3 = l5(i3, { data: e3, defaultError: o3 }).message;
  return { ...n4, path: a4, message: n4.message || o3 };
};
var Ie = [];
function u(s2, e3) {
  let t3 = ue({ issueData: e3, data: s2.data, path: s2.path, errorMaps: [s2.common.contextualErrorMap, s2.schemaErrorMap, de(), oe].filter((r3) => !!r3) });
  s2.common.issues.push(t3);
}
var k = class {
  constructor() {
    this.value = "valid";
  }
  dirty() {
    this.value === "valid" && (this.value = "dirty");
  }
  abort() {
    this.value !== "aborted" && (this.value = "aborted");
  }
  static mergeArray(e3, t3) {
    let r3 = [];
    for (let n4 of t3) {
      if (n4.status === "aborted")
        return m;
      n4.status === "dirty" && e3.dirty(), r3.push(n4.value);
    }
    return { status: e3.value, value: r3 };
  }
  static async mergeObjectAsync(e3, t3) {
    let r3 = [];
    for (let n4 of t3)
      r3.push({ key: await n4.key, value: await n4.value });
    return k.mergeObjectSync(e3, r3);
  }
  static mergeObjectSync(e3, t3) {
    let r3 = {};
    for (let n4 of t3) {
      let { key: a4, value: i3 } = n4;
      if (a4.status === "aborted" || i3.status === "aborted")
        return m;
      a4.status === "dirty" && e3.dirty(), i3.status === "dirty" && e3.dirty(), (typeof i3.value < "u" || n4.alwaysSet) && (r3[a4.value] = i3.value);
    }
    return { status: e3.value, value: r3 };
  }
};
var m = Object.freeze({ status: "aborted" });
var be = (s2) => ({ status: "dirty", value: s2 });
var b = (s2) => ({ status: "valid", value: s2 });
var ye = (s2) => s2.status === "aborted";
var ve = (s2) => s2.status === "dirty";
var le = (s2) => s2.status === "valid";
var fe = (s2) => typeof Promise < "u" && s2 instanceof Promise;
var h;
(function(s2) {
  s2.errToObj = (e3) => typeof e3 == "string" ? { message: e3 } : e3 || {}, s2.toString = (e3) => typeof e3 == "string" ? e3 : e3?.message;
})(h || (h = {}));
var O = class {
  constructor(e3, t3, r3, n4) {
    this._cachedPath = [], this.parent = e3, this.data = t3, this._path = r3, this._key = n4;
  }
  get path() {
    return this._cachedPath.length || (this._key instanceof Array ? this._cachedPath.push(...this._path, ...this._key) : this._cachedPath.push(...this._path, this._key)), this._cachedPath;
  }
};
var ge = (s2, e3) => {
  if (le(e3))
    return { success: true, data: e3.value };
  if (!s2.common.issues.length)
    throw new Error("Validation failed but no issues detected.");
  return { success: false, get error() {
    if (this._error)
      return this._error;
    let t3 = new T(s2.common.issues);
    return this._error = t3, this._error;
  } };
};
function y(s2) {
  if (!s2)
    return {};
  let { errorMap: e3, invalid_type_error: t3, required_error: r3, description: n4 } = s2;
  if (e3 && (t3 || r3))
    throw new Error(`Can't use "invalid_type_error" or "required_error" in conjunction with custom error map.`);
  return e3 ? { errorMap: e3, description: n4 } : { errorMap: (i3, o3) => i3.code !== "invalid_type" ? { message: o3.defaultError } : typeof o3.data > "u" ? { message: r3 ?? o3.defaultError } : { message: t3 ?? o3.defaultError }, description: n4 };
}
var v = class {
  constructor(e3) {
    this.spa = this.safeParseAsync, this._def = e3, this.parse = this.parse.bind(this), this.safeParse = this.safeParse.bind(this), this.parseAsync = this.parseAsync.bind(this), this.safeParseAsync = this.safeParseAsync.bind(this), this.spa = this.spa.bind(this), this.refine = this.refine.bind(this), this.refinement = this.refinement.bind(this), this.superRefine = this.superRefine.bind(this), this.optional = this.optional.bind(this), this.nullable = this.nullable.bind(this), this.nullish = this.nullish.bind(this), this.array = this.array.bind(this), this.promise = this.promise.bind(this), this.or = this.or.bind(this), this.and = this.and.bind(this), this.transform = this.transform.bind(this), this.brand = this.brand.bind(this), this.default = this.default.bind(this), this.catch = this.catch.bind(this), this.describe = this.describe.bind(this), this.pipe = this.pipe.bind(this), this.isNullable = this.isNullable.bind(this), this.isOptional = this.isOptional.bind(this);
  }
  get description() {
    return this._def.description;
  }
  _getType(e3) {
    return P(e3.data);
  }
  _getOrReturnCtx(e3, t3) {
    return t3 || { common: e3.parent.common, data: e3.data, parsedType: P(e3.data), schemaErrorMap: this._def.errorMap, path: e3.path, parent: e3.parent };
  }
  _processInputParams(e3) {
    return { status: new k(), ctx: { common: e3.parent.common, data: e3.data, parsedType: P(e3.data), schemaErrorMap: this._def.errorMap, path: e3.path, parent: e3.parent } };
  }
  _parseSync(e3) {
    let t3 = this._parse(e3);
    if (fe(t3))
      throw new Error("Synchronous parse encountered promise.");
    return t3;
  }
  _parseAsync(e3) {
    let t3 = this._parse(e3);
    return Promise.resolve(t3);
  }
  parse(e3, t3) {
    let r3 = this.safeParse(e3, t3);
    if (r3.success)
      return r3.data;
    throw r3.error;
  }
  safeParse(e3, t3) {
    var r3;
    let n4 = { common: { issues: [], async: (r3 = t3?.async) !== null && r3 !== void 0 ? r3 : false, contextualErrorMap: t3?.errorMap }, path: t3?.path || [], schemaErrorMap: this._def.errorMap, parent: null, data: e3, parsedType: P(e3) }, a4 = this._parseSync({ data: e3, path: n4.path, parent: n4 });
    return ge(n4, a4);
  }
  async parseAsync(e3, t3) {
    let r3 = await this.safeParseAsync(e3, t3);
    if (r3.success)
      return r3.data;
    throw r3.error;
  }
  async safeParseAsync(e3, t3) {
    let r3 = { common: { issues: [], contextualErrorMap: t3?.errorMap, async: true }, path: t3?.path || [], schemaErrorMap: this._def.errorMap, parent: null, data: e3, parsedType: P(e3) }, n4 = this._parse({ data: e3, path: r3.path, parent: r3 }), a4 = await (fe(n4) ? n4 : Promise.resolve(n4));
    return ge(r3, a4);
  }
  refine(e3, t3) {
    let r3 = (n4) => typeof t3 == "string" || typeof t3 > "u" ? { message: t3 } : typeof t3 == "function" ? t3(n4) : t3;
    return this._refinement((n4, a4) => {
      let i3 = e3(n4), o3 = () => a4.addIssue({ code: c.custom, ...r3(n4) });
      return typeof Promise < "u" && i3 instanceof Promise ? i3.then((f8) => f8 ? true : (o3(), false)) : i3 ? true : (o3(), false);
    });
  }
  refinement(e3, t3) {
    return this._refinement((r3, n4) => e3(r3) ? true : (n4.addIssue(typeof t3 == "function" ? t3(r3, n4) : t3), false));
  }
  _refinement(e3) {
    return new C({ schema: this, typeName: p.ZodEffects, effect: { type: "refinement", refinement: e3 } });
  }
  superRefine(e3) {
    return this._refinement(e3);
  }
  optional() {
    return E.create(this, this._def);
  }
  nullable() {
    return $.create(this, this._def);
  }
  nullish() {
    return this.nullable().optional();
  }
  array() {
    return S.create(this, this._def);
  }
  promise() {
    return D.create(this, this._def);
  }
  or(e3) {
    return q.create([this, e3], this._def);
  }
  and(e3) {
    return J.create(this, e3, this._def);
  }
  transform(e3) {
    return new C({ ...y(this._def), schema: this, typeName: p.ZodEffects, effect: { type: "transform", transform: e3 } });
  }
  default(e3) {
    let t3 = typeof e3 == "function" ? e3 : () => e3;
    return new K({ ...y(this._def), innerType: this, defaultValue: t3, typeName: p.ZodDefault });
  }
  brand() {
    return new he({ typeName: p.ZodBranded, type: this, ...y(this._def) });
  }
  catch(e3) {
    let t3 = typeof e3 == "function" ? e3 : () => e3;
    return new ae({ ...y(this._def), innerType: this, catchValue: t3, typeName: p.ZodCatch });
  }
  describe(e3) {
    let t3 = this.constructor;
    return new t3({ ...this._def, description: e3 });
  }
  pipe(e3) {
    return Q.create(this, e3);
  }
  isOptional() {
    return this.safeParse(void 0).success;
  }
  isNullable() {
    return this.safeParse(null).success;
  }
};
var je = /^c[^\s-]{8,}$/i;
var Re = /^[a-z][a-z0-9]*$/;
var Ae = /[0-9A-HJKMNP-TV-Z]{26}/;
var Ze = /^([a-f0-9]{8}-[a-f0-9]{4}-[1-5][a-f0-9]{3}-[a-f0-9]{4}-[a-f0-9]{12}|00000000-0000-0000-0000-000000000000)$/i;
var Me = /^(([^<>()[\]\\.,;:\s@\"]+(\.[^<>()[\]\\.,;:\s@\"]+)*)|(\".+\"))@((\[(((25[0-5])|(2[0-4][0-9])|(1[0-9]{2})|([0-9]{1,2}))\.){3}((25[0-5])|(2[0-4][0-9])|(1[0-9]{2})|([0-9]{1,2}))\])|(\[IPv6:(([a-f0-9]{1,4}:){7}|::([a-f0-9]{1,4}:){0,6}|([a-f0-9]{1,4}:){1}:([a-f0-9]{1,4}:){0,5}|([a-f0-9]{1,4}:){2}:([a-f0-9]{1,4}:){0,4}|([a-f0-9]{1,4}:){3}:([a-f0-9]{1,4}:){0,3}|([a-f0-9]{1,4}:){4}:([a-f0-9]{1,4}:){0,2}|([a-f0-9]{1,4}:){5}:([a-f0-9]{1,4}:){0,1})([a-f0-9]{1,4}|(((25[0-5])|(2[0-4][0-9])|(1[0-9]{2})|([0-9]{1,2}))\.){3}((25[0-5])|(2[0-4][0-9])|(1[0-9]{2})|([0-9]{1,2})))\])|([A-Za-z0-9]([A-Za-z0-9-]*[A-Za-z0-9])*(\.[A-Za-z]{2,})+))$/;
var Ve = /^(\p{Extended_Pictographic}|\p{Emoji_Component})+$/u;
var $e = /^(((25[0-5])|(2[0-4][0-9])|(1[0-9]{2})|([0-9]{1,2}))\.){3}((25[0-5])|(2[0-4][0-9])|(1[0-9]{2})|([0-9]{1,2}))$/;
var Pe = /^(([a-f0-9]{1,4}:){7}|::([a-f0-9]{1,4}:){0,6}|([a-f0-9]{1,4}:){1}:([a-f0-9]{1,4}:){0,5}|([a-f0-9]{1,4}:){2}:([a-f0-9]{1,4}:){0,4}|([a-f0-9]{1,4}:){3}:([a-f0-9]{1,4}:){0,3}|([a-f0-9]{1,4}:){4}:([a-f0-9]{1,4}:){0,2}|([a-f0-9]{1,4}:){5}:([a-f0-9]{1,4}:){0,1})([a-f0-9]{1,4}|(((25[0-5])|(2[0-4][0-9])|(1[0-9]{2})|([0-9]{1,2}))\.){3}((25[0-5])|(2[0-4][0-9])|(1[0-9]{2})|([0-9]{1,2})))$/;
var Le = (s2) => s2.precision ? s2.offset ? new RegExp(`^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{${s2.precision}}(([+-]\\d{2}(:?\\d{2})?)|Z)$`) : new RegExp(`^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{${s2.precision}}Z$`) : s2.precision === 0 ? s2.offset ? new RegExp("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(([+-]\\d{2}(:?\\d{2})?)|Z)$") : new RegExp("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z$") : s2.offset ? new RegExp("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?(([+-]\\d{2}(:?\\d{2})?)|Z)$") : new RegExp("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z$");
function ze(s2, e3) {
  return !!((e3 === "v4" || !e3) && $e.test(s2) || (e3 === "v6" || !e3) && Pe.test(s2));
}
var w = class extends v {
  constructor() {
    super(...arguments), this._regex = (e3, t3, r3) => this.refinement((n4) => e3.test(n4), { validation: t3, code: c.invalid_string, ...h.errToObj(r3) }), this.nonempty = (e3) => this.min(1, h.errToObj(e3)), this.trim = () => new w({ ...this._def, checks: [...this._def.checks, { kind: "trim" }] }), this.toLowerCase = () => new w({ ...this._def, checks: [...this._def.checks, { kind: "toLowerCase" }] }), this.toUpperCase = () => new w({ ...this._def, checks: [...this._def.checks, { kind: "toUpperCase" }] });
  }
  _parse(e3) {
    if (this._def.coerce && (e3.data = String(e3.data)), this._getType(e3) !== d.string) {
      let a4 = this._getOrReturnCtx(e3);
      return u(a4, { code: c.invalid_type, expected: d.string, received: a4.parsedType }), m;
    }
    let r3 = new k(), n4;
    for (let a4 of this._def.checks)
      if (a4.kind === "min")
        e3.data.length < a4.value && (n4 = this._getOrReturnCtx(e3, n4), u(n4, { code: c.too_small, minimum: a4.value, type: "string", inclusive: true, exact: false, message: a4.message }), r3.dirty());
      else if (a4.kind === "max")
        e3.data.length > a4.value && (n4 = this._getOrReturnCtx(e3, n4), u(n4, { code: c.too_big, maximum: a4.value, type: "string", inclusive: true, exact: false, message: a4.message }), r3.dirty());
      else if (a4.kind === "length") {
        let i3 = e3.data.length > a4.value, o3 = e3.data.length < a4.value;
        (i3 || o3) && (n4 = this._getOrReturnCtx(e3, n4), i3 ? u(n4, { code: c.too_big, maximum: a4.value, type: "string", inclusive: true, exact: true, message: a4.message }) : o3 && u(n4, { code: c.too_small, minimum: a4.value, type: "string", inclusive: true, exact: true, message: a4.message }), r3.dirty());
      } else if (a4.kind === "email")
        Me.test(e3.data) || (n4 = this._getOrReturnCtx(e3, n4), u(n4, { validation: "email", code: c.invalid_string, message: a4.message }), r3.dirty());
      else if (a4.kind === "emoji")
        Ve.test(e3.data) || (n4 = this._getOrReturnCtx(e3, n4), u(n4, { validation: "emoji", code: c.invalid_string, message: a4.message }), r3.dirty());
      else if (a4.kind === "uuid")
        Ze.test(e3.data) || (n4 = this._getOrReturnCtx(e3, n4), u(n4, { validation: "uuid", code: c.invalid_string, message: a4.message }), r3.dirty());
      else if (a4.kind === "cuid")
        je.test(e3.data) || (n4 = this._getOrReturnCtx(e3, n4), u(n4, { validation: "cuid", code: c.invalid_string, message: a4.message }), r3.dirty());
      else if (a4.kind === "cuid2")
        Re.test(e3.data) || (n4 = this._getOrReturnCtx(e3, n4), u(n4, { validation: "cuid2", code: c.invalid_string, message: a4.message }), r3.dirty());
      else if (a4.kind === "ulid")
        Ae.test(e3.data) || (n4 = this._getOrReturnCtx(e3, n4), u(n4, { validation: "ulid", code: c.invalid_string, message: a4.message }), r3.dirty());
      else if (a4.kind === "url")
        try {
          new URL(e3.data);
        } catch {
          n4 = this._getOrReturnCtx(e3, n4), u(n4, { validation: "url", code: c.invalid_string, message: a4.message }), r3.dirty();
        }
      else
        a4.kind === "regex" ? (a4.regex.lastIndex = 0, a4.regex.test(e3.data) || (n4 = this._getOrReturnCtx(e3, n4), u(n4, { validation: "regex", code: c.invalid_string, message: a4.message }), r3.dirty())) : a4.kind === "trim" ? e3.data = e3.data.trim() : a4.kind === "includes" ? e3.data.includes(a4.value, a4.position) || (n4 = this._getOrReturnCtx(e3, n4), u(n4, { code: c.invalid_string, validation: { includes: a4.value, position: a4.position }, message: a4.message }), r3.dirty()) : a4.kind === "toLowerCase" ? e3.data = e3.data.toLowerCase() : a4.kind === "toUpperCase" ? e3.data = e3.data.toUpperCase() : a4.kind === "startsWith" ? e3.data.startsWith(a4.value) || (n4 = this._getOrReturnCtx(e3, n4), u(n4, { code: c.invalid_string, validation: { startsWith: a4.value }, message: a4.message }), r3.dirty()) : a4.kind === "endsWith" ? e3.data.endsWith(a4.value) || (n4 = this._getOrReturnCtx(e3, n4), u(n4, { code: c.invalid_string, validation: { endsWith: a4.value }, message: a4.message }), r3.dirty()) : a4.kind === "datetime" ? Le(a4).test(e3.data) || (n4 = this._getOrReturnCtx(e3, n4), u(n4, { code: c.invalid_string, validation: "datetime", message: a4.message }), r3.dirty()) : a4.kind === "ip" ? ze(e3.data, a4.version) || (n4 = this._getOrReturnCtx(e3, n4), u(n4, { validation: "ip", code: c.invalid_string, message: a4.message }), r3.dirty()) : g.assertNever(a4);
    return { status: r3.value, value: e3.data };
  }
  _addCheck(e3) {
    return new w({ ...this._def, checks: [...this._def.checks, e3] });
  }
  email(e3) {
    return this._addCheck({ kind: "email", ...h.errToObj(e3) });
  }
  url(e3) {
    return this._addCheck({ kind: "url", ...h.errToObj(e3) });
  }
  emoji(e3) {
    return this._addCheck({ kind: "emoji", ...h.errToObj(e3) });
  }
  uuid(e3) {
    return this._addCheck({ kind: "uuid", ...h.errToObj(e3) });
  }
  cuid(e3) {
    return this._addCheck({ kind: "cuid", ...h.errToObj(e3) });
  }
  cuid2(e3) {
    return this._addCheck({ kind: "cuid2", ...h.errToObj(e3) });
  }
  ulid(e3) {
    return this._addCheck({ kind: "ulid", ...h.errToObj(e3) });
  }
  ip(e3) {
    return this._addCheck({ kind: "ip", ...h.errToObj(e3) });
  }
  datetime(e3) {
    var t3;
    return typeof e3 == "string" ? this._addCheck({ kind: "datetime", precision: null, offset: false, message: e3 }) : this._addCheck({ kind: "datetime", precision: typeof e3?.precision > "u" ? null : e3?.precision, offset: (t3 = e3?.offset) !== null && t3 !== void 0 ? t3 : false, ...h.errToObj(e3?.message) });
  }
  regex(e3, t3) {
    return this._addCheck({ kind: "regex", regex: e3, ...h.errToObj(t3) });
  }
  includes(e3, t3) {
    return this._addCheck({ kind: "includes", value: e3, position: t3?.position, ...h.errToObj(t3?.message) });
  }
  startsWith(e3, t3) {
    return this._addCheck({ kind: "startsWith", value: e3, ...h.errToObj(t3) });
  }
  endsWith(e3, t3) {
    return this._addCheck({ kind: "endsWith", value: e3, ...h.errToObj(t3) });
  }
  min(e3, t3) {
    return this._addCheck({ kind: "min", value: e3, ...h.errToObj(t3) });
  }
  max(e3, t3) {
    return this._addCheck({ kind: "max", value: e3, ...h.errToObj(t3) });
  }
  length(e3, t3) {
    return this._addCheck({ kind: "length", value: e3, ...h.errToObj(t3) });
  }
  get isDatetime() {
    return !!this._def.checks.find((e3) => e3.kind === "datetime");
  }
  get isEmail() {
    return !!this._def.checks.find((e3) => e3.kind === "email");
  }
  get isURL() {
    return !!this._def.checks.find((e3) => e3.kind === "url");
  }
  get isEmoji() {
    return !!this._def.checks.find((e3) => e3.kind === "emoji");
  }
  get isUUID() {
    return !!this._def.checks.find((e3) => e3.kind === "uuid");
  }
  get isCUID() {
    return !!this._def.checks.find((e3) => e3.kind === "cuid");
  }
  get isCUID2() {
    return !!this._def.checks.find((e3) => e3.kind === "cuid2");
  }
  get isULID() {
    return !!this._def.checks.find((e3) => e3.kind === "ulid");
  }
  get isIP() {
    return !!this._def.checks.find((e3) => e3.kind === "ip");
  }
  get minLength() {
    let e3 = null;
    for (let t3 of this._def.checks)
      t3.kind === "min" && (e3 === null || t3.value > e3) && (e3 = t3.value);
    return e3;
  }
  get maxLength() {
    let e3 = null;
    for (let t3 of this._def.checks)
      t3.kind === "max" && (e3 === null || t3.value < e3) && (e3 = t3.value);
    return e3;
  }
};
w.create = (s2) => {
  var e3;
  return new w({ checks: [], typeName: p.ZodString, coerce: (e3 = s2?.coerce) !== null && e3 !== void 0 ? e3 : false, ...y(s2) });
};
function De(s2, e3) {
  let t3 = (s2.toString().split(".")[1] || "").length, r3 = (e3.toString().split(".")[1] || "").length, n4 = t3 > r3 ? t3 : r3, a4 = parseInt(s2.toFixed(n4).replace(".", "")), i3 = parseInt(e3.toFixed(n4).replace(".", ""));
  return a4 % i3 / Math.pow(10, n4);
}
var j = class extends v {
  constructor() {
    super(...arguments), this.min = this.gte, this.max = this.lte, this.step = this.multipleOf;
  }
  _parse(e3) {
    if (this._def.coerce && (e3.data = Number(e3.data)), this._getType(e3) !== d.number) {
      let a4 = this._getOrReturnCtx(e3);
      return u(a4, { code: c.invalid_type, expected: d.number, received: a4.parsedType }), m;
    }
    let r3, n4 = new k();
    for (let a4 of this._def.checks)
      a4.kind === "int" ? g.isInteger(e3.data) || (r3 = this._getOrReturnCtx(e3, r3), u(r3, { code: c.invalid_type, expected: "integer", received: "float", message: a4.message }), n4.dirty()) : a4.kind === "min" ? (a4.inclusive ? e3.data < a4.value : e3.data <= a4.value) && (r3 = this._getOrReturnCtx(e3, r3), u(r3, { code: c.too_small, minimum: a4.value, type: "number", inclusive: a4.inclusive, exact: false, message: a4.message }), n4.dirty()) : a4.kind === "max" ? (a4.inclusive ? e3.data > a4.value : e3.data >= a4.value) && (r3 = this._getOrReturnCtx(e3, r3), u(r3, { code: c.too_big, maximum: a4.value, type: "number", inclusive: a4.inclusive, exact: false, message: a4.message }), n4.dirty()) : a4.kind === "multipleOf" ? De(e3.data, a4.value) !== 0 && (r3 = this._getOrReturnCtx(e3, r3), u(r3, { code: c.not_multiple_of, multipleOf: a4.value, message: a4.message }), n4.dirty()) : a4.kind === "finite" ? Number.isFinite(e3.data) || (r3 = this._getOrReturnCtx(e3, r3), u(r3, { code: c.not_finite, message: a4.message }), n4.dirty()) : g.assertNever(a4);
    return { status: n4.value, value: e3.data };
  }
  gte(e3, t3) {
    return this.setLimit("min", e3, true, h.toString(t3));
  }
  gt(e3, t3) {
    return this.setLimit("min", e3, false, h.toString(t3));
  }
  lte(e3, t3) {
    return this.setLimit("max", e3, true, h.toString(t3));
  }
  lt(e3, t3) {
    return this.setLimit("max", e3, false, h.toString(t3));
  }
  setLimit(e3, t3, r3, n4) {
    return new j({ ...this._def, checks: [...this._def.checks, { kind: e3, value: t3, inclusive: r3, message: h.toString(n4) }] });
  }
  _addCheck(e3) {
    return new j({ ...this._def, checks: [...this._def.checks, e3] });
  }
  int(e3) {
    return this._addCheck({ kind: "int", message: h.toString(e3) });
  }
  positive(e3) {
    return this._addCheck({ kind: "min", value: 0, inclusive: false, message: h.toString(e3) });
  }
  negative(e3) {
    return this._addCheck({ kind: "max", value: 0, inclusive: false, message: h.toString(e3) });
  }
  nonpositive(e3) {
    return this._addCheck({ kind: "max", value: 0, inclusive: true, message: h.toString(e3) });
  }
  nonnegative(e3) {
    return this._addCheck({ kind: "min", value: 0, inclusive: true, message: h.toString(e3) });
  }
  multipleOf(e3, t3) {
    return this._addCheck({ kind: "multipleOf", value: e3, message: h.toString(t3) });
  }
  finite(e3) {
    return this._addCheck({ kind: "finite", message: h.toString(e3) });
  }
  safe(e3) {
    return this._addCheck({ kind: "min", inclusive: true, value: Number.MIN_SAFE_INTEGER, message: h.toString(e3) })._addCheck({ kind: "max", inclusive: true, value: Number.MAX_SAFE_INTEGER, message: h.toString(e3) });
  }
  get minValue() {
    let e3 = null;
    for (let t3 of this._def.checks)
      t3.kind === "min" && (e3 === null || t3.value > e3) && (e3 = t3.value);
    return e3;
  }
  get maxValue() {
    let e3 = null;
    for (let t3 of this._def.checks)
      t3.kind === "max" && (e3 === null || t3.value < e3) && (e3 = t3.value);
    return e3;
  }
  get isInt() {
    return !!this._def.checks.find((e3) => e3.kind === "int" || e3.kind === "multipleOf" && g.isInteger(e3.value));
  }
  get isFinite() {
    let e3 = null, t3 = null;
    for (let r3 of this._def.checks) {
      if (r3.kind === "finite" || r3.kind === "int" || r3.kind === "multipleOf")
        return true;
      r3.kind === "min" ? (t3 === null || r3.value > t3) && (t3 = r3.value) : r3.kind === "max" && (e3 === null || r3.value < e3) && (e3 = r3.value);
    }
    return Number.isFinite(t3) && Number.isFinite(e3);
  }
};
j.create = (s2) => new j({ checks: [], typeName: p.ZodNumber, coerce: s2?.coerce || false, ...y(s2) });
var R = class extends v {
  constructor() {
    super(...arguments), this.min = this.gte, this.max = this.lte;
  }
  _parse(e3) {
    if (this._def.coerce && (e3.data = BigInt(e3.data)), this._getType(e3) !== d.bigint) {
      let a4 = this._getOrReturnCtx(e3);
      return u(a4, { code: c.invalid_type, expected: d.bigint, received: a4.parsedType }), m;
    }
    let r3, n4 = new k();
    for (let a4 of this._def.checks)
      a4.kind === "min" ? (a4.inclusive ? e3.data < a4.value : e3.data <= a4.value) && (r3 = this._getOrReturnCtx(e3, r3), u(r3, { code: c.too_small, type: "bigint", minimum: a4.value, inclusive: a4.inclusive, message: a4.message }), n4.dirty()) : a4.kind === "max" ? (a4.inclusive ? e3.data > a4.value : e3.data >= a4.value) && (r3 = this._getOrReturnCtx(e3, r3), u(r3, { code: c.too_big, type: "bigint", maximum: a4.value, inclusive: a4.inclusive, message: a4.message }), n4.dirty()) : a4.kind === "multipleOf" ? e3.data % a4.value !== BigInt(0) && (r3 = this._getOrReturnCtx(e3, r3), u(r3, { code: c.not_multiple_of, multipleOf: a4.value, message: a4.message }), n4.dirty()) : g.assertNever(a4);
    return { status: n4.value, value: e3.data };
  }
  gte(e3, t3) {
    return this.setLimit("min", e3, true, h.toString(t3));
  }
  gt(e3, t3) {
    return this.setLimit("min", e3, false, h.toString(t3));
  }
  lte(e3, t3) {
    return this.setLimit("max", e3, true, h.toString(t3));
  }
  lt(e3, t3) {
    return this.setLimit("max", e3, false, h.toString(t3));
  }
  setLimit(e3, t3, r3, n4) {
    return new R({ ...this._def, checks: [...this._def.checks, { kind: e3, value: t3, inclusive: r3, message: h.toString(n4) }] });
  }
  _addCheck(e3) {
    return new R({ ...this._def, checks: [...this._def.checks, e3] });
  }
  positive(e3) {
    return this._addCheck({ kind: "min", value: BigInt(0), inclusive: false, message: h.toString(e3) });
  }
  negative(e3) {
    return this._addCheck({ kind: "max", value: BigInt(0), inclusive: false, message: h.toString(e3) });
  }
  nonpositive(e3) {
    return this._addCheck({ kind: "max", value: BigInt(0), inclusive: true, message: h.toString(e3) });
  }
  nonnegative(e3) {
    return this._addCheck({ kind: "min", value: BigInt(0), inclusive: true, message: h.toString(e3) });
  }
  multipleOf(e3, t3) {
    return this._addCheck({ kind: "multipleOf", value: e3, message: h.toString(t3) });
  }
  get minValue() {
    let e3 = null;
    for (let t3 of this._def.checks)
      t3.kind === "min" && (e3 === null || t3.value > e3) && (e3 = t3.value);
    return e3;
  }
  get maxValue() {
    let e3 = null;
    for (let t3 of this._def.checks)
      t3.kind === "max" && (e3 === null || t3.value < e3) && (e3 = t3.value);
    return e3;
  }
};
R.create = (s2) => {
  var e3;
  return new R({ checks: [], typeName: p.ZodBigInt, coerce: (e3 = s2?.coerce) !== null && e3 !== void 0 ? e3 : false, ...y(s2) });
};
var U = class extends v {
  _parse(e3) {
    if (this._def.coerce && (e3.data = !!e3.data), this._getType(e3) !== d.boolean) {
      let r3 = this._getOrReturnCtx(e3);
      return u(r3, { code: c.invalid_type, expected: d.boolean, received: r3.parsedType }), m;
    }
    return b(e3.data);
  }
};
U.create = (s2) => new U({ typeName: p.ZodBoolean, coerce: s2?.coerce || false, ...y(s2) });
var M = class extends v {
  _parse(e3) {
    if (this._def.coerce && (e3.data = new Date(e3.data)), this._getType(e3) !== d.date) {
      let a4 = this._getOrReturnCtx(e3);
      return u(a4, { code: c.invalid_type, expected: d.date, received: a4.parsedType }), m;
    }
    if (isNaN(e3.data.getTime())) {
      let a4 = this._getOrReturnCtx(e3);
      return u(a4, { code: c.invalid_date }), m;
    }
    let r3 = new k(), n4;
    for (let a4 of this._def.checks)
      a4.kind === "min" ? e3.data.getTime() < a4.value && (n4 = this._getOrReturnCtx(e3, n4), u(n4, { code: c.too_small, message: a4.message, inclusive: true, exact: false, minimum: a4.value, type: "date" }), r3.dirty()) : a4.kind === "max" ? e3.data.getTime() > a4.value && (n4 = this._getOrReturnCtx(e3, n4), u(n4, { code: c.too_big, message: a4.message, inclusive: true, exact: false, maximum: a4.value, type: "date" }), r3.dirty()) : g.assertNever(a4);
    return { status: r3.value, value: new Date(e3.data.getTime()) };
  }
  _addCheck(e3) {
    return new M({ ...this._def, checks: [...this._def.checks, e3] });
  }
  min(e3, t3) {
    return this._addCheck({ kind: "min", value: e3.getTime(), message: h.toString(t3) });
  }
  max(e3, t3) {
    return this._addCheck({ kind: "max", value: e3.getTime(), message: h.toString(t3) });
  }
  get minDate() {
    let e3 = null;
    for (let t3 of this._def.checks)
      t3.kind === "min" && (e3 === null || t3.value > e3) && (e3 = t3.value);
    return e3 != null ? new Date(e3) : null;
  }
  get maxDate() {
    let e3 = null;
    for (let t3 of this._def.checks)
      t3.kind === "max" && (e3 === null || t3.value < e3) && (e3 = t3.value);
    return e3 != null ? new Date(e3) : null;
  }
};
M.create = (s2) => new M({ checks: [], coerce: s2?.coerce || false, typeName: p.ZodDate, ...y(s2) });
var te = class extends v {
  _parse(e3) {
    if (this._getType(e3) !== d.symbol) {
      let r3 = this._getOrReturnCtx(e3);
      return u(r3, { code: c.invalid_type, expected: d.symbol, received: r3.parsedType }), m;
    }
    return b(e3.data);
  }
};
te.create = (s2) => new te({ typeName: p.ZodSymbol, ...y(s2) });
var B = class extends v {
  _parse(e3) {
    if (this._getType(e3) !== d.undefined) {
      let r3 = this._getOrReturnCtx(e3);
      return u(r3, { code: c.invalid_type, expected: d.undefined, received: r3.parsedType }), m;
    }
    return b(e3.data);
  }
};
B.create = (s2) => new B({ typeName: p.ZodUndefined, ...y(s2) });
var W = class extends v {
  _parse(e3) {
    if (this._getType(e3) !== d.null) {
      let r3 = this._getOrReturnCtx(e3);
      return u(r3, { code: c.invalid_type, expected: d.null, received: r3.parsedType }), m;
    }
    return b(e3.data);
  }
};
W.create = (s2) => new W({ typeName: p.ZodNull, ...y(s2) });
var z = class extends v {
  constructor() {
    super(...arguments), this._any = true;
  }
  _parse(e3) {
    return b(e3.data);
  }
};
z.create = (s2) => new z({ typeName: p.ZodAny, ...y(s2) });
var Z = class extends v {
  constructor() {
    super(...arguments), this._unknown = true;
  }
  _parse(e3) {
    return b(e3.data);
  }
};
Z.create = (s2) => new Z({ typeName: p.ZodUnknown, ...y(s2) });
var I = class extends v {
  _parse(e3) {
    let t3 = this._getOrReturnCtx(e3);
    return u(t3, { code: c.invalid_type, expected: d.never, received: t3.parsedType }), m;
  }
};
I.create = (s2) => new I({ typeName: p.ZodNever, ...y(s2) });
var se = class extends v {
  _parse(e3) {
    if (this._getType(e3) !== d.undefined) {
      let r3 = this._getOrReturnCtx(e3);
      return u(r3, { code: c.invalid_type, expected: d.void, received: r3.parsedType }), m;
    }
    return b(e3.data);
  }
};
se.create = (s2) => new se({ typeName: p.ZodVoid, ...y(s2) });
var S = class extends v {
  _parse(e3) {
    let { ctx: t3, status: r3 } = this._processInputParams(e3), n4 = this._def;
    if (t3.parsedType !== d.array)
      return u(t3, { code: c.invalid_type, expected: d.array, received: t3.parsedType }), m;
    if (n4.exactLength !== null) {
      let i3 = t3.data.length > n4.exactLength.value, o3 = t3.data.length < n4.exactLength.value;
      (i3 || o3) && (u(t3, { code: i3 ? c.too_big : c.too_small, minimum: o3 ? n4.exactLength.value : void 0, maximum: i3 ? n4.exactLength.value : void 0, type: "array", inclusive: true, exact: true, message: n4.exactLength.message }), r3.dirty());
    }
    if (n4.minLength !== null && t3.data.length < n4.minLength.value && (u(t3, { code: c.too_small, minimum: n4.minLength.value, type: "array", inclusive: true, exact: false, message: n4.minLength.message }), r3.dirty()), n4.maxLength !== null && t3.data.length > n4.maxLength.value && (u(t3, { code: c.too_big, maximum: n4.maxLength.value, type: "array", inclusive: true, exact: false, message: n4.maxLength.message }), r3.dirty()), t3.common.async)
      return Promise.all([...t3.data].map((i3, o3) => n4.type._parseAsync(new O(t3, i3, t3.path, o3)))).then((i3) => k.mergeArray(r3, i3));
    let a4 = [...t3.data].map((i3, o3) => n4.type._parseSync(new O(t3, i3, t3.path, o3)));
    return k.mergeArray(r3, a4);
  }
  get element() {
    return this._def.type;
  }
  min(e3, t3) {
    return new S({ ...this._def, minLength: { value: e3, message: h.toString(t3) } });
  }
  max(e3, t3) {
    return new S({ ...this._def, maxLength: { value: e3, message: h.toString(t3) } });
  }
  length(e3, t3) {
    return new S({ ...this._def, exactLength: { value: e3, message: h.toString(t3) } });
  }
  nonempty(e3) {
    return this.min(1, e3);
  }
};
S.create = (s2, e3) => new S({ type: s2, minLength: null, maxLength: null, exactLength: null, typeName: p.ZodArray, ...y(e3) });
function ee(s2) {
  if (s2 instanceof x) {
    let e3 = {};
    for (let t3 in s2.shape) {
      let r3 = s2.shape[t3];
      e3[t3] = E.create(ee(r3));
    }
    return new x({ ...s2._def, shape: () => e3 });
  } else
    return s2 instanceof S ? new S({ ...s2._def, type: ee(s2.element) }) : s2 instanceof E ? E.create(ee(s2.unwrap())) : s2 instanceof $ ? $.create(ee(s2.unwrap())) : s2 instanceof N ? N.create(s2.items.map((e3) => ee(e3))) : s2;
}
var x = class extends v {
  constructor() {
    super(...arguments), this._cached = null, this.nonstrict = this.passthrough, this.augment = this.extend;
  }
  _getCached() {
    if (this._cached !== null)
      return this._cached;
    let e3 = this._def.shape(), t3 = g.objectKeys(e3);
    return this._cached = { shape: e3, keys: t3 };
  }
  _parse(e3) {
    if (this._getType(e3) !== d.object) {
      let l5 = this._getOrReturnCtx(e3);
      return u(l5, { code: c.invalid_type, expected: d.object, received: l5.parsedType }), m;
    }
    let { status: r3, ctx: n4 } = this._processInputParams(e3), { shape: a4, keys: i3 } = this._getCached(), o3 = [];
    if (!(this._def.catchall instanceof I && this._def.unknownKeys === "strip"))
      for (let l5 in n4.data)
        i3.includes(l5) || o3.push(l5);
    let f8 = [];
    for (let l5 of i3) {
      let _10 = a4[l5], F7 = n4.data[l5];
      f8.push({ key: { status: "valid", value: l5 }, value: _10._parse(new O(n4, F7, n4.path, l5)), alwaysSet: l5 in n4.data });
    }
    if (this._def.catchall instanceof I) {
      let l5 = this._def.unknownKeys;
      if (l5 === "passthrough")
        for (let _10 of o3)
          f8.push({ key: { status: "valid", value: _10 }, value: { status: "valid", value: n4.data[_10] } });
      else if (l5 === "strict")
        o3.length > 0 && (u(n4, { code: c.unrecognized_keys, keys: o3 }), r3.dirty());
      else if (l5 !== "strip")
        throw new Error("Internal ZodObject error: invalid unknownKeys value.");
    } else {
      let l5 = this._def.catchall;
      for (let _10 of o3) {
        let F7 = n4.data[_10];
        f8.push({ key: { status: "valid", value: _10 }, value: l5._parse(new O(n4, F7, n4.path, _10)), alwaysSet: _10 in n4.data });
      }
    }
    return n4.common.async ? Promise.resolve().then(async () => {
      let l5 = [];
      for (let _10 of f8) {
        let F7 = await _10.key;
        l5.push({ key: F7, value: await _10.value, alwaysSet: _10.alwaysSet });
      }
      return l5;
    }).then((l5) => k.mergeObjectSync(r3, l5)) : k.mergeObjectSync(r3, f8);
  }
  get shape() {
    return this._def.shape();
  }
  strict(e3) {
    return h.errToObj, new x({ ...this._def, unknownKeys: "strict", ...e3 !== void 0 ? { errorMap: (t3, r3) => {
      var n4, a4, i3, o3;
      let f8 = (i3 = (a4 = (n4 = this._def).errorMap) === null || a4 === void 0 ? void 0 : a4.call(n4, t3, r3).message) !== null && i3 !== void 0 ? i3 : r3.defaultError;
      return t3.code === "unrecognized_keys" ? { message: (o3 = h.errToObj(e3).message) !== null && o3 !== void 0 ? o3 : f8 } : { message: f8 };
    } } : {} });
  }
  strip() {
    return new x({ ...this._def, unknownKeys: "strip" });
  }
  passthrough() {
    return new x({ ...this._def, unknownKeys: "passthrough" });
  }
  extend(e3) {
    return new x({ ...this._def, shape: () => ({ ...this._def.shape(), ...e3 }) });
  }
  merge(e3) {
    return new x({ unknownKeys: e3._def.unknownKeys, catchall: e3._def.catchall, shape: () => ({ ...this._def.shape(), ...e3._def.shape() }), typeName: p.ZodObject });
  }
  setKey(e3, t3) {
    return this.augment({ [e3]: t3 });
  }
  catchall(e3) {
    return new x({ ...this._def, catchall: e3 });
  }
  pick(e3) {
    let t3 = {};
    return g.objectKeys(e3).forEach((r3) => {
      e3[r3] && this.shape[r3] && (t3[r3] = this.shape[r3]);
    }), new x({ ...this._def, shape: () => t3 });
  }
  omit(e3) {
    let t3 = {};
    return g.objectKeys(this.shape).forEach((r3) => {
      e3[r3] || (t3[r3] = this.shape[r3]);
    }), new x({ ...this._def, shape: () => t3 });
  }
  deepPartial() {
    return ee(this);
  }
  partial(e3) {
    let t3 = {};
    return g.objectKeys(this.shape).forEach((r3) => {
      let n4 = this.shape[r3];
      e3 && !e3[r3] ? t3[r3] = n4 : t3[r3] = n4.optional();
    }), new x({ ...this._def, shape: () => t3 });
  }
  required(e3) {
    let t3 = {};
    return g.objectKeys(this.shape).forEach((r3) => {
      if (e3 && !e3[r3])
        t3[r3] = this.shape[r3];
      else {
        let a4 = this.shape[r3];
        for (; a4 instanceof E; )
          a4 = a4._def.innerType;
        t3[r3] = a4;
      }
    }), new x({ ...this._def, shape: () => t3 });
  }
  keyof() {
    return we(g.objectKeys(this.shape));
  }
};
x.create = (s2, e3) => new x({ shape: () => s2, unknownKeys: "strip", catchall: I.create(), typeName: p.ZodObject, ...y(e3) });
x.strictCreate = (s2, e3) => new x({ shape: () => s2, unknownKeys: "strict", catchall: I.create(), typeName: p.ZodObject, ...y(e3) });
x.lazycreate = (s2, e3) => new x({ shape: s2, unknownKeys: "strip", catchall: I.create(), typeName: p.ZodObject, ...y(e3) });
var q = class extends v {
  _parse(e3) {
    let { ctx: t3 } = this._processInputParams(e3), r3 = this._def.options;
    function n4(a4) {
      for (let o3 of a4)
        if (o3.result.status === "valid")
          return o3.result;
      for (let o3 of a4)
        if (o3.result.status === "dirty")
          return t3.common.issues.push(...o3.ctx.common.issues), o3.result;
      let i3 = a4.map((o3) => new T(o3.ctx.common.issues));
      return u(t3, { code: c.invalid_union, unionErrors: i3 }), m;
    }
    if (t3.common.async)
      return Promise.all(r3.map(async (a4) => {
        let i3 = { ...t3, common: { ...t3.common, issues: [] }, parent: null };
        return { result: await a4._parseAsync({ data: t3.data, path: t3.path, parent: i3 }), ctx: i3 };
      })).then(n4);
    {
      let a4, i3 = [];
      for (let f8 of r3) {
        let l5 = { ...t3, common: { ...t3.common, issues: [] }, parent: null }, _10 = f8._parseSync({ data: t3.data, path: t3.path, parent: l5 });
        if (_10.status === "valid")
          return _10;
        _10.status === "dirty" && !a4 && (a4 = { result: _10, ctx: l5 }), l5.common.issues.length && i3.push(l5.common.issues);
      }
      if (a4)
        return t3.common.issues.push(...a4.ctx.common.issues), a4.result;
      let o3 = i3.map((f8) => new T(f8));
      return u(t3, { code: c.invalid_union, unionErrors: o3 }), m;
    }
  }
  get options() {
    return this._def.options;
  }
};
q.create = (s2, e3) => new q({ options: s2, typeName: p.ZodUnion, ...y(e3) });
var ce = (s2) => s2 instanceof H ? ce(s2.schema) : s2 instanceof C ? ce(s2.innerType()) : s2 instanceof G ? [s2.value] : s2 instanceof A ? s2.options : s2 instanceof X ? Object.keys(s2.enum) : s2 instanceof K ? ce(s2._def.innerType) : s2 instanceof B ? [void 0] : s2 instanceof W ? [null] : null;
var re = class extends v {
  _parse(e3) {
    let { ctx: t3 } = this._processInputParams(e3);
    if (t3.parsedType !== d.object)
      return u(t3, { code: c.invalid_type, expected: d.object, received: t3.parsedType }), m;
    let r3 = this.discriminator, n4 = t3.data[r3], a4 = this.optionsMap.get(n4);
    return a4 ? t3.common.async ? a4._parseAsync({ data: t3.data, path: t3.path, parent: t3 }) : a4._parseSync({ data: t3.data, path: t3.path, parent: t3 }) : (u(t3, { code: c.invalid_union_discriminator, options: Array.from(this.optionsMap.keys()), path: [r3] }), m);
  }
  get discriminator() {
    return this._def.discriminator;
  }
  get options() {
    return this._def.options;
  }
  get optionsMap() {
    return this._def.optionsMap;
  }
  static create(e3, t3, r3) {
    let n4 = /* @__PURE__ */ new Map();
    for (let a4 of t3) {
      let i3 = ce(a4.shape[e3]);
      if (!i3)
        throw new Error(`A discriminator value for key \`${e3}\` could not be extracted from all schema options`);
      for (let o3 of i3) {
        if (n4.has(o3))
          throw new Error(`Discriminator property ${String(e3)} has duplicate value ${String(o3)}`);
        n4.set(o3, a4);
      }
    }
    return new re({ typeName: p.ZodDiscriminatedUnion, discriminator: e3, options: t3, optionsMap: n4, ...y(r3) });
  }
};
function _e(s2, e3) {
  let t3 = P(s2), r3 = P(e3);
  if (s2 === e3)
    return { valid: true, data: s2 };
  if (t3 === d.object && r3 === d.object) {
    let n4 = g.objectKeys(e3), a4 = g.objectKeys(s2).filter((o3) => n4.indexOf(o3) !== -1), i3 = { ...s2, ...e3 };
    for (let o3 of a4) {
      let f8 = _e(s2[o3], e3[o3]);
      if (!f8.valid)
        return { valid: false };
      i3[o3] = f8.data;
    }
    return { valid: true, data: i3 };
  } else if (t3 === d.array && r3 === d.array) {
    if (s2.length !== e3.length)
      return { valid: false };
    let n4 = [];
    for (let a4 = 0; a4 < s2.length; a4++) {
      let i3 = s2[a4], o3 = e3[a4], f8 = _e(i3, o3);
      if (!f8.valid)
        return { valid: false };
      n4.push(f8.data);
    }
    return { valid: true, data: n4 };
  } else
    return t3 === d.date && r3 === d.date && +s2 == +e3 ? { valid: true, data: s2 } : { valid: false };
}
var J = class extends v {
  _parse(e3) {
    let { status: t3, ctx: r3 } = this._processInputParams(e3), n4 = (a4, i3) => {
      if (ye(a4) || ye(i3))
        return m;
      let o3 = _e(a4.value, i3.value);
      return o3.valid ? ((ve(a4) || ve(i3)) && t3.dirty(), { status: t3.value, value: o3.data }) : (u(r3, { code: c.invalid_intersection_types }), m);
    };
    return r3.common.async ? Promise.all([this._def.left._parseAsync({ data: r3.data, path: r3.path, parent: r3 }), this._def.right._parseAsync({ data: r3.data, path: r3.path, parent: r3 })]).then(([a4, i3]) => n4(a4, i3)) : n4(this._def.left._parseSync({ data: r3.data, path: r3.path, parent: r3 }), this._def.right._parseSync({ data: r3.data, path: r3.path, parent: r3 }));
  }
};
J.create = (s2, e3, t3) => new J({ left: s2, right: e3, typeName: p.ZodIntersection, ...y(t3) });
var N = class extends v {
  _parse(e3) {
    let { status: t3, ctx: r3 } = this._processInputParams(e3);
    if (r3.parsedType !== d.array)
      return u(r3, { code: c.invalid_type, expected: d.array, received: r3.parsedType }), m;
    if (r3.data.length < this._def.items.length)
      return u(r3, { code: c.too_small, minimum: this._def.items.length, inclusive: true, exact: false, type: "array" }), m;
    !this._def.rest && r3.data.length > this._def.items.length && (u(r3, { code: c.too_big, maximum: this._def.items.length, inclusive: true, exact: false, type: "array" }), t3.dirty());
    let a4 = [...r3.data].map((i3, o3) => {
      let f8 = this._def.items[o3] || this._def.rest;
      return f8 ? f8._parse(new O(r3, i3, r3.path, o3)) : null;
    }).filter((i3) => !!i3);
    return r3.common.async ? Promise.all(a4).then((i3) => k.mergeArray(t3, i3)) : k.mergeArray(t3, a4);
  }
  get items() {
    return this._def.items;
  }
  rest(e3) {
    return new N({ ...this._def, rest: e3 });
  }
};
N.create = (s2, e3) => {
  if (!Array.isArray(s2))
    throw new Error("You must pass an array of schemas to z.tuple([ ... ])");
  return new N({ items: s2, typeName: p.ZodTuple, rest: null, ...y(e3) });
};
var Y = class extends v {
  get keySchema() {
    return this._def.keyType;
  }
  get valueSchema() {
    return this._def.valueType;
  }
  _parse(e3) {
    let { status: t3, ctx: r3 } = this._processInputParams(e3);
    if (r3.parsedType !== d.object)
      return u(r3, { code: c.invalid_type, expected: d.object, received: r3.parsedType }), m;
    let n4 = [], a4 = this._def.keyType, i3 = this._def.valueType;
    for (let o3 in r3.data)
      n4.push({ key: a4._parse(new O(r3, o3, r3.path, o3)), value: i3._parse(new O(r3, r3.data[o3], r3.path, o3)) });
    return r3.common.async ? k.mergeObjectAsync(t3, n4) : k.mergeObjectSync(t3, n4);
  }
  get element() {
    return this._def.valueType;
  }
  static create(e3, t3, r3) {
    return t3 instanceof v ? new Y({ keyType: e3, valueType: t3, typeName: p.ZodRecord, ...y(r3) }) : new Y({ keyType: w.create(), valueType: e3, typeName: p.ZodRecord, ...y(t3) });
  }
};
var ne = class extends v {
  _parse(e3) {
    let { status: t3, ctx: r3 } = this._processInputParams(e3);
    if (r3.parsedType !== d.map)
      return u(r3, { code: c.invalid_type, expected: d.map, received: r3.parsedType }), m;
    let n4 = this._def.keyType, a4 = this._def.valueType, i3 = [...r3.data.entries()].map(([o3, f8], l5) => ({ key: n4._parse(new O(r3, o3, r3.path, [l5, "key"])), value: a4._parse(new O(r3, f8, r3.path, [l5, "value"])) }));
    if (r3.common.async) {
      let o3 = /* @__PURE__ */ new Map();
      return Promise.resolve().then(async () => {
        for (let f8 of i3) {
          let l5 = await f8.key, _10 = await f8.value;
          if (l5.status === "aborted" || _10.status === "aborted")
            return m;
          (l5.status === "dirty" || _10.status === "dirty") && t3.dirty(), o3.set(l5.value, _10.value);
        }
        return { status: t3.value, value: o3 };
      });
    } else {
      let o3 = /* @__PURE__ */ new Map();
      for (let f8 of i3) {
        let l5 = f8.key, _10 = f8.value;
        if (l5.status === "aborted" || _10.status === "aborted")
          return m;
        (l5.status === "dirty" || _10.status === "dirty") && t3.dirty(), o3.set(l5.value, _10.value);
      }
      return { status: t3.value, value: o3 };
    }
  }
};
ne.create = (s2, e3, t3) => new ne({ valueType: e3, keyType: s2, typeName: p.ZodMap, ...y(t3) });
var V = class extends v {
  _parse(e3) {
    let { status: t3, ctx: r3 } = this._processInputParams(e3);
    if (r3.parsedType !== d.set)
      return u(r3, { code: c.invalid_type, expected: d.set, received: r3.parsedType }), m;
    let n4 = this._def;
    n4.minSize !== null && r3.data.size < n4.minSize.value && (u(r3, { code: c.too_small, minimum: n4.minSize.value, type: "set", inclusive: true, exact: false, message: n4.minSize.message }), t3.dirty()), n4.maxSize !== null && r3.data.size > n4.maxSize.value && (u(r3, { code: c.too_big, maximum: n4.maxSize.value, type: "set", inclusive: true, exact: false, message: n4.maxSize.message }), t3.dirty());
    let a4 = this._def.valueType;
    function i3(f8) {
      let l5 = /* @__PURE__ */ new Set();
      for (let _10 of f8) {
        if (_10.status === "aborted")
          return m;
        _10.status === "dirty" && t3.dirty(), l5.add(_10.value);
      }
      return { status: t3.value, value: l5 };
    }
    let o3 = [...r3.data.values()].map((f8, l5) => a4._parse(new O(r3, f8, r3.path, l5)));
    return r3.common.async ? Promise.all(o3).then((f8) => i3(f8)) : i3(o3);
  }
  min(e3, t3) {
    return new V({ ...this._def, minSize: { value: e3, message: h.toString(t3) } });
  }
  max(e3, t3) {
    return new V({ ...this._def, maxSize: { value: e3, message: h.toString(t3) } });
  }
  size(e3, t3) {
    return this.min(e3, t3).max(e3, t3);
  }
  nonempty(e3) {
    return this.min(1, e3);
  }
};
V.create = (s2, e3) => new V({ valueType: s2, minSize: null, maxSize: null, typeName: p.ZodSet, ...y(e3) });
var L = class extends v {
  constructor() {
    super(...arguments), this.validate = this.implement;
  }
  _parse(e3) {
    let { ctx: t3 } = this._processInputParams(e3);
    if (t3.parsedType !== d.function)
      return u(t3, { code: c.invalid_type, expected: d.function, received: t3.parsedType }), m;
    function r3(o3, f8) {
      return ue({ data: o3, path: t3.path, errorMaps: [t3.common.contextualErrorMap, t3.schemaErrorMap, de(), oe].filter((l5) => !!l5), issueData: { code: c.invalid_arguments, argumentsError: f8 } });
    }
    function n4(o3, f8) {
      return ue({ data: o3, path: t3.path, errorMaps: [t3.common.contextualErrorMap, t3.schemaErrorMap, de(), oe].filter((l5) => !!l5), issueData: { code: c.invalid_return_type, returnTypeError: f8 } });
    }
    let a4 = { errorMap: t3.common.contextualErrorMap }, i3 = t3.data;
    return this._def.returns instanceof D ? b(async (...o3) => {
      let f8 = new T([]), l5 = await this._def.args.parseAsync(o3, a4).catch((pe2) => {
        throw f8.addIssue(r3(o3, pe2)), f8;
      }), _10 = await i3(...l5);
      return await this._def.returns._def.type.parseAsync(_10, a4).catch((pe2) => {
        throw f8.addIssue(n4(_10, pe2)), f8;
      });
    }) : b((...o3) => {
      let f8 = this._def.args.safeParse(o3, a4);
      if (!f8.success)
        throw new T([r3(o3, f8.error)]);
      let l5 = i3(...f8.data), _10 = this._def.returns.safeParse(l5, a4);
      if (!_10.success)
        throw new T([n4(l5, _10.error)]);
      return _10.data;
    });
  }
  parameters() {
    return this._def.args;
  }
  returnType() {
    return this._def.returns;
  }
  args(...e3) {
    return new L({ ...this._def, args: N.create(e3).rest(Z.create()) });
  }
  returns(e3) {
    return new L({ ...this._def, returns: e3 });
  }
  implement(e3) {
    return this.parse(e3);
  }
  strictImplement(e3) {
    return this.parse(e3);
  }
  static create(e3, t3, r3) {
    return new L({ args: e3 || N.create([]).rest(Z.create()), returns: t3 || Z.create(), typeName: p.ZodFunction, ...y(r3) });
  }
};
var H = class extends v {
  get schema() {
    return this._def.getter();
  }
  _parse(e3) {
    let { ctx: t3 } = this._processInputParams(e3);
    return this._def.getter()._parse({ data: t3.data, path: t3.path, parent: t3 });
  }
};
H.create = (s2, e3) => new H({ getter: s2, typeName: p.ZodLazy, ...y(e3) });
var G = class extends v {
  _parse(e3) {
    if (e3.data !== this._def.value) {
      let t3 = this._getOrReturnCtx(e3);
      return u(t3, { received: t3.data, code: c.invalid_literal, expected: this._def.value }), m;
    }
    return { status: "valid", value: e3.data };
  }
  get value() {
    return this._def.value;
  }
};
G.create = (s2, e3) => new G({ value: s2, typeName: p.ZodLiteral, ...y(e3) });
function we(s2, e3) {
  return new A({ values: s2, typeName: p.ZodEnum, ...y(e3) });
}
var A = class extends v {
  _parse(e3) {
    if (typeof e3.data != "string") {
      let t3 = this._getOrReturnCtx(e3), r3 = this._def.values;
      return u(t3, { expected: g.joinValues(r3), received: t3.parsedType, code: c.invalid_type }), m;
    }
    if (this._def.values.indexOf(e3.data) === -1) {
      let t3 = this._getOrReturnCtx(e3), r3 = this._def.values;
      return u(t3, { received: t3.data, code: c.invalid_enum_value, options: r3 }), m;
    }
    return b(e3.data);
  }
  get options() {
    return this._def.values;
  }
  get enum() {
    let e3 = {};
    for (let t3 of this._def.values)
      e3[t3] = t3;
    return e3;
  }
  get Values() {
    let e3 = {};
    for (let t3 of this._def.values)
      e3[t3] = t3;
    return e3;
  }
  get Enum() {
    let e3 = {};
    for (let t3 of this._def.values)
      e3[t3] = t3;
    return e3;
  }
  extract(e3) {
    return A.create(e3);
  }
  exclude(e3) {
    return A.create(this.options.filter((t3) => !e3.includes(t3)));
  }
};
A.create = we;
var X = class extends v {
  _parse(e3) {
    let t3 = g.getValidEnumValues(this._def.values), r3 = this._getOrReturnCtx(e3);
    if (r3.parsedType !== d.string && r3.parsedType !== d.number) {
      let n4 = g.objectValues(t3);
      return u(r3, { expected: g.joinValues(n4), received: r3.parsedType, code: c.invalid_type }), m;
    }
    if (t3.indexOf(e3.data) === -1) {
      let n4 = g.objectValues(t3);
      return u(r3, { received: r3.data, code: c.invalid_enum_value, options: n4 }), m;
    }
    return b(e3.data);
  }
  get enum() {
    return this._def.values;
  }
};
X.create = (s2, e3) => new X({ values: s2, typeName: p.ZodNativeEnum, ...y(e3) });
var D = class extends v {
  unwrap() {
    return this._def.type;
  }
  _parse(e3) {
    let { ctx: t3 } = this._processInputParams(e3);
    if (t3.parsedType !== d.promise && t3.common.async === false)
      return u(t3, { code: c.invalid_type, expected: d.promise, received: t3.parsedType }), m;
    let r3 = t3.parsedType === d.promise ? t3.data : Promise.resolve(t3.data);
    return b(r3.then((n4) => this._def.type.parseAsync(n4, { path: t3.path, errorMap: t3.common.contextualErrorMap })));
  }
};
D.create = (s2, e3) => new D({ type: s2, typeName: p.ZodPromise, ...y(e3) });
var C = class extends v {
  innerType() {
    return this._def.schema;
  }
  sourceType() {
    return this._def.schema._def.typeName === p.ZodEffects ? this._def.schema.sourceType() : this._def.schema;
  }
  _parse(e3) {
    let { status: t3, ctx: r3 } = this._processInputParams(e3), n4 = this._def.effect || null;
    if (n4.type === "preprocess") {
      let i3 = n4.transform(r3.data);
      return r3.common.async ? Promise.resolve(i3).then((o3) => this._def.schema._parseAsync({ data: o3, path: r3.path, parent: r3 })) : this._def.schema._parseSync({ data: i3, path: r3.path, parent: r3 });
    }
    let a4 = { addIssue: (i3) => {
      u(r3, i3), i3.fatal ? t3.abort() : t3.dirty();
    }, get path() {
      return r3.path;
    } };
    if (a4.addIssue = a4.addIssue.bind(a4), n4.type === "refinement") {
      let i3 = (o3) => {
        let f8 = n4.refinement(o3, a4);
        if (r3.common.async)
          return Promise.resolve(f8);
        if (f8 instanceof Promise)
          throw new Error("Async refinement encountered during synchronous parse operation. Use .parseAsync instead.");
        return o3;
      };
      if (r3.common.async === false) {
        let o3 = this._def.schema._parseSync({ data: r3.data, path: r3.path, parent: r3 });
        return o3.status === "aborted" ? m : (o3.status === "dirty" && t3.dirty(), i3(o3.value), { status: t3.value, value: o3.value });
      } else
        return this._def.schema._parseAsync({ data: r3.data, path: r3.path, parent: r3 }).then((o3) => o3.status === "aborted" ? m : (o3.status === "dirty" && t3.dirty(), i3(o3.value).then(() => ({ status: t3.value, value: o3.value }))));
    }
    if (n4.type === "transform")
      if (r3.common.async === false) {
        let i3 = this._def.schema._parseSync({ data: r3.data, path: r3.path, parent: r3 });
        if (!le(i3))
          return i3;
        let o3 = n4.transform(i3.value, a4);
        if (o3 instanceof Promise)
          throw new Error("Asynchronous transform encountered during synchronous parse operation. Use .parseAsync instead.");
        return { status: t3.value, value: o3 };
      } else
        return this._def.schema._parseAsync({ data: r3.data, path: r3.path, parent: r3 }).then((i3) => le(i3) ? Promise.resolve(n4.transform(i3.value, a4)).then((o3) => ({ status: t3.value, value: o3 })) : i3);
    g.assertNever(n4);
  }
};
C.create = (s2, e3, t3) => new C({ schema: s2, typeName: p.ZodEffects, effect: e3, ...y(t3) });
C.createWithPreprocess = (s2, e3, t3) => new C({ schema: e3, effect: { type: "preprocess", transform: s2 }, typeName: p.ZodEffects, ...y(t3) });
var E = class extends v {
  _parse(e3) {
    return this._getType(e3) === d.undefined ? b(void 0) : this._def.innerType._parse(e3);
  }
  unwrap() {
    return this._def.innerType;
  }
};
E.create = (s2, e3) => new E({ innerType: s2, typeName: p.ZodOptional, ...y(e3) });
var $ = class extends v {
  _parse(e3) {
    return this._getType(e3) === d.null ? b(null) : this._def.innerType._parse(e3);
  }
  unwrap() {
    return this._def.innerType;
  }
};
$.create = (s2, e3) => new $({ innerType: s2, typeName: p.ZodNullable, ...y(e3) });
var K = class extends v {
  _parse(e3) {
    let { ctx: t3 } = this._processInputParams(e3), r3 = t3.data;
    return t3.parsedType === d.undefined && (r3 = this._def.defaultValue()), this._def.innerType._parse({ data: r3, path: t3.path, parent: t3 });
  }
  removeDefault() {
    return this._def.innerType;
  }
};
K.create = (s2, e3) => new K({ innerType: s2, typeName: p.ZodDefault, defaultValue: typeof e3.default == "function" ? e3.default : () => e3.default, ...y(e3) });
var ae = class extends v {
  _parse(e3) {
    let { ctx: t3 } = this._processInputParams(e3), r3 = { ...t3, common: { ...t3.common, issues: [] } }, n4 = this._def.innerType._parse({ data: r3.data, path: r3.path, parent: { ...r3 } });
    return fe(n4) ? n4.then((a4) => ({ status: "valid", value: a4.status === "valid" ? a4.value : this._def.catchValue({ get error() {
      return new T(r3.common.issues);
    }, input: r3.data }) })) : { status: "valid", value: n4.status === "valid" ? n4.value : this._def.catchValue({ get error() {
      return new T(r3.common.issues);
    }, input: r3.data }) };
  }
  removeCatch() {
    return this._def.innerType;
  }
};
ae.create = (s2, e3) => new ae({ innerType: s2, typeName: p.ZodCatch, catchValue: typeof e3.catch == "function" ? e3.catch : () => e3.catch, ...y(e3) });
var ie = class extends v {
  _parse(e3) {
    if (this._getType(e3) !== d.nan) {
      let r3 = this._getOrReturnCtx(e3);
      return u(r3, { code: c.invalid_type, expected: d.nan, received: r3.parsedType }), m;
    }
    return { status: "valid", value: e3.data };
  }
};
ie.create = (s2) => new ie({ typeName: p.ZodNaN, ...y(s2) });
var Ue = Symbol("zod_brand");
var he = class extends v {
  _parse(e3) {
    let { ctx: t3 } = this._processInputParams(e3), r3 = t3.data;
    return this._def.type._parse({ data: r3, path: t3.path, parent: t3 });
  }
  unwrap() {
    return this._def.type;
  }
};
var Q = class extends v {
  _parse(e3) {
    let { status: t3, ctx: r3 } = this._processInputParams(e3);
    if (r3.common.async)
      return (async () => {
        let a4 = await this._def.in._parseAsync({ data: r3.data, path: r3.path, parent: r3 });
        return a4.status === "aborted" ? m : a4.status === "dirty" ? (t3.dirty(), be(a4.value)) : this._def.out._parseAsync({ data: a4.value, path: r3.path, parent: r3 });
      })();
    {
      let n4 = this._def.in._parseSync({ data: r3.data, path: r3.path, parent: r3 });
      return n4.status === "aborted" ? m : n4.status === "dirty" ? (t3.dirty(), { status: "dirty", value: n4.value }) : this._def.out._parseSync({ data: n4.value, path: r3.path, parent: r3 });
    }
  }
  static create(e3, t3) {
    return new Q({ in: e3, out: t3, typeName: p.ZodPipeline });
  }
};
var Te = (s2, e3 = {}, t3) => s2 ? z.create().superRefine((r3, n4) => {
  var a4, i3;
  if (!s2(r3)) {
    let o3 = typeof e3 == "function" ? e3(r3) : typeof e3 == "string" ? { message: e3 } : e3, f8 = (i3 = (a4 = o3.fatal) !== null && a4 !== void 0 ? a4 : t3) !== null && i3 !== void 0 ? i3 : true, l5 = typeof o3 == "string" ? { message: o3 } : o3;
    n4.addIssue({ code: "custom", ...l5, fatal: f8 });
  }
}) : z.create();
var Be = { object: x.lazycreate };
var p;
(function(s2) {
  s2.ZodString = "ZodString", s2.ZodNumber = "ZodNumber", s2.ZodNaN = "ZodNaN", s2.ZodBigInt = "ZodBigInt", s2.ZodBoolean = "ZodBoolean", s2.ZodDate = "ZodDate", s2.ZodSymbol = "ZodSymbol", s2.ZodUndefined = "ZodUndefined", s2.ZodNull = "ZodNull", s2.ZodAny = "ZodAny", s2.ZodUnknown = "ZodUnknown", s2.ZodNever = "ZodNever", s2.ZodVoid = "ZodVoid", s2.ZodArray = "ZodArray", s2.ZodObject = "ZodObject", s2.ZodUnion = "ZodUnion", s2.ZodDiscriminatedUnion = "ZodDiscriminatedUnion", s2.ZodIntersection = "ZodIntersection", s2.ZodTuple = "ZodTuple", s2.ZodRecord = "ZodRecord", s2.ZodMap = "ZodMap", s2.ZodSet = "ZodSet", s2.ZodFunction = "ZodFunction", s2.ZodLazy = "ZodLazy", s2.ZodLiteral = "ZodLiteral", s2.ZodEnum = "ZodEnum", s2.ZodEffects = "ZodEffects", s2.ZodNativeEnum = "ZodNativeEnum", s2.ZodOptional = "ZodOptional", s2.ZodNullable = "ZodNullable", s2.ZodDefault = "ZodDefault", s2.ZodCatch = "ZodCatch", s2.ZodPromise = "ZodPromise", s2.ZodBranded = "ZodBranded", s2.ZodPipeline = "ZodPipeline";
})(p || (p = {}));
var We = (s2, e3 = { message: `Input not instance of ${s2.name}` }) => Te((t3) => t3 instanceof s2, e3);
var Se = w.create;
var Ce = j.create;
var qe = ie.create;
var Je = R.create;
var Oe = U.create;
var Ye = M.create;
var He = te.create;
var Ge = B.create;
var Xe = W.create;
var Ke = z.create;
var Qe = Z.create;
var Fe = I.create;
var et = se.create;
var tt = S.create;
var st = x.create;
var rt = x.strictCreate;
var nt = q.create;
var at = re.create;
var it = J.create;
var ot = N.create;
var ct = Y.create;
var dt = ne.create;
var ut = V.create;
var lt = L.create;
var ft = H.create;
var ht = G.create;
var pt = A.create;
var mt = X.create;
var yt = D.create;
var xe = C.create;
var vt = E.create;
var _t = $.create;
var gt = C.createWithPreprocess;
var xt = Q.create;
var kt = () => Se().optional();
var bt = () => Ce().optional();
var wt = () => Oe().optional();
var Tt = { string: (s2) => w.create({ ...s2, coerce: true }), number: (s2) => j.create({ ...s2, coerce: true }), boolean: (s2) => U.create({ ...s2, coerce: true }), bigint: (s2) => R.create({ ...s2, coerce: true }), date: (s2) => M.create({ ...s2, coerce: true }) };
var St = m;
var Ct = Object.freeze({ __proto__: null, defaultErrorMap: oe, setErrorMap: Ee, getErrorMap: de, makeIssue: ue, EMPTY_PATH: Ie, addIssueToContext: u, ParseStatus: k, INVALID: m, DIRTY: be, OK: b, isAborted: ye, isDirty: ve, isValid: le, isAsync: fe, get util() {
  return g;
}, get objectUtil() {
  return me;
}, ZodParsedType: d, getParsedType: P, ZodType: v, ZodString: w, ZodNumber: j, ZodBigInt: R, ZodBoolean: U, ZodDate: M, ZodSymbol: te, ZodUndefined: B, ZodNull: W, ZodAny: z, ZodUnknown: Z, ZodNever: I, ZodVoid: se, ZodArray: S, ZodObject: x, ZodUnion: q, ZodDiscriminatedUnion: re, ZodIntersection: J, ZodTuple: N, ZodRecord: Y, ZodMap: ne, ZodSet: V, ZodFunction: L, ZodLazy: H, ZodLiteral: G, ZodEnum: A, ZodNativeEnum: X, ZodPromise: D, ZodEffects: C, ZodTransformer: C, ZodOptional: E, ZodNullable: $, ZodDefault: K, ZodCatch: ae, ZodNaN: ie, BRAND: Ue, ZodBranded: he, ZodPipeline: Q, custom: Te, Schema: v, ZodSchema: v, late: Be, get ZodFirstPartyTypeKind() {
  return p;
}, coerce: Tt, any: Ke, array: tt, bigint: Je, boolean: Oe, date: Ye, discriminatedUnion: at, effect: xe, enum: pt, function: lt, instanceof: We, intersection: it, lazy: ft, literal: ht, map: dt, nan: qe, nativeEnum: mt, never: Fe, null: Xe, nullable: _t, number: Ce, object: st, oboolean: wt, onumber: bt, optional: vt, ostring: kt, pipeline: xt, preprocess: gt, promise: yt, record: ct, set: ut, strictObject: rt, string: Se, symbol: He, transformer: xe, tuple: ot, undefined: Ge, union: nt, unknown: Qe, void: et, NEVER: St, ZodIssueCode: c, quotelessJson: Ne, ZodError: T });

// ../desktop-dev/src/helper/zodHelper.ts
var mmidType = Ct.custom((val) => {
  return typeof val === "string" && val.endsWith(".dweb");
});
var DEFAULT_ERROR_MESSAGE = "Bad Request";
var DEFAULT_ERROR_STATUS = 400;
function createErrorResponse(options = {}) {
  const statusText = options?.message || DEFAULT_ERROR_MESSAGE;
  const status = options?.status || DEFAULT_ERROR_STATUS;
  return Response.json(statusText, { status, statusText });
}
var isZodType = (input) => {
  return typeof input.parse === "function";
};
function parseQuery(request, schema, options) {
  try {
    const searchParams = isURLSearchParams(request) ? request : getSearchParamsFromRequest(request);
    const params = parseSearchParams(searchParams, options?.parser);
    const finalSchema = isZodType(schema) ? schema : Ct.object(schema);
    return finalSchema.parse(params);
  } catch (error) {
    throw createErrorResponse(options);
  }
}
var zq = {
  string: () => Ct.string(),
  number: (float = true) => Ct.string().transform((val, ctx) => {
    const num = float ? parseFloat(val) : parseInt(val);
    if (isFinite(num)) {
      return num;
    }
    ctx.addIssue({
      code: Ct.ZodIssueCode.custom,
      message: `fail to parse ${ctx.path.join(".")} to number`
    });
    return Ct.NEVER;
  }),
  boolean: () => Ct.string().transform((val) => /^true$/i.test(val)),
  transform: (transform) => Ct.string().transform(transform),
  parseQuery
};
function parseSearchParams(searchParams, customParser) {
  const parser = customParser || parseSearchParamsDefault;
  return parser(searchParams);
}
var parseSearchParamsDefault = (searchParams) => {
  const values = {};
  searchParams.forEach((value, key) => {
    const currentVal = values[key];
    if (currentVal && Array.isArray(currentVal)) {
      currentVal.push(value);
    } else if (currentVal) {
      values[key] = [currentVal, value];
    } else {
      values[key] = value;
    }
  });
  return values;
};
function getSearchParamsFromRequest(request) {
  const url = new URL(request.url);
  return url.searchParams;
}
function isURLSearchParams(value) {
  return getObjectTypeName(value) === "URLSearchParams";
}
function getObjectTypeName(value) {
  return toString.call(value).slice(8, -1);
}

// ../desktop-dev/src/core/ipc/const.ts
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

// ../desktop-dev/src/helper/fetchExtends/$makeFetchBaseExtends.ts
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

// ../desktop-dev/src/helper/JsonlinesStream.ts
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

// ../desktop-dev/src/helper/fetchExtends/$makeFetchStreamExtends.ts
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

// ../desktop-dev/src/helper/fetchExtends/index.ts
var fetchExtends = {
  ...fetchBaseExtends,
  ...fetchStreamExtends
};

// ../desktop-dev/src/helper/urlHelper.ts
var getBaseUrl = () => URL_BASE ??= "document" in globalThis ? document.baseURI : "location" in globalThis && (location.protocol === "http:" || location.protocol === "https:" || location.protocol === "file:" || location.protocol === "chrome-extension:") ? location.href : "file:///";
var URL_BASE;
var parseUrl = (url, base = getBaseUrl()) => {
  return new URL(url, base);
};

// ../desktop-dev/src/helper/normalizeFetchArgs.ts
var normalizeFetchArgs = (url, init) => {
  let _parsed_url;
  let _request_init = init;
  if (typeof url === "string") {
    _parsed_url = parseUrl(url);
  } else if (url instanceof Request) {
    _parsed_url = parseUrl(url.url);
    _request_init = url;
  } else if (url instanceof URL) {
    _parsed_url = url;
  }
  if (_parsed_url === void 0) {
    throw new Error(`no found url for fetch`);
  }
  const parsed_url = _parsed_url;
  const request_init = _request_init ?? {};
  return {
    parsed_url,
    request_init
  };
};

// ../desktop-dev/src/helper/AdaptersManager.ts
var AdaptersManager = class {
  constructor() {
    this.adapterOrderMap = /* @__PURE__ */ new Map();
    this.orderdAdapters = [];
  }
  _reorder() {
    this.orderdAdapters = [...this.adapterOrderMap].sort((a4, b7) => b7[1] - a4[1]).map((a4) => a4[0]);
  }
  get adapters() {
    return this.orderdAdapters;
  }
  /**
   * 
   * @param adapter 
   * @param order 越大优先级越高
   * @returns 
   */
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

// ../desktop-dev/src/sys/dns/nativeFetch.ts
var nativeFetchAdaptersManager = new AdaptersManager();

// ../desktop-dev/src/core/micro-module.ts
var MicroModule = class {
  constructor() {
    this._running_state_lock = PromiseOut.resolve(false);
    this._after_shutdown_signal = createSignal();
    this._ipcSet = /* @__PURE__ */ new Set();
    /**
     * 内部程序与外部程序通讯的方法
     * TODO 这里应该是可以是多个
     */
    this._connectSignal = createSignal();
  }
  addToIpcSet(ipc) {
    this._ipcSet.add(ipc);
    ipc.onClose(() => {
      this._ipcSet.delete(ipc);
    });
  }
  get isRunning() {
    return this._running_state_lock.promise;
  }
  async before_bootstrap(context) {
    if (await this._running_state_lock.promise) {
      throw new Error(`module ${this.mmid} alreay running`);
    }
    this._running_state_lock = new PromiseOut();
    this.context = context;
  }
  after_bootstrap(_context) {
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
    this._running_state_lock = new PromiseOut();
    this.context = void 0;
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
  /**
   * 尝试连接到指定对象
   */
  connect(mmid) {
    this.context?.dns.open(mmid);
    return this.context?.dns.connect(mmid);
  }
  /**
   * 收到一个连接，触发相关事件
   */
  beConnect(ipc, reason) {
    this.addToIpcSet(ipc);
    ipc.onEvent((event, ipc2) => {
      if (event.name == "activity") {
        this.onActivity(event, ipc2);
      }
    });
    this._connectSignal.emit(ipc, reason);
  }
  onActivity(event, ipc) {
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

// ../desktop-dev/src/helper/$once.ts
var once = (fn) => {
  let first = true;
  let resolved;
  let rejected;
  let success = false;
  return function(...args) {
    if (first) {
      first = false;
      try {
        resolved = fn.apply(this, args);
        success = true;
      } catch (err) {
        rejected = err;
      }
    }
    if (success) {
      return resolved;
    }
    throw rejected;
  };
};

// ../desktop-dev/src/helper/httpHelper.ts
var httpMethodCanOwnBody = (method) => {
  return method !== "GET" && method !== "HEAD" && method !== "TRACE" && method !== "OPTIONS";
};

// ../desktop-dev/src/core/ipc/IpcBody.ts
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
      _IpcBody.CACHE.raw_ipcBody_WMap.set(body_u8a, this);
    }
    return body_u8a;
  }
  async stream() {
    const bodyHub = this._bodyHub;
    let body_stream = bodyHub.stream;
    if (body_stream === void 0) {
      body_stream = new Blob([await this.u8a()]).stream();
      bodyHub.stream = body_stream;
      _IpcBody.CACHE.raw_ipcBody_WMap.set(body_stream, this);
    }
    return body_stream;
  }
  async text() {
    const bodyHub = this._bodyHub;
    let body_text = bodyHub.text;
    if (body_text === void 0) {
      body_text = simpleDecoder(await this.u8a(), "utf8");
      bodyHub.text = body_text;
    }
    return body_text;
  }
};
var IpcBody = _IpcBody;
IpcBody.CACHE = new class {
  constructor() {
    /**
     * 任意的 RAW 背后都会有一个 IpcBodySender/IpcBodyReceiver
     * 将它们缓存起来，那么使用这些 RAW 确保只拿到同一个 IpcBody，这对 RAW-Stream 很重要，流不可以被多次打开读取
     */
    this.raw_ipcBody_WMap = /* @__PURE__ */ new WeakMap();
    /**
     * 每一个 metaBody 背后，都会有第一个 接收者IPC，这直接定义了它的应该由谁来接收这个数据，
     * 其它的 IPC 即便拿到了这个 metaBody 也是没有意义的，除非它是 INLINE
     */
    this.metaId_receiverIpc_Map = /* @__PURE__ */ new Map();
    /**
     * 每一个 metaBody 背后，都会有一个 IpcBodySender,
     * 这里主要是存储 流，因为它有明确的 open/close 生命周期
     */
    this.metaId_ipcBodySender_Map = /* @__PURE__ */ new Map();
  }
}();
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

// ../desktop-dev/src/core/ipc/IpcStreamData.ts
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

// ../desktop-dev/src/core/ipc/IpcStreamEnd.ts
var IpcStreamEnd = class extends IpcMessage {
  constructor(stream_id) {
    super(5 /* STREAM_END */);
    this.stream_id = stream_id;
  }
};

// ../desktop-dev/src/core/ipc/MetaBody.ts
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
      default:
        return 2 /* UTF8 */;
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

// ../desktop-dev/src/core/ipc/IpcBodySender.ts
var _IpcBodySender = class extends IpcBody {
  constructor(data, ipc) {
    super();
    this.data = data;
    this.ipc = ipc;
    this.streamCtorSignal = createSignal();
    /**
     * 被哪些 ipc 所真正使用，使用的进度分别是多少
     *
     * 这个进度 用于 类似流的 多发
     */
    this.usedIpcMap = /* @__PURE__ */ new Map();
    this.UsedIpcInfo = class UsedIpcInfo {
      constructor(ipcBody, ipc, bandwidth = 0, fuse = 0) {
        this.ipcBody = ipcBody;
        this.ipc = ipc;
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
    this.closeSignal = createSignal();
    this.openSignal = createSignal();
    this._isStreamOpened = false;
    this._isStreamClosed = false;
    this._bodyHub = new BodyHub(data);
    this.metaBody = this.$bodyAsMeta(data, ipc);
    this.isStream = data instanceof ReadableStream;
    if (typeof data !== "string") {
      _IpcBodySender.CACHE.raw_ipcBody_WMap.set(data, this);
    }
    _IpcBodySender.$usableByIpc(ipc, this);
  }
  static fromAny(data, ipc) {
    if (typeof data !== "string") {
      const cache = _IpcBodySender.CACHE.raw_ipcBody_WMap.get(data);
      if (cache !== void 0) {
        return cache;
      }
    }
    return new _IpcBodySender(data, ipc);
  }
  static fromText(raw, ipc) {
    return this.fromAny(raw, ipc);
  }
  static fromBinary(raw, ipc) {
    return this.fromAny(raw, ipc);
  }
  static fromStream(raw, ipc) {
    return this.fromAny(raw, ipc);
  }
  /**
   * 绑定使用
   */
  useByIpc(ipc) {
    const info = this.usedIpcMap.get(ipc);
    if (info !== void 0) {
      return info;
    }
    if (this.isStream && !this._isStreamOpened) {
      const info2 = new this.UsedIpcInfo(this, ipc);
      this.usedIpcMap.set(ipc, info2);
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
  $bodyAsMeta(body, ipc) {
    if (typeof body === "string") {
      return MetaBody.fromText(ipc.uid, body);
    }
    if (body instanceof ReadableStream) {
      return this.$streamAsMeta(body, ipc);
    }
    return MetaBody.fromBinary(ipc, body);
  }
  /**
   * 如果 rawData 是流模式，需要提供数据发送服务
   *
   * 这里不会一直无脑发，而是对方有需要的时候才发
   * @param stream_id
   * @param stream
   * @param ipc
   */
  $streamAsMeta(stream, ipc) {
    const stream_id = getStreamId(stream);
    let _reader;
    const getReader = () => _reader ??= binaryStreamRead(stream);
    (async () => {
      let pullingLock = new PromiseOut();
      this.streamCtorSignal.listen(async (signal) => {
        switch (signal) {
          case 0 /* PULLING */: {
            pullingLock.resolve();
            break;
          }
          case 1 /* PAUSED */: {
            if (pullingLock.is_finished) {
              pullingLock = new PromiseOut();
            }
            break;
          }
          case 2 /* ABORTED */: {
            await getReader().return();
            await stream.cancel();
            this.emitStreamClose();
          }
        }
      });
      while (true) {
        await pullingLock.promise;
        const reader = getReader();
        const availableLen = await reader.available();
        if (availableLen > 0) {
          this.isStreamOpened = true;
          const message = IpcStreamData.fromBinary(
            stream_id,
            await reader.readBinary(availableLen)
          );
          for (const ipc2 of this.usedIpcMap.keys()) {
            ipc2.postMessage(message);
          }
        } else if (availableLen === -1) {
          const message = new IpcStreamEnd(stream_id);
          for (const ipc2 of this.usedIpcMap.keys()) {
            ipc2.postMessage(message);
          }
          await stream.cancel();
          this.emitStreamClose();
          break;
        }
      }
    })().catch(console.error);
    const streamType = 0 /* STREAM_ID */;
    const streamFirstData = "";
    if ("preReadableSize" in stream && typeof stream.preReadableSize === "number" && stream.preReadableSize > 0) {
    }
    const metaBody = new MetaBody(
      streamType,
      ipc.uid,
      streamFirstData,
      stream_id
    );
    _IpcBodySender.CACHE.metaId_ipcBodySender_Map.set(metaBody.metaId, this);
    this.streamCtorSignal.listen((signal) => {
      if (signal == 2 /* ABORTED */) {
        _IpcBodySender.CACHE.metaId_ipcBodySender_Map.delete(metaBody.metaId);
      }
    });
    return metaBody;
  }
};
var IpcBodySender = _IpcBodySender;
/**
 * ipc 将会使用它
 */
IpcBodySender.$usableByIpc = (ipc, ipcBody) => {
  if (ipcBody.isStream && !ipcBody._isStreamOpened) {
    const streamId = ipcBody.metaBody.streamId;
    let usableIpcBodyMapper = IpcUsableIpcBodyMap.get(ipc);
    if (usableIpcBodyMapper === void 0) {
      const mapper = new UsableIpcBodyMapper();
      mapper.onDestroy(
        ipc.onStream((message) => {
          switch (message.type) {
            case 3 /* STREAM_PULLING */:
              mapper.get(message.stream_id)?.useByIpc(ipc)?.emitStreamPull(message);
              break;
            case 4 /* STREAM_PAUSED */:
              mapper.get(message.stream_id)?.useByIpc(ipc)?.emitStreamPaused(message);
              break;
            case 6 /* STREAM_ABORT */:
              mapper.get(message.stream_id)?.useByIpc(ipc)?.emitStreamAborted();
              break;
          }
        })
      );
      mapper.onDestroy(() => IpcUsableIpcBodyMap.delete(ipc));
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

// ../desktop-dev/src/core/ipc/IpcHeaders.ts
var IpcHeaders = class extends Headers {
  init(key, value) {
    if (this.has(key) === false) {
      this.set(key, value);
    }
    return this;
  }
  toJSON() {
    const record = {};
    this.forEach((value, key) => {
      record[key.replace(/\w+/g, (w9) => w9[0].toUpperCase() + w9.slice(1))] = value;
    });
    return record;
  }
};

// ../desktop-dev/src/core/ipc/IpcRequest.ts
var IpcRequest = class extends IpcMessage {
  constructor(req_id, url, method, headers, body, ipc) {
    super(0 /* REQUEST */);
    this.req_id = req_id;
    this.url = url;
    this.method = method;
    this.headers = headers;
    this.body = body;
    this.ipc = ipc;
    this.ipcReqMessage = once(
      () => new IpcReqMessage(
        this.req_id,
        this.method,
        this.url,
        this.headers.toJSON(),
        this.body.metaBody
      )
    );
    if (body instanceof IpcBodySender) {
      IpcBodySender.$usableByIpc(ipc, body);
    }
  }
  #parsed_url;
  get parsed_url() {
    return this.#parsed_url ??= parseUrl(this.url);
  }
  static fromText(req_id, url, method = "GET" /* GET */, headers = new IpcHeaders(), text, ipc) {
    return new IpcRequest(
      req_id,
      url,
      method,
      headers,
      IpcBodySender.fromText(text, ipc),
      ipc
    );
  }
  static fromBinary(req_id, url, method = "GET" /* GET */, headers = new IpcHeaders(), binary, ipc) {
    headers.init("Content-Type", "application/octet-stream");
    headers.init("Content-Length", binary.byteLength + "");
    return new IpcRequest(
      req_id,
      url,
      method,
      headers,
      IpcBodySender.fromBinary(binaryToU8a(binary), ipc),
      ipc
    );
  }
  // 如果需要发送stream数据 一定要使用这个方法才可以传递数据否则数据无法传递
  static fromStream(req_id, url, method = "GET" /* GET */, headers = new IpcHeaders(), stream, ipc) {
    headers.init("Content-Type", "application/octet-stream");
    return new IpcRequest(
      req_id,
      url,
      method,
      headers,
      IpcBodySender.fromStream(stream, ipc),
      ipc
    );
  }
  static fromRequest(req_id, ipc, url, init = {}) {
    const method = toIpcMethod(init.method);
    const headers = init.headers instanceof IpcHeaders ? init.headers : new IpcHeaders(init.headers);
    let ipcBody;
    if (isBinary(init.body)) {
      ipcBody = IpcBodySender.fromBinary(init.body, ipc);
    } else if (init.body instanceof ReadableStream) {
      ipcBody = IpcBodySender.fromStream(init.body, ipc);
    } else {
      ipcBody = IpcBodySender.fromText(init.body ?? "", ipc);
    }
    return new IpcRequest(req_id, url, method, headers, ipcBody, ipc);
  }
  /**
   * 判断是否是双工协议
   *
   * 比如目前双工协议可以由 WebSocket 来提供支持
   */
  isDuplex() {
    return this.method === "GET" && this.headers.get("Upgrade")?.toLowerCase() === "websocket";
  }
  toRequest() {
    let { method } = this;
    let body;
    const isWebSocket = this.isDuplex();
    if (isWebSocket) {
      method = "POST" /* POST */;
      body = this.body.raw;
    } else if (httpMethodCanOwnBody(method)) {
      body = this.body.raw;
    }
    const init = {
      method,
      headers: this.headers,
      body,
      duplex: body instanceof ReadableStream ? "half" : void 0
    };
    if (body) {
      Reflect.set(init, "duplex", "half");
    }
    const request = new Request(this.url, init);
    if (isWebSocket) {
      Object.defineProperty(request, "method", {
        configurable: true,
        enumerable: true,
        writable: false,
        value: "GET"
      });
    }
    return request;
  }
  toJSON() {
    const { method } = this;
    if ((method === "GET" /* GET */ || method === "HEAD" /* HEAD */) === false) {
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

// ../desktop-dev/src/core/ipc/ipc.ts
var ipc_uid_acc = 0;
var Ipc = class {
  constructor() {
    this.uid = ipc_uid_acc++;
    this._support_message_pack = false;
    this._support_protobuf = false;
    this._support_raw = false;
    this._support_binary = false;
    this._closeSignal = createSignal(false);
    this.onClose = this._closeSignal.listen;
    this._messageSignal = this._createSignal(false);
    this.onMessage = this._messageSignal.listen;
    /**
     * 强制触发消息传入，而不是依赖远端的 postMessage
     */
    this.emitMessage = (args) => this._messageSignal.emit(args, this);
    this._closed = false;
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
  // deno-lint-ignore no-explicit-any
  _createSignal(autoStart) {
    const signal = createSignal(autoStart);
    this.onClose(() => signal.clear());
    return signal;
  }
  postMessage(message) {
    if (this._closed) {
      return;
    }
    this._doPostMessage(message);
  }
  get _onRequestSignal() {
    const signal = this._createSignal(false);
    this.onMessage((request, ipc) => {
      if (request.type === 0 /* REQUEST */) {
        signal.emit(request, ipc);
      }
    });
    return signal;
  }
  onRequest(cb) {
    return this._onRequestSignal.listen(cb);
  }
  onFetch(...handlers) {
    const onRequest = createFetchHandler(handlers);
    return onRequest.extendsTo(this.onRequest(onRequest));
  }
  get _onStreamSignal() {
    const signal = this._createSignal(false);
    this.onMessage((request, ipc) => {
      if ("stream_id" in request) {
        signal.emit(request, ipc);
      }
    });
    return signal;
  }
  onStream(cb) {
    return this._onStreamSignal.listen(cb);
  }
  get _onEventSignal() {
    const signal = this._createSignal(false);
    this.onMessage((event, ipc) => {
      if (event.type === 7 /* EVENT */) {
        signal.emit(event, ipc);
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
  allocReqId(_url) {
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
    const response_po = new PromiseOut();
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

// ../desktop-dev/src/core/ipc/IpcStreamAbort.ts
var IpcStreamAbort = class extends IpcMessage {
  constructor(stream_id) {
    super(6 /* STREAM_ABORT */);
    this.stream_id = stream_id;
  }
};

// ../desktop-dev/src/core/ipc/IpcStreamPulling.ts
var IpcStreamPulling = class extends IpcMessage {
  constructor(stream_id, bandwidth) {
    super(3 /* STREAM_PULLING */);
    this.stream_id = stream_id;
    this.bandwidth = bandwidth ?? 0;
  }
};

// ../desktop-dev/src/core/ipc/IpcBodyReceiver.ts
var IpcBodyReceiver = class extends IpcBody {
  constructor(metaBody, ipc) {
    super();
    this.metaBody = metaBody;
    if (metaBody.type_isStream) {
      const streamId = metaBody.streamId;
      const senderIpcUid = metaBody.senderUid;
      const metaId = `${senderIpcUid}/${streamId}`;
      if (IpcBodyReceiver.CACHE.metaId_receiverIpc_Map.has(metaId) === false) {
        ipc.onClose(() => {
          IpcBodyReceiver.CACHE.metaId_receiverIpc_Map.delete(metaId);
        });
        IpcBodyReceiver.CACHE.metaId_receiverIpc_Map.set(metaId, ipc);
        metaBody.receiverUid = ipc.uid;
      }
      const receiver = IpcBodyReceiver.CACHE.metaId_receiverIpc_Map.get(metaId);
      if (receiver === void 0) {
        throw new Error(`no found ipc by metaId:${metaId}`);
      }
      ipc = receiver;
      this._bodyHub = new BodyHub($metaToStream(this.metaBody, ipc));
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
  /**
   * 基于 metaBody 还原 IpcBodyReceiver
   */
  static from(metaBody, ipc) {
    return IpcBodyReceiver.CACHE.metaId_ipcBodySender_Map.get(metaBody.metaId) ?? new IpcBodyReceiver(metaBody, ipc);
  }
};
var $metaToStream = (metaBody, ipc) => {
  if (ipc == null) {
    throw new Error(`miss ipc when ipc-response has stream-body`);
  }
  const stream_ipc = ipc;
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
        const off = ipc.onStream((message) => {
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
      pull(_controller) {
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

// ../desktop-dev/src/core/ipc/IpcEvent.ts
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

// ../desktop-dev/src/core/ipc/IpcResponse.ts
var IpcResponse = class extends IpcMessage {
  constructor(req_id, statusCode, headers, body, ipc) {
    super(1 /* RESPONSE */);
    this.req_id = req_id;
    this.statusCode = statusCode;
    this.headers = headers;
    this.body = body;
    this.ipc = ipc;
    this.ipcResMessage = once(
      () => new IpcResMessage(
        this.req_id,
        this.statusCode,
        this.headers.toJSON(),
        this.body.metaBody
      )
    );
    if (body instanceof IpcBodySender) {
      IpcBodySender.$usableByIpc(ipc, body);
    }
  }
  #ipcHeaders;
  get ipcHeaders() {
    return this.#ipcHeaders ??= new IpcHeaders(this.headers);
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
  static async fromResponse(req_id, response, ipc, asBinary = false) {
    if (response.bodyUsed) {
      throw new Error("body used");
    }
    let ipcBody;
    if (asBinary || response.body == void 0 || parseInt(response.headers.get("Content-Length") || "NaN") < 16 * 1024 * 1024) {
      ipcBody = IpcBodySender.fromBinary(
        binaryToU8a(await response.arrayBuffer()),
        ipc
      );
    } else {
      setStreamId(response.body, response.url);
      ipcBody = IpcBodySender.fromStream(response.body, ipc);
    }
    const ipcHeaders = new IpcHeaders(response.headers);
    return new IpcResponse(req_id, response.status, ipcHeaders, ipcBody, ipc);
  }
  static fromJson(req_id, statusCode, headers = new IpcHeaders(), jsonable, ipc) {
    headers.init("Content-Type", "application/json");
    return this.fromText(
      req_id,
      statusCode,
      headers,
      JSON.stringify(jsonable),
      ipc
    );
  }
  static fromText(req_id, statusCode, headers = new IpcHeaders(), text, ipc) {
    headers.init("Content-Type", "text/plain");
    return new IpcResponse(
      req_id,
      statusCode,
      headers,
      IpcBodySender.fromText(text, ipc),
      ipc
    );
  }
  static fromBinary(req_id, statusCode, headers = new IpcHeaders(), binary, ipc) {
    headers.init("Content-Type", "application/octet-stream");
    headers.init("Content-Length", binary.byteLength + "");
    return new IpcResponse(
      req_id,
      statusCode,
      headers,
      IpcBodySender.fromBinary(binaryToU8a(binary), ipc),
      ipc
    );
  }
  static fromStream(req_id, statusCode, headers = new IpcHeaders(), stream, ipc) {
    headers.init("Content-Type", "application/octet-stream");
    const ipcResponse = new IpcResponse(
      req_id,
      statusCode,
      headers,
      IpcBodySender.fromStream(stream, ipc),
      ipc
    );
    return ipcResponse;
  }
  toJSON() {
    return this.ipcResMessage();
  }
};
var IpcResMessage = class extends IpcMessage {
  constructor(req_id, statusCode, headers, metaBody) {
    super(1 /* RESPONSE */);
    this.req_id = req_id;
    this.statusCode = statusCode;
    this.headers = headers;
    this.metaBody = metaBody;
  }
};

// ../desktop-dev/src/core/ipc/IpcStreamPaused.ts
var IpcStreamPaused = class extends IpcMessage {
  constructor(stream_id, fuse) {
    super(4 /* STREAM_PAUSED */);
    this.stream_id = stream_id;
    this.fuse = fuse ?? 1;
  }
};

// ../desktop-dev/src/core/helper/ipcRequestHelper.ts
var $bodyInitToIpcBodyArgs = async (bodyInit, onUnknown) => {
  let body = "";
  if (bodyInit instanceof FormData || bodyInit instanceof URLSearchParams) {
    bodyInit = await new Request("", {
      body: bodyInit
    }).blob();
  }
  if (bodyInit instanceof ReadableStream) {
    body = bodyInit;
  } else if (bodyInit instanceof Blob) {
    if (bodyInit.size >= 16 * 1024 * 1024) {
      body = bodyInit?.stream() || "";
    }
    if (body === "") {
      body = new Uint8Array(await bodyInit.arrayBuffer());
    }
  } else if (isBinary(bodyInit)) {
    body = binaryToU8a(bodyInit);
  } else if (typeof bodyInit === "string") {
    body = bodyInit;
  } else if (bodyInit) {
    if (onUnknown) {
      bodyInit = onUnknown(bodyInit);
    } else {
      throw new Error(
        `unsupport body type: ${bodyInit?.constructor.name}`
      );
    }
  }
  return body;
};

// ../desktop-dev/src/core/helper/ipcFetchHelper.ts
var fetchMid = (handler) => Object.assign(handler, { [FETCH_MID_SYMBOL]: true });
var FETCH_MID_SYMBOL = Symbol("fetch.middleware");
var fetchEnd = (handler) => Object.assign(handler, { [FETCH_END_SYMBOL]: true });
var FETCH_END_SYMBOL = Symbol("fetch.end");
var FETCH_WS_SYMBOL = Symbol("fetch.websocket");
var $throw = (err) => {
  throw err;
};
var fetchHanlderFactory = {
  NoFound: () => fetchEnd(
    (event, res) => res ?? $throw(new FetchError("No Found", { status: 404 }))
  ),
  Forbidden: () => fetchEnd(
    (event, res) => res ?? $throw(new FetchError("Forbidden", { status: 403 }))
  ),
  BadRequest: () => fetchEnd(
    (event, res) => res ?? $throw(new FetchError("Bad Request", { status: 400 }))
  ),
  InternalServerError: (message = "Internal Server Error") => fetchEnd(
    (event, res) => res ?? $throw(new FetchError(message, { status: 500 }))
  )
};
var createFetchHandler = (onFetchs) => {
  const onFetchHanlders = [...onFetchs];
  const extendsTo = (_to) => {
    const wrapFactory = (factory) => {
      return (...args) => {
        onFetchHanlders.push(factory(...args));
        return to;
      };
    };
    const EXT = {
      onFetch: (handler) => {
        onFetchHanlders.push(handler);
        return to;
      },
      onWebSocket: (hanlder) => {
        onFetchHanlders.push(hanlder);
        return to;
      },
      mid: (handler) => {
        onFetchHanlders.push(fetchMid(handler));
        return to;
      },
      end: (handler) => {
        onFetchHanlders.push(fetchEnd(handler));
        return to;
      },
      /**
       * 配置跨域，一般是最后调用
       * @param config
       */
      cors: (config = {}) => {
        onFetchHanlders.unshift((event) => {
          if (event.method === "OPTIONS") {
            return { body: "" };
          }
        });
        onFetchHanlders.push(
          fetchMid((res) => {
            res?.headers.init("Access-Control-Allow-Origin", config.origin ?? "*").init("Access-Control-Allow-Headers", config.headers ?? "*").init("Access-Control-Allow-Methods", config.methods ?? "*");
            return res;
          })
        );
        return to;
      },
      noFound: wrapFactory(fetchHanlderFactory.NoFound),
      forbidden: wrapFactory(fetchHanlderFactory.Forbidden),
      badRequest: wrapFactory(fetchHanlderFactory.BadRequest),
      internalServerError: wrapFactory(fetchHanlderFactory.InternalServerError),
      extendsTo
    };
    const to = _to;
    Object.assign(to, EXT);
    return to;
  };
  const onRequest = async (request, ipc) => {
    const event = new FetchEvent(request, ipc);
    let res;
    for (const handler of onFetchHanlders) {
      try {
        let result;
        if (FETCH_MID_SYMBOL in handler) {
          if (res !== void 0) {
            result = await handler(res, event);
          }
        } else if (FETCH_END_SYMBOL in handler) {
          result = await handler(event, res);
        } else {
          if (res === void 0) {
            result = await handler(event);
          }
        }
        if (result instanceof IpcResponse) {
          res = result;
        } else if (result instanceof Response) {
          res = await IpcResponse.fromResponse(request.req_id, result, ipc);
        } else if (typeof result === "object") {
          const req_id = request.req_id;
          const status = result.status ?? 200;
          const headers = new IpcHeaders(result.headers);
          if (result.body instanceof IpcBody) {
            res = new IpcResponse(req_id, status, headers, result.body, ipc);
          } else {
            const body = await $bodyInitToIpcBodyArgs(
              result.body,
              (bodyInit) => {
                if (headers.has("Content-Type") === false || headers.get("Content-Type").startsWith("application/javascript")) {
                  headers.init(
                    "Content-Type",
                    "application/javascript,charset=utf8"
                  );
                  return JSON.stringify(bodyInit);
                }
                return String(bodyInit);
              }
            );
            if (typeof body === "string") {
              res = IpcResponse.fromText(req_id, status, headers, body, ipc);
            } else if (isBinary(body)) {
              res = IpcResponse.fromBinary(req_id, status, headers, body, ipc);
            } else if (body instanceof ReadableStream) {
              res = IpcResponse.fromStream(req_id, status, headers, body, ipc);
            }
          }
        }
      } catch (err) {
        let err_code = 500;
        let err_message = "";
        let err_detail = "";
        if (err instanceof Error) {
          err_message = err.message;
          err_detail = err.stack ?? err.name;
          if (err instanceof FetchError) {
            err_code = err.code;
          }
        } else {
          err_message = String(err);
        }
        if (request.headers.get("Accept") === "application/json") {
          res = IpcResponse.fromJson(
            request.req_id,
            err_code,
            new IpcHeaders().init("Content-Type", "text/html,charset=utf8"),
            { message: err_message, detail: err_detail },
            ipc
          );
        } else {
          res = IpcResponse.fromText(
            request.req_id,
            err_code,
            new IpcHeaders().init("Content-Type", "text/html,charset=utf8"),
            err instanceof Error ? `<h1>${err.message}</h1><hr/><pre>${err.stack}</pre>` : String(err),
            ipc
          );
        }
      }
    }
    if (res) {
      ipc.postMessage(res);
      return res;
    }
  };
  return extendsTo(onRequest);
};
var FetchEvent = class {
  constructor(ipcRequest, ipc) {
    this.ipcRequest = ipcRequest;
    this.ipc = ipc;
  }
  get url() {
    return this.ipcRequest.parsed_url;
  }
  get pathname() {
    return this.url.pathname;
  }
  get search() {
    return this.url.search;
  }
  get searchParams() {
    return this.url.searchParams;
  }
  #request;
  get request() {
    return this.#request ??= this.ipcRequest.toRequest();
  }
  //#region Body 相关的属性与方法
  /** A simple getter used to expose a `ReadableStream` of the body contents. */
  get body() {
    return this.request.body;
  }
  /** Stores a `Boolean` that declares whether the body has been used in a
   * response yet.
   */
  get bodyUsed() {
    return this.request.bodyUsed;
  }
  /** Takes a `Response` stream and reads it to completion. It returns a promise
   * that resolves with an `ArrayBuffer`.
   */
  arrayBuffer() {
    return this.request.arrayBuffer();
  }
  async typedArray() {
    return new Uint8Array(await this.request.arrayBuffer());
  }
  /** Takes a `Response` stream and reads it to completion. It returns a promise
   * that resolves with a `Blob`.
   */
  blob() {
    return this.request.blob();
  }
  /** Takes a `Response` stream and reads it to completion. It returns a promise
   * that resolves with a `FormData` object.
   */
  formData() {
    return this.request.formData();
  }
  /** Takes a `Response` stream and reads it to completion. It returns a promise
   * that resolves with the result of parsing the body text as JSON.
   */
  json() {
    return this.request.json();
  }
  /** Takes a `Response` stream and reads it to completion. It returns a promise
   * that resolves with a `USVString` (text).
   */
  text() {
    return this.request.text();
  }
  //#endregion
  /** Returns a Headers object consisting of the headers associated with request. Note that headers added in the network layer by the user agent will not be accounted for in this object, e.g., the "Host" header. */
  get headers() {
    return this.ipcRequest.headers;
  }
  /** Returns request's HTTP method, which is "GET" by default. */
  get method() {
    return this.ipcRequest.method;
  }
  /** Returns the URL of request as a string. */
  get href() {
    return this.url.href;
  }
  get req_id() {
    return this.ipcRequest.req_id;
  }
};
var FetchError = class extends Error {
  constructor(message, options) {
    super(message, options);
    this.code = options?.status ?? 500;
  }
};

// https://esm.sh/v124/ieee754@1.2.1/es2022/ieee754.mjs
var ieee754_exports = {};
__export(ieee754_exports, {
  default: () => O2,
  read: () => H2,
  write: () => J2
});
var y2 = Object.create;
var v2 = Object.defineProperty;
var z2 = Object.getOwnPropertyDescriptor;
var A2 = Object.getOwnPropertyNames;
var C2 = Object.getPrototypeOf;
var D2 = Object.prototype.hasOwnProperty;
var F = (a4, r3) => () => (r3 || a4((r3 = { exports: {} }).exports, r3), r3.exports);
var G2 = (a4, r3) => {
  for (var i3 in r3)
    v2(a4, i3, { get: r3[i3], enumerable: true });
};
var e = (a4, r3, i3, f8) => {
  if (r3 && typeof r3 == "object" || typeof r3 == "function")
    for (let o3 of A2(r3))
      !D2.call(a4, o3) && o3 !== i3 && v2(a4, o3, { get: () => r3[o3], enumerable: !(f8 = z2(r3, o3)) || f8.enumerable });
  return a4;
};
var _ = (a4, r3, i3) => (e(a4, r3, "default"), i3 && e(i3, r3, "default"));
var B2 = (a4, r3, i3) => (i3 = a4 != null ? y2(C2(a4)) : {}, e(r3 || !a4 || !a4.__esModule ? v2(i3, "default", { value: a4, enumerable: true }) : i3, a4));
var g2 = F((I7) => {
  I7.read = function(a4, r3, i3, f8, o3) {
    var h4, t3, w9 = o3 * 8 - f8 - 1, s2 = (1 << w9) - 1, N8 = s2 >> 1, M8 = -7, p10 = i3 ? o3 - 1 : 0, c8 = i3 ? -1 : 1, d6 = a4[r3 + p10];
    for (p10 += c8, h4 = d6 & (1 << -M8) - 1, d6 >>= -M8, M8 += w9; M8 > 0; h4 = h4 * 256 + a4[r3 + p10], p10 += c8, M8 -= 8)
      ;
    for (t3 = h4 & (1 << -M8) - 1, h4 >>= -M8, M8 += f8; M8 > 0; t3 = t3 * 256 + a4[r3 + p10], p10 += c8, M8 -= 8)
      ;
    if (h4 === 0)
      h4 = 1 - N8;
    else {
      if (h4 === s2)
        return t3 ? NaN : (d6 ? -1 : 1) * (1 / 0);
      t3 = t3 + Math.pow(2, f8), h4 = h4 - N8;
    }
    return (d6 ? -1 : 1) * t3 * Math.pow(2, h4 - f8);
  };
  I7.write = function(a4, r3, i3, f8, o3, h4) {
    var t3, w9, s2, N8 = h4 * 8 - o3 - 1, M8 = (1 << N8) - 1, p10 = M8 >> 1, c8 = o3 === 23 ? Math.pow(2, -24) - Math.pow(2, -77) : 0, d6 = f8 ? 0 : h4 - 1, n4 = f8 ? 1 : -1, q7 = r3 < 0 || r3 === 0 && 1 / r3 < 0 ? 1 : 0;
    for (r3 = Math.abs(r3), isNaN(r3) || r3 === 1 / 0 ? (w9 = isNaN(r3) ? 1 : 0, t3 = M8) : (t3 = Math.floor(Math.log(r3) / Math.LN2), r3 * (s2 = Math.pow(2, -t3)) < 1 && (t3--, s2 *= 2), t3 + p10 >= 1 ? r3 += c8 / s2 : r3 += c8 * Math.pow(2, 1 - p10), r3 * s2 >= 2 && (t3++, s2 /= 2), t3 + p10 >= M8 ? (w9 = 0, t3 = M8) : t3 + p10 >= 1 ? (w9 = (r3 * s2 - 1) * Math.pow(2, o3), t3 = t3 + p10) : (w9 = r3 * Math.pow(2, p10 - 1) * Math.pow(2, o3), t3 = 0)); o3 >= 8; a4[i3 + d6] = w9 & 255, d6 += n4, w9 /= 256, o3 -= 8)
      ;
    for (t3 = t3 << o3 | w9, N8 += o3; N8 > 0; a4[i3 + d6] = t3 & 255, d6 += n4, t3 /= 256, N8 -= 8)
      ;
    a4[i3 + d6 - n4] |= q7 * 128;
  };
});
var x2 = {};
G2(x2, { default: () => O2, read: () => H2, write: () => J2 });
var k2 = B2(g2());
_(x2, B2(g2()));
var { read: H2, write: J2 } = k2;
var { default: j2, ...K2 } = k2;
var O2 = j2 !== void 0 ? j2 : K2;

// https://esm.sh/v124/base64-js@1.5.1/es2022/base64-js.mjs
var base64_js_exports = {};
__export(base64_js_exports, {
  byteLength: () => G3,
  default: () => N2,
  fromByteArray: () => K3,
  toByteArray: () => J3
});
var B3 = Object.create;
var l = Object.defineProperty;
var _2 = Object.getOwnPropertyDescriptor;
var k3 = Object.getOwnPropertyNames;
var w2 = Object.getPrototypeOf;
var j3 = Object.prototype.hasOwnProperty;
var H3 = (r3, e3) => () => (e3 || r3((e3 = { exports: {} }).exports, e3), e3.exports);
var U2 = (r3, e3) => {
  for (var t3 in e3)
    l(r3, t3, { get: e3[t3], enumerable: true });
};
var A3 = (r3, e3, t3, a4) => {
  if (e3 && typeof e3 == "object" || typeof e3 == "function")
    for (let o3 of k3(e3))
      !j3.call(r3, o3) && o3 !== t3 && l(r3, o3, { get: () => e3[o3], enumerable: !(a4 = _2(e3, o3)) || a4.enumerable });
  return r3;
};
var u2 = (r3, e3, t3) => (A3(r3, e3, "default"), t3 && A3(t3, e3, "default"));
var C3 = (r3, e3, t3) => (t3 = r3 != null ? B3(w2(r3)) : {}, A3(e3 || !r3 || !r3.__esModule ? l(t3, "default", { value: r3, enumerable: true }) : t3, r3));
var p2 = H3((y13) => {
  "use strict";
  y13.byteLength = I7;
  y13.toByteArray = T7;
  y13.fromByteArray = D9;
  var h4 = [], d6 = [], E8 = typeof Uint8Array < "u" ? Uint8Array : Array, s2 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
  for (F7 = 0, L8 = s2.length; F7 < L8; ++F7)
    h4[F7] = s2[F7], d6[s2.charCodeAt(F7)] = F7;
  var F7, L8;
  d6["-".charCodeAt(0)] = 62;
  d6["_".charCodeAt(0)] = 63;
  function g9(r3) {
    var e3 = r3.length;
    if (e3 % 4 > 0)
      throw new Error("Invalid string. Length must be a multiple of 4");
    var t3 = r3.indexOf("=");
    t3 === -1 && (t3 = e3);
    var a4 = t3 === e3 ? 0 : 4 - t3 % 4;
    return [t3, a4];
  }
  function I7(r3) {
    var e3 = g9(r3), t3 = e3[0], a4 = e3[1];
    return (t3 + a4) * 3 / 4 - a4;
  }
  function O7(r3, e3, t3) {
    return (e3 + t3) * 3 / 4 - t3;
  }
  function T7(r3) {
    var e3, t3 = g9(r3), a4 = t3[0], o3 = t3[1], n4 = new E8(O7(r3, a4, o3)), v12 = 0, x8 = o3 > 0 ? a4 - 4 : a4, f8;
    for (f8 = 0; f8 < x8; f8 += 4)
      e3 = d6[r3.charCodeAt(f8)] << 18 | d6[r3.charCodeAt(f8 + 1)] << 12 | d6[r3.charCodeAt(f8 + 2)] << 6 | d6[r3.charCodeAt(f8 + 3)], n4[v12++] = e3 >> 16 & 255, n4[v12++] = e3 >> 8 & 255, n4[v12++] = e3 & 255;
    return o3 === 2 && (e3 = d6[r3.charCodeAt(f8)] << 2 | d6[r3.charCodeAt(f8 + 1)] >> 4, n4[v12++] = e3 & 255), o3 === 1 && (e3 = d6[r3.charCodeAt(f8)] << 10 | d6[r3.charCodeAt(f8 + 1)] << 4 | d6[r3.charCodeAt(f8 + 2)] >> 2, n4[v12++] = e3 >> 8 & 255, n4[v12++] = e3 & 255), n4;
  }
  function q7(r3) {
    return h4[r3 >> 18 & 63] + h4[r3 >> 12 & 63] + h4[r3 >> 6 & 63] + h4[r3 & 63];
  }
  function z8(r3, e3, t3) {
    for (var a4, o3 = [], n4 = e3; n4 < t3; n4 += 3)
      a4 = (r3[n4] << 16 & 16711680) + (r3[n4 + 1] << 8 & 65280) + (r3[n4 + 2] & 255), o3.push(q7(a4));
    return o3.join("");
  }
  function D9(r3) {
    for (var e3, t3 = r3.length, a4 = t3 % 3, o3 = [], n4 = 16383, v12 = 0, x8 = t3 - a4; v12 < x8; v12 += n4)
      o3.push(z8(r3, v12, v12 + n4 > x8 ? x8 : v12 + n4));
    return a4 === 1 ? (e3 = r3[t3 - 1], o3.push(h4[e3 >> 2] + h4[e3 << 4 & 63] + "==")) : a4 === 2 && (e3 = (r3[t3 - 2] << 8) + r3[t3 - 1], o3.push(h4[e3 >> 10] + h4[e3 >> 4 & 63] + h4[e3 << 2 & 63] + "=")), o3.join("");
  }
});
var c2 = {};
U2(c2, { byteLength: () => G3, default: () => N2, fromByteArray: () => K3, toByteArray: () => J3 });
var i = C3(p2());
u2(c2, C3(p2()));
var { byteLength: G3, toByteArray: J3, fromByteArray: K3 } = i;
var { default: m2, ...M2 } = i;
var N2 = m2 !== void 0 ? m2 : M2;

// https://esm.sh/v124/buffer@6.0.3/es2022/buffer.bundle.mjs
var v3 = ((s2) => typeof __require < "u" ? __require : typeof Proxy < "u" ? new Proxy(s2, { get: (h4, c8) => (typeof __require < "u" ? __require : h4)[c8] }) : s2)(function(s2) {
  if (typeof __require < "u")
    return __require.apply(this, arguments);
  throw new Error('Dynamic require of "' + s2 + '" is not supported');
});
var Lt = O2 ?? ieee754_exports;
var xt2 = N2 ?? base64_js_exports;
var $t = Object.create;
var k4 = Object.defineProperty;
var Pt = Object.getOwnPropertyDescriptor;
var Ct2 = Object.getOwnPropertyNames;
var Mt = Object.getPrototypeOf;
var Nt = Object.prototype.hasOwnProperty;
var Wt = ((s2) => typeof v3 < "u" ? v3 : typeof Proxy < "u" ? new Proxy(s2, { get: (h4, c8) => (typeof v3 < "u" ? v3 : h4)[c8] }) : s2)(function(s2) {
  if (typeof v3 < "u")
    return v3.apply(this, arguments);
  throw new Error('Dynamic require of "' + s2 + '" is not supported');
});
var kt2 = (s2, h4) => () => (h4 || s2((h4 = { exports: {} }).exports, h4), h4.exports);
var jt = (s2, h4) => {
  for (var c8 in h4)
    k4(s2, c8, { get: h4[c8], enumerable: true });
};
var C4 = (s2, h4, c8, R8) => {
  if (h4 && typeof h4 == "object" || typeof h4 == "function")
    for (let b7 of Ct2(h4))
      !Nt.call(s2, b7) && b7 !== c8 && k4(s2, b7, { get: () => h4[b7], enumerable: !(R8 = Pt(h4, b7)) || R8.enumerable });
  return s2;
};
var Ft = (s2, h4, c8) => (C4(s2, h4, "default"), c8 && C4(c8, h4, "default"));
var nt2 = (s2, h4, c8) => (c8 = s2 != null ? $t(Mt(s2)) : {}, C4(h4 || !s2 || !s2.__esModule ? k4(c8, "default", { value: s2, enumerable: true }) : c8, s2));
var rt2 = kt2((s2) => {
  "use strict";
  var h4 = xt2, c8 = Lt, R8 = typeof Symbol == "function" && typeof Symbol.for == "function" ? Symbol.for("nodejs.util.inspect.custom") : null;
  s2.Buffer = i3, s2.SlowBuffer = at5, s2.INSPECT_MAX_BYTES = 50;
  var b7 = 2147483647;
  s2.kMaxLength = b7, i3.TYPED_ARRAY_SUPPORT = ft3(), !i3.TYPED_ARRAY_SUPPORT && typeof console < "u" && typeof console.error == "function" && console.error("This browser lacks typed array (Uint8Array) support which is required by `buffer` v5.x. Use `buffer` v4.x if you require old browser support.");
  function ft3() {
    try {
      let t3 = new Uint8Array(1), e3 = { foo: function() {
        return 42;
      } };
      return Object.setPrototypeOf(e3, Uint8Array.prototype), Object.setPrototypeOf(t3, e3), t3.foo() === 42;
    } catch {
      return false;
    }
  }
  Object.defineProperty(i3.prototype, "parent", { enumerable: true, get: function() {
    if (i3.isBuffer(this))
      return this.buffer;
  } }), Object.defineProperty(i3.prototype, "offset", { enumerable: true, get: function() {
    if (i3.isBuffer(this))
      return this.byteOffset;
  } });
  function m10(t3) {
    if (t3 > b7)
      throw new RangeError('The value "' + t3 + '" is invalid for option "size"');
    let e3 = new Uint8Array(t3);
    return Object.setPrototypeOf(e3, i3.prototype), e3;
  }
  function i3(t3, e3, n4) {
    if (typeof t3 == "number") {
      if (typeof e3 == "string")
        throw new TypeError('The "string" argument must be of type string. Received type number');
      return _10(t3);
    }
    return j10(t3, e3, n4);
  }
  i3.poolSize = 8192;
  function j10(t3, e3, n4) {
    if (typeof t3 == "string")
      return st8(t3, e3);
    if (ArrayBuffer.isView(t3))
      return ht4(t3);
    if (t3 == null)
      throw new TypeError("The first argument must be one of type string, Buffer, ArrayBuffer, Array, or Array-like Object. Received type " + typeof t3);
    if (B9(t3, ArrayBuffer) || t3 && B9(t3.buffer, ArrayBuffer) || typeof SharedArrayBuffer < "u" && (B9(t3, SharedArrayBuffer) || t3 && B9(t3.buffer, SharedArrayBuffer)))
      return D9(t3, e3, n4);
    if (typeof t3 == "number")
      throw new TypeError('The "value" argument must not be of type number. Received type number');
    let r3 = t3.valueOf && t3.valueOf();
    if (r3 != null && r3 !== t3)
      return i3.from(r3, e3, n4);
    let o3 = lt4(t3);
    if (o3)
      return o3;
    if (typeof Symbol < "u" && Symbol.toPrimitive != null && typeof t3[Symbol.toPrimitive] == "function")
      return i3.from(t3[Symbol.toPrimitive]("string"), e3, n4);
    throw new TypeError("The first argument must be one of type string, Buffer, ArrayBuffer, Array, or Array-like Object. Received type " + typeof t3);
  }
  i3.from = function(t3, e3, n4) {
    return j10(t3, e3, n4);
  }, Object.setPrototypeOf(i3.prototype, Uint8Array.prototype), Object.setPrototypeOf(i3, Uint8Array);
  function F7(t3) {
    if (typeof t3 != "number")
      throw new TypeError('"size" argument must be of type number');
    if (t3 < 0)
      throw new RangeError('The value "' + t3 + '" is invalid for option "size"');
  }
  function ut5(t3, e3, n4) {
    return F7(t3), t3 <= 0 ? m10(t3) : e3 !== void 0 ? typeof n4 == "string" ? m10(t3).fill(e3, n4) : m10(t3).fill(e3) : m10(t3);
  }
  i3.alloc = function(t3, e3, n4) {
    return ut5(t3, e3, n4);
  };
  function _10(t3) {
    return F7(t3), m10(t3 < 0 ? 0 : L8(t3) | 0);
  }
  i3.allocUnsafe = function(t3) {
    return _10(t3);
  }, i3.allocUnsafeSlow = function(t3) {
    return _10(t3);
  };
  function st8(t3, e3) {
    if ((typeof e3 != "string" || e3 === "") && (e3 = "utf8"), !i3.isEncoding(e3))
      throw new TypeError("Unknown encoding: " + e3);
    let n4 = Y8(t3, e3) | 0, r3 = m10(n4), o3 = r3.write(t3, e3);
    return o3 !== n4 && (r3 = r3.slice(0, o3)), r3;
  }
  function S9(t3) {
    let e3 = t3.length < 0 ? 0 : L8(t3.length) | 0, n4 = m10(e3);
    for (let r3 = 0; r3 < e3; r3 += 1)
      n4[r3] = t3[r3] & 255;
    return n4;
  }
  function ht4(t3) {
    if (B9(t3, Uint8Array)) {
      let e3 = new Uint8Array(t3);
      return D9(e3.buffer, e3.byteOffset, e3.byteLength);
    }
    return S9(t3);
  }
  function D9(t3, e3, n4) {
    if (e3 < 0 || t3.byteLength < e3)
      throw new RangeError('"offset" is outside of buffer bounds');
    if (t3.byteLength < e3 + (n4 || 0))
      throw new RangeError('"length" is outside of buffer bounds');
    let r3;
    return e3 === void 0 && n4 === void 0 ? r3 = new Uint8Array(t3) : n4 === void 0 ? r3 = new Uint8Array(t3, e3) : r3 = new Uint8Array(t3, e3, n4), Object.setPrototypeOf(r3, i3.prototype), r3;
  }
  function lt4(t3) {
    if (i3.isBuffer(t3)) {
      let e3 = L8(t3.length) | 0, n4 = m10(e3);
      return n4.length === 0 || t3.copy(n4, 0, 0, e3), n4;
    }
    if (t3.length !== void 0)
      return typeof t3.length != "number" || P8(t3.length) ? m10(0) : S9(t3);
    if (t3.type === "Buffer" && Array.isArray(t3.data))
      return S9(t3.data);
  }
  function L8(t3) {
    if (t3 >= b7)
      throw new RangeError("Attempt to allocate Buffer larger than maximum size: 0x" + b7.toString(16) + " bytes");
    return t3 | 0;
  }
  function at5(t3) {
    return +t3 != t3 && (t3 = 0), i3.alloc(+t3);
  }
  i3.isBuffer = function(t3) {
    return t3 != null && t3._isBuffer === true && t3 !== i3.prototype;
  }, i3.compare = function(t3, e3) {
    if (B9(t3, Uint8Array) && (t3 = i3.from(t3, t3.offset, t3.byteLength)), B9(e3, Uint8Array) && (e3 = i3.from(e3, e3.offset, e3.byteLength)), !i3.isBuffer(t3) || !i3.isBuffer(e3))
      throw new TypeError('The "buf1", "buf2" arguments must be one of type Buffer or Uint8Array');
    if (t3 === e3)
      return 0;
    let n4 = t3.length, r3 = e3.length;
    for (let o3 = 0, f8 = Math.min(n4, r3); o3 < f8; ++o3)
      if (t3[o3] !== e3[o3]) {
        n4 = t3[o3], r3 = e3[o3];
        break;
      }
    return n4 < r3 ? -1 : r3 < n4 ? 1 : 0;
  }, i3.isEncoding = function(t3) {
    switch (String(t3).toLowerCase()) {
      case "hex":
      case "utf8":
      case "utf-8":
      case "ascii":
      case "latin1":
      case "binary":
      case "base64":
      case "ucs2":
      case "ucs-2":
      case "utf16le":
      case "utf-16le":
        return true;
      default:
        return false;
    }
  }, i3.concat = function(t3, e3) {
    if (!Array.isArray(t3))
      throw new TypeError('"list" argument must be an Array of Buffers');
    if (t3.length === 0)
      return i3.alloc(0);
    let n4;
    if (e3 === void 0)
      for (e3 = 0, n4 = 0; n4 < t3.length; ++n4)
        e3 += t3[n4].length;
    let r3 = i3.allocUnsafe(e3), o3 = 0;
    for (n4 = 0; n4 < t3.length; ++n4) {
      let f8 = t3[n4];
      if (B9(f8, Uint8Array))
        o3 + f8.length > r3.length ? (i3.isBuffer(f8) || (f8 = i3.from(f8)), f8.copy(r3, o3)) : Uint8Array.prototype.set.call(r3, f8, o3);
      else if (i3.isBuffer(f8))
        f8.copy(r3, o3);
      else
        throw new TypeError('"list" argument must be an Array of Buffers');
      o3 += f8.length;
    }
    return r3;
  };
  function Y8(t3, e3) {
    if (i3.isBuffer(t3))
      return t3.length;
    if (ArrayBuffer.isView(t3) || B9(t3, ArrayBuffer))
      return t3.byteLength;
    if (typeof t3 != "string")
      throw new TypeError('The "string" argument must be one of type string, Buffer, or ArrayBuffer. Received type ' + typeof t3);
    let n4 = t3.length, r3 = arguments.length > 2 && arguments[2] === true;
    if (!r3 && n4 === 0)
      return 0;
    let o3 = false;
    for (; ; )
      switch (e3) {
        case "ascii":
        case "latin1":
        case "binary":
          return n4;
        case "utf8":
        case "utf-8":
          return $5(t3).length;
        case "ucs2":
        case "ucs-2":
        case "utf16le":
        case "utf-16le":
          return n4 * 2;
        case "hex":
          return n4 >>> 1;
        case "base64":
          return tt8(t3).length;
        default:
          if (o3)
            return r3 ? -1 : $5(t3).length;
          e3 = ("" + e3).toLowerCase(), o3 = true;
      }
  }
  i3.byteLength = Y8;
  function pt4(t3, e3, n4) {
    let r3 = false;
    if ((e3 === void 0 || e3 < 0) && (e3 = 0), e3 > this.length || ((n4 === void 0 || n4 > this.length) && (n4 = this.length), n4 <= 0) || (n4 >>>= 0, e3 >>>= 0, n4 <= e3))
      return "";
    for (t3 || (t3 = "utf8"); ; )
      switch (t3) {
        case "hex":
          return It2(this, e3, n4);
        case "utf8":
        case "utf-8":
          return G10(this, e3, n4);
        case "ascii":
          return mt3(this, e3, n4);
        case "latin1":
        case "binary":
          return Et2(this, e3, n4);
        case "base64":
          return bt3(this, e3, n4);
        case "ucs2":
        case "ucs-2":
        case "utf16le":
        case "utf-16le":
          return vt3(this, e3, n4);
        default:
          if (r3)
            throw new TypeError("Unknown encoding: " + t3);
          t3 = (t3 + "").toLowerCase(), r3 = true;
      }
  }
  i3.prototype._isBuffer = true;
  function I7(t3, e3, n4) {
    let r3 = t3[e3];
    t3[e3] = t3[n4], t3[n4] = r3;
  }
  i3.prototype.swap16 = function() {
    let t3 = this.length;
    if (t3 % 2 !== 0)
      throw new RangeError("Buffer size must be a multiple of 16-bits");
    for (let e3 = 0; e3 < t3; e3 += 2)
      I7(this, e3, e3 + 1);
    return this;
  }, i3.prototype.swap32 = function() {
    let t3 = this.length;
    if (t3 % 4 !== 0)
      throw new RangeError("Buffer size must be a multiple of 32-bits");
    for (let e3 = 0; e3 < t3; e3 += 4)
      I7(this, e3, e3 + 3), I7(this, e3 + 1, e3 + 2);
    return this;
  }, i3.prototype.swap64 = function() {
    let t3 = this.length;
    if (t3 % 8 !== 0)
      throw new RangeError("Buffer size must be a multiple of 64-bits");
    for (let e3 = 0; e3 < t3; e3 += 8)
      I7(this, e3, e3 + 7), I7(this, e3 + 1, e3 + 6), I7(this, e3 + 2, e3 + 5), I7(this, e3 + 3, e3 + 4);
    return this;
  }, i3.prototype.toString = function() {
    let t3 = this.length;
    return t3 === 0 ? "" : arguments.length === 0 ? G10(this, 0, t3) : pt4.apply(this, arguments);
  }, i3.prototype.toLocaleString = i3.prototype.toString, i3.prototype.equals = function(t3) {
    if (!i3.isBuffer(t3))
      throw new TypeError("Argument must be a Buffer");
    return this === t3 ? true : i3.compare(this, t3) === 0;
  }, i3.prototype.inspect = function() {
    let t3 = "", e3 = s2.INSPECT_MAX_BYTES;
    return t3 = this.toString("hex", 0, e3).replace(/(.{2})/g, "$1 ").trim(), this.length > e3 && (t3 += " ... "), "<Buffer " + t3 + ">";
  }, R8 && (i3.prototype[R8] = i3.prototype.inspect), i3.prototype.compare = function(t3, e3, n4, r3, o3) {
    if (B9(t3, Uint8Array) && (t3 = i3.from(t3, t3.offset, t3.byteLength)), !i3.isBuffer(t3))
      throw new TypeError('The "target" argument must be one of type Buffer or Uint8Array. Received type ' + typeof t3);
    if (e3 === void 0 && (e3 = 0), n4 === void 0 && (n4 = t3 ? t3.length : 0), r3 === void 0 && (r3 = 0), o3 === void 0 && (o3 = this.length), e3 < 0 || n4 > t3.length || r3 < 0 || o3 > this.length)
      throw new RangeError("out of range index");
    if (r3 >= o3 && e3 >= n4)
      return 0;
    if (r3 >= o3)
      return -1;
    if (e3 >= n4)
      return 1;
    if (e3 >>>= 0, n4 >>>= 0, r3 >>>= 0, o3 >>>= 0, this === t3)
      return 0;
    let f8 = o3 - r3, u7 = n4 - e3, l5 = Math.min(f8, u7), w9 = this.slice(r3, o3), p10 = t3.slice(e3, n4);
    for (let a4 = 0; a4 < l5; ++a4)
      if (w9[a4] !== p10[a4]) {
        f8 = w9[a4], u7 = p10[a4];
        break;
      }
    return f8 < u7 ? -1 : u7 < f8 ? 1 : 0;
  };
  function q7(t3, e3, n4, r3, o3) {
    if (t3.length === 0)
      return -1;
    if (typeof n4 == "string" ? (r3 = n4, n4 = 0) : n4 > 2147483647 ? n4 = 2147483647 : n4 < -2147483648 && (n4 = -2147483648), n4 = +n4, P8(n4) && (n4 = o3 ? 0 : t3.length - 1), n4 < 0 && (n4 = t3.length + n4), n4 >= t3.length) {
      if (o3)
        return -1;
      n4 = t3.length - 1;
    } else if (n4 < 0)
      if (o3)
        n4 = 0;
      else
        return -1;
    if (typeof e3 == "string" && (e3 = i3.from(e3, r3)), i3.isBuffer(e3))
      return e3.length === 0 ? -1 : z8(t3, e3, n4, r3, o3);
    if (typeof e3 == "number")
      return e3 = e3 & 255, typeof Uint8Array.prototype.indexOf == "function" ? o3 ? Uint8Array.prototype.indexOf.call(t3, e3, n4) : Uint8Array.prototype.lastIndexOf.call(t3, e3, n4) : z8(t3, [e3], n4, r3, o3);
    throw new TypeError("val must be string, number or Buffer");
  }
  function z8(t3, e3, n4, r3, o3) {
    let f8 = 1, u7 = t3.length, l5 = e3.length;
    if (r3 !== void 0 && (r3 = String(r3).toLowerCase(), r3 === "ucs2" || r3 === "ucs-2" || r3 === "utf16le" || r3 === "utf-16le")) {
      if (t3.length < 2 || e3.length < 2)
        return -1;
      f8 = 2, u7 /= 2, l5 /= 2, n4 /= 2;
    }
    function w9(a4, y13) {
      return f8 === 1 ? a4[y13] : a4.readUInt16BE(y13 * f8);
    }
    let p10;
    if (o3) {
      let a4 = -1;
      for (p10 = n4; p10 < u7; p10++)
        if (w9(t3, p10) === w9(e3, a4 === -1 ? 0 : p10 - a4)) {
          if (a4 === -1 && (a4 = p10), p10 - a4 + 1 === l5)
            return a4 * f8;
        } else
          a4 !== -1 && (p10 -= p10 - a4), a4 = -1;
    } else
      for (n4 + l5 > u7 && (n4 = u7 - l5), p10 = n4; p10 >= 0; p10--) {
        let a4 = true;
        for (let y13 = 0; y13 < l5; y13++)
          if (w9(t3, p10 + y13) !== w9(e3, y13)) {
            a4 = false;
            break;
          }
        if (a4)
          return p10;
      }
    return -1;
  }
  i3.prototype.includes = function(t3, e3, n4) {
    return this.indexOf(t3, e3, n4) !== -1;
  }, i3.prototype.indexOf = function(t3, e3, n4) {
    return q7(this, t3, e3, n4, true);
  }, i3.prototype.lastIndexOf = function(t3, e3, n4) {
    return q7(this, t3, e3, n4, false);
  };
  function ct4(t3, e3, n4, r3) {
    n4 = Number(n4) || 0;
    let o3 = t3.length - n4;
    r3 ? (r3 = Number(r3), r3 > o3 && (r3 = o3)) : r3 = o3;
    let f8 = e3.length;
    r3 > f8 / 2 && (r3 = f8 / 2);
    let u7;
    for (u7 = 0; u7 < r3; ++u7) {
      let l5 = parseInt(e3.substr(u7 * 2, 2), 16);
      if (P8(l5))
        return u7;
      t3[n4 + u7] = l5;
    }
    return u7;
  }
  function yt2(t3, e3, n4, r3) {
    return T7($5(e3, t3.length - n4), t3, n4, r3);
  }
  function gt3(t3, e3, n4, r3) {
    return T7(Ot2(e3), t3, n4, r3);
  }
  function wt3(t3, e3, n4, r3) {
    return T7(tt8(e3), t3, n4, r3);
  }
  function dt5(t3, e3, n4, r3) {
    return T7(Tt3(e3, t3.length - n4), t3, n4, r3);
  }
  i3.prototype.write = function(t3, e3, n4, r3) {
    if (e3 === void 0)
      r3 = "utf8", n4 = this.length, e3 = 0;
    else if (n4 === void 0 && typeof e3 == "string")
      r3 = e3, n4 = this.length, e3 = 0;
    else if (isFinite(e3))
      e3 = e3 >>> 0, isFinite(n4) ? (n4 = n4 >>> 0, r3 === void 0 && (r3 = "utf8")) : (r3 = n4, n4 = void 0);
    else
      throw new Error("Buffer.write(string, encoding, offset[, length]) is no longer supported");
    let o3 = this.length - e3;
    if ((n4 === void 0 || n4 > o3) && (n4 = o3), t3.length > 0 && (n4 < 0 || e3 < 0) || e3 > this.length)
      throw new RangeError("Attempt to write outside buffer bounds");
    r3 || (r3 = "utf8");
    let f8 = false;
    for (; ; )
      switch (r3) {
        case "hex":
          return ct4(this, t3, e3, n4);
        case "utf8":
        case "utf-8":
          return yt2(this, t3, e3, n4);
        case "ascii":
        case "latin1":
        case "binary":
          return gt3(this, t3, e3, n4);
        case "base64":
          return wt3(this, t3, e3, n4);
        case "ucs2":
        case "ucs-2":
        case "utf16le":
        case "utf-16le":
          return dt5(this, t3, e3, n4);
        default:
          if (f8)
            throw new TypeError("Unknown encoding: " + r3);
          r3 = ("" + r3).toLowerCase(), f8 = true;
      }
  }, i3.prototype.toJSON = function() {
    return { type: "Buffer", data: Array.prototype.slice.call(this._arr || this, 0) };
  };
  function bt3(t3, e3, n4) {
    return e3 === 0 && n4 === t3.length ? h4.fromByteArray(t3) : h4.fromByteArray(t3.slice(e3, n4));
  }
  function G10(t3, e3, n4) {
    n4 = Math.min(t3.length, n4);
    let r3 = [], o3 = e3;
    for (; o3 < n4; ) {
      let f8 = t3[o3], u7 = null, l5 = f8 > 239 ? 4 : f8 > 223 ? 3 : f8 > 191 ? 2 : 1;
      if (o3 + l5 <= n4) {
        let w9, p10, a4, y13;
        switch (l5) {
          case 1:
            f8 < 128 && (u7 = f8);
            break;
          case 2:
            w9 = t3[o3 + 1], (w9 & 192) === 128 && (y13 = (f8 & 31) << 6 | w9 & 63, y13 > 127 && (u7 = y13));
            break;
          case 3:
            w9 = t3[o3 + 1], p10 = t3[o3 + 2], (w9 & 192) === 128 && (p10 & 192) === 128 && (y13 = (f8 & 15) << 12 | (w9 & 63) << 6 | p10 & 63, y13 > 2047 && (y13 < 55296 || y13 > 57343) && (u7 = y13));
            break;
          case 4:
            w9 = t3[o3 + 1], p10 = t3[o3 + 2], a4 = t3[o3 + 3], (w9 & 192) === 128 && (p10 & 192) === 128 && (a4 & 192) === 128 && (y13 = (f8 & 15) << 18 | (w9 & 63) << 12 | (p10 & 63) << 6 | a4 & 63, y13 > 65535 && y13 < 1114112 && (u7 = y13));
        }
      }
      u7 === null ? (u7 = 65533, l5 = 1) : u7 > 65535 && (u7 -= 65536, r3.push(u7 >>> 10 & 1023 | 55296), u7 = 56320 | u7 & 1023), r3.push(u7), o3 += l5;
    }
    return Bt2(r3);
  }
  var X8 = 4096;
  function Bt2(t3) {
    let e3 = t3.length;
    if (e3 <= X8)
      return String.fromCharCode.apply(String, t3);
    let n4 = "", r3 = 0;
    for (; r3 < e3; )
      n4 += String.fromCharCode.apply(String, t3.slice(r3, r3 += X8));
    return n4;
  }
  function mt3(t3, e3, n4) {
    let r3 = "";
    n4 = Math.min(t3.length, n4);
    for (let o3 = e3; o3 < n4; ++o3)
      r3 += String.fromCharCode(t3[o3] & 127);
    return r3;
  }
  function Et2(t3, e3, n4) {
    let r3 = "";
    n4 = Math.min(t3.length, n4);
    for (let o3 = e3; o3 < n4; ++o3)
      r3 += String.fromCharCode(t3[o3]);
    return r3;
  }
  function It2(t3, e3, n4) {
    let r3 = t3.length;
    (!e3 || e3 < 0) && (e3 = 0), (!n4 || n4 < 0 || n4 > r3) && (n4 = r3);
    let o3 = "";
    for (let f8 = e3; f8 < n4; ++f8)
      o3 += _t4[t3[f8]];
    return o3;
  }
  function vt3(t3, e3, n4) {
    let r3 = t3.slice(e3, n4), o3 = "";
    for (let f8 = 0; f8 < r3.length - 1; f8 += 2)
      o3 += String.fromCharCode(r3[f8] + r3[f8 + 1] * 256);
    return o3;
  }
  i3.prototype.slice = function(t3, e3) {
    let n4 = this.length;
    t3 = ~~t3, e3 = e3 === void 0 ? n4 : ~~e3, t3 < 0 ? (t3 += n4, t3 < 0 && (t3 = 0)) : t3 > n4 && (t3 = n4), e3 < 0 ? (e3 += n4, e3 < 0 && (e3 = 0)) : e3 > n4 && (e3 = n4), e3 < t3 && (e3 = t3);
    let r3 = this.subarray(t3, e3);
    return Object.setPrototypeOf(r3, i3.prototype), r3;
  };
  function g9(t3, e3, n4) {
    if (t3 % 1 !== 0 || t3 < 0)
      throw new RangeError("offset is not uint");
    if (t3 + e3 > n4)
      throw new RangeError("Trying to access beyond buffer length");
  }
  i3.prototype.readUintLE = i3.prototype.readUIntLE = function(t3, e3, n4) {
    t3 = t3 >>> 0, e3 = e3 >>> 0, n4 || g9(t3, e3, this.length);
    let r3 = this[t3], o3 = 1, f8 = 0;
    for (; ++f8 < e3 && (o3 *= 256); )
      r3 += this[t3 + f8] * o3;
    return r3;
  }, i3.prototype.readUintBE = i3.prototype.readUIntBE = function(t3, e3, n4) {
    t3 = t3 >>> 0, e3 = e3 >>> 0, n4 || g9(t3, e3, this.length);
    let r3 = this[t3 + --e3], o3 = 1;
    for (; e3 > 0 && (o3 *= 256); )
      r3 += this[t3 + --e3] * o3;
    return r3;
  }, i3.prototype.readUint8 = i3.prototype.readUInt8 = function(t3, e3) {
    return t3 = t3 >>> 0, e3 || g9(t3, 1, this.length), this[t3];
  }, i3.prototype.readUint16LE = i3.prototype.readUInt16LE = function(t3, e3) {
    return t3 = t3 >>> 0, e3 || g9(t3, 2, this.length), this[t3] | this[t3 + 1] << 8;
  }, i3.prototype.readUint16BE = i3.prototype.readUInt16BE = function(t3, e3) {
    return t3 = t3 >>> 0, e3 || g9(t3, 2, this.length), this[t3] << 8 | this[t3 + 1];
  }, i3.prototype.readUint32LE = i3.prototype.readUInt32LE = function(t3, e3) {
    return t3 = t3 >>> 0, e3 || g9(t3, 4, this.length), (this[t3] | this[t3 + 1] << 8 | this[t3 + 2] << 16) + this[t3 + 3] * 16777216;
  }, i3.prototype.readUint32BE = i3.prototype.readUInt32BE = function(t3, e3) {
    return t3 = t3 >>> 0, e3 || g9(t3, 4, this.length), this[t3] * 16777216 + (this[t3 + 1] << 16 | this[t3 + 2] << 8 | this[t3 + 3]);
  }, i3.prototype.readBigUInt64LE = E8(function(t3) {
    t3 = t3 >>> 0, U9(t3, "offset");
    let e3 = this[t3], n4 = this[t3 + 7];
    (e3 === void 0 || n4 === void 0) && O7(t3, this.length - 8);
    let r3 = e3 + this[++t3] * 2 ** 8 + this[++t3] * 2 ** 16 + this[++t3] * 2 ** 24, o3 = this[++t3] + this[++t3] * 2 ** 8 + this[++t3] * 2 ** 16 + n4 * 2 ** 24;
    return BigInt(r3) + (BigInt(o3) << BigInt(32));
  }), i3.prototype.readBigUInt64BE = E8(function(t3) {
    t3 = t3 >>> 0, U9(t3, "offset");
    let e3 = this[t3], n4 = this[t3 + 7];
    (e3 === void 0 || n4 === void 0) && O7(t3, this.length - 8);
    let r3 = e3 * 2 ** 24 + this[++t3] * 2 ** 16 + this[++t3] * 2 ** 8 + this[++t3], o3 = this[++t3] * 2 ** 24 + this[++t3] * 2 ** 16 + this[++t3] * 2 ** 8 + n4;
    return (BigInt(r3) << BigInt(32)) + BigInt(o3);
  }), i3.prototype.readIntLE = function(t3, e3, n4) {
    t3 = t3 >>> 0, e3 = e3 >>> 0, n4 || g9(t3, e3, this.length);
    let r3 = this[t3], o3 = 1, f8 = 0;
    for (; ++f8 < e3 && (o3 *= 256); )
      r3 += this[t3 + f8] * o3;
    return o3 *= 128, r3 >= o3 && (r3 -= Math.pow(2, 8 * e3)), r3;
  }, i3.prototype.readIntBE = function(t3, e3, n4) {
    t3 = t3 >>> 0, e3 = e3 >>> 0, n4 || g9(t3, e3, this.length);
    let r3 = e3, o3 = 1, f8 = this[t3 + --r3];
    for (; r3 > 0 && (o3 *= 256); )
      f8 += this[t3 + --r3] * o3;
    return o3 *= 128, f8 >= o3 && (f8 -= Math.pow(2, 8 * e3)), f8;
  }, i3.prototype.readInt8 = function(t3, e3) {
    return t3 = t3 >>> 0, e3 || g9(t3, 1, this.length), this[t3] & 128 ? (255 - this[t3] + 1) * -1 : this[t3];
  }, i3.prototype.readInt16LE = function(t3, e3) {
    t3 = t3 >>> 0, e3 || g9(t3, 2, this.length);
    let n4 = this[t3] | this[t3 + 1] << 8;
    return n4 & 32768 ? n4 | 4294901760 : n4;
  }, i3.prototype.readInt16BE = function(t3, e3) {
    t3 = t3 >>> 0, e3 || g9(t3, 2, this.length);
    let n4 = this[t3 + 1] | this[t3] << 8;
    return n4 & 32768 ? n4 | 4294901760 : n4;
  }, i3.prototype.readInt32LE = function(t3, e3) {
    return t3 = t3 >>> 0, e3 || g9(t3, 4, this.length), this[t3] | this[t3 + 1] << 8 | this[t3 + 2] << 16 | this[t3 + 3] << 24;
  }, i3.prototype.readInt32BE = function(t3, e3) {
    return t3 = t3 >>> 0, e3 || g9(t3, 4, this.length), this[t3] << 24 | this[t3 + 1] << 16 | this[t3 + 2] << 8 | this[t3 + 3];
  }, i3.prototype.readBigInt64LE = E8(function(t3) {
    t3 = t3 >>> 0, U9(t3, "offset");
    let e3 = this[t3], n4 = this[t3 + 7];
    (e3 === void 0 || n4 === void 0) && O7(t3, this.length - 8);
    let r3 = this[t3 + 4] + this[t3 + 5] * 2 ** 8 + this[t3 + 6] * 2 ** 16 + (n4 << 24);
    return (BigInt(r3) << BigInt(32)) + BigInt(e3 + this[++t3] * 2 ** 8 + this[++t3] * 2 ** 16 + this[++t3] * 2 ** 24);
  }), i3.prototype.readBigInt64BE = E8(function(t3) {
    t3 = t3 >>> 0, U9(t3, "offset");
    let e3 = this[t3], n4 = this[t3 + 7];
    (e3 === void 0 || n4 === void 0) && O7(t3, this.length - 8);
    let r3 = (e3 << 24) + this[++t3] * 2 ** 16 + this[++t3] * 2 ** 8 + this[++t3];
    return (BigInt(r3) << BigInt(32)) + BigInt(this[++t3] * 2 ** 24 + this[++t3] * 2 ** 16 + this[++t3] * 2 ** 8 + n4);
  }), i3.prototype.readFloatLE = function(t3, e3) {
    return t3 = t3 >>> 0, e3 || g9(t3, 4, this.length), c8.read(this, t3, true, 23, 4);
  }, i3.prototype.readFloatBE = function(t3, e3) {
    return t3 = t3 >>> 0, e3 || g9(t3, 4, this.length), c8.read(this, t3, false, 23, 4);
  }, i3.prototype.readDoubleLE = function(t3, e3) {
    return t3 = t3 >>> 0, e3 || g9(t3, 8, this.length), c8.read(this, t3, true, 52, 8);
  }, i3.prototype.readDoubleBE = function(t3, e3) {
    return t3 = t3 >>> 0, e3 || g9(t3, 8, this.length), c8.read(this, t3, false, 52, 8);
  };
  function d6(t3, e3, n4, r3, o3, f8) {
    if (!i3.isBuffer(t3))
      throw new TypeError('"buffer" argument must be a Buffer instance');
    if (e3 > o3 || e3 < f8)
      throw new RangeError('"value" argument is out of bounds');
    if (n4 + r3 > t3.length)
      throw new RangeError("Index out of range");
  }
  i3.prototype.writeUintLE = i3.prototype.writeUIntLE = function(t3, e3, n4, r3) {
    if (t3 = +t3, e3 = e3 >>> 0, n4 = n4 >>> 0, !r3) {
      let u7 = Math.pow(2, 8 * n4) - 1;
      d6(this, t3, e3, n4, u7, 0);
    }
    let o3 = 1, f8 = 0;
    for (this[e3] = t3 & 255; ++f8 < n4 && (o3 *= 256); )
      this[e3 + f8] = t3 / o3 & 255;
    return e3 + n4;
  }, i3.prototype.writeUintBE = i3.prototype.writeUIntBE = function(t3, e3, n4, r3) {
    if (t3 = +t3, e3 = e3 >>> 0, n4 = n4 >>> 0, !r3) {
      let u7 = Math.pow(2, 8 * n4) - 1;
      d6(this, t3, e3, n4, u7, 0);
    }
    let o3 = n4 - 1, f8 = 1;
    for (this[e3 + o3] = t3 & 255; --o3 >= 0 && (f8 *= 256); )
      this[e3 + o3] = t3 / f8 & 255;
    return e3 + n4;
  }, i3.prototype.writeUint8 = i3.prototype.writeUInt8 = function(t3, e3, n4) {
    return t3 = +t3, e3 = e3 >>> 0, n4 || d6(this, t3, e3, 1, 255, 0), this[e3] = t3 & 255, e3 + 1;
  }, i3.prototype.writeUint16LE = i3.prototype.writeUInt16LE = function(t3, e3, n4) {
    return t3 = +t3, e3 = e3 >>> 0, n4 || d6(this, t3, e3, 2, 65535, 0), this[e3] = t3 & 255, this[e3 + 1] = t3 >>> 8, e3 + 2;
  }, i3.prototype.writeUint16BE = i3.prototype.writeUInt16BE = function(t3, e3, n4) {
    return t3 = +t3, e3 = e3 >>> 0, n4 || d6(this, t3, e3, 2, 65535, 0), this[e3] = t3 >>> 8, this[e3 + 1] = t3 & 255, e3 + 2;
  }, i3.prototype.writeUint32LE = i3.prototype.writeUInt32LE = function(t3, e3, n4) {
    return t3 = +t3, e3 = e3 >>> 0, n4 || d6(this, t3, e3, 4, 4294967295, 0), this[e3 + 3] = t3 >>> 24, this[e3 + 2] = t3 >>> 16, this[e3 + 1] = t3 >>> 8, this[e3] = t3 & 255, e3 + 4;
  }, i3.prototype.writeUint32BE = i3.prototype.writeUInt32BE = function(t3, e3, n4) {
    return t3 = +t3, e3 = e3 >>> 0, n4 || d6(this, t3, e3, 4, 4294967295, 0), this[e3] = t3 >>> 24, this[e3 + 1] = t3 >>> 16, this[e3 + 2] = t3 >>> 8, this[e3 + 3] = t3 & 255, e3 + 4;
  };
  function V7(t3, e3, n4, r3, o3) {
    Q8(e3, r3, o3, t3, n4, 7);
    let f8 = Number(e3 & BigInt(4294967295));
    t3[n4++] = f8, f8 = f8 >> 8, t3[n4++] = f8, f8 = f8 >> 8, t3[n4++] = f8, f8 = f8 >> 8, t3[n4++] = f8;
    let u7 = Number(e3 >> BigInt(32) & BigInt(4294967295));
    return t3[n4++] = u7, u7 = u7 >> 8, t3[n4++] = u7, u7 = u7 >> 8, t3[n4++] = u7, u7 = u7 >> 8, t3[n4++] = u7, n4;
  }
  function W8(t3, e3, n4, r3, o3) {
    Q8(e3, r3, o3, t3, n4, 7);
    let f8 = Number(e3 & BigInt(4294967295));
    t3[n4 + 7] = f8, f8 = f8 >> 8, t3[n4 + 6] = f8, f8 = f8 >> 8, t3[n4 + 5] = f8, f8 = f8 >> 8, t3[n4 + 4] = f8;
    let u7 = Number(e3 >> BigInt(32) & BigInt(4294967295));
    return t3[n4 + 3] = u7, u7 = u7 >> 8, t3[n4 + 2] = u7, u7 = u7 >> 8, t3[n4 + 1] = u7, u7 = u7 >> 8, t3[n4] = u7, n4 + 8;
  }
  i3.prototype.writeBigUInt64LE = E8(function(t3, e3 = 0) {
    return V7(this, t3, e3, BigInt(0), BigInt("0xffffffffffffffff"));
  }), i3.prototype.writeBigUInt64BE = E8(function(t3, e3 = 0) {
    return W8(this, t3, e3, BigInt(0), BigInt("0xffffffffffffffff"));
  }), i3.prototype.writeIntLE = function(t3, e3, n4, r3) {
    if (t3 = +t3, e3 = e3 >>> 0, !r3) {
      let l5 = Math.pow(2, 8 * n4 - 1);
      d6(this, t3, e3, n4, l5 - 1, -l5);
    }
    let o3 = 0, f8 = 1, u7 = 0;
    for (this[e3] = t3 & 255; ++o3 < n4 && (f8 *= 256); )
      t3 < 0 && u7 === 0 && this[e3 + o3 - 1] !== 0 && (u7 = 1), this[e3 + o3] = (t3 / f8 >> 0) - u7 & 255;
    return e3 + n4;
  }, i3.prototype.writeIntBE = function(t3, e3, n4, r3) {
    if (t3 = +t3, e3 = e3 >>> 0, !r3) {
      let l5 = Math.pow(2, 8 * n4 - 1);
      d6(this, t3, e3, n4, l5 - 1, -l5);
    }
    let o3 = n4 - 1, f8 = 1, u7 = 0;
    for (this[e3 + o3] = t3 & 255; --o3 >= 0 && (f8 *= 256); )
      t3 < 0 && u7 === 0 && this[e3 + o3 + 1] !== 0 && (u7 = 1), this[e3 + o3] = (t3 / f8 >> 0) - u7 & 255;
    return e3 + n4;
  }, i3.prototype.writeInt8 = function(t3, e3, n4) {
    return t3 = +t3, e3 = e3 >>> 0, n4 || d6(this, t3, e3, 1, 127, -128), t3 < 0 && (t3 = 255 + t3 + 1), this[e3] = t3 & 255, e3 + 1;
  }, i3.prototype.writeInt16LE = function(t3, e3, n4) {
    return t3 = +t3, e3 = e3 >>> 0, n4 || d6(this, t3, e3, 2, 32767, -32768), this[e3] = t3 & 255, this[e3 + 1] = t3 >>> 8, e3 + 2;
  }, i3.prototype.writeInt16BE = function(t3, e3, n4) {
    return t3 = +t3, e3 = e3 >>> 0, n4 || d6(this, t3, e3, 2, 32767, -32768), this[e3] = t3 >>> 8, this[e3 + 1] = t3 & 255, e3 + 2;
  }, i3.prototype.writeInt32LE = function(t3, e3, n4) {
    return t3 = +t3, e3 = e3 >>> 0, n4 || d6(this, t3, e3, 4, 2147483647, -2147483648), this[e3] = t3 & 255, this[e3 + 1] = t3 >>> 8, this[e3 + 2] = t3 >>> 16, this[e3 + 3] = t3 >>> 24, e3 + 4;
  }, i3.prototype.writeInt32BE = function(t3, e3, n4) {
    return t3 = +t3, e3 = e3 >>> 0, n4 || d6(this, t3, e3, 4, 2147483647, -2147483648), t3 < 0 && (t3 = 4294967295 + t3 + 1), this[e3] = t3 >>> 24, this[e3 + 1] = t3 >>> 16, this[e3 + 2] = t3 >>> 8, this[e3 + 3] = t3 & 255, e3 + 4;
  }, i3.prototype.writeBigInt64LE = E8(function(t3, e3 = 0) {
    return V7(this, t3, e3, -BigInt("0x8000000000000000"), BigInt("0x7fffffffffffffff"));
  }), i3.prototype.writeBigInt64BE = E8(function(t3, e3 = 0) {
    return W8(this, t3, e3, -BigInt("0x8000000000000000"), BigInt("0x7fffffffffffffff"));
  });
  function J10(t3, e3, n4, r3, o3, f8) {
    if (n4 + r3 > t3.length)
      throw new RangeError("Index out of range");
    if (n4 < 0)
      throw new RangeError("Index out of range");
  }
  function Z7(t3, e3, n4, r3, o3) {
    return e3 = +e3, n4 = n4 >>> 0, o3 || J10(t3, e3, n4, 4, 34028234663852886e22, -34028234663852886e22), c8.write(t3, e3, n4, r3, 23, 4), n4 + 4;
  }
  i3.prototype.writeFloatLE = function(t3, e3, n4) {
    return Z7(this, t3, e3, true, n4);
  }, i3.prototype.writeFloatBE = function(t3, e3, n4) {
    return Z7(this, t3, e3, false, n4);
  };
  function H10(t3, e3, n4, r3, o3) {
    return e3 = +e3, n4 = n4 >>> 0, o3 || J10(t3, e3, n4, 8, 17976931348623157e292, -17976931348623157e292), c8.write(t3, e3, n4, r3, 52, 8), n4 + 8;
  }
  i3.prototype.writeDoubleLE = function(t3, e3, n4) {
    return H10(this, t3, e3, true, n4);
  }, i3.prototype.writeDoubleBE = function(t3, e3, n4) {
    return H10(this, t3, e3, false, n4);
  }, i3.prototype.copy = function(t3, e3, n4, r3) {
    if (!i3.isBuffer(t3))
      throw new TypeError("argument should be a Buffer");
    if (n4 || (n4 = 0), !r3 && r3 !== 0 && (r3 = this.length), e3 >= t3.length && (e3 = t3.length), e3 || (e3 = 0), r3 > 0 && r3 < n4 && (r3 = n4), r3 === n4 || t3.length === 0 || this.length === 0)
      return 0;
    if (e3 < 0)
      throw new RangeError("targetStart out of bounds");
    if (n4 < 0 || n4 >= this.length)
      throw new RangeError("Index out of range");
    if (r3 < 0)
      throw new RangeError("sourceEnd out of bounds");
    r3 > this.length && (r3 = this.length), t3.length - e3 < r3 - n4 && (r3 = t3.length - e3 + n4);
    let o3 = r3 - n4;
    return this === t3 && typeof Uint8Array.prototype.copyWithin == "function" ? this.copyWithin(e3, n4, r3) : Uint8Array.prototype.set.call(t3, this.subarray(n4, r3), e3), o3;
  }, i3.prototype.fill = function(t3, e3, n4, r3) {
    if (typeof t3 == "string") {
      if (typeof e3 == "string" ? (r3 = e3, e3 = 0, n4 = this.length) : typeof n4 == "string" && (r3 = n4, n4 = this.length), r3 !== void 0 && typeof r3 != "string")
        throw new TypeError("encoding must be a string");
      if (typeof r3 == "string" && !i3.isEncoding(r3))
        throw new TypeError("Unknown encoding: " + r3);
      if (t3.length === 1) {
        let f8 = t3.charCodeAt(0);
        (r3 === "utf8" && f8 < 128 || r3 === "latin1") && (t3 = f8);
      }
    } else
      typeof t3 == "number" ? t3 = t3 & 255 : typeof t3 == "boolean" && (t3 = Number(t3));
    if (e3 < 0 || this.length < e3 || this.length < n4)
      throw new RangeError("Out of range index");
    if (n4 <= e3)
      return this;
    e3 = e3 >>> 0, n4 = n4 === void 0 ? this.length : n4 >>> 0, t3 || (t3 = 0);
    let o3;
    if (typeof t3 == "number")
      for (o3 = e3; o3 < n4; ++o3)
        this[o3] = t3;
    else {
      let f8 = i3.isBuffer(t3) ? t3 : i3.from(t3, r3), u7 = f8.length;
      if (u7 === 0)
        throw new TypeError('The value "' + t3 + '" is invalid for argument "value"');
      for (o3 = 0; o3 < n4 - e3; ++o3)
        this[o3 + e3] = f8[o3 % u7];
    }
    return this;
  };
  var A5 = {};
  function x8(t3, e3, n4) {
    A5[t3] = class extends n4 {
      constructor() {
        super(), Object.defineProperty(this, "message", { value: e3.apply(this, arguments), writable: true, configurable: true }), this.name = `${this.name} [${t3}]`, this.stack, delete this.name;
      }
      get code() {
        return t3;
      }
      set code(r3) {
        Object.defineProperty(this, "code", { configurable: true, enumerable: true, value: r3, writable: true });
      }
      toString() {
        return `${this.name} [${t3}]: ${this.message}`;
      }
    };
  }
  x8("ERR_BUFFER_OUT_OF_BOUNDS", function(t3) {
    return t3 ? `${t3} is outside of buffer bounds` : "Attempt to access memory outside buffer bounds";
  }, RangeError), x8("ERR_INVALID_ARG_TYPE", function(t3, e3) {
    return `The "${t3}" argument must be of type number. Received type ${typeof e3}`;
  }, TypeError), x8("ERR_OUT_OF_RANGE", function(t3, e3, n4) {
    let r3 = `The value of "${t3}" is out of range.`, o3 = n4;
    return Number.isInteger(n4) && Math.abs(n4) > 2 ** 32 ? o3 = K9(String(n4)) : typeof n4 == "bigint" && (o3 = String(n4), (n4 > BigInt(2) ** BigInt(32) || n4 < -(BigInt(2) ** BigInt(32))) && (o3 = K9(o3)), o3 += "n"), r3 += ` It must be ${e3}. Received ${o3}`, r3;
  }, RangeError);
  function K9(t3) {
    let e3 = "", n4 = t3.length, r3 = t3[0] === "-" ? 1 : 0;
    for (; n4 >= r3 + 4; n4 -= 3)
      e3 = `_${t3.slice(n4 - 3, n4)}${e3}`;
    return `${t3.slice(0, n4)}${e3}`;
  }
  function At3(t3, e3, n4) {
    U9(e3, "offset"), (t3[e3] === void 0 || t3[e3 + n4] === void 0) && O7(e3, t3.length - (n4 + 1));
  }
  function Q8(t3, e3, n4, r3, o3, f8) {
    if (t3 > n4 || t3 < e3) {
      let u7 = typeof e3 == "bigint" ? "n" : "", l5;
      throw f8 > 3 ? e3 === 0 || e3 === BigInt(0) ? l5 = `>= 0${u7} and < 2${u7} ** ${(f8 + 1) * 8}${u7}` : l5 = `>= -(2${u7} ** ${(f8 + 1) * 8 - 1}${u7}) and < 2 ** ${(f8 + 1) * 8 - 1}${u7}` : l5 = `>= ${e3}${u7} and <= ${n4}${u7}`, new A5.ERR_OUT_OF_RANGE("value", l5, t3);
    }
    At3(r3, o3, f8);
  }
  function U9(t3, e3) {
    if (typeof t3 != "number")
      throw new A5.ERR_INVALID_ARG_TYPE(e3, "number", t3);
  }
  function O7(t3, e3, n4) {
    throw Math.floor(t3) !== t3 ? (U9(t3, n4), new A5.ERR_OUT_OF_RANGE(n4 || "offset", "an integer", t3)) : e3 < 0 ? new A5.ERR_BUFFER_OUT_OF_BOUNDS() : new A5.ERR_OUT_OF_RANGE(n4 || "offset", `>= ${n4 ? 1 : 0} and <= ${e3}`, t3);
  }
  var Ut = /[^+/0-9A-Za-z-_]/g;
  function Rt2(t3) {
    if (t3 = t3.split("=")[0], t3 = t3.trim().replace(Ut, ""), t3.length < 2)
      return "";
    for (; t3.length % 4 !== 0; )
      t3 = t3 + "=";
    return t3;
  }
  function $5(t3, e3) {
    e3 = e3 || 1 / 0;
    let n4, r3 = t3.length, o3 = null, f8 = [];
    for (let u7 = 0; u7 < r3; ++u7) {
      if (n4 = t3.charCodeAt(u7), n4 > 55295 && n4 < 57344) {
        if (!o3) {
          if (n4 > 56319) {
            (e3 -= 3) > -1 && f8.push(239, 191, 189);
            continue;
          } else if (u7 + 1 === r3) {
            (e3 -= 3) > -1 && f8.push(239, 191, 189);
            continue;
          }
          o3 = n4;
          continue;
        }
        if (n4 < 56320) {
          (e3 -= 3) > -1 && f8.push(239, 191, 189), o3 = n4;
          continue;
        }
        n4 = (o3 - 55296 << 10 | n4 - 56320) + 65536;
      } else
        o3 && (e3 -= 3) > -1 && f8.push(239, 191, 189);
      if (o3 = null, n4 < 128) {
        if ((e3 -= 1) < 0)
          break;
        f8.push(n4);
      } else if (n4 < 2048) {
        if ((e3 -= 2) < 0)
          break;
        f8.push(n4 >> 6 | 192, n4 & 63 | 128);
      } else if (n4 < 65536) {
        if ((e3 -= 3) < 0)
          break;
        f8.push(n4 >> 12 | 224, n4 >> 6 & 63 | 128, n4 & 63 | 128);
      } else if (n4 < 1114112) {
        if ((e3 -= 4) < 0)
          break;
        f8.push(n4 >> 18 | 240, n4 >> 12 & 63 | 128, n4 >> 6 & 63 | 128, n4 & 63 | 128);
      } else
        throw new Error("Invalid code point");
    }
    return f8;
  }
  function Ot2(t3) {
    let e3 = [];
    for (let n4 = 0; n4 < t3.length; ++n4)
      e3.push(t3.charCodeAt(n4) & 255);
    return e3;
  }
  function Tt3(t3, e3) {
    let n4, r3, o3, f8 = [];
    for (let u7 = 0; u7 < t3.length && !((e3 -= 2) < 0); ++u7)
      n4 = t3.charCodeAt(u7), r3 = n4 >> 8, o3 = n4 % 256, f8.push(o3), f8.push(r3);
    return f8;
  }
  function tt8(t3) {
    return h4.toByteArray(Rt2(t3));
  }
  function T7(t3, e3, n4, r3) {
    let o3;
    for (o3 = 0; o3 < r3 && !(o3 + n4 >= e3.length || o3 >= t3.length); ++o3)
      e3[o3 + n4] = t3[o3];
    return o3;
  }
  function B9(t3, e3) {
    return t3 instanceof e3 || t3 != null && t3.constructor != null && t3.constructor.name != null && t3.constructor.name === e3.name;
  }
  function P8(t3) {
    return t3 !== t3;
  }
  var _t4 = function() {
    let t3 = "0123456789abcdef", e3 = new Array(256);
    for (let n4 = 0; n4 < 16; ++n4) {
      let r3 = n4 * 16;
      for (let o3 = 0; o3 < 16; ++o3)
        e3[r3 + o3] = t3[n4] + t3[o3];
    }
    return e3;
  }();
  function E8(t3) {
    return typeof BigInt > "u" ? St3 : t3;
  }
  function St3() {
    throw new Error("BigInt not supported");
  }
});
var ot2 = {};
jt(ot2, { Buffer: () => Dt, INSPECT_MAX_BYTES: () => qt, SlowBuffer: () => Yt, default: () => Xt, kMaxLength: () => zt });
var it2 = nt2(rt2());
Ft(ot2, nt2(rt2()));
var { Buffer: Dt, SlowBuffer: Yt, INSPECT_MAX_BYTES: qt, kMaxLength: zt } = it2;
var { default: et2, ...Gt } = it2;
var Xt = et2 !== void 0 ? et2 : Gt;

// https://esm.sh/v124/cbor-x@1.5.3/es2022/cbor-x.mjs
var Ue2;
try {
  Ue2 = new TextDecoder();
} catch {
}
var y3;
var v4;
var a = 0;
var ve2 = [];
var ut2 = 105;
var dt2 = 57342;
var xt3 = 57343;
var qe2 = 57337;
var Ze2 = 6;
var re2 = {};
var Me2 = ve2;
var _e2 = 0;
var E2 = {};
var _3;
var ye2;
var pe = 0;
var ae2 = 0;
var T2;
var W2;
var R2 = [];
var Re2 = [];
var P2;
var C5;
var oe2;
var Xe2 = { useRecords: false, mapsAsObjects: true };
var ce2 = false;
var et3 = 2;
try {
  new Function("");
} catch {
  et3 = 1 / 0;
}
var Y2 = class {
  constructor(t3) {
    if (t3 && ((t3.keyMap || t3._keyMap) && !t3.useRecords && (t3.useRecords = false, t3.mapsAsObjects = true), t3.useRecords === false && t3.mapsAsObjects === void 0 && (t3.mapsAsObjects = true), t3.getStructures && (t3.getShared = t3.getStructures), t3.getShared && !t3.structures && ((t3.structures = []).uninitialized = true), t3.keyMap)) {
      this.mapKey = /* @__PURE__ */ new Map();
      for (let [l5, n4] of Object.entries(t3.keyMap))
        this.mapKey.set(n4, l5);
    }
    Object.assign(this, t3);
  }
  decodeKey(t3) {
    return this.keyMap && this.mapKey.get(t3) || t3;
  }
  encodeKey(t3) {
    return this.keyMap && this.keyMap.hasOwnProperty(t3) ? this.keyMap[t3] : t3;
  }
  encodeKeys(t3) {
    if (!this._keyMap)
      return t3;
    let l5 = /* @__PURE__ */ new Map();
    for (let [n4, f8] of Object.entries(t3))
      l5.set(this._keyMap.hasOwnProperty(n4) ? this._keyMap[n4] : n4, f8);
    return l5;
  }
  decodeKeys(t3) {
    if (!this._keyMap || t3.constructor.name != "Map")
      return t3;
    if (!this._mapKey) {
      this._mapKey = /* @__PURE__ */ new Map();
      for (let [n4, f8] of Object.entries(this._keyMap))
        this._mapKey.set(f8, n4);
    }
    let l5 = {};
    return t3.forEach((n4, f8) => l5[j4(this._mapKey.has(f8) ? this._mapKey.get(f8) : f8)] = n4), l5;
  }
  mapDecode(t3, l5) {
    let n4 = this.decode(t3);
    if (this._keyMap)
      switch (n4.constructor.name) {
        case "Array":
          return n4.map((f8) => this.decodeKeys(f8));
      }
    return n4;
  }
  decode(t3, l5) {
    if (y3)
      return nt3(() => (ge2(), this ? this.decode(t3, l5) : Y2.prototype.decode.call(Xe2, t3, l5)));
    v4 = l5 > -1 ? l5 : t3.length, a = 0, _e2 = 0, ae2 = 0, ye2 = null, Me2 = ve2, T2 = null, y3 = t3;
    try {
      C5 = t3.dataView || (t3.dataView = new DataView(t3.buffer, t3.byteOffset, t3.byteLength));
    } catch (n4) {
      throw y3 = null, t3 instanceof Uint8Array ? n4 : new Error("Source must be a Uint8Array or Buffer but was a " + (t3 && typeof t3 == "object" ? t3.constructor.name : typeof t3));
    }
    if (this instanceof Y2) {
      if (E2 = this, P2 = this.sharedValues && (this.pack ? new Array(this.maxPrivatePackedValues || 16).concat(this.sharedValues) : this.sharedValues), this.structures)
        return _3 = this.structures, he2();
      (!_3 || _3.length > 0) && (_3 = []);
    } else
      E2 = Xe2, (!_3 || _3.length > 0) && (_3 = []), P2 = null;
    return he2();
  }
  decodeMultiple(t3, l5) {
    let n4, f8 = 0;
    try {
      let o3 = t3.length;
      ce2 = true;
      let d6 = this ? this.decode(t3, o3) : Ce2.decode(t3, o3);
      if (l5) {
        if (l5(d6) === false)
          return;
        for (; a < o3; )
          if (f8 = a, l5(he2()) === false)
            return;
      } else {
        for (n4 = [d6]; a < o3; )
          f8 = a, n4.push(he2());
        return n4;
      }
    } catch (o3) {
      throw o3.lastPosition = f8, o3.values = n4, o3;
    } finally {
      ce2 = false, ge2();
    }
  }
};
function he2() {
  try {
    let e3 = S2();
    if (T2) {
      if (a >= T2.postBundlePosition) {
        let t3 = new Error("Unexpected bundle position");
        throw t3.incomplete = true, t3;
      }
      a = T2.postBundlePosition, T2 = null;
    }
    if (a == v4)
      _3 = null, y3 = null, W2 && (W2 = null);
    else if (a > v4) {
      let t3 = new Error("Unexpected end of CBOR data");
      throw t3.incomplete = true, t3;
    } else if (!ce2)
      throw new Error("Data read, but end of buffer not reached");
    return e3;
  } catch (e3) {
    throw ge2(), (e3 instanceof RangeError || e3.message.startsWith("Unexpected end of buffer")) && (e3.incomplete = true), e3;
  }
}
function S2() {
  let e3 = y3[a++], t3 = e3 >> 5;
  if (e3 = e3 & 31, e3 > 23)
    switch (e3) {
      case 24:
        e3 = y3[a++];
        break;
      case 25:
        if (t3 == 7)
          return wt2();
        e3 = C5.getUint16(a), a += 2;
        break;
      case 26:
        if (t3 == 7) {
          let l5 = C5.getFloat32(a);
          if (E2.useFloat32 > 2) {
            let n4 = ue2[(y3[a] & 127) << 1 | y3[a + 1] >> 7];
            return a += 4, (n4 * l5 + (l5 > 0 ? 0.5 : -0.5) >> 0) / n4;
          }
          return a += 4, l5;
        }
        e3 = C5.getUint32(a), a += 4;
        break;
      case 27:
        if (t3 == 7) {
          let l5 = C5.getFloat64(a);
          return a += 8, l5;
        }
        if (t3 > 1) {
          if (C5.getUint32(a) > 0)
            throw new Error("JavaScript does not support arrays, maps, or strings with length over 4294967295");
          e3 = C5.getUint32(a + 4);
        } else
          E2.int64AsNumber ? (e3 = C5.getUint32(a) * 4294967296, e3 += C5.getUint32(a + 4)) : e3 = C5.getBigUint64(a);
        a += 8;
        break;
      case 31:
        switch (t3) {
          case 2:
          case 3:
            throw new Error("Indefinite length not supported for byte or text strings");
          case 4:
            let l5 = [], n4, f8 = 0;
            for (; (n4 = S2()) != re2; )
              l5[f8++] = n4;
            return t3 == 4 ? l5 : t3 == 3 ? l5.join("") : Dt.concat(l5);
          case 5:
            let o3;
            if (E2.mapsAsObjects) {
              let d6 = {};
              if (E2.keyMap)
                for (; (o3 = S2()) != re2; )
                  d6[j4(E2.decodeKey(o3))] = S2();
              else
                for (; (o3 = S2()) != re2; )
                  d6[j4(o3)] = S2();
              return d6;
            } else {
              oe2 && (E2.mapsAsObjects = true, oe2 = false);
              let d6 = /* @__PURE__ */ new Map();
              if (E2.keyMap)
                for (; (o3 = S2()) != re2; )
                  d6.set(E2.decodeKey(o3), S2());
              else
                for (; (o3 = S2()) != re2; )
                  d6.set(o3, S2());
              return d6;
            }
          case 7:
            return re2;
          default:
            throw new Error("Invalid major type for indefinite length " + t3);
        }
      default:
        throw new Error("Unknown token " + e3);
    }
  switch (t3) {
    case 0:
      return e3;
    case 1:
      return ~e3;
    case 2:
      return gt2(e3);
    case 3:
      if (ae2 >= a)
        return ye2.slice(a - pe, (a += e3) - pe);
      if (ae2 == 0 && v4 < 140 && e3 < 32) {
        let f8 = e3 < 16 ? tt2(e3) : pt2(e3);
        if (f8 != null)
          return f8;
      }
      return ht2(e3);
    case 4:
      let l5 = new Array(e3);
      for (let f8 = 0; f8 < e3; f8++)
        l5[f8] = S2();
      return l5;
    case 5:
      if (E2.mapsAsObjects) {
        let f8 = {};
        if (E2.keyMap)
          for (let o3 = 0; o3 < e3; o3++)
            f8[j4(E2.decodeKey(S2()))] = S2();
        else
          for (let o3 = 0; o3 < e3; o3++)
            f8[j4(S2())] = S2();
        return f8;
      } else {
        oe2 && (E2.mapsAsObjects = true, oe2 = false);
        let f8 = /* @__PURE__ */ new Map();
        if (E2.keyMap)
          for (let o3 = 0; o3 < e3; o3++)
            f8.set(E2.decodeKey(S2()), S2());
        else
          for (let o3 = 0; o3 < e3; o3++)
            f8.set(S2(), S2());
        return f8;
      }
    case 6:
      if (e3 >= qe2) {
        let f8 = _3[e3 & 8191];
        if (f8)
          return f8.read || (f8.read = Be2(f8)), f8.read();
        if (e3 < 65536) {
          if (e3 == xt3) {
            let o3 = se2(), d6 = S2(), w9 = S2();
            De2(d6, w9);
            let U9 = {};
            if (E2.keyMap)
              for (let p10 = 2; p10 < o3; p10++) {
                let B9 = E2.decodeKey(w9[p10 - 2]);
                U9[j4(B9)] = S2();
              }
            else
              for (let p10 = 2; p10 < o3; p10++) {
                let B9 = w9[p10 - 2];
                U9[j4(B9)] = S2();
              }
            return U9;
          } else if (e3 == dt2) {
            let o3 = se2(), d6 = S2();
            for (let w9 = 2; w9 < o3; w9++)
              De2(d6++, S2());
            return S2();
          } else if (e3 == qe2)
            return It();
          if (E2.getShared && (Ve2(), f8 = _3[e3 & 8191], f8))
            return f8.read || (f8.read = Be2(f8)), f8.read();
        }
      }
      let n4 = R2[e3];
      if (n4)
        return n4.handlesRead ? n4(S2) : n4(S2());
      {
        let f8 = S2();
        for (let o3 = 0; o3 < Re2.length; o3++) {
          let d6 = Re2[o3](e3, f8);
          if (d6 !== void 0)
            return d6;
        }
        return new H4(f8, e3);
      }
    case 7:
      switch (e3) {
        case 20:
          return false;
        case 21:
          return true;
        case 22:
          return null;
        case 23:
          return;
        case 31:
        default:
          let f8 = (P2 || Q2())[e3];
          if (f8 !== void 0)
            return f8;
          throw new Error("Unknown token " + e3);
      }
    default:
      if (isNaN(e3)) {
        let f8 = new Error("Unexpected end of CBOR data");
        throw f8.incomplete = true, f8;
      }
      throw new Error("Unknown CBOR token " + e3);
  }
}
var $e2 = /^[a-zA-Z_$][a-zA-Z\d_$]*$/;
function Be2(e3) {
  function t3() {
    let l5 = y3[a++];
    if (l5 = l5 & 31, l5 > 23)
      switch (l5) {
        case 24:
          l5 = y3[a++];
          break;
        case 25:
          l5 = C5.getUint16(a), a += 2;
          break;
        case 26:
          l5 = C5.getUint32(a), a += 4;
          break;
        default:
          throw new Error("Expected array header, but got " + y3[a - 1]);
      }
    let n4 = this.compiledReader;
    for (; n4; ) {
      if (n4.propertyCount === l5)
        return n4(S2);
      n4 = n4.next;
    }
    if (this.slowReads++ >= et3) {
      let o3 = this.length == l5 ? this : this.slice(0, l5);
      return n4 = E2.keyMap ? new Function("r", "return {" + o3.map((d6) => E2.decodeKey(d6)).map((d6) => $e2.test(d6) ? j4(d6) + ":r()" : "[" + JSON.stringify(d6) + "]:r()").join(",") + "}") : new Function("r", "return {" + o3.map((d6) => $e2.test(d6) ? j4(d6) + ":r()" : "[" + JSON.stringify(d6) + "]:r()").join(",") + "}"), this.compiledReader && (n4.next = this.compiledReader), n4.propertyCount = l5, this.compiledReader = n4, n4(S2);
    }
    let f8 = {};
    if (E2.keyMap)
      for (let o3 = 0; o3 < l5; o3++)
        f8[j4(E2.decodeKey(this[o3]))] = S2();
    else
      for (let o3 = 0; o3 < l5; o3++)
        f8[j4(this[o3])] = S2();
    return f8;
  }
  return e3.slowReads = 0, t3;
}
function j4(e3) {
  return e3 === "__proto__" ? "__proto_" : e3;
}
var ht2 = Te2;
function Te2(e3) {
  let t3;
  if (e3 < 16 && (t3 = tt2(e3)))
    return t3;
  if (e3 > 64 && Ue2)
    return Ue2.decode(y3.subarray(a, a += e3));
  let l5 = a + e3, n4 = [];
  for (t3 = ""; a < l5; ) {
    let f8 = y3[a++];
    if (!(f8 & 128))
      n4.push(f8);
    else if ((f8 & 224) === 192) {
      let o3 = y3[a++] & 63;
      n4.push((f8 & 31) << 6 | o3);
    } else if ((f8 & 240) === 224) {
      let o3 = y3[a++] & 63, d6 = y3[a++] & 63;
      n4.push((f8 & 31) << 12 | o3 << 6 | d6);
    } else if ((f8 & 248) === 240) {
      let o3 = y3[a++] & 63, d6 = y3[a++] & 63, w9 = y3[a++] & 63, U9 = (f8 & 7) << 18 | o3 << 12 | d6 << 6 | w9;
      U9 > 65535 && (U9 -= 65536, n4.push(U9 >>> 10 & 1023 | 55296), U9 = 56320 | U9 & 1023), n4.push(U9);
    } else
      n4.push(f8);
    n4.length >= 4096 && (t3 += F2.apply(String, n4), n4.length = 0);
  }
  return n4.length > 0 && (t3 += F2.apply(String, n4)), t3;
}
var F2 = String.fromCharCode;
function pt2(e3) {
  let t3 = a, l5 = new Array(e3);
  for (let n4 = 0; n4 < e3; n4++) {
    let f8 = y3[a++];
    if ((f8 & 128) > 0) {
      a = t3;
      return;
    }
    l5[n4] = f8;
  }
  return F2.apply(String, l5);
}
function tt2(e3) {
  if (e3 < 4)
    if (e3 < 2) {
      if (e3 === 0)
        return "";
      {
        let t3 = y3[a++];
        if ((t3 & 128) > 1) {
          a -= 1;
          return;
        }
        return F2(t3);
      }
    } else {
      let t3 = y3[a++], l5 = y3[a++];
      if ((t3 & 128) > 0 || (l5 & 128) > 0) {
        a -= 2;
        return;
      }
      if (e3 < 3)
        return F2(t3, l5);
      let n4 = y3[a++];
      if ((n4 & 128) > 0) {
        a -= 3;
        return;
      }
      return F2(t3, l5, n4);
    }
  else {
    let t3 = y3[a++], l5 = y3[a++], n4 = y3[a++], f8 = y3[a++];
    if ((t3 & 128) > 0 || (l5 & 128) > 0 || (n4 & 128) > 0 || (f8 & 128) > 0) {
      a -= 4;
      return;
    }
    if (e3 < 6) {
      if (e3 === 4)
        return F2(t3, l5, n4, f8);
      {
        let o3 = y3[a++];
        if ((o3 & 128) > 0) {
          a -= 5;
          return;
        }
        return F2(t3, l5, n4, f8, o3);
      }
    } else if (e3 < 8) {
      let o3 = y3[a++], d6 = y3[a++];
      if ((o3 & 128) > 0 || (d6 & 128) > 0) {
        a -= 6;
        return;
      }
      if (e3 < 7)
        return F2(t3, l5, n4, f8, o3, d6);
      let w9 = y3[a++];
      if ((w9 & 128) > 0) {
        a -= 7;
        return;
      }
      return F2(t3, l5, n4, f8, o3, d6, w9);
    } else {
      let o3 = y3[a++], d6 = y3[a++], w9 = y3[a++], U9 = y3[a++];
      if ((o3 & 128) > 0 || (d6 & 128) > 0 || (w9 & 128) > 0 || (U9 & 128) > 0) {
        a -= 8;
        return;
      }
      if (e3 < 10) {
        if (e3 === 8)
          return F2(t3, l5, n4, f8, o3, d6, w9, U9);
        {
          let p10 = y3[a++];
          if ((p10 & 128) > 0) {
            a -= 9;
            return;
          }
          return F2(t3, l5, n4, f8, o3, d6, w9, U9, p10);
        }
      } else if (e3 < 12) {
        let p10 = y3[a++], B9 = y3[a++];
        if ((p10 & 128) > 0 || (B9 & 128) > 0) {
          a -= 10;
          return;
        }
        if (e3 < 11)
          return F2(t3, l5, n4, f8, o3, d6, w9, U9, p10, B9);
        let O7 = y3[a++];
        if ((O7 & 128) > 0) {
          a -= 11;
          return;
        }
        return F2(t3, l5, n4, f8, o3, d6, w9, U9, p10, B9, O7);
      } else {
        let p10 = y3[a++], B9 = y3[a++], O7 = y3[a++], N8 = y3[a++];
        if ((p10 & 128) > 0 || (B9 & 128) > 0 || (O7 & 128) > 0 || (N8 & 128) > 0) {
          a -= 12;
          return;
        }
        if (e3 < 14) {
          if (e3 === 12)
            return F2(t3, l5, n4, f8, o3, d6, w9, U9, p10, B9, O7, N8);
          {
            let V7 = y3[a++];
            if ((V7 & 128) > 0) {
              a -= 13;
              return;
            }
            return F2(t3, l5, n4, f8, o3, d6, w9, U9, p10, B9, O7, N8, V7);
          }
        } else {
          let V7 = y3[a++], K9 = y3[a++];
          if ((V7 & 128) > 0 || (K9 & 128) > 0) {
            a -= 14;
            return;
          }
          if (e3 < 15)
            return F2(t3, l5, n4, f8, o3, d6, w9, U9, p10, B9, O7, N8, V7, K9);
          let q7 = y3[a++];
          if ((q7 & 128) > 0) {
            a -= 15;
            return;
          }
          return F2(t3, l5, n4, f8, o3, d6, w9, U9, p10, B9, O7, N8, V7, K9, q7);
        }
      }
    }
  }
}
function gt2(e3) {
  return E2.copyBuffers ? Uint8Array.prototype.slice.call(y3, a, a += e3) : y3.subarray(a, a += e3);
}
var Fe2 = new Float32Array(1);
var ie2 = new Uint8Array(Fe2.buffer, 0, 4);
function wt2() {
  let e3 = y3[a++], t3 = y3[a++], l5 = (e3 & 127) >> 2;
  if (l5 === 31)
    return t3 || e3 & 3 ? NaN : e3 & 128 ? -1 / 0 : 1 / 0;
  if (l5 === 0) {
    let n4 = ((e3 & 3) << 8 | t3) / 16777216;
    return e3 & 128 ? -n4 : n4;
  }
  return ie2[3] = e3 & 128 | (l5 >> 1) + 56, ie2[2] = (e3 & 7) << 5 | t3 >> 3, ie2[1] = t3 << 5, ie2[0] = 0, Fe2[0];
}
var Jt = new Array(4096);
var H4 = class {
  constructor(t3, l5) {
    this.value = t3, this.tag = l5;
  }
};
R2[0] = (e3) => new Date(e3);
R2[1] = (e3) => new Date(Math.round(e3 * 1e3));
R2[2] = (e3) => {
  let t3 = BigInt(0);
  for (let l5 = 0, n4 = e3.byteLength; l5 < n4; l5++)
    t3 = BigInt(e3[l5]) + t3 << BigInt(8);
  return t3;
};
R2[3] = (e3) => BigInt(-1) - R2[2](e3);
R2[4] = (e3) => +(e3[1] + "e" + e3[0]);
R2[5] = (e3) => e3[1] * Math.exp(e3[0] * Math.log(2));
var De2 = (e3, t3) => {
  e3 = e3 - 57344;
  let l5 = _3[e3];
  l5 && l5.isShared && ((_3.restoreStructures || (_3.restoreStructures = []))[e3] = l5), _3[e3] = t3, t3.read = Be2(t3);
};
R2[ut2] = (e3) => {
  let t3 = e3.length, l5 = e3[1];
  De2(e3[0], l5);
  let n4 = {};
  for (let f8 = 2; f8 < t3; f8++) {
    let o3 = l5[f8 - 2];
    n4[j4(o3)] = e3[f8];
  }
  return n4;
};
R2[14] = (e3) => T2 ? T2[0].slice(T2.position0, T2.position0 += e3) : new H4(e3, 14);
R2[15] = (e3) => T2 ? T2[1].slice(T2.position1, T2.position1 += e3) : new H4(e3, 15);
var bt2 = { Error, RegExp };
R2[27] = (e3) => (bt2[e3[0]] || Error)(e3[1], e3[2]);
var rt3 = (e3) => {
  if (y3[a++] != 132)
    throw new Error("Packed values structure must be followed by a 4 element array");
  let t3 = e3();
  return P2 = P2 ? t3.concat(P2.slice(t3.length)) : t3, P2.prefixes = e3(), P2.suffixes = e3(), e3();
};
rt3.handlesRead = true;
R2[51] = rt3;
R2[Ze2] = (e3) => {
  if (!P2)
    if (E2.getShared)
      Ve2();
    else
      return new H4(e3, Ze2);
  if (typeof e3 == "number")
    return P2[16 + (e3 >= 0 ? 2 * e3 : -2 * e3 - 1)];
  throw new Error("No support for non-integer packed references yet");
};
R2[28] = (e3) => {
  W2 || (W2 = /* @__PURE__ */ new Map(), W2.id = 0);
  let t3 = W2.id++, l5 = y3[a], n4;
  l5 >> 5 == 4 ? n4 = [] : n4 = {};
  let f8 = { target: n4 };
  W2.set(t3, f8);
  let o3 = e3();
  return f8.used ? Object.assign(n4, o3) : (f8.target = o3, o3);
};
R2[28].handlesRead = true;
R2[29] = (e3) => {
  let t3 = W2.get(e3);
  return t3.used = true, t3.target;
};
R2[258] = (e3) => new Set(e3);
(R2[259] = (e3) => (E2.mapsAsObjects && (E2.mapsAsObjects = false, oe2 = true), e3())).handlesRead = true;
function ne2(e3, t3) {
  return typeof e3 == "string" ? e3 + t3 : e3 instanceof Array ? e3.concat(t3) : Object.assign({}, e3, t3);
}
function Q2() {
  if (!P2)
    if (E2.getShared)
      Ve2();
    else
      throw new Error("No packed values available");
  return P2;
}
var mt2 = 1399353956;
Re2.push((e3, t3) => {
  if (e3 >= 225 && e3 <= 255)
    return ne2(Q2().prefixes[e3 - 224], t3);
  if (e3 >= 28704 && e3 <= 32767)
    return ne2(Q2().prefixes[e3 - 28672], t3);
  if (e3 >= 1879052288 && e3 <= 2147483647)
    return ne2(Q2().prefixes[e3 - 1879048192], t3);
  if (e3 >= 216 && e3 <= 223)
    return ne2(t3, Q2().suffixes[e3 - 216]);
  if (e3 >= 27647 && e3 <= 28671)
    return ne2(t3, Q2().suffixes[e3 - 27639]);
  if (e3 >= 1811940352 && e3 <= 1879048191)
    return ne2(t3, Q2().suffixes[e3 - 1811939328]);
  if (e3 == mt2)
    return { packedValues: P2, structures: _3.slice(0), version: t3 };
  if (e3 == 55799)
    return t3;
});
var At = new Uint8Array(new Uint16Array([1]).buffer)[0] == 1;
var Qe2 = [Uint8Array, Uint8ClampedArray, Uint16Array, Uint32Array, typeof BigUint64Array > "u" ? { name: "BigUint64Array" } : BigUint64Array, Int8Array, Int16Array, Int32Array, typeof BigInt64Array > "u" ? { name: "BigInt64Array" } : BigInt64Array, Float32Array, Float64Array];
var St2 = [64, 68, 69, 70, 71, 72, 77, 78, 79, 85, 86];
for (let e3 = 0; e3 < Qe2.length; e3++)
  Et(Qe2[e3], St2[e3]);
function Et(e3, t3) {
  let l5 = "get" + e3.name.slice(0, -5), n4;
  typeof e3 == "function" ? n4 = e3.BYTES_PER_ELEMENT : e3 = null;
  for (let f8 = 0; f8 < 2; f8++) {
    if (!f8 && n4 == 1)
      continue;
    let o3 = n4 == 2 ? 1 : n4 == 4 ? 2 : 3;
    R2[f8 ? t3 : t3 - 4] = n4 == 1 || f8 == At ? (d6) => {
      if (!e3)
        throw new Error("Could not find typed array for code " + t3);
      return new e3(Uint8Array.prototype.slice.call(d6, 0).buffer);
    } : (d6) => {
      if (!e3)
        throw new Error("Could not find typed array for code " + t3);
      let w9 = new DataView(d6.buffer, d6.byteOffset, d6.byteLength), U9 = d6.length >> o3, p10 = new e3(U9), B9 = w9[l5];
      for (let O7 = 0; O7 < U9; O7++)
        p10[O7] = B9.call(w9, O7 << o3, f8);
      return p10;
    };
  }
}
function It() {
  let e3 = se2(), t3 = a + S2();
  for (let n4 = 2; n4 < e3; n4++) {
    let f8 = se2();
    a += f8;
  }
  let l5 = a;
  return a = t3, T2 = [Te2(se2()), Te2(se2())], T2.position0 = 0, T2.position1 = 0, T2.postBundlePosition = a, a = l5, S2();
}
function se2() {
  let e3 = y3[a++] & 31;
  if (e3 > 23)
    switch (e3) {
      case 24:
        e3 = y3[a++];
        break;
      case 25:
        e3 = C5.getUint16(a), a += 2;
        break;
      case 26:
        e3 = C5.getUint32(a), a += 4;
        break;
    }
  return e3;
}
function Ve2() {
  if (E2.getShared) {
    let e3 = nt3(() => (y3 = null, E2.getShared())) || {}, t3 = e3.structures || [];
    E2.sharedVersion = e3.version, P2 = E2.sharedValues = e3.packedValues, _3 === true ? E2.structures = _3 = t3 : _3.splice.apply(_3, [0, t3.length].concat(t3));
  }
}
function nt3(e3) {
  let t3 = v4, l5 = a, n4 = _e2, f8 = pe, o3 = ae2, d6 = ye2, w9 = Me2, U9 = W2, p10 = T2, B9 = new Uint8Array(y3.slice(0, v4)), O7 = _3, N8 = E2, V7 = ce2, K9 = e3();
  return v4 = t3, a = l5, _e2 = n4, pe = f8, ae2 = o3, ye2 = d6, Me2 = w9, W2 = U9, T2 = p10, y3 = B9, ce2 = V7, _3 = O7, E2 = N8, C5 = new DataView(y3.buffer, y3.byteOffset, y3.byteLength), K9;
}
function ge2() {
  y3 = null, W2 = null, _3 = null;
}
var ue2 = new Array(147);
for (let e3 = 0; e3 < 256; e3++)
  ue2[e3] = +("1e" + Math.floor(45.15 - e3 * 0.30103));
var Ce2 = new Y2({ useRecords: false });
var kt3 = Ce2.decode;
var Ot = Ce2.decodeMultiple;
var we2 = { NEVER: 0, ALWAYS: 1, DECIMAL_ROUND: 3, DECIMAL_FIT: 4 };
var be2;
try {
  be2 = new TextEncoder();
} catch {
}
var Ae2;
var ze2;
var Ee2 = typeof globalThis == "object" && globalThis.Buffer;
var de2 = typeof Ee2 < "u";
var Pe2 = de2 ? Ee2.allocUnsafeSlow : Uint8Array;
var st2 = de2 ? Ee2 : Uint8Array;
var lt2 = 256;
var ft2 = de2 ? 4294967296 : 2144337920;
var Le2;
var i2;
var M3;
var r = 0;
var X2;
var D3 = null;
var Mt2 = 61440;
var _t2 = /[\u0080-\uFFFF]/;
var L2 = Symbol("record-id");
var ee2 = class extends Y2 {
  constructor(t3) {
    super(t3), this.offset = 0;
    let l5, n4, f8, o3, d6, w9;
    t3 = t3 || {};
    let U9 = st2.prototype.utf8Write ? function(s2, h4, c8) {
      return i2.utf8Write(s2, h4, c8);
    } : be2 && be2.encodeInto ? function(s2, h4) {
      return be2.encodeInto(s2, i2.subarray(h4)).written;
    } : false, p10 = this, B9 = t3.structures || t3.saveStructures, O7 = t3.maxSharedStructures;
    if (O7 == null && (O7 = B9 ? 128 : 0), O7 > 8190)
      throw new Error("Maximum maxSharedStructure is 8190");
    let N8 = t3.sequential;
    N8 && (O7 = 0), this.structures || (this.structures = []), this.saveStructures && (this.saveShared = this.saveStructures);
    let V7, K9, q7 = t3.sharedValues, z8;
    if (q7) {
      z8 = /* @__PURE__ */ Object.create(null);
      for (let s2 = 0, h4 = q7.length; s2 < h4; s2++)
        z8[q7[s2]] = s2;
    }
    let Z7 = [], Ie2 = 0, xe2 = 0;
    this.mapEncode = function(s2, h4) {
      if (this._keyMap && !this._mapped)
        switch (s2.constructor.name) {
          case "Array":
            s2 = s2.map((c8) => this.encodeKeys(c8));
            break;
        }
      return this.encode(s2, h4);
    }, this.encode = function(s2, h4) {
      if (i2 || (i2 = new Pe2(8192), M3 = new DataView(i2.buffer, 0, 8192), r = 0), X2 = i2.length - 10, X2 - r < 2048 ? (i2 = new Pe2(i2.length), M3 = new DataView(i2.buffer, 0, i2.length), X2 = i2.length - 10, r = 0) : h4 === Ke2 && (r = r + 7 & 2147483640), n4 = r, p10.useSelfDescribedHeader && (M3.setUint32(r, 3654940416), r += 3), w9 = p10.structuredClone ? /* @__PURE__ */ new Map() : null, p10.bundleStrings && typeof s2 != "string" ? (D3 = [], D3.size = 1 / 0) : D3 = null, f8 = p10.structures, f8) {
        if (f8.uninitialized) {
          let x8 = p10.getShared() || {};
          p10.structures = f8 = x8.structures || [], p10.sharedVersion = x8.version;
          let u7 = p10.sharedValues = x8.packedValues;
          if (u7) {
            z8 = {};
            for (let g9 = 0, b7 = u7.length; g9 < b7; g9++)
              z8[u7[g9]] = g9;
          }
        }
        let c8 = f8.length;
        if (c8 > O7 && !N8 && (c8 = O7), !f8.transitions) {
          f8.transitions = /* @__PURE__ */ Object.create(null);
          for (let x8 = 0; x8 < c8; x8++) {
            let u7 = f8[x8];
            if (!u7)
              continue;
            let g9, b7 = f8.transitions;
            for (let m10 = 0, A5 = u7.length; m10 < A5; m10++) {
              b7[L2] === void 0 && (b7[L2] = x8);
              let I7 = u7[m10];
              g9 = b7[I7], g9 || (g9 = b7[I7] = /* @__PURE__ */ Object.create(null)), b7 = g9;
            }
            b7[L2] = x8 | 1048576;
          }
        }
        N8 || (f8.nextId = c8);
      }
      if (o3 && (o3 = false), d6 = f8 || [], K9 = z8, t3.pack) {
        let c8 = /* @__PURE__ */ new Map();
        if (c8.values = [], c8.encoder = p10, c8.maxValues = t3.maxPrivatePackedValues || (z8 ? 16 : 1 / 0), c8.objectMap = z8 || false, c8.samplingPackedValues = V7, me2(s2, c8), c8.values.length > 0) {
          i2[r++] = 216, i2[r++] = 51, G4(4);
          let x8 = c8.values;
          k10(x8), G4(0), G4(0), K9 = Object.create(z8 || null);
          for (let u7 = 0, g9 = x8.length; u7 < g9; u7++)
            K9[x8[u7]] = u7;
        }
      }
      Le2 = h4 & je2;
      try {
        if (Le2)
          return;
        if (k10(s2), D3 && ct2(n4, k10), p10.offset = r, w9 && w9.idsToInsert) {
          r += w9.idsToInsert.length * 2, r > X2 && le2(r), p10.offset = r;
          let c8 = Tt2(i2.subarray(n4, r), w9.idsToInsert);
          return w9 = null, c8;
        }
        return h4 & Ke2 ? (i2.start = n4, i2.end = r, i2) : i2.subarray(n4, r);
      } finally {
        if (f8) {
          if (xe2 < 10 && xe2++, f8.length > O7 && (f8.length = O7), Ie2 > 1e4)
            f8.transitions = null, xe2 = 0, Ie2 = 0, Z7.length > 0 && (Z7 = []);
          else if (Z7.length > 0 && !N8) {
            for (let c8 = 0, x8 = Z7.length; c8 < x8; c8++)
              Z7[c8][L2] = void 0;
            Z7 = [];
          }
        }
        if (o3 && p10.saveShared) {
          p10.structures.length > O7 && (p10.structures = p10.structures.slice(0, O7));
          let c8 = i2.subarray(n4, r);
          return p10.updateSharedData() === false ? p10.encode(s2) : c8;
        }
        h4 & Kt && (r = n4);
      }
    }, this.findCommonStringsToPack = () => (V7 = /* @__PURE__ */ new Map(), z8 || (z8 = /* @__PURE__ */ Object.create(null)), (s2) => {
      let h4 = s2 && s2.threshold || 4, c8 = this.pack ? s2.maxPrivatePackedValues || 16 : 0;
      q7 || (q7 = this.sharedValues = []);
      for (let [x8, u7] of V7)
        u7.count > h4 && (z8[x8] = c8++, q7.push(x8), o3 = true);
      for (; this.saveShared && this.updateSharedData() === false; )
        ;
      V7 = null;
    });
    let k10 = (s2) => {
      r > X2 && (i2 = le2(r));
      var h4 = typeof s2, c8;
      if (h4 === "string") {
        if (K9) {
          let b7 = K9[s2];
          if (b7 >= 0) {
            b7 < 16 ? i2[r++] = b7 + 224 : (i2[r++] = 198, b7 & 1 ? k10(15 - b7 >> 1) : k10(b7 - 16 >> 1));
            return;
          } else if (V7 && !t3.pack) {
            let m10 = V7.get(s2);
            m10 ? m10.count++ : V7.set(s2, { count: 1 });
          }
        }
        let x8 = s2.length;
        if (D3 && x8 >= 4 && x8 < 1024) {
          if ((D3.size += x8) > Mt2) {
            let m10, A5 = (D3[0] ? D3[0].length * 3 + D3[1].length : 0) + 10;
            r + A5 > X2 && (i2 = le2(r + A5)), i2[r++] = 217, i2[r++] = 223, i2[r++] = 249, i2[r++] = D3.position ? 132 : 130, i2[r++] = 26, m10 = r - n4, r += 4, D3.position && ct2(n4, k10), D3 = ["", ""], D3.size = 0, D3.position = m10;
          }
          let b7 = _t2.test(s2);
          D3[b7 ? 0 : 1] += s2, i2[r++] = b7 ? 206 : 207, k10(x8);
          return;
        }
        let u7;
        x8 < 32 ? u7 = 1 : x8 < 256 ? u7 = 2 : x8 < 65536 ? u7 = 3 : u7 = 5;
        let g9 = x8 * 3;
        if (r + g9 > X2 && (i2 = le2(r + g9)), x8 < 64 || !U9) {
          let b7, m10, A5, I7 = r + u7;
          for (b7 = 0; b7 < x8; b7++)
            m10 = s2.charCodeAt(b7), m10 < 128 ? i2[I7++] = m10 : m10 < 2048 ? (i2[I7++] = m10 >> 6 | 192, i2[I7++] = m10 & 63 | 128) : (m10 & 64512) === 55296 && ((A5 = s2.charCodeAt(b7 + 1)) & 64512) === 56320 ? (m10 = 65536 + ((m10 & 1023) << 10) + (A5 & 1023), b7++, i2[I7++] = m10 >> 18 | 240, i2[I7++] = m10 >> 12 & 63 | 128, i2[I7++] = m10 >> 6 & 63 | 128, i2[I7++] = m10 & 63 | 128) : (i2[I7++] = m10 >> 12 | 224, i2[I7++] = m10 >> 6 & 63 | 128, i2[I7++] = m10 & 63 | 128);
          c8 = I7 - r - u7;
        } else
          c8 = U9(s2, r + u7, g9);
        c8 < 24 ? i2[r++] = 96 | c8 : c8 < 256 ? (u7 < 2 && i2.copyWithin(r + 2, r + 1, r + 1 + c8), i2[r++] = 120, i2[r++] = c8) : c8 < 65536 ? (u7 < 3 && i2.copyWithin(r + 3, r + 2, r + 2 + c8), i2[r++] = 121, i2[r++] = c8 >> 8, i2[r++] = c8 & 255) : (u7 < 5 && i2.copyWithin(r + 5, r + 3, r + 3 + c8), i2[r++] = 122, M3.setUint32(r, c8), r += 4), r += c8;
      } else if (h4 === "number")
        if (!this.alwaysUseFloat && s2 >>> 0 === s2)
          s2 < 24 ? i2[r++] = s2 : s2 < 256 ? (i2[r++] = 24, i2[r++] = s2) : s2 < 65536 ? (i2[r++] = 25, i2[r++] = s2 >> 8, i2[r++] = s2 & 255) : (i2[r++] = 26, M3.setUint32(r, s2), r += 4);
        else if (!this.alwaysUseFloat && s2 >> 0 === s2)
          s2 >= -24 ? i2[r++] = 31 - s2 : s2 >= -256 ? (i2[r++] = 56, i2[r++] = ~s2) : s2 >= -65536 ? (i2[r++] = 57, M3.setUint16(r, ~s2), r += 2) : (i2[r++] = 58, M3.setUint32(r, ~s2), r += 4);
        else {
          let x8;
          if ((x8 = this.useFloat32) > 0 && s2 < 4294967296 && s2 >= -2147483648) {
            i2[r++] = 250, M3.setFloat32(r, s2);
            let u7;
            if (x8 < 4 || (u7 = s2 * ue2[(i2[r] & 127) << 1 | i2[r + 1] >> 7]) >> 0 === u7) {
              r += 4;
              return;
            } else
              r--;
          }
          i2[r++] = 251, M3.setFloat64(r, s2), r += 8;
        }
      else if (h4 === "object")
        if (!s2)
          i2[r++] = 246;
        else {
          if (w9) {
            let u7 = w9.get(s2);
            if (u7) {
              if (i2[r++] = 216, i2[r++] = 29, i2[r++] = 25, !u7.references) {
                let g9 = w9.idsToInsert || (w9.idsToInsert = []);
                u7.references = [], g9.push(u7);
              }
              u7.references.push(r - n4), r += 2;
              return;
            } else
              w9.set(s2, { offset: r - n4 });
          }
          let x8 = s2.constructor;
          if (x8 === Object)
            ke2(s2, true);
          else if (x8 === Array) {
            c8 = s2.length, c8 < 24 ? i2[r++] = 128 | c8 : G4(c8);
            for (let u7 = 0; u7 < c8; u7++)
              k10(s2[u7]);
          } else if (x8 === Map)
            if ((this.mapsAsObjects ? this.useTag259ForMaps !== false : this.useTag259ForMaps) && (i2[r++] = 217, i2[r++] = 1, i2[r++] = 3), c8 = s2.size, c8 < 24 ? i2[r++] = 160 | c8 : c8 < 256 ? (i2[r++] = 184, i2[r++] = c8) : c8 < 65536 ? (i2[r++] = 185, i2[r++] = c8 >> 8, i2[r++] = c8 & 255) : (i2[r++] = 186, M3.setUint32(r, c8), r += 4), p10.keyMap)
              for (let [u7, g9] of s2)
                k10(p10.encodeKey(u7)), k10(g9);
            else
              for (let [u7, g9] of s2)
                k10(u7), k10(g9);
          else {
            for (let u7 = 0, g9 = Ae2.length; u7 < g9; u7++) {
              let b7 = ze2[u7];
              if (s2 instanceof b7) {
                let m10 = Ae2[u7], A5 = m10.tag;
                A5 == null && (A5 = m10.getTag && m10.getTag.call(this, s2)), A5 < 24 ? i2[r++] = 192 | A5 : A5 < 256 ? (i2[r++] = 216, i2[r++] = A5) : A5 < 65536 ? (i2[r++] = 217, i2[r++] = A5 >> 8, i2[r++] = A5 & 255) : A5 > -1 && (i2[r++] = 218, M3.setUint32(r, A5), r += 4), m10.encode.call(this, s2, k10, le2);
                return;
              }
            }
            if (s2[Symbol.iterator]) {
              if (Le2) {
                let u7 = new Error("Iterable should be serialized as iterator");
                throw u7.iteratorNotHandled = true, u7;
              }
              i2[r++] = 159;
              for (let u7 of s2)
                k10(u7);
              i2[r++] = 255;
              return;
            }
            if (s2[Symbol.asyncIterator] || Ne2(s2)) {
              let u7 = new Error("Iterable/blob should be serialized as iterator");
              throw u7.iteratorNotHandled = true, u7;
            }
            ke2(s2, !s2.hasOwnProperty);
          }
        }
      else if (h4 === "boolean")
        i2[r++] = s2 ? 245 : 244;
      else if (h4 === "bigint") {
        if (s2 < BigInt(1) << BigInt(64) && s2 >= 0)
          i2[r++] = 27, M3.setBigUint64(r, s2);
        else if (s2 > -(BigInt(1) << BigInt(64)) && s2 < 0)
          i2[r++] = 59, M3.setBigUint64(r, -s2 - BigInt(1));
        else if (this.largeBigIntToFloat)
          i2[r++] = 251, M3.setFloat64(r, Number(s2));
        else
          throw new RangeError(s2 + " was too large to fit in CBOR 64-bit integer format, set largeBigIntToFloat to convert to float-64");
        r += 8;
      } else if (h4 === "undefined")
        i2[r++] = 247;
      else
        throw new Error("Unknown type: " + h4);
    }, ke2 = this.useRecords === false ? this.variableMapSize ? (s2) => {
      let h4 = Object.keys(s2), c8 = Object.values(s2), x8 = h4.length;
      x8 < 24 ? i2[r++] = 160 | x8 : x8 < 256 ? (i2[r++] = 184, i2[r++] = x8) : x8 < 65536 ? (i2[r++] = 185, i2[r++] = x8 >> 8, i2[r++] = x8 & 255) : (i2[r++] = 186, M3.setUint32(r, x8), r += 4);
      let u7;
      if (p10.keyMap)
        for (let g9 = 0; g9 < x8; g9++)
          k10(encodeKey(h4[g9])), k10(c8[g9]);
      else
        for (let g9 = 0; g9 < x8; g9++)
          k10(h4[g9]), k10(c8[g9]);
    } : (s2, h4) => {
      i2[r++] = 185;
      let c8 = r - n4;
      r += 2;
      let x8 = 0;
      if (p10.keyMap)
        for (let u7 in s2)
          (h4 || s2.hasOwnProperty(u7)) && (k10(p10.encodeKey(u7)), k10(s2[u7]), x8++);
      else
        for (let u7 in s2)
          (h4 || s2.hasOwnProperty(u7)) && (k10(u7), k10(s2[u7]), x8++);
      i2[c8++ + n4] = x8 >> 8, i2[c8 + n4] = x8 & 255;
    } : (s2, h4) => {
      let c8, x8 = d6.transitions || (d6.transitions = /* @__PURE__ */ Object.create(null)), u7 = 0, g9 = 0, b7, m10;
      if (this.keyMap) {
        m10 = Object.keys(s2).map((I7) => this.encodeKey(I7)), g9 = m10.length;
        for (let I7 = 0; I7 < g9; I7++) {
          let Ge2 = m10[I7];
          c8 = x8[Ge2], c8 || (c8 = x8[Ge2] = /* @__PURE__ */ Object.create(null), u7++), x8 = c8;
        }
      } else
        for (let I7 in s2)
          (h4 || s2.hasOwnProperty(I7)) && (c8 = x8[I7], c8 || (x8[L2] & 1048576 && (b7 = x8[L2] & 65535), c8 = x8[I7] = /* @__PURE__ */ Object.create(null), u7++), x8 = c8, g9++);
      let A5 = x8[L2];
      if (A5 !== void 0)
        A5 &= 65535, i2[r++] = 217, i2[r++] = A5 >> 8 | 224, i2[r++] = A5 & 255;
      else if (m10 || (m10 = x8.__keys__ || (x8.__keys__ = Object.keys(s2))), b7 === void 0 ? (A5 = d6.nextId++, A5 || (A5 = 0, d6.nextId = 1), A5 >= lt2 && (d6.nextId = (A5 = O7) + 1)) : A5 = b7, d6[A5] = m10, A5 < O7) {
        i2[r++] = 217, i2[r++] = A5 >> 8 | 224, i2[r++] = A5 & 255, x8 = d6.transitions;
        for (let I7 = 0; I7 < g9; I7++)
          (x8[L2] === void 0 || x8[L2] & 1048576) && (x8[L2] = A5), x8 = x8[m10[I7]];
        x8[L2] = A5 | 1048576, o3 = true;
      } else {
        if (x8[L2] = A5, M3.setUint32(r, 3655335680), r += 3, u7 && (Ie2 += xe2 * u7), Z7.length >= lt2 - O7 && (Z7.shift()[L2] = void 0), Z7.push(x8), G4(g9 + 2), k10(57344 + A5), k10(m10), h4 === null)
          return;
        for (let I7 in s2)
          (h4 || s2.hasOwnProperty(I7)) && k10(s2[I7]);
        return;
      }
      if (g9 < 24 ? i2[r++] = 128 | g9 : G4(g9), h4 !== null)
        for (let I7 in s2)
          (h4 || s2.hasOwnProperty(I7)) && k10(s2[I7]);
    }, le2 = (s2) => {
      let h4;
      if (s2 > 16777216) {
        if (s2 - n4 > ft2)
          throw new Error("Encoded buffer would be larger than maximum buffer size");
        h4 = Math.min(ft2, Math.round(Math.max((s2 - n4) * (s2 > 67108864 ? 1.25 : 2), 4194304) / 4096) * 4096);
      } else
        h4 = (Math.max(s2 - n4 << 2, i2.length - 1) >> 12) + 1 << 12;
      let c8 = new Pe2(h4);
      return M3 = new DataView(c8.buffer, 0, h4), i2.copy ? i2.copy(c8, 0, n4, s2) : c8.set(i2.slice(n4, s2)), r -= n4, n4 = 0, X2 = c8.length - 10, i2 = c8;
    }, $5 = 100, Ye2 = 1e3;
    this.encodeAsIterable = function(s2, h4) {
      return He2(s2, h4, te2);
    }, this.encodeAsAsyncIterable = function(s2, h4) {
      return He2(s2, h4, Je2);
    };
    function* te2(s2, h4, c8) {
      let x8 = s2.constructor;
      if (x8 === Object) {
        let u7 = p10.useRecords !== false;
        u7 ? ke2(s2, null) : ot3(Object.keys(s2).length, 160);
        for (let g9 in s2) {
          let b7 = s2[g9];
          u7 || k10(g9), b7 && typeof b7 == "object" ? h4[g9] ? yield* te2(b7, h4[g9]) : yield* Oe2(b7, h4, g9) : k10(b7);
        }
      } else if (x8 === Array) {
        let u7 = s2.length;
        G4(u7);
        for (let g9 = 0; g9 < u7; g9++) {
          let b7 = s2[g9];
          b7 && (typeof b7 == "object" || r - n4 > $5) ? h4.element ? yield* te2(b7, h4.element) : yield* Oe2(b7, h4, "element") : k10(b7);
        }
      } else if (s2[Symbol.iterator]) {
        i2[r++] = 159;
        for (let u7 of s2)
          u7 && (typeof u7 == "object" || r - n4 > $5) ? h4.element ? yield* te2(u7, h4.element) : yield* Oe2(u7, h4, "element") : k10(u7);
        i2[r++] = 255;
      } else
        Ne2(s2) ? (ot3(s2.size, 64), yield i2.subarray(n4, r), yield s2, fe2()) : s2[Symbol.asyncIterator] ? (i2[r++] = 159, yield i2.subarray(n4, r), yield s2, fe2(), i2[r++] = 255) : k10(s2);
      c8 && r > n4 ? yield i2.subarray(n4, r) : r - n4 > $5 && (yield i2.subarray(n4, r), fe2());
    }
    function* Oe2(s2, h4, c8) {
      let x8 = r - n4;
      try {
        k10(s2), r - n4 > $5 && (yield i2.subarray(n4, r), fe2());
      } catch (u7) {
        if (u7.iteratorNotHandled)
          h4[c8] = {}, r = n4 + x8, yield* te2.call(this, s2, h4[c8]);
        else
          throw u7;
      }
    }
    function fe2() {
      $5 = Ye2, p10.encode(null, je2);
    }
    function He2(s2, h4, c8) {
      return h4 && h4.chunkThreshold ? $5 = Ye2 = h4.chunkThreshold : $5 = 100, s2 && typeof s2 == "object" ? (p10.encode(null, je2), c8(s2, p10.iterateProperties || (p10.iterateProperties = {}), true)) : [p10.encode(s2)];
    }
    async function* Je2(s2, h4) {
      for (let c8 of te2(s2, h4, true)) {
        let x8 = c8.constructor;
        if (x8 === st2 || x8 === Uint8Array)
          yield c8;
        else if (Ne2(c8)) {
          let u7 = c8.stream().getReader(), g9;
          for (; !(g9 = await u7.read()).done; )
            yield g9.value;
        } else if (c8[Symbol.asyncIterator])
          for await (let u7 of c8)
            fe2(), u7 ? yield* Je2(u7, h4.async || (h4.async = {})) : yield p10.encode(u7);
        else
          yield c8;
      }
    }
  }
  useBuffer(t3) {
    i2 = t3, M3 = new DataView(i2.buffer, i2.byteOffset, i2.byteLength), r = 0;
  }
  clearSharedData() {
    this.structures && (this.structures = []), this.sharedValues && (this.sharedValues = void 0);
  }
  updateSharedData() {
    let t3 = this.sharedVersion || 0;
    this.sharedVersion = t3 + 1;
    let l5 = this.structures.slice(0), n4 = new Se2(l5, this.sharedValues, this.sharedVersion), f8 = this.saveShared(n4, (o3) => (o3 && o3.version || 0) == t3);
    return f8 === false ? (n4 = this.getShared() || {}, this.structures = n4.structures || [], this.sharedValues = n4.packedValues, this.sharedVersion = n4.version, this.structures.nextId = this.structures.length) : l5.forEach((o3, d6) => this.structures[d6] = o3), f8;
  }
};
function ot3(e3, t3) {
  e3 < 24 ? i2[r++] = t3 | e3 : e3 < 256 ? (i2[r++] = t3 | 24, i2[r++] = e3) : e3 < 65536 ? (i2[r++] = t3 | 25, i2[r++] = e3 >> 8, i2[r++] = e3 & 255) : (i2[r++] = t3 | 26, M3.setUint32(r, e3), r += 4);
}
var Se2 = class {
  constructor(t3, l5, n4) {
    this.structures = t3, this.packedValues = l5, this.version = n4;
  }
};
function G4(e3) {
  e3 < 24 ? i2[r++] = 128 | e3 : e3 < 256 ? (i2[r++] = 152, i2[r++] = e3) : e3 < 65536 ? (i2[r++] = 153, i2[r++] = e3 >> 8, i2[r++] = e3 & 255) : (i2[r++] = 154, M3.setUint32(r, e3), r += 4);
}
var Rt = typeof Blob > "u" ? function() {
} : Blob;
function Ne2(e3) {
  if (e3 instanceof Rt)
    return true;
  let t3 = e3[Symbol.toStringTag];
  return t3 === "Blob" || t3 === "File";
}
function me2(e3, t3) {
  switch (typeof e3) {
    case "string":
      if (e3.length > 3) {
        if (t3.objectMap[e3] > -1 || t3.values.length >= t3.maxValues)
          return;
        let n4 = t3.get(e3);
        if (n4)
          ++n4.count == 2 && t3.values.push(e3);
        else if (t3.set(e3, { count: 1 }), t3.samplingPackedValues) {
          let f8 = t3.samplingPackedValues.get(e3);
          f8 ? f8.count++ : t3.samplingPackedValues.set(e3, { count: 1 });
        }
      }
      break;
    case "object":
      if (e3)
        if (e3 instanceof Array)
          for (let n4 = 0, f8 = e3.length; n4 < f8; n4++)
            me2(e3[n4], t3);
        else {
          let n4 = !t3.encoder.useRecords;
          for (var l5 in e3)
            e3.hasOwnProperty(l5) && (n4 && me2(l5, t3), me2(e3[l5], t3));
        }
      break;
    case "function":
      console.log(e3);
  }
}
var Bt = new Uint8Array(new Uint16Array([1]).buffer)[0] == 1;
ze2 = [Date, Set, Error, RegExp, H4, ArrayBuffer, Uint8Array, Uint8ClampedArray, Uint16Array, Uint32Array, typeof BigUint64Array > "u" ? function() {
} : BigUint64Array, Int8Array, Int16Array, Int32Array, typeof BigInt64Array > "u" ? function() {
} : BigInt64Array, Float32Array, Float64Array, Se2];
Ae2 = [{ tag: 1, encode(e3, t3) {
  let l5 = e3.getTime() / 1e3;
  (this.useTimestamp32 || e3.getMilliseconds() === 0) && l5 >= 0 && l5 < 4294967296 ? (i2[r++] = 26, M3.setUint32(r, l5), r += 4) : (i2[r++] = 251, M3.setFloat64(r, l5), r += 8);
} }, { tag: 258, encode(e3, t3) {
  let l5 = Array.from(e3);
  t3(l5);
} }, { tag: 27, encode(e3, t3) {
  t3([e3.name, e3.message]);
} }, { tag: 27, encode(e3, t3) {
  t3(["RegExp", e3.source, e3.flags]);
} }, { getTag(e3) {
  return e3.tag;
}, encode(e3, t3) {
  t3(e3.value);
} }, { encode(e3, t3, l5) {
  at2(e3, l5);
} }, { getTag(e3) {
  if (e3.constructor === Uint8Array && (this.tagUint8Array || de2 && this.tagUint8Array !== false))
    return 64;
}, encode(e3, t3, l5) {
  at2(e3, l5);
} }, J4(68, 1), J4(69, 2), J4(70, 4), J4(71, 8), J4(72, 1), J4(77, 2), J4(78, 4), J4(79, 8), J4(85, 4), J4(86, 8), { encode(e3, t3) {
  let l5 = e3.packedValues || [], n4 = e3.structures || [];
  if (l5.values.length > 0) {
    i2[r++] = 216, i2[r++] = 51, G4(4);
    let f8 = l5.values;
    t3(f8), G4(0), G4(0), packedObjectMap = Object.create(sharedPackedObjectMap || null);
    for (let o3 = 0, d6 = f8.length; o3 < d6; o3++)
      packedObjectMap[f8[o3]] = o3;
  }
  if (n4) {
    M3.setUint32(r, 3655335424), r += 3;
    let f8 = n4.slice(0);
    f8.unshift(57344), f8.push(new H4(e3.version, 1399353956)), t3(f8);
  } else
    t3(new H4(e3.version, 1399353956));
} }];
function J4(e3, t3) {
  return !Bt && t3 > 1 && (e3 -= 4), { tag: e3, encode: function(n4, f8) {
    let o3 = n4.byteLength, d6 = n4.byteOffset || 0, w9 = n4.buffer || n4;
    f8(de2 ? Ee2.from(w9, d6, o3) : new Uint8Array(w9, d6, o3));
  } };
}
function at2(e3, t3) {
  let l5 = e3.byteLength;
  l5 < 24 ? i2[r++] = 64 + l5 : l5 < 256 ? (i2[r++] = 88, i2[r++] = l5) : l5 < 65536 ? (i2[r++] = 89, i2[r++] = l5 >> 8, i2[r++] = l5 & 255) : (i2[r++] = 90, M3.setUint32(r, l5), r += 4), r + l5 >= i2.length && t3(r + l5), i2.set(e3.buffer ? e3 : new Uint8Array(e3), r), r += l5;
}
function Tt2(e3, t3) {
  let l5, n4 = t3.length * 2, f8 = e3.length - n4;
  t3.sort((o3, d6) => o3.offset > d6.offset ? 1 : -1);
  for (let o3 = 0; o3 < t3.length; o3++) {
    let d6 = t3[o3];
    d6.id = o3;
    for (let w9 of d6.references)
      e3[w9++] = o3 >> 8, e3[w9] = o3 & 255;
  }
  for (; l5 = t3.pop(); ) {
    let o3 = l5.offset;
    e3.copyWithin(o3 + n4, o3, f8), n4 -= 2;
    let d6 = o3 + n4;
    e3[d6++] = 216, e3[d6++] = 28, f8 = o3;
  }
  return e3;
}
function ct2(e3, t3) {
  M3.setUint32(D3.position + e3, r - D3.position - e3 + 1);
  let l5 = D3;
  D3 = null, t3(l5[0]), t3(l5[1]);
}
var We2 = new ee2({ useRecords: false });
var Ft2 = We2.encode;
var Vt = We2.encodeAsIterable;
var Ct3 = We2.encodeAsAsyncIterable;
var { NEVER: Pt2, ALWAYS: Lt2, DECIMAL_ROUND: Nt2, DECIMAL_FIT: jt2 } = we2;
var Ke2 = 512;
var Kt = 1024;
var je2 = 2048;

// ../desktop-dev/src/core/ipc-web/$messageToIpcMessage.ts
var $isIpcSignalMessage = (msg) => msg === "close" || msg === "ping" || msg === "pong";
var $objectToIpcMessage = (data, ipc) => {
  let message;
  if (data.type === 0 /* REQUEST */) {
    message = new IpcRequest(
      data.req_id,
      data.url,
      data.method,
      new IpcHeaders(data.headers),
      IpcBodyReceiver.from(MetaBody.fromJSON(data.metaBody), ipc),
      ipc
    );
  } else if (data.type === 1 /* RESPONSE */) {
    message = new IpcResponse(
      data.req_id,
      data.statusCode,
      new IpcHeaders(data.headers),
      IpcBodyReceiver.from(MetaBody.fromJSON(data.metaBody), ipc),
      ipc
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
var $messageToIpcMessage = (data, ipc) => {
  if ($isIpcSignalMessage(data)) {
    return data;
  }
  return $objectToIpcMessage(data, ipc);
};
var $jsonToIpcMessage = (data, ipc) => {
  if ($isIpcSignalMessage(data)) {
    return data;
  }
  return $objectToIpcMessage(JSON.parse(data), ipc);
};

// ../desktop-dev/src/core/ipc-web/$messagePackToIpcMessage.ts
var $messagePackToIpcMessage = (data, ipc) => {
  return $messageToIpcMessage(
    kt3(data),
    ipc
  );
};

// ../desktop-dev/src/core/ipc-web/ReadableStreamIpc.ts
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
    this.#rso = new ReadableStreamOut();
    this.PONG_DATA = once(() => {
      const pong = Ft2("pong");
      this._len[0] = pong.length;
      return u8aConcat([this._len_u8a, pong]);
    });
    this.CLOSE_DATA = once(() => {
      const close = Ft2("close");
      this._len[0] = close.length;
      return u8aConcat([this._len_u8a, close]);
    });
    this._len = new Uint32Array(1);
    this._len_u8a = new Uint8Array(this._len.buffer);
    this._support_message_pack = self_support_protocols.message_pack && remote.ipc_support_protocols.message_pack;
  }
  #rso;
  /** 这是输出流，给外部读取用的 */
  get stream() {
    return this.#rso.stream;
  }
  get controller() {
    return this.#rso.controller;
  }
  /**
   * 输入流要额外绑定
   * 注意，非必要不要 await 这个promise
   */
  async bindIncomeStream(stream, options = {}) {
    if (this._incomne_stream !== void 0) {
      throw new Error("in come stream alreay binded.");
    }
    this._incomne_stream = await stream;
    const { signal } = options;
    const reader = binaryStreamRead(this._incomne_stream, { signal });
    this.onClose(() => {
      reader.throw("output stream closed");
    });
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
    this.close();
  }
  _doPostMessage(message) {
    let message_raw;
    if (message.type === 0 /* REQUEST */) {
      message_raw = message.ipcReqMessage();
    } else if (message.type === 1 /* RESPONSE */) {
      message_raw = message.ipcResMessage();
    } else {
      message_raw = message;
    }
    const message_data = this.support_message_pack ? Ft2(message_raw) : simpleEncoder(JSON.stringify(message_raw), "utf8");
    this._len[0] = message_data.length;
    const chunk = u8aConcat([this._len_u8a, message_data]);
    this.controller.enqueue(chunk);
  }
  _doClose() {
    this.controller.enqueue(this.CLOSE_DATA());
    this.controller.close();
  }
};

// src/emulator/helper/helper.ts
var EMULATOR = "/emulator";
var BASE_URL = new URL(
  new URLSearchParams(location.search).get("X-Plaoc-Internal-Url" /* API_INTERNAL_URL */).replace(/^http:/, "ws:").replace(/^https:/, "wss:")
);
BASE_URL.pathname = EMULATOR;
var createMockModuleServerIpc = (mmid, apiUrl = BASE_URL) => {
  const waitOpenPo = new PromiseOut();
  const wsUrl = new URL(apiUrl);
  wsUrl.searchParams.set("mmid", mmid);
  const ws = new WebSocket(wsUrl);
  ws.binaryType = "arraybuffer";
  ws.onerror = (event) => {
    waitOpenPo.reject(event);
  };
  ws.onopen = () => {
    const serverIpc = new ReadableStreamIpc(
      {
        mmid,
        ipc_support_protocols: {
          message_pack: false,
          protobuf: false,
          raw: false
        },
        dweb_deeplinks: []
      },
      "client" /* CLIENT */
    );
    waitOpenPo.resolve(serverIpc);
    const proxyStream = new ReadableStreamOut({ highWaterMark: 0 });
    serverIpc.bindIncomeStream(proxyStream.stream);
    ws.onclose = () => {
      proxyStream.controller.close();
      serverIpc.close();
    };
    waitOpenPo.onError((event) => {
      proxyStream.controller.error(event.error);
    });
    ws.onmessage = (event) => {
      try {
        const data = event.data;
        if (typeof data === "string") {
          proxyStream.controller.enqueue(simpleEncoder(data, "utf8"));
        } else if (data instanceof ArrayBuffer) {
          proxyStream.controller.enqueue(new Uint8Array(data));
        } else {
          throw new Error("should not happend");
        }
      } catch (err) {
        console.error(err);
      }
    };
    void streamReadAll(serverIpc.stream, {
      map(chunk) {
        ws.send(chunk);
      },
      complete() {
        ws.close();
      }
    });
  };
  return waitOpenPo.promise;
};

// src/emulator/controller/base-controller.ts
var BaseController = class {
  // Using the Web Animations API
  onUpdate(cb) {
    this._onUpdate = cb;
    return this;
  }
  // <T>
  emitUpdate() {
    this._onUpdate?.();
  }
};

// src/emulator/controller/biometrics.controller.ts
var BiometricsController = class extends BaseController {
  constructor() {
    super(...arguments);
    this._init = (async () => {
      const ipc = await createMockModuleServerIpc("biometrics.sys.dweb");
      ipc.onFetch(async (event) => {
        const { pathname } = event;
        if (pathname === "/check") {
          return Response.json(true);
        }
        if (pathname === "/biometrics") {
          return Response.json(await this.biometricsMock());
        }
      }).forbidden().cors();
    })();
    this.queue = [];
  }
  get state() {
    return this.queue.at(0);
  }
  biometricsMock() {
    const task = new PromiseOut();
    this.queue.push(task);
    this.emitUpdate();
    task.onFinished(() => {
      this.queue = this.queue.filter((t3) => t3 !== task);
      this.emitUpdate();
    });
    return task.promise;
  }
};

// src/emulator/helper/StateObservable.ts
var StateObservable = class {
  constructor(getStateJson) {
    this.getStateJson = getStateJson;
    this._observerIpcMap = /* @__PURE__ */ new Map();
    this._changeSignal = createSignal();
    this._observe = (cb) => this._changeSignal.listen(cb);
  }
  startObserve(ipc) {
    mapHelper.getOrPut(this._observerIpcMap, ipc, (ipc2) => {
      return this._observe(() => {
        ipc2.postMessage(
          IpcEvent.fromUtf8(
            "observe",
            simpleEncoder(this.getStateJson(), "utf8")
          )
        );
      });
    });
  }
  notifyObserver() {
    this._changeSignal.emit();
  }
  stopObserve(ipc) {
    return mapHelper.getAndRemove(this._observerIpcMap, ipc)?.apply(void 0);
  }
};

// src/emulator/controller/status-bar.controller.ts
var StatusBarController = class extends BaseController {
  constructor() {
    super(...arguments);
    this._init = (async () => {
      const ipc = await createMockModuleServerIpc(
        "status-bar.nativeui.browser.dweb"
      );
      const query_state = Ct.object({
        color: zq.transform((color) => colorToHex(JSON.parse(color))).optional(),
        style: Ct.enum(["DARK", "LIGHT", "DEFAULT"]).optional(),
        overlay: zq.boolean().optional(),
        visible: zq.boolean().optional()
      });
      ipc.onFetch((event) => {
        const { pathname, searchParams, ipc: ipc2 } = event;
        if (pathname.endsWith("/getState")) {
          const state = this.statusBarGetState();
          return Response.json(state);
        }
        if (pathname.endsWith("/setState")) {
          const states = parseQuery(searchParams, query_state);
          this.statusBarSetState(states);
          return Response.json(null);
        }
        if (pathname.endsWith("/startObserve")) {
          this.observer.startObserve(ipc2);
          return Response.json(true);
        }
        if (pathname.endsWith("/stopObserve")) {
          this.observer.startObserve(ipc2);
          return Response.json("");
        }
      }).forbidden().cors();
    })();
    this.observer = new StateObservable(() => {
      return JSON.stringify(this.state);
    });
    this.state = {
      color: "#FFFFFF80",
      style: "DEFAULT",
      insets: {
        top: 38,
        right: 0,
        bottom: 0,
        left: 0
      },
      overlay: false,
      visible: true
    };
  }
  statusBarSetState(state) {
    this.state = {
      ...this.state,
      /// 这边这样做的目的是移除undefined值
      ...JSON.parse(JSON.stringify(state))
    };
    this.emitUpdate();
  }
  statusBarSetStyle(style) {
    this.state = {
      ...this.state,
      style
    };
    this.emitUpdate();
  }
  statusBarSetBackground(color) {
    this.state = {
      ...this.state,
      color
    };
    this.emitUpdate();
  }
  statusBarSetOverlay(overlay) {
    this.state = {
      ...this.state,
      overlay
    };
    this.emitUpdate();
  }
  statusBarSetVisible(visible) {
    this.state = {
      ...this.state,
      visible
    };
    this.emitUpdate();
  }
  statusBarGetState() {
    return {
      ...this.state,
      color: hexaToRGBA(this.state.color)
    };
  }
};

// https://esm.sh/v124/@lit/reactive-element@1.6.2/denonext/reactive-element.mjs
var l2 = window;
var c3 = l2.ShadowRoot && (l2.ShadyCSS === void 0 || l2.ShadyCSS.nativeShadow) && "adoptedStyleSheets" in Document.prototype && "replace" in CSSStyleSheet.prototype;
var u3 = Symbol();
var _4 = /* @__PURE__ */ new WeakMap();
var h2 = class {
  constructor(t3, e3, s2) {
    if (this._$cssResult$ = true, s2 !== u3)
      throw Error("CSSResult is not constructable. Use `unsafeCSS` or `css` instead.");
    this.cssText = t3, this.t = e3;
  }
  get styleSheet() {
    let t3 = this.o, e3 = this.t;
    if (c3 && t3 === void 0) {
      let s2 = e3 !== void 0 && e3.length === 1;
      s2 && (t3 = _4.get(e3)), t3 === void 0 && ((this.o = t3 = new CSSStyleSheet()).replaceSync(this.cssText), s2 && _4.set(e3, t3));
    }
    return t3;
  }
  toString() {
    return this.cssText;
  }
};
var $2 = (r3) => new h2(typeof r3 == "string" ? r3 : r3 + "", void 0, u3);
var v5 = (r3, t3) => {
  c3 ? r3.adoptedStyleSheets = t3.map((e3) => e3 instanceof CSSStyleSheet ? e3 : e3.styleSheet) : t3.forEach((e3) => {
    let s2 = document.createElement("style"), i3 = l2.litNonce;
    i3 !== void 0 && s2.setAttribute("nonce", i3), s2.textContent = e3.cssText, r3.appendChild(s2);
  });
};
var d2 = c3 ? (r3) => r3 : (r3) => r3 instanceof CSSStyleSheet ? ((t3) => {
  let e3 = "";
  for (let s2 of t3.cssRules)
    e3 += s2.cssText;
  return $2(e3);
})(r3) : r3;
var S3;
var p3 = window;
var m3 = p3.trustedTypes;
var U3 = m3 ? m3.emptyScript : "";
var b2 = p3.reactiveElementPolyfillSupport;
var y4 = { toAttribute(r3, t3) {
  switch (t3) {
    case Boolean:
      r3 = r3 ? U3 : null;
      break;
    case Object:
    case Array:
      r3 = r3 == null ? r3 : JSON.stringify(r3);
  }
  return r3;
}, fromAttribute(r3, t3) {
  let e3 = r3;
  switch (t3) {
    case Boolean:
      e3 = r3 !== null;
      break;
    case Number:
      e3 = r3 === null ? null : Number(r3);
      break;
    case Object:
    case Array:
      try {
        e3 = JSON.parse(r3);
      } catch {
        e3 = null;
      }
  }
  return e3;
} };
var g3 = (r3, t3) => t3 !== r3 && (t3 == t3 || r3 == r3);
var f = { attribute: true, type: String, converter: y4, reflect: false, hasChanged: g3 };
var E3 = "finalized";
var a2 = class extends HTMLElement {
  constructor() {
    super(), this._$Ei = /* @__PURE__ */ new Map(), this.isUpdatePending = false, this.hasUpdated = false, this._$El = null, this.u();
  }
  static addInitializer(t3) {
    var e3;
    this.finalize(), ((e3 = this.h) !== null && e3 !== void 0 ? e3 : this.h = []).push(t3);
  }
  static get observedAttributes() {
    this.finalize();
    let t3 = [];
    return this.elementProperties.forEach((e3, s2) => {
      let i3 = this._$Ep(s2, e3);
      i3 !== void 0 && (this._$Ev.set(i3, s2), t3.push(i3));
    }), t3;
  }
  static createProperty(t3, e3 = f) {
    if (e3.state && (e3.attribute = false), this.finalize(), this.elementProperties.set(t3, e3), !e3.noAccessor && !this.prototype.hasOwnProperty(t3)) {
      let s2 = typeof t3 == "symbol" ? Symbol() : "__" + t3, i3 = this.getPropertyDescriptor(t3, s2, e3);
      i3 !== void 0 && Object.defineProperty(this.prototype, t3, i3);
    }
  }
  static getPropertyDescriptor(t3, e3, s2) {
    return { get() {
      return this[e3];
    }, set(i3) {
      let o3 = this[t3];
      this[e3] = i3, this.requestUpdate(t3, o3, s2);
    }, configurable: true, enumerable: true };
  }
  static getPropertyOptions(t3) {
    return this.elementProperties.get(t3) || f;
  }
  static finalize() {
    if (this.hasOwnProperty(E3))
      return false;
    this[E3] = true;
    let t3 = Object.getPrototypeOf(this);
    if (t3.finalize(), t3.h !== void 0 && (this.h = [...t3.h]), this.elementProperties = new Map(t3.elementProperties), this._$Ev = /* @__PURE__ */ new Map(), this.hasOwnProperty("properties")) {
      let e3 = this.properties, s2 = [...Object.getOwnPropertyNames(e3), ...Object.getOwnPropertySymbols(e3)];
      for (let i3 of s2)
        this.createProperty(i3, e3[i3]);
    }
    return this.elementStyles = this.finalizeStyles(this.styles), true;
  }
  static finalizeStyles(t3) {
    let e3 = [];
    if (Array.isArray(t3)) {
      let s2 = new Set(t3.flat(1 / 0).reverse());
      for (let i3 of s2)
        e3.unshift(d2(i3));
    } else
      t3 !== void 0 && e3.push(d2(t3));
    return e3;
  }
  static _$Ep(t3, e3) {
    let s2 = e3.attribute;
    return s2 === false ? void 0 : typeof s2 == "string" ? s2 : typeof t3 == "string" ? t3.toLowerCase() : void 0;
  }
  u() {
    var t3;
    this._$E_ = new Promise((e3) => this.enableUpdating = e3), this._$AL = /* @__PURE__ */ new Map(), this._$Eg(), this.requestUpdate(), (t3 = this.constructor.h) === null || t3 === void 0 || t3.forEach((e3) => e3(this));
  }
  addController(t3) {
    var e3, s2;
    ((e3 = this._$ES) !== null && e3 !== void 0 ? e3 : this._$ES = []).push(t3), this.renderRoot !== void 0 && this.isConnected && ((s2 = t3.hostConnected) === null || s2 === void 0 || s2.call(t3));
  }
  removeController(t3) {
    var e3;
    (e3 = this._$ES) === null || e3 === void 0 || e3.splice(this._$ES.indexOf(t3) >>> 0, 1);
  }
  _$Eg() {
    this.constructor.elementProperties.forEach((t3, e3) => {
      this.hasOwnProperty(e3) && (this._$Ei.set(e3, this[e3]), delete this[e3]);
    });
  }
  createRenderRoot() {
    var t3;
    let e3 = (t3 = this.shadowRoot) !== null && t3 !== void 0 ? t3 : this.attachShadow(this.constructor.shadowRootOptions);
    return v5(e3, this.constructor.elementStyles), e3;
  }
  connectedCallback() {
    var t3;
    this.renderRoot === void 0 && (this.renderRoot = this.createRenderRoot()), this.enableUpdating(true), (t3 = this._$ES) === null || t3 === void 0 || t3.forEach((e3) => {
      var s2;
      return (s2 = e3.hostConnected) === null || s2 === void 0 ? void 0 : s2.call(e3);
    });
  }
  enableUpdating(t3) {
  }
  disconnectedCallback() {
    var t3;
    (t3 = this._$ES) === null || t3 === void 0 || t3.forEach((e3) => {
      var s2;
      return (s2 = e3.hostDisconnected) === null || s2 === void 0 ? void 0 : s2.call(e3);
    });
  }
  attributeChangedCallback(t3, e3, s2) {
    this._$AK(t3, s2);
  }
  _$EO(t3, e3, s2 = f) {
    var i3;
    let o3 = this.constructor._$Ep(t3, s2);
    if (o3 !== void 0 && s2.reflect === true) {
      let n4 = (((i3 = s2.converter) === null || i3 === void 0 ? void 0 : i3.toAttribute) !== void 0 ? s2.converter : y4).toAttribute(e3, s2.type);
      this._$El = t3, n4 == null ? this.removeAttribute(o3) : this.setAttribute(o3, n4), this._$El = null;
    }
  }
  _$AK(t3, e3) {
    var s2;
    let i3 = this.constructor, o3 = i3._$Ev.get(t3);
    if (o3 !== void 0 && this._$El !== o3) {
      let n4 = i3.getPropertyOptions(o3), C11 = typeof n4.converter == "function" ? { fromAttribute: n4.converter } : ((s2 = n4.converter) === null || s2 === void 0 ? void 0 : s2.fromAttribute) !== void 0 ? n4.converter : y4;
      this._$El = o3, this[o3] = C11.fromAttribute(e3, n4.type), this._$El = null;
    }
  }
  requestUpdate(t3, e3, s2) {
    let i3 = true;
    t3 !== void 0 && (((s2 = s2 || this.constructor.getPropertyOptions(t3)).hasChanged || g3)(this[t3], e3) ? (this._$AL.has(t3) || this._$AL.set(t3, e3), s2.reflect === true && this._$El !== t3 && (this._$EC === void 0 && (this._$EC = /* @__PURE__ */ new Map()), this._$EC.set(t3, s2))) : i3 = false), !this.isUpdatePending && i3 && (this._$E_ = this._$Ej());
  }
  async _$Ej() {
    this.isUpdatePending = true;
    try {
      await this._$E_;
    } catch (e3) {
      Promise.reject(e3);
    }
    let t3 = this.scheduleUpdate();
    return t3 != null && await t3, !this.isUpdatePending;
  }
  scheduleUpdate() {
    return this.performUpdate();
  }
  performUpdate() {
    var t3;
    if (!this.isUpdatePending)
      return;
    this.hasUpdated, this._$Ei && (this._$Ei.forEach((i3, o3) => this[o3] = i3), this._$Ei = void 0);
    let e3 = false, s2 = this._$AL;
    try {
      e3 = this.shouldUpdate(s2), e3 ? (this.willUpdate(s2), (t3 = this._$ES) === null || t3 === void 0 || t3.forEach((i3) => {
        var o3;
        return (o3 = i3.hostUpdate) === null || o3 === void 0 ? void 0 : o3.call(i3);
      }), this.update(s2)) : this._$Ek();
    } catch (i3) {
      throw e3 = false, this._$Ek(), i3;
    }
    e3 && this._$AE(s2);
  }
  willUpdate(t3) {
  }
  _$AE(t3) {
    var e3;
    (e3 = this._$ES) === null || e3 === void 0 || e3.forEach((s2) => {
      var i3;
      return (i3 = s2.hostUpdated) === null || i3 === void 0 ? void 0 : i3.call(s2);
    }), this.hasUpdated || (this.hasUpdated = true, this.firstUpdated(t3)), this.updated(t3);
  }
  _$Ek() {
    this._$AL = /* @__PURE__ */ new Map(), this.isUpdatePending = false;
  }
  get updateComplete() {
    return this.getUpdateComplete();
  }
  getUpdateComplete() {
    return this._$E_;
  }
  shouldUpdate(t3) {
    return true;
  }
  update(t3) {
    this._$EC !== void 0 && (this._$EC.forEach((e3, s2) => this._$EO(s2, this[s2], e3)), this._$EC = void 0), this._$Ek();
  }
  updated(t3) {
  }
  firstUpdated(t3) {
  }
};
a2[E3] = true, a2.elementProperties = /* @__PURE__ */ new Map(), a2.elementStyles = [], a2.shadowRootOptions = { mode: "open" }, b2?.({ ReactiveElement: a2 }), ((S3 = p3.reactiveElementVersions) !== null && S3 !== void 0 ? S3 : p3.reactiveElementVersions = []).push("1.6.2");

// https://esm.sh/v124/lit-html@2.7.4/denonext/lit-html.mjs
var R3;
var S4 = window;
var x3 = S4.trustedTypes;
var D4 = x3 ? x3.createPolicy("lit-html", { createHTML: (h4) => h4 }) : void 0;
var E4 = "$lit$";
var _5 = `lit$${(Math.random() + "").slice(9)}$`;
var k5 = "?" + _5;
var X3 = `<${k5}>`;
var m4 = document;
var C6 = () => m4.createComment("");
var b3 = (h4) => h4 === null || typeof h4 != "object" && typeof h4 != "function";
var q2 = Array.isArray;
var G5 = (h4) => q2(h4) || typeof h4?.[Symbol.iterator] == "function";
var j5 = `[ 	
\f\r]`;
var N3 = /<(?:(!--|\/[^a-zA-Z])|(\/?[a-zA-Z][^>\s]*)|(\/?$))/g;
var W3 = /-->/g;
var O3 = />/g;
var p4 = RegExp(`>|${j5}(?:([^\\s"'>=/]+)(${j5}*=${j5}*(?:[^ 	
\f\r"'\`<>=]|("|')|))|$)`, "g");
var V2 = /'/g;
var Z2 = /"/g;
var J5 = /^(?:script|style|textarea|title)$/i;
var K4 = (h4) => (t3, ...e3) => ({ _$litType$: h4, strings: t3, values: e3 });
var tt3 = K4(1);
var et4 = K4(2);
var w3 = Symbol.for("lit-noChange");
var A4 = Symbol.for("lit-nothing");
var z3 = /* @__PURE__ */ new WeakMap();
var g4 = m4.createTreeWalker(m4, 129, null, false);
var Q3 = (h4, t3) => {
  let e3 = h4.length - 1, i3 = [], s2, o3 = t3 === 2 ? "<svg>" : "", n4 = N3;
  for (let l5 = 0; l5 < e3; l5++) {
    let r3 = h4[l5], u7, $5, d6 = -1, c8 = 0;
    for (; c8 < r3.length && (n4.lastIndex = c8, $5 = n4.exec(r3), $5 !== null); )
      c8 = n4.lastIndex, n4 === N3 ? $5[1] === "!--" ? n4 = W3 : $5[1] !== void 0 ? n4 = O3 : $5[2] !== void 0 ? (J5.test($5[2]) && (s2 = RegExp("</" + $5[2], "g")), n4 = p4) : $5[3] !== void 0 && (n4 = p4) : n4 === p4 ? $5[0] === ">" ? (n4 = s2 ?? N3, d6 = -1) : $5[1] === void 0 ? d6 = -2 : (d6 = n4.lastIndex - $5[2].length, u7 = $5[1], n4 = $5[3] === void 0 ? p4 : $5[3] === '"' ? Z2 : V2) : n4 === Z2 || n4 === V2 ? n4 = p4 : n4 === W3 || n4 === O3 ? n4 = N3 : (n4 = p4, s2 = void 0);
    let T7 = n4 === p4 && h4[l5 + 1].startsWith("/>") ? " " : "";
    o3 += n4 === N3 ? r3 + X3 : d6 >= 0 ? (i3.push(u7), r3.slice(0, d6) + E4 + r3.slice(d6) + _5 + T7) : r3 + _5 + (d6 === -2 ? (i3.push(void 0), l5) : T7);
  }
  let a4 = o3 + (h4[e3] || "<?>") + (t3 === 2 ? "</svg>" : "");
  if (!Array.isArray(h4) || !h4.hasOwnProperty("raw"))
    throw Error("invalid template strings array");
  return [D4 !== void 0 ? D4.createHTML(a4) : a4, i3];
};
var f2 = class {
  constructor({ strings: t3, _$litType$: e3 }, i3) {
    let s2;
    this.parts = [];
    let o3 = 0, n4 = 0, a4 = t3.length - 1, l5 = this.parts, [r3, u7] = Q3(t3, e3);
    if (this.el = f2.createElement(r3, i3), g4.currentNode = this.el.content, e3 === 2) {
      let $5 = this.el.content, d6 = $5.firstChild;
      d6.remove(), $5.append(...d6.childNodes);
    }
    for (; (s2 = g4.nextNode()) !== null && l5.length < a4; ) {
      if (s2.nodeType === 1) {
        if (s2.hasAttributes()) {
          let $5 = [];
          for (let d6 of s2.getAttributeNames())
            if (d6.endsWith(E4) || d6.startsWith(_5)) {
              let c8 = u7[n4++];
              if ($5.push(d6), c8 !== void 0) {
                let T7 = s2.getAttribute(c8.toLowerCase() + E4).split(_5), M8 = /([.?@])?(.*)/.exec(c8);
                l5.push({ type: 1, index: o3, name: M8[2], strings: T7, ctor: M8[1] === "." ? B4 : M8[1] === "?" ? P3 : M8[1] === "@" ? U4 : H5 });
              } else
                l5.push({ type: 6, index: o3 });
            }
          for (let d6 of $5)
            s2.removeAttribute(d6);
        }
        if (J5.test(s2.tagName)) {
          let $5 = s2.textContent.split(_5), d6 = $5.length - 1;
          if (d6 > 0) {
            s2.textContent = x3 ? x3.emptyScript : "";
            for (let c8 = 0; c8 < d6; c8++)
              s2.append($5[c8], C6()), g4.nextNode(), l5.push({ type: 2, index: ++o3 });
            s2.append($5[d6], C6());
          }
        }
      } else if (s2.nodeType === 8)
        if (s2.data === k5)
          l5.push({ type: 2, index: o3 });
        else {
          let $5 = -1;
          for (; ($5 = s2.data.indexOf(_5, $5 + 1)) !== -1; )
            l5.push({ type: 7, index: o3 }), $5 += _5.length - 1;
        }
      o3++;
    }
  }
  static createElement(t3, e3) {
    let i3 = m4.createElement("template");
    return i3.innerHTML = t3, i3;
  }
};
function y5(h4, t3, e3 = h4, i3) {
  var s2, o3, n4, a4;
  if (t3 === w3)
    return t3;
  let l5 = i3 !== void 0 ? (s2 = e3._$Co) === null || s2 === void 0 ? void 0 : s2[i3] : e3._$Cl, r3 = b3(t3) ? void 0 : t3._$litDirective$;
  return l5?.constructor !== r3 && ((o3 = l5?._$AO) === null || o3 === void 0 || o3.call(l5, false), r3 === void 0 ? l5 = void 0 : (l5 = new r3(h4), l5._$AT(h4, e3, i3)), i3 !== void 0 ? ((n4 = (a4 = e3)._$Co) !== null && n4 !== void 0 ? n4 : a4._$Co = [])[i3] = l5 : e3._$Cl = l5), l5 !== void 0 && (t3 = y5(h4, l5._$AS(h4, t3.values), l5, i3)), t3;
}
var I2 = class {
  constructor(t3, e3) {
    this._$AV = [], this._$AN = void 0, this._$AD = t3, this._$AM = e3;
  }
  get parentNode() {
    return this._$AM.parentNode;
  }
  get _$AU() {
    return this._$AM._$AU;
  }
  u(t3) {
    var e3;
    let { el: { content: i3 }, parts: s2 } = this._$AD, o3 = ((e3 = t3?.creationScope) !== null && e3 !== void 0 ? e3 : m4).importNode(i3, true);
    g4.currentNode = o3;
    let n4 = g4.nextNode(), a4 = 0, l5 = 0, r3 = s2[0];
    for (; r3 !== void 0; ) {
      if (a4 === r3.index) {
        let u7;
        r3.type === 2 ? u7 = new v6(n4, n4.nextSibling, this, t3) : r3.type === 1 ? u7 = new r3.ctor(n4, r3.name, r3.strings, this, t3) : r3.type === 6 && (u7 = new L3(n4, this, t3)), this._$AV.push(u7), r3 = s2[++l5];
      }
      a4 !== r3?.index && (n4 = g4.nextNode(), a4++);
    }
    return g4.currentNode = m4, o3;
  }
  v(t3) {
    let e3 = 0;
    for (let i3 of this._$AV)
      i3 !== void 0 && (i3.strings !== void 0 ? (i3._$AI(t3, i3, e3), e3 += i3.strings.length - 2) : i3._$AI(t3[e3])), e3++;
  }
};
var v6 = class {
  constructor(t3, e3, i3, s2) {
    var o3;
    this.type = 2, this._$AH = A4, this._$AN = void 0, this._$AA = t3, this._$AB = e3, this._$AM = i3, this.options = s2, this._$Cp = (o3 = s2?.isConnected) === null || o3 === void 0 || o3;
  }
  get _$AU() {
    var t3, e3;
    return (e3 = (t3 = this._$AM) === null || t3 === void 0 ? void 0 : t3._$AU) !== null && e3 !== void 0 ? e3 : this._$Cp;
  }
  get parentNode() {
    let t3 = this._$AA.parentNode, e3 = this._$AM;
    return e3 !== void 0 && t3?.nodeType === 11 && (t3 = e3.parentNode), t3;
  }
  get startNode() {
    return this._$AA;
  }
  get endNode() {
    return this._$AB;
  }
  _$AI(t3, e3 = this) {
    t3 = y5(this, t3, e3), b3(t3) ? t3 === A4 || t3 == null || t3 === "" ? (this._$AH !== A4 && this._$AR(), this._$AH = A4) : t3 !== this._$AH && t3 !== w3 && this._(t3) : t3._$litType$ !== void 0 ? this.g(t3) : t3.nodeType !== void 0 ? this.$(t3) : G5(t3) ? this.T(t3) : this._(t3);
  }
  k(t3) {
    return this._$AA.parentNode.insertBefore(t3, this._$AB);
  }
  $(t3) {
    this._$AH !== t3 && (this._$AR(), this._$AH = this.k(t3));
  }
  _(t3) {
    this._$AH !== A4 && b3(this._$AH) ? this._$AA.nextSibling.data = t3 : this.$(m4.createTextNode(t3)), this._$AH = t3;
  }
  g(t3) {
    var e3;
    let { values: i3, _$litType$: s2 } = t3, o3 = typeof s2 == "number" ? this._$AC(t3) : (s2.el === void 0 && (s2.el = f2.createElement(s2.h, this.options)), s2);
    if (((e3 = this._$AH) === null || e3 === void 0 ? void 0 : e3._$AD) === o3)
      this._$AH.v(i3);
    else {
      let n4 = new I2(o3, this), a4 = n4.u(this.options);
      n4.v(i3), this.$(a4), this._$AH = n4;
    }
  }
  _$AC(t3) {
    let e3 = z3.get(t3.strings);
    return e3 === void 0 && z3.set(t3.strings, e3 = new f2(t3)), e3;
  }
  T(t3) {
    q2(this._$AH) || (this._$AH = [], this._$AR());
    let e3 = this._$AH, i3, s2 = 0;
    for (let o3 of t3)
      s2 === e3.length ? e3.push(i3 = new v6(this.k(C6()), this.k(C6()), this, this.options)) : i3 = e3[s2], i3._$AI(o3), s2++;
    s2 < e3.length && (this._$AR(i3 && i3._$AB.nextSibling, s2), e3.length = s2);
  }
  _$AR(t3 = this._$AA.nextSibling, e3) {
    var i3;
    for ((i3 = this._$AP) === null || i3 === void 0 || i3.call(this, false, true, e3); t3 && t3 !== this._$AB; ) {
      let s2 = t3.nextSibling;
      t3.remove(), t3 = s2;
    }
  }
  setConnected(t3) {
    var e3;
    this._$AM === void 0 && (this._$Cp = t3, (e3 = this._$AP) === null || e3 === void 0 || e3.call(this, t3));
  }
};
var H5 = class {
  constructor(t3, e3, i3, s2, o3) {
    this.type = 1, this._$AH = A4, this._$AN = void 0, this.element = t3, this.name = e3, this._$AM = s2, this.options = o3, i3.length > 2 || i3[0] !== "" || i3[1] !== "" ? (this._$AH = Array(i3.length - 1).fill(new String()), this.strings = i3) : this._$AH = A4;
  }
  get tagName() {
    return this.element.tagName;
  }
  get _$AU() {
    return this._$AM._$AU;
  }
  _$AI(t3, e3 = this, i3, s2) {
    let o3 = this.strings, n4 = false;
    if (o3 === void 0)
      t3 = y5(this, t3, e3, 0), n4 = !b3(t3) || t3 !== this._$AH && t3 !== w3, n4 && (this._$AH = t3);
    else {
      let a4 = t3, l5, r3;
      for (t3 = o3[0], l5 = 0; l5 < o3.length - 1; l5++)
        r3 = y5(this, a4[i3 + l5], e3, l5), r3 === w3 && (r3 = this._$AH[l5]), n4 || (n4 = !b3(r3) || r3 !== this._$AH[l5]), r3 === A4 ? t3 = A4 : t3 !== A4 && (t3 += (r3 ?? "") + o3[l5 + 1]), this._$AH[l5] = r3;
    }
    n4 && !s2 && this.j(t3);
  }
  j(t3) {
    t3 === A4 ? this.element.removeAttribute(this.name) : this.element.setAttribute(this.name, t3 ?? "");
  }
};
var B4 = class extends H5 {
  constructor() {
    super(...arguments), this.type = 3;
  }
  j(t3) {
    this.element[this.name] = t3 === A4 ? void 0 : t3;
  }
};
var Y3 = x3 ? x3.emptyScript : "";
var P3 = class extends H5 {
  constructor() {
    super(...arguments), this.type = 4;
  }
  j(t3) {
    t3 && t3 !== A4 ? this.element.setAttribute(this.name, Y3) : this.element.removeAttribute(this.name);
  }
};
var U4 = class extends H5 {
  constructor(t3, e3, i3, s2, o3) {
    super(t3, e3, i3, s2, o3), this.type = 5;
  }
  _$AI(t3, e3 = this) {
    var i3;
    if ((t3 = (i3 = y5(this, t3, e3, 0)) !== null && i3 !== void 0 ? i3 : A4) === w3)
      return;
    let s2 = this._$AH, o3 = t3 === A4 && s2 !== A4 || t3.capture !== s2.capture || t3.once !== s2.once || t3.passive !== s2.passive, n4 = t3 !== A4 && (s2 === A4 || o3);
    o3 && this.element.removeEventListener(this.name, this, s2), n4 && this.element.addEventListener(this.name, this, t3), this._$AH = t3;
  }
  handleEvent(t3) {
    var e3, i3;
    typeof this._$AH == "function" ? this._$AH.call((i3 = (e3 = this.options) === null || e3 === void 0 ? void 0 : e3.host) !== null && i3 !== void 0 ? i3 : this.element, t3) : this._$AH.handleEvent(t3);
  }
};
var L3 = class {
  constructor(t3, e3, i3) {
    this.element = t3, this.type = 6, this._$AN = void 0, this._$AM = e3, this.options = i3;
  }
  get _$AU() {
    return this._$AM._$AU;
  }
  _$AI(t3) {
    y5(this, t3);
  }
};
var F3 = S4.litHtmlPolyfillSupport;
F3?.(f2, v6), ((R3 = S4.litHtmlVersions) !== null && R3 !== void 0 ? R3 : S4.litHtmlVersions = []).push("2.7.4");
var st3 = (h4, t3, e3) => {
  var i3, s2;
  let o3 = (i3 = e3?.renderBefore) !== null && i3 !== void 0 ? i3 : t3, n4 = o3._$litPart$;
  if (n4 === void 0) {
    let a4 = (s2 = e3?.renderBefore) !== null && s2 !== void 0 ? s2 : null;
    o3._$litPart$ = n4 = new v6(t3.insertBefore(C6(), a4), a4, void 0, e3 ?? {});
  }
  return n4._$AI(h4), n4;
};

// https://esm.sh/v124/@lit/reactive-element@1.6.1/denonext/reactive-element.mjs
var l3 = window;
var c4 = l3.ShadowRoot && (l3.ShadyCSS === void 0 || l3.ShadyCSS.nativeShadow) && "adoptedStyleSheets" in Document.prototype && "replace" in CSSStyleSheet.prototype;
var u4 = Symbol();
var E5 = /* @__PURE__ */ new WeakMap();
var h3 = class {
  constructor(t3, e3, s2) {
    if (this._$cssResult$ = true, s2 !== u4)
      throw Error("CSSResult is not constructable. Use `unsafeCSS` or `css` instead.");
    this.cssText = t3, this.t = e3;
  }
  get styleSheet() {
    let t3 = this.o, e3 = this.t;
    if (c4 && t3 === void 0) {
      let s2 = e3 !== void 0 && e3.length === 1;
      s2 && (t3 = E5.get(e3)), t3 === void 0 && ((this.o = t3 = new CSSStyleSheet()).replaceSync(this.cssText), s2 && E5.set(e3, t3));
    }
    return t3;
  }
  toString() {
    return this.cssText;
  }
};
var _6 = (r3) => new h3(typeof r3 == "string" ? r3 : r3 + "", void 0, u4);
var C7 = (r3, ...t3) => {
  let e3 = r3.length === 1 ? r3[0] : t3.reduce((s2, i3, o3) => s2 + ((n4) => {
    if (n4._$cssResult$ === true)
      return n4.cssText;
    if (typeof n4 == "number")
      return n4;
    throw Error("Value passed to 'css' function must be a 'css' function result: " + n4 + ". Use 'unsafeCSS' to pass non-literal values, but take care to ensure page security.");
  })(i3) + r3[o3 + 1], r3[0]);
  return new h3(e3, r3, u4);
};
var v7 = (r3, t3) => {
  c4 ? r3.adoptedStyleSheets = t3.map((e3) => e3 instanceof CSSStyleSheet ? e3 : e3.styleSheet) : t3.forEach((e3) => {
    let s2 = document.createElement("style"), i3 = l3.litNonce;
    i3 !== void 0 && s2.setAttribute("nonce", i3), s2.textContent = e3.cssText, r3.appendChild(s2);
  });
};
var d3 = c4 ? (r3) => r3 : (r3) => r3 instanceof CSSStyleSheet ? ((t3) => {
  let e3 = "";
  for (let s2 of t3.cssRules)
    e3 += s2.cssText;
  return _6(e3);
})(r3) : r3;
var S5;
var p5 = window;
var $3 = p5.trustedTypes;
var w4 = $3 ? $3.emptyScript : "";
var m5 = p5.reactiveElementPolyfillSupport;
var y6 = { toAttribute(r3, t3) {
  switch (t3) {
    case Boolean:
      r3 = r3 ? w4 : null;
      break;
    case Object:
    case Array:
      r3 = r3 == null ? r3 : JSON.stringify(r3);
  }
  return r3;
}, fromAttribute(r3, t3) {
  let e3 = r3;
  switch (t3) {
    case Boolean:
      e3 = r3 !== null;
      break;
    case Number:
      e3 = r3 === null ? null : Number(r3);
      break;
    case Object:
    case Array:
      try {
        e3 = JSON.parse(r3);
      } catch {
        e3 = null;
      }
  }
  return e3;
} };
var b4 = (r3, t3) => t3 !== r3 && (t3 == t3 || r3 == r3);
var f3 = { attribute: true, type: String, converter: y6, reflect: false, hasChanged: b4 };
var a3 = class extends HTMLElement {
  constructor() {
    super(), this._$Ei = /* @__PURE__ */ new Map(), this.isUpdatePending = false, this.hasUpdated = false, this._$El = null, this.u();
  }
  static addInitializer(t3) {
    var e3;
    this.finalize(), ((e3 = this.h) !== null && e3 !== void 0 ? e3 : this.h = []).push(t3);
  }
  static get observedAttributes() {
    this.finalize();
    let t3 = [];
    return this.elementProperties.forEach((e3, s2) => {
      let i3 = this._$Ep(s2, e3);
      i3 !== void 0 && (this._$Ev.set(i3, s2), t3.push(i3));
    }), t3;
  }
  static createProperty(t3, e3 = f3) {
    if (e3.state && (e3.attribute = false), this.finalize(), this.elementProperties.set(t3, e3), !e3.noAccessor && !this.prototype.hasOwnProperty(t3)) {
      let s2 = typeof t3 == "symbol" ? Symbol() : "__" + t3, i3 = this.getPropertyDescriptor(t3, s2, e3);
      i3 !== void 0 && Object.defineProperty(this.prototype, t3, i3);
    }
  }
  static getPropertyDescriptor(t3, e3, s2) {
    return { get() {
      return this[e3];
    }, set(i3) {
      let o3 = this[t3];
      this[e3] = i3, this.requestUpdate(t3, o3, s2);
    }, configurable: true, enumerable: true };
  }
  static getPropertyOptions(t3) {
    return this.elementProperties.get(t3) || f3;
  }
  static finalize() {
    if (this.hasOwnProperty("finalized"))
      return false;
    this.finalized = true;
    let t3 = Object.getPrototypeOf(this);
    if (t3.finalize(), t3.h !== void 0 && (this.h = [...t3.h]), this.elementProperties = new Map(t3.elementProperties), this._$Ev = /* @__PURE__ */ new Map(), this.hasOwnProperty("properties")) {
      let e3 = this.properties, s2 = [...Object.getOwnPropertyNames(e3), ...Object.getOwnPropertySymbols(e3)];
      for (let i3 of s2)
        this.createProperty(i3, e3[i3]);
    }
    return this.elementStyles = this.finalizeStyles(this.styles), true;
  }
  static finalizeStyles(t3) {
    let e3 = [];
    if (Array.isArray(t3)) {
      let s2 = new Set(t3.flat(1 / 0).reverse());
      for (let i3 of s2)
        e3.unshift(d3(i3));
    } else
      t3 !== void 0 && e3.push(d3(t3));
    return e3;
  }
  static _$Ep(t3, e3) {
    let s2 = e3.attribute;
    return s2 === false ? void 0 : typeof s2 == "string" ? s2 : typeof t3 == "string" ? t3.toLowerCase() : void 0;
  }
  u() {
    var t3;
    this._$E_ = new Promise((e3) => this.enableUpdating = e3), this._$AL = /* @__PURE__ */ new Map(), this._$Eg(), this.requestUpdate(), (t3 = this.constructor.h) === null || t3 === void 0 || t3.forEach((e3) => e3(this));
  }
  addController(t3) {
    var e3, s2;
    ((e3 = this._$ES) !== null && e3 !== void 0 ? e3 : this._$ES = []).push(t3), this.renderRoot !== void 0 && this.isConnected && ((s2 = t3.hostConnected) === null || s2 === void 0 || s2.call(t3));
  }
  removeController(t3) {
    var e3;
    (e3 = this._$ES) === null || e3 === void 0 || e3.splice(this._$ES.indexOf(t3) >>> 0, 1);
  }
  _$Eg() {
    this.constructor.elementProperties.forEach((t3, e3) => {
      this.hasOwnProperty(e3) && (this._$Ei.set(e3, this[e3]), delete this[e3]);
    });
  }
  createRenderRoot() {
    var t3;
    let e3 = (t3 = this.shadowRoot) !== null && t3 !== void 0 ? t3 : this.attachShadow(this.constructor.shadowRootOptions);
    return v7(e3, this.constructor.elementStyles), e3;
  }
  connectedCallback() {
    var t3;
    this.renderRoot === void 0 && (this.renderRoot = this.createRenderRoot()), this.enableUpdating(true), (t3 = this._$ES) === null || t3 === void 0 || t3.forEach((e3) => {
      var s2;
      return (s2 = e3.hostConnected) === null || s2 === void 0 ? void 0 : s2.call(e3);
    });
  }
  enableUpdating(t3) {
  }
  disconnectedCallback() {
    var t3;
    (t3 = this._$ES) === null || t3 === void 0 || t3.forEach((e3) => {
      var s2;
      return (s2 = e3.hostDisconnected) === null || s2 === void 0 ? void 0 : s2.call(e3);
    });
  }
  attributeChangedCallback(t3, e3, s2) {
    this._$AK(t3, s2);
  }
  _$EO(t3, e3, s2 = f3) {
    var i3;
    let o3 = this.constructor._$Ep(t3, s2);
    if (o3 !== void 0 && s2.reflect === true) {
      let n4 = (((i3 = s2.converter) === null || i3 === void 0 ? void 0 : i3.toAttribute) !== void 0 ? s2.converter : y6).toAttribute(e3, s2.type);
      this._$El = t3, n4 == null ? this.removeAttribute(o3) : this.setAttribute(o3, n4), this._$El = null;
    }
  }
  _$AK(t3, e3) {
    var s2;
    let i3 = this.constructor, o3 = i3._$Ev.get(t3);
    if (o3 !== void 0 && this._$El !== o3) {
      let n4 = i3.getPropertyOptions(o3), g9 = typeof n4.converter == "function" ? { fromAttribute: n4.converter } : ((s2 = n4.converter) === null || s2 === void 0 ? void 0 : s2.fromAttribute) !== void 0 ? n4.converter : y6;
      this._$El = o3, this[o3] = g9.fromAttribute(e3, n4.type), this._$El = null;
    }
  }
  requestUpdate(t3, e3, s2) {
    let i3 = true;
    t3 !== void 0 && (((s2 = s2 || this.constructor.getPropertyOptions(t3)).hasChanged || b4)(this[t3], e3) ? (this._$AL.has(t3) || this._$AL.set(t3, e3), s2.reflect === true && this._$El !== t3 && (this._$EC === void 0 && (this._$EC = /* @__PURE__ */ new Map()), this._$EC.set(t3, s2))) : i3 = false), !this.isUpdatePending && i3 && (this._$E_ = this._$Ej());
  }
  async _$Ej() {
    this.isUpdatePending = true;
    try {
      await this._$E_;
    } catch (e3) {
      Promise.reject(e3);
    }
    let t3 = this.scheduleUpdate();
    return t3 != null && await t3, !this.isUpdatePending;
  }
  scheduleUpdate() {
    return this.performUpdate();
  }
  performUpdate() {
    var t3;
    if (!this.isUpdatePending)
      return;
    this.hasUpdated, this._$Ei && (this._$Ei.forEach((i3, o3) => this[o3] = i3), this._$Ei = void 0);
    let e3 = false, s2 = this._$AL;
    try {
      e3 = this.shouldUpdate(s2), e3 ? (this.willUpdate(s2), (t3 = this._$ES) === null || t3 === void 0 || t3.forEach((i3) => {
        var o3;
        return (o3 = i3.hostUpdate) === null || o3 === void 0 ? void 0 : o3.call(i3);
      }), this.update(s2)) : this._$Ek();
    } catch (i3) {
      throw e3 = false, this._$Ek(), i3;
    }
    e3 && this._$AE(s2);
  }
  willUpdate(t3) {
  }
  _$AE(t3) {
    var e3;
    (e3 = this._$ES) === null || e3 === void 0 || e3.forEach((s2) => {
      var i3;
      return (i3 = s2.hostUpdated) === null || i3 === void 0 ? void 0 : i3.call(s2);
    }), this.hasUpdated || (this.hasUpdated = true, this.firstUpdated(t3)), this.updated(t3);
  }
  _$Ek() {
    this._$AL = /* @__PURE__ */ new Map(), this.isUpdatePending = false;
  }
  get updateComplete() {
    return this.getUpdateComplete();
  }
  getUpdateComplete() {
    return this._$E_;
  }
  shouldUpdate(t3) {
    return true;
  }
  update(t3) {
    this._$EC !== void 0 && (this._$EC.forEach((e3, s2) => this._$EO(s2, this[s2], e3)), this._$EC = void 0), this._$Ek();
  }
  updated(t3) {
  }
  firstUpdated(t3) {
  }
};
a3.finalized = true, a3.elementProperties = /* @__PURE__ */ new Map(), a3.elementStyles = [], a3.shadowRootOptions = { mode: "open" }, m5?.({ ReactiveElement: a3 }), ((S5 = p5.reactiveElementVersions) !== null && S5 !== void 0 ? S5 : p5.reactiveElementVersions = []).push("1.6.1");

// https://esm.sh/v124/lit-element@3.3.2/denonext/lit-element.js
var r2;
var s;
var n = class extends a3 {
  constructor() {
    super(...arguments), this.renderOptions = { host: this }, this._$Do = void 0;
  }
  createRenderRoot() {
    var e3, t3;
    let i3 = super.createRenderRoot();
    return (e3 = (t3 = this.renderOptions).renderBefore) !== null && e3 !== void 0 || (t3.renderBefore = i3.firstChild), i3;
  }
  update(e3) {
    let t3 = this.render();
    this.hasUpdated || (this.renderOptions.isConnected = this.isConnected), super.update(e3), this._$Do = st3(t3, this.renderRoot, this.renderOptions);
  }
  connectedCallback() {
    var e3;
    super.connectedCallback(), (e3 = this._$Do) === null || e3 === void 0 || e3.setConnected(true);
  }
  disconnectedCallback() {
    var e3;
    super.disconnectedCallback(), (e3 = this._$Do) === null || e3 === void 0 || e3.setConnected(false);
  }
  render() {
    return w3;
  }
};
n.finalized = true, n._$litElement$ = true, (r2 = globalThis.litElementHydrateSupport) === null || r2 === void 0 || r2.call(globalThis, { LitElement: n });
var l4 = globalThis.litElementPolyfillSupport;
l4?.({ LitElement: n });
((s = globalThis.litElementVersions) !== null && s !== void 0 ? s : globalThis.litElementVersions = []).push("3.3.2");

// https://esm.sh/v124/@lit/reactive-element@1.6.2/denonext/decorators/custom-element.js
var c5 = (s2) => (t3) => typeof t3 == "function" ? ((n4, e3) => (customElements.define(n4, e3), e3))(s2, t3) : ((n4, e3) => {
  let { kind: m10, elements: o3 } = e3;
  return { kind: m10, elements: o3, finisher(i3) {
    customElements.define(n4, i3);
  } };
})(s2, t3);

// https://esm.sh/v124/@lit/reactive-element@1.6.2/denonext/decorators/property.js
var t = (e3, i3) => i3.kind === "method" && i3.descriptor && !("value" in i3.descriptor) ? { ...i3, finisher(r3) {
  r3.createProperty(i3.key, e3);
} } : { kind: "field", key: Symbol(), placement: "own", descriptor: {}, originalKey: i3.key, initializer() {
  typeof i3.initializer == "function" && (this[i3.key] = i3.initializer.call(this));
}, finisher(r3) {
  r3.createProperty(i3.key, e3);
} };
var n2 = (e3, i3, r3) => {
  i3.constructor.createProperty(r3, e3);
};
function o(e3) {
  return (i3, r3) => r3 !== void 0 ? n2(e3, i3, r3) : t(e3, i3);
}

// https://esm.sh/v124/@lit/reactive-element@1.6.2/denonext/decorators/state.js
var o2 = (t3, r3) => r3.kind === "method" && r3.descriptor && !("value" in r3.descriptor) ? { ...r3, finisher(i3) {
  i3.createProperty(r3.key, t3);
} } : { kind: "field", key: Symbol(), placement: "own", descriptor: {}, originalKey: r3.key, initializer() {
  typeof r3.initializer == "function" && (this[r3.key] = r3.initializer.call(this));
}, finisher(i3) {
  i3.createProperty(r3.key, t3);
} };
var n3 = (t3, r3, i3) => {
  r3.constructor.createProperty(i3, t3);
};
function e2(t3) {
  return (r3, i3) => i3 !== void 0 ? n3(t3, r3, i3) : o2(t3, r3);
}
function c6(t3) {
  return e2({ ...t3, state: true });
}

// https://esm.sh/v124/@lit/reactive-element@1.6.2/denonext/decorators/query.js
var d4 = ({ finisher: o3, descriptor: i3 }) => (t3, n4) => {
  var r3;
  if (n4 === void 0) {
    let e3 = (r3 = t3.originalKey) !== null && r3 !== void 0 ? r3 : t3.key, l5 = i3 != null ? { kind: "method", placement: "prototype", key: e3, descriptor: i3(t3.key) } : { ...t3, key: e3 };
    return o3 != null && (l5.finisher = function(c8) {
      o3(c8, e3);
    }), l5;
  }
  {
    let e3 = t3.constructor;
    i3 !== void 0 && Object.defineProperty(t3, n4, i3(n4)), o3?.(e3, n4);
  }
};
function y7(o3, i3) {
  return d4({ descriptor: (t3) => {
    let n4 = { get() {
      var r3, e3;
      return (e3 = (r3 = this.renderRoot) === null || r3 === void 0 ? void 0 : r3.querySelector(o3)) !== null && e3 !== void 0 ? e3 : null;
    }, enumerable: true, configurable: true };
    if (i3) {
      let r3 = typeof t3 == "symbol" ? Symbol() : "__" + t3;
      n4.get = function() {
        var e3, l5;
        return this[r3] === void 0 && (this[r3] = (l5 = (e3 = this.renderRoot) === null || e3 === void 0 ? void 0 : e3.querySelector(o3)) !== null && l5 !== void 0 ? l5 : null), this[r3];
      };
    }
    return n4;
  } });
}

// https://esm.sh/v124/@lit/reactive-element@1.6.2/denonext/decorators/query-assigned-elements.js
var d5;
var p6 = ((d5 = window.HTMLSlotElement) === null || d5 === void 0 ? void 0 : d5.prototype.assignedElements) != null ? (e3, t3) => e3.assignedElements(t3) : (e3, t3) => e3.assignedNodes(t3).filter((o3) => o3.nodeType === Node.ELEMENT_NODE);

// https://esm.sh/v124/@lit/reactive-element@1.6.2/denonext/decorators/query-assigned-nodes.js
var c7;
var y8 = ((c7 = window.HTMLSlotElement) === null || c7 === void 0 ? void 0 : c7.prototype.assignedElements) != null ? (e3, t3) => e3.assignedElements(t3) : (e3, t3) => e3.assignedNodes(t3).filter((o3) => o3.nodeType === Node.ELEMENT_NODE);

// https://esm.sh/v124/lit-html@2.7.4/denonext/directives/when.js
function t2(e3, o3, n4) {
  return e3 ? o3() : n4?.();
}

// src/emulator/controller/haptics.controller.ts
var HapticsController = class extends BaseController {
  constructor() {
    super(...arguments);
    this._init = (async () => {
      const ipc = await createMockModuleServerIpc("haptics.sys.dweb");
      const query_state = Ct.object({
        type: zq.string().optional(),
        duration: zq.number().optional()
      });
      ipc.onFetch((event) => {
        const { pathname, searchParams } = event;
        const state = zq.parseQuery(searchParams, query_state);
        this.hapticsMock(JSON.stringify({ pathname, state }));
        return Response.json(true);
      }).forbidden().cors();
    })();
  }
  hapticsMock(text) {
    console.log("hapticsMock", text);
    this.emitUpdate();
  }
};

// src/emulator/controller/navigation-bar.controller.ts
var NavigationBarController = class extends BaseController {
  constructor() {
    super(...arguments);
    this._init = (async () => {
      const ipc = await createMockModuleServerIpc(
        "navigation-bar.nativeui.browser.dweb"
      );
      const query_state = Ct.object({
        color: zq.transform((color) => colorToHex(JSON.parse(color))).optional(),
        style: Ct.enum(["DARK", "LIGHT", "DEFAULT"]).optional(),
        overlay: zq.boolean().optional(),
        visible: zq.boolean().optional()
      });
      ipc.onFetch(async (event) => {
        const { pathname, searchParams } = event;
        if (pathname.endsWith("/getState")) {
          const state = await this.navigationBarGetState();
          return Response.json(state);
        }
        if (pathname.endsWith("/setState")) {
          const states = parseQuery(searchParams, query_state);
          this.navigationBarSetState(states);
          return Response.json(true);
        }
        if (pathname.endsWith("/startObserve")) {
          this.observer.startObserve(ipc);
          return Response.json(true);
        }
        if (pathname.endsWith("/stopObserve")) {
          this.observer.startObserve(ipc);
          return Response.json("");
        }
      }).forbidden().cors();
    })();
    this.observer = new StateObservable(() => {
      return JSON.stringify(this.state);
    });
    this.state = {
      color: "#FFFFFFFF",
      style: "DEFAULT",
      insets: {
        top: 0,
        right: 0,
        bottom: 26,
        left: 0
      },
      overlay: false,
      visible: true
    };
  }
  navigationBarSetState(state) {
    this.state = {
      ...this.state,
      /// 这边这样做的目的是移除undefined值
      ...JSON.parse(JSON.stringify(state))
    };
    this.emitUpdate();
  }
  navigationBarSetStyle(style) {
    this.state = {
      ...this.state,
      style
    };
    this.emitUpdate();
  }
  navigationBarSetBackground(color) {
    this.state = {
      ...this.state,
      color
    };
    this.emitUpdate();
  }
  navigationBarSetOverlay(overlay) {
    this.state = {
      ...this.state,
      overlay
    };
    this.emitUpdate();
  }
  navigationBarSetVisible(visible) {
    this.state = {
      ...this.state,
      visible
    };
    this.emitUpdate();
  }
  async navigationBarGetState() {
    return {
      ...this.state,
      color: hexaToRGBA(this.state.color)
    };
  }
};

// src/emulator/controller/torch.controller.ts
var TorchController = class extends BaseController {
  constructor() {
    super(...arguments);
    this._init = (async () => {
      const ipc = await createMockModuleServerIpc("torch.nativeui.browser.dweb");
      ipc.onFetch(async (event) => {
        const { pathname } = event;
        if (pathname === "/toggleTorch") {
          this.torchToggleTorch();
          return Response.json(true);
        }
        if (pathname === "/state") {
          return Response.json(this.state.isOpen);
        }
      }).forbidden().cors();
    })();
    this.state = { isOpen: false };
  }
  torchToggleTorch() {
    this.state = {
      isOpen: !this.state.isOpen
    };
    this.emitUpdate();
    return this.state.isOpen;
  }
};

// src/emulator/controller/virtual-keyboard.controller.ts
var VirtualKeyboardController = class extends BaseController {
  constructor() {
    super(...arguments);
    this._init = (async () => {
      const ipc = await createMockModuleServerIpc(
        "virtual-keyboard.nativeui.browser.dweb"
      );
      const query_state = Ct.object({
        overlay: zq.boolean().optional(),
        visible: zq.boolean().optional()
      });
      ipc.onFetch(async (event) => {
        const { pathname, searchParams } = event;
        if (pathname.endsWith("/getState")) {
          return Response.json(this.state);
        }
        if (pathname.endsWith("/setState")) {
          const states = parseQuery(searchParams, query_state);
          this.virtualKeyboardSeVisiable(states.visible);
          this.virtualKeyboardSetOverlay(states.overlay);
          return Response.json(true);
        }
        if (pathname.endsWith("/startObserve")) {
          this.observer.startObserve(ipc);
          return Response.json(true);
        }
        if (pathname.endsWith("/stopObserve")) {
          this.observer.startObserve(ipc);
          return Response.json("");
        }
      }).forbidden().cors();
    })();
    this.observer = new StateObservable(() => {
      return JSON.stringify(this.state);
    });
    // 控制显示隐藏
    this.isShowVirtualKeyboard = false;
    this.state = {
      insets: {
        top: 0,
        right: 0,
        bottom: 0,
        left: 0
      },
      overlay: false,
      visible: false
    };
  }
  virtualKeyboardSetOverlay(overlay = true) {
    this.state = {
      ...this.state,
      overlay
    };
    this.emitUpdate();
  }
  virtualKeyboardSeVisiable(visible = true) {
    this.state = {
      ...this.state,
      visible
    };
    this.emitUpdate();
  }
  virtualKeyboardFirstUpdated() {
    this.state = {
      ...this.state,
      visible: true
    };
    this.emitUpdate();
  }
  virtualKeyboardHideCompleted() {
    this.isShowVirtualKeyboard = false;
    console.error(`virtualKeybark \u9690\u85CF\u5B8C\u6210\u4E86 \u4F46\u662F\u8FD8\u6CA1\u6709\u5904\u7406`);
  }
  virtualKeyboardShowCompleted() {
    console.error("virutalKeyboard \u663E\u793A\u5B8C\u6210\u4E86 \u4F46\u662F\u8FD8\u6CA1\u6709\u5904\u7406");
  }
};

// https://esm.sh/v124/lit-html@2.7.4/denonext/static.js
var E6;
var T3 = window;
var m6 = T3.trustedTypes;
var D5 = m6 ? m6.createPolicy("lit-html", { createHTML: (r3) => r3 }) : void 0;
var B5 = "$lit$";
var _7 = `lit$${(Math.random() + "").slice(9)}$`;
var q3 = "?" + _7;
var et5 = `<${q3}>`;
var g5 = document;
var M4 = () => g5.createComment("");
var b5 = (r3) => r3 === null || typeof r3 != "object" && typeof r3 != "function";
var G6 = Array.isArray;
var it3 = (r3) => G6(r3) || typeof r3?.[Symbol.iterator] == "function";
var I3 = `[ 	
\f\r]`;
var N4 = /<(?:(!--|\/[^a-zA-Z])|(\/?[a-zA-Z][^>\s]*)|(\/?$))/g;
var V3 = /-->/g;
var W4 = />/g;
var v8 = RegExp(`>|${I3}(?:([^\\s"'>=/]+)(${I3}*=${I3}*(?:[^ 	
\f\r"'\`<>=]|("|')|))|$)`, "g");
var O4 = /'/g;
var Z3 = /"/g;
var J6 = /^(?:script|style|textarea|title)$/i;
var K5 = (r3) => (t3, ...e3) => ({ _$litType$: r3, strings: t3, values: e3 });
var Q4 = K5(1);
var X4 = K5(2);
var w5 = Symbol.for("lit-noChange");
var u5 = Symbol.for("lit-nothing");
var z4 = /* @__PURE__ */ new WeakMap();
var p7 = g5.createTreeWalker(g5, 129, null, false);
var st4 = (r3, t3) => {
  let e3 = r3.length - 1, i3 = [], s2, o3 = t3 === 2 ? "<svg>" : "", n4 = N4;
  for (let h4 = 0; h4 < e3; h4++) {
    let l5 = r3[h4], c8, a4, $5 = -1, A5 = 0;
    for (; A5 < l5.length && (n4.lastIndex = A5, a4 = n4.exec(l5), a4 !== null); )
      A5 = n4.lastIndex, n4 === N4 ? a4[1] === "!--" ? n4 = V3 : a4[1] !== void 0 ? n4 = W4 : a4[2] !== void 0 ? (J6.test(a4[2]) && (s2 = RegExp("</" + a4[2], "g")), n4 = v8) : a4[3] !== void 0 && (n4 = v8) : n4 === v8 ? a4[0] === ">" ? (n4 = s2 ?? N4, $5 = -1) : a4[1] === void 0 ? $5 = -2 : ($5 = n4.lastIndex - a4[2].length, c8 = a4[1], n4 = a4[3] === void 0 ? v8 : a4[3] === '"' ? Z3 : O4) : n4 === Z3 || n4 === O4 ? n4 = v8 : n4 === V3 || n4 === W4 ? n4 = N4 : (n4 = v8, s2 = void 0);
    let S9 = n4 === v8 && r3[h4 + 1].startsWith("/>") ? " " : "";
    o3 += n4 === N4 ? l5 + et5 : $5 >= 0 ? (i3.push(c8), l5.slice(0, $5) + B5 + l5.slice($5) + _7 + S9) : l5 + _7 + ($5 === -2 ? (i3.push(void 0), h4) : S9);
  }
  let d6 = o3 + (r3[e3] || "<?>") + (t3 === 2 ? "</svg>" : "");
  if (!Array.isArray(r3) || !r3.hasOwnProperty("raw"))
    throw Error("invalid template strings array");
  return [D5 !== void 0 ? D5.createHTML(d6) : d6, i3];
};
var f4 = class {
  constructor({ strings: t3, _$litType$: e3 }, i3) {
    let s2;
    this.parts = [];
    let o3 = 0, n4 = 0, d6 = t3.length - 1, h4 = this.parts, [l5, c8] = st4(t3, e3);
    if (this.el = f4.createElement(l5, i3), p7.currentNode = this.el.content, e3 === 2) {
      let a4 = this.el.content, $5 = a4.firstChild;
      $5.remove(), a4.append(...$5.childNodes);
    }
    for (; (s2 = p7.nextNode()) !== null && h4.length < d6; ) {
      if (s2.nodeType === 1) {
        if (s2.hasAttributes()) {
          let a4 = [];
          for (let $5 of s2.getAttributeNames())
            if ($5.endsWith(B5) || $5.startsWith(_7)) {
              let A5 = c8[n4++];
              if (a4.push($5), A5 !== void 0) {
                let S9 = s2.getAttribute(A5.toLowerCase() + B5).split(_7), C11 = /([.?@])?(.*)/.exec(A5);
                h4.push({ type: 1, index: o3, name: C11[2], strings: S9, ctor: C11[1] === "." ? P4 : C11[1] === "?" ? j6 : C11[1] === "@" ? L4 : x4 });
              } else
                h4.push({ type: 6, index: o3 });
            }
          for (let $5 of a4)
            s2.removeAttribute($5);
        }
        if (J6.test(s2.tagName)) {
          let a4 = s2.textContent.split(_7), $5 = a4.length - 1;
          if ($5 > 0) {
            s2.textContent = m6 ? m6.emptyScript : "";
            for (let A5 = 0; A5 < $5; A5++)
              s2.append(a4[A5], M4()), p7.nextNode(), h4.push({ type: 2, index: ++o3 });
            s2.append(a4[$5], M4());
          }
        }
      } else if (s2.nodeType === 8)
        if (s2.data === q3)
          h4.push({ type: 2, index: o3 });
        else {
          let a4 = -1;
          for (; (a4 = s2.data.indexOf(_7, a4 + 1)) !== -1; )
            h4.push({ type: 7, index: o3 }), a4 += _7.length - 1;
        }
      o3++;
    }
  }
  static createElement(t3, e3) {
    let i3 = g5.createElement("template");
    return i3.innerHTML = t3, i3;
  }
};
function y9(r3, t3, e3 = r3, i3) {
  var s2, o3, n4, d6;
  if (t3 === w5)
    return t3;
  let h4 = i3 !== void 0 ? (s2 = e3._$Co) === null || s2 === void 0 ? void 0 : s2[i3] : e3._$Cl, l5 = b5(t3) ? void 0 : t3._$litDirective$;
  return h4?.constructor !== l5 && ((o3 = h4?._$AO) === null || o3 === void 0 || o3.call(h4, false), l5 === void 0 ? h4 = void 0 : (h4 = new l5(r3), h4._$AT(r3, e3, i3)), i3 !== void 0 ? ((n4 = (d6 = e3)._$Co) !== null && n4 !== void 0 ? n4 : d6._$Co = [])[i3] = h4 : e3._$Cl = h4), h4 !== void 0 && (t3 = y9(r3, h4._$AS(r3, t3.values), h4, i3)), t3;
}
var U5 = class {
  constructor(t3, e3) {
    this._$AV = [], this._$AN = void 0, this._$AD = t3, this._$AM = e3;
  }
  get parentNode() {
    return this._$AM.parentNode;
  }
  get _$AU() {
    return this._$AM._$AU;
  }
  u(t3) {
    var e3;
    let { el: { content: i3 }, parts: s2 } = this._$AD, o3 = ((e3 = t3?.creationScope) !== null && e3 !== void 0 ? e3 : g5).importNode(i3, true);
    p7.currentNode = o3;
    let n4 = p7.nextNode(), d6 = 0, h4 = 0, l5 = s2[0];
    for (; l5 !== void 0; ) {
      if (d6 === l5.index) {
        let c8;
        l5.type === 2 ? c8 = new H6(n4, n4.nextSibling, this, t3) : l5.type === 1 ? c8 = new l5.ctor(n4, l5.name, l5.strings, this, t3) : l5.type === 6 && (c8 = new R4(n4, this, t3)), this._$AV.push(c8), l5 = s2[++h4];
      }
      d6 !== l5?.index && (n4 = p7.nextNode(), d6++);
    }
    return p7.currentNode = g5, o3;
  }
  v(t3) {
    let e3 = 0;
    for (let i3 of this._$AV)
      i3 !== void 0 && (i3.strings !== void 0 ? (i3._$AI(t3, i3, e3), e3 += i3.strings.length - 2) : i3._$AI(t3[e3])), e3++;
  }
};
var H6 = class {
  constructor(t3, e3, i3, s2) {
    var o3;
    this.type = 2, this._$AH = u5, this._$AN = void 0, this._$AA = t3, this._$AB = e3, this._$AM = i3, this.options = s2, this._$Cp = (o3 = s2?.isConnected) === null || o3 === void 0 || o3;
  }
  get _$AU() {
    var t3, e3;
    return (e3 = (t3 = this._$AM) === null || t3 === void 0 ? void 0 : t3._$AU) !== null && e3 !== void 0 ? e3 : this._$Cp;
  }
  get parentNode() {
    let t3 = this._$AA.parentNode, e3 = this._$AM;
    return e3 !== void 0 && t3?.nodeType === 11 && (t3 = e3.parentNode), t3;
  }
  get startNode() {
    return this._$AA;
  }
  get endNode() {
    return this._$AB;
  }
  _$AI(t3, e3 = this) {
    t3 = y9(this, t3, e3), b5(t3) ? t3 === u5 || t3 == null || t3 === "" ? (this._$AH !== u5 && this._$AR(), this._$AH = u5) : t3 !== this._$AH && t3 !== w5 && this._(t3) : t3._$litType$ !== void 0 ? this.g(t3) : t3.nodeType !== void 0 ? this.$(t3) : it3(t3) ? this.T(t3) : this._(t3);
  }
  k(t3) {
    return this._$AA.parentNode.insertBefore(t3, this._$AB);
  }
  $(t3) {
    this._$AH !== t3 && (this._$AR(), this._$AH = this.k(t3));
  }
  _(t3) {
    this._$AH !== u5 && b5(this._$AH) ? this._$AA.nextSibling.data = t3 : this.$(g5.createTextNode(t3)), this._$AH = t3;
  }
  g(t3) {
    var e3;
    let { values: i3, _$litType$: s2 } = t3, o3 = typeof s2 == "number" ? this._$AC(t3) : (s2.el === void 0 && (s2.el = f4.createElement(s2.h, this.options)), s2);
    if (((e3 = this._$AH) === null || e3 === void 0 ? void 0 : e3._$AD) === o3)
      this._$AH.v(i3);
    else {
      let n4 = new U5(o3, this), d6 = n4.u(this.options);
      n4.v(i3), this.$(d6), this._$AH = n4;
    }
  }
  _$AC(t3) {
    let e3 = z4.get(t3.strings);
    return e3 === void 0 && z4.set(t3.strings, e3 = new f4(t3)), e3;
  }
  T(t3) {
    G6(this._$AH) || (this._$AH = [], this._$AR());
    let e3 = this._$AH, i3, s2 = 0;
    for (let o3 of t3)
      s2 === e3.length ? e3.push(i3 = new H6(this.k(M4()), this.k(M4()), this, this.options)) : i3 = e3[s2], i3._$AI(o3), s2++;
    s2 < e3.length && (this._$AR(i3 && i3._$AB.nextSibling, s2), e3.length = s2);
  }
  _$AR(t3 = this._$AA.nextSibling, e3) {
    var i3;
    for ((i3 = this._$AP) === null || i3 === void 0 || i3.call(this, false, true, e3); t3 && t3 !== this._$AB; ) {
      let s2 = t3.nextSibling;
      t3.remove(), t3 = s2;
    }
  }
  setConnected(t3) {
    var e3;
    this._$AM === void 0 && (this._$Cp = t3, (e3 = this._$AP) === null || e3 === void 0 || e3.call(this, t3));
  }
};
var x4 = class {
  constructor(t3, e3, i3, s2, o3) {
    this.type = 1, this._$AH = u5, this._$AN = void 0, this.element = t3, this.name = e3, this._$AM = s2, this.options = o3, i3.length > 2 || i3[0] !== "" || i3[1] !== "" ? (this._$AH = Array(i3.length - 1).fill(new String()), this.strings = i3) : this._$AH = u5;
  }
  get tagName() {
    return this.element.tagName;
  }
  get _$AU() {
    return this._$AM._$AU;
  }
  _$AI(t3, e3 = this, i3, s2) {
    let o3 = this.strings, n4 = false;
    if (o3 === void 0)
      t3 = y9(this, t3, e3, 0), n4 = !b5(t3) || t3 !== this._$AH && t3 !== w5, n4 && (this._$AH = t3);
    else {
      let d6 = t3, h4, l5;
      for (t3 = o3[0], h4 = 0; h4 < o3.length - 1; h4++)
        l5 = y9(this, d6[i3 + h4], e3, h4), l5 === w5 && (l5 = this._$AH[h4]), n4 || (n4 = !b5(l5) || l5 !== this._$AH[h4]), l5 === u5 ? t3 = u5 : t3 !== u5 && (t3 += (l5 ?? "") + o3[h4 + 1]), this._$AH[h4] = l5;
    }
    n4 && !s2 && this.j(t3);
  }
  j(t3) {
    t3 === u5 ? this.element.removeAttribute(this.name) : this.element.setAttribute(this.name, t3 ?? "");
  }
};
var P4 = class extends x4 {
  constructor() {
    super(...arguments), this.type = 3;
  }
  j(t3) {
    this.element[this.name] = t3 === u5 ? void 0 : t3;
  }
};
var nt4 = m6 ? m6.emptyScript : "";
var j6 = class extends x4 {
  constructor() {
    super(...arguments), this.type = 4;
  }
  j(t3) {
    t3 && t3 !== u5 ? this.element.setAttribute(this.name, nt4) : this.element.removeAttribute(this.name);
  }
};
var L4 = class extends x4 {
  constructor(t3, e3, i3, s2, o3) {
    super(t3, e3, i3, s2, o3), this.type = 5;
  }
  _$AI(t3, e3 = this) {
    var i3;
    if ((t3 = (i3 = y9(this, t3, e3, 0)) !== null && i3 !== void 0 ? i3 : u5) === w5)
      return;
    let s2 = this._$AH, o3 = t3 === u5 && s2 !== u5 || t3.capture !== s2.capture || t3.once !== s2.once || t3.passive !== s2.passive, n4 = t3 !== u5 && (s2 === u5 || o3);
    o3 && this.element.removeEventListener(this.name, this, s2), n4 && this.element.addEventListener(this.name, this, t3), this._$AH = t3;
  }
  handleEvent(t3) {
    var e3, i3;
    typeof this._$AH == "function" ? this._$AH.call((i3 = (e3 = this.options) === null || e3 === void 0 ? void 0 : e3.host) !== null && i3 !== void 0 ? i3 : this.element, t3) : this._$AH.handleEvent(t3);
  }
};
var R4 = class {
  constructor(t3, e3, i3) {
    this.element = t3, this.type = 6, this._$AN = void 0, this._$AM = e3, this.options = i3;
  }
  get _$AU() {
    return this._$AM._$AU;
  }
  _$AI(t3) {
    y9(this, t3);
  }
};
var F4 = T3.litHtmlPolyfillSupport;
F4?.(f4, H6), ((E6 = T3.litHtmlVersions) !== null && E6 !== void 0 ? E6 : T3.litHtmlVersions = []).push("2.7.4");
var k6 = Symbol.for("");
var ot4 = (r3) => {
  if (r3?.r === k6)
    return r3?._$litStatic$;
};
var Y4 = /* @__PURE__ */ new Map();
var tt4 = (r3) => (t3, ...e3) => {
  let i3 = e3.length, s2, o3, n4 = [], d6 = [], h4, l5 = 0, c8 = false;
  for (; l5 < i3; ) {
    for (h4 = t3[l5]; l5 < i3 && (o3 = e3[l5], (s2 = ot4(o3)) !== void 0); )
      h4 += s2 + t3[++l5], c8 = true;
    l5 !== i3 && d6.push(o3), n4.push(h4), l5++;
  }
  if (l5 === i3 && n4.push(t3[i3]), c8) {
    let a4 = n4.join("$$lit$$");
    (t3 = Y4.get(a4)) === void 0 && (n4.raw = n4, Y4.set(a4, t3 = n4)), e3 = d6;
  }
  return r3(t3, ...e3);
};
var $t2 = tt4(Q4);
var dt3 = tt4(X4);

// src/emulator/multi-webview-comp-biometrics.html.ts
var TAG = "multi-webview-comp-biometrics";
var MultiWebviewCompBiometrics = class extends n {
  pass() {
    console.error("\u70B9\u51FB\u4E86 pass \u4F46\u662F\u8FD8\u6CA1\u6709\u5904\u7406");
    this.dispatchEvent(new Event("pass"));
    this.shadowRoot?.host.remove();
  }
  noPass() {
    console.error("\u70B9\u51FB\u4E86 no pass \u4F46\u662F\u8FD8\u6CA1\u6709\u5904\u7406");
    this.dispatchEvent(new Event("no-pass"));
    this.shadowRoot?.host.remove();
  }
  render() {
    return $t2`
      <div class="panel">
        <p>点击按钮 模拟 返回结果</p>
        <div class="btn_group">
          <button class="pass" @click=${this.pass}>识别通过</button>
          <button class="no_pass" @click=${this.noPass}>识别没通过</button>
        </div>
      </div>
    `;
  }
};
MultiWebviewCompBiometrics.styles = createAllCSS();
MultiWebviewCompBiometrics = __decorateClass([
  c5(TAG)
], MultiWebviewCompBiometrics);
function createAllCSS() {
  return [
    C7`
      :host {
        position: absolute;
        z-index: 1;
        left: 0px;
        top: 0px;
        box-sizing: border-box;
        padding-bottom: 100px;
        width: 100%;
        height: 100%;
        display: flex;
        justify-content: center;
        align-items: center;
        background: #00000033;
      }

      .panel {
        padding: 12px 20px;
        width: 80%;
        border-radius: 12px;
        background: #ffffffff;
      }

      .btn_group {
        width: 100%;
        display: flex;
        justify-content: space-between;
      }

      .pass,
      .no_pass {
        padding: 8px 20px;
        border-radius: 5px;
        border: none;
      }

      .pass {
        color: #ffffffff;
        background: #1677ff;
      }

      .no_pass {
        background: #d9d9d9;
      }
    `
  ];
}

// src/emulator/multi-webview-comp-haptics.html.ts
var TAG2 = "multi-webview-comp-haptics";
var MultiWebviewCompHaptics = class extends n {
  constructor() {
    super(...arguments);
    this.text = "";
  }
  firstUpdated() {
    this.shadowRoot?.host.addEventListener("click", this.cancel);
  }
  cancel() {
    this.shadowRoot?.host.remove();
  }
  render() {
    return tt3`
      <div class="panel">
        <p>模拟: ${this.text}</p>
        <div class="btn_group">
          <button class="btn" @click=${this.cancel}>取消</button>
        </div>
      </div>
    `;
  }
};
MultiWebviewCompHaptics.styles = createAllCSS2();
__decorateClass([
  o({ type: String })
], MultiWebviewCompHaptics.prototype, "text", 2);
MultiWebviewCompHaptics = __decorateClass([
  c5(TAG2)
], MultiWebviewCompHaptics);
function createAllCSS2() {
  return [
    C7`
      :host {
        position: absolute;
        z-index: 1;
        left: 0px;
        top: 0px;
        box-sizing: border-box;
        padding-bottom: 100px;
        width: 100%;
        height: 100%;
        display: flex;
        justify-content: center;
        align-items: center;
        background: #00000033;
        cursor: pointer;
      }

      .panel {
        padding: 12px 20px;
        width: 80%;
        border-radius: 12px;
        background: #ffffffff;
      }

      .btn_group {
        width: 100%;
        display: flex;
        justify-content: flex-end;
      }

      .btn {
        padding: 8px 20px;
        border-radius: 5px;
        border: none;
        color: #ffffffff;
        background: #1677ff;
      }
    `
  ];
}

// src/emulator/multi-webview-comp-mobile-shell.html.ts
var TAG3 = "multi-webview-comp-mobile-shell";
var MultiWebViewCompMobileShell = class extends n {
  /**
   *
   * @param message
   * @param duration
   * @param position
   */
  toastShow(message, duration, position) {
    const multiWebviewCompToast = document.createElement(
      "multi-webview-comp-toast"
    );
    [
      ["_message", message],
      ["_duration", duration],
      ["_position", position]
    ].forEach(([key, value]) => {
      multiWebviewCompToast.setAttribute(key, value);
    });
    this.appContentContainer?.append(multiWebviewCompToast);
  }
  shareShare(options) {
    const el = document.createElement("multi-webview-comp-share");
    const ui8 = options.body;
    const contentType = options.bodyType;
    const sparator = new TextEncoder().encode(contentType.split("boundary=")[1]).join();
    const file = this.getFileFromUin8Array(ui8, sparator, 1);
    let src = "";
    let filename = "";
    if (file !== void 0) {
      if (file.name.endsWith(".gif") || file.name.endsWith(".png") || file.name.endsWith(".jpg") || file.name.endsWith(".bmp") || file.name.endsWith(".svg") || file.name.endsWith(".webp")) {
        src = URL.createObjectURL(file);
      } else {
        filename = file.name;
      }
    }
    [
      ["_title", options.title],
      ["_text", options.text],
      ["_link", options.link],
      ["_src", src],
      ["_filename", filename]
    ].forEach(([key, value]) => el.setAttribute(key, value));
    this.appContentContainer?.appendChild(el);
  }
  /**
   *  formData 原始 Uint8Array 数据
   *  separatorStr 在请求的headers里面的 multipart/form-data; boundary=----WebKitFormBoundarySLm2pLgOKCimWFjG boundary=后面的内容
   *  index file 数据保存在原始 formData 被分割后的位置
   *  这个位置会根据 formData参数的不同而不同
   */
  getFileFromUin8Array(rawUi8, separatorStr, index) {
    const ui8Str = rawUi8.join();
    const dubleLineBreak = new TextEncoder().encode("\r\n\r\n").join();
    const resultNoPeratorStrArr = ui8Str.split(separatorStr);
    let contentType = "";
    let filename = "";
    let file = void 0;
    const str = resultNoPeratorStrArr[index];
    const arr = str.slice(7, -7).split(dubleLineBreak);
    arr.forEach((str2, index2) => {
      if (str2.length === 0)
        return;
      if (index2 === 0) {
        const des = new TextDecoder().decode(
          new Uint8Array(
            str2.slice(0, -1).split(",")
          )
        );
        des.split("\r\n").forEach((str3, index3) => {
          if (index3 === 0) {
            filename = str3.split("filename=")[1].slice(1, -1);
          } else if (index3 === 1) {
            contentType = str3.split(":")[1];
          }
        });
      } else {
        const s2 = str2.slice(1, -6);
        const a4 = new Uint8Array(s2.split(","));
        const blob = new Blob([a4], { type: contentType });
        file = new File([blob], filename);
      }
    });
    return file;
  }
  render() {
    return tt3`
      <div class="shell">
        <div class="shell_container">
          <slot name="status-bar"></slot>
          <div class="app_content_container">
            <slot name="shell-content"> ... 桌面 ... </slot>
          </div>
          <slot name="bottom-bar"></slot>
        </div>
      </div>
    `;
  }
};
MultiWebViewCompMobileShell.styles = createAllCSS3();
__decorateClass([
  y7(".app_content_container")
], MultiWebViewCompMobileShell.prototype, "appContentContainer", 2);
MultiWebViewCompMobileShell = __decorateClass([
  c5(TAG3)
], MultiWebViewCompMobileShell);
function createAllCSS3() {
  return [
    C7`
      :host {
        display: block;
        height: 100%;
        overflow: hidden;
        -webkit-app-region: resize;
      }
      .shell {
        padding: 0.8em 0.8em 0.8em 0.8em;
        border-radius: 2.6em;
        overflow: hidden;
        background: #000;
        height: 100%;
        width: 100%;
        box-sizing: border-box;
        -webkit-app-region: drag;
      }
      .shell_container {
        -webkit-app-region: no-drag;
        position: relative;
        display: flex;
        background: #fff;
        flex-direction: column;
        box-sizing: content-box;
        width: 100%;
        height: 100%;
        overflow: hidden;
        border-radius: 2em;
      }
      @media (prefers-color-scheme: dark) {
        .shell_container {
          background: #333;
        }
      }

      .app_content_container {
        position: relative;
        box-sizing: border-box;
        width: 100%;
        height: 100%;
      }
    `
  ];
}

// https://esm.sh/v124/lit-html@2.7.4/denonext/directives/style-map.js
var P5;
var w6 = window;
var y10 = w6.trustedTypes;
var j7 = y10 ? y10.createPolicy("lit-html", { createHTML: (o3) => o3 }) : void 0;
var B6 = "$lit$";
var _8 = `lit$${(Math.random() + "").slice(9)}$`;
var Y5 = "?" + _8;
var tt5 = `<${Y5}>`;
var g6 = document;
var M5 = () => g6.createComment("");
var C8 = (o3) => o3 === null || typeof o3 != "object" && typeof o3 != "function";
var q4 = Array.isArray;
var et6 = (o3) => q4(o3) || typeof o3?.[Symbol.iterator] == "function";
var S6 = `[ 	
\f\r]`;
var N5 = /<(?:(!--|\/[^a-zA-Z])|(\/?[a-zA-Z][^>\s]*)|(\/?$))/g;
var k7 = /-->/g;
var W5 = />/g;
var v9 = RegExp(`>|${S6}(?:([^\\s"'>=/]+)(${S6}*=${S6}*(?:[^ 	
\f\r"'\`<>=]|("|')|))|$)`, "g");
var V4 = /'/g;
var Z4 = /"/g;
var G7 = /^(?:script|style|textarea|title)$/i;
var J7 = (o3) => (t3, ...e3) => ({ _$litType$: o3, strings: t3, values: e3 });
var ot5 = J7(1);
var rt4 = J7(2);
var m7 = Symbol.for("lit-noChange");
var u6 = Symbol.for("lit-nothing");
var z5 = /* @__PURE__ */ new WeakMap();
var p8 = g6.createTreeWalker(g6, 129, null, false);
var it4 = (o3, t3) => {
  let e3 = o3.length - 1, i3 = [], s2, r3 = t3 === 2 ? "<svg>" : "", n4 = N5;
  for (let l5 = 0; l5 < e3; l5++) {
    let h4 = o3[l5], A5, a4, d6 = -1, c8 = 0;
    for (; c8 < h4.length && (n4.lastIndex = c8, a4 = n4.exec(h4), a4 !== null); )
      c8 = n4.lastIndex, n4 === N5 ? a4[1] === "!--" ? n4 = k7 : a4[1] !== void 0 ? n4 = W5 : a4[2] !== void 0 ? (G7.test(a4[2]) && (s2 = RegExp("</" + a4[2], "g")), n4 = v9) : a4[3] !== void 0 && (n4 = v9) : n4 === v9 ? a4[0] === ">" ? (n4 = s2 ?? N5, d6 = -1) : a4[1] === void 0 ? d6 = -2 : (d6 = n4.lastIndex - a4[2].length, A5 = a4[1], n4 = a4[3] === void 0 ? v9 : a4[3] === '"' ? Z4 : V4) : n4 === Z4 || n4 === V4 ? n4 = v9 : n4 === k7 || n4 === W5 ? n4 = N5 : (n4 = v9, s2 = void 0);
    let b7 = n4 === v9 && o3[l5 + 1].startsWith("/>") ? " " : "";
    r3 += n4 === N5 ? h4 + tt5 : d6 >= 0 ? (i3.push(A5), h4.slice(0, d6) + B6 + h4.slice(d6) + _8 + b7) : h4 + _8 + (d6 === -2 ? (i3.push(void 0), l5) : b7);
  }
  let $5 = r3 + (o3[e3] || "<?>") + (t3 === 2 ? "</svg>" : "");
  if (!Array.isArray(o3) || !o3.hasOwnProperty("raw"))
    throw Error("invalid template strings array");
  return [j7 !== void 0 ? j7.createHTML($5) : $5, i3];
};
var f5 = class {
  constructor({ strings: t3, _$litType$: e3 }, i3) {
    let s2;
    this.parts = [];
    let r3 = 0, n4 = 0, $5 = t3.length - 1, l5 = this.parts, [h4, A5] = it4(t3, e3);
    if (this.el = f5.createElement(h4, i3), p8.currentNode = this.el.content, e3 === 2) {
      let a4 = this.el.content, d6 = a4.firstChild;
      d6.remove(), a4.append(...d6.childNodes);
    }
    for (; (s2 = p8.nextNode()) !== null && l5.length < $5; ) {
      if (s2.nodeType === 1) {
        if (s2.hasAttributes()) {
          let a4 = [];
          for (let d6 of s2.getAttributeNames())
            if (d6.endsWith(B6) || d6.startsWith(_8)) {
              let c8 = A5[n4++];
              if (a4.push(d6), c8 !== void 0) {
                let b7 = s2.getAttribute(c8.toLowerCase() + B6).split(_8), E8 = /([.?@])?(.*)/.exec(c8);
                l5.push({ type: 1, index: r3, name: E8[2], strings: b7, ctor: E8[1] === "." ? R5 : E8[1] === "?" ? L5 : E8[1] === "@" ? D6 : T4 });
              } else
                l5.push({ type: 6, index: r3 });
            }
          for (let d6 of a4)
            s2.removeAttribute(d6);
        }
        if (G7.test(s2.tagName)) {
          let a4 = s2.textContent.split(_8), d6 = a4.length - 1;
          if (d6 > 0) {
            s2.textContent = y10 ? y10.emptyScript : "";
            for (let c8 = 0; c8 < d6; c8++)
              s2.append(a4[c8], M5()), p8.nextNode(), l5.push({ type: 2, index: ++r3 });
            s2.append(a4[d6], M5());
          }
        }
      } else if (s2.nodeType === 8)
        if (s2.data === Y5)
          l5.push({ type: 2, index: r3 });
        else {
          let a4 = -1;
          for (; (a4 = s2.data.indexOf(_8, a4 + 1)) !== -1; )
            l5.push({ type: 7, index: r3 }), a4 += _8.length - 1;
        }
      r3++;
    }
  }
  static createElement(t3, e3) {
    let i3 = g6.createElement("template");
    return i3.innerHTML = t3, i3;
  }
};
function H7(o3, t3, e3 = o3, i3) {
  var s2, r3, n4, $5;
  if (t3 === m7)
    return t3;
  let l5 = i3 !== void 0 ? (s2 = e3._$Co) === null || s2 === void 0 ? void 0 : s2[i3] : e3._$Cl, h4 = C8(t3) ? void 0 : t3._$litDirective$;
  return l5?.constructor !== h4 && ((r3 = l5?._$AO) === null || r3 === void 0 || r3.call(l5, false), h4 === void 0 ? l5 = void 0 : (l5 = new h4(o3), l5._$AT(o3, e3, i3)), i3 !== void 0 ? ((n4 = ($5 = e3)._$Co) !== null && n4 !== void 0 ? n4 : $5._$Co = [])[i3] = l5 : e3._$Cl = l5), l5 !== void 0 && (t3 = H7(o3, l5._$AS(o3, t3.values), l5, i3)), t3;
}
var U6 = class {
  constructor(t3, e3) {
    this._$AV = [], this._$AN = void 0, this._$AD = t3, this._$AM = e3;
  }
  get parentNode() {
    return this._$AM.parentNode;
  }
  get _$AU() {
    return this._$AM._$AU;
  }
  u(t3) {
    var e3;
    let { el: { content: i3 }, parts: s2 } = this._$AD, r3 = ((e3 = t3?.creationScope) !== null && e3 !== void 0 ? e3 : g6).importNode(i3, true);
    p8.currentNode = r3;
    let n4 = p8.nextNode(), $5 = 0, l5 = 0, h4 = s2[0];
    for (; h4 !== void 0; ) {
      if ($5 === h4.index) {
        let A5;
        h4.type === 2 ? A5 = new x5(n4, n4.nextSibling, this, t3) : h4.type === 1 ? A5 = new h4.ctor(n4, h4.name, h4.strings, this, t3) : h4.type === 6 && (A5 = new O5(n4, this, t3)), this._$AV.push(A5), h4 = s2[++l5];
      }
      $5 !== h4?.index && (n4 = p8.nextNode(), $5++);
    }
    return p8.currentNode = g6, r3;
  }
  v(t3) {
    let e3 = 0;
    for (let i3 of this._$AV)
      i3 !== void 0 && (i3.strings !== void 0 ? (i3._$AI(t3, i3, e3), e3 += i3.strings.length - 2) : i3._$AI(t3[e3])), e3++;
  }
};
var x5 = class {
  constructor(t3, e3, i3, s2) {
    var r3;
    this.type = 2, this._$AH = u6, this._$AN = void 0, this._$AA = t3, this._$AB = e3, this._$AM = i3, this.options = s2, this._$Cp = (r3 = s2?.isConnected) === null || r3 === void 0 || r3;
  }
  get _$AU() {
    var t3, e3;
    return (e3 = (t3 = this._$AM) === null || t3 === void 0 ? void 0 : t3._$AU) !== null && e3 !== void 0 ? e3 : this._$Cp;
  }
  get parentNode() {
    let t3 = this._$AA.parentNode, e3 = this._$AM;
    return e3 !== void 0 && t3?.nodeType === 11 && (t3 = e3.parentNode), t3;
  }
  get startNode() {
    return this._$AA;
  }
  get endNode() {
    return this._$AB;
  }
  _$AI(t3, e3 = this) {
    t3 = H7(this, t3, e3), C8(t3) ? t3 === u6 || t3 == null || t3 === "" ? (this._$AH !== u6 && this._$AR(), this._$AH = u6) : t3 !== this._$AH && t3 !== m7 && this._(t3) : t3._$litType$ !== void 0 ? this.g(t3) : t3.nodeType !== void 0 ? this.$(t3) : et6(t3) ? this.T(t3) : this._(t3);
  }
  k(t3) {
    return this._$AA.parentNode.insertBefore(t3, this._$AB);
  }
  $(t3) {
    this._$AH !== t3 && (this._$AR(), this._$AH = this.k(t3));
  }
  _(t3) {
    this._$AH !== u6 && C8(this._$AH) ? this._$AA.nextSibling.data = t3 : this.$(g6.createTextNode(t3)), this._$AH = t3;
  }
  g(t3) {
    var e3;
    let { values: i3, _$litType$: s2 } = t3, r3 = typeof s2 == "number" ? this._$AC(t3) : (s2.el === void 0 && (s2.el = f5.createElement(s2.h, this.options)), s2);
    if (((e3 = this._$AH) === null || e3 === void 0 ? void 0 : e3._$AD) === r3)
      this._$AH.v(i3);
    else {
      let n4 = new U6(r3, this), $5 = n4.u(this.options);
      n4.v(i3), this.$($5), this._$AH = n4;
    }
  }
  _$AC(t3) {
    let e3 = z5.get(t3.strings);
    return e3 === void 0 && z5.set(t3.strings, e3 = new f5(t3)), e3;
  }
  T(t3) {
    q4(this._$AH) || (this._$AH = [], this._$AR());
    let e3 = this._$AH, i3, s2 = 0;
    for (let r3 of t3)
      s2 === e3.length ? e3.push(i3 = new x5(this.k(M5()), this.k(M5()), this, this.options)) : i3 = e3[s2], i3._$AI(r3), s2++;
    s2 < e3.length && (this._$AR(i3 && i3._$AB.nextSibling, s2), e3.length = s2);
  }
  _$AR(t3 = this._$AA.nextSibling, e3) {
    var i3;
    for ((i3 = this._$AP) === null || i3 === void 0 || i3.call(this, false, true, e3); t3 && t3 !== this._$AB; ) {
      let s2 = t3.nextSibling;
      t3.remove(), t3 = s2;
    }
  }
  setConnected(t3) {
    var e3;
    this._$AM === void 0 && (this._$Cp = t3, (e3 = this._$AP) === null || e3 === void 0 || e3.call(this, t3));
  }
};
var T4 = class {
  constructor(t3, e3, i3, s2, r3) {
    this.type = 1, this._$AH = u6, this._$AN = void 0, this.element = t3, this.name = e3, this._$AM = s2, this.options = r3, i3.length > 2 || i3[0] !== "" || i3[1] !== "" ? (this._$AH = Array(i3.length - 1).fill(new String()), this.strings = i3) : this._$AH = u6;
  }
  get tagName() {
    return this.element.tagName;
  }
  get _$AU() {
    return this._$AM._$AU;
  }
  _$AI(t3, e3 = this, i3, s2) {
    let r3 = this.strings, n4 = false;
    if (r3 === void 0)
      t3 = H7(this, t3, e3, 0), n4 = !C8(t3) || t3 !== this._$AH && t3 !== m7, n4 && (this._$AH = t3);
    else {
      let $5 = t3, l5, h4;
      for (t3 = r3[0], l5 = 0; l5 < r3.length - 1; l5++)
        h4 = H7(this, $5[i3 + l5], e3, l5), h4 === m7 && (h4 = this._$AH[l5]), n4 || (n4 = !C8(h4) || h4 !== this._$AH[l5]), h4 === u6 ? t3 = u6 : t3 !== u6 && (t3 += (h4 ?? "") + r3[l5 + 1]), this._$AH[l5] = h4;
    }
    n4 && !s2 && this.j(t3);
  }
  j(t3) {
    t3 === u6 ? this.element.removeAttribute(this.name) : this.element.setAttribute(this.name, t3 ?? "");
  }
};
var R5 = class extends T4 {
  constructor() {
    super(...arguments), this.type = 3;
  }
  j(t3) {
    this.element[this.name] = t3 === u6 ? void 0 : t3;
  }
};
var st5 = y10 ? y10.emptyScript : "";
var L5 = class extends T4 {
  constructor() {
    super(...arguments), this.type = 4;
  }
  j(t3) {
    t3 && t3 !== u6 ? this.element.setAttribute(this.name, st5) : this.element.removeAttribute(this.name);
  }
};
var D6 = class extends T4 {
  constructor(t3, e3, i3, s2, r3) {
    super(t3, e3, i3, s2, r3), this.type = 5;
  }
  _$AI(t3, e3 = this) {
    var i3;
    if ((t3 = (i3 = H7(this, t3, e3, 0)) !== null && i3 !== void 0 ? i3 : u6) === m7)
      return;
    let s2 = this._$AH, r3 = t3 === u6 && s2 !== u6 || t3.capture !== s2.capture || t3.once !== s2.once || t3.passive !== s2.passive, n4 = t3 !== u6 && (s2 === u6 || r3);
    r3 && this.element.removeEventListener(this.name, this, s2), n4 && this.element.addEventListener(this.name, this, t3), this._$AH = t3;
  }
  handleEvent(t3) {
    var e3, i3;
    typeof this._$AH == "function" ? this._$AH.call((i3 = (e3 = this.options) === null || e3 === void 0 ? void 0 : e3.host) !== null && i3 !== void 0 ? i3 : this.element, t3) : this._$AH.handleEvent(t3);
  }
};
var O5 = class {
  constructor(t3, e3, i3) {
    this.element = t3, this.type = 6, this._$AN = void 0, this._$AM = e3, this.options = i3;
  }
  get _$AU() {
    return this._$AM._$AU;
  }
  _$AI(t3) {
    H7(this, t3);
  }
};
var F5 = w6.litHtmlPolyfillSupport;
F5?.(f5, x5), ((P5 = w6.litHtmlVersions) !== null && P5 !== void 0 ? P5 : w6.litHtmlVersions = []).push("2.7.4");
var K6 = { ATTRIBUTE: 1, CHILD: 2, PROPERTY: 3, BOOLEAN_ATTRIBUTE: 4, EVENT: 5, ELEMENT: 6 };
var Q5 = (o3) => (...t3) => ({ _$litDirective$: o3, values: t3 });
var I4 = class {
  constructor(t3) {
  }
  get _$AU() {
    return this._$AM._$AU;
  }
  _$AT(t3, e3, i3) {
    this._$Ct = t3, this._$AM = e3, this._$Ci = i3;
  }
  _$AS(t3, e3) {
    return this.update(t3, e3);
  }
  update(t3, e3) {
    return this.render(...e3);
  }
};
var X5 = "important";
var nt5 = " !" + X5;
var ut3 = Q5(class extends I4 {
  constructor(o3) {
    var t3;
    if (super(o3), o3.type !== K6.ATTRIBUTE || o3.name !== "style" || ((t3 = o3.strings) === null || t3 === void 0 ? void 0 : t3.length) > 2)
      throw Error("The `styleMap` directive must be used in the `style` attribute and must be the only part in the attribute.");
  }
  render(o3) {
    return Object.keys(o3).reduce((t3, e3) => {
      let i3 = o3[e3];
      return i3 == null ? t3 : t3 + `${e3 = e3.includes("-") ? e3 : e3.replace(/(?:^(webkit|moz|ms|o)|)(?=[A-Z])/g, "-$&").toLowerCase()}:${i3};`;
    }, "");
  }
  update(o3, [t3]) {
    let { style: e3 } = o3.element;
    if (this.ut === void 0) {
      this.ut = /* @__PURE__ */ new Set();
      for (let i3 in t3)
        this.ut.add(i3);
      return this.render(t3);
    }
    this.ut.forEach((i3) => {
      t3[i3] == null && (this.ut.delete(i3), i3.includes("-") ? e3.removeProperty(i3) : e3[i3] = "");
    });
    for (let i3 in t3) {
      let s2 = t3[i3];
      if (s2 != null) {
        this.ut.add(i3);
        let r3 = typeof s2 == "string" && s2.endsWith(nt5);
        i3.includes("-") || r3 ? e3.setProperty(i3, r3 ? s2.slice(0, -11) : s2, r3 ? X5 : "") : e3[i3] = s2;
      }
    }
    return m7;
  }
});

// src/emulator/multi-webview-comp-navigator-bar.html.ts
var TAG4 = "multi-webview-comp-navigation-bar";
var MultiWebviewCompNavigationBar = class extends n {
  constructor() {
    super(...arguments);
    this._color = "#ccccccFF";
    this._style = "DEFAULT";
    this._overlay = false;
    this._visible = true;
    this._insets = {
      top: 0,
      right: 0,
      bottom: 20,
      left: 0
    };
  }
  updated(_changedProperties) {
    if (_changedProperties.has("_visible") || _changedProperties.has("_overlay")) {
      this.dispatchEvent(new Event("safe_area_need_update"));
    }
  }
  createBackgroundStyleMap() {
    return {
      backgroundColor: this._overlay ? "transparent" : this._color
    };
  }
  createContainerStyleMap() {
    const isLight = window.matchMedia("(prefers-color-scheme: light)");
    return {
      color: this._style === "LIGHT" ? "#000000FF" : this._style === "DARK" ? "#FFFFFFFF" : isLight ? "#000000FF" : "#FFFFFFFF"
    };
  }
  setHostStyle() {
    const host = this.renderRoot.host;
    host.style.position = this._overlay ? "absolute" : "relative";
    host.style.overflow = this._visible ? "visible" : "hidden";
  }
  back() {
    this.dispatchEvent(new Event("back"));
  }
  home() {
    console.error("navigation-bar click home \u4F46\u662F\u8FD8\u6CA1\u6709\u5904\u7406");
  }
  menu() {
    console.error(`navigation-bar \u70B9\u51FB\u4E86menu \u4F46\u662F\u8FD8\u6CA1\u6709\u5904\u7406`);
  }
  render() {
    this.setHostStyle();
    const backgroundStyleMap = this.createBackgroundStyleMap();
    const containerStyleMap = this.createContainerStyleMap();
    return tt3`
      <div class="container">
        <div class="background" style=${ut3(backgroundStyleMap)}></div>
        <!-- android 导航栏 -->
        <div
          class="navigation_bar_container"
          style=${ut3(containerStyleMap)}
        >
          <div class="menu" @click="${this.menu}">
            <svg
              class="icon_svg menu_svg"
              xmlns="http://www.w3.org/2000/svg"
              viewBox="0 0 448 512"
            >
              <path
                fill="currentColor"
                d="M0 96C0 78.3 14.3 64 32 64H416c17.7 0 32 14.3 32 32s-14.3 32-32 32H32C14.3 128 0 113.7 0 96zM0 256c0-17.7 14.3-32 32-32H416c17.7 0 32 14.3 32 32s-14.3 32-32 32H32c-17.7 0-32-14.3-32-32zM448 416c0 17.7-14.3 32-32 32H32c-17.7 0-32-14.3-32-32s14.3-32 32-32H416c17.7 0 32 14.3 32 32z"
              />
            </svg>
          </div>
          <div class="home" @click="${this.home}">
            <svg
              class="icon_svg"
              xmlns="http://www.w3.org/2000/svg"
              viewBox="0 0 512 512"
            >
              <path
                fill="currentColor"
                d="M464 256A208 208 0 1 0 48 256a208 208 0 1 0 416 0zM0 256a256 256 0 1 1 512 0A256 256 0 1 1 0 256z"
              />
            </svg>
          </div>
          <div class="back" @click="${this.back}">
            <svg
              class="icon_svg"
              viewBox="0 0 1024 1024"
              version="1.1"
              xmlns="http://www.w3.org/2000/svg"
            >
              <path
                fill="currentColor"
                d="M814.40768 119.93088a46.08 46.08 0 0 0-45.13792 2.58048l-568.07424 368.64a40.42752 40.42752 0 0 0-18.75968 33.71008c0 13.39392 7.00416 25.96864 18.75968 33.66912l568.07424 368.64c13.35296 8.68352 30.72 9.66656 45.13792 2.58048a40.67328 40.67328 0 0 0 23.38816-36.2496v-737.28a40.71424 40.71424 0 0 0-23.38816-36.29056zM750.3872 815.3088L302.81728 524.86144l447.61088-290.44736v580.89472z"
              ></path>
            </svg>
          </div>
        </div>
      </div>
    `;
  }
};
MultiWebviewCompNavigationBar.styles = createAllCSS4();
__decorateClass([
  o({ type: String })
], MultiWebviewCompNavigationBar.prototype, "_color", 2);
__decorateClass([
  o({ type: String })
], MultiWebviewCompNavigationBar.prototype, "_style", 2);
__decorateClass([
  o({ type: Boolean })
], MultiWebviewCompNavigationBar.prototype, "_overlay", 2);
__decorateClass([
  o({ type: Boolean })
], MultiWebviewCompNavigationBar.prototype, "_visible", 2);
__decorateClass([
  o({ type: Object })
], MultiWebviewCompNavigationBar.prototype, "_insets", 2);
MultiWebviewCompNavigationBar = __decorateClass([
  c5(TAG4)
], MultiWebviewCompNavigationBar);
function createAllCSS4() {
  return [
    C7`
      :host {
        position: relative;
        z-index: 999999999;
        box-sizing: border-box;
        left: 0px;
        bottom: 0px;
        margin: 0px;
        width: 100%;
        -webkit-app-region: drag;
        -webkit-user-select: none;
      }

      .container {
        position: relative;
        box-sizing: border-box;
        width: 100%;
        height: 26px;
      }
      .background {
        position: absolute;
        top: 0px;
        left: 0px;
        width: 100%;
        height: 100%;
        background: #ffffff00;
      }

      .line-container {
        position: absolute;
        top: 0px;
        left: 0px;
        display: flex;
        justify-content: center;
        align-items: center;
        width: 100%;
        height: 100%;
      }

      .line {
        width: 50%;
        height: 4px;
        border-radius: 4px;
      }

      .line-default {
        background: #ffffffff;
      }

      .line-dark {
        background: #000000ff;
      }

      .line-light {
        background: #ffffffff;
      }

      .navigation_bar_container {
        position: absolute;
        top: 0px;
        left: 0px;
        display: flex;
        justify-content: space-around;
        align-items: center;
        width: 100%;
        height: 100%;
      }

      .menu,
      .home,
      .back {
        display: flex;
        justify-content: center;
        align-items: center;
        cursor: pointer;
        -webkit-app-region: no-drag;
      }

      .icon_svg {
        width: 20px;
        height: 20px;
      }
    `
  ];
}

// src/emulator/multi-webview-comp-share.html.ts
var TAG5 = "multi-webview-comp-share";
var MultiWebviewCompShare = class extends n {
  constructor() {
    super(...arguments);
    this._title = "\u6807\u9898 \u8FD9\u91CC\u662F\u8D85\u957F\u7684\u6807\u9898\uFF0C\u8FD9\u91CC\u662F\u8D85\u957F\u7684\u6807\u9898\u8FD9\u91CC\u662F\u8D85\u957F\u7684\uFF0C\u8FD9\u91CC\u662F\u8D85\u957F\u7684\u6807\u9898\uFF0C\u8FD9\u91CC\u662F\u8D85\u957F\u7684\u6807\u9898";
    this._text = "\u6587\u672C\u5185\u5BB9 \u8FD9\u91CC\u662F\u8D85\u957F\u7684\u5185\u5BB9\uFF0C\u8FD9\u91CC\u662F\u8D85\u957F\u7684\u5185\u5BB9\uFF0C\u8FD9\u91CC\u662F\u8D85\u957F\u7684\u5185\u5BB9\uFF0C\u8FD9\u91CC\u662F\u8D85\u957F\u7684\u5185\u5BB9\uFF0C";
    this._link = "http://www.baidu.com?url=";
    this._src = "https://img.tukuppt.com/photo-big/00/00/94/6152bc0ce6e5d805.jpg";
    this._filename = "";
  }
  firstUpdated(_changedProperties) {
    this.shadowRoot?.host.addEventListener("click", this.cancel);
  }
  cancel() {
    this.shadowRoot?.host.remove();
  }
  render() {
    console.log("this._src: ", this._src);
    return tt3`
      <div class="panel">
        ${t2(
      this._src,
      () => tt3`<img class="img" src=${this._src}></img>`,
      () => tt3`<div class="filename">${this._filename}</div>`
    )}
        <div class="text_container">
          <h2 class="h2">${this._title}</h2>
          <p class="p">${this._text}</p>
          <a class="a" href=${this._link} target="_blank">${this._link}</a>
        </div>
      </div>
    `;
  }
};
MultiWebviewCompShare.styles = createAllCSS5();
__decorateClass([
  o({ type: String })
], MultiWebviewCompShare.prototype, "_title", 2);
__decorateClass([
  o({ type: String })
], MultiWebviewCompShare.prototype, "_text", 2);
__decorateClass([
  o({ type: String })
], MultiWebviewCompShare.prototype, "_link", 2);
__decorateClass([
  o({ type: String })
], MultiWebviewCompShare.prototype, "_src", 2);
__decorateClass([
  o({ type: String })
], MultiWebviewCompShare.prototype, "_filename", 2);
MultiWebviewCompShare = __decorateClass([
  c5(TAG5)
], MultiWebviewCompShare);
function createAllCSS5() {
  return [
    C7`
      :host {
        position: absolute;
        z-index: 1;
        left: 0px;
        top: 0px;
        box-sizing: border-box;
        padding-bottom: 200px;
        width: 100%;
        height: 100%;
        display: flex;
        justify-content: center;
        align-items: center;
        background: #000000cc;
        cursor: pointer;
        backdrop-filter: blur(5px);
      }

      .panel {
        display: flex;
        flex-direction: column;
        justify-content: center;
        width: 70%;
        border-radius: 6px;
        background: #ffffffff;
        border-radius: 6px;
        overflow: hidden;
      }

      .img {
        display: block;
        box-sizing: border-box;
        padding: 30px;
        max-width: 100%;
        max-height: 300px;
      }

      .filename {
        display: flex;
        justify-content: center;
        align-items: center;
        box-sizing: border-box;
        padding: 30px;
        max-width: 100%;
        max-height: 300px;
        font-size: 18px;
      }

      .text_container {
        box-sizing: border-box;
        padding: 20px;
        width: 100%;
        height: auto;
        background: #000000ff;
      }

      .h2 {
        margin: 0px;
        padding: 0px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        font-size: 16px;
        color: #fff;
      }

      .p {
        margin: 0px;
        padding: 0px;
        font-size: 13px;
        color: #666;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .a {
        display: block;
        font-size: 12px;
        color: #999;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    `
  ];
}

// https://esm.sh/v124/lit-html@2.7.4/denonext/directives/class-map.js
var I5;
var w7 = window;
var y11 = w7.trustedTypes;
var O6 = y11 ? y11.createPolicy("lit-html", { createHTML: (r3) => r3 }) : void 0;
var U7 = "$lit$";
var v10 = `lit$${(Math.random() + "").slice(9)}$`;
var Y6 = "?" + v10;
var X6 = `<${Y6}>`;
var g7 = document;
var M6 = () => g7.createComment("");
var C9 = (r3) => r3 === null || typeof r3 != "object" && typeof r3 != "function";
var q5 = Array.isArray;
var tt6 = (r3) => q5(r3) || typeof r3?.[Symbol.iterator] == "function";
var B7 = `[ 	
\f\r]`;
var N6 = /<(?:(!--|\/[^a-zA-Z])|(\/?[a-zA-Z][^>\s]*)|(\/?$))/g;
var k8 = /-->/g;
var V5 = />/g;
var _9 = RegExp(`>|${B7}(?:([^\\s"'>=/]+)(${B7}*=${B7}*(?:[^ 	
\f\r"'\`<>=]|("|')|))|$)`, "g");
var W6 = /'/g;
var Z5 = /"/g;
var G8 = /^(?:script|style|textarea|title)$/i;
var J8 = (r3) => (t3, ...e3) => ({ _$litType$: r3, strings: t3, values: e3 });
var st6 = J8(1);
var nt6 = J8(2);
var m8 = Symbol.for("lit-noChange");
var $4 = Symbol.for("lit-nothing");
var z6 = /* @__PURE__ */ new WeakMap();
var p9 = g7.createTreeWalker(g7, 129, null, false);
var et7 = (r3, t3) => {
  let e3 = r3.length - 1, i3 = [], s2, n4 = t3 === 2 ? "<svg>" : "", o3 = N6;
  for (let l5 = 0; l5 < e3; l5++) {
    let h4 = r3[l5], A5, a4, d6 = -1, u7 = 0;
    for (; u7 < h4.length && (o3.lastIndex = u7, a4 = o3.exec(h4), a4 !== null); )
      u7 = o3.lastIndex, o3 === N6 ? a4[1] === "!--" ? o3 = k8 : a4[1] !== void 0 ? o3 = V5 : a4[2] !== void 0 ? (G8.test(a4[2]) && (s2 = RegExp("</" + a4[2], "g")), o3 = _9) : a4[3] !== void 0 && (o3 = _9) : o3 === _9 ? a4[0] === ">" ? (o3 = s2 ?? N6, d6 = -1) : a4[1] === void 0 ? d6 = -2 : (d6 = o3.lastIndex - a4[2].length, A5 = a4[1], o3 = a4[3] === void 0 ? _9 : a4[3] === '"' ? Z5 : W6) : o3 === Z5 || o3 === W6 ? o3 = _9 : o3 === k8 || o3 === V5 ? o3 = N6 : (o3 = _9, s2 = void 0);
    let b7 = o3 === _9 && r3[l5 + 1].startsWith("/>") ? " " : "";
    n4 += o3 === N6 ? h4 + X6 : d6 >= 0 ? (i3.push(A5), h4.slice(0, d6) + U7 + h4.slice(d6) + v10 + b7) : h4 + v10 + (d6 === -2 ? (i3.push(void 0), l5) : b7);
  }
  let c8 = n4 + (r3[e3] || "<?>") + (t3 === 2 ? "</svg>" : "");
  if (!Array.isArray(r3) || !r3.hasOwnProperty("raw"))
    throw Error("invalid template strings array");
  return [O6 !== void 0 ? O6.createHTML(c8) : c8, i3];
};
var f6 = class {
  constructor({ strings: t3, _$litType$: e3 }, i3) {
    let s2;
    this.parts = [];
    let n4 = 0, o3 = 0, c8 = t3.length - 1, l5 = this.parts, [h4, A5] = et7(t3, e3);
    if (this.el = f6.createElement(h4, i3), p9.currentNode = this.el.content, e3 === 2) {
      let a4 = this.el.content, d6 = a4.firstChild;
      d6.remove(), a4.append(...d6.childNodes);
    }
    for (; (s2 = p9.nextNode()) !== null && l5.length < c8; ) {
      if (s2.nodeType === 1) {
        if (s2.hasAttributes()) {
          let a4 = [];
          for (let d6 of s2.getAttributeNames())
            if (d6.endsWith(U7) || d6.startsWith(v10)) {
              let u7 = A5[o3++];
              if (a4.push(d6), u7 !== void 0) {
                let b7 = s2.getAttribute(u7.toLowerCase() + U7).split(v10), E8 = /([.?@])?(.*)/.exec(u7);
                l5.push({ type: 1, index: n4, name: E8[2], strings: b7, ctor: E8[1] === "." ? R6 : E8[1] === "?" ? L6 : E8[1] === "@" ? j8 : T5 });
              } else
                l5.push({ type: 6, index: n4 });
            }
          for (let d6 of a4)
            s2.removeAttribute(d6);
        }
        if (G8.test(s2.tagName)) {
          let a4 = s2.textContent.split(v10), d6 = a4.length - 1;
          if (d6 > 0) {
            s2.textContent = y11 ? y11.emptyScript : "";
            for (let u7 = 0; u7 < d6; u7++)
              s2.append(a4[u7], M6()), p9.nextNode(), l5.push({ type: 2, index: ++n4 });
            s2.append(a4[d6], M6());
          }
        }
      } else if (s2.nodeType === 8)
        if (s2.data === Y6)
          l5.push({ type: 2, index: n4 });
        else {
          let a4 = -1;
          for (; (a4 = s2.data.indexOf(v10, a4 + 1)) !== -1; )
            l5.push({ type: 7, index: n4 }), a4 += v10.length - 1;
        }
      n4++;
    }
  }
  static createElement(t3, e3) {
    let i3 = g7.createElement("template");
    return i3.innerHTML = t3, i3;
  }
};
function H8(r3, t3, e3 = r3, i3) {
  var s2, n4, o3, c8;
  if (t3 === m8)
    return t3;
  let l5 = i3 !== void 0 ? (s2 = e3._$Co) === null || s2 === void 0 ? void 0 : s2[i3] : e3._$Cl, h4 = C9(t3) ? void 0 : t3._$litDirective$;
  return l5?.constructor !== h4 && ((n4 = l5?._$AO) === null || n4 === void 0 || n4.call(l5, false), h4 === void 0 ? l5 = void 0 : (l5 = new h4(r3), l5._$AT(r3, e3, i3)), i3 !== void 0 ? ((o3 = (c8 = e3)._$Co) !== null && o3 !== void 0 ? o3 : c8._$Co = [])[i3] = l5 : e3._$Cl = l5), l5 !== void 0 && (t3 = H8(r3, l5._$AS(r3, t3.values), l5, i3)), t3;
}
var P6 = class {
  constructor(t3, e3) {
    this._$AV = [], this._$AN = void 0, this._$AD = t3, this._$AM = e3;
  }
  get parentNode() {
    return this._$AM.parentNode;
  }
  get _$AU() {
    return this._$AM._$AU;
  }
  u(t3) {
    var e3;
    let { el: { content: i3 }, parts: s2 } = this._$AD, n4 = ((e3 = t3?.creationScope) !== null && e3 !== void 0 ? e3 : g7).importNode(i3, true);
    p9.currentNode = n4;
    let o3 = p9.nextNode(), c8 = 0, l5 = 0, h4 = s2[0];
    for (; h4 !== void 0; ) {
      if (c8 === h4.index) {
        let A5;
        h4.type === 2 ? A5 = new x6(o3, o3.nextSibling, this, t3) : h4.type === 1 ? A5 = new h4.ctor(o3, h4.name, h4.strings, this, t3) : h4.type === 6 && (A5 = new D7(o3, this, t3)), this._$AV.push(A5), h4 = s2[++l5];
      }
      c8 !== h4?.index && (o3 = p9.nextNode(), c8++);
    }
    return p9.currentNode = g7, n4;
  }
  v(t3) {
    let e3 = 0;
    for (let i3 of this._$AV)
      i3 !== void 0 && (i3.strings !== void 0 ? (i3._$AI(t3, i3, e3), e3 += i3.strings.length - 2) : i3._$AI(t3[e3])), e3++;
  }
};
var x6 = class {
  constructor(t3, e3, i3, s2) {
    var n4;
    this.type = 2, this._$AH = $4, this._$AN = void 0, this._$AA = t3, this._$AB = e3, this._$AM = i3, this.options = s2, this._$Cp = (n4 = s2?.isConnected) === null || n4 === void 0 || n4;
  }
  get _$AU() {
    var t3, e3;
    return (e3 = (t3 = this._$AM) === null || t3 === void 0 ? void 0 : t3._$AU) !== null && e3 !== void 0 ? e3 : this._$Cp;
  }
  get parentNode() {
    let t3 = this._$AA.parentNode, e3 = this._$AM;
    return e3 !== void 0 && t3?.nodeType === 11 && (t3 = e3.parentNode), t3;
  }
  get startNode() {
    return this._$AA;
  }
  get endNode() {
    return this._$AB;
  }
  _$AI(t3, e3 = this) {
    t3 = H8(this, t3, e3), C9(t3) ? t3 === $4 || t3 == null || t3 === "" ? (this._$AH !== $4 && this._$AR(), this._$AH = $4) : t3 !== this._$AH && t3 !== m8 && this._(t3) : t3._$litType$ !== void 0 ? this.g(t3) : t3.nodeType !== void 0 ? this.$(t3) : tt6(t3) ? this.T(t3) : this._(t3);
  }
  k(t3) {
    return this._$AA.parentNode.insertBefore(t3, this._$AB);
  }
  $(t3) {
    this._$AH !== t3 && (this._$AR(), this._$AH = this.k(t3));
  }
  _(t3) {
    this._$AH !== $4 && C9(this._$AH) ? this._$AA.nextSibling.data = t3 : this.$(g7.createTextNode(t3)), this._$AH = t3;
  }
  g(t3) {
    var e3;
    let { values: i3, _$litType$: s2 } = t3, n4 = typeof s2 == "number" ? this._$AC(t3) : (s2.el === void 0 && (s2.el = f6.createElement(s2.h, this.options)), s2);
    if (((e3 = this._$AH) === null || e3 === void 0 ? void 0 : e3._$AD) === n4)
      this._$AH.v(i3);
    else {
      let o3 = new P6(n4, this), c8 = o3.u(this.options);
      o3.v(i3), this.$(c8), this._$AH = o3;
    }
  }
  _$AC(t3) {
    let e3 = z6.get(t3.strings);
    return e3 === void 0 && z6.set(t3.strings, e3 = new f6(t3)), e3;
  }
  T(t3) {
    q5(this._$AH) || (this._$AH = [], this._$AR());
    let e3 = this._$AH, i3, s2 = 0;
    for (let n4 of t3)
      s2 === e3.length ? e3.push(i3 = new x6(this.k(M6()), this.k(M6()), this, this.options)) : i3 = e3[s2], i3._$AI(n4), s2++;
    s2 < e3.length && (this._$AR(i3 && i3._$AB.nextSibling, s2), e3.length = s2);
  }
  _$AR(t3 = this._$AA.nextSibling, e3) {
    var i3;
    for ((i3 = this._$AP) === null || i3 === void 0 || i3.call(this, false, true, e3); t3 && t3 !== this._$AB; ) {
      let s2 = t3.nextSibling;
      t3.remove(), t3 = s2;
    }
  }
  setConnected(t3) {
    var e3;
    this._$AM === void 0 && (this._$Cp = t3, (e3 = this._$AP) === null || e3 === void 0 || e3.call(this, t3));
  }
};
var T5 = class {
  constructor(t3, e3, i3, s2, n4) {
    this.type = 1, this._$AH = $4, this._$AN = void 0, this.element = t3, this.name = e3, this._$AM = s2, this.options = n4, i3.length > 2 || i3[0] !== "" || i3[1] !== "" ? (this._$AH = Array(i3.length - 1).fill(new String()), this.strings = i3) : this._$AH = $4;
  }
  get tagName() {
    return this.element.tagName;
  }
  get _$AU() {
    return this._$AM._$AU;
  }
  _$AI(t3, e3 = this, i3, s2) {
    let n4 = this.strings, o3 = false;
    if (n4 === void 0)
      t3 = H8(this, t3, e3, 0), o3 = !C9(t3) || t3 !== this._$AH && t3 !== m8, o3 && (this._$AH = t3);
    else {
      let c8 = t3, l5, h4;
      for (t3 = n4[0], l5 = 0; l5 < n4.length - 1; l5++)
        h4 = H8(this, c8[i3 + l5], e3, l5), h4 === m8 && (h4 = this._$AH[l5]), o3 || (o3 = !C9(h4) || h4 !== this._$AH[l5]), h4 === $4 ? t3 = $4 : t3 !== $4 && (t3 += (h4 ?? "") + n4[l5 + 1]), this._$AH[l5] = h4;
    }
    o3 && !s2 && this.j(t3);
  }
  j(t3) {
    t3 === $4 ? this.element.removeAttribute(this.name) : this.element.setAttribute(this.name, t3 ?? "");
  }
};
var R6 = class extends T5 {
  constructor() {
    super(...arguments), this.type = 3;
  }
  j(t3) {
    this.element[this.name] = t3 === $4 ? void 0 : t3;
  }
};
var it5 = y11 ? y11.emptyScript : "";
var L6 = class extends T5 {
  constructor() {
    super(...arguments), this.type = 4;
  }
  j(t3) {
    t3 && t3 !== $4 ? this.element.setAttribute(this.name, it5) : this.element.removeAttribute(this.name);
  }
};
var j8 = class extends T5 {
  constructor(t3, e3, i3, s2, n4) {
    super(t3, e3, i3, s2, n4), this.type = 5;
  }
  _$AI(t3, e3 = this) {
    var i3;
    if ((t3 = (i3 = H8(this, t3, e3, 0)) !== null && i3 !== void 0 ? i3 : $4) === m8)
      return;
    let s2 = this._$AH, n4 = t3 === $4 && s2 !== $4 || t3.capture !== s2.capture || t3.once !== s2.once || t3.passive !== s2.passive, o3 = t3 !== $4 && (s2 === $4 || n4);
    n4 && this.element.removeEventListener(this.name, this, s2), o3 && this.element.addEventListener(this.name, this, t3), this._$AH = t3;
  }
  handleEvent(t3) {
    var e3, i3;
    typeof this._$AH == "function" ? this._$AH.call((i3 = (e3 = this.options) === null || e3 === void 0 ? void 0 : e3.host) !== null && i3 !== void 0 ? i3 : this.element, t3) : this._$AH.handleEvent(t3);
  }
};
var D7 = class {
  constructor(t3, e3, i3) {
    this.element = t3, this.type = 6, this._$AN = void 0, this._$AM = e3, this.options = i3;
  }
  get _$AU() {
    return this._$AM._$AU;
  }
  _$AI(t3) {
    H8(this, t3);
  }
};
var F6 = w7.litHtmlPolyfillSupport;
F6?.(f6, x6), ((I5 = w7.litHtmlVersions) !== null && I5 !== void 0 ? I5 : w7.litHtmlVersions = []).push("2.7.4");
var K7 = { ATTRIBUTE: 1, CHILD: 2, PROPERTY: 3, BOOLEAN_ATTRIBUTE: 4, EVENT: 5, ELEMENT: 6 };
var Q6 = (r3) => (...t3) => ({ _$litDirective$: r3, values: t3 });
var S7 = class {
  constructor(t3) {
  }
  get _$AU() {
    return this._$AM._$AU;
  }
  _$AT(t3, e3, i3) {
    this._$Ct = t3, this._$AM = e3, this._$Ci = i3;
  }
  _$AS(t3, e3) {
    return this.update(t3, e3);
  }
  update(t3, e3) {
    return this.render(...e3);
  }
};
var at3 = Q6(class extends S7 {
  constructor(r3) {
    var t3;
    if (super(r3), r3.type !== K7.ATTRIBUTE || r3.name !== "class" || ((t3 = r3.strings) === null || t3 === void 0 ? void 0 : t3.length) > 2)
      throw Error("`classMap()` can only be used in the `class` attribute and must be the only part in the attribute.");
  }
  render(r3) {
    return " " + Object.keys(r3).filter((t3) => r3[t3]).join(" ") + " ";
  }
  update(r3, [t3]) {
    var e3, i3;
    if (this.it === void 0) {
      this.it = /* @__PURE__ */ new Set(), r3.strings !== void 0 && (this.nt = new Set(r3.strings.join(" ").split(/\s/).filter((n4) => n4 !== "")));
      for (let n4 in t3)
        t3[n4] && !(!((e3 = this.nt) === null || e3 === void 0) && e3.has(n4)) && this.it.add(n4);
      return this.render(t3);
    }
    let s2 = r3.element.classList;
    this.it.forEach((n4) => {
      n4 in t3 || (s2.remove(n4), this.it.delete(n4));
    });
    for (let n4 in t3) {
      let o3 = !!t3[n4];
      o3 === this.it.has(n4) || !((i3 = this.nt) === null || i3 === void 0) && i3.has(n4) || (o3 ? (s2.add(n4), this.it.add(n4)) : (s2.remove(n4), this.it.delete(n4)));
    }
    return m8;
  }
});

// src/emulator/multi-webview-comp-status-bar.html.ts
var TAG6 = "multi-webview-comp-status-bar";
var MultiWebviewCompStatusBar = class extends n {
  constructor() {
    super(...arguments);
    this._color = "#FFFFFF80";
    this._style = "DEFAULT";
    this._overlay = false;
    this._visible = true;
    this._insets = {
      top: 0,
      right: 0,
      bottom: 0,
      left: 0
    };
    this._torchIsOpen = false;
  }
  updated(changedProperties) {
    if (changedProperties.has("_visible") || changedProperties.has("_overlay")) {
      this.dispatchEvent(new Event("safe_area_need_update"));
    }
    super.updated(changedProperties);
  }
  render() {
    return tt3`
      <div
        class=${at3({
      "comp-container": true,
      overlay: this._overlay,
      visible: this._visible,
      [this._style.toLowerCase()]: true
    })}
        style=${ut3({
      "--bg-color": this._color,
      height: this._insets.top + "px"
    })}
      >
        <div class="background"></div>
        <div class="container">
          ${t2(
      this._visible,
      () => tt3`<div class="left_container">10:00</div>`
    )}
          <div class="center_container">
            ${t2(
      this._torchIsOpen,
      () => tt3`<div class="torch_symbol"></div>`
    )}
          </div>
          ${t2(
      this._visible,
      () => tt3`
              <div class="right_container">
                <!-- 移动信号标志 -->
                <svg
                  t="1677291966287"
                  class="icon icon-signal"
                  viewBox="0 0 1024 1024"
                  version="1.1"
                  xmlns="http://www.w3.org/2000/svg"
                  p-id="5745"
                  width="32"
                  height="32"
                >
                  <path
                    fill="currentColor"
                    d="M0 704h208v192H0zM272 512h208v384H272zM544 288h208v608H544zM816 128h208v768H816z"
                    p-id="5746"
                  ></path>
                </svg>

                <!-- wifi 信号标志 -->
                <svg
                  t="1677291873784"
                  class="icon icon-wifi"
                  viewBox="0 0 1024 1024"
                  version="1.1"
                  xmlns="http://www.w3.org/2000/svg"
                  p-id="4699"
                  width="32"
                  height="32"
                >
                  <path
                    fill="currentColor"
                    d="M512 896 665.6 691.2C622.933333 659.2 569.6 640 512 640 454.4 640 401.066667 659.2 358.4 691.2L512 896M512 128C339.2 128 179.626667 185.173333 51.2 281.6L128 384C234.666667 303.786667 367.786667 256 512 256 656.213333 256 789.333333 303.786667 896 384L972.8 281.6C844.373333 185.173333 684.8 128 512 128M512 384C396.8 384 290.56 421.973333 204.8 486.4L281.6 588.8C345.6 540.586667 425.386667 512 512 512 598.613333 512 678.4 540.586667 742.4 588.8L819.2 486.4C733.44 421.973333 627.2 384 512 384Z"
                    p-id="4700"
                  ></path>
                </svg>

                <!-- 电池电量标志 -->
                <svg
                  t="1677291736404"
                  class="icon icon-electricity"
                  viewBox="0 0 1024 1024"
                  version="1.1"
                  xmlns="http://www.w3.org/2000/svg"
                  p-id="2796"
                  width="32"
                  height="32"
                >
                  <path
                    fill="currentColor"
                    d="M984.2 434.8c-5-2.9-8.2-8.2-8.2-13.9v-99.3c0-53.6-43.9-97.5-97.5-97.5h-781C43.9 224 0 267.9 0 321.5v380.9C0 756.1 43.9 800 97.5 800h780.9c53.6 0 97.5-43.9 97.5-97.5v-99.3c0-5.8 3.2-11 8.2-13.9 23.8-13.9 39.8-39.7 39.8-69.2v-16c0.1-29.6-15.9-55.5-39.7-69.3zM912 702.5c0 12-6.2 19.9-9.9 23.6-3.7 3.7-11.7 9.9-23.6 9.9h-781c-11.9 0-19.9-6.2-23.6-9.9-3.7-3.7-9.9-11.7-9.9-23.6v-381c0-11.9 6.2-19.9 9.9-23.6 3.7-3.7 11.7-9.9 23.6-9.9h780.9c11.9 0 19.9 6.2 23.6 9.9 3.7 3.7 9.9 11.7 9.9 23.6v381z"
                    fill="#606266"
                    p-id="2797"
                  ></path>
                  <path
                    fill="currentColor"
                    d="M736 344v336c0 8.8-7.2 16-16 16H112c-8.8 0-16-7.2-16-16V344c0-8.8 7.2-16 16-16h608c8.8 0 16 7.2 16 16z"
                    fill="#606266"
                    p-id="2798"
                  ></path>
                </svg>
              </div>
            `
    )}
        </div>
      </div>
    `;
  }
};
MultiWebviewCompStatusBar.styles = createAllCSS6();
__decorateClass([
  o({ type: String })
], MultiWebviewCompStatusBar.prototype, "_color", 2);
__decorateClass([
  o({ type: String })
], MultiWebviewCompStatusBar.prototype, "_style", 2);
__decorateClass([
  o({ type: Boolean })
], MultiWebviewCompStatusBar.prototype, "_overlay", 2);
__decorateClass([
  o({ type: Boolean })
], MultiWebviewCompStatusBar.prototype, "_visible", 2);
__decorateClass([
  o({ type: Object })
], MultiWebviewCompStatusBar.prototype, "_insets", 2);
__decorateClass([
  o({ type: Boolean })
], MultiWebviewCompStatusBar.prototype, "_torchIsOpen", 2);
MultiWebviewCompStatusBar = __decorateClass([
  c5(TAG6)
], MultiWebviewCompStatusBar);
function createAllCSS6() {
  return [
    C7`
      :host {
        display: block;
        -webkit-app-region: drag;
        -webkit-user-select: none;
        --cell-width: 80px;
      }

      .comp-container {
        display: grid;
        grid-template-columns: 1fr;
        grid-template-rows: 1fr;
        gap: 0px 0px;
        grid-template-areas: "view";
      }
      .comp-container.overlay {
        position: absolute;
        width: 100%;
        z-index: 1;
      }
      .comp-container:not(.visible) {
        display: none;
      }
      .comp-container.light {
        --fg-color: #ffffffff;
      }
      .comp-container.dark {
        --fg-color: #000000ff;
      }
      .comp-container.default {
        --fg-color: #ffffffff;
      }

      .background {
        grid-area: view;

        background: var(--bg-color);
      }

      .container {
        grid-area: view;
        color: var(--fg-color);

        display: flex;
        justify-content: center;
        align-items: flex-end;

        font-family: PingFangSC-Light, sans-serif;
      }
      /// 使用混合模式自适应当前视图的，大部分情况下可以使用，但是如何状态是灰色，这个效果会很糟糕。对此需要更好的css函数来解决，而不应该依靠js
      .comp-container.default .left_container,
      .comp-container.default .right_container {
        mix-blend-mode: difference;
      }

      .left_container {
        display: flex;
        justify-content: center;
        align-items: center;
        width: var(--cell-width);
        height: 100%;
        font-size: 15px;
        font-weight: 900;
        height: 2em;
      }

      .center_container {
        position: relative;
        display: flex;
        justify-content: center;
        align-items: center;
        width: calc(100% - var(--cell-width) * 2);
        height: 100%;
        border-bottom-left-radius: var(--border-radius);
        border-bottom-right-radius: var(--border-radius);
      }

      .center_container::after {
        content: "";
        width: 50%;
        height: 20px;
        border-radius: 10px;
        background: #111111;
      }

      .torch_symbol {
        position: absolute;
        z-index: 1;
        width: 10px;
        height: 10px;
        border-radius: 20px;
        background: #fa541c;
      }

      .right_container {
        display: flex;
        justify-content: flex-start;
        align-items: center;
        width: var(--cell-width);
        height: 100%;
      }

      .icon {
        margin-right: 5px;
        width: 18px;
        height: 18px;
      }
    `
  ];
}

// src/emulator/multi-webview-comp-toast.html.ts
var TAG7 = "multi-webview-comp-toast";
var MultiWebviewCompToast = class extends n {
  constructor() {
    super(...arguments);
    this._message = "test message";
    this._duration = `1000`;
    this._position = "top";
    this._beforeEntry = true;
  }
  firstUpdated() {
    setTimeout(() => {
      this._beforeEntry = false;
    }, 0);
  }
  transitionend(e3) {
    if (this._beforeEntry) {
      e3.target.remove();
      return;
    }
    setTimeout(() => {
      this._beforeEntry = true;
    }, parseInt(this._duration));
  }
  render() {
    const containerClassMap = {
      container: true,
      before_entry: this._beforeEntry ? true : false,
      after_entry: this._beforeEntry ? false : true,
      container_bottom: this._position === "bottom" ? true : false,
      container_top: this._position === "bottom" ? false : true
    };
    return tt3`
      <div
        class=${at3(containerClassMap)}
        @transitionend=${this.transitionend}
      >
        <p class="message">${this._message}</p>
      </div>
    `;
  }
};
MultiWebviewCompToast.styles = createAllCSS7();
MultiWebviewCompToast.properties = {
  _beforeEntry: { state: true }
};
__decorateClass([
  o({ type: String })
], MultiWebviewCompToast.prototype, "_message", 2);
__decorateClass([
  o({ type: String })
], MultiWebviewCompToast.prototype, "_duration", 2);
__decorateClass([
  o({ type: String })
], MultiWebviewCompToast.prototype, "_position", 2);
__decorateClass([
  c6()
], MultiWebviewCompToast.prototype, "_beforeEntry", 2);
MultiWebviewCompToast = __decorateClass([
  c5(TAG7)
], MultiWebviewCompToast);
function createAllCSS7() {
  return [
    C7`
      .container {
        position: absolute;
        left: 0px;
        box-sizing: border-box;
        padding: 0px 20px;
        width: 100%;
        transition: all 0.25s ease-in-out;
      }

      .container_bottom {
        bottom: 0px;
      }

      .container_top {
        top: 0px;
      }

      .before_entry {
        transform: translateX(-100vw);
      }

      .after_entry {
        transform: translateX(0vw);
      }

      .message {
        box-sizing: border-box;
        padding: 0px 6px;
        width: 100%;
        height: 38px;
        color: #ffffff;
        line-height: 38px;
        text-align: left;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        background: #eee;
        border-radius: 5px;
        background: #1677ff;
      }
    `
  ];
}

// https://esm.sh/v124/lit-html@2.7.4/denonext/directives/repeat.js
var W7;
var S8 = window;
var P7 = S8.trustedTypes;
var G9 = P7 ? P7.createPolicy("lit-html", { createHTML: (n4) => n4 }) : void 0;
var B8 = "$lit$";
var f7 = `lit$${(Math.random() + "").slice(9)}$`;
var z7 = "?" + f7;
var ut4 = `<${z7}>`;
var x7 = document;
var I6 = () => x7.createComment("");
var M7 = (n4) => n4 === null || typeof n4 != "object" && typeof n4 != "function";
var tt7 = Array.isArray;
var et8 = (n4) => tt7(n4) || typeof n4?.[Symbol.iterator] == "function";
var Z6 = `[ 	
\f\r]`;
var w8 = /<(?:(!--|\/[^a-zA-Z])|(\/?[a-zA-Z][^>\s]*)|(\/?$))/g;
var Q7 = /-->/g;
var Y7 = />/g;
var g8 = RegExp(`>|${Z6}(?:([^\\s"'>=/]+)(${Z6}*=${Z6}*(?:[^ 	
\f\r"'\`<>=]|("|')|))|$)`, "g");
var q6 = /'/g;
var J9 = /"/g;
var it6 = /^(?:script|style|textarea|title)$/i;
var st7 = (n4) => (t3, ...e3) => ({ _$litType$: n4, strings: t3, values: e3 });
var _t3 = st7(1);
var pt3 = st7(2);
var H9 = Symbol.for("lit-noChange");
var v11 = Symbol.for("lit-nothing");
var K8 = /* @__PURE__ */ new WeakMap();
var y12 = x7.createTreeWalker(x7, 129, null, false);
var nt7 = (n4, t3) => {
  let e3 = n4.length - 1, i3 = [], s2, r3 = t3 === 2 ? "<svg>" : "", o3 = w8;
  for (let h4 = 0; h4 < e3; h4++) {
    let l5 = n4[h4], _10, $5, a4 = -1, u7 = 0;
    for (; u7 < l5.length && (o3.lastIndex = u7, $5 = o3.exec(l5), $5 !== null); )
      u7 = o3.lastIndex, o3 === w8 ? $5[1] === "!--" ? o3 = Q7 : $5[1] !== void 0 ? o3 = Y7 : $5[2] !== void 0 ? (it6.test($5[2]) && (s2 = RegExp("</" + $5[2], "g")), o3 = g8) : $5[3] !== void 0 && (o3 = g8) : o3 === g8 ? $5[0] === ">" ? (o3 = s2 ?? w8, a4 = -1) : $5[1] === void 0 ? a4 = -2 : (a4 = o3.lastIndex - $5[2].length, _10 = $5[1], o3 = $5[3] === void 0 ? g8 : $5[3] === '"' ? J9 : q6) : o3 === J9 || o3 === q6 ? o3 = g8 : o3 === Q7 || o3 === Y7 ? o3 = w8 : (o3 = g8, s2 = void 0);
    let c8 = o3 === g8 && n4[h4 + 1].startsWith("/>") ? " " : "";
    r3 += o3 === w8 ? l5 + ut4 : a4 >= 0 ? (i3.push(_10), l5.slice(0, a4) + B8 + l5.slice(a4) + f7 + c8) : l5 + f7 + (a4 === -2 ? (i3.push(void 0), h4) : c8);
  }
  let d6 = r3 + (n4[e3] || "<?>") + (t3 === 2 ? "</svg>" : "");
  if (!Array.isArray(n4) || !n4.hasOwnProperty("raw"))
    throw Error("invalid template strings array");
  return [G9 !== void 0 ? G9.createHTML(d6) : d6, i3];
};
var T6 = class {
  constructor({ strings: t3, _$litType$: e3 }, i3) {
    let s2;
    this.parts = [];
    let r3 = 0, o3 = 0, d6 = t3.length - 1, h4 = this.parts, [l5, _10] = nt7(t3, e3);
    if (this.el = T6.createElement(l5, i3), y12.currentNode = this.el.content, e3 === 2) {
      let $5 = this.el.content, a4 = $5.firstChild;
      a4.remove(), $5.append(...a4.childNodes);
    }
    for (; (s2 = y12.nextNode()) !== null && h4.length < d6; ) {
      if (s2.nodeType === 1) {
        if (s2.hasAttributes()) {
          let $5 = [];
          for (let a4 of s2.getAttributeNames())
            if (a4.endsWith(B8) || a4.startsWith(f7)) {
              let u7 = _10[o3++];
              if ($5.push(a4), u7 !== void 0) {
                let c8 = s2.getAttribute(u7.toLowerCase() + B8).split(f7), A5 = /([.?@])?(.*)/.exec(u7);
                h4.push({ type: 1, index: r3, name: A5[2], strings: c8, ctor: A5[1] === "." ? U8 : A5[1] === "?" ? D8 : A5[1] === "@" ? L7 : b6 });
              } else
                h4.push({ type: 6, index: r3 });
            }
          for (let a4 of $5)
            s2.removeAttribute(a4);
        }
        if (it6.test(s2.tagName)) {
          let $5 = s2.textContent.split(f7), a4 = $5.length - 1;
          if (a4 > 0) {
            s2.textContent = P7 ? P7.emptyScript : "";
            for (let u7 = 0; u7 < a4; u7++)
              s2.append($5[u7], I6()), y12.nextNode(), h4.push({ type: 2, index: ++r3 });
            s2.append($5[a4], I6());
          }
        }
      } else if (s2.nodeType === 8)
        if (s2.data === z7)
          h4.push({ type: 2, index: r3 });
        else {
          let $5 = -1;
          for (; ($5 = s2.data.indexOf(f7, $5 + 1)) !== -1; )
            h4.push({ type: 7, index: r3 }), $5 += f7.length - 1;
        }
      r3++;
    }
  }
  static createElement(t3, e3) {
    let i3 = x7.createElement("template");
    return i3.innerHTML = t3, i3;
  }
};
function C10(n4, t3, e3 = n4, i3) {
  var s2, r3, o3, d6;
  if (t3 === H9)
    return t3;
  let h4 = i3 !== void 0 ? (s2 = e3._$Co) === null || s2 === void 0 ? void 0 : s2[i3] : e3._$Cl, l5 = M7(t3) ? void 0 : t3._$litDirective$;
  return h4?.constructor !== l5 && ((r3 = h4?._$AO) === null || r3 === void 0 || r3.call(h4, false), l5 === void 0 ? h4 = void 0 : (h4 = new l5(n4), h4._$AT(n4, e3, i3)), i3 !== void 0 ? ((o3 = (d6 = e3)._$Co) !== null && o3 !== void 0 ? o3 : d6._$Co = [])[i3] = h4 : e3._$Cl = h4), h4 !== void 0 && (t3 = C10(n4, h4._$AS(n4, t3.values), h4, i3)), t3;
}
var R7 = class {
  constructor(t3, e3) {
    this._$AV = [], this._$AN = void 0, this._$AD = t3, this._$AM = e3;
  }
  get parentNode() {
    return this._$AM.parentNode;
  }
  get _$AU() {
    return this._$AM._$AU;
  }
  u(t3) {
    var e3;
    let { el: { content: i3 }, parts: s2 } = this._$AD, r3 = ((e3 = t3?.creationScope) !== null && e3 !== void 0 ? e3 : x7).importNode(i3, true);
    y12.currentNode = r3;
    let o3 = y12.nextNode(), d6 = 0, h4 = 0, l5 = s2[0];
    for (; l5 !== void 0; ) {
      if (d6 === l5.index) {
        let _10;
        l5.type === 2 ? _10 = new N7(o3, o3.nextSibling, this, t3) : l5.type === 1 ? _10 = new l5.ctor(o3, l5.name, l5.strings, this, t3) : l5.type === 6 && (_10 = new V6(o3, this, t3)), this._$AV.push(_10), l5 = s2[++h4];
      }
      d6 !== l5?.index && (o3 = y12.nextNode(), d6++);
    }
    return y12.currentNode = x7, r3;
  }
  v(t3) {
    let e3 = 0;
    for (let i3 of this._$AV)
      i3 !== void 0 && (i3.strings !== void 0 ? (i3._$AI(t3, i3, e3), e3 += i3.strings.length - 2) : i3._$AI(t3[e3])), e3++;
  }
};
var N7 = class {
  constructor(t3, e3, i3, s2) {
    var r3;
    this.type = 2, this._$AH = v11, this._$AN = void 0, this._$AA = t3, this._$AB = e3, this._$AM = i3, this.options = s2, this._$Cp = (r3 = s2?.isConnected) === null || r3 === void 0 || r3;
  }
  get _$AU() {
    var t3, e3;
    return (e3 = (t3 = this._$AM) === null || t3 === void 0 ? void 0 : t3._$AU) !== null && e3 !== void 0 ? e3 : this._$Cp;
  }
  get parentNode() {
    let t3 = this._$AA.parentNode, e3 = this._$AM;
    return e3 !== void 0 && t3?.nodeType === 11 && (t3 = e3.parentNode), t3;
  }
  get startNode() {
    return this._$AA;
  }
  get endNode() {
    return this._$AB;
  }
  _$AI(t3, e3 = this) {
    t3 = C10(this, t3, e3), M7(t3) ? t3 === v11 || t3 == null || t3 === "" ? (this._$AH !== v11 && this._$AR(), this._$AH = v11) : t3 !== this._$AH && t3 !== H9 && this._(t3) : t3._$litType$ !== void 0 ? this.g(t3) : t3.nodeType !== void 0 ? this.$(t3) : et8(t3) ? this.T(t3) : this._(t3);
  }
  k(t3) {
    return this._$AA.parentNode.insertBefore(t3, this._$AB);
  }
  $(t3) {
    this._$AH !== t3 && (this._$AR(), this._$AH = this.k(t3));
  }
  _(t3) {
    this._$AH !== v11 && M7(this._$AH) ? this._$AA.nextSibling.data = t3 : this.$(x7.createTextNode(t3)), this._$AH = t3;
  }
  g(t3) {
    var e3;
    let { values: i3, _$litType$: s2 } = t3, r3 = typeof s2 == "number" ? this._$AC(t3) : (s2.el === void 0 && (s2.el = T6.createElement(s2.h, this.options)), s2);
    if (((e3 = this._$AH) === null || e3 === void 0 ? void 0 : e3._$AD) === r3)
      this._$AH.v(i3);
    else {
      let o3 = new R7(r3, this), d6 = o3.u(this.options);
      o3.v(i3), this.$(d6), this._$AH = o3;
    }
  }
  _$AC(t3) {
    let e3 = K8.get(t3.strings);
    return e3 === void 0 && K8.set(t3.strings, e3 = new T6(t3)), e3;
  }
  T(t3) {
    tt7(this._$AH) || (this._$AH = [], this._$AR());
    let e3 = this._$AH, i3, s2 = 0;
    for (let r3 of t3)
      s2 === e3.length ? e3.push(i3 = new N7(this.k(I6()), this.k(I6()), this, this.options)) : i3 = e3[s2], i3._$AI(r3), s2++;
    s2 < e3.length && (this._$AR(i3 && i3._$AB.nextSibling, s2), e3.length = s2);
  }
  _$AR(t3 = this._$AA.nextSibling, e3) {
    var i3;
    for ((i3 = this._$AP) === null || i3 === void 0 || i3.call(this, false, true, e3); t3 && t3 !== this._$AB; ) {
      let s2 = t3.nextSibling;
      t3.remove(), t3 = s2;
    }
  }
  setConnected(t3) {
    var e3;
    this._$AM === void 0 && (this._$Cp = t3, (e3 = this._$AP) === null || e3 === void 0 || e3.call(this, t3));
  }
};
var b6 = class {
  constructor(t3, e3, i3, s2, r3) {
    this.type = 1, this._$AH = v11, this._$AN = void 0, this.element = t3, this.name = e3, this._$AM = s2, this.options = r3, i3.length > 2 || i3[0] !== "" || i3[1] !== "" ? (this._$AH = Array(i3.length - 1).fill(new String()), this.strings = i3) : this._$AH = v11;
  }
  get tagName() {
    return this.element.tagName;
  }
  get _$AU() {
    return this._$AM._$AU;
  }
  _$AI(t3, e3 = this, i3, s2) {
    let r3 = this.strings, o3 = false;
    if (r3 === void 0)
      t3 = C10(this, t3, e3, 0), o3 = !M7(t3) || t3 !== this._$AH && t3 !== H9, o3 && (this._$AH = t3);
    else {
      let d6 = t3, h4, l5;
      for (t3 = r3[0], h4 = 0; h4 < r3.length - 1; h4++)
        l5 = C10(this, d6[i3 + h4], e3, h4), l5 === H9 && (l5 = this._$AH[h4]), o3 || (o3 = !M7(l5) || l5 !== this._$AH[h4]), l5 === v11 ? t3 = v11 : t3 !== v11 && (t3 += (l5 ?? "") + r3[h4 + 1]), this._$AH[h4] = l5;
    }
    o3 && !s2 && this.j(t3);
  }
  j(t3) {
    t3 === v11 ? this.element.removeAttribute(this.name) : this.element.setAttribute(this.name, t3 ?? "");
  }
};
var U8 = class extends b6 {
  constructor() {
    super(...arguments), this.type = 3;
  }
  j(t3) {
    this.element[this.name] = t3 === v11 ? void 0 : t3;
  }
};
var ct3 = P7 ? P7.emptyScript : "";
var D8 = class extends b6 {
  constructor() {
    super(...arguments), this.type = 4;
  }
  j(t3) {
    t3 && t3 !== v11 ? this.element.setAttribute(this.name, ct3) : this.element.removeAttribute(this.name);
  }
};
var L7 = class extends b6 {
  constructor(t3, e3, i3, s2, r3) {
    super(t3, e3, i3, s2, r3), this.type = 5;
  }
  _$AI(t3, e3 = this) {
    var i3;
    if ((t3 = (i3 = C10(this, t3, e3, 0)) !== null && i3 !== void 0 ? i3 : v11) === H9)
      return;
    let s2 = this._$AH, r3 = t3 === v11 && s2 !== v11 || t3.capture !== s2.capture || t3.once !== s2.once || t3.passive !== s2.passive, o3 = t3 !== v11 && (s2 === v11 || r3);
    r3 && this.element.removeEventListener(this.name, this, s2), o3 && this.element.addEventListener(this.name, this, t3), this._$AH = t3;
  }
  handleEvent(t3) {
    var e3, i3;
    typeof this._$AH == "function" ? this._$AH.call((i3 = (e3 = this.options) === null || e3 === void 0 ? void 0 : e3.host) !== null && i3 !== void 0 ? i3 : this.element, t3) : this._$AH.handleEvent(t3);
  }
};
var V6 = class {
  constructor(t3, e3, i3) {
    this.element = t3, this.type = 6, this._$AN = void 0, this._$AM = e3, this.options = i3;
  }
  get _$AU() {
    return this._$AM._$AU;
  }
  _$AI(t3) {
    C10(this, t3);
  }
};
var ot6 = { O: B8, P: f7, A: z7, C: 1, M: nt7, L: R7, D: et8, R: C10, I: N7, V: b6, H: D8, N: L7, U: U8, F: V6 };
var X7 = S8.litHtmlPolyfillSupport;
X7?.(T6, N7), ((W7 = S8.litHtmlVersions) !== null && W7 !== void 0 ? W7 : S8.litHtmlVersions = []).push("2.7.4");
var rt5 = { ATTRIBUTE: 1, CHILD: 2, PROPERTY: 3, BOOLEAN_ATTRIBUTE: 4, EVENT: 5, ELEMENT: 6 };
var lt3 = (n4) => (...t3) => ({ _$litDirective$: n4, values: t3 });
var j9 = class {
  constructor(t3) {
  }
  get _$AU() {
    return this._$AM._$AU;
  }
  _$AT(t3, e3, i3) {
    this._$Ct = t3, this._$AM = e3, this._$Ci = i3;
  }
  _$AS(t3, e3) {
    return this.update(t3, e3);
  }
  update(t3, e3) {
    return this.render(...e3);
  }
};
var { I: At2 } = ot6;
var ht3 = () => document.createComment("");
var E7 = (n4, t3, e3) => {
  var i3;
  let s2 = n4._$AA.parentNode, r3 = t3 === void 0 ? n4._$AB : t3._$AA;
  if (e3 === void 0) {
    let o3 = s2.insertBefore(ht3(), r3), d6 = s2.insertBefore(ht3(), r3);
    e3 = new At2(o3, d6, n4, n4.options);
  } else {
    let o3 = e3._$AB.nextSibling, d6 = e3._$AM, h4 = d6 !== n4;
    if (h4) {
      let l5;
      (i3 = e3._$AQ) === null || i3 === void 0 || i3.call(e3, n4), e3._$AM = n4, e3._$AP !== void 0 && (l5 = n4._$AU) !== d6._$AU && e3._$AP(l5);
    }
    if (o3 !== r3 || h4) {
      let l5 = e3._$AA;
      for (; l5 !== o3; ) {
        let _10 = l5.nextSibling;
        s2.insertBefore(l5, r3), l5 = _10;
      }
    }
  }
  return e3;
};
var m9 = (n4, t3, e3 = n4) => (n4._$AI(t3, e3), n4);
var vt2 = {};
var at4 = (n4, t3 = vt2) => n4._$AH = t3;
var dt4 = (n4) => n4._$AH;
var k9 = (n4) => {
  var t3;
  (t3 = n4._$AP) === null || t3 === void 0 || t3.call(n4, false, true);
  let e3 = n4._$AA, i3 = n4._$AB.nextSibling;
  for (; e3 !== i3; ) {
    let s2 = e3.nextSibling;
    e3.remove(), e3 = s2;
  }
};
var $t3 = (n4, t3, e3) => {
  let i3 = /* @__PURE__ */ new Map();
  for (let s2 = t3; s2 <= e3; s2++)
    i3.set(n4[s2], s2);
  return i3;
};
var Ct4 = lt3(class extends j9 {
  constructor(n4) {
    if (super(n4), n4.type !== rt5.CHILD)
      throw Error("repeat() can only be used in text expressions");
  }
  dt(n4, t3, e3) {
    let i3;
    e3 === void 0 ? e3 = t3 : t3 !== void 0 && (i3 = t3);
    let s2 = [], r3 = [], o3 = 0;
    for (let d6 of n4)
      s2[o3] = i3 ? i3(d6, o3) : o3, r3[o3] = e3(d6, o3), o3++;
    return { values: r3, keys: s2 };
  }
  render(n4, t3, e3) {
    return this.dt(n4, t3, e3).values;
  }
  update(n4, [t3, e3, i3]) {
    var s2;
    let r3 = dt4(n4), { values: o3, keys: d6 } = this.dt(t3, e3, i3);
    if (!Array.isArray(r3))
      return this.ht = d6, o3;
    let h4 = (s2 = this.ht) !== null && s2 !== void 0 ? s2 : this.ht = [], l5 = [], _10, $5, a4 = 0, u7 = r3.length - 1, c8 = 0, A5 = o3.length - 1;
    for (; a4 <= u7 && c8 <= A5; )
      if (r3[a4] === null)
        a4++;
      else if (r3[u7] === null)
        u7--;
      else if (h4[a4] === d6[c8])
        l5[c8] = m9(r3[a4], o3[c8]), a4++, c8++;
      else if (h4[u7] === d6[A5])
        l5[A5] = m9(r3[u7], o3[A5]), u7--, A5--;
      else if (h4[a4] === d6[A5])
        l5[A5] = m9(r3[a4], o3[A5]), E7(n4, l5[A5 + 1], r3[a4]), a4++, A5--;
      else if (h4[u7] === d6[c8])
        l5[c8] = m9(r3[u7], o3[c8]), E7(n4, r3[a4], r3[u7]), u7--, c8++;
      else if (_10 === void 0 && (_10 = $t3(d6, c8, A5), $5 = $t3(h4, a4, u7)), _10.has(h4[a4]))
        if (_10.has(h4[u7])) {
          let p10 = $5.get(d6[c8]), O7 = p10 !== void 0 ? r3[p10] : null;
          if (O7 === null) {
            let F7 = E7(n4, r3[a4]);
            m9(F7, o3[c8]), l5[c8] = F7;
          } else
            l5[c8] = m9(O7, o3[c8]), E7(n4, r3[a4], O7), r3[p10] = null;
          c8++;
        } else
          k9(r3[u7]), u7--;
      else
        k9(r3[a4]), a4++;
    for (; c8 <= A5; ) {
      let p10 = E7(n4, l5[A5 + 1]);
      m9(p10, o3[c8]), l5[c8++] = p10;
    }
    for (; a4 <= u7; ) {
      let p10 = r3[a4++];
      p10 !== null && k9(p10);
    }
    return this.ht = d6, at4(n4, l5), H9;
  }
});

// src/emulator/multi-webview-comp-virtual-keyboard.html.ts
var TAG8 = "multi-webview-comp-virtual-keyboard";
var MultiWebviewCompVirtualKeyboard = class extends n {
  constructor() {
    super(...arguments);
    this._visible = false;
    this._overlay = false;
    this._navigation_bar_height = 0;
    this.timer = 0;
    this.requestId = 0;
    this.insets = {
      left: 0,
      top: 0,
      right: 0,
      bottom: 0
    };
    this.maxHeight = 0;
    this.row1Keys = ["q", "w", "e", "r", "t", "y", "u", "i", "o", "p"];
    this.row2Keys = ["a", "s", "d", "f", "g", "h", "j", "k", "l"];
    this.row3Keys = ["&#8679", "z", "x", "c", "v", "b", "n", "m", "&#10005"];
    this.row4Keys = ["123", "&#128512", "space", "search"];
  }
  setHostStyle() {
    const host = this.renderRoot.host;
    host.style.position = this._overlay ? "absolute" : "relative";
    host.style.overflow = this._visible ? "visible" : "hidden";
  }
  firstUpdated() {
    this.setCSSVar();
    this.dispatchEvent(new Event("first-updated"));
  }
  setCSSVar() {
    if (!this._elContainer)
      throw new Error(`this._elContainer === null`);
    const rowWidth = this._elContainer.getBoundingClientRect().width;
    const alphabetWidth = rowWidth / 11;
    const alphabetHeight = alphabetWidth * 1;
    const rowPaddingVertical = 3;
    const rowPaddingHorizontal = 2;
    this.maxHeight = (alphabetHeight + rowPaddingVertical * 2) * 4 + alphabetHeight;
    [
      ["--key-alphabet-width", alphabetWidth],
      ["--key-alphabet-height", alphabetHeight],
      ["--row-padding-vertical", rowPaddingVertical],
      ["--row-padding-horizontal", rowPaddingHorizontal],
      ["--height", this._navigation_bar_height]
    ].forEach(([propertyName, n4]) => {
      this._elContainer?.style.setProperty(propertyName, n4 + "px");
    });
    return this;
  }
  repeatGetKey(item) {
    return item;
  }
  createElement(classname, key) {
    const div = document.createElement("div");
    div.setAttribute("class", classname);
    div.innerHTML = key;
    return div;
  }
  createElementForRow3(classNameSymbol, classNameAlphabet, key) {
    return this.createElement(
      key.startsWith("&") ? classNameSymbol : classNameAlphabet,
      key
    );
  }
  createElementForRow4(classNameSymbol, classNameSpace, classNameSearch, key) {
    return this.createElement(
      key.startsWith("1") || key.startsWith("&") ? classNameSymbol : key === "space" ? classNameSpace : classNameSearch,
      key
    );
  }
  transitionstart() {
    this.timer = setInterval(() => {
      this.dispatchEvent(new Event("height-changed"));
    }, 16);
  }
  transitionend() {
    this.dispatchEvent(
      new Event(this._visible ? "show-completed" : "hide-completed")
    );
    clearInterval(this.timer);
    this.dispatchEvent(new Event("height-changed"));
  }
  render() {
    this.setHostStyle();
    const containerClassMap = {
      container: true,
      container_active: this._visible
    };
    return tt3`
      <div
        class="${at3(containerClassMap)}"
        @transitionstart=${this.transitionstart}
        @transitionend=${this.transitionend}
      >
        <div class="row line-1">
          ${Ct4(
      this.row1Keys,
      this.repeatGetKey,
      this.createElement.bind(this, "key-alphabet")
    )}
        </div>
        <div class="row line-2">
          ${Ct4(
      this.row2Keys,
      this.repeatGetKey,
      this.createElement.bind(this, "key-alphabet")
    )}
        </div>
        <div class="row line-3">
          ${Ct4(
      this.row3Keys,
      this.repeatGetKey,
      this.createElementForRow3.bind(this, "key-symbol", "key-alphabet")
    )}
        </div>
        <div class="row line-4">
          ${Ct4(
      this.row4Keys,
      this.repeatGetKey,
      this.createElementForRow4.bind(
        this,
        "key-symbol",
        "key-space",
        "key-search"
      )
    )}
        </div>
      </div>
    `;
  }
};
MultiWebviewCompVirtualKeyboard.styles = createAllCSS8();
__decorateClass([
  y7(".container")
], MultiWebviewCompVirtualKeyboard.prototype, "_elContainer", 2);
__decorateClass([
  o({ type: Boolean })
], MultiWebviewCompVirtualKeyboard.prototype, "_visible", 2);
__decorateClass([
  o({ type: Boolean })
], MultiWebviewCompVirtualKeyboard.prototype, "_overlay", 2);
__decorateClass([
  o({ type: Number })
], MultiWebviewCompVirtualKeyboard.prototype, "_navigation_bar_height", 2);
MultiWebviewCompVirtualKeyboard = __decorateClass([
  c5(TAG8)
], MultiWebviewCompVirtualKeyboard);
function createAllCSS8() {
  return [
    C7`
      :host {
        left: 0px;
        bottom: 0px;
        width: 100%;
      }

      .container {
        --key-alphabet-width: 0px;
        --key-alphabet-height: 0px;
        --row-padding-vertical: 3px;
        --row-padding-horizontal: 2px;
        --border-radius: 3px;
        --height: 0px;
        margin: 0px;
        height: var(--height);
        transition: all 0.25s ease-out;
        overflow: hidden;
        background: #999999;
      }

      .container_active {
        height: calc(
          (var(--key-alphabet-height) + var(--row-padding-vertical) * 2) * 4 +
            var(--key-alphabet-height)
        );
      }

      .row {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: var(--row-padding-vertical) var(--row-padding-horizontal);
      }

      .key-alphabet {
        display: flex;
        justify-content: center;
        align-items: center;
        width: var(--key-alphabet-width);
        height: var(--key-alphabet-height);
        border-radius: var(--border-radius);
        background: #fff;
      }

      .line-2 {
        padding: var(--row-padding-vertical)
          calc(var(--row-padding-horizontal) + var(--key-alphabet-width) / 2);
      }

      .key-symbol {
        --margin-horizontal: calc(var(--key-alphabet-width) * 0.3);
        display: flex;
        justify-content: center;
        align-items: center;
        width: calc(var(--key-alphabet-width) * 1.2);
        height: var(--key-alphabet-height);
        border-radius: var(--border-radius);
        background: #aaa;
      }

      .key-symbol:first-child {
        margin-right: var(--margin-horizontal);
      }

      .key-symbol:last-child {
        margin-left: var(--margin-horizontal);
      }

      .line-4 .key-symbol:first-child {
        margin-right: 0px;
      }

      .line-4 .key-symbol:nth-of-type(2) {
        width: calc(var(--key-alphabet-width) * 1.3);
      }

      .key-space {
        display: flex;
        justify-content: center;
        align-items: center;
        border-radius: var(--border-radius);
        width: calc(var(--key-alphabet-width) * 6);
        height: var(--key-alphabet-height);
        background: #fff;
      }

      .key-search {
        width: calc(var(--key-alphabet-width) * 2);
        height: var(--key-alphabet-height);
        display: flex;
        justify-content: center;
        align-items: center;
        border-radius: var(--border-radius);
        background: #4096ff;
        color: #fff;
      }
    `
  ];
}

// src/emulator/index.html.ts
var TAG9 = "root-comp";
var RootComp = class extends n {
  constructor() {
    super(...arguments);
    /**statusBar */
    this.statusBarController = new StatusBarController().onUpdate(() => {
      this.requestUpdate();
    });
    /**navigationBar */
    this.navigationController = new NavigationBarController().onUpdate(() => {
      this.requestUpdate();
    });
    /**virtualboard */
    this.virtualKeyboardController = new VirtualKeyboardController().onUpdate(
      () => this.requestUpdate()
    );
    this.torchController = new TorchController().onUpdate(() => {
      this.requestUpdate();
    });
    this.hapticsController = new HapticsController();
    this.biometricsController = new BiometricsController().onUpdate(
      () => this.requestUpdate()
    );
  }
  get statusBarState() {
    return this.statusBarController.state;
  }
  get navigationBarState() {
    return this.navigationController.state;
  }
  get virtualKeyboardState() {
    return this.virtualKeyboardController.state;
  }
  get torchState() {
    return this.torchController.state;
  }
  render() {
    return tt3`
      <multi-webview-comp-mobile-shell>
        ${t2(this.biometricsController.state, () => {
      const state = this.biometricsController.state;
      tt3`<multi-webview-comp-biometrics
            @pass=${state.resolve({ success: true, message: "okk" })}
            @no-pass=${state.resolve({ success: false, message: "...." })}
          ></multi-webview-comp-biometrics>`;
    })}
        <multi-webview-comp-status-bar
          slot="status-bar"
          ._color=${this.statusBarState.color}
          ._style=${this.statusBarState.style}
          ._overlay=${this.statusBarState.overlay}
          ._visible=${this.statusBarState.visible}
          ._insets=${this.statusBarState.insets}
          ._torchIsOpen=${this.torchState.isOpen}
        ></multi-webview-comp-status-bar>
        <slot slot="shell-content"></slot>
        ${t2(
      this.virtualKeyboardController.isShowVirtualKeyboard,
      () => tt3`
            <multi-webview-comp-virtual-keyboard
              slot="bottom-bar"
              ._visible=${this.virtualKeyboardState.visible}
              ._overlay=${this.virtualKeyboardState.overlay}
              @first-updated=${this.virtualKeyboardController.virtualKeyboardFirstUpdated}
              @hide-completed=${this.virtualKeyboardController.virtualKeyboardHideCompleted}
              @show-completed=${this.virtualKeyboardController.virtualKeyboardShowCompleted}
            ></multi-webview-comp-virtual-keyboard>
          `,
      () => {
        return tt3`
              <multi-webview-comp-navigation-bar
                slot="bottom-bar"
                ._color=${this.navigationBarState.color}
                ._style=${this.navigationBarState.style}
                ._overlay=${this.navigationBarState.overlay}
                ._visible=${this.navigationBarState.visible}
                ._inserts=${this.navigationBarState.insets}
              ></multi-webview-comp-navigation-bar>
            `;
      }
    )}
      </multi-webview-comp-mobile-shell>
    `;
  }
};
RootComp.styles = createAllCSS9();
RootComp = __decorateClass([
  c5(TAG9)
], RootComp);
function createAllCSS9() {
  return [
    C7`
      :host {
        display: block;
      }
    `
  ];
}
//! use zod error: Relative import path "zod" not prefixed with / or ./ or ../ only remote
//! https://github.com/denoland/deno/issues/17598
/*! Bundled license information:

ieee754/index.js:
  (*! ieee754. BSD-3-Clause License. Feross Aboukhadijeh <https://feross.org/opensource> *)
*/
/*! Bundled license information:

buffer/index.js:
  (*!
   * The buffer module from node.js, for the browser.
   *
   * @author   Feross Aboukhadijeh <https://feross.org>
   * @license  MIT
   *)
*/
/*! Bundled license information:

@lit/reactive-element/css-tag.js:
  (**
   * @license
   * Copyright 2019 Google LLC
   * SPDX-License-Identifier: BSD-3-Clause
   *)

@lit/reactive-element/reactive-element.js:
  (**
   * @license
   * Copyright 2017 Google LLC
   * SPDX-License-Identifier: BSD-3-Clause
   *)
*/
/*! Bundled license information:

lit-html/lit-html.js:
  (**
   * @license
   * Copyright 2017 Google LLC
   * SPDX-License-Identifier: BSD-3-Clause
   *)
*/
/*! Bundled license information:

lit-element/lit-element.js:
  (**
   * @license
   * Copyright 2017 Google LLC
   * SPDX-License-Identifier: BSD-3-Clause
   *)
*/
/*! Bundled license information:

lit-html/is-server.js:
  (**
   * @license
   * Copyright 2022 Google LLC
   * SPDX-License-Identifier: BSD-3-Clause
   *)
*/
/*! Bundled license information:

@lit/reactive-element/decorators/custom-element.js:
  (**
   * @license
   * Copyright 2017 Google LLC
   * SPDX-License-Identifier: BSD-3-Clause
   *)
*/
/*! Bundled license information:

@lit/reactive-element/decorators/property.js:
  (**
   * @license
   * Copyright 2017 Google LLC
   * SPDX-License-Identifier: BSD-3-Clause
   *)
*/
/*! Bundled license information:

@lit/reactive-element/decorators/property.js:
  (**
   * @license
   * Copyright 2017 Google LLC
   * SPDX-License-Identifier: BSD-3-Clause
   *)

@lit/reactive-element/decorators/state.js:
  (**
   * @license
   * Copyright 2017 Google LLC
   * SPDX-License-Identifier: BSD-3-Clause
   *)
*/
/*! Bundled license information:

@lit/reactive-element/decorators/base.js:
  (**
   * @license
   * Copyright 2017 Google LLC
   * SPDX-License-Identifier: BSD-3-Clause
   *)

@lit/reactive-element/decorators/event-options.js:
  (**
   * @license
   * Copyright 2017 Google LLC
   * SPDX-License-Identifier: BSD-3-Clause
   *)
*/
/*! Bundled license information:

@lit/reactive-element/decorators/base.js:
  (**
   * @license
   * Copyright 2017 Google LLC
   * SPDX-License-Identifier: BSD-3-Clause
   *)

@lit/reactive-element/decorators/query.js:
  (**
   * @license
   * Copyright 2017 Google LLC
   * SPDX-License-Identifier: BSD-3-Clause
   *)
*/
/*! Bundled license information:

@lit/reactive-element/decorators/base.js:
  (**
   * @license
   * Copyright 2017 Google LLC
   * SPDX-License-Identifier: BSD-3-Clause
   *)

@lit/reactive-element/decorators/query-all.js:
  (**
   * @license
   * Copyright 2017 Google LLC
   * SPDX-License-Identifier: BSD-3-Clause
   *)
*/
/*! Bundled license information:

@lit/reactive-element/decorators/base.js:
  (**
   * @license
   * Copyright 2017 Google LLC
   * SPDX-License-Identifier: BSD-3-Clause
   *)

@lit/reactive-element/decorators/query-async.js:
  (**
   * @license
   * Copyright 2017 Google LLC
   * SPDX-License-Identifier: BSD-3-Clause
   *)
*/
/*! Bundled license information:

@lit/reactive-element/decorators/base.js:
  (**
   * @license
   * Copyright 2017 Google LLC
   * SPDX-License-Identifier: BSD-3-Clause
   *)

@lit/reactive-element/decorators/query-assigned-elements.js:
  (**
   * @license
   * Copyright 2021 Google LLC
   * SPDX-License-Identifier: BSD-3-Clause
   *)
*/
/*! Bundled license information:

@lit/reactive-element/decorators/base.js:
  (**
   * @license
   * Copyright 2017 Google LLC
   * SPDX-License-Identifier: BSD-3-Clause
   *)

@lit/reactive-element/decorators/query-assigned-elements.js:
  (**
   * @license
   * Copyright 2021 Google LLC
   * SPDX-License-Identifier: BSD-3-Clause
   *)

@lit/reactive-element/decorators/query-assigned-nodes.js:
  (**
   * @license
   * Copyright 2017 Google LLC
   * SPDX-License-Identifier: BSD-3-Clause
   *)
*/
/*! Bundled license information:

lit-html/directives/when.js:
  (**
   * @license
   * Copyright 2021 Google LLC
   * SPDX-License-Identifier: BSD-3-Clause
   *)
*/
/*! Bundled license information:

lit-html/lit-html.js:
  (**
   * @license
   * Copyright 2017 Google LLC
   * SPDX-License-Identifier: BSD-3-Clause
   *)

lit-html/static.js:
  (**
   * @license
   * Copyright 2020 Google LLC
   * SPDX-License-Identifier: BSD-3-Clause
   *)
*/
/*! Bundled license information:

lit-html/lit-html.js:
  (**
   * @license
   * Copyright 2017 Google LLC
   * SPDX-License-Identifier: BSD-3-Clause
   *)

lit-html/directive.js:
  (**
   * @license
   * Copyright 2017 Google LLC
   * SPDX-License-Identifier: BSD-3-Clause
   *)

lit-html/directives/style-map.js:
  (**
   * @license
   * Copyright 2018 Google LLC
   * SPDX-License-Identifier: BSD-3-Clause
   *)
*/
/*! Bundled license information:

lit-html/lit-html.js:
  (**
   * @license
   * Copyright 2017 Google LLC
   * SPDX-License-Identifier: BSD-3-Clause
   *)

lit-html/directive.js:
  (**
   * @license
   * Copyright 2017 Google LLC
   * SPDX-License-Identifier: BSD-3-Clause
   *)

lit-html/directives/class-map.js:
  (**
   * @license
   * Copyright 2018 Google LLC
   * SPDX-License-Identifier: BSD-3-Clause
   *)
*/
/*! Bundled license information:

lit-html/lit-html.js:
  (**
   * @license
   * Copyright 2017 Google LLC
   * SPDX-License-Identifier: BSD-3-Clause
   *)

lit-html/directive.js:
  (**
   * @license
   * Copyright 2017 Google LLC
   * SPDX-License-Identifier: BSD-3-Clause
   *)

lit-html/directive-helpers.js:
  (**
   * @license
   * Copyright 2020 Google LLC
   * SPDX-License-Identifier: BSD-3-Clause
   *)

lit-html/directives/repeat.js:
  (**
   * @license
   * Copyright 2017 Google LLC
   * SPDX-License-Identifier: BSD-3-Clause
   *)
*/
