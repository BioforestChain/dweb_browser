var __defProp = Object.defineProperty;
var __getOwnPropDesc = Object.getOwnPropertyDescriptor;
var __require = /* @__PURE__ */ ((x8) => typeof require !== "undefined" ? require : typeof Proxy !== "undefined" ? new Proxy(x8, {
  get: (a3, b6) => (typeof require !== "undefined" ? require : a3)[b6]
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
  static resolve(v11) {
    const po = new PromiseOut();
    po.resolve(v11);
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
  return `#${rgbaColor.map((v11) => (v11 & 255).toString(16).padStart(2, "0")).join("")}`;
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
  s2.assertEqual = (n2) => n2;
  function e3(n2) {
  }
  s2.assertIs = e3;
  function t2(n2) {
    throw new Error();
  }
  s2.assertNever = t2, s2.arrayToEnum = (n2) => {
    let a3 = {};
    for (let i3 of n2)
      a3[i3] = i3;
    return a3;
  }, s2.getValidEnumValues = (n2) => {
    let a3 = s2.objectKeys(n2).filter((o) => typeof n2[n2[o]] != "number"), i3 = {};
    for (let o of a3)
      i3[o] = n2[o];
    return s2.objectValues(i3);
  }, s2.objectValues = (n2) => s2.objectKeys(n2).map(function(a3) {
    return n2[a3];
  }), s2.objectKeys = typeof Object.keys == "function" ? (n2) => Object.keys(n2) : (n2) => {
    let a3 = [];
    for (let i3 in n2)
      Object.prototype.hasOwnProperty.call(n2, i3) && a3.push(i3);
    return a3;
  }, s2.find = (n2, a3) => {
    for (let i3 of n2)
      if (a3(i3))
        return i3;
  }, s2.isInteger = typeof Number.isInteger == "function" ? (n2) => Number.isInteger(n2) : (n2) => typeof n2 == "number" && isFinite(n2) && Math.floor(n2) === n2;
  function r3(n2, a3 = " | ") {
    return n2.map((i3) => typeof i3 == "string" ? `'${i3}'` : i3).join(a3);
  }
  s2.joinValues = r3, s2.jsonStringifyReplacer = (n2, a3) => typeof a3 == "bigint" ? a3.toString() : a3;
})(g || (g = {}));
var me;
(function(s2) {
  s2.mergeShapes = (e3, t2) => ({ ...e3, ...t2 });
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
    let t2 = new.target.prototype;
    Object.setPrototypeOf ? Object.setPrototypeOf(this, t2) : this.__proto__ = t2, this.name = "ZodError", this.issues = e3;
  }
  get errors() {
    return this.issues;
  }
  format(e3) {
    let t2 = e3 || function(a3) {
      return a3.message;
    }, r3 = { _errors: [] }, n2 = (a3) => {
      for (let i3 of a3.issues)
        if (i3.code === "invalid_union")
          i3.unionErrors.map(n2);
        else if (i3.code === "invalid_return_type")
          n2(i3.returnTypeError);
        else if (i3.code === "invalid_arguments")
          n2(i3.argumentsError);
        else if (i3.path.length === 0)
          r3._errors.push(t2(i3));
        else {
          let o = r3, f7 = 0;
          for (; f7 < i3.path.length; ) {
            let l4 = i3.path[f7];
            f7 === i3.path.length - 1 ? (o[l4] = o[l4] || { _errors: [] }, o[l4]._errors.push(t2(i3))) : o[l4] = o[l4] || { _errors: [] }, o = o[l4], f7++;
          }
        }
    };
    return n2(this), r3;
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
  flatten(e3 = (t2) => t2.message) {
    let t2 = {}, r3 = [];
    for (let n2 of this.issues)
      n2.path.length > 0 ? (t2[n2.path[0]] = t2[n2.path[0]] || [], t2[n2.path[0]].push(e3(n2))) : r3.push(e3(n2));
    return { formErrors: r3, fieldErrors: t2 };
  }
  get formErrors() {
    return this.flatten();
  }
};
T.create = (s2) => new T(s2);
var oe = (s2, e3) => {
  let t2;
  switch (s2.code) {
    case c.invalid_type:
      s2.received === d.undefined ? t2 = "Required" : t2 = `Expected ${s2.expected}, received ${s2.received}`;
      break;
    case c.invalid_literal:
      t2 = `Invalid literal value, expected ${JSON.stringify(s2.expected, g.jsonStringifyReplacer)}`;
      break;
    case c.unrecognized_keys:
      t2 = `Unrecognized key(s) in object: ${g.joinValues(s2.keys, ", ")}`;
      break;
    case c.invalid_union:
      t2 = "Invalid input";
      break;
    case c.invalid_union_discriminator:
      t2 = `Invalid discriminator value. Expected ${g.joinValues(s2.options)}`;
      break;
    case c.invalid_enum_value:
      t2 = `Invalid enum value. Expected ${g.joinValues(s2.options)}, received '${s2.received}'`;
      break;
    case c.invalid_arguments:
      t2 = "Invalid function arguments";
      break;
    case c.invalid_return_type:
      t2 = "Invalid function return type";
      break;
    case c.invalid_date:
      t2 = "Invalid date";
      break;
    case c.invalid_string:
      typeof s2.validation == "object" ? "includes" in s2.validation ? (t2 = `Invalid input: must include "${s2.validation.includes}"`, typeof s2.validation.position == "number" && (t2 = `${t2} at one or more positions greater than or equal to ${s2.validation.position}`)) : "startsWith" in s2.validation ? t2 = `Invalid input: must start with "${s2.validation.startsWith}"` : "endsWith" in s2.validation ? t2 = `Invalid input: must end with "${s2.validation.endsWith}"` : g.assertNever(s2.validation) : s2.validation !== "regex" ? t2 = `Invalid ${s2.validation}` : t2 = "Invalid";
      break;
    case c.too_small:
      s2.type === "array" ? t2 = `Array must contain ${s2.exact ? "exactly" : s2.inclusive ? "at least" : "more than"} ${s2.minimum} element(s)` : s2.type === "string" ? t2 = `String must contain ${s2.exact ? "exactly" : s2.inclusive ? "at least" : "over"} ${s2.minimum} character(s)` : s2.type === "number" ? t2 = `Number must be ${s2.exact ? "exactly equal to " : s2.inclusive ? "greater than or equal to " : "greater than "}${s2.minimum}` : s2.type === "date" ? t2 = `Date must be ${s2.exact ? "exactly equal to " : s2.inclusive ? "greater than or equal to " : "greater than "}${new Date(Number(s2.minimum))}` : t2 = "Invalid input";
      break;
    case c.too_big:
      s2.type === "array" ? t2 = `Array must contain ${s2.exact ? "exactly" : s2.inclusive ? "at most" : "less than"} ${s2.maximum} element(s)` : s2.type === "string" ? t2 = `String must contain ${s2.exact ? "exactly" : s2.inclusive ? "at most" : "under"} ${s2.maximum} character(s)` : s2.type === "number" ? t2 = `Number must be ${s2.exact ? "exactly" : s2.inclusive ? "less than or equal to" : "less than"} ${s2.maximum}` : s2.type === "bigint" ? t2 = `BigInt must be ${s2.exact ? "exactly" : s2.inclusive ? "less than or equal to" : "less than"} ${s2.maximum}` : s2.type === "date" ? t2 = `Date must be ${s2.exact ? "exactly" : s2.inclusive ? "smaller than or equal to" : "smaller than"} ${new Date(Number(s2.maximum))}` : t2 = "Invalid input";
      break;
    case c.custom:
      t2 = "Invalid input";
      break;
    case c.invalid_intersection_types:
      t2 = "Intersection results could not be merged";
      break;
    case c.not_multiple_of:
      t2 = `Number must be a multiple of ${s2.multipleOf}`;
      break;
    case c.not_finite:
      t2 = "Number must be finite";
      break;
    default:
      t2 = e3.defaultError, g.assertNever(s2);
  }
  return { message: t2 };
};
var ke = oe;
function Ee(s2) {
  ke = s2;
}
function de() {
  return ke;
}
var ue = (s2) => {
  let { data: e3, path: t2, errorMaps: r3, issueData: n2 } = s2, a3 = [...t2, ...n2.path || []], i3 = { ...n2, path: a3 }, o = "", f7 = r3.filter((l4) => !!l4).slice().reverse();
  for (let l4 of f7)
    o = l4(i3, { data: e3, defaultError: o }).message;
  return { ...n2, path: a3, message: n2.message || o };
};
var Ie = [];
function u(s2, e3) {
  let t2 = ue({ issueData: e3, data: s2.data, path: s2.path, errorMaps: [s2.common.contextualErrorMap, s2.schemaErrorMap, de(), oe].filter((r3) => !!r3) });
  s2.common.issues.push(t2);
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
  static mergeArray(e3, t2) {
    let r3 = [];
    for (let n2 of t2) {
      if (n2.status === "aborted")
        return m;
      n2.status === "dirty" && e3.dirty(), r3.push(n2.value);
    }
    return { status: e3.value, value: r3 };
  }
  static async mergeObjectAsync(e3, t2) {
    let r3 = [];
    for (let n2 of t2)
      r3.push({ key: await n2.key, value: await n2.value });
    return k.mergeObjectSync(e3, r3);
  }
  static mergeObjectSync(e3, t2) {
    let r3 = {};
    for (let n2 of t2) {
      let { key: a3, value: i3 } = n2;
      if (a3.status === "aborted" || i3.status === "aborted")
        return m;
      a3.status === "dirty" && e3.dirty(), i3.status === "dirty" && e3.dirty(), (typeof i3.value < "u" || n2.alwaysSet) && (r3[a3.value] = i3.value);
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
  constructor(e3, t2, r3, n2) {
    this._cachedPath = [], this.parent = e3, this.data = t2, this._path = r3, this._key = n2;
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
    let t2 = new T(s2.common.issues);
    return this._error = t2, this._error;
  } };
};
function y(s2) {
  if (!s2)
    return {};
  let { errorMap: e3, invalid_type_error: t2, required_error: r3, description: n2 } = s2;
  if (e3 && (t2 || r3))
    throw new Error(`Can't use "invalid_type_error" or "required_error" in conjunction with custom error map.`);
  return e3 ? { errorMap: e3, description: n2 } : { errorMap: (i3, o) => i3.code !== "invalid_type" ? { message: o.defaultError } : typeof o.data > "u" ? { message: r3 ?? o.defaultError } : { message: t2 ?? o.defaultError }, description: n2 };
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
  _getOrReturnCtx(e3, t2) {
    return t2 || { common: e3.parent.common, data: e3.data, parsedType: P(e3.data), schemaErrorMap: this._def.errorMap, path: e3.path, parent: e3.parent };
  }
  _processInputParams(e3) {
    return { status: new k(), ctx: { common: e3.parent.common, data: e3.data, parsedType: P(e3.data), schemaErrorMap: this._def.errorMap, path: e3.path, parent: e3.parent } };
  }
  _parseSync(e3) {
    let t2 = this._parse(e3);
    if (fe(t2))
      throw new Error("Synchronous parse encountered promise.");
    return t2;
  }
  _parseAsync(e3) {
    let t2 = this._parse(e3);
    return Promise.resolve(t2);
  }
  parse(e3, t2) {
    let r3 = this.safeParse(e3, t2);
    if (r3.success)
      return r3.data;
    throw r3.error;
  }
  safeParse(e3, t2) {
    var r3;
    let n2 = { common: { issues: [], async: (r3 = t2?.async) !== null && r3 !== void 0 ? r3 : false, contextualErrorMap: t2?.errorMap }, path: t2?.path || [], schemaErrorMap: this._def.errorMap, parent: null, data: e3, parsedType: P(e3) }, a3 = this._parseSync({ data: e3, path: n2.path, parent: n2 });
    return ge(n2, a3);
  }
  async parseAsync(e3, t2) {
    let r3 = await this.safeParseAsync(e3, t2);
    if (r3.success)
      return r3.data;
    throw r3.error;
  }
  async safeParseAsync(e3, t2) {
    let r3 = { common: { issues: [], contextualErrorMap: t2?.errorMap, async: true }, path: t2?.path || [], schemaErrorMap: this._def.errorMap, parent: null, data: e3, parsedType: P(e3) }, n2 = this._parse({ data: e3, path: r3.path, parent: r3 }), a3 = await (fe(n2) ? n2 : Promise.resolve(n2));
    return ge(r3, a3);
  }
  refine(e3, t2) {
    let r3 = (n2) => typeof t2 == "string" || typeof t2 > "u" ? { message: t2 } : typeof t2 == "function" ? t2(n2) : t2;
    return this._refinement((n2, a3) => {
      let i3 = e3(n2), o = () => a3.addIssue({ code: c.custom, ...r3(n2) });
      return typeof Promise < "u" && i3 instanceof Promise ? i3.then((f7) => f7 ? true : (o(), false)) : i3 ? true : (o(), false);
    });
  }
  refinement(e3, t2) {
    return this._refinement((r3, n2) => e3(r3) ? true : (n2.addIssue(typeof t2 == "function" ? t2(r3, n2) : t2), false));
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
    let t2 = typeof e3 == "function" ? e3 : () => e3;
    return new K({ ...y(this._def), innerType: this, defaultValue: t2, typeName: p.ZodDefault });
  }
  brand() {
    return new he({ typeName: p.ZodBranded, type: this, ...y(this._def) });
  }
  catch(e3) {
    let t2 = typeof e3 == "function" ? e3 : () => e3;
    return new ae({ ...y(this._def), innerType: this, catchValue: t2, typeName: p.ZodCatch });
  }
  describe(e3) {
    let t2 = this.constructor;
    return new t2({ ...this._def, description: e3 });
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
    super(...arguments), this._regex = (e3, t2, r3) => this.refinement((n2) => e3.test(n2), { validation: t2, code: c.invalid_string, ...h.errToObj(r3) }), this.nonempty = (e3) => this.min(1, h.errToObj(e3)), this.trim = () => new w({ ...this._def, checks: [...this._def.checks, { kind: "trim" }] }), this.toLowerCase = () => new w({ ...this._def, checks: [...this._def.checks, { kind: "toLowerCase" }] }), this.toUpperCase = () => new w({ ...this._def, checks: [...this._def.checks, { kind: "toUpperCase" }] });
  }
  _parse(e3) {
    if (this._def.coerce && (e3.data = String(e3.data)), this._getType(e3) !== d.string) {
      let a3 = this._getOrReturnCtx(e3);
      return u(a3, { code: c.invalid_type, expected: d.string, received: a3.parsedType }), m;
    }
    let r3 = new k(), n2;
    for (let a3 of this._def.checks)
      if (a3.kind === "min")
        e3.data.length < a3.value && (n2 = this._getOrReturnCtx(e3, n2), u(n2, { code: c.too_small, minimum: a3.value, type: "string", inclusive: true, exact: false, message: a3.message }), r3.dirty());
      else if (a3.kind === "max")
        e3.data.length > a3.value && (n2 = this._getOrReturnCtx(e3, n2), u(n2, { code: c.too_big, maximum: a3.value, type: "string", inclusive: true, exact: false, message: a3.message }), r3.dirty());
      else if (a3.kind === "length") {
        let i3 = e3.data.length > a3.value, o = e3.data.length < a3.value;
        (i3 || o) && (n2 = this._getOrReturnCtx(e3, n2), i3 ? u(n2, { code: c.too_big, maximum: a3.value, type: "string", inclusive: true, exact: true, message: a3.message }) : o && u(n2, { code: c.too_small, minimum: a3.value, type: "string", inclusive: true, exact: true, message: a3.message }), r3.dirty());
      } else if (a3.kind === "email")
        Me.test(e3.data) || (n2 = this._getOrReturnCtx(e3, n2), u(n2, { validation: "email", code: c.invalid_string, message: a3.message }), r3.dirty());
      else if (a3.kind === "emoji")
        Ve.test(e3.data) || (n2 = this._getOrReturnCtx(e3, n2), u(n2, { validation: "emoji", code: c.invalid_string, message: a3.message }), r3.dirty());
      else if (a3.kind === "uuid")
        Ze.test(e3.data) || (n2 = this._getOrReturnCtx(e3, n2), u(n2, { validation: "uuid", code: c.invalid_string, message: a3.message }), r3.dirty());
      else if (a3.kind === "cuid")
        je.test(e3.data) || (n2 = this._getOrReturnCtx(e3, n2), u(n2, { validation: "cuid", code: c.invalid_string, message: a3.message }), r3.dirty());
      else if (a3.kind === "cuid2")
        Re.test(e3.data) || (n2 = this._getOrReturnCtx(e3, n2), u(n2, { validation: "cuid2", code: c.invalid_string, message: a3.message }), r3.dirty());
      else if (a3.kind === "ulid")
        Ae.test(e3.data) || (n2 = this._getOrReturnCtx(e3, n2), u(n2, { validation: "ulid", code: c.invalid_string, message: a3.message }), r3.dirty());
      else if (a3.kind === "url")
        try {
          new URL(e3.data);
        } catch {
          n2 = this._getOrReturnCtx(e3, n2), u(n2, { validation: "url", code: c.invalid_string, message: a3.message }), r3.dirty();
        }
      else
        a3.kind === "regex" ? (a3.regex.lastIndex = 0, a3.regex.test(e3.data) || (n2 = this._getOrReturnCtx(e3, n2), u(n2, { validation: "regex", code: c.invalid_string, message: a3.message }), r3.dirty())) : a3.kind === "trim" ? e3.data = e3.data.trim() : a3.kind === "includes" ? e3.data.includes(a3.value, a3.position) || (n2 = this._getOrReturnCtx(e3, n2), u(n2, { code: c.invalid_string, validation: { includes: a3.value, position: a3.position }, message: a3.message }), r3.dirty()) : a3.kind === "toLowerCase" ? e3.data = e3.data.toLowerCase() : a3.kind === "toUpperCase" ? e3.data = e3.data.toUpperCase() : a3.kind === "startsWith" ? e3.data.startsWith(a3.value) || (n2 = this._getOrReturnCtx(e3, n2), u(n2, { code: c.invalid_string, validation: { startsWith: a3.value }, message: a3.message }), r3.dirty()) : a3.kind === "endsWith" ? e3.data.endsWith(a3.value) || (n2 = this._getOrReturnCtx(e3, n2), u(n2, { code: c.invalid_string, validation: { endsWith: a3.value }, message: a3.message }), r3.dirty()) : a3.kind === "datetime" ? Le(a3).test(e3.data) || (n2 = this._getOrReturnCtx(e3, n2), u(n2, { code: c.invalid_string, validation: "datetime", message: a3.message }), r3.dirty()) : a3.kind === "ip" ? ze(e3.data, a3.version) || (n2 = this._getOrReturnCtx(e3, n2), u(n2, { validation: "ip", code: c.invalid_string, message: a3.message }), r3.dirty()) : g.assertNever(a3);
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
    var t2;
    return typeof e3 == "string" ? this._addCheck({ kind: "datetime", precision: null, offset: false, message: e3 }) : this._addCheck({ kind: "datetime", precision: typeof e3?.precision > "u" ? null : e3?.precision, offset: (t2 = e3?.offset) !== null && t2 !== void 0 ? t2 : false, ...h.errToObj(e3?.message) });
  }
  regex(e3, t2) {
    return this._addCheck({ kind: "regex", regex: e3, ...h.errToObj(t2) });
  }
  includes(e3, t2) {
    return this._addCheck({ kind: "includes", value: e3, position: t2?.position, ...h.errToObj(t2?.message) });
  }
  startsWith(e3, t2) {
    return this._addCheck({ kind: "startsWith", value: e3, ...h.errToObj(t2) });
  }
  endsWith(e3, t2) {
    return this._addCheck({ kind: "endsWith", value: e3, ...h.errToObj(t2) });
  }
  min(e3, t2) {
    return this._addCheck({ kind: "min", value: e3, ...h.errToObj(t2) });
  }
  max(e3, t2) {
    return this._addCheck({ kind: "max", value: e3, ...h.errToObj(t2) });
  }
  length(e3, t2) {
    return this._addCheck({ kind: "length", value: e3, ...h.errToObj(t2) });
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
    for (let t2 of this._def.checks)
      t2.kind === "min" && (e3 === null || t2.value > e3) && (e3 = t2.value);
    return e3;
  }
  get maxLength() {
    let e3 = null;
    for (let t2 of this._def.checks)
      t2.kind === "max" && (e3 === null || t2.value < e3) && (e3 = t2.value);
    return e3;
  }
};
w.create = (s2) => {
  var e3;
  return new w({ checks: [], typeName: p.ZodString, coerce: (e3 = s2?.coerce) !== null && e3 !== void 0 ? e3 : false, ...y(s2) });
};
function De(s2, e3) {
  let t2 = (s2.toString().split(".")[1] || "").length, r3 = (e3.toString().split(".")[1] || "").length, n2 = t2 > r3 ? t2 : r3, a3 = parseInt(s2.toFixed(n2).replace(".", "")), i3 = parseInt(e3.toFixed(n2).replace(".", ""));
  return a3 % i3 / Math.pow(10, n2);
}
var j = class extends v {
  constructor() {
    super(...arguments), this.min = this.gte, this.max = this.lte, this.step = this.multipleOf;
  }
  _parse(e3) {
    if (this._def.coerce && (e3.data = Number(e3.data)), this._getType(e3) !== d.number) {
      let a3 = this._getOrReturnCtx(e3);
      return u(a3, { code: c.invalid_type, expected: d.number, received: a3.parsedType }), m;
    }
    let r3, n2 = new k();
    for (let a3 of this._def.checks)
      a3.kind === "int" ? g.isInteger(e3.data) || (r3 = this._getOrReturnCtx(e3, r3), u(r3, { code: c.invalid_type, expected: "integer", received: "float", message: a3.message }), n2.dirty()) : a3.kind === "min" ? (a3.inclusive ? e3.data < a3.value : e3.data <= a3.value) && (r3 = this._getOrReturnCtx(e3, r3), u(r3, { code: c.too_small, minimum: a3.value, type: "number", inclusive: a3.inclusive, exact: false, message: a3.message }), n2.dirty()) : a3.kind === "max" ? (a3.inclusive ? e3.data > a3.value : e3.data >= a3.value) && (r3 = this._getOrReturnCtx(e3, r3), u(r3, { code: c.too_big, maximum: a3.value, type: "number", inclusive: a3.inclusive, exact: false, message: a3.message }), n2.dirty()) : a3.kind === "multipleOf" ? De(e3.data, a3.value) !== 0 && (r3 = this._getOrReturnCtx(e3, r3), u(r3, { code: c.not_multiple_of, multipleOf: a3.value, message: a3.message }), n2.dirty()) : a3.kind === "finite" ? Number.isFinite(e3.data) || (r3 = this._getOrReturnCtx(e3, r3), u(r3, { code: c.not_finite, message: a3.message }), n2.dirty()) : g.assertNever(a3);
    return { status: n2.value, value: e3.data };
  }
  gte(e3, t2) {
    return this.setLimit("min", e3, true, h.toString(t2));
  }
  gt(e3, t2) {
    return this.setLimit("min", e3, false, h.toString(t2));
  }
  lte(e3, t2) {
    return this.setLimit("max", e3, true, h.toString(t2));
  }
  lt(e3, t2) {
    return this.setLimit("max", e3, false, h.toString(t2));
  }
  setLimit(e3, t2, r3, n2) {
    return new j({ ...this._def, checks: [...this._def.checks, { kind: e3, value: t2, inclusive: r3, message: h.toString(n2) }] });
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
  multipleOf(e3, t2) {
    return this._addCheck({ kind: "multipleOf", value: e3, message: h.toString(t2) });
  }
  finite(e3) {
    return this._addCheck({ kind: "finite", message: h.toString(e3) });
  }
  safe(e3) {
    return this._addCheck({ kind: "min", inclusive: true, value: Number.MIN_SAFE_INTEGER, message: h.toString(e3) })._addCheck({ kind: "max", inclusive: true, value: Number.MAX_SAFE_INTEGER, message: h.toString(e3) });
  }
  get minValue() {
    let e3 = null;
    for (let t2 of this._def.checks)
      t2.kind === "min" && (e3 === null || t2.value > e3) && (e3 = t2.value);
    return e3;
  }
  get maxValue() {
    let e3 = null;
    for (let t2 of this._def.checks)
      t2.kind === "max" && (e3 === null || t2.value < e3) && (e3 = t2.value);
    return e3;
  }
  get isInt() {
    return !!this._def.checks.find((e3) => e3.kind === "int" || e3.kind === "multipleOf" && g.isInteger(e3.value));
  }
  get isFinite() {
    let e3 = null, t2 = null;
    for (let r3 of this._def.checks) {
      if (r3.kind === "finite" || r3.kind === "int" || r3.kind === "multipleOf")
        return true;
      r3.kind === "min" ? (t2 === null || r3.value > t2) && (t2 = r3.value) : r3.kind === "max" && (e3 === null || r3.value < e3) && (e3 = r3.value);
    }
    return Number.isFinite(t2) && Number.isFinite(e3);
  }
};
j.create = (s2) => new j({ checks: [], typeName: p.ZodNumber, coerce: s2?.coerce || false, ...y(s2) });
var R = class extends v {
  constructor() {
    super(...arguments), this.min = this.gte, this.max = this.lte;
  }
  _parse(e3) {
    if (this._def.coerce && (e3.data = BigInt(e3.data)), this._getType(e3) !== d.bigint) {
      let a3 = this._getOrReturnCtx(e3);
      return u(a3, { code: c.invalid_type, expected: d.bigint, received: a3.parsedType }), m;
    }
    let r3, n2 = new k();
    for (let a3 of this._def.checks)
      a3.kind === "min" ? (a3.inclusive ? e3.data < a3.value : e3.data <= a3.value) && (r3 = this._getOrReturnCtx(e3, r3), u(r3, { code: c.too_small, type: "bigint", minimum: a3.value, inclusive: a3.inclusive, message: a3.message }), n2.dirty()) : a3.kind === "max" ? (a3.inclusive ? e3.data > a3.value : e3.data >= a3.value) && (r3 = this._getOrReturnCtx(e3, r3), u(r3, { code: c.too_big, type: "bigint", maximum: a3.value, inclusive: a3.inclusive, message: a3.message }), n2.dirty()) : a3.kind === "multipleOf" ? e3.data % a3.value !== BigInt(0) && (r3 = this._getOrReturnCtx(e3, r3), u(r3, { code: c.not_multiple_of, multipleOf: a3.value, message: a3.message }), n2.dirty()) : g.assertNever(a3);
    return { status: n2.value, value: e3.data };
  }
  gte(e3, t2) {
    return this.setLimit("min", e3, true, h.toString(t2));
  }
  gt(e3, t2) {
    return this.setLimit("min", e3, false, h.toString(t2));
  }
  lte(e3, t2) {
    return this.setLimit("max", e3, true, h.toString(t2));
  }
  lt(e3, t2) {
    return this.setLimit("max", e3, false, h.toString(t2));
  }
  setLimit(e3, t2, r3, n2) {
    return new R({ ...this._def, checks: [...this._def.checks, { kind: e3, value: t2, inclusive: r3, message: h.toString(n2) }] });
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
  multipleOf(e3, t2) {
    return this._addCheck({ kind: "multipleOf", value: e3, message: h.toString(t2) });
  }
  get minValue() {
    let e3 = null;
    for (let t2 of this._def.checks)
      t2.kind === "min" && (e3 === null || t2.value > e3) && (e3 = t2.value);
    return e3;
  }
  get maxValue() {
    let e3 = null;
    for (let t2 of this._def.checks)
      t2.kind === "max" && (e3 === null || t2.value < e3) && (e3 = t2.value);
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
      let a3 = this._getOrReturnCtx(e3);
      return u(a3, { code: c.invalid_type, expected: d.date, received: a3.parsedType }), m;
    }
    if (isNaN(e3.data.getTime())) {
      let a3 = this._getOrReturnCtx(e3);
      return u(a3, { code: c.invalid_date }), m;
    }
    let r3 = new k(), n2;
    for (let a3 of this._def.checks)
      a3.kind === "min" ? e3.data.getTime() < a3.value && (n2 = this._getOrReturnCtx(e3, n2), u(n2, { code: c.too_small, message: a3.message, inclusive: true, exact: false, minimum: a3.value, type: "date" }), r3.dirty()) : a3.kind === "max" ? e3.data.getTime() > a3.value && (n2 = this._getOrReturnCtx(e3, n2), u(n2, { code: c.too_big, message: a3.message, inclusive: true, exact: false, maximum: a3.value, type: "date" }), r3.dirty()) : g.assertNever(a3);
    return { status: r3.value, value: new Date(e3.data.getTime()) };
  }
  _addCheck(e3) {
    return new M({ ...this._def, checks: [...this._def.checks, e3] });
  }
  min(e3, t2) {
    return this._addCheck({ kind: "min", value: e3.getTime(), message: h.toString(t2) });
  }
  max(e3, t2) {
    return this._addCheck({ kind: "max", value: e3.getTime(), message: h.toString(t2) });
  }
  get minDate() {
    let e3 = null;
    for (let t2 of this._def.checks)
      t2.kind === "min" && (e3 === null || t2.value > e3) && (e3 = t2.value);
    return e3 != null ? new Date(e3) : null;
  }
  get maxDate() {
    let e3 = null;
    for (let t2 of this._def.checks)
      t2.kind === "max" && (e3 === null || t2.value < e3) && (e3 = t2.value);
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
    let t2 = this._getOrReturnCtx(e3);
    return u(t2, { code: c.invalid_type, expected: d.never, received: t2.parsedType }), m;
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
    let { ctx: t2, status: r3 } = this._processInputParams(e3), n2 = this._def;
    if (t2.parsedType !== d.array)
      return u(t2, { code: c.invalid_type, expected: d.array, received: t2.parsedType }), m;
    if (n2.exactLength !== null) {
      let i3 = t2.data.length > n2.exactLength.value, o = t2.data.length < n2.exactLength.value;
      (i3 || o) && (u(t2, { code: i3 ? c.too_big : c.too_small, minimum: o ? n2.exactLength.value : void 0, maximum: i3 ? n2.exactLength.value : void 0, type: "array", inclusive: true, exact: true, message: n2.exactLength.message }), r3.dirty());
    }
    if (n2.minLength !== null && t2.data.length < n2.minLength.value && (u(t2, { code: c.too_small, minimum: n2.minLength.value, type: "array", inclusive: true, exact: false, message: n2.minLength.message }), r3.dirty()), n2.maxLength !== null && t2.data.length > n2.maxLength.value && (u(t2, { code: c.too_big, maximum: n2.maxLength.value, type: "array", inclusive: true, exact: false, message: n2.maxLength.message }), r3.dirty()), t2.common.async)
      return Promise.all([...t2.data].map((i3, o) => n2.type._parseAsync(new O(t2, i3, t2.path, o)))).then((i3) => k.mergeArray(r3, i3));
    let a3 = [...t2.data].map((i3, o) => n2.type._parseSync(new O(t2, i3, t2.path, o)));
    return k.mergeArray(r3, a3);
  }
  get element() {
    return this._def.type;
  }
  min(e3, t2) {
    return new S({ ...this._def, minLength: { value: e3, message: h.toString(t2) } });
  }
  max(e3, t2) {
    return new S({ ...this._def, maxLength: { value: e3, message: h.toString(t2) } });
  }
  length(e3, t2) {
    return new S({ ...this._def, exactLength: { value: e3, message: h.toString(t2) } });
  }
  nonempty(e3) {
    return this.min(1, e3);
  }
};
S.create = (s2, e3) => new S({ type: s2, minLength: null, maxLength: null, exactLength: null, typeName: p.ZodArray, ...y(e3) });
function ee(s2) {
  if (s2 instanceof x) {
    let e3 = {};
    for (let t2 in s2.shape) {
      let r3 = s2.shape[t2];
      e3[t2] = E.create(ee(r3));
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
    let e3 = this._def.shape(), t2 = g.objectKeys(e3);
    return this._cached = { shape: e3, keys: t2 };
  }
  _parse(e3) {
    if (this._getType(e3) !== d.object) {
      let l4 = this._getOrReturnCtx(e3);
      return u(l4, { code: c.invalid_type, expected: d.object, received: l4.parsedType }), m;
    }
    let { status: r3, ctx: n2 } = this._processInputParams(e3), { shape: a3, keys: i3 } = this._getCached(), o = [];
    if (!(this._def.catchall instanceof I && this._def.unknownKeys === "strip"))
      for (let l4 in n2.data)
        i3.includes(l4) || o.push(l4);
    let f7 = [];
    for (let l4 of i3) {
      let _9 = a3[l4], F7 = n2.data[l4];
      f7.push({ key: { status: "valid", value: l4 }, value: _9._parse(new O(n2, F7, n2.path, l4)), alwaysSet: l4 in n2.data });
    }
    if (this._def.catchall instanceof I) {
      let l4 = this._def.unknownKeys;
      if (l4 === "passthrough")
        for (let _9 of o)
          f7.push({ key: { status: "valid", value: _9 }, value: { status: "valid", value: n2.data[_9] } });
      else if (l4 === "strict")
        o.length > 0 && (u(n2, { code: c.unrecognized_keys, keys: o }), r3.dirty());
      else if (l4 !== "strip")
        throw new Error("Internal ZodObject error: invalid unknownKeys value.");
    } else {
      let l4 = this._def.catchall;
      for (let _9 of o) {
        let F7 = n2.data[_9];
        f7.push({ key: { status: "valid", value: _9 }, value: l4._parse(new O(n2, F7, n2.path, _9)), alwaysSet: _9 in n2.data });
      }
    }
    return n2.common.async ? Promise.resolve().then(async () => {
      let l4 = [];
      for (let _9 of f7) {
        let F7 = await _9.key;
        l4.push({ key: F7, value: await _9.value, alwaysSet: _9.alwaysSet });
      }
      return l4;
    }).then((l4) => k.mergeObjectSync(r3, l4)) : k.mergeObjectSync(r3, f7);
  }
  get shape() {
    return this._def.shape();
  }
  strict(e3) {
    return h.errToObj, new x({ ...this._def, unknownKeys: "strict", ...e3 !== void 0 ? { errorMap: (t2, r3) => {
      var n2, a3, i3, o;
      let f7 = (i3 = (a3 = (n2 = this._def).errorMap) === null || a3 === void 0 ? void 0 : a3.call(n2, t2, r3).message) !== null && i3 !== void 0 ? i3 : r3.defaultError;
      return t2.code === "unrecognized_keys" ? { message: (o = h.errToObj(e3).message) !== null && o !== void 0 ? o : f7 } : { message: f7 };
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
  setKey(e3, t2) {
    return this.augment({ [e3]: t2 });
  }
  catchall(e3) {
    return new x({ ...this._def, catchall: e3 });
  }
  pick(e3) {
    let t2 = {};
    return g.objectKeys(e3).forEach((r3) => {
      e3[r3] && this.shape[r3] && (t2[r3] = this.shape[r3]);
    }), new x({ ...this._def, shape: () => t2 });
  }
  omit(e3) {
    let t2 = {};
    return g.objectKeys(this.shape).forEach((r3) => {
      e3[r3] || (t2[r3] = this.shape[r3]);
    }), new x({ ...this._def, shape: () => t2 });
  }
  deepPartial() {
    return ee(this);
  }
  partial(e3) {
    let t2 = {};
    return g.objectKeys(this.shape).forEach((r3) => {
      let n2 = this.shape[r3];
      e3 && !e3[r3] ? t2[r3] = n2 : t2[r3] = n2.optional();
    }), new x({ ...this._def, shape: () => t2 });
  }
  required(e3) {
    let t2 = {};
    return g.objectKeys(this.shape).forEach((r3) => {
      if (e3 && !e3[r3])
        t2[r3] = this.shape[r3];
      else {
        let a3 = this.shape[r3];
        for (; a3 instanceof E; )
          a3 = a3._def.innerType;
        t2[r3] = a3;
      }
    }), new x({ ...this._def, shape: () => t2 });
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
    let { ctx: t2 } = this._processInputParams(e3), r3 = this._def.options;
    function n2(a3) {
      for (let o of a3)
        if (o.result.status === "valid")
          return o.result;
      for (let o of a3)
        if (o.result.status === "dirty")
          return t2.common.issues.push(...o.ctx.common.issues), o.result;
      let i3 = a3.map((o) => new T(o.ctx.common.issues));
      return u(t2, { code: c.invalid_union, unionErrors: i3 }), m;
    }
    if (t2.common.async)
      return Promise.all(r3.map(async (a3) => {
        let i3 = { ...t2, common: { ...t2.common, issues: [] }, parent: null };
        return { result: await a3._parseAsync({ data: t2.data, path: t2.path, parent: i3 }), ctx: i3 };
      })).then(n2);
    {
      let a3, i3 = [];
      for (let f7 of r3) {
        let l4 = { ...t2, common: { ...t2.common, issues: [] }, parent: null }, _9 = f7._parseSync({ data: t2.data, path: t2.path, parent: l4 });
        if (_9.status === "valid")
          return _9;
        _9.status === "dirty" && !a3 && (a3 = { result: _9, ctx: l4 }), l4.common.issues.length && i3.push(l4.common.issues);
      }
      if (a3)
        return t2.common.issues.push(...a3.ctx.common.issues), a3.result;
      let o = i3.map((f7) => new T(f7));
      return u(t2, { code: c.invalid_union, unionErrors: o }), m;
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
    let { ctx: t2 } = this._processInputParams(e3);
    if (t2.parsedType !== d.object)
      return u(t2, { code: c.invalid_type, expected: d.object, received: t2.parsedType }), m;
    let r3 = this.discriminator, n2 = t2.data[r3], a3 = this.optionsMap.get(n2);
    return a3 ? t2.common.async ? a3._parseAsync({ data: t2.data, path: t2.path, parent: t2 }) : a3._parseSync({ data: t2.data, path: t2.path, parent: t2 }) : (u(t2, { code: c.invalid_union_discriminator, options: Array.from(this.optionsMap.keys()), path: [r3] }), m);
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
  static create(e3, t2, r3) {
    let n2 = /* @__PURE__ */ new Map();
    for (let a3 of t2) {
      let i3 = ce(a3.shape[e3]);
      if (!i3)
        throw new Error(`A discriminator value for key \`${e3}\` could not be extracted from all schema options`);
      for (let o of i3) {
        if (n2.has(o))
          throw new Error(`Discriminator property ${String(e3)} has duplicate value ${String(o)}`);
        n2.set(o, a3);
      }
    }
    return new re({ typeName: p.ZodDiscriminatedUnion, discriminator: e3, options: t2, optionsMap: n2, ...y(r3) });
  }
};
function _e(s2, e3) {
  let t2 = P(s2), r3 = P(e3);
  if (s2 === e3)
    return { valid: true, data: s2 };
  if (t2 === d.object && r3 === d.object) {
    let n2 = g.objectKeys(e3), a3 = g.objectKeys(s2).filter((o) => n2.indexOf(o) !== -1), i3 = { ...s2, ...e3 };
    for (let o of a3) {
      let f7 = _e(s2[o], e3[o]);
      if (!f7.valid)
        return { valid: false };
      i3[o] = f7.data;
    }
    return { valid: true, data: i3 };
  } else if (t2 === d.array && r3 === d.array) {
    if (s2.length !== e3.length)
      return { valid: false };
    let n2 = [];
    for (let a3 = 0; a3 < s2.length; a3++) {
      let i3 = s2[a3], o = e3[a3], f7 = _e(i3, o);
      if (!f7.valid)
        return { valid: false };
      n2.push(f7.data);
    }
    return { valid: true, data: n2 };
  } else
    return t2 === d.date && r3 === d.date && +s2 == +e3 ? { valid: true, data: s2 } : { valid: false };
}
var J = class extends v {
  _parse(e3) {
    let { status: t2, ctx: r3 } = this._processInputParams(e3), n2 = (a3, i3) => {
      if (ye(a3) || ye(i3))
        return m;
      let o = _e(a3.value, i3.value);
      return o.valid ? ((ve(a3) || ve(i3)) && t2.dirty(), { status: t2.value, value: o.data }) : (u(r3, { code: c.invalid_intersection_types }), m);
    };
    return r3.common.async ? Promise.all([this._def.left._parseAsync({ data: r3.data, path: r3.path, parent: r3 }), this._def.right._parseAsync({ data: r3.data, path: r3.path, parent: r3 })]).then(([a3, i3]) => n2(a3, i3)) : n2(this._def.left._parseSync({ data: r3.data, path: r3.path, parent: r3 }), this._def.right._parseSync({ data: r3.data, path: r3.path, parent: r3 }));
  }
};
J.create = (s2, e3, t2) => new J({ left: s2, right: e3, typeName: p.ZodIntersection, ...y(t2) });
var N = class extends v {
  _parse(e3) {
    let { status: t2, ctx: r3 } = this._processInputParams(e3);
    if (r3.parsedType !== d.array)
      return u(r3, { code: c.invalid_type, expected: d.array, received: r3.parsedType }), m;
    if (r3.data.length < this._def.items.length)
      return u(r3, { code: c.too_small, minimum: this._def.items.length, inclusive: true, exact: false, type: "array" }), m;
    !this._def.rest && r3.data.length > this._def.items.length && (u(r3, { code: c.too_big, maximum: this._def.items.length, inclusive: true, exact: false, type: "array" }), t2.dirty());
    let a3 = [...r3.data].map((i3, o) => {
      let f7 = this._def.items[o] || this._def.rest;
      return f7 ? f7._parse(new O(r3, i3, r3.path, o)) : null;
    }).filter((i3) => !!i3);
    return r3.common.async ? Promise.all(a3).then((i3) => k.mergeArray(t2, i3)) : k.mergeArray(t2, a3);
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
    let { status: t2, ctx: r3 } = this._processInputParams(e3);
    if (r3.parsedType !== d.object)
      return u(r3, { code: c.invalid_type, expected: d.object, received: r3.parsedType }), m;
    let n2 = [], a3 = this._def.keyType, i3 = this._def.valueType;
    for (let o in r3.data)
      n2.push({ key: a3._parse(new O(r3, o, r3.path, o)), value: i3._parse(new O(r3, r3.data[o], r3.path, o)) });
    return r3.common.async ? k.mergeObjectAsync(t2, n2) : k.mergeObjectSync(t2, n2);
  }
  get element() {
    return this._def.valueType;
  }
  static create(e3, t2, r3) {
    return t2 instanceof v ? new Y({ keyType: e3, valueType: t2, typeName: p.ZodRecord, ...y(r3) }) : new Y({ keyType: w.create(), valueType: e3, typeName: p.ZodRecord, ...y(t2) });
  }
};
var ne = class extends v {
  _parse(e3) {
    let { status: t2, ctx: r3 } = this._processInputParams(e3);
    if (r3.parsedType !== d.map)
      return u(r3, { code: c.invalid_type, expected: d.map, received: r3.parsedType }), m;
    let n2 = this._def.keyType, a3 = this._def.valueType, i3 = [...r3.data.entries()].map(([o, f7], l4) => ({ key: n2._parse(new O(r3, o, r3.path, [l4, "key"])), value: a3._parse(new O(r3, f7, r3.path, [l4, "value"])) }));
    if (r3.common.async) {
      let o = /* @__PURE__ */ new Map();
      return Promise.resolve().then(async () => {
        for (let f7 of i3) {
          let l4 = await f7.key, _9 = await f7.value;
          if (l4.status === "aborted" || _9.status === "aborted")
            return m;
          (l4.status === "dirty" || _9.status === "dirty") && t2.dirty(), o.set(l4.value, _9.value);
        }
        return { status: t2.value, value: o };
      });
    } else {
      let o = /* @__PURE__ */ new Map();
      for (let f7 of i3) {
        let l4 = f7.key, _9 = f7.value;
        if (l4.status === "aborted" || _9.status === "aborted")
          return m;
        (l4.status === "dirty" || _9.status === "dirty") && t2.dirty(), o.set(l4.value, _9.value);
      }
      return { status: t2.value, value: o };
    }
  }
};
ne.create = (s2, e3, t2) => new ne({ valueType: e3, keyType: s2, typeName: p.ZodMap, ...y(t2) });
var V = class extends v {
  _parse(e3) {
    let { status: t2, ctx: r3 } = this._processInputParams(e3);
    if (r3.parsedType !== d.set)
      return u(r3, { code: c.invalid_type, expected: d.set, received: r3.parsedType }), m;
    let n2 = this._def;
    n2.minSize !== null && r3.data.size < n2.minSize.value && (u(r3, { code: c.too_small, minimum: n2.minSize.value, type: "set", inclusive: true, exact: false, message: n2.minSize.message }), t2.dirty()), n2.maxSize !== null && r3.data.size > n2.maxSize.value && (u(r3, { code: c.too_big, maximum: n2.maxSize.value, type: "set", inclusive: true, exact: false, message: n2.maxSize.message }), t2.dirty());
    let a3 = this._def.valueType;
    function i3(f7) {
      let l4 = /* @__PURE__ */ new Set();
      for (let _9 of f7) {
        if (_9.status === "aborted")
          return m;
        _9.status === "dirty" && t2.dirty(), l4.add(_9.value);
      }
      return { status: t2.value, value: l4 };
    }
    let o = [...r3.data.values()].map((f7, l4) => a3._parse(new O(r3, f7, r3.path, l4)));
    return r3.common.async ? Promise.all(o).then((f7) => i3(f7)) : i3(o);
  }
  min(e3, t2) {
    return new V({ ...this._def, minSize: { value: e3, message: h.toString(t2) } });
  }
  max(e3, t2) {
    return new V({ ...this._def, maxSize: { value: e3, message: h.toString(t2) } });
  }
  size(e3, t2) {
    return this.min(e3, t2).max(e3, t2);
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
    let { ctx: t2 } = this._processInputParams(e3);
    if (t2.parsedType !== d.function)
      return u(t2, { code: c.invalid_type, expected: d.function, received: t2.parsedType }), m;
    function r3(o, f7) {
      return ue({ data: o, path: t2.path, errorMaps: [t2.common.contextualErrorMap, t2.schemaErrorMap, de(), oe].filter((l4) => !!l4), issueData: { code: c.invalid_arguments, argumentsError: f7 } });
    }
    function n2(o, f7) {
      return ue({ data: o, path: t2.path, errorMaps: [t2.common.contextualErrorMap, t2.schemaErrorMap, de(), oe].filter((l4) => !!l4), issueData: { code: c.invalid_return_type, returnTypeError: f7 } });
    }
    let a3 = { errorMap: t2.common.contextualErrorMap }, i3 = t2.data;
    return this._def.returns instanceof D ? b(async (...o) => {
      let f7 = new T([]), l4 = await this._def.args.parseAsync(o, a3).catch((pe2) => {
        throw f7.addIssue(r3(o, pe2)), f7;
      }), _9 = await i3(...l4);
      return await this._def.returns._def.type.parseAsync(_9, a3).catch((pe2) => {
        throw f7.addIssue(n2(_9, pe2)), f7;
      });
    }) : b((...o) => {
      let f7 = this._def.args.safeParse(o, a3);
      if (!f7.success)
        throw new T([r3(o, f7.error)]);
      let l4 = i3(...f7.data), _9 = this._def.returns.safeParse(l4, a3);
      if (!_9.success)
        throw new T([n2(l4, _9.error)]);
      return _9.data;
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
  static create(e3, t2, r3) {
    return new L({ args: e3 || N.create([]).rest(Z.create()), returns: t2 || Z.create(), typeName: p.ZodFunction, ...y(r3) });
  }
};
var H = class extends v {
  get schema() {
    return this._def.getter();
  }
  _parse(e3) {
    let { ctx: t2 } = this._processInputParams(e3);
    return this._def.getter()._parse({ data: t2.data, path: t2.path, parent: t2 });
  }
};
H.create = (s2, e3) => new H({ getter: s2, typeName: p.ZodLazy, ...y(e3) });
var G = class extends v {
  _parse(e3) {
    if (e3.data !== this._def.value) {
      let t2 = this._getOrReturnCtx(e3);
      return u(t2, { received: t2.data, code: c.invalid_literal, expected: this._def.value }), m;
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
      let t2 = this._getOrReturnCtx(e3), r3 = this._def.values;
      return u(t2, { expected: g.joinValues(r3), received: t2.parsedType, code: c.invalid_type }), m;
    }
    if (this._def.values.indexOf(e3.data) === -1) {
      let t2 = this._getOrReturnCtx(e3), r3 = this._def.values;
      return u(t2, { received: t2.data, code: c.invalid_enum_value, options: r3 }), m;
    }
    return b(e3.data);
  }
  get options() {
    return this._def.values;
  }
  get enum() {
    let e3 = {};
    for (let t2 of this._def.values)
      e3[t2] = t2;
    return e3;
  }
  get Values() {
    let e3 = {};
    for (let t2 of this._def.values)
      e3[t2] = t2;
    return e3;
  }
  get Enum() {
    let e3 = {};
    for (let t2 of this._def.values)
      e3[t2] = t2;
    return e3;
  }
  extract(e3) {
    return A.create(e3);
  }
  exclude(e3) {
    return A.create(this.options.filter((t2) => !e3.includes(t2)));
  }
};
A.create = we;
var X = class extends v {
  _parse(e3) {
    let t2 = g.getValidEnumValues(this._def.values), r3 = this._getOrReturnCtx(e3);
    if (r3.parsedType !== d.string && r3.parsedType !== d.number) {
      let n2 = g.objectValues(t2);
      return u(r3, { expected: g.joinValues(n2), received: r3.parsedType, code: c.invalid_type }), m;
    }
    if (t2.indexOf(e3.data) === -1) {
      let n2 = g.objectValues(t2);
      return u(r3, { received: r3.data, code: c.invalid_enum_value, options: n2 }), m;
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
    let { ctx: t2 } = this._processInputParams(e3);
    if (t2.parsedType !== d.promise && t2.common.async === false)
      return u(t2, { code: c.invalid_type, expected: d.promise, received: t2.parsedType }), m;
    let r3 = t2.parsedType === d.promise ? t2.data : Promise.resolve(t2.data);
    return b(r3.then((n2) => this._def.type.parseAsync(n2, { path: t2.path, errorMap: t2.common.contextualErrorMap })));
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
    let { status: t2, ctx: r3 } = this._processInputParams(e3), n2 = this._def.effect || null;
    if (n2.type === "preprocess") {
      let i3 = n2.transform(r3.data);
      return r3.common.async ? Promise.resolve(i3).then((o) => this._def.schema._parseAsync({ data: o, path: r3.path, parent: r3 })) : this._def.schema._parseSync({ data: i3, path: r3.path, parent: r3 });
    }
    let a3 = { addIssue: (i3) => {
      u(r3, i3), i3.fatal ? t2.abort() : t2.dirty();
    }, get path() {
      return r3.path;
    } };
    if (a3.addIssue = a3.addIssue.bind(a3), n2.type === "refinement") {
      let i3 = (o) => {
        let f7 = n2.refinement(o, a3);
        if (r3.common.async)
          return Promise.resolve(f7);
        if (f7 instanceof Promise)
          throw new Error("Async refinement encountered during synchronous parse operation. Use .parseAsync instead.");
        return o;
      };
      if (r3.common.async === false) {
        let o = this._def.schema._parseSync({ data: r3.data, path: r3.path, parent: r3 });
        return o.status === "aborted" ? m : (o.status === "dirty" && t2.dirty(), i3(o.value), { status: t2.value, value: o.value });
      } else
        return this._def.schema._parseAsync({ data: r3.data, path: r3.path, parent: r3 }).then((o) => o.status === "aborted" ? m : (o.status === "dirty" && t2.dirty(), i3(o.value).then(() => ({ status: t2.value, value: o.value }))));
    }
    if (n2.type === "transform")
      if (r3.common.async === false) {
        let i3 = this._def.schema._parseSync({ data: r3.data, path: r3.path, parent: r3 });
        if (!le(i3))
          return i3;
        let o = n2.transform(i3.value, a3);
        if (o instanceof Promise)
          throw new Error("Asynchronous transform encountered during synchronous parse operation. Use .parseAsync instead.");
        return { status: t2.value, value: o };
      } else
        return this._def.schema._parseAsync({ data: r3.data, path: r3.path, parent: r3 }).then((i3) => le(i3) ? Promise.resolve(n2.transform(i3.value, a3)).then((o) => ({ status: t2.value, value: o })) : i3);
    g.assertNever(n2);
  }
};
C.create = (s2, e3, t2) => new C({ schema: s2, typeName: p.ZodEffects, effect: e3, ...y(t2) });
C.createWithPreprocess = (s2, e3, t2) => new C({ schema: e3, effect: { type: "preprocess", transform: s2 }, typeName: p.ZodEffects, ...y(t2) });
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
    let { ctx: t2 } = this._processInputParams(e3), r3 = t2.data;
    return t2.parsedType === d.undefined && (r3 = this._def.defaultValue()), this._def.innerType._parse({ data: r3, path: t2.path, parent: t2 });
  }
  removeDefault() {
    return this._def.innerType;
  }
};
K.create = (s2, e3) => new K({ innerType: s2, typeName: p.ZodDefault, defaultValue: typeof e3.default == "function" ? e3.default : () => e3.default, ...y(e3) });
var ae = class extends v {
  _parse(e3) {
    let { ctx: t2 } = this._processInputParams(e3), r3 = { ...t2, common: { ...t2.common, issues: [] } }, n2 = this._def.innerType._parse({ data: r3.data, path: r3.path, parent: { ...r3 } });
    return fe(n2) ? n2.then((a3) => ({ status: "valid", value: a3.status === "valid" ? a3.value : this._def.catchValue({ get error() {
      return new T(r3.common.issues);
    }, input: r3.data }) })) : { status: "valid", value: n2.status === "valid" ? n2.value : this._def.catchValue({ get error() {
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
    let { ctx: t2 } = this._processInputParams(e3), r3 = t2.data;
    return this._def.type._parse({ data: r3, path: t2.path, parent: t2 });
  }
  unwrap() {
    return this._def.type;
  }
};
var Q = class extends v {
  _parse(e3) {
    let { status: t2, ctx: r3 } = this._processInputParams(e3);
    if (r3.common.async)
      return (async () => {
        let a3 = await this._def.in._parseAsync({ data: r3.data, path: r3.path, parent: r3 });
        return a3.status === "aborted" ? m : a3.status === "dirty" ? (t2.dirty(), be(a3.value)) : this._def.out._parseAsync({ data: a3.value, path: r3.path, parent: r3 });
      })();
    {
      let n2 = this._def.in._parseSync({ data: r3.data, path: r3.path, parent: r3 });
      return n2.status === "aborted" ? m : n2.status === "dirty" ? (t2.dirty(), { status: "dirty", value: n2.value }) : this._def.out._parseSync({ data: n2.value, path: r3.path, parent: r3 });
    }
  }
  static create(e3, t2) {
    return new Q({ in: e3, out: t2, typeName: p.ZodPipeline });
  }
};
var Te = (s2, e3 = {}, t2) => s2 ? z.create().superRefine((r3, n2) => {
  var a3, i3;
  if (!s2(r3)) {
    let o = typeof e3 == "function" ? e3(r3) : typeof e3 == "string" ? { message: e3 } : e3, f7 = (i3 = (a3 = o.fatal) !== null && a3 !== void 0 ? a3 : t2) !== null && i3 !== void 0 ? i3 : true, l4 = typeof o == "string" ? { message: o } : o;
    n2.addIssue({ code: "custom", ...l4, fatal: f7 });
  }
}) : z.create();
var Be = { object: x.lazycreate };
var p;
(function(s2) {
  s2.ZodString = "ZodString", s2.ZodNumber = "ZodNumber", s2.ZodNaN = "ZodNaN", s2.ZodBigInt = "ZodBigInt", s2.ZodBoolean = "ZodBoolean", s2.ZodDate = "ZodDate", s2.ZodSymbol = "ZodSymbol", s2.ZodUndefined = "ZodUndefined", s2.ZodNull = "ZodNull", s2.ZodAny = "ZodAny", s2.ZodUnknown = "ZodUnknown", s2.ZodNever = "ZodNever", s2.ZodVoid = "ZodVoid", s2.ZodArray = "ZodArray", s2.ZodObject = "ZodObject", s2.ZodUnion = "ZodUnion", s2.ZodDiscriminatedUnion = "ZodDiscriminatedUnion", s2.ZodIntersection = "ZodIntersection", s2.ZodTuple = "ZodTuple", s2.ZodRecord = "ZodRecord", s2.ZodMap = "ZodMap", s2.ZodSet = "ZodSet", s2.ZodFunction = "ZodFunction", s2.ZodLazy = "ZodLazy", s2.ZodLiteral = "ZodLiteral", s2.ZodEnum = "ZodEnum", s2.ZodEffects = "ZodEffects", s2.ZodNativeEnum = "ZodNativeEnum", s2.ZodOptional = "ZodOptional", s2.ZodNullable = "ZodNullable", s2.ZodDefault = "ZodDefault", s2.ZodCatch = "ZodCatch", s2.ZodPromise = "ZodPromise", s2.ZodBranded = "ZodBranded", s2.ZodPipeline = "ZodPipeline";
})(p || (p = {}));
var We = (s2, e3 = { message: `Input not instance of ${s2.name}` }) => Te((t2) => t2 instanceof s2, e3);
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
    this.orderdAdapters = [...this.adapterOrderMap].sort((a3, b6) => b6[1] - a3[1]).map((a3) => a3[0]);
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
        ipc.onClose(() => {
          controller.close();
        });
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
var F = (a3, r3) => () => (r3 || a3((r3 = { exports: {} }).exports, r3), r3.exports);
var G2 = (a3, r3) => {
  for (var i3 in r3)
    v2(a3, i3, { get: r3[i3], enumerable: true });
};
var e = (a3, r3, i3, f7) => {
  if (r3 && typeof r3 == "object" || typeof r3 == "function")
    for (let o of A2(r3))
      !D2.call(a3, o) && o !== i3 && v2(a3, o, { get: () => r3[o], enumerable: !(f7 = z2(r3, o)) || f7.enumerable });
  return a3;
};
var _ = (a3, r3, i3) => (e(a3, r3, "default"), i3 && e(i3, r3, "default"));
var B2 = (a3, r3, i3) => (i3 = a3 != null ? y2(C2(a3)) : {}, e(r3 || !a3 || !a3.__esModule ? v2(i3, "default", { value: a3, enumerable: true }) : i3, a3));
var g2 = F((I7) => {
  I7.read = function(a3, r3, i3, f7, o) {
    var h3, t2, w9 = o * 8 - f7 - 1, s2 = (1 << w9) - 1, N8 = s2 >> 1, M8 = -7, p10 = i3 ? o - 1 : 0, c7 = i3 ? -1 : 1, d5 = a3[r3 + p10];
    for (p10 += c7, h3 = d5 & (1 << -M8) - 1, d5 >>= -M8, M8 += w9; M8 > 0; h3 = h3 * 256 + a3[r3 + p10], p10 += c7, M8 -= 8)
      ;
    for (t2 = h3 & (1 << -M8) - 1, h3 >>= -M8, M8 += f7; M8 > 0; t2 = t2 * 256 + a3[r3 + p10], p10 += c7, M8 -= 8)
      ;
    if (h3 === 0)
      h3 = 1 - N8;
    else {
      if (h3 === s2)
        return t2 ? NaN : (d5 ? -1 : 1) * (1 / 0);
      t2 = t2 + Math.pow(2, f7), h3 = h3 - N8;
    }
    return (d5 ? -1 : 1) * t2 * Math.pow(2, h3 - f7);
  };
  I7.write = function(a3, r3, i3, f7, o, h3) {
    var t2, w9, s2, N8 = h3 * 8 - o - 1, M8 = (1 << N8) - 1, p10 = M8 >> 1, c7 = o === 23 ? Math.pow(2, -24) - Math.pow(2, -77) : 0, d5 = f7 ? 0 : h3 - 1, n2 = f7 ? 1 : -1, q7 = r3 < 0 || r3 === 0 && 1 / r3 < 0 ? 1 : 0;
    for (r3 = Math.abs(r3), isNaN(r3) || r3 === 1 / 0 ? (w9 = isNaN(r3) ? 1 : 0, t2 = M8) : (t2 = Math.floor(Math.log(r3) / Math.LN2), r3 * (s2 = Math.pow(2, -t2)) < 1 && (t2--, s2 *= 2), t2 + p10 >= 1 ? r3 += c7 / s2 : r3 += c7 * Math.pow(2, 1 - p10), r3 * s2 >= 2 && (t2++, s2 /= 2), t2 + p10 >= M8 ? (w9 = 0, t2 = M8) : t2 + p10 >= 1 ? (w9 = (r3 * s2 - 1) * Math.pow(2, o), t2 = t2 + p10) : (w9 = r3 * Math.pow(2, p10 - 1) * Math.pow(2, o), t2 = 0)); o >= 8; a3[i3 + d5] = w9 & 255, d5 += n2, w9 /= 256, o -= 8)
      ;
    for (t2 = t2 << o | w9, N8 += o; N8 > 0; a3[i3 + d5] = t2 & 255, d5 += n2, t2 /= 256, N8 -= 8)
      ;
    a3[i3 + d5 - n2] |= q7 * 128;
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
  for (var t2 in e3)
    l(r3, t2, { get: e3[t2], enumerable: true });
};
var A3 = (r3, e3, t2, a3) => {
  if (e3 && typeof e3 == "object" || typeof e3 == "function")
    for (let o of k3(e3))
      !j3.call(r3, o) && o !== t2 && l(r3, o, { get: () => e3[o], enumerable: !(a3 = _2(e3, o)) || a3.enumerable });
  return r3;
};
var u2 = (r3, e3, t2) => (A3(r3, e3, "default"), t2 && A3(t2, e3, "default"));
var C3 = (r3, e3, t2) => (t2 = r3 != null ? B3(w2(r3)) : {}, A3(e3 || !r3 || !r3.__esModule ? l(t2, "default", { value: r3, enumerable: true }) : t2, r3));
var p2 = H3((y14) => {
  "use strict";
  y14.byteLength = I7;
  y14.toByteArray = T7;
  y14.fromByteArray = D9;
  var h3 = [], d5 = [], E7 = typeof Uint8Array < "u" ? Uint8Array : Array, s2 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
  for (F7 = 0, L8 = s2.length; F7 < L8; ++F7)
    h3[F7] = s2[F7], d5[s2.charCodeAt(F7)] = F7;
  var F7, L8;
  d5["-".charCodeAt(0)] = 62;
  d5["_".charCodeAt(0)] = 63;
  function g8(r3) {
    var e3 = r3.length;
    if (e3 % 4 > 0)
      throw new Error("Invalid string. Length must be a multiple of 4");
    var t2 = r3.indexOf("=");
    t2 === -1 && (t2 = e3);
    var a3 = t2 === e3 ? 0 : 4 - t2 % 4;
    return [t2, a3];
  }
  function I7(r3) {
    var e3 = g8(r3), t2 = e3[0], a3 = e3[1];
    return (t2 + a3) * 3 / 4 - a3;
  }
  function O7(r3, e3, t2) {
    return (e3 + t2) * 3 / 4 - t2;
  }
  function T7(r3) {
    var e3, t2 = g8(r3), a3 = t2[0], o = t2[1], n2 = new E7(O7(r3, a3, o)), v11 = 0, x8 = o > 0 ? a3 - 4 : a3, f7;
    for (f7 = 0; f7 < x8; f7 += 4)
      e3 = d5[r3.charCodeAt(f7)] << 18 | d5[r3.charCodeAt(f7 + 1)] << 12 | d5[r3.charCodeAt(f7 + 2)] << 6 | d5[r3.charCodeAt(f7 + 3)], n2[v11++] = e3 >> 16 & 255, n2[v11++] = e3 >> 8 & 255, n2[v11++] = e3 & 255;
    return o === 2 && (e3 = d5[r3.charCodeAt(f7)] << 2 | d5[r3.charCodeAt(f7 + 1)] >> 4, n2[v11++] = e3 & 255), o === 1 && (e3 = d5[r3.charCodeAt(f7)] << 10 | d5[r3.charCodeAt(f7 + 1)] << 4 | d5[r3.charCodeAt(f7 + 2)] >> 2, n2[v11++] = e3 >> 8 & 255, n2[v11++] = e3 & 255), n2;
  }
  function q7(r3) {
    return h3[r3 >> 18 & 63] + h3[r3 >> 12 & 63] + h3[r3 >> 6 & 63] + h3[r3 & 63];
  }
  function z8(r3, e3, t2) {
    for (var a3, o = [], n2 = e3; n2 < t2; n2 += 3)
      a3 = (r3[n2] << 16 & 16711680) + (r3[n2 + 1] << 8 & 65280) + (r3[n2 + 2] & 255), o.push(q7(a3));
    return o.join("");
  }
  function D9(r3) {
    for (var e3, t2 = r3.length, a3 = t2 % 3, o = [], n2 = 16383, v11 = 0, x8 = t2 - a3; v11 < x8; v11 += n2)
      o.push(z8(r3, v11, v11 + n2 > x8 ? x8 : v11 + n2));
    return a3 === 1 ? (e3 = r3[t2 - 1], o.push(h3[e3 >> 2] + h3[e3 << 4 & 63] + "==")) : a3 === 2 && (e3 = (r3[t2 - 2] << 8) + r3[t2 - 1], o.push(h3[e3 >> 10] + h3[e3 >> 4 & 63] + h3[e3 << 2 & 63] + "=")), o.join("");
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
var v3 = ((s2) => typeof __require < "u" ? __require : typeof Proxy < "u" ? new Proxy(s2, { get: (h3, c7) => (typeof __require < "u" ? __require : h3)[c7] }) : s2)(function(s2) {
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
var Wt = ((s2) => typeof v3 < "u" ? v3 : typeof Proxy < "u" ? new Proxy(s2, { get: (h3, c7) => (typeof v3 < "u" ? v3 : h3)[c7] }) : s2)(function(s2) {
  if (typeof v3 < "u")
    return v3.apply(this, arguments);
  throw new Error('Dynamic require of "' + s2 + '" is not supported');
});
var kt2 = (s2, h3) => () => (h3 || s2((h3 = { exports: {} }).exports, h3), h3.exports);
var jt = (s2, h3) => {
  for (var c7 in h3)
    k4(s2, c7, { get: h3[c7], enumerable: true });
};
var C4 = (s2, h3, c7, R8) => {
  if (h3 && typeof h3 == "object" || typeof h3 == "function")
    for (let b6 of Ct2(h3))
      !Nt.call(s2, b6) && b6 !== c7 && k4(s2, b6, { get: () => h3[b6], enumerable: !(R8 = Pt(h3, b6)) || R8.enumerable });
  return s2;
};
var Ft = (s2, h3, c7) => (C4(s2, h3, "default"), c7 && C4(c7, h3, "default"));
var nt2 = (s2, h3, c7) => (c7 = s2 != null ? $t(Mt(s2)) : {}, C4(h3 || !s2 || !s2.__esModule ? k4(c7, "default", { value: s2, enumerable: true }) : c7, s2));
var rt2 = kt2((s2) => {
  "use strict";
  var h3 = xt2, c7 = Lt, R8 = typeof Symbol == "function" && typeof Symbol.for == "function" ? Symbol.for("nodejs.util.inspect.custom") : null;
  s2.Buffer = i3, s2.SlowBuffer = at5, s2.INSPECT_MAX_BYTES = 50;
  var b6 = 2147483647;
  s2.kMaxLength = b6, i3.TYPED_ARRAY_SUPPORT = ft3(), !i3.TYPED_ARRAY_SUPPORT && typeof console < "u" && typeof console.error == "function" && console.error("This browser lacks typed array (Uint8Array) support which is required by `buffer` v5.x. Use `buffer` v4.x if you require old browser support.");
  function ft3() {
    try {
      let t2 = new Uint8Array(1), e3 = { foo: function() {
        return 42;
      } };
      return Object.setPrototypeOf(e3, Uint8Array.prototype), Object.setPrototypeOf(t2, e3), t2.foo() === 42;
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
  function m9(t2) {
    if (t2 > b6)
      throw new RangeError('The value "' + t2 + '" is invalid for option "size"');
    let e3 = new Uint8Array(t2);
    return Object.setPrototypeOf(e3, i3.prototype), e3;
  }
  function i3(t2, e3, n2) {
    if (typeof t2 == "number") {
      if (typeof e3 == "string")
        throw new TypeError('The "string" argument must be of type string. Received type number');
      return _9(t2);
    }
    return j10(t2, e3, n2);
  }
  i3.poolSize = 8192;
  function j10(t2, e3, n2) {
    if (typeof t2 == "string")
      return st8(t2, e3);
    if (ArrayBuffer.isView(t2))
      return ht4(t2);
    if (t2 == null)
      throw new TypeError("The first argument must be one of type string, Buffer, ArrayBuffer, Array, or Array-like Object. Received type " + typeof t2);
    if (B9(t2, ArrayBuffer) || t2 && B9(t2.buffer, ArrayBuffer) || typeof SharedArrayBuffer < "u" && (B9(t2, SharedArrayBuffer) || t2 && B9(t2.buffer, SharedArrayBuffer)))
      return D9(t2, e3, n2);
    if (typeof t2 == "number")
      throw new TypeError('The "value" argument must not be of type number. Received type number');
    let r3 = t2.valueOf && t2.valueOf();
    if (r3 != null && r3 !== t2)
      return i3.from(r3, e3, n2);
    let o = lt4(t2);
    if (o)
      return o;
    if (typeof Symbol < "u" && Symbol.toPrimitive != null && typeof t2[Symbol.toPrimitive] == "function")
      return i3.from(t2[Symbol.toPrimitive]("string"), e3, n2);
    throw new TypeError("The first argument must be one of type string, Buffer, ArrayBuffer, Array, or Array-like Object. Received type " + typeof t2);
  }
  i3.from = function(t2, e3, n2) {
    return j10(t2, e3, n2);
  }, Object.setPrototypeOf(i3.prototype, Uint8Array.prototype), Object.setPrototypeOf(i3, Uint8Array);
  function F7(t2) {
    if (typeof t2 != "number")
      throw new TypeError('"size" argument must be of type number');
    if (t2 < 0)
      throw new RangeError('The value "' + t2 + '" is invalid for option "size"');
  }
  function ut5(t2, e3, n2) {
    return F7(t2), t2 <= 0 ? m9(t2) : e3 !== void 0 ? typeof n2 == "string" ? m9(t2).fill(e3, n2) : m9(t2).fill(e3) : m9(t2);
  }
  i3.alloc = function(t2, e3, n2) {
    return ut5(t2, e3, n2);
  };
  function _9(t2) {
    return F7(t2), m9(t2 < 0 ? 0 : L8(t2) | 0);
  }
  i3.allocUnsafe = function(t2) {
    return _9(t2);
  }, i3.allocUnsafeSlow = function(t2) {
    return _9(t2);
  };
  function st8(t2, e3) {
    if ((typeof e3 != "string" || e3 === "") && (e3 = "utf8"), !i3.isEncoding(e3))
      throw new TypeError("Unknown encoding: " + e3);
    let n2 = Y8(t2, e3) | 0, r3 = m9(n2), o = r3.write(t2, e3);
    return o !== n2 && (r3 = r3.slice(0, o)), r3;
  }
  function S8(t2) {
    let e3 = t2.length < 0 ? 0 : L8(t2.length) | 0, n2 = m9(e3);
    for (let r3 = 0; r3 < e3; r3 += 1)
      n2[r3] = t2[r3] & 255;
    return n2;
  }
  function ht4(t2) {
    if (B9(t2, Uint8Array)) {
      let e3 = new Uint8Array(t2);
      return D9(e3.buffer, e3.byteOffset, e3.byteLength);
    }
    return S8(t2);
  }
  function D9(t2, e3, n2) {
    if (e3 < 0 || t2.byteLength < e3)
      throw new RangeError('"offset" is outside of buffer bounds');
    if (t2.byteLength < e3 + (n2 || 0))
      throw new RangeError('"length" is outside of buffer bounds');
    let r3;
    return e3 === void 0 && n2 === void 0 ? r3 = new Uint8Array(t2) : n2 === void 0 ? r3 = new Uint8Array(t2, e3) : r3 = new Uint8Array(t2, e3, n2), Object.setPrototypeOf(r3, i3.prototype), r3;
  }
  function lt4(t2) {
    if (i3.isBuffer(t2)) {
      let e3 = L8(t2.length) | 0, n2 = m9(e3);
      return n2.length === 0 || t2.copy(n2, 0, 0, e3), n2;
    }
    if (t2.length !== void 0)
      return typeof t2.length != "number" || P8(t2.length) ? m9(0) : S8(t2);
    if (t2.type === "Buffer" && Array.isArray(t2.data))
      return S8(t2.data);
  }
  function L8(t2) {
    if (t2 >= b6)
      throw new RangeError("Attempt to allocate Buffer larger than maximum size: 0x" + b6.toString(16) + " bytes");
    return t2 | 0;
  }
  function at5(t2) {
    return +t2 != t2 && (t2 = 0), i3.alloc(+t2);
  }
  i3.isBuffer = function(t2) {
    return t2 != null && t2._isBuffer === true && t2 !== i3.prototype;
  }, i3.compare = function(t2, e3) {
    if (B9(t2, Uint8Array) && (t2 = i3.from(t2, t2.offset, t2.byteLength)), B9(e3, Uint8Array) && (e3 = i3.from(e3, e3.offset, e3.byteLength)), !i3.isBuffer(t2) || !i3.isBuffer(e3))
      throw new TypeError('The "buf1", "buf2" arguments must be one of type Buffer or Uint8Array');
    if (t2 === e3)
      return 0;
    let n2 = t2.length, r3 = e3.length;
    for (let o = 0, f7 = Math.min(n2, r3); o < f7; ++o)
      if (t2[o] !== e3[o]) {
        n2 = t2[o], r3 = e3[o];
        break;
      }
    return n2 < r3 ? -1 : r3 < n2 ? 1 : 0;
  }, i3.isEncoding = function(t2) {
    switch (String(t2).toLowerCase()) {
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
  }, i3.concat = function(t2, e3) {
    if (!Array.isArray(t2))
      throw new TypeError('"list" argument must be an Array of Buffers');
    if (t2.length === 0)
      return i3.alloc(0);
    let n2;
    if (e3 === void 0)
      for (e3 = 0, n2 = 0; n2 < t2.length; ++n2)
        e3 += t2[n2].length;
    let r3 = i3.allocUnsafe(e3), o = 0;
    for (n2 = 0; n2 < t2.length; ++n2) {
      let f7 = t2[n2];
      if (B9(f7, Uint8Array))
        o + f7.length > r3.length ? (i3.isBuffer(f7) || (f7 = i3.from(f7)), f7.copy(r3, o)) : Uint8Array.prototype.set.call(r3, f7, o);
      else if (i3.isBuffer(f7))
        f7.copy(r3, o);
      else
        throw new TypeError('"list" argument must be an Array of Buffers');
      o += f7.length;
    }
    return r3;
  };
  function Y8(t2, e3) {
    if (i3.isBuffer(t2))
      return t2.length;
    if (ArrayBuffer.isView(t2) || B9(t2, ArrayBuffer))
      return t2.byteLength;
    if (typeof t2 != "string")
      throw new TypeError('The "string" argument must be one of type string, Buffer, or ArrayBuffer. Received type ' + typeof t2);
    let n2 = t2.length, r3 = arguments.length > 2 && arguments[2] === true;
    if (!r3 && n2 === 0)
      return 0;
    let o = false;
    for (; ; )
      switch (e3) {
        case "ascii":
        case "latin1":
        case "binary":
          return n2;
        case "utf8":
        case "utf-8":
          return $4(t2).length;
        case "ucs2":
        case "ucs-2":
        case "utf16le":
        case "utf-16le":
          return n2 * 2;
        case "hex":
          return n2 >>> 1;
        case "base64":
          return tt8(t2).length;
        default:
          if (o)
            return r3 ? -1 : $4(t2).length;
          e3 = ("" + e3).toLowerCase(), o = true;
      }
  }
  i3.byteLength = Y8;
  function pt4(t2, e3, n2) {
    let r3 = false;
    if ((e3 === void 0 || e3 < 0) && (e3 = 0), e3 > this.length || ((n2 === void 0 || n2 > this.length) && (n2 = this.length), n2 <= 0) || (n2 >>>= 0, e3 >>>= 0, n2 <= e3))
      return "";
    for (t2 || (t2 = "utf8"); ; )
      switch (t2) {
        case "hex":
          return It2(this, e3, n2);
        case "utf8":
        case "utf-8":
          return G10(this, e3, n2);
        case "ascii":
          return mt3(this, e3, n2);
        case "latin1":
        case "binary":
          return Et2(this, e3, n2);
        case "base64":
          return bt3(this, e3, n2);
        case "ucs2":
        case "ucs-2":
        case "utf16le":
        case "utf-16le":
          return vt3(this, e3, n2);
        default:
          if (r3)
            throw new TypeError("Unknown encoding: " + t2);
          t2 = (t2 + "").toLowerCase(), r3 = true;
      }
  }
  i3.prototype._isBuffer = true;
  function I7(t2, e3, n2) {
    let r3 = t2[e3];
    t2[e3] = t2[n2], t2[n2] = r3;
  }
  i3.prototype.swap16 = function() {
    let t2 = this.length;
    if (t2 % 2 !== 0)
      throw new RangeError("Buffer size must be a multiple of 16-bits");
    for (let e3 = 0; e3 < t2; e3 += 2)
      I7(this, e3, e3 + 1);
    return this;
  }, i3.prototype.swap32 = function() {
    let t2 = this.length;
    if (t2 % 4 !== 0)
      throw new RangeError("Buffer size must be a multiple of 32-bits");
    for (let e3 = 0; e3 < t2; e3 += 4)
      I7(this, e3, e3 + 3), I7(this, e3 + 1, e3 + 2);
    return this;
  }, i3.prototype.swap64 = function() {
    let t2 = this.length;
    if (t2 % 8 !== 0)
      throw new RangeError("Buffer size must be a multiple of 64-bits");
    for (let e3 = 0; e3 < t2; e3 += 8)
      I7(this, e3, e3 + 7), I7(this, e3 + 1, e3 + 6), I7(this, e3 + 2, e3 + 5), I7(this, e3 + 3, e3 + 4);
    return this;
  }, i3.prototype.toString = function() {
    let t2 = this.length;
    return t2 === 0 ? "" : arguments.length === 0 ? G10(this, 0, t2) : pt4.apply(this, arguments);
  }, i3.prototype.toLocaleString = i3.prototype.toString, i3.prototype.equals = function(t2) {
    if (!i3.isBuffer(t2))
      throw new TypeError("Argument must be a Buffer");
    return this === t2 ? true : i3.compare(this, t2) === 0;
  }, i3.prototype.inspect = function() {
    let t2 = "", e3 = s2.INSPECT_MAX_BYTES;
    return t2 = this.toString("hex", 0, e3).replace(/(.{2})/g, "$1 ").trim(), this.length > e3 && (t2 += " ... "), "<Buffer " + t2 + ">";
  }, R8 && (i3.prototype[R8] = i3.prototype.inspect), i3.prototype.compare = function(t2, e3, n2, r3, o) {
    if (B9(t2, Uint8Array) && (t2 = i3.from(t2, t2.offset, t2.byteLength)), !i3.isBuffer(t2))
      throw new TypeError('The "target" argument must be one of type Buffer or Uint8Array. Received type ' + typeof t2);
    if (e3 === void 0 && (e3 = 0), n2 === void 0 && (n2 = t2 ? t2.length : 0), r3 === void 0 && (r3 = 0), o === void 0 && (o = this.length), e3 < 0 || n2 > t2.length || r3 < 0 || o > this.length)
      throw new RangeError("out of range index");
    if (r3 >= o && e3 >= n2)
      return 0;
    if (r3 >= o)
      return -1;
    if (e3 >= n2)
      return 1;
    if (e3 >>>= 0, n2 >>>= 0, r3 >>>= 0, o >>>= 0, this === t2)
      return 0;
    let f7 = o - r3, u6 = n2 - e3, l4 = Math.min(f7, u6), w9 = this.slice(r3, o), p10 = t2.slice(e3, n2);
    for (let a3 = 0; a3 < l4; ++a3)
      if (w9[a3] !== p10[a3]) {
        f7 = w9[a3], u6 = p10[a3];
        break;
      }
    return f7 < u6 ? -1 : u6 < f7 ? 1 : 0;
  };
  function q7(t2, e3, n2, r3, o) {
    if (t2.length === 0)
      return -1;
    if (typeof n2 == "string" ? (r3 = n2, n2 = 0) : n2 > 2147483647 ? n2 = 2147483647 : n2 < -2147483648 && (n2 = -2147483648), n2 = +n2, P8(n2) && (n2 = o ? 0 : t2.length - 1), n2 < 0 && (n2 = t2.length + n2), n2 >= t2.length) {
      if (o)
        return -1;
      n2 = t2.length - 1;
    } else if (n2 < 0)
      if (o)
        n2 = 0;
      else
        return -1;
    if (typeof e3 == "string" && (e3 = i3.from(e3, r3)), i3.isBuffer(e3))
      return e3.length === 0 ? -1 : z8(t2, e3, n2, r3, o);
    if (typeof e3 == "number")
      return e3 = e3 & 255, typeof Uint8Array.prototype.indexOf == "function" ? o ? Uint8Array.prototype.indexOf.call(t2, e3, n2) : Uint8Array.prototype.lastIndexOf.call(t2, e3, n2) : z8(t2, [e3], n2, r3, o);
    throw new TypeError("val must be string, number or Buffer");
  }
  function z8(t2, e3, n2, r3, o) {
    let f7 = 1, u6 = t2.length, l4 = e3.length;
    if (r3 !== void 0 && (r3 = String(r3).toLowerCase(), r3 === "ucs2" || r3 === "ucs-2" || r3 === "utf16le" || r3 === "utf-16le")) {
      if (t2.length < 2 || e3.length < 2)
        return -1;
      f7 = 2, u6 /= 2, l4 /= 2, n2 /= 2;
    }
    function w9(a3, y14) {
      return f7 === 1 ? a3[y14] : a3.readUInt16BE(y14 * f7);
    }
    let p10;
    if (o) {
      let a3 = -1;
      for (p10 = n2; p10 < u6; p10++)
        if (w9(t2, p10) === w9(e3, a3 === -1 ? 0 : p10 - a3)) {
          if (a3 === -1 && (a3 = p10), p10 - a3 + 1 === l4)
            return a3 * f7;
        } else
          a3 !== -1 && (p10 -= p10 - a3), a3 = -1;
    } else
      for (n2 + l4 > u6 && (n2 = u6 - l4), p10 = n2; p10 >= 0; p10--) {
        let a3 = true;
        for (let y14 = 0; y14 < l4; y14++)
          if (w9(t2, p10 + y14) !== w9(e3, y14)) {
            a3 = false;
            break;
          }
        if (a3)
          return p10;
      }
    return -1;
  }
  i3.prototype.includes = function(t2, e3, n2) {
    return this.indexOf(t2, e3, n2) !== -1;
  }, i3.prototype.indexOf = function(t2, e3, n2) {
    return q7(this, t2, e3, n2, true);
  }, i3.prototype.lastIndexOf = function(t2, e3, n2) {
    return q7(this, t2, e3, n2, false);
  };
  function ct4(t2, e3, n2, r3) {
    n2 = Number(n2) || 0;
    let o = t2.length - n2;
    r3 ? (r3 = Number(r3), r3 > o && (r3 = o)) : r3 = o;
    let f7 = e3.length;
    r3 > f7 / 2 && (r3 = f7 / 2);
    let u6;
    for (u6 = 0; u6 < r3; ++u6) {
      let l4 = parseInt(e3.substr(u6 * 2, 2), 16);
      if (P8(l4))
        return u6;
      t2[n2 + u6] = l4;
    }
    return u6;
  }
  function yt2(t2, e3, n2, r3) {
    return T7($4(e3, t2.length - n2), t2, n2, r3);
  }
  function gt3(t2, e3, n2, r3) {
    return T7(Ot2(e3), t2, n2, r3);
  }
  function wt3(t2, e3, n2, r3) {
    return T7(tt8(e3), t2, n2, r3);
  }
  function dt5(t2, e3, n2, r3) {
    return T7(Tt3(e3, t2.length - n2), t2, n2, r3);
  }
  i3.prototype.write = function(t2, e3, n2, r3) {
    if (e3 === void 0)
      r3 = "utf8", n2 = this.length, e3 = 0;
    else if (n2 === void 0 && typeof e3 == "string")
      r3 = e3, n2 = this.length, e3 = 0;
    else if (isFinite(e3))
      e3 = e3 >>> 0, isFinite(n2) ? (n2 = n2 >>> 0, r3 === void 0 && (r3 = "utf8")) : (r3 = n2, n2 = void 0);
    else
      throw new Error("Buffer.write(string, encoding, offset[, length]) is no longer supported");
    let o = this.length - e3;
    if ((n2 === void 0 || n2 > o) && (n2 = o), t2.length > 0 && (n2 < 0 || e3 < 0) || e3 > this.length)
      throw new RangeError("Attempt to write outside buffer bounds");
    r3 || (r3 = "utf8");
    let f7 = false;
    for (; ; )
      switch (r3) {
        case "hex":
          return ct4(this, t2, e3, n2);
        case "utf8":
        case "utf-8":
          return yt2(this, t2, e3, n2);
        case "ascii":
        case "latin1":
        case "binary":
          return gt3(this, t2, e3, n2);
        case "base64":
          return wt3(this, t2, e3, n2);
        case "ucs2":
        case "ucs-2":
        case "utf16le":
        case "utf-16le":
          return dt5(this, t2, e3, n2);
        default:
          if (f7)
            throw new TypeError("Unknown encoding: " + r3);
          r3 = ("" + r3).toLowerCase(), f7 = true;
      }
  }, i3.prototype.toJSON = function() {
    return { type: "Buffer", data: Array.prototype.slice.call(this._arr || this, 0) };
  };
  function bt3(t2, e3, n2) {
    return e3 === 0 && n2 === t2.length ? h3.fromByteArray(t2) : h3.fromByteArray(t2.slice(e3, n2));
  }
  function G10(t2, e3, n2) {
    n2 = Math.min(t2.length, n2);
    let r3 = [], o = e3;
    for (; o < n2; ) {
      let f7 = t2[o], u6 = null, l4 = f7 > 239 ? 4 : f7 > 223 ? 3 : f7 > 191 ? 2 : 1;
      if (o + l4 <= n2) {
        let w9, p10, a3, y14;
        switch (l4) {
          case 1:
            f7 < 128 && (u6 = f7);
            break;
          case 2:
            w9 = t2[o + 1], (w9 & 192) === 128 && (y14 = (f7 & 31) << 6 | w9 & 63, y14 > 127 && (u6 = y14));
            break;
          case 3:
            w9 = t2[o + 1], p10 = t2[o + 2], (w9 & 192) === 128 && (p10 & 192) === 128 && (y14 = (f7 & 15) << 12 | (w9 & 63) << 6 | p10 & 63, y14 > 2047 && (y14 < 55296 || y14 > 57343) && (u6 = y14));
            break;
          case 4:
            w9 = t2[o + 1], p10 = t2[o + 2], a3 = t2[o + 3], (w9 & 192) === 128 && (p10 & 192) === 128 && (a3 & 192) === 128 && (y14 = (f7 & 15) << 18 | (w9 & 63) << 12 | (p10 & 63) << 6 | a3 & 63, y14 > 65535 && y14 < 1114112 && (u6 = y14));
        }
      }
      u6 === null ? (u6 = 65533, l4 = 1) : u6 > 65535 && (u6 -= 65536, r3.push(u6 >>> 10 & 1023 | 55296), u6 = 56320 | u6 & 1023), r3.push(u6), o += l4;
    }
    return Bt2(r3);
  }
  var X8 = 4096;
  function Bt2(t2) {
    let e3 = t2.length;
    if (e3 <= X8)
      return String.fromCharCode.apply(String, t2);
    let n2 = "", r3 = 0;
    for (; r3 < e3; )
      n2 += String.fromCharCode.apply(String, t2.slice(r3, r3 += X8));
    return n2;
  }
  function mt3(t2, e3, n2) {
    let r3 = "";
    n2 = Math.min(t2.length, n2);
    for (let o = e3; o < n2; ++o)
      r3 += String.fromCharCode(t2[o] & 127);
    return r3;
  }
  function Et2(t2, e3, n2) {
    let r3 = "";
    n2 = Math.min(t2.length, n2);
    for (let o = e3; o < n2; ++o)
      r3 += String.fromCharCode(t2[o]);
    return r3;
  }
  function It2(t2, e3, n2) {
    let r3 = t2.length;
    (!e3 || e3 < 0) && (e3 = 0), (!n2 || n2 < 0 || n2 > r3) && (n2 = r3);
    let o = "";
    for (let f7 = e3; f7 < n2; ++f7)
      o += _t4[t2[f7]];
    return o;
  }
  function vt3(t2, e3, n2) {
    let r3 = t2.slice(e3, n2), o = "";
    for (let f7 = 0; f7 < r3.length - 1; f7 += 2)
      o += String.fromCharCode(r3[f7] + r3[f7 + 1] * 256);
    return o;
  }
  i3.prototype.slice = function(t2, e3) {
    let n2 = this.length;
    t2 = ~~t2, e3 = e3 === void 0 ? n2 : ~~e3, t2 < 0 ? (t2 += n2, t2 < 0 && (t2 = 0)) : t2 > n2 && (t2 = n2), e3 < 0 ? (e3 += n2, e3 < 0 && (e3 = 0)) : e3 > n2 && (e3 = n2), e3 < t2 && (e3 = t2);
    let r3 = this.subarray(t2, e3);
    return Object.setPrototypeOf(r3, i3.prototype), r3;
  };
  function g8(t2, e3, n2) {
    if (t2 % 1 !== 0 || t2 < 0)
      throw new RangeError("offset is not uint");
    if (t2 + e3 > n2)
      throw new RangeError("Trying to access beyond buffer length");
  }
  i3.prototype.readUintLE = i3.prototype.readUIntLE = function(t2, e3, n2) {
    t2 = t2 >>> 0, e3 = e3 >>> 0, n2 || g8(t2, e3, this.length);
    let r3 = this[t2], o = 1, f7 = 0;
    for (; ++f7 < e3 && (o *= 256); )
      r3 += this[t2 + f7] * o;
    return r3;
  }, i3.prototype.readUintBE = i3.prototype.readUIntBE = function(t2, e3, n2) {
    t2 = t2 >>> 0, e3 = e3 >>> 0, n2 || g8(t2, e3, this.length);
    let r3 = this[t2 + --e3], o = 1;
    for (; e3 > 0 && (o *= 256); )
      r3 += this[t2 + --e3] * o;
    return r3;
  }, i3.prototype.readUint8 = i3.prototype.readUInt8 = function(t2, e3) {
    return t2 = t2 >>> 0, e3 || g8(t2, 1, this.length), this[t2];
  }, i3.prototype.readUint16LE = i3.prototype.readUInt16LE = function(t2, e3) {
    return t2 = t2 >>> 0, e3 || g8(t2, 2, this.length), this[t2] | this[t2 + 1] << 8;
  }, i3.prototype.readUint16BE = i3.prototype.readUInt16BE = function(t2, e3) {
    return t2 = t2 >>> 0, e3 || g8(t2, 2, this.length), this[t2] << 8 | this[t2 + 1];
  }, i3.prototype.readUint32LE = i3.prototype.readUInt32LE = function(t2, e3) {
    return t2 = t2 >>> 0, e3 || g8(t2, 4, this.length), (this[t2] | this[t2 + 1] << 8 | this[t2 + 2] << 16) + this[t2 + 3] * 16777216;
  }, i3.prototype.readUint32BE = i3.prototype.readUInt32BE = function(t2, e3) {
    return t2 = t2 >>> 0, e3 || g8(t2, 4, this.length), this[t2] * 16777216 + (this[t2 + 1] << 16 | this[t2 + 2] << 8 | this[t2 + 3]);
  }, i3.prototype.readBigUInt64LE = E7(function(t2) {
    t2 = t2 >>> 0, U8(t2, "offset");
    let e3 = this[t2], n2 = this[t2 + 7];
    (e3 === void 0 || n2 === void 0) && O7(t2, this.length - 8);
    let r3 = e3 + this[++t2] * 2 ** 8 + this[++t2] * 2 ** 16 + this[++t2] * 2 ** 24, o = this[++t2] + this[++t2] * 2 ** 8 + this[++t2] * 2 ** 16 + n2 * 2 ** 24;
    return BigInt(r3) + (BigInt(o) << BigInt(32));
  }), i3.prototype.readBigUInt64BE = E7(function(t2) {
    t2 = t2 >>> 0, U8(t2, "offset");
    let e3 = this[t2], n2 = this[t2 + 7];
    (e3 === void 0 || n2 === void 0) && O7(t2, this.length - 8);
    let r3 = e3 * 2 ** 24 + this[++t2] * 2 ** 16 + this[++t2] * 2 ** 8 + this[++t2], o = this[++t2] * 2 ** 24 + this[++t2] * 2 ** 16 + this[++t2] * 2 ** 8 + n2;
    return (BigInt(r3) << BigInt(32)) + BigInt(o);
  }), i3.prototype.readIntLE = function(t2, e3, n2) {
    t2 = t2 >>> 0, e3 = e3 >>> 0, n2 || g8(t2, e3, this.length);
    let r3 = this[t2], o = 1, f7 = 0;
    for (; ++f7 < e3 && (o *= 256); )
      r3 += this[t2 + f7] * o;
    return o *= 128, r3 >= o && (r3 -= Math.pow(2, 8 * e3)), r3;
  }, i3.prototype.readIntBE = function(t2, e3, n2) {
    t2 = t2 >>> 0, e3 = e3 >>> 0, n2 || g8(t2, e3, this.length);
    let r3 = e3, o = 1, f7 = this[t2 + --r3];
    for (; r3 > 0 && (o *= 256); )
      f7 += this[t2 + --r3] * o;
    return o *= 128, f7 >= o && (f7 -= Math.pow(2, 8 * e3)), f7;
  }, i3.prototype.readInt8 = function(t2, e3) {
    return t2 = t2 >>> 0, e3 || g8(t2, 1, this.length), this[t2] & 128 ? (255 - this[t2] + 1) * -1 : this[t2];
  }, i3.prototype.readInt16LE = function(t2, e3) {
    t2 = t2 >>> 0, e3 || g8(t2, 2, this.length);
    let n2 = this[t2] | this[t2 + 1] << 8;
    return n2 & 32768 ? n2 | 4294901760 : n2;
  }, i3.prototype.readInt16BE = function(t2, e3) {
    t2 = t2 >>> 0, e3 || g8(t2, 2, this.length);
    let n2 = this[t2 + 1] | this[t2] << 8;
    return n2 & 32768 ? n2 | 4294901760 : n2;
  }, i3.prototype.readInt32LE = function(t2, e3) {
    return t2 = t2 >>> 0, e3 || g8(t2, 4, this.length), this[t2] | this[t2 + 1] << 8 | this[t2 + 2] << 16 | this[t2 + 3] << 24;
  }, i3.prototype.readInt32BE = function(t2, e3) {
    return t2 = t2 >>> 0, e3 || g8(t2, 4, this.length), this[t2] << 24 | this[t2 + 1] << 16 | this[t2 + 2] << 8 | this[t2 + 3];
  }, i3.prototype.readBigInt64LE = E7(function(t2) {
    t2 = t2 >>> 0, U8(t2, "offset");
    let e3 = this[t2], n2 = this[t2 + 7];
    (e3 === void 0 || n2 === void 0) && O7(t2, this.length - 8);
    let r3 = this[t2 + 4] + this[t2 + 5] * 2 ** 8 + this[t2 + 6] * 2 ** 16 + (n2 << 24);
    return (BigInt(r3) << BigInt(32)) + BigInt(e3 + this[++t2] * 2 ** 8 + this[++t2] * 2 ** 16 + this[++t2] * 2 ** 24);
  }), i3.prototype.readBigInt64BE = E7(function(t2) {
    t2 = t2 >>> 0, U8(t2, "offset");
    let e3 = this[t2], n2 = this[t2 + 7];
    (e3 === void 0 || n2 === void 0) && O7(t2, this.length - 8);
    let r3 = (e3 << 24) + this[++t2] * 2 ** 16 + this[++t2] * 2 ** 8 + this[++t2];
    return (BigInt(r3) << BigInt(32)) + BigInt(this[++t2] * 2 ** 24 + this[++t2] * 2 ** 16 + this[++t2] * 2 ** 8 + n2);
  }), i3.prototype.readFloatLE = function(t2, e3) {
    return t2 = t2 >>> 0, e3 || g8(t2, 4, this.length), c7.read(this, t2, true, 23, 4);
  }, i3.prototype.readFloatBE = function(t2, e3) {
    return t2 = t2 >>> 0, e3 || g8(t2, 4, this.length), c7.read(this, t2, false, 23, 4);
  }, i3.prototype.readDoubleLE = function(t2, e3) {
    return t2 = t2 >>> 0, e3 || g8(t2, 8, this.length), c7.read(this, t2, true, 52, 8);
  }, i3.prototype.readDoubleBE = function(t2, e3) {
    return t2 = t2 >>> 0, e3 || g8(t2, 8, this.length), c7.read(this, t2, false, 52, 8);
  };
  function d5(t2, e3, n2, r3, o, f7) {
    if (!i3.isBuffer(t2))
      throw new TypeError('"buffer" argument must be a Buffer instance');
    if (e3 > o || e3 < f7)
      throw new RangeError('"value" argument is out of bounds');
    if (n2 + r3 > t2.length)
      throw new RangeError("Index out of range");
  }
  i3.prototype.writeUintLE = i3.prototype.writeUIntLE = function(t2, e3, n2, r3) {
    if (t2 = +t2, e3 = e3 >>> 0, n2 = n2 >>> 0, !r3) {
      let u6 = Math.pow(2, 8 * n2) - 1;
      d5(this, t2, e3, n2, u6, 0);
    }
    let o = 1, f7 = 0;
    for (this[e3] = t2 & 255; ++f7 < n2 && (o *= 256); )
      this[e3 + f7] = t2 / o & 255;
    return e3 + n2;
  }, i3.prototype.writeUintBE = i3.prototype.writeUIntBE = function(t2, e3, n2, r3) {
    if (t2 = +t2, e3 = e3 >>> 0, n2 = n2 >>> 0, !r3) {
      let u6 = Math.pow(2, 8 * n2) - 1;
      d5(this, t2, e3, n2, u6, 0);
    }
    let o = n2 - 1, f7 = 1;
    for (this[e3 + o] = t2 & 255; --o >= 0 && (f7 *= 256); )
      this[e3 + o] = t2 / f7 & 255;
    return e3 + n2;
  }, i3.prototype.writeUint8 = i3.prototype.writeUInt8 = function(t2, e3, n2) {
    return t2 = +t2, e3 = e3 >>> 0, n2 || d5(this, t2, e3, 1, 255, 0), this[e3] = t2 & 255, e3 + 1;
  }, i3.prototype.writeUint16LE = i3.prototype.writeUInt16LE = function(t2, e3, n2) {
    return t2 = +t2, e3 = e3 >>> 0, n2 || d5(this, t2, e3, 2, 65535, 0), this[e3] = t2 & 255, this[e3 + 1] = t2 >>> 8, e3 + 2;
  }, i3.prototype.writeUint16BE = i3.prototype.writeUInt16BE = function(t2, e3, n2) {
    return t2 = +t2, e3 = e3 >>> 0, n2 || d5(this, t2, e3, 2, 65535, 0), this[e3] = t2 >>> 8, this[e3 + 1] = t2 & 255, e3 + 2;
  }, i3.prototype.writeUint32LE = i3.prototype.writeUInt32LE = function(t2, e3, n2) {
    return t2 = +t2, e3 = e3 >>> 0, n2 || d5(this, t2, e3, 4, 4294967295, 0), this[e3 + 3] = t2 >>> 24, this[e3 + 2] = t2 >>> 16, this[e3 + 1] = t2 >>> 8, this[e3] = t2 & 255, e3 + 4;
  }, i3.prototype.writeUint32BE = i3.prototype.writeUInt32BE = function(t2, e3, n2) {
    return t2 = +t2, e3 = e3 >>> 0, n2 || d5(this, t2, e3, 4, 4294967295, 0), this[e3] = t2 >>> 24, this[e3 + 1] = t2 >>> 16, this[e3 + 2] = t2 >>> 8, this[e3 + 3] = t2 & 255, e3 + 4;
  };
  function V7(t2, e3, n2, r3, o) {
    Q8(e3, r3, o, t2, n2, 7);
    let f7 = Number(e3 & BigInt(4294967295));
    t2[n2++] = f7, f7 = f7 >> 8, t2[n2++] = f7, f7 = f7 >> 8, t2[n2++] = f7, f7 = f7 >> 8, t2[n2++] = f7;
    let u6 = Number(e3 >> BigInt(32) & BigInt(4294967295));
    return t2[n2++] = u6, u6 = u6 >> 8, t2[n2++] = u6, u6 = u6 >> 8, t2[n2++] = u6, u6 = u6 >> 8, t2[n2++] = u6, n2;
  }
  function W8(t2, e3, n2, r3, o) {
    Q8(e3, r3, o, t2, n2, 7);
    let f7 = Number(e3 & BigInt(4294967295));
    t2[n2 + 7] = f7, f7 = f7 >> 8, t2[n2 + 6] = f7, f7 = f7 >> 8, t2[n2 + 5] = f7, f7 = f7 >> 8, t2[n2 + 4] = f7;
    let u6 = Number(e3 >> BigInt(32) & BigInt(4294967295));
    return t2[n2 + 3] = u6, u6 = u6 >> 8, t2[n2 + 2] = u6, u6 = u6 >> 8, t2[n2 + 1] = u6, u6 = u6 >> 8, t2[n2] = u6, n2 + 8;
  }
  i3.prototype.writeBigUInt64LE = E7(function(t2, e3 = 0) {
    return V7(this, t2, e3, BigInt(0), BigInt("0xffffffffffffffff"));
  }), i3.prototype.writeBigUInt64BE = E7(function(t2, e3 = 0) {
    return W8(this, t2, e3, BigInt(0), BigInt("0xffffffffffffffff"));
  }), i3.prototype.writeIntLE = function(t2, e3, n2, r3) {
    if (t2 = +t2, e3 = e3 >>> 0, !r3) {
      let l4 = Math.pow(2, 8 * n2 - 1);
      d5(this, t2, e3, n2, l4 - 1, -l4);
    }
    let o = 0, f7 = 1, u6 = 0;
    for (this[e3] = t2 & 255; ++o < n2 && (f7 *= 256); )
      t2 < 0 && u6 === 0 && this[e3 + o - 1] !== 0 && (u6 = 1), this[e3 + o] = (t2 / f7 >> 0) - u6 & 255;
    return e3 + n2;
  }, i3.prototype.writeIntBE = function(t2, e3, n2, r3) {
    if (t2 = +t2, e3 = e3 >>> 0, !r3) {
      let l4 = Math.pow(2, 8 * n2 - 1);
      d5(this, t2, e3, n2, l4 - 1, -l4);
    }
    let o = n2 - 1, f7 = 1, u6 = 0;
    for (this[e3 + o] = t2 & 255; --o >= 0 && (f7 *= 256); )
      t2 < 0 && u6 === 0 && this[e3 + o + 1] !== 0 && (u6 = 1), this[e3 + o] = (t2 / f7 >> 0) - u6 & 255;
    return e3 + n2;
  }, i3.prototype.writeInt8 = function(t2, e3, n2) {
    return t2 = +t2, e3 = e3 >>> 0, n2 || d5(this, t2, e3, 1, 127, -128), t2 < 0 && (t2 = 255 + t2 + 1), this[e3] = t2 & 255, e3 + 1;
  }, i3.prototype.writeInt16LE = function(t2, e3, n2) {
    return t2 = +t2, e3 = e3 >>> 0, n2 || d5(this, t2, e3, 2, 32767, -32768), this[e3] = t2 & 255, this[e3 + 1] = t2 >>> 8, e3 + 2;
  }, i3.prototype.writeInt16BE = function(t2, e3, n2) {
    return t2 = +t2, e3 = e3 >>> 0, n2 || d5(this, t2, e3, 2, 32767, -32768), this[e3] = t2 >>> 8, this[e3 + 1] = t2 & 255, e3 + 2;
  }, i3.prototype.writeInt32LE = function(t2, e3, n2) {
    return t2 = +t2, e3 = e3 >>> 0, n2 || d5(this, t2, e3, 4, 2147483647, -2147483648), this[e3] = t2 & 255, this[e3 + 1] = t2 >>> 8, this[e3 + 2] = t2 >>> 16, this[e3 + 3] = t2 >>> 24, e3 + 4;
  }, i3.prototype.writeInt32BE = function(t2, e3, n2) {
    return t2 = +t2, e3 = e3 >>> 0, n2 || d5(this, t2, e3, 4, 2147483647, -2147483648), t2 < 0 && (t2 = 4294967295 + t2 + 1), this[e3] = t2 >>> 24, this[e3 + 1] = t2 >>> 16, this[e3 + 2] = t2 >>> 8, this[e3 + 3] = t2 & 255, e3 + 4;
  }, i3.prototype.writeBigInt64LE = E7(function(t2, e3 = 0) {
    return V7(this, t2, e3, -BigInt("0x8000000000000000"), BigInt("0x7fffffffffffffff"));
  }), i3.prototype.writeBigInt64BE = E7(function(t2, e3 = 0) {
    return W8(this, t2, e3, -BigInt("0x8000000000000000"), BigInt("0x7fffffffffffffff"));
  });
  function J10(t2, e3, n2, r3, o, f7) {
    if (n2 + r3 > t2.length)
      throw new RangeError("Index out of range");
    if (n2 < 0)
      throw new RangeError("Index out of range");
  }
  function Z7(t2, e3, n2, r3, o) {
    return e3 = +e3, n2 = n2 >>> 0, o || J10(t2, e3, n2, 4, 34028234663852886e22, -34028234663852886e22), c7.write(t2, e3, n2, r3, 23, 4), n2 + 4;
  }
  i3.prototype.writeFloatLE = function(t2, e3, n2) {
    return Z7(this, t2, e3, true, n2);
  }, i3.prototype.writeFloatBE = function(t2, e3, n2) {
    return Z7(this, t2, e3, false, n2);
  };
  function H10(t2, e3, n2, r3, o) {
    return e3 = +e3, n2 = n2 >>> 0, o || J10(t2, e3, n2, 8, 17976931348623157e292, -17976931348623157e292), c7.write(t2, e3, n2, r3, 52, 8), n2 + 8;
  }
  i3.prototype.writeDoubleLE = function(t2, e3, n2) {
    return H10(this, t2, e3, true, n2);
  }, i3.prototype.writeDoubleBE = function(t2, e3, n2) {
    return H10(this, t2, e3, false, n2);
  }, i3.prototype.copy = function(t2, e3, n2, r3) {
    if (!i3.isBuffer(t2))
      throw new TypeError("argument should be a Buffer");
    if (n2 || (n2 = 0), !r3 && r3 !== 0 && (r3 = this.length), e3 >= t2.length && (e3 = t2.length), e3 || (e3 = 0), r3 > 0 && r3 < n2 && (r3 = n2), r3 === n2 || t2.length === 0 || this.length === 0)
      return 0;
    if (e3 < 0)
      throw new RangeError("targetStart out of bounds");
    if (n2 < 0 || n2 >= this.length)
      throw new RangeError("Index out of range");
    if (r3 < 0)
      throw new RangeError("sourceEnd out of bounds");
    r3 > this.length && (r3 = this.length), t2.length - e3 < r3 - n2 && (r3 = t2.length - e3 + n2);
    let o = r3 - n2;
    return this === t2 && typeof Uint8Array.prototype.copyWithin == "function" ? this.copyWithin(e3, n2, r3) : Uint8Array.prototype.set.call(t2, this.subarray(n2, r3), e3), o;
  }, i3.prototype.fill = function(t2, e3, n2, r3) {
    if (typeof t2 == "string") {
      if (typeof e3 == "string" ? (r3 = e3, e3 = 0, n2 = this.length) : typeof n2 == "string" && (r3 = n2, n2 = this.length), r3 !== void 0 && typeof r3 != "string")
        throw new TypeError("encoding must be a string");
      if (typeof r3 == "string" && !i3.isEncoding(r3))
        throw new TypeError("Unknown encoding: " + r3);
      if (t2.length === 1) {
        let f7 = t2.charCodeAt(0);
        (r3 === "utf8" && f7 < 128 || r3 === "latin1") && (t2 = f7);
      }
    } else
      typeof t2 == "number" ? t2 = t2 & 255 : typeof t2 == "boolean" && (t2 = Number(t2));
    if (e3 < 0 || this.length < e3 || this.length < n2)
      throw new RangeError("Out of range index");
    if (n2 <= e3)
      return this;
    e3 = e3 >>> 0, n2 = n2 === void 0 ? this.length : n2 >>> 0, t2 || (t2 = 0);
    let o;
    if (typeof t2 == "number")
      for (o = e3; o < n2; ++o)
        this[o] = t2;
    else {
      let f7 = i3.isBuffer(t2) ? t2 : i3.from(t2, r3), u6 = f7.length;
      if (u6 === 0)
        throw new TypeError('The value "' + t2 + '" is invalid for argument "value"');
      for (o = 0; o < n2 - e3; ++o)
        this[o + e3] = f7[o % u6];
    }
    return this;
  };
  var A5 = {};
  function x8(t2, e3, n2) {
    A5[t2] = class extends n2 {
      constructor() {
        super(), Object.defineProperty(this, "message", { value: e3.apply(this, arguments), writable: true, configurable: true }), this.name = `${this.name} [${t2}]`, this.stack, delete this.name;
      }
      get code() {
        return t2;
      }
      set code(r3) {
        Object.defineProperty(this, "code", { configurable: true, enumerable: true, value: r3, writable: true });
      }
      toString() {
        return `${this.name} [${t2}]: ${this.message}`;
      }
    };
  }
  x8("ERR_BUFFER_OUT_OF_BOUNDS", function(t2) {
    return t2 ? `${t2} is outside of buffer bounds` : "Attempt to access memory outside buffer bounds";
  }, RangeError), x8("ERR_INVALID_ARG_TYPE", function(t2, e3) {
    return `The "${t2}" argument must be of type number. Received type ${typeof e3}`;
  }, TypeError), x8("ERR_OUT_OF_RANGE", function(t2, e3, n2) {
    let r3 = `The value of "${t2}" is out of range.`, o = n2;
    return Number.isInteger(n2) && Math.abs(n2) > 2 ** 32 ? o = K9(String(n2)) : typeof n2 == "bigint" && (o = String(n2), (n2 > BigInt(2) ** BigInt(32) || n2 < -(BigInt(2) ** BigInt(32))) && (o = K9(o)), o += "n"), r3 += ` It must be ${e3}. Received ${o}`, r3;
  }, RangeError);
  function K9(t2) {
    let e3 = "", n2 = t2.length, r3 = t2[0] === "-" ? 1 : 0;
    for (; n2 >= r3 + 4; n2 -= 3)
      e3 = `_${t2.slice(n2 - 3, n2)}${e3}`;
    return `${t2.slice(0, n2)}${e3}`;
  }
  function At3(t2, e3, n2) {
    U8(e3, "offset"), (t2[e3] === void 0 || t2[e3 + n2] === void 0) && O7(e3, t2.length - (n2 + 1));
  }
  function Q8(t2, e3, n2, r3, o, f7) {
    if (t2 > n2 || t2 < e3) {
      let u6 = typeof e3 == "bigint" ? "n" : "", l4;
      throw f7 > 3 ? e3 === 0 || e3 === BigInt(0) ? l4 = `>= 0${u6} and < 2${u6} ** ${(f7 + 1) * 8}${u6}` : l4 = `>= -(2${u6} ** ${(f7 + 1) * 8 - 1}${u6}) and < 2 ** ${(f7 + 1) * 8 - 1}${u6}` : l4 = `>= ${e3}${u6} and <= ${n2}${u6}`, new A5.ERR_OUT_OF_RANGE("value", l4, t2);
    }
    At3(r3, o, f7);
  }
  function U8(t2, e3) {
    if (typeof t2 != "number")
      throw new A5.ERR_INVALID_ARG_TYPE(e3, "number", t2);
  }
  function O7(t2, e3, n2) {
    throw Math.floor(t2) !== t2 ? (U8(t2, n2), new A5.ERR_OUT_OF_RANGE(n2 || "offset", "an integer", t2)) : e3 < 0 ? new A5.ERR_BUFFER_OUT_OF_BOUNDS() : new A5.ERR_OUT_OF_RANGE(n2 || "offset", `>= ${n2 ? 1 : 0} and <= ${e3}`, t2);
  }
  var Ut = /[^+/0-9A-Za-z-_]/g;
  function Rt2(t2) {
    if (t2 = t2.split("=")[0], t2 = t2.trim().replace(Ut, ""), t2.length < 2)
      return "";
    for (; t2.length % 4 !== 0; )
      t2 = t2 + "=";
    return t2;
  }
  function $4(t2, e3) {
    e3 = e3 || 1 / 0;
    let n2, r3 = t2.length, o = null, f7 = [];
    for (let u6 = 0; u6 < r3; ++u6) {
      if (n2 = t2.charCodeAt(u6), n2 > 55295 && n2 < 57344) {
        if (!o) {
          if (n2 > 56319) {
            (e3 -= 3) > -1 && f7.push(239, 191, 189);
            continue;
          } else if (u6 + 1 === r3) {
            (e3 -= 3) > -1 && f7.push(239, 191, 189);
            continue;
          }
          o = n2;
          continue;
        }
        if (n2 < 56320) {
          (e3 -= 3) > -1 && f7.push(239, 191, 189), o = n2;
          continue;
        }
        n2 = (o - 55296 << 10 | n2 - 56320) + 65536;
      } else
        o && (e3 -= 3) > -1 && f7.push(239, 191, 189);
      if (o = null, n2 < 128) {
        if ((e3 -= 1) < 0)
          break;
        f7.push(n2);
      } else if (n2 < 2048) {
        if ((e3 -= 2) < 0)
          break;
        f7.push(n2 >> 6 | 192, n2 & 63 | 128);
      } else if (n2 < 65536) {
        if ((e3 -= 3) < 0)
          break;
        f7.push(n2 >> 12 | 224, n2 >> 6 & 63 | 128, n2 & 63 | 128);
      } else if (n2 < 1114112) {
        if ((e3 -= 4) < 0)
          break;
        f7.push(n2 >> 18 | 240, n2 >> 12 & 63 | 128, n2 >> 6 & 63 | 128, n2 & 63 | 128);
      } else
        throw new Error("Invalid code point");
    }
    return f7;
  }
  function Ot2(t2) {
    let e3 = [];
    for (let n2 = 0; n2 < t2.length; ++n2)
      e3.push(t2.charCodeAt(n2) & 255);
    return e3;
  }
  function Tt3(t2, e3) {
    let n2, r3, o, f7 = [];
    for (let u6 = 0; u6 < t2.length && !((e3 -= 2) < 0); ++u6)
      n2 = t2.charCodeAt(u6), r3 = n2 >> 8, o = n2 % 256, f7.push(o), f7.push(r3);
    return f7;
  }
  function tt8(t2) {
    return h3.toByteArray(Rt2(t2));
  }
  function T7(t2, e3, n2, r3) {
    let o;
    for (o = 0; o < r3 && !(o + n2 >= e3.length || o >= t2.length); ++o)
      e3[o + n2] = t2[o];
    return o;
  }
  function B9(t2, e3) {
    return t2 instanceof e3 || t2 != null && t2.constructor != null && t2.constructor.name != null && t2.constructor.name === e3.name;
  }
  function P8(t2) {
    return t2 !== t2;
  }
  var _t4 = function() {
    let t2 = "0123456789abcdef", e3 = new Array(256);
    for (let n2 = 0; n2 < 16; ++n2) {
      let r3 = n2 * 16;
      for (let o = 0; o < 16; ++o)
        e3[r3 + o] = t2[n2] + t2[o];
    }
    return e3;
  }();
  function E7(t2) {
    return typeof BigInt > "u" ? St3 : t2;
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
  constructor(t2) {
    if (t2 && ((t2.keyMap || t2._keyMap) && !t2.useRecords && (t2.useRecords = false, t2.mapsAsObjects = true), t2.useRecords === false && t2.mapsAsObjects === void 0 && (t2.mapsAsObjects = true), t2.getStructures && (t2.getShared = t2.getStructures), t2.getShared && !t2.structures && ((t2.structures = []).uninitialized = true), t2.keyMap)) {
      this.mapKey = /* @__PURE__ */ new Map();
      for (let [l4, n2] of Object.entries(t2.keyMap))
        this.mapKey.set(n2, l4);
    }
    Object.assign(this, t2);
  }
  decodeKey(t2) {
    return this.keyMap && this.mapKey.get(t2) || t2;
  }
  encodeKey(t2) {
    return this.keyMap && this.keyMap.hasOwnProperty(t2) ? this.keyMap[t2] : t2;
  }
  encodeKeys(t2) {
    if (!this._keyMap)
      return t2;
    let l4 = /* @__PURE__ */ new Map();
    for (let [n2, f7] of Object.entries(t2))
      l4.set(this._keyMap.hasOwnProperty(n2) ? this._keyMap[n2] : n2, f7);
    return l4;
  }
  decodeKeys(t2) {
    if (!this._keyMap || t2.constructor.name != "Map")
      return t2;
    if (!this._mapKey) {
      this._mapKey = /* @__PURE__ */ new Map();
      for (let [n2, f7] of Object.entries(this._keyMap))
        this._mapKey.set(f7, n2);
    }
    let l4 = {};
    return t2.forEach((n2, f7) => l4[j4(this._mapKey.has(f7) ? this._mapKey.get(f7) : f7)] = n2), l4;
  }
  mapDecode(t2, l4) {
    let n2 = this.decode(t2);
    if (this._keyMap)
      switch (n2.constructor.name) {
        case "Array":
          return n2.map((f7) => this.decodeKeys(f7));
      }
    return n2;
  }
  decode(t2, l4) {
    if (y3)
      return nt3(() => (ge2(), this ? this.decode(t2, l4) : Y2.prototype.decode.call(Xe2, t2, l4)));
    v4 = l4 > -1 ? l4 : t2.length, a = 0, _e2 = 0, ae2 = 0, ye2 = null, Me2 = ve2, T2 = null, y3 = t2;
    try {
      C5 = t2.dataView || (t2.dataView = new DataView(t2.buffer, t2.byteOffset, t2.byteLength));
    } catch (n2) {
      throw y3 = null, t2 instanceof Uint8Array ? n2 : new Error("Source must be a Uint8Array or Buffer but was a " + (t2 && typeof t2 == "object" ? t2.constructor.name : typeof t2));
    }
    if (this instanceof Y2) {
      if (E2 = this, P2 = this.sharedValues && (this.pack ? new Array(this.maxPrivatePackedValues || 16).concat(this.sharedValues) : this.sharedValues), this.structures)
        return _3 = this.structures, he2();
      (!_3 || _3.length > 0) && (_3 = []);
    } else
      E2 = Xe2, (!_3 || _3.length > 0) && (_3 = []), P2 = null;
    return he2();
  }
  decodeMultiple(t2, l4) {
    let n2, f7 = 0;
    try {
      let o = t2.length;
      ce2 = true;
      let d5 = this ? this.decode(t2, o) : Ce2.decode(t2, o);
      if (l4) {
        if (l4(d5) === false)
          return;
        for (; a < o; )
          if (f7 = a, l4(he2()) === false)
            return;
      } else {
        for (n2 = [d5]; a < o; )
          f7 = a, n2.push(he2());
        return n2;
      }
    } catch (o) {
      throw o.lastPosition = f7, o.values = n2, o;
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
        let t2 = new Error("Unexpected bundle position");
        throw t2.incomplete = true, t2;
      }
      a = T2.postBundlePosition, T2 = null;
    }
    if (a == v4)
      _3 = null, y3 = null, W2 && (W2 = null);
    else if (a > v4) {
      let t2 = new Error("Unexpected end of CBOR data");
      throw t2.incomplete = true, t2;
    } else if (!ce2)
      throw new Error("Data read, but end of buffer not reached");
    return e3;
  } catch (e3) {
    throw ge2(), (e3 instanceof RangeError || e3.message.startsWith("Unexpected end of buffer")) && (e3.incomplete = true), e3;
  }
}
function S2() {
  let e3 = y3[a++], t2 = e3 >> 5;
  if (e3 = e3 & 31, e3 > 23)
    switch (e3) {
      case 24:
        e3 = y3[a++];
        break;
      case 25:
        if (t2 == 7)
          return wt2();
        e3 = C5.getUint16(a), a += 2;
        break;
      case 26:
        if (t2 == 7) {
          let l4 = C5.getFloat32(a);
          if (E2.useFloat32 > 2) {
            let n2 = ue2[(y3[a] & 127) << 1 | y3[a + 1] >> 7];
            return a += 4, (n2 * l4 + (l4 > 0 ? 0.5 : -0.5) >> 0) / n2;
          }
          return a += 4, l4;
        }
        e3 = C5.getUint32(a), a += 4;
        break;
      case 27:
        if (t2 == 7) {
          let l4 = C5.getFloat64(a);
          return a += 8, l4;
        }
        if (t2 > 1) {
          if (C5.getUint32(a) > 0)
            throw new Error("JavaScript does not support arrays, maps, or strings with length over 4294967295");
          e3 = C5.getUint32(a + 4);
        } else
          E2.int64AsNumber ? (e3 = C5.getUint32(a) * 4294967296, e3 += C5.getUint32(a + 4)) : e3 = C5.getBigUint64(a);
        a += 8;
        break;
      case 31:
        switch (t2) {
          case 2:
          case 3:
            throw new Error("Indefinite length not supported for byte or text strings");
          case 4:
            let l4 = [], n2, f7 = 0;
            for (; (n2 = S2()) != re2; )
              l4[f7++] = n2;
            return t2 == 4 ? l4 : t2 == 3 ? l4.join("") : Dt.concat(l4);
          case 5:
            let o;
            if (E2.mapsAsObjects) {
              let d5 = {};
              if (E2.keyMap)
                for (; (o = S2()) != re2; )
                  d5[j4(E2.decodeKey(o))] = S2();
              else
                for (; (o = S2()) != re2; )
                  d5[j4(o)] = S2();
              return d5;
            } else {
              oe2 && (E2.mapsAsObjects = true, oe2 = false);
              let d5 = /* @__PURE__ */ new Map();
              if (E2.keyMap)
                for (; (o = S2()) != re2; )
                  d5.set(E2.decodeKey(o), S2());
              else
                for (; (o = S2()) != re2; )
                  d5.set(o, S2());
              return d5;
            }
          case 7:
            return re2;
          default:
            throw new Error("Invalid major type for indefinite length " + t2);
        }
      default:
        throw new Error("Unknown token " + e3);
    }
  switch (t2) {
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
        let f7 = e3 < 16 ? tt2(e3) : pt2(e3);
        if (f7 != null)
          return f7;
      }
      return ht2(e3);
    case 4:
      let l4 = new Array(e3);
      for (let f7 = 0; f7 < e3; f7++)
        l4[f7] = S2();
      return l4;
    case 5:
      if (E2.mapsAsObjects) {
        let f7 = {};
        if (E2.keyMap)
          for (let o = 0; o < e3; o++)
            f7[j4(E2.decodeKey(S2()))] = S2();
        else
          for (let o = 0; o < e3; o++)
            f7[j4(S2())] = S2();
        return f7;
      } else {
        oe2 && (E2.mapsAsObjects = true, oe2 = false);
        let f7 = /* @__PURE__ */ new Map();
        if (E2.keyMap)
          for (let o = 0; o < e3; o++)
            f7.set(E2.decodeKey(S2()), S2());
        else
          for (let o = 0; o < e3; o++)
            f7.set(S2(), S2());
        return f7;
      }
    case 6:
      if (e3 >= qe2) {
        let f7 = _3[e3 & 8191];
        if (f7)
          return f7.read || (f7.read = Be2(f7)), f7.read();
        if (e3 < 65536) {
          if (e3 == xt3) {
            let o = se2(), d5 = S2(), w9 = S2();
            De2(d5, w9);
            let U8 = {};
            if (E2.keyMap)
              for (let p10 = 2; p10 < o; p10++) {
                let B9 = E2.decodeKey(w9[p10 - 2]);
                U8[j4(B9)] = S2();
              }
            else
              for (let p10 = 2; p10 < o; p10++) {
                let B9 = w9[p10 - 2];
                U8[j4(B9)] = S2();
              }
            return U8;
          } else if (e3 == dt2) {
            let o = se2(), d5 = S2();
            for (let w9 = 2; w9 < o; w9++)
              De2(d5++, S2());
            return S2();
          } else if (e3 == qe2)
            return It();
          if (E2.getShared && (Ve2(), f7 = _3[e3 & 8191], f7))
            return f7.read || (f7.read = Be2(f7)), f7.read();
        }
      }
      let n2 = R2[e3];
      if (n2)
        return n2.handlesRead ? n2(S2) : n2(S2());
      {
        let f7 = S2();
        for (let o = 0; o < Re2.length; o++) {
          let d5 = Re2[o](e3, f7);
          if (d5 !== void 0)
            return d5;
        }
        return new H4(f7, e3);
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
          let f7 = (P2 || Q2())[e3];
          if (f7 !== void 0)
            return f7;
          throw new Error("Unknown token " + e3);
      }
    default:
      if (isNaN(e3)) {
        let f7 = new Error("Unexpected end of CBOR data");
        throw f7.incomplete = true, f7;
      }
      throw new Error("Unknown CBOR token " + e3);
  }
}
var $e2 = /^[a-zA-Z_$][a-zA-Z\d_$]*$/;
function Be2(e3) {
  function t2() {
    let l4 = y3[a++];
    if (l4 = l4 & 31, l4 > 23)
      switch (l4) {
        case 24:
          l4 = y3[a++];
          break;
        case 25:
          l4 = C5.getUint16(a), a += 2;
          break;
        case 26:
          l4 = C5.getUint32(a), a += 4;
          break;
        default:
          throw new Error("Expected array header, but got " + y3[a - 1]);
      }
    let n2 = this.compiledReader;
    for (; n2; ) {
      if (n2.propertyCount === l4)
        return n2(S2);
      n2 = n2.next;
    }
    if (this.slowReads++ >= et3) {
      let o = this.length == l4 ? this : this.slice(0, l4);
      return n2 = E2.keyMap ? new Function("r", "return {" + o.map((d5) => E2.decodeKey(d5)).map((d5) => $e2.test(d5) ? j4(d5) + ":r()" : "[" + JSON.stringify(d5) + "]:r()").join(",") + "}") : new Function("r", "return {" + o.map((d5) => $e2.test(d5) ? j4(d5) + ":r()" : "[" + JSON.stringify(d5) + "]:r()").join(",") + "}"), this.compiledReader && (n2.next = this.compiledReader), n2.propertyCount = l4, this.compiledReader = n2, n2(S2);
    }
    let f7 = {};
    if (E2.keyMap)
      for (let o = 0; o < l4; o++)
        f7[j4(E2.decodeKey(this[o]))] = S2();
    else
      for (let o = 0; o < l4; o++)
        f7[j4(this[o])] = S2();
    return f7;
  }
  return e3.slowReads = 0, t2;
}
function j4(e3) {
  return e3 === "__proto__" ? "__proto_" : e3;
}
var ht2 = Te2;
function Te2(e3) {
  let t2;
  if (e3 < 16 && (t2 = tt2(e3)))
    return t2;
  if (e3 > 64 && Ue2)
    return Ue2.decode(y3.subarray(a, a += e3));
  let l4 = a + e3, n2 = [];
  for (t2 = ""; a < l4; ) {
    let f7 = y3[a++];
    if (!(f7 & 128))
      n2.push(f7);
    else if ((f7 & 224) === 192) {
      let o = y3[a++] & 63;
      n2.push((f7 & 31) << 6 | o);
    } else if ((f7 & 240) === 224) {
      let o = y3[a++] & 63, d5 = y3[a++] & 63;
      n2.push((f7 & 31) << 12 | o << 6 | d5);
    } else if ((f7 & 248) === 240) {
      let o = y3[a++] & 63, d5 = y3[a++] & 63, w9 = y3[a++] & 63, U8 = (f7 & 7) << 18 | o << 12 | d5 << 6 | w9;
      U8 > 65535 && (U8 -= 65536, n2.push(U8 >>> 10 & 1023 | 55296), U8 = 56320 | U8 & 1023), n2.push(U8);
    } else
      n2.push(f7);
    n2.length >= 4096 && (t2 += F2.apply(String, n2), n2.length = 0);
  }
  return n2.length > 0 && (t2 += F2.apply(String, n2)), t2;
}
var F2 = String.fromCharCode;
function pt2(e3) {
  let t2 = a, l4 = new Array(e3);
  for (let n2 = 0; n2 < e3; n2++) {
    let f7 = y3[a++];
    if ((f7 & 128) > 0) {
      a = t2;
      return;
    }
    l4[n2] = f7;
  }
  return F2.apply(String, l4);
}
function tt2(e3) {
  if (e3 < 4)
    if (e3 < 2) {
      if (e3 === 0)
        return "";
      {
        let t2 = y3[a++];
        if ((t2 & 128) > 1) {
          a -= 1;
          return;
        }
        return F2(t2);
      }
    } else {
      let t2 = y3[a++], l4 = y3[a++];
      if ((t2 & 128) > 0 || (l4 & 128) > 0) {
        a -= 2;
        return;
      }
      if (e3 < 3)
        return F2(t2, l4);
      let n2 = y3[a++];
      if ((n2 & 128) > 0) {
        a -= 3;
        return;
      }
      return F2(t2, l4, n2);
    }
  else {
    let t2 = y3[a++], l4 = y3[a++], n2 = y3[a++], f7 = y3[a++];
    if ((t2 & 128) > 0 || (l4 & 128) > 0 || (n2 & 128) > 0 || (f7 & 128) > 0) {
      a -= 4;
      return;
    }
    if (e3 < 6) {
      if (e3 === 4)
        return F2(t2, l4, n2, f7);
      {
        let o = y3[a++];
        if ((o & 128) > 0) {
          a -= 5;
          return;
        }
        return F2(t2, l4, n2, f7, o);
      }
    } else if (e3 < 8) {
      let o = y3[a++], d5 = y3[a++];
      if ((o & 128) > 0 || (d5 & 128) > 0) {
        a -= 6;
        return;
      }
      if (e3 < 7)
        return F2(t2, l4, n2, f7, o, d5);
      let w9 = y3[a++];
      if ((w9 & 128) > 0) {
        a -= 7;
        return;
      }
      return F2(t2, l4, n2, f7, o, d5, w9);
    } else {
      let o = y3[a++], d5 = y3[a++], w9 = y3[a++], U8 = y3[a++];
      if ((o & 128) > 0 || (d5 & 128) > 0 || (w9 & 128) > 0 || (U8 & 128) > 0) {
        a -= 8;
        return;
      }
      if (e3 < 10) {
        if (e3 === 8)
          return F2(t2, l4, n2, f7, o, d5, w9, U8);
        {
          let p10 = y3[a++];
          if ((p10 & 128) > 0) {
            a -= 9;
            return;
          }
          return F2(t2, l4, n2, f7, o, d5, w9, U8, p10);
        }
      } else if (e3 < 12) {
        let p10 = y3[a++], B9 = y3[a++];
        if ((p10 & 128) > 0 || (B9 & 128) > 0) {
          a -= 10;
          return;
        }
        if (e3 < 11)
          return F2(t2, l4, n2, f7, o, d5, w9, U8, p10, B9);
        let O7 = y3[a++];
        if ((O7 & 128) > 0) {
          a -= 11;
          return;
        }
        return F2(t2, l4, n2, f7, o, d5, w9, U8, p10, B9, O7);
      } else {
        let p10 = y3[a++], B9 = y3[a++], O7 = y3[a++], N8 = y3[a++];
        if ((p10 & 128) > 0 || (B9 & 128) > 0 || (O7 & 128) > 0 || (N8 & 128) > 0) {
          a -= 12;
          return;
        }
        if (e3 < 14) {
          if (e3 === 12)
            return F2(t2, l4, n2, f7, o, d5, w9, U8, p10, B9, O7, N8);
          {
            let V7 = y3[a++];
            if ((V7 & 128) > 0) {
              a -= 13;
              return;
            }
            return F2(t2, l4, n2, f7, o, d5, w9, U8, p10, B9, O7, N8, V7);
          }
        } else {
          let V7 = y3[a++], K9 = y3[a++];
          if ((V7 & 128) > 0 || (K9 & 128) > 0) {
            a -= 14;
            return;
          }
          if (e3 < 15)
            return F2(t2, l4, n2, f7, o, d5, w9, U8, p10, B9, O7, N8, V7, K9);
          let q7 = y3[a++];
          if ((q7 & 128) > 0) {
            a -= 15;
            return;
          }
          return F2(t2, l4, n2, f7, o, d5, w9, U8, p10, B9, O7, N8, V7, K9, q7);
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
  let e3 = y3[a++], t2 = y3[a++], l4 = (e3 & 127) >> 2;
  if (l4 === 31)
    return t2 || e3 & 3 ? NaN : e3 & 128 ? -1 / 0 : 1 / 0;
  if (l4 === 0) {
    let n2 = ((e3 & 3) << 8 | t2) / 16777216;
    return e3 & 128 ? -n2 : n2;
  }
  return ie2[3] = e3 & 128 | (l4 >> 1) + 56, ie2[2] = (e3 & 7) << 5 | t2 >> 3, ie2[1] = t2 << 5, ie2[0] = 0, Fe2[0];
}
var Jt = new Array(4096);
var H4 = class {
  constructor(t2, l4) {
    this.value = t2, this.tag = l4;
  }
};
R2[0] = (e3) => new Date(e3);
R2[1] = (e3) => new Date(Math.round(e3 * 1e3));
R2[2] = (e3) => {
  let t2 = BigInt(0);
  for (let l4 = 0, n2 = e3.byteLength; l4 < n2; l4++)
    t2 = BigInt(e3[l4]) + t2 << BigInt(8);
  return t2;
};
R2[3] = (e3) => BigInt(-1) - R2[2](e3);
R2[4] = (e3) => +(e3[1] + "e" + e3[0]);
R2[5] = (e3) => e3[1] * Math.exp(e3[0] * Math.log(2));
var De2 = (e3, t2) => {
  e3 = e3 - 57344;
  let l4 = _3[e3];
  l4 && l4.isShared && ((_3.restoreStructures || (_3.restoreStructures = []))[e3] = l4), _3[e3] = t2, t2.read = Be2(t2);
};
R2[ut2] = (e3) => {
  let t2 = e3.length, l4 = e3[1];
  De2(e3[0], l4);
  let n2 = {};
  for (let f7 = 2; f7 < t2; f7++) {
    let o = l4[f7 - 2];
    n2[j4(o)] = e3[f7];
  }
  return n2;
};
R2[14] = (e3) => T2 ? T2[0].slice(T2.position0, T2.position0 += e3) : new H4(e3, 14);
R2[15] = (e3) => T2 ? T2[1].slice(T2.position1, T2.position1 += e3) : new H4(e3, 15);
var bt2 = { Error, RegExp };
R2[27] = (e3) => (bt2[e3[0]] || Error)(e3[1], e3[2]);
var rt3 = (e3) => {
  if (y3[a++] != 132)
    throw new Error("Packed values structure must be followed by a 4 element array");
  let t2 = e3();
  return P2 = P2 ? t2.concat(P2.slice(t2.length)) : t2, P2.prefixes = e3(), P2.suffixes = e3(), e3();
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
  let t2 = W2.id++, l4 = y3[a], n2;
  l4 >> 5 == 4 ? n2 = [] : n2 = {};
  let f7 = { target: n2 };
  W2.set(t2, f7);
  let o = e3();
  return f7.used ? Object.assign(n2, o) : (f7.target = o, o);
};
R2[28].handlesRead = true;
R2[29] = (e3) => {
  let t2 = W2.get(e3);
  return t2.used = true, t2.target;
};
R2[258] = (e3) => new Set(e3);
(R2[259] = (e3) => (E2.mapsAsObjects && (E2.mapsAsObjects = false, oe2 = true), e3())).handlesRead = true;
function ne2(e3, t2) {
  return typeof e3 == "string" ? e3 + t2 : e3 instanceof Array ? e3.concat(t2) : Object.assign({}, e3, t2);
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
Re2.push((e3, t2) => {
  if (e3 >= 225 && e3 <= 255)
    return ne2(Q2().prefixes[e3 - 224], t2);
  if (e3 >= 28704 && e3 <= 32767)
    return ne2(Q2().prefixes[e3 - 28672], t2);
  if (e3 >= 1879052288 && e3 <= 2147483647)
    return ne2(Q2().prefixes[e3 - 1879048192], t2);
  if (e3 >= 216 && e3 <= 223)
    return ne2(t2, Q2().suffixes[e3 - 216]);
  if (e3 >= 27647 && e3 <= 28671)
    return ne2(t2, Q2().suffixes[e3 - 27639]);
  if (e3 >= 1811940352 && e3 <= 1879048191)
    return ne2(t2, Q2().suffixes[e3 - 1811939328]);
  if (e3 == mt2)
    return { packedValues: P2, structures: _3.slice(0), version: t2 };
  if (e3 == 55799)
    return t2;
});
var At = new Uint8Array(new Uint16Array([1]).buffer)[0] == 1;
var Qe2 = [Uint8Array, Uint8ClampedArray, Uint16Array, Uint32Array, typeof BigUint64Array > "u" ? { name: "BigUint64Array" } : BigUint64Array, Int8Array, Int16Array, Int32Array, typeof BigInt64Array > "u" ? { name: "BigInt64Array" } : BigInt64Array, Float32Array, Float64Array];
var St2 = [64, 68, 69, 70, 71, 72, 77, 78, 79, 85, 86];
for (let e3 = 0; e3 < Qe2.length; e3++)
  Et(Qe2[e3], St2[e3]);
function Et(e3, t2) {
  let l4 = "get" + e3.name.slice(0, -5), n2;
  typeof e3 == "function" ? n2 = e3.BYTES_PER_ELEMENT : e3 = null;
  for (let f7 = 0; f7 < 2; f7++) {
    if (!f7 && n2 == 1)
      continue;
    let o = n2 == 2 ? 1 : n2 == 4 ? 2 : 3;
    R2[f7 ? t2 : t2 - 4] = n2 == 1 || f7 == At ? (d5) => {
      if (!e3)
        throw new Error("Could not find typed array for code " + t2);
      return new e3(Uint8Array.prototype.slice.call(d5, 0).buffer);
    } : (d5) => {
      if (!e3)
        throw new Error("Could not find typed array for code " + t2);
      let w9 = new DataView(d5.buffer, d5.byteOffset, d5.byteLength), U8 = d5.length >> o, p10 = new e3(U8), B9 = w9[l4];
      for (let O7 = 0; O7 < U8; O7++)
        p10[O7] = B9.call(w9, O7 << o, f7);
      return p10;
    };
  }
}
function It() {
  let e3 = se2(), t2 = a + S2();
  for (let n2 = 2; n2 < e3; n2++) {
    let f7 = se2();
    a += f7;
  }
  let l4 = a;
  return a = t2, T2 = [Te2(se2()), Te2(se2())], T2.position0 = 0, T2.position1 = 0, T2.postBundlePosition = a, a = l4, S2();
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
    let e3 = nt3(() => (y3 = null, E2.getShared())) || {}, t2 = e3.structures || [];
    E2.sharedVersion = e3.version, P2 = E2.sharedValues = e3.packedValues, _3 === true ? E2.structures = _3 = t2 : _3.splice.apply(_3, [0, t2.length].concat(t2));
  }
}
function nt3(e3) {
  let t2 = v4, l4 = a, n2 = _e2, f7 = pe, o = ae2, d5 = ye2, w9 = Me2, U8 = W2, p10 = T2, B9 = new Uint8Array(y3.slice(0, v4)), O7 = _3, N8 = E2, V7 = ce2, K9 = e3();
  return v4 = t2, a = l4, _e2 = n2, pe = f7, ae2 = o, ye2 = d5, Me2 = w9, W2 = U8, T2 = p10, y3 = B9, ce2 = V7, _3 = O7, E2 = N8, C5 = new DataView(y3.buffer, y3.byteOffset, y3.byteLength), K9;
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
  constructor(t2) {
    super(t2), this.offset = 0;
    let l4, n2, f7, o, d5, w9;
    t2 = t2 || {};
    let U8 = st2.prototype.utf8Write ? function(s2, h3, c7) {
      return i2.utf8Write(s2, h3, c7);
    } : be2 && be2.encodeInto ? function(s2, h3) {
      return be2.encodeInto(s2, i2.subarray(h3)).written;
    } : false, p10 = this, B9 = t2.structures || t2.saveStructures, O7 = t2.maxSharedStructures;
    if (O7 == null && (O7 = B9 ? 128 : 0), O7 > 8190)
      throw new Error("Maximum maxSharedStructure is 8190");
    let N8 = t2.sequential;
    N8 && (O7 = 0), this.structures || (this.structures = []), this.saveStructures && (this.saveShared = this.saveStructures);
    let V7, K9, q7 = t2.sharedValues, z8;
    if (q7) {
      z8 = /* @__PURE__ */ Object.create(null);
      for (let s2 = 0, h3 = q7.length; s2 < h3; s2++)
        z8[q7[s2]] = s2;
    }
    let Z7 = [], Ie2 = 0, xe2 = 0;
    this.mapEncode = function(s2, h3) {
      if (this._keyMap && !this._mapped)
        switch (s2.constructor.name) {
          case "Array":
            s2 = s2.map((c7) => this.encodeKeys(c7));
            break;
        }
      return this.encode(s2, h3);
    }, this.encode = function(s2, h3) {
      if (i2 || (i2 = new Pe2(8192), M3 = new DataView(i2.buffer, 0, 8192), r = 0), X2 = i2.length - 10, X2 - r < 2048 ? (i2 = new Pe2(i2.length), M3 = new DataView(i2.buffer, 0, i2.length), X2 = i2.length - 10, r = 0) : h3 === Ke2 && (r = r + 7 & 2147483640), n2 = r, p10.useSelfDescribedHeader && (M3.setUint32(r, 3654940416), r += 3), w9 = p10.structuredClone ? /* @__PURE__ */ new Map() : null, p10.bundleStrings && typeof s2 != "string" ? (D3 = [], D3.size = 1 / 0) : D3 = null, f7 = p10.structures, f7) {
        if (f7.uninitialized) {
          let x8 = p10.getShared() || {};
          p10.structures = f7 = x8.structures || [], p10.sharedVersion = x8.version;
          let u6 = p10.sharedValues = x8.packedValues;
          if (u6) {
            z8 = {};
            for (let g8 = 0, b6 = u6.length; g8 < b6; g8++)
              z8[u6[g8]] = g8;
          }
        }
        let c7 = f7.length;
        if (c7 > O7 && !N8 && (c7 = O7), !f7.transitions) {
          f7.transitions = /* @__PURE__ */ Object.create(null);
          for (let x8 = 0; x8 < c7; x8++) {
            let u6 = f7[x8];
            if (!u6)
              continue;
            let g8, b6 = f7.transitions;
            for (let m9 = 0, A5 = u6.length; m9 < A5; m9++) {
              b6[L2] === void 0 && (b6[L2] = x8);
              let I7 = u6[m9];
              g8 = b6[I7], g8 || (g8 = b6[I7] = /* @__PURE__ */ Object.create(null)), b6 = g8;
            }
            b6[L2] = x8 | 1048576;
          }
        }
        N8 || (f7.nextId = c7);
      }
      if (o && (o = false), d5 = f7 || [], K9 = z8, t2.pack) {
        let c7 = /* @__PURE__ */ new Map();
        if (c7.values = [], c7.encoder = p10, c7.maxValues = t2.maxPrivatePackedValues || (z8 ? 16 : 1 / 0), c7.objectMap = z8 || false, c7.samplingPackedValues = V7, me2(s2, c7), c7.values.length > 0) {
          i2[r++] = 216, i2[r++] = 51, G4(4);
          let x8 = c7.values;
          k10(x8), G4(0), G4(0), K9 = Object.create(z8 || null);
          for (let u6 = 0, g8 = x8.length; u6 < g8; u6++)
            K9[x8[u6]] = u6;
        }
      }
      Le2 = h3 & je2;
      try {
        if (Le2)
          return;
        if (k10(s2), D3 && ct2(n2, k10), p10.offset = r, w9 && w9.idsToInsert) {
          r += w9.idsToInsert.length * 2, r > X2 && le2(r), p10.offset = r;
          let c7 = Tt2(i2.subarray(n2, r), w9.idsToInsert);
          return w9 = null, c7;
        }
        return h3 & Ke2 ? (i2.start = n2, i2.end = r, i2) : i2.subarray(n2, r);
      } finally {
        if (f7) {
          if (xe2 < 10 && xe2++, f7.length > O7 && (f7.length = O7), Ie2 > 1e4)
            f7.transitions = null, xe2 = 0, Ie2 = 0, Z7.length > 0 && (Z7 = []);
          else if (Z7.length > 0 && !N8) {
            for (let c7 = 0, x8 = Z7.length; c7 < x8; c7++)
              Z7[c7][L2] = void 0;
            Z7 = [];
          }
        }
        if (o && p10.saveShared) {
          p10.structures.length > O7 && (p10.structures = p10.structures.slice(0, O7));
          let c7 = i2.subarray(n2, r);
          return p10.updateSharedData() === false ? p10.encode(s2) : c7;
        }
        h3 & Kt && (r = n2);
      }
    }, this.findCommonStringsToPack = () => (V7 = /* @__PURE__ */ new Map(), z8 || (z8 = /* @__PURE__ */ Object.create(null)), (s2) => {
      let h3 = s2 && s2.threshold || 4, c7 = this.pack ? s2.maxPrivatePackedValues || 16 : 0;
      q7 || (q7 = this.sharedValues = []);
      for (let [x8, u6] of V7)
        u6.count > h3 && (z8[x8] = c7++, q7.push(x8), o = true);
      for (; this.saveShared && this.updateSharedData() === false; )
        ;
      V7 = null;
    });
    let k10 = (s2) => {
      r > X2 && (i2 = le2(r));
      var h3 = typeof s2, c7;
      if (h3 === "string") {
        if (K9) {
          let b6 = K9[s2];
          if (b6 >= 0) {
            b6 < 16 ? i2[r++] = b6 + 224 : (i2[r++] = 198, b6 & 1 ? k10(15 - b6 >> 1) : k10(b6 - 16 >> 1));
            return;
          } else if (V7 && !t2.pack) {
            let m9 = V7.get(s2);
            m9 ? m9.count++ : V7.set(s2, { count: 1 });
          }
        }
        let x8 = s2.length;
        if (D3 && x8 >= 4 && x8 < 1024) {
          if ((D3.size += x8) > Mt2) {
            let m9, A5 = (D3[0] ? D3[0].length * 3 + D3[1].length : 0) + 10;
            r + A5 > X2 && (i2 = le2(r + A5)), i2[r++] = 217, i2[r++] = 223, i2[r++] = 249, i2[r++] = D3.position ? 132 : 130, i2[r++] = 26, m9 = r - n2, r += 4, D3.position && ct2(n2, k10), D3 = ["", ""], D3.size = 0, D3.position = m9;
          }
          let b6 = _t2.test(s2);
          D3[b6 ? 0 : 1] += s2, i2[r++] = b6 ? 206 : 207, k10(x8);
          return;
        }
        let u6;
        x8 < 32 ? u6 = 1 : x8 < 256 ? u6 = 2 : x8 < 65536 ? u6 = 3 : u6 = 5;
        let g8 = x8 * 3;
        if (r + g8 > X2 && (i2 = le2(r + g8)), x8 < 64 || !U8) {
          let b6, m9, A5, I7 = r + u6;
          for (b6 = 0; b6 < x8; b6++)
            m9 = s2.charCodeAt(b6), m9 < 128 ? i2[I7++] = m9 : m9 < 2048 ? (i2[I7++] = m9 >> 6 | 192, i2[I7++] = m9 & 63 | 128) : (m9 & 64512) === 55296 && ((A5 = s2.charCodeAt(b6 + 1)) & 64512) === 56320 ? (m9 = 65536 + ((m9 & 1023) << 10) + (A5 & 1023), b6++, i2[I7++] = m9 >> 18 | 240, i2[I7++] = m9 >> 12 & 63 | 128, i2[I7++] = m9 >> 6 & 63 | 128, i2[I7++] = m9 & 63 | 128) : (i2[I7++] = m9 >> 12 | 224, i2[I7++] = m9 >> 6 & 63 | 128, i2[I7++] = m9 & 63 | 128);
          c7 = I7 - r - u6;
        } else
          c7 = U8(s2, r + u6, g8);
        c7 < 24 ? i2[r++] = 96 | c7 : c7 < 256 ? (u6 < 2 && i2.copyWithin(r + 2, r + 1, r + 1 + c7), i2[r++] = 120, i2[r++] = c7) : c7 < 65536 ? (u6 < 3 && i2.copyWithin(r + 3, r + 2, r + 2 + c7), i2[r++] = 121, i2[r++] = c7 >> 8, i2[r++] = c7 & 255) : (u6 < 5 && i2.copyWithin(r + 5, r + 3, r + 3 + c7), i2[r++] = 122, M3.setUint32(r, c7), r += 4), r += c7;
      } else if (h3 === "number")
        if (!this.alwaysUseFloat && s2 >>> 0 === s2)
          s2 < 24 ? i2[r++] = s2 : s2 < 256 ? (i2[r++] = 24, i2[r++] = s2) : s2 < 65536 ? (i2[r++] = 25, i2[r++] = s2 >> 8, i2[r++] = s2 & 255) : (i2[r++] = 26, M3.setUint32(r, s2), r += 4);
        else if (!this.alwaysUseFloat && s2 >> 0 === s2)
          s2 >= -24 ? i2[r++] = 31 - s2 : s2 >= -256 ? (i2[r++] = 56, i2[r++] = ~s2) : s2 >= -65536 ? (i2[r++] = 57, M3.setUint16(r, ~s2), r += 2) : (i2[r++] = 58, M3.setUint32(r, ~s2), r += 4);
        else {
          let x8;
          if ((x8 = this.useFloat32) > 0 && s2 < 4294967296 && s2 >= -2147483648) {
            i2[r++] = 250, M3.setFloat32(r, s2);
            let u6;
            if (x8 < 4 || (u6 = s2 * ue2[(i2[r] & 127) << 1 | i2[r + 1] >> 7]) >> 0 === u6) {
              r += 4;
              return;
            } else
              r--;
          }
          i2[r++] = 251, M3.setFloat64(r, s2), r += 8;
        }
      else if (h3 === "object")
        if (!s2)
          i2[r++] = 246;
        else {
          if (w9) {
            let u6 = w9.get(s2);
            if (u6) {
              if (i2[r++] = 216, i2[r++] = 29, i2[r++] = 25, !u6.references) {
                let g8 = w9.idsToInsert || (w9.idsToInsert = []);
                u6.references = [], g8.push(u6);
              }
              u6.references.push(r - n2), r += 2;
              return;
            } else
              w9.set(s2, { offset: r - n2 });
          }
          let x8 = s2.constructor;
          if (x8 === Object)
            ke2(s2, true);
          else if (x8 === Array) {
            c7 = s2.length, c7 < 24 ? i2[r++] = 128 | c7 : G4(c7);
            for (let u6 = 0; u6 < c7; u6++)
              k10(s2[u6]);
          } else if (x8 === Map)
            if ((this.mapsAsObjects ? this.useTag259ForMaps !== false : this.useTag259ForMaps) && (i2[r++] = 217, i2[r++] = 1, i2[r++] = 3), c7 = s2.size, c7 < 24 ? i2[r++] = 160 | c7 : c7 < 256 ? (i2[r++] = 184, i2[r++] = c7) : c7 < 65536 ? (i2[r++] = 185, i2[r++] = c7 >> 8, i2[r++] = c7 & 255) : (i2[r++] = 186, M3.setUint32(r, c7), r += 4), p10.keyMap)
              for (let [u6, g8] of s2)
                k10(p10.encodeKey(u6)), k10(g8);
            else
              for (let [u6, g8] of s2)
                k10(u6), k10(g8);
          else {
            for (let u6 = 0, g8 = Ae2.length; u6 < g8; u6++) {
              let b6 = ze2[u6];
              if (s2 instanceof b6) {
                let m9 = Ae2[u6], A5 = m9.tag;
                A5 == null && (A5 = m9.getTag && m9.getTag.call(this, s2)), A5 < 24 ? i2[r++] = 192 | A5 : A5 < 256 ? (i2[r++] = 216, i2[r++] = A5) : A5 < 65536 ? (i2[r++] = 217, i2[r++] = A5 >> 8, i2[r++] = A5 & 255) : A5 > -1 && (i2[r++] = 218, M3.setUint32(r, A5), r += 4), m9.encode.call(this, s2, k10, le2);
                return;
              }
            }
            if (s2[Symbol.iterator]) {
              if (Le2) {
                let u6 = new Error("Iterable should be serialized as iterator");
                throw u6.iteratorNotHandled = true, u6;
              }
              i2[r++] = 159;
              for (let u6 of s2)
                k10(u6);
              i2[r++] = 255;
              return;
            }
            if (s2[Symbol.asyncIterator] || Ne2(s2)) {
              let u6 = new Error("Iterable/blob should be serialized as iterator");
              throw u6.iteratorNotHandled = true, u6;
            }
            ke2(s2, !s2.hasOwnProperty);
          }
        }
      else if (h3 === "boolean")
        i2[r++] = s2 ? 245 : 244;
      else if (h3 === "bigint") {
        if (s2 < BigInt(1) << BigInt(64) && s2 >= 0)
          i2[r++] = 27, M3.setBigUint64(r, s2);
        else if (s2 > -(BigInt(1) << BigInt(64)) && s2 < 0)
          i2[r++] = 59, M3.setBigUint64(r, -s2 - BigInt(1));
        else if (this.largeBigIntToFloat)
          i2[r++] = 251, M3.setFloat64(r, Number(s2));
        else
          throw new RangeError(s2 + " was too large to fit in CBOR 64-bit integer format, set largeBigIntToFloat to convert to float-64");
        r += 8;
      } else if (h3 === "undefined")
        i2[r++] = 247;
      else
        throw new Error("Unknown type: " + h3);
    }, ke2 = this.useRecords === false ? this.variableMapSize ? (s2) => {
      let h3 = Object.keys(s2), c7 = Object.values(s2), x8 = h3.length;
      x8 < 24 ? i2[r++] = 160 | x8 : x8 < 256 ? (i2[r++] = 184, i2[r++] = x8) : x8 < 65536 ? (i2[r++] = 185, i2[r++] = x8 >> 8, i2[r++] = x8 & 255) : (i2[r++] = 186, M3.setUint32(r, x8), r += 4);
      let u6;
      if (p10.keyMap)
        for (let g8 = 0; g8 < x8; g8++)
          k10(encodeKey(h3[g8])), k10(c7[g8]);
      else
        for (let g8 = 0; g8 < x8; g8++)
          k10(h3[g8]), k10(c7[g8]);
    } : (s2, h3) => {
      i2[r++] = 185;
      let c7 = r - n2;
      r += 2;
      let x8 = 0;
      if (p10.keyMap)
        for (let u6 in s2)
          (h3 || s2.hasOwnProperty(u6)) && (k10(p10.encodeKey(u6)), k10(s2[u6]), x8++);
      else
        for (let u6 in s2)
          (h3 || s2.hasOwnProperty(u6)) && (k10(u6), k10(s2[u6]), x8++);
      i2[c7++ + n2] = x8 >> 8, i2[c7 + n2] = x8 & 255;
    } : (s2, h3) => {
      let c7, x8 = d5.transitions || (d5.transitions = /* @__PURE__ */ Object.create(null)), u6 = 0, g8 = 0, b6, m9;
      if (this.keyMap) {
        m9 = Object.keys(s2).map((I7) => this.encodeKey(I7)), g8 = m9.length;
        for (let I7 = 0; I7 < g8; I7++) {
          let Ge2 = m9[I7];
          c7 = x8[Ge2], c7 || (c7 = x8[Ge2] = /* @__PURE__ */ Object.create(null), u6++), x8 = c7;
        }
      } else
        for (let I7 in s2)
          (h3 || s2.hasOwnProperty(I7)) && (c7 = x8[I7], c7 || (x8[L2] & 1048576 && (b6 = x8[L2] & 65535), c7 = x8[I7] = /* @__PURE__ */ Object.create(null), u6++), x8 = c7, g8++);
      let A5 = x8[L2];
      if (A5 !== void 0)
        A5 &= 65535, i2[r++] = 217, i2[r++] = A5 >> 8 | 224, i2[r++] = A5 & 255;
      else if (m9 || (m9 = x8.__keys__ || (x8.__keys__ = Object.keys(s2))), b6 === void 0 ? (A5 = d5.nextId++, A5 || (A5 = 0, d5.nextId = 1), A5 >= lt2 && (d5.nextId = (A5 = O7) + 1)) : A5 = b6, d5[A5] = m9, A5 < O7) {
        i2[r++] = 217, i2[r++] = A5 >> 8 | 224, i2[r++] = A5 & 255, x8 = d5.transitions;
        for (let I7 = 0; I7 < g8; I7++)
          (x8[L2] === void 0 || x8[L2] & 1048576) && (x8[L2] = A5), x8 = x8[m9[I7]];
        x8[L2] = A5 | 1048576, o = true;
      } else {
        if (x8[L2] = A5, M3.setUint32(r, 3655335680), r += 3, u6 && (Ie2 += xe2 * u6), Z7.length >= lt2 - O7 && (Z7.shift()[L2] = void 0), Z7.push(x8), G4(g8 + 2), k10(57344 + A5), k10(m9), h3 === null)
          return;
        for (let I7 in s2)
          (h3 || s2.hasOwnProperty(I7)) && k10(s2[I7]);
        return;
      }
      if (g8 < 24 ? i2[r++] = 128 | g8 : G4(g8), h3 !== null)
        for (let I7 in s2)
          (h3 || s2.hasOwnProperty(I7)) && k10(s2[I7]);
    }, le2 = (s2) => {
      let h3;
      if (s2 > 16777216) {
        if (s2 - n2 > ft2)
          throw new Error("Encoded buffer would be larger than maximum buffer size");
        h3 = Math.min(ft2, Math.round(Math.max((s2 - n2) * (s2 > 67108864 ? 1.25 : 2), 4194304) / 4096) * 4096);
      } else
        h3 = (Math.max(s2 - n2 << 2, i2.length - 1) >> 12) + 1 << 12;
      let c7 = new Pe2(h3);
      return M3 = new DataView(c7.buffer, 0, h3), i2.copy ? i2.copy(c7, 0, n2, s2) : c7.set(i2.slice(n2, s2)), r -= n2, n2 = 0, X2 = c7.length - 10, i2 = c7;
    }, $4 = 100, Ye2 = 1e3;
    this.encodeAsIterable = function(s2, h3) {
      return He2(s2, h3, te2);
    }, this.encodeAsAsyncIterable = function(s2, h3) {
      return He2(s2, h3, Je2);
    };
    function* te2(s2, h3, c7) {
      let x8 = s2.constructor;
      if (x8 === Object) {
        let u6 = p10.useRecords !== false;
        u6 ? ke2(s2, null) : ot3(Object.keys(s2).length, 160);
        for (let g8 in s2) {
          let b6 = s2[g8];
          u6 || k10(g8), b6 && typeof b6 == "object" ? h3[g8] ? yield* te2(b6, h3[g8]) : yield* Oe2(b6, h3, g8) : k10(b6);
        }
      } else if (x8 === Array) {
        let u6 = s2.length;
        G4(u6);
        for (let g8 = 0; g8 < u6; g8++) {
          let b6 = s2[g8];
          b6 && (typeof b6 == "object" || r - n2 > $4) ? h3.element ? yield* te2(b6, h3.element) : yield* Oe2(b6, h3, "element") : k10(b6);
        }
      } else if (s2[Symbol.iterator]) {
        i2[r++] = 159;
        for (let u6 of s2)
          u6 && (typeof u6 == "object" || r - n2 > $4) ? h3.element ? yield* te2(u6, h3.element) : yield* Oe2(u6, h3, "element") : k10(u6);
        i2[r++] = 255;
      } else
        Ne2(s2) ? (ot3(s2.size, 64), yield i2.subarray(n2, r), yield s2, fe2()) : s2[Symbol.asyncIterator] ? (i2[r++] = 159, yield i2.subarray(n2, r), yield s2, fe2(), i2[r++] = 255) : k10(s2);
      c7 && r > n2 ? yield i2.subarray(n2, r) : r - n2 > $4 && (yield i2.subarray(n2, r), fe2());
    }
    function* Oe2(s2, h3, c7) {
      let x8 = r - n2;
      try {
        k10(s2), r - n2 > $4 && (yield i2.subarray(n2, r), fe2());
      } catch (u6) {
        if (u6.iteratorNotHandled)
          h3[c7] = {}, r = n2 + x8, yield* te2.call(this, s2, h3[c7]);
        else
          throw u6;
      }
    }
    function fe2() {
      $4 = Ye2, p10.encode(null, je2);
    }
    function He2(s2, h3, c7) {
      return h3 && h3.chunkThreshold ? $4 = Ye2 = h3.chunkThreshold : $4 = 100, s2 && typeof s2 == "object" ? (p10.encode(null, je2), c7(s2, p10.iterateProperties || (p10.iterateProperties = {}), true)) : [p10.encode(s2)];
    }
    async function* Je2(s2, h3) {
      for (let c7 of te2(s2, h3, true)) {
        let x8 = c7.constructor;
        if (x8 === st2 || x8 === Uint8Array)
          yield c7;
        else if (Ne2(c7)) {
          let u6 = c7.stream().getReader(), g8;
          for (; !(g8 = await u6.read()).done; )
            yield g8.value;
        } else if (c7[Symbol.asyncIterator])
          for await (let u6 of c7)
            fe2(), u6 ? yield* Je2(u6, h3.async || (h3.async = {})) : yield p10.encode(u6);
        else
          yield c7;
      }
    }
  }
  useBuffer(t2) {
    i2 = t2, M3 = new DataView(i2.buffer, i2.byteOffset, i2.byteLength), r = 0;
  }
  clearSharedData() {
    this.structures && (this.structures = []), this.sharedValues && (this.sharedValues = void 0);
  }
  updateSharedData() {
    let t2 = this.sharedVersion || 0;
    this.sharedVersion = t2 + 1;
    let l4 = this.structures.slice(0), n2 = new Se2(l4, this.sharedValues, this.sharedVersion), f7 = this.saveShared(n2, (o) => (o && o.version || 0) == t2);
    return f7 === false ? (n2 = this.getShared() || {}, this.structures = n2.structures || [], this.sharedValues = n2.packedValues, this.sharedVersion = n2.version, this.structures.nextId = this.structures.length) : l4.forEach((o, d5) => this.structures[d5] = o), f7;
  }
};
function ot3(e3, t2) {
  e3 < 24 ? i2[r++] = t2 | e3 : e3 < 256 ? (i2[r++] = t2 | 24, i2[r++] = e3) : e3 < 65536 ? (i2[r++] = t2 | 25, i2[r++] = e3 >> 8, i2[r++] = e3 & 255) : (i2[r++] = t2 | 26, M3.setUint32(r, e3), r += 4);
}
var Se2 = class {
  constructor(t2, l4, n2) {
    this.structures = t2, this.packedValues = l4, this.version = n2;
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
  let t2 = e3[Symbol.toStringTag];
  return t2 === "Blob" || t2 === "File";
}
function me2(e3, t2) {
  switch (typeof e3) {
    case "string":
      if (e3.length > 3) {
        if (t2.objectMap[e3] > -1 || t2.values.length >= t2.maxValues)
          return;
        let n2 = t2.get(e3);
        if (n2)
          ++n2.count == 2 && t2.values.push(e3);
        else if (t2.set(e3, { count: 1 }), t2.samplingPackedValues) {
          let f7 = t2.samplingPackedValues.get(e3);
          f7 ? f7.count++ : t2.samplingPackedValues.set(e3, { count: 1 });
        }
      }
      break;
    case "object":
      if (e3)
        if (e3 instanceof Array)
          for (let n2 = 0, f7 = e3.length; n2 < f7; n2++)
            me2(e3[n2], t2);
        else {
          let n2 = !t2.encoder.useRecords;
          for (var l4 in e3)
            e3.hasOwnProperty(l4) && (n2 && me2(l4, t2), me2(e3[l4], t2));
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
Ae2 = [{ tag: 1, encode(e3, t2) {
  let l4 = e3.getTime() / 1e3;
  (this.useTimestamp32 || e3.getMilliseconds() === 0) && l4 >= 0 && l4 < 4294967296 ? (i2[r++] = 26, M3.setUint32(r, l4), r += 4) : (i2[r++] = 251, M3.setFloat64(r, l4), r += 8);
} }, { tag: 258, encode(e3, t2) {
  let l4 = Array.from(e3);
  t2(l4);
} }, { tag: 27, encode(e3, t2) {
  t2([e3.name, e3.message]);
} }, { tag: 27, encode(e3, t2) {
  t2(["RegExp", e3.source, e3.flags]);
} }, { getTag(e3) {
  return e3.tag;
}, encode(e3, t2) {
  t2(e3.value);
} }, { encode(e3, t2, l4) {
  at2(e3, l4);
} }, { getTag(e3) {
  if (e3.constructor === Uint8Array && (this.tagUint8Array || de2 && this.tagUint8Array !== false))
    return 64;
}, encode(e3, t2, l4) {
  at2(e3, l4);
} }, J4(68, 1), J4(69, 2), J4(70, 4), J4(71, 8), J4(72, 1), J4(77, 2), J4(78, 4), J4(79, 8), J4(85, 4), J4(86, 8), { encode(e3, t2) {
  let l4 = e3.packedValues || [], n2 = e3.structures || [];
  if (l4.values.length > 0) {
    i2[r++] = 216, i2[r++] = 51, G4(4);
    let f7 = l4.values;
    t2(f7), G4(0), G4(0), packedObjectMap = Object.create(sharedPackedObjectMap || null);
    for (let o = 0, d5 = f7.length; o < d5; o++)
      packedObjectMap[f7[o]] = o;
  }
  if (n2) {
    M3.setUint32(r, 3655335424), r += 3;
    let f7 = n2.slice(0);
    f7.unshift(57344), f7.push(new H4(e3.version, 1399353956)), t2(f7);
  } else
    t2(new H4(e3.version, 1399353956));
} }];
function J4(e3, t2) {
  return !Bt && t2 > 1 && (e3 -= 4), { tag: e3, encode: function(n2, f7) {
    let o = n2.byteLength, d5 = n2.byteOffset || 0, w9 = n2.buffer || n2;
    f7(de2 ? Ee2.from(w9, d5, o) : new Uint8Array(w9, d5, o));
  } };
}
function at2(e3, t2) {
  let l4 = e3.byteLength;
  l4 < 24 ? i2[r++] = 64 + l4 : l4 < 256 ? (i2[r++] = 88, i2[r++] = l4) : l4 < 65536 ? (i2[r++] = 89, i2[r++] = l4 >> 8, i2[r++] = l4 & 255) : (i2[r++] = 90, M3.setUint32(r, l4), r += 4), r + l4 >= i2.length && t2(r + l4), i2.set(e3.buffer ? e3 : new Uint8Array(e3), r), r += l4;
}
function Tt2(e3, t2) {
  let l4, n2 = t2.length * 2, f7 = e3.length - n2;
  t2.sort((o, d5) => o.offset > d5.offset ? 1 : -1);
  for (let o = 0; o < t2.length; o++) {
    let d5 = t2[o];
    d5.id = o;
    for (let w9 of d5.references)
      e3[w9++] = o >> 8, e3[w9] = o & 255;
  }
  for (; l4 = t2.pop(); ) {
    let o = l4.offset;
    e3.copyWithin(o + n2, o, f7), n2 -= 2;
    let d5 = o + n2;
    e3[d5++] = 216, e3[d5++] = 28, f7 = o;
  }
  return e3;
}
function ct2(e3, t2) {
  M3.setUint32(D3.position + e3, r - D3.position - e3 + 1);
  let l4 = D3;
  D3 = null, t2(l4[0]), t2(l4[1]);
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
  constructor() {
    this._inited = false;
    this._ready = false;
  }
  // Using the Web Animations API
  onUpdate(cb) {
    this._onUpdate = cb;
    return this;
  }
  // <T>
  emitUpdate() {
    this._onUpdate?.(this);
  }
  onInit(cb) {
    if (this._inited) {
      cb(this);
    }
    this._onInit = cb;
    return this;
  }
  emitInit() {
    if (this._inited) {
      return;
    }
    this._inited = true;
    this._onInit?.(this);
  }
  onReady(cb) {
    if (this._ready) {
      cb(this);
    }
    this._onReady = cb;
    return this;
  }
  emitReady() {
    if (this._ready) {
      return;
    }
    this._ready = true;
    this._onReady?.(this);
  }
};

// src/emulator/controller/biometrics.controller.ts
var BiometricsController = class extends BaseController {
  constructor() {
    super(...arguments);
    this._init = (async () => {
      this.emitInit();
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
      this.emitReady();
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
      this.queue = this.queue.filter((t2) => t2 !== task);
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
      this.emitInit();
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
        const { pathname, searchParams } = event;
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
          this.observer.startObserve(ipc);
          return Response.json(true);
        }
        if (pathname.endsWith("/stopObserve")) {
          this.observer.startObserve(ipc);
          return Response.json("");
        }
      }).forbidden().cors();
      this.emitReady();
    })();
    this.observer = new StateObservable(() => {
      return JSON.stringify(this.statusBarGetState());
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
  emitUpdate() {
    this.observer.notifyObserver();
    super.emitUpdate();
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

// https://esm.sh/v124/@lit/reactive-element@1.6.1/deno/reactive-element.mjs
var l2 = window;
var c3 = l2.ShadowRoot && (l2.ShadyCSS === void 0 || l2.ShadyCSS.nativeShadow) && "adoptedStyleSheets" in Document.prototype && "replace" in CSSStyleSheet.prototype;
var u3 = Symbol();
var E3 = /* @__PURE__ */ new WeakMap();
var h2 = class {
  constructor(t2, e3, s2) {
    if (this._$cssResult$ = true, s2 !== u3)
      throw Error("CSSResult is not constructable. Use `unsafeCSS` or `css` instead.");
    this.cssText = t2, this.t = e3;
  }
  get styleSheet() {
    let t2 = this.o, e3 = this.t;
    if (c3 && t2 === void 0) {
      let s2 = e3 !== void 0 && e3.length === 1;
      s2 && (t2 = E3.get(e3)), t2 === void 0 && ((this.o = t2 = new CSSStyleSheet()).replaceSync(this.cssText), s2 && E3.set(e3, t2));
    }
    return t2;
  }
  toString() {
    return this.cssText;
  }
};
var _4 = (r3) => new h2(typeof r3 == "string" ? r3 : r3 + "", void 0, u3);
var C6 = (r3, ...t2) => {
  let e3 = r3.length === 1 ? r3[0] : t2.reduce((s2, i3, o) => s2 + ((n2) => {
    if (n2._$cssResult$ === true)
      return n2.cssText;
    if (typeof n2 == "number")
      return n2;
    throw Error("Value passed to 'css' function must be a 'css' function result: " + n2 + ". Use 'unsafeCSS' to pass non-literal values, but take care to ensure page security.");
  })(i3) + r3[o + 1], r3[0]);
  return new h2(e3, r3, u3);
};
var v5 = (r3, t2) => {
  c3 ? r3.adoptedStyleSheets = t2.map((e3) => e3 instanceof CSSStyleSheet ? e3 : e3.styleSheet) : t2.forEach((e3) => {
    let s2 = document.createElement("style"), i3 = l2.litNonce;
    i3 !== void 0 && s2.setAttribute("nonce", i3), s2.textContent = e3.cssText, r3.appendChild(s2);
  });
};
var d2 = c3 ? (r3) => r3 : (r3) => r3 instanceof CSSStyleSheet ? ((t2) => {
  let e3 = "";
  for (let s2 of t2.cssRules)
    e3 += s2.cssText;
  return _4(e3);
})(r3) : r3;
var S3;
var p3 = window;
var $2 = p3.trustedTypes;
var w3 = $2 ? $2.emptyScript : "";
var m3 = p3.reactiveElementPolyfillSupport;
var y4 = { toAttribute(r3, t2) {
  switch (t2) {
    case Boolean:
      r3 = r3 ? w3 : null;
      break;
    case Object:
    case Array:
      r3 = r3 == null ? r3 : JSON.stringify(r3);
  }
  return r3;
}, fromAttribute(r3, t2) {
  let e3 = r3;
  switch (t2) {
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
var b2 = (r3, t2) => t2 !== r3 && (t2 == t2 || r3 == r3);
var f = { attribute: true, type: String, converter: y4, reflect: false, hasChanged: b2 };
var a2 = class extends HTMLElement {
  constructor() {
    super(), this._$Ei = /* @__PURE__ */ new Map(), this.isUpdatePending = false, this.hasUpdated = false, this._$El = null, this.u();
  }
  static addInitializer(t2) {
    var e3;
    this.finalize(), ((e3 = this.h) !== null && e3 !== void 0 ? e3 : this.h = []).push(t2);
  }
  static get observedAttributes() {
    this.finalize();
    let t2 = [];
    return this.elementProperties.forEach((e3, s2) => {
      let i3 = this._$Ep(s2, e3);
      i3 !== void 0 && (this._$Ev.set(i3, s2), t2.push(i3));
    }), t2;
  }
  static createProperty(t2, e3 = f) {
    if (e3.state && (e3.attribute = false), this.finalize(), this.elementProperties.set(t2, e3), !e3.noAccessor && !this.prototype.hasOwnProperty(t2)) {
      let s2 = typeof t2 == "symbol" ? Symbol() : "__" + t2, i3 = this.getPropertyDescriptor(t2, s2, e3);
      i3 !== void 0 && Object.defineProperty(this.prototype, t2, i3);
    }
  }
  static getPropertyDescriptor(t2, e3, s2) {
    return { get() {
      return this[e3];
    }, set(i3) {
      let o = this[t2];
      this[e3] = i3, this.requestUpdate(t2, o, s2);
    }, configurable: true, enumerable: true };
  }
  static getPropertyOptions(t2) {
    return this.elementProperties.get(t2) || f;
  }
  static finalize() {
    if (this.hasOwnProperty("finalized"))
      return false;
    this.finalized = true;
    let t2 = Object.getPrototypeOf(this);
    if (t2.finalize(), t2.h !== void 0 && (this.h = [...t2.h]), this.elementProperties = new Map(t2.elementProperties), this._$Ev = /* @__PURE__ */ new Map(), this.hasOwnProperty("properties")) {
      let e3 = this.properties, s2 = [...Object.getOwnPropertyNames(e3), ...Object.getOwnPropertySymbols(e3)];
      for (let i3 of s2)
        this.createProperty(i3, e3[i3]);
    }
    return this.elementStyles = this.finalizeStyles(this.styles), true;
  }
  static finalizeStyles(t2) {
    let e3 = [];
    if (Array.isArray(t2)) {
      let s2 = new Set(t2.flat(1 / 0).reverse());
      for (let i3 of s2)
        e3.unshift(d2(i3));
    } else
      t2 !== void 0 && e3.push(d2(t2));
    return e3;
  }
  static _$Ep(t2, e3) {
    let s2 = e3.attribute;
    return s2 === false ? void 0 : typeof s2 == "string" ? s2 : typeof t2 == "string" ? t2.toLowerCase() : void 0;
  }
  u() {
    var t2;
    this._$E_ = new Promise((e3) => this.enableUpdating = e3), this._$AL = /* @__PURE__ */ new Map(), this._$Eg(), this.requestUpdate(), (t2 = this.constructor.h) === null || t2 === void 0 || t2.forEach((e3) => e3(this));
  }
  addController(t2) {
    var e3, s2;
    ((e3 = this._$ES) !== null && e3 !== void 0 ? e3 : this._$ES = []).push(t2), this.renderRoot !== void 0 && this.isConnected && ((s2 = t2.hostConnected) === null || s2 === void 0 || s2.call(t2));
  }
  removeController(t2) {
    var e3;
    (e3 = this._$ES) === null || e3 === void 0 || e3.splice(this._$ES.indexOf(t2) >>> 0, 1);
  }
  _$Eg() {
    this.constructor.elementProperties.forEach((t2, e3) => {
      this.hasOwnProperty(e3) && (this._$Ei.set(e3, this[e3]), delete this[e3]);
    });
  }
  createRenderRoot() {
    var t2;
    let e3 = (t2 = this.shadowRoot) !== null && t2 !== void 0 ? t2 : this.attachShadow(this.constructor.shadowRootOptions);
    return v5(e3, this.constructor.elementStyles), e3;
  }
  connectedCallback() {
    var t2;
    this.renderRoot === void 0 && (this.renderRoot = this.createRenderRoot()), this.enableUpdating(true), (t2 = this._$ES) === null || t2 === void 0 || t2.forEach((e3) => {
      var s2;
      return (s2 = e3.hostConnected) === null || s2 === void 0 ? void 0 : s2.call(e3);
    });
  }
  enableUpdating(t2) {
  }
  disconnectedCallback() {
    var t2;
    (t2 = this._$ES) === null || t2 === void 0 || t2.forEach((e3) => {
      var s2;
      return (s2 = e3.hostDisconnected) === null || s2 === void 0 ? void 0 : s2.call(e3);
    });
  }
  attributeChangedCallback(t2, e3, s2) {
    this._$AK(t2, s2);
  }
  _$EO(t2, e3, s2 = f) {
    var i3;
    let o = this.constructor._$Ep(t2, s2);
    if (o !== void 0 && s2.reflect === true) {
      let n2 = (((i3 = s2.converter) === null || i3 === void 0 ? void 0 : i3.toAttribute) !== void 0 ? s2.converter : y4).toAttribute(e3, s2.type);
      this._$El = t2, n2 == null ? this.removeAttribute(o) : this.setAttribute(o, n2), this._$El = null;
    }
  }
  _$AK(t2, e3) {
    var s2;
    let i3 = this.constructor, o = i3._$Ev.get(t2);
    if (o !== void 0 && this._$El !== o) {
      let n2 = i3.getPropertyOptions(o), g8 = typeof n2.converter == "function" ? { fromAttribute: n2.converter } : ((s2 = n2.converter) === null || s2 === void 0 ? void 0 : s2.fromAttribute) !== void 0 ? n2.converter : y4;
      this._$El = o, this[o] = g8.fromAttribute(e3, n2.type), this._$El = null;
    }
  }
  requestUpdate(t2, e3, s2) {
    let i3 = true;
    t2 !== void 0 && (((s2 = s2 || this.constructor.getPropertyOptions(t2)).hasChanged || b2)(this[t2], e3) ? (this._$AL.has(t2) || this._$AL.set(t2, e3), s2.reflect === true && this._$El !== t2 && (this._$EC === void 0 && (this._$EC = /* @__PURE__ */ new Map()), this._$EC.set(t2, s2))) : i3 = false), !this.isUpdatePending && i3 && (this._$E_ = this._$Ej());
  }
  async _$Ej() {
    this.isUpdatePending = true;
    try {
      await this._$E_;
    } catch (e3) {
      Promise.reject(e3);
    }
    let t2 = this.scheduleUpdate();
    return t2 != null && await t2, !this.isUpdatePending;
  }
  scheduleUpdate() {
    return this.performUpdate();
  }
  performUpdate() {
    var t2;
    if (!this.isUpdatePending)
      return;
    this.hasUpdated, this._$Ei && (this._$Ei.forEach((i3, o) => this[o] = i3), this._$Ei = void 0);
    let e3 = false, s2 = this._$AL;
    try {
      e3 = this.shouldUpdate(s2), e3 ? (this.willUpdate(s2), (t2 = this._$ES) === null || t2 === void 0 || t2.forEach((i3) => {
        var o;
        return (o = i3.hostUpdate) === null || o === void 0 ? void 0 : o.call(i3);
      }), this.update(s2)) : this._$Ek();
    } catch (i3) {
      throw e3 = false, this._$Ek(), i3;
    }
    e3 && this._$AE(s2);
  }
  willUpdate(t2) {
  }
  _$AE(t2) {
    var e3;
    (e3 = this._$ES) === null || e3 === void 0 || e3.forEach((s2) => {
      var i3;
      return (i3 = s2.hostUpdated) === null || i3 === void 0 ? void 0 : i3.call(s2);
    }), this.hasUpdated || (this.hasUpdated = true, this.firstUpdated(t2)), this.updated(t2);
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
  shouldUpdate(t2) {
    return true;
  }
  update(t2) {
    this._$EC !== void 0 && (this._$EC.forEach((e3, s2) => this._$EO(s2, this[s2], e3)), this._$EC = void 0), this._$Ek();
  }
  updated(t2) {
  }
  firstUpdated(t2) {
  }
};
a2.finalized = true, a2.elementProperties = /* @__PURE__ */ new Map(), a2.elementStyles = [], a2.shadowRootOptions = { mode: "open" }, m3?.({ ReactiveElement: a2 }), ((S3 = p3.reactiveElementVersions) !== null && S3 !== void 0 ? S3 : p3.reactiveElementVersions = []).push("1.6.1");

// https://esm.sh/v124/lit-html@2.7.4/deno/lit-html.mjs
var R3;
var S4 = window;
var x3 = S4.trustedTypes;
var D4 = x3 ? x3.createPolicy("lit-html", { createHTML: (h3) => h3 }) : void 0;
var E4 = "$lit$";
var _5 = `lit$${(Math.random() + "").slice(9)}$`;
var k5 = "?" + _5;
var X3 = `<${k5}>`;
var m4 = document;
var C7 = () => m4.createComment("");
var b3 = (h3) => h3 === null || typeof h3 != "object" && typeof h3 != "function";
var q2 = Array.isArray;
var G5 = (h3) => q2(h3) || typeof h3?.[Symbol.iterator] == "function";
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
var K4 = (h3) => (t2, ...e3) => ({ _$litType$: h3, strings: t2, values: e3 });
var tt3 = K4(1);
var et4 = K4(2);
var w4 = Symbol.for("lit-noChange");
var A4 = Symbol.for("lit-nothing");
var z3 = /* @__PURE__ */ new WeakMap();
var g3 = m4.createTreeWalker(m4, 129, null, false);
var Q3 = (h3, t2) => {
  let e3 = h3.length - 1, i3 = [], s2, o = t2 === 2 ? "<svg>" : "", n2 = N3;
  for (let l4 = 0; l4 < e3; l4++) {
    let r3 = h3[l4], u6, $4, d5 = -1, c7 = 0;
    for (; c7 < r3.length && (n2.lastIndex = c7, $4 = n2.exec(r3), $4 !== null); )
      c7 = n2.lastIndex, n2 === N3 ? $4[1] === "!--" ? n2 = W3 : $4[1] !== void 0 ? n2 = O3 : $4[2] !== void 0 ? (J5.test($4[2]) && (s2 = RegExp("</" + $4[2], "g")), n2 = p4) : $4[3] !== void 0 && (n2 = p4) : n2 === p4 ? $4[0] === ">" ? (n2 = s2 ?? N3, d5 = -1) : $4[1] === void 0 ? d5 = -2 : (d5 = n2.lastIndex - $4[2].length, u6 = $4[1], n2 = $4[3] === void 0 ? p4 : $4[3] === '"' ? Z2 : V2) : n2 === Z2 || n2 === V2 ? n2 = p4 : n2 === W3 || n2 === O3 ? n2 = N3 : (n2 = p4, s2 = void 0);
    let T7 = n2 === p4 && h3[l4 + 1].startsWith("/>") ? " " : "";
    o += n2 === N3 ? r3 + X3 : d5 >= 0 ? (i3.push(u6), r3.slice(0, d5) + E4 + r3.slice(d5) + _5 + T7) : r3 + _5 + (d5 === -2 ? (i3.push(void 0), l4) : T7);
  }
  let a3 = o + (h3[e3] || "<?>") + (t2 === 2 ? "</svg>" : "");
  if (!Array.isArray(h3) || !h3.hasOwnProperty("raw"))
    throw Error("invalid template strings array");
  return [D4 !== void 0 ? D4.createHTML(a3) : a3, i3];
};
var f2 = class {
  constructor({ strings: t2, _$litType$: e3 }, i3) {
    let s2;
    this.parts = [];
    let o = 0, n2 = 0, a3 = t2.length - 1, l4 = this.parts, [r3, u6] = Q3(t2, e3);
    if (this.el = f2.createElement(r3, i3), g3.currentNode = this.el.content, e3 === 2) {
      let $4 = this.el.content, d5 = $4.firstChild;
      d5.remove(), $4.append(...d5.childNodes);
    }
    for (; (s2 = g3.nextNode()) !== null && l4.length < a3; ) {
      if (s2.nodeType === 1) {
        if (s2.hasAttributes()) {
          let $4 = [];
          for (let d5 of s2.getAttributeNames())
            if (d5.endsWith(E4) || d5.startsWith(_5)) {
              let c7 = u6[n2++];
              if ($4.push(d5), c7 !== void 0) {
                let T7 = s2.getAttribute(c7.toLowerCase() + E4).split(_5), M8 = /([.?@])?(.*)/.exec(c7);
                l4.push({ type: 1, index: o, name: M8[2], strings: T7, ctor: M8[1] === "." ? B4 : M8[1] === "?" ? P3 : M8[1] === "@" ? U3 : H5 });
              } else
                l4.push({ type: 6, index: o });
            }
          for (let d5 of $4)
            s2.removeAttribute(d5);
        }
        if (J5.test(s2.tagName)) {
          let $4 = s2.textContent.split(_5), d5 = $4.length - 1;
          if (d5 > 0) {
            s2.textContent = x3 ? x3.emptyScript : "";
            for (let c7 = 0; c7 < d5; c7++)
              s2.append($4[c7], C7()), g3.nextNode(), l4.push({ type: 2, index: ++o });
            s2.append($4[d5], C7());
          }
        }
      } else if (s2.nodeType === 8)
        if (s2.data === k5)
          l4.push({ type: 2, index: o });
        else {
          let $4 = -1;
          for (; ($4 = s2.data.indexOf(_5, $4 + 1)) !== -1; )
            l4.push({ type: 7, index: o }), $4 += _5.length - 1;
        }
      o++;
    }
  }
  static createElement(t2, e3) {
    let i3 = m4.createElement("template");
    return i3.innerHTML = t2, i3;
  }
};
function y5(h3, t2, e3 = h3, i3) {
  var s2, o, n2, a3;
  if (t2 === w4)
    return t2;
  let l4 = i3 !== void 0 ? (s2 = e3._$Co) === null || s2 === void 0 ? void 0 : s2[i3] : e3._$Cl, r3 = b3(t2) ? void 0 : t2._$litDirective$;
  return l4?.constructor !== r3 && ((o = l4?._$AO) === null || o === void 0 || o.call(l4, false), r3 === void 0 ? l4 = void 0 : (l4 = new r3(h3), l4._$AT(h3, e3, i3)), i3 !== void 0 ? ((n2 = (a3 = e3)._$Co) !== null && n2 !== void 0 ? n2 : a3._$Co = [])[i3] = l4 : e3._$Cl = l4), l4 !== void 0 && (t2 = y5(h3, l4._$AS(h3, t2.values), l4, i3)), t2;
}
var I2 = class {
  constructor(t2, e3) {
    this._$AV = [], this._$AN = void 0, this._$AD = t2, this._$AM = e3;
  }
  get parentNode() {
    return this._$AM.parentNode;
  }
  get _$AU() {
    return this._$AM._$AU;
  }
  u(t2) {
    var e3;
    let { el: { content: i3 }, parts: s2 } = this._$AD, o = ((e3 = t2?.creationScope) !== null && e3 !== void 0 ? e3 : m4).importNode(i3, true);
    g3.currentNode = o;
    let n2 = g3.nextNode(), a3 = 0, l4 = 0, r3 = s2[0];
    for (; r3 !== void 0; ) {
      if (a3 === r3.index) {
        let u6;
        r3.type === 2 ? u6 = new v6(n2, n2.nextSibling, this, t2) : r3.type === 1 ? u6 = new r3.ctor(n2, r3.name, r3.strings, this, t2) : r3.type === 6 && (u6 = new L3(n2, this, t2)), this._$AV.push(u6), r3 = s2[++l4];
      }
      a3 !== r3?.index && (n2 = g3.nextNode(), a3++);
    }
    return g3.currentNode = m4, o;
  }
  v(t2) {
    let e3 = 0;
    for (let i3 of this._$AV)
      i3 !== void 0 && (i3.strings !== void 0 ? (i3._$AI(t2, i3, e3), e3 += i3.strings.length - 2) : i3._$AI(t2[e3])), e3++;
  }
};
var v6 = class {
  constructor(t2, e3, i3, s2) {
    var o;
    this.type = 2, this._$AH = A4, this._$AN = void 0, this._$AA = t2, this._$AB = e3, this._$AM = i3, this.options = s2, this._$Cp = (o = s2?.isConnected) === null || o === void 0 || o;
  }
  get _$AU() {
    var t2, e3;
    return (e3 = (t2 = this._$AM) === null || t2 === void 0 ? void 0 : t2._$AU) !== null && e3 !== void 0 ? e3 : this._$Cp;
  }
  get parentNode() {
    let t2 = this._$AA.parentNode, e3 = this._$AM;
    return e3 !== void 0 && t2?.nodeType === 11 && (t2 = e3.parentNode), t2;
  }
  get startNode() {
    return this._$AA;
  }
  get endNode() {
    return this._$AB;
  }
  _$AI(t2, e3 = this) {
    t2 = y5(this, t2, e3), b3(t2) ? t2 === A4 || t2 == null || t2 === "" ? (this._$AH !== A4 && this._$AR(), this._$AH = A4) : t2 !== this._$AH && t2 !== w4 && this._(t2) : t2._$litType$ !== void 0 ? this.g(t2) : t2.nodeType !== void 0 ? this.$(t2) : G5(t2) ? this.T(t2) : this._(t2);
  }
  k(t2) {
    return this._$AA.parentNode.insertBefore(t2, this._$AB);
  }
  $(t2) {
    this._$AH !== t2 && (this._$AR(), this._$AH = this.k(t2));
  }
  _(t2) {
    this._$AH !== A4 && b3(this._$AH) ? this._$AA.nextSibling.data = t2 : this.$(m4.createTextNode(t2)), this._$AH = t2;
  }
  g(t2) {
    var e3;
    let { values: i3, _$litType$: s2 } = t2, o = typeof s2 == "number" ? this._$AC(t2) : (s2.el === void 0 && (s2.el = f2.createElement(s2.h, this.options)), s2);
    if (((e3 = this._$AH) === null || e3 === void 0 ? void 0 : e3._$AD) === o)
      this._$AH.v(i3);
    else {
      let n2 = new I2(o, this), a3 = n2.u(this.options);
      n2.v(i3), this.$(a3), this._$AH = n2;
    }
  }
  _$AC(t2) {
    let e3 = z3.get(t2.strings);
    return e3 === void 0 && z3.set(t2.strings, e3 = new f2(t2)), e3;
  }
  T(t2) {
    q2(this._$AH) || (this._$AH = [], this._$AR());
    let e3 = this._$AH, i3, s2 = 0;
    for (let o of t2)
      s2 === e3.length ? e3.push(i3 = new v6(this.k(C7()), this.k(C7()), this, this.options)) : i3 = e3[s2], i3._$AI(o), s2++;
    s2 < e3.length && (this._$AR(i3 && i3._$AB.nextSibling, s2), e3.length = s2);
  }
  _$AR(t2 = this._$AA.nextSibling, e3) {
    var i3;
    for ((i3 = this._$AP) === null || i3 === void 0 || i3.call(this, false, true, e3); t2 && t2 !== this._$AB; ) {
      let s2 = t2.nextSibling;
      t2.remove(), t2 = s2;
    }
  }
  setConnected(t2) {
    var e3;
    this._$AM === void 0 && (this._$Cp = t2, (e3 = this._$AP) === null || e3 === void 0 || e3.call(this, t2));
  }
};
var H5 = class {
  constructor(t2, e3, i3, s2, o) {
    this.type = 1, this._$AH = A4, this._$AN = void 0, this.element = t2, this.name = e3, this._$AM = s2, this.options = o, i3.length > 2 || i3[0] !== "" || i3[1] !== "" ? (this._$AH = Array(i3.length - 1).fill(new String()), this.strings = i3) : this._$AH = A4;
  }
  get tagName() {
    return this.element.tagName;
  }
  get _$AU() {
    return this._$AM._$AU;
  }
  _$AI(t2, e3 = this, i3, s2) {
    let o = this.strings, n2 = false;
    if (o === void 0)
      t2 = y5(this, t2, e3, 0), n2 = !b3(t2) || t2 !== this._$AH && t2 !== w4, n2 && (this._$AH = t2);
    else {
      let a3 = t2, l4, r3;
      for (t2 = o[0], l4 = 0; l4 < o.length - 1; l4++)
        r3 = y5(this, a3[i3 + l4], e3, l4), r3 === w4 && (r3 = this._$AH[l4]), n2 || (n2 = !b3(r3) || r3 !== this._$AH[l4]), r3 === A4 ? t2 = A4 : t2 !== A4 && (t2 += (r3 ?? "") + o[l4 + 1]), this._$AH[l4] = r3;
    }
    n2 && !s2 && this.j(t2);
  }
  j(t2) {
    t2 === A4 ? this.element.removeAttribute(this.name) : this.element.setAttribute(this.name, t2 ?? "");
  }
};
var B4 = class extends H5 {
  constructor() {
    super(...arguments), this.type = 3;
  }
  j(t2) {
    this.element[this.name] = t2 === A4 ? void 0 : t2;
  }
};
var Y3 = x3 ? x3.emptyScript : "";
var P3 = class extends H5 {
  constructor() {
    super(...arguments), this.type = 4;
  }
  j(t2) {
    t2 && t2 !== A4 ? this.element.setAttribute(this.name, Y3) : this.element.removeAttribute(this.name);
  }
};
var U3 = class extends H5 {
  constructor(t2, e3, i3, s2, o) {
    super(t2, e3, i3, s2, o), this.type = 5;
  }
  _$AI(t2, e3 = this) {
    var i3;
    if ((t2 = (i3 = y5(this, t2, e3, 0)) !== null && i3 !== void 0 ? i3 : A4) === w4)
      return;
    let s2 = this._$AH, o = t2 === A4 && s2 !== A4 || t2.capture !== s2.capture || t2.once !== s2.once || t2.passive !== s2.passive, n2 = t2 !== A4 && (s2 === A4 || o);
    o && this.element.removeEventListener(this.name, this, s2), n2 && this.element.addEventListener(this.name, this, t2), this._$AH = t2;
  }
  handleEvent(t2) {
    var e3, i3;
    typeof this._$AH == "function" ? this._$AH.call((i3 = (e3 = this.options) === null || e3 === void 0 ? void 0 : e3.host) !== null && i3 !== void 0 ? i3 : this.element, t2) : this._$AH.handleEvent(t2);
  }
};
var L3 = class {
  constructor(t2, e3, i3) {
    this.element = t2, this.type = 6, this._$AN = void 0, this._$AM = e3, this.options = i3;
  }
  get _$AU() {
    return this._$AM._$AU;
  }
  _$AI(t2) {
    y5(this, t2);
  }
};
var F3 = S4.litHtmlPolyfillSupport;
F3?.(f2, v6), ((R3 = S4.litHtmlVersions) !== null && R3 !== void 0 ? R3 : S4.litHtmlVersions = []).push("2.7.4");
var st3 = (h3, t2, e3) => {
  var i3, s2;
  let o = (i3 = e3?.renderBefore) !== null && i3 !== void 0 ? i3 : t2, n2 = o._$litPart$;
  if (n2 === void 0) {
    let a3 = (s2 = e3?.renderBefore) !== null && s2 !== void 0 ? s2 : null;
    o._$litPart$ = n2 = new v6(t2.insertBefore(C7(), a3), a3, void 0, e3 ?? {});
  }
  return n2._$AI(h3), n2;
};

// https://esm.sh/v124/lit-element@3.3.2/deno/lit-element.js
var r2;
var s;
var n = class extends a2 {
  constructor() {
    super(...arguments), this.renderOptions = { host: this }, this._$Do = void 0;
  }
  createRenderRoot() {
    var e3, t2;
    let i3 = super.createRenderRoot();
    return (e3 = (t2 = this.renderOptions).renderBefore) !== null && e3 !== void 0 || (t2.renderBefore = i3.firstChild), i3;
  }
  update(e3) {
    let t2 = this.render();
    this.hasUpdated || (this.renderOptions.isConnected = this.isConnected), super.update(e3), this._$Do = st3(t2, this.renderRoot, this.renderOptions);
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
    return w4;
  }
};
n.finalized = true, n._$litElement$ = true, (r2 = globalThis.litElementHydrateSupport) === null || r2 === void 0 || r2.call(globalThis, { LitElement: n });
var l3 = globalThis.litElementPolyfillSupport;
l3?.({ LitElement: n });
((s = globalThis.litElementVersions) !== null && s !== void 0 ? s : globalThis.litElementVersions = []).push("3.3.2");

// https://esm.sh/v124/@lit/reactive-element@1.6.1/deno/decorators/custom-element.js
var c4 = (s2) => (t2) => typeof t2 == "function" ? ((n2, e3) => (customElements.define(n2, e3), e3))(s2, t2) : ((n2, e3) => {
  let { kind: m9, elements: o } = e3;
  return { kind: m9, elements: o, finisher(i3) {
    customElements.define(n2, i3);
  } };
})(s2, t2);

// https://esm.sh/v124/@lit/reactive-element@1.6.1/deno/decorators/property.js
var c5 = (t2, i3) => i3.kind === "method" && i3.descriptor && !("value" in i3.descriptor) ? { ...i3, finisher(r3) {
  r3.createProperty(i3.key, t2);
} } : { kind: "field", key: Symbol(), placement: "own", descriptor: {}, originalKey: i3.key, initializer() {
  typeof i3.initializer == "function" && (this[i3.key] = i3.initializer.call(this));
}, finisher(r3) {
  r3.createProperty(i3.key, t2);
} };
function y6(t2) {
  return (i3, r3) => r3 !== void 0 ? ((e3, o, n2) => {
    o.constructor.createProperty(n2, e3);
  })(t2, i3, r3) : c5(t2, i3);
}

// https://esm.sh/v124/@lit/reactive-element@1.6.1/deno/decorators/state.js
var p5 = (t2, r3) => r3.kind === "method" && r3.descriptor && !("value" in r3.descriptor) ? { ...r3, finisher(i3) {
  i3.createProperty(r3.key, t2);
} } : { kind: "field", key: Symbol(), placement: "own", descriptor: {}, originalKey: r3.key, initializer() {
  typeof r3.initializer == "function" && (this[r3.key] = r3.initializer.call(this));
}, finisher(i3) {
  i3.createProperty(r3.key, t2);
} };
function e2(t2) {
  return (r3, i3) => i3 !== void 0 ? ((o, n2, a3) => {
    n2.constructor.createProperty(a3, o);
  })(t2, r3, i3) : p5(t2, r3);
}
function y7(t2) {
  return e2({ ...t2, state: true });
}

// https://esm.sh/v124/@lit/reactive-element@1.6.1/deno/decorators/query.js
var d3 = ({ finisher: o, descriptor: i3 }) => (t2, n2) => {
  var r3;
  if (n2 === void 0) {
    let e3 = (r3 = t2.originalKey) !== null && r3 !== void 0 ? r3 : t2.key, l4 = i3 != null ? { kind: "method", placement: "prototype", key: e3, descriptor: i3(t2.key) } : { ...t2, key: e3 };
    return o != null && (l4.finisher = function(c7) {
      o(c7, e3);
    }), l4;
  }
  {
    let e3 = t2.constructor;
    i3 !== void 0 && Object.defineProperty(t2, n2, i3(n2)), o?.(e3, n2);
  }
};
function y8(o, i3) {
  return d3({ descriptor: (t2) => {
    let n2 = { get() {
      var r3, e3;
      return (e3 = (r3 = this.renderRoot) === null || r3 === void 0 ? void 0 : r3.querySelector(o)) !== null && e3 !== void 0 ? e3 : null;
    }, enumerable: true, configurable: true };
    if (i3) {
      let r3 = typeof t2 == "symbol" ? Symbol() : "__" + t2;
      n2.get = function() {
        var e3, l4;
        return this[r3] === void 0 && (this[r3] = (l4 = (e3 = this.renderRoot) === null || e3 === void 0 ? void 0 : e3.querySelector(o)) !== null && l4 !== void 0 ? l4 : null), this[r3];
      };
    }
    return n2;
  } });
}

// https://esm.sh/v124/@lit/reactive-element@1.6.1/deno/decorators/query-assigned-elements.js
var d4;
var p6 = ((d4 = window.HTMLSlotElement) === null || d4 === void 0 ? void 0 : d4.prototype.assignedElements) != null ? (e3, t2) => e3.assignedElements(t2) : (e3, t2) => e3.assignedNodes(t2).filter((o) => o.nodeType === Node.ELEMENT_NODE);

// https://esm.sh/v124/@lit/reactive-element@1.6.1/deno/decorators/query-assigned-nodes.js
var c6;
var y9 = ((c6 = window.HTMLSlotElement) === null || c6 === void 0 ? void 0 : c6.prototype.assignedElements) != null ? (e3, t2) => e3.assignedElements(t2) : (e3, t2) => e3.assignedNodes(t2).filter((o) => o.nodeType === Node.ELEMENT_NODE);

// https://esm.sh/v124/lit-html@2.7.4/deno/directives/when.js
function t(e3, o, n2) {
  return e3 ? o() : n2?.();
}

// src/emulator/controller/haptics.controller.ts
var HapticsController = class extends BaseController {
  constructor() {
    super(...arguments);
    this._init = (async () => {
      this.emitInit();
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
      this.emitReady();
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
      this.emitInit();
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
      this.emitReady();
    })();
    this.observer = new StateObservable(() => {
      return JSON.stringify(this.navigationBarGetState());
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
  emitUpdate() {
    this.observer.notifyObserver();
    super.emitUpdate();
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
      this.emitInit();
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
      this.emitReady();
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
      this.emitInit();
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
      this.emitReady();
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
  emitUpdate() {
    this.observer.notifyObserver();
    super.emitUpdate();
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

// https://esm.sh/v124/lit-html@2.7.4/deno/static.js
var E5;
var T3 = window;
var m5 = T3.trustedTypes;
var D5 = m5 ? m5.createPolicy("lit-html", { createHTML: (r3) => r3 }) : void 0;
var B5 = "$lit$";
var _6 = `lit$${(Math.random() + "").slice(9)}$`;
var q3 = "?" + _6;
var et5 = `<${q3}>`;
var g4 = document;
var M4 = () => g4.createComment("");
var b4 = (r3) => r3 === null || typeof r3 != "object" && typeof r3 != "function";
var G6 = Array.isArray;
var it3 = (r3) => G6(r3) || typeof r3?.[Symbol.iterator] == "function";
var I3 = `[ 	
\f\r]`;
var N4 = /<(?:(!--|\/[^a-zA-Z])|(\/?[a-zA-Z][^>\s]*)|(\/?$))/g;
var V3 = /-->/g;
var W4 = />/g;
var v7 = RegExp(`>|${I3}(?:([^\\s"'>=/]+)(${I3}*=${I3}*(?:[^ 	
\f\r"'\`<>=]|("|')|))|$)`, "g");
var O4 = /'/g;
var Z3 = /"/g;
var J6 = /^(?:script|style|textarea|title)$/i;
var K5 = (r3) => (t2, ...e3) => ({ _$litType$: r3, strings: t2, values: e3 });
var Q4 = K5(1);
var X4 = K5(2);
var w5 = Symbol.for("lit-noChange");
var u4 = Symbol.for("lit-nothing");
var z4 = /* @__PURE__ */ new WeakMap();
var p7 = g4.createTreeWalker(g4, 129, null, false);
var st4 = (r3, t2) => {
  let e3 = r3.length - 1, i3 = [], s2, o = t2 === 2 ? "<svg>" : "", n2 = N4;
  for (let h3 = 0; h3 < e3; h3++) {
    let l4 = r3[h3], c7, a3, $4 = -1, A5 = 0;
    for (; A5 < l4.length && (n2.lastIndex = A5, a3 = n2.exec(l4), a3 !== null); )
      A5 = n2.lastIndex, n2 === N4 ? a3[1] === "!--" ? n2 = V3 : a3[1] !== void 0 ? n2 = W4 : a3[2] !== void 0 ? (J6.test(a3[2]) && (s2 = RegExp("</" + a3[2], "g")), n2 = v7) : a3[3] !== void 0 && (n2 = v7) : n2 === v7 ? a3[0] === ">" ? (n2 = s2 ?? N4, $4 = -1) : a3[1] === void 0 ? $4 = -2 : ($4 = n2.lastIndex - a3[2].length, c7 = a3[1], n2 = a3[3] === void 0 ? v7 : a3[3] === '"' ? Z3 : O4) : n2 === Z3 || n2 === O4 ? n2 = v7 : n2 === V3 || n2 === W4 ? n2 = N4 : (n2 = v7, s2 = void 0);
    let S8 = n2 === v7 && r3[h3 + 1].startsWith("/>") ? " " : "";
    o += n2 === N4 ? l4 + et5 : $4 >= 0 ? (i3.push(c7), l4.slice(0, $4) + B5 + l4.slice($4) + _6 + S8) : l4 + _6 + ($4 === -2 ? (i3.push(void 0), h3) : S8);
  }
  let d5 = o + (r3[e3] || "<?>") + (t2 === 2 ? "</svg>" : "");
  if (!Array.isArray(r3) || !r3.hasOwnProperty("raw"))
    throw Error("invalid template strings array");
  return [D5 !== void 0 ? D5.createHTML(d5) : d5, i3];
};
var f3 = class {
  constructor({ strings: t2, _$litType$: e3 }, i3) {
    let s2;
    this.parts = [];
    let o = 0, n2 = 0, d5 = t2.length - 1, h3 = this.parts, [l4, c7] = st4(t2, e3);
    if (this.el = f3.createElement(l4, i3), p7.currentNode = this.el.content, e3 === 2) {
      let a3 = this.el.content, $4 = a3.firstChild;
      $4.remove(), a3.append(...$4.childNodes);
    }
    for (; (s2 = p7.nextNode()) !== null && h3.length < d5; ) {
      if (s2.nodeType === 1) {
        if (s2.hasAttributes()) {
          let a3 = [];
          for (let $4 of s2.getAttributeNames())
            if ($4.endsWith(B5) || $4.startsWith(_6)) {
              let A5 = c7[n2++];
              if (a3.push($4), A5 !== void 0) {
                let S8 = s2.getAttribute(A5.toLowerCase() + B5).split(_6), C11 = /([.?@])?(.*)/.exec(A5);
                h3.push({ type: 1, index: o, name: C11[2], strings: S8, ctor: C11[1] === "." ? P4 : C11[1] === "?" ? j6 : C11[1] === "@" ? L4 : x4 });
              } else
                h3.push({ type: 6, index: o });
            }
          for (let $4 of a3)
            s2.removeAttribute($4);
        }
        if (J6.test(s2.tagName)) {
          let a3 = s2.textContent.split(_6), $4 = a3.length - 1;
          if ($4 > 0) {
            s2.textContent = m5 ? m5.emptyScript : "";
            for (let A5 = 0; A5 < $4; A5++)
              s2.append(a3[A5], M4()), p7.nextNode(), h3.push({ type: 2, index: ++o });
            s2.append(a3[$4], M4());
          }
        }
      } else if (s2.nodeType === 8)
        if (s2.data === q3)
          h3.push({ type: 2, index: o });
        else {
          let a3 = -1;
          for (; (a3 = s2.data.indexOf(_6, a3 + 1)) !== -1; )
            h3.push({ type: 7, index: o }), a3 += _6.length - 1;
        }
      o++;
    }
  }
  static createElement(t2, e3) {
    let i3 = g4.createElement("template");
    return i3.innerHTML = t2, i3;
  }
};
function y10(r3, t2, e3 = r3, i3) {
  var s2, o, n2, d5;
  if (t2 === w5)
    return t2;
  let h3 = i3 !== void 0 ? (s2 = e3._$Co) === null || s2 === void 0 ? void 0 : s2[i3] : e3._$Cl, l4 = b4(t2) ? void 0 : t2._$litDirective$;
  return h3?.constructor !== l4 && ((o = h3?._$AO) === null || o === void 0 || o.call(h3, false), l4 === void 0 ? h3 = void 0 : (h3 = new l4(r3), h3._$AT(r3, e3, i3)), i3 !== void 0 ? ((n2 = (d5 = e3)._$Co) !== null && n2 !== void 0 ? n2 : d5._$Co = [])[i3] = h3 : e3._$Cl = h3), h3 !== void 0 && (t2 = y10(r3, h3._$AS(r3, t2.values), h3, i3)), t2;
}
var U4 = class {
  constructor(t2, e3) {
    this._$AV = [], this._$AN = void 0, this._$AD = t2, this._$AM = e3;
  }
  get parentNode() {
    return this._$AM.parentNode;
  }
  get _$AU() {
    return this._$AM._$AU;
  }
  u(t2) {
    var e3;
    let { el: { content: i3 }, parts: s2 } = this._$AD, o = ((e3 = t2?.creationScope) !== null && e3 !== void 0 ? e3 : g4).importNode(i3, true);
    p7.currentNode = o;
    let n2 = p7.nextNode(), d5 = 0, h3 = 0, l4 = s2[0];
    for (; l4 !== void 0; ) {
      if (d5 === l4.index) {
        let c7;
        l4.type === 2 ? c7 = new H6(n2, n2.nextSibling, this, t2) : l4.type === 1 ? c7 = new l4.ctor(n2, l4.name, l4.strings, this, t2) : l4.type === 6 && (c7 = new R4(n2, this, t2)), this._$AV.push(c7), l4 = s2[++h3];
      }
      d5 !== l4?.index && (n2 = p7.nextNode(), d5++);
    }
    return p7.currentNode = g4, o;
  }
  v(t2) {
    let e3 = 0;
    for (let i3 of this._$AV)
      i3 !== void 0 && (i3.strings !== void 0 ? (i3._$AI(t2, i3, e3), e3 += i3.strings.length - 2) : i3._$AI(t2[e3])), e3++;
  }
};
var H6 = class {
  constructor(t2, e3, i3, s2) {
    var o;
    this.type = 2, this._$AH = u4, this._$AN = void 0, this._$AA = t2, this._$AB = e3, this._$AM = i3, this.options = s2, this._$Cp = (o = s2?.isConnected) === null || o === void 0 || o;
  }
  get _$AU() {
    var t2, e3;
    return (e3 = (t2 = this._$AM) === null || t2 === void 0 ? void 0 : t2._$AU) !== null && e3 !== void 0 ? e3 : this._$Cp;
  }
  get parentNode() {
    let t2 = this._$AA.parentNode, e3 = this._$AM;
    return e3 !== void 0 && t2?.nodeType === 11 && (t2 = e3.parentNode), t2;
  }
  get startNode() {
    return this._$AA;
  }
  get endNode() {
    return this._$AB;
  }
  _$AI(t2, e3 = this) {
    t2 = y10(this, t2, e3), b4(t2) ? t2 === u4 || t2 == null || t2 === "" ? (this._$AH !== u4 && this._$AR(), this._$AH = u4) : t2 !== this._$AH && t2 !== w5 && this._(t2) : t2._$litType$ !== void 0 ? this.g(t2) : t2.nodeType !== void 0 ? this.$(t2) : it3(t2) ? this.T(t2) : this._(t2);
  }
  k(t2) {
    return this._$AA.parentNode.insertBefore(t2, this._$AB);
  }
  $(t2) {
    this._$AH !== t2 && (this._$AR(), this._$AH = this.k(t2));
  }
  _(t2) {
    this._$AH !== u4 && b4(this._$AH) ? this._$AA.nextSibling.data = t2 : this.$(g4.createTextNode(t2)), this._$AH = t2;
  }
  g(t2) {
    var e3;
    let { values: i3, _$litType$: s2 } = t2, o = typeof s2 == "number" ? this._$AC(t2) : (s2.el === void 0 && (s2.el = f3.createElement(s2.h, this.options)), s2);
    if (((e3 = this._$AH) === null || e3 === void 0 ? void 0 : e3._$AD) === o)
      this._$AH.v(i3);
    else {
      let n2 = new U4(o, this), d5 = n2.u(this.options);
      n2.v(i3), this.$(d5), this._$AH = n2;
    }
  }
  _$AC(t2) {
    let e3 = z4.get(t2.strings);
    return e3 === void 0 && z4.set(t2.strings, e3 = new f3(t2)), e3;
  }
  T(t2) {
    G6(this._$AH) || (this._$AH = [], this._$AR());
    let e3 = this._$AH, i3, s2 = 0;
    for (let o of t2)
      s2 === e3.length ? e3.push(i3 = new H6(this.k(M4()), this.k(M4()), this, this.options)) : i3 = e3[s2], i3._$AI(o), s2++;
    s2 < e3.length && (this._$AR(i3 && i3._$AB.nextSibling, s2), e3.length = s2);
  }
  _$AR(t2 = this._$AA.nextSibling, e3) {
    var i3;
    for ((i3 = this._$AP) === null || i3 === void 0 || i3.call(this, false, true, e3); t2 && t2 !== this._$AB; ) {
      let s2 = t2.nextSibling;
      t2.remove(), t2 = s2;
    }
  }
  setConnected(t2) {
    var e3;
    this._$AM === void 0 && (this._$Cp = t2, (e3 = this._$AP) === null || e3 === void 0 || e3.call(this, t2));
  }
};
var x4 = class {
  constructor(t2, e3, i3, s2, o) {
    this.type = 1, this._$AH = u4, this._$AN = void 0, this.element = t2, this.name = e3, this._$AM = s2, this.options = o, i3.length > 2 || i3[0] !== "" || i3[1] !== "" ? (this._$AH = Array(i3.length - 1).fill(new String()), this.strings = i3) : this._$AH = u4;
  }
  get tagName() {
    return this.element.tagName;
  }
  get _$AU() {
    return this._$AM._$AU;
  }
  _$AI(t2, e3 = this, i3, s2) {
    let o = this.strings, n2 = false;
    if (o === void 0)
      t2 = y10(this, t2, e3, 0), n2 = !b4(t2) || t2 !== this._$AH && t2 !== w5, n2 && (this._$AH = t2);
    else {
      let d5 = t2, h3, l4;
      for (t2 = o[0], h3 = 0; h3 < o.length - 1; h3++)
        l4 = y10(this, d5[i3 + h3], e3, h3), l4 === w5 && (l4 = this._$AH[h3]), n2 || (n2 = !b4(l4) || l4 !== this._$AH[h3]), l4 === u4 ? t2 = u4 : t2 !== u4 && (t2 += (l4 ?? "") + o[h3 + 1]), this._$AH[h3] = l4;
    }
    n2 && !s2 && this.j(t2);
  }
  j(t2) {
    t2 === u4 ? this.element.removeAttribute(this.name) : this.element.setAttribute(this.name, t2 ?? "");
  }
};
var P4 = class extends x4 {
  constructor() {
    super(...arguments), this.type = 3;
  }
  j(t2) {
    this.element[this.name] = t2 === u4 ? void 0 : t2;
  }
};
var nt4 = m5 ? m5.emptyScript : "";
var j6 = class extends x4 {
  constructor() {
    super(...arguments), this.type = 4;
  }
  j(t2) {
    t2 && t2 !== u4 ? this.element.setAttribute(this.name, nt4) : this.element.removeAttribute(this.name);
  }
};
var L4 = class extends x4 {
  constructor(t2, e3, i3, s2, o) {
    super(t2, e3, i3, s2, o), this.type = 5;
  }
  _$AI(t2, e3 = this) {
    var i3;
    if ((t2 = (i3 = y10(this, t2, e3, 0)) !== null && i3 !== void 0 ? i3 : u4) === w5)
      return;
    let s2 = this._$AH, o = t2 === u4 && s2 !== u4 || t2.capture !== s2.capture || t2.once !== s2.once || t2.passive !== s2.passive, n2 = t2 !== u4 && (s2 === u4 || o);
    o && this.element.removeEventListener(this.name, this, s2), n2 && this.element.addEventListener(this.name, this, t2), this._$AH = t2;
  }
  handleEvent(t2) {
    var e3, i3;
    typeof this._$AH == "function" ? this._$AH.call((i3 = (e3 = this.options) === null || e3 === void 0 ? void 0 : e3.host) !== null && i3 !== void 0 ? i3 : this.element, t2) : this._$AH.handleEvent(t2);
  }
};
var R4 = class {
  constructor(t2, e3, i3) {
    this.element = t2, this.type = 6, this._$AN = void 0, this._$AM = e3, this.options = i3;
  }
  get _$AU() {
    return this._$AM._$AU;
  }
  _$AI(t2) {
    y10(this, t2);
  }
};
var F4 = T3.litHtmlPolyfillSupport;
F4?.(f3, H6), ((E5 = T3.litHtmlVersions) !== null && E5 !== void 0 ? E5 : T3.litHtmlVersions = []).push("2.7.4");
var k6 = Symbol.for("");
var ot4 = (r3) => {
  if (r3?.r === k6)
    return r3?._$litStatic$;
};
var Y4 = /* @__PURE__ */ new Map();
var tt4 = (r3) => (t2, ...e3) => {
  let i3 = e3.length, s2, o, n2 = [], d5 = [], h3, l4 = 0, c7 = false;
  for (; l4 < i3; ) {
    for (h3 = t2[l4]; l4 < i3 && (o = e3[l4], (s2 = ot4(o)) !== void 0); )
      h3 += s2 + t2[++l4], c7 = true;
    l4 !== i3 && d5.push(o), n2.push(h3), l4++;
  }
  if (l4 === i3 && n2.push(t2[i3]), c7) {
    let a3 = n2.join("$$lit$$");
    (t2 = Y4.get(a3)) === void 0 && (n2.raw = n2, Y4.set(a3, t2 = n2)), e3 = d5;
  }
  return r3(t2, ...e3);
};
var $t2 = tt4(Q4);
var dt3 = tt4(X4);

// src/emulator/emulator-toolbar.html.ts
var TAG = "emulator-toolbar";
var EmulatorToolbarElement = class extends n {
  constructor() {
    super(...arguments);
    this.url = "";
    this._on_keydown_reload = (e3) => {
      e3 = e3 || window.event;
      if (e3.ctrlKey && e3.keyCode == 82 || //ctrl+R
      e3.keyCode == 116) {
        debugger;
      }
    };
  }
  connectedCallback() {
    super.connectedCallback();
    document.addEventListener("keydown", this._on_keydown_reload);
  }
  disconnectedCallback() {
    super.disconnectedCallback();
    document.removeEventListener("keydown", this._on_keydown_reload);
  }
  render() {
    return $t2`
      <div class="bar">
        <input
          .value=${this.url}
          readonly
          @input=${(e3) => {
      this.url = e3.target.value;
    }}
        />
      </div>
    `;
  }
};
EmulatorToolbarElement.styles = createAllCSS();
__decorateClass([
  y6({ type: String })
], EmulatorToolbarElement.prototype, "url", 2);
EmulatorToolbarElement = __decorateClass([
  c4(TAG)
], EmulatorToolbarElement);
function createAllCSS() {
  return [
    C6`
      :host {
        display: block;
      }
      .bar {
        background: #00000033;
      }
      input {
        width: 100%;
        height: 2em;
      }
    `
  ];
}

// src/emulator/multi-webview-comp-biometrics.html.ts
var TAG2 = "multi-webview-comp-biometrics";
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
MultiWebviewCompBiometrics.styles = createAllCSS2();
MultiWebviewCompBiometrics = __decorateClass([
  c4(TAG2)
], MultiWebviewCompBiometrics);
function createAllCSS2() {
  return [
    C6`
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
var TAG3 = "multi-webview-comp-haptics";
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
MultiWebviewCompHaptics.styles = createAllCSS3();
__decorateClass([
  y6({ type: String })
], MultiWebviewCompHaptics.prototype, "text", 2);
MultiWebviewCompHaptics = __decorateClass([
  c4(TAG3)
], MultiWebviewCompHaptics);
function createAllCSS3() {
  return [
    C6`
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
var TAG4 = "multi-webview-comp-mobile-shell";
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
        const a3 = new Uint8Array(s2.split(","));
        const blob = new Blob([a3], { type: contentType });
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
MultiWebViewCompMobileShell.styles = createAllCSS4();
__decorateClass([
  y8(".app_content_container")
], MultiWebViewCompMobileShell.prototype, "appContentContainer", 2);
MultiWebViewCompMobileShell = __decorateClass([
  c4(TAG4)
], MultiWebViewCompMobileShell);
function createAllCSS4() {
  return [
    C6`
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

// https://esm.sh/v124/lit-html@2.7.4/deno/directives/style-map.js
var P5;
var w6 = window;
var y11 = w6.trustedTypes;
var j7 = y11 ? y11.createPolicy("lit-html", { createHTML: (o) => o }) : void 0;
var B6 = "$lit$";
var _7 = `lit$${(Math.random() + "").slice(9)}$`;
var Y5 = "?" + _7;
var tt5 = `<${Y5}>`;
var g5 = document;
var M5 = () => g5.createComment("");
var C8 = (o) => o === null || typeof o != "object" && typeof o != "function";
var q4 = Array.isArray;
var et6 = (o) => q4(o) || typeof o?.[Symbol.iterator] == "function";
var S5 = `[ 	
\f\r]`;
var N5 = /<(?:(!--|\/[^a-zA-Z])|(\/?[a-zA-Z][^>\s]*)|(\/?$))/g;
var k7 = /-->/g;
var W5 = />/g;
var v8 = RegExp(`>|${S5}(?:([^\\s"'>=/]+)(${S5}*=${S5}*(?:[^ 	
\f\r"'\`<>=]|("|')|))|$)`, "g");
var V4 = /'/g;
var Z4 = /"/g;
var G7 = /^(?:script|style|textarea|title)$/i;
var J7 = (o) => (t2, ...e3) => ({ _$litType$: o, strings: t2, values: e3 });
var ot5 = J7(1);
var rt4 = J7(2);
var m6 = Symbol.for("lit-noChange");
var u5 = Symbol.for("lit-nothing");
var z5 = /* @__PURE__ */ new WeakMap();
var p8 = g5.createTreeWalker(g5, 129, null, false);
var it4 = (o, t2) => {
  let e3 = o.length - 1, i3 = [], s2, r3 = t2 === 2 ? "<svg>" : "", n2 = N5;
  for (let l4 = 0; l4 < e3; l4++) {
    let h3 = o[l4], A5, a3, d5 = -1, c7 = 0;
    for (; c7 < h3.length && (n2.lastIndex = c7, a3 = n2.exec(h3), a3 !== null); )
      c7 = n2.lastIndex, n2 === N5 ? a3[1] === "!--" ? n2 = k7 : a3[1] !== void 0 ? n2 = W5 : a3[2] !== void 0 ? (G7.test(a3[2]) && (s2 = RegExp("</" + a3[2], "g")), n2 = v8) : a3[3] !== void 0 && (n2 = v8) : n2 === v8 ? a3[0] === ">" ? (n2 = s2 ?? N5, d5 = -1) : a3[1] === void 0 ? d5 = -2 : (d5 = n2.lastIndex - a3[2].length, A5 = a3[1], n2 = a3[3] === void 0 ? v8 : a3[3] === '"' ? Z4 : V4) : n2 === Z4 || n2 === V4 ? n2 = v8 : n2 === k7 || n2 === W5 ? n2 = N5 : (n2 = v8, s2 = void 0);
    let b6 = n2 === v8 && o[l4 + 1].startsWith("/>") ? " " : "";
    r3 += n2 === N5 ? h3 + tt5 : d5 >= 0 ? (i3.push(A5), h3.slice(0, d5) + B6 + h3.slice(d5) + _7 + b6) : h3 + _7 + (d5 === -2 ? (i3.push(void 0), l4) : b6);
  }
  let $4 = r3 + (o[e3] || "<?>") + (t2 === 2 ? "</svg>" : "");
  if (!Array.isArray(o) || !o.hasOwnProperty("raw"))
    throw Error("invalid template strings array");
  return [j7 !== void 0 ? j7.createHTML($4) : $4, i3];
};
var f4 = class {
  constructor({ strings: t2, _$litType$: e3 }, i3) {
    let s2;
    this.parts = [];
    let r3 = 0, n2 = 0, $4 = t2.length - 1, l4 = this.parts, [h3, A5] = it4(t2, e3);
    if (this.el = f4.createElement(h3, i3), p8.currentNode = this.el.content, e3 === 2) {
      let a3 = this.el.content, d5 = a3.firstChild;
      d5.remove(), a3.append(...d5.childNodes);
    }
    for (; (s2 = p8.nextNode()) !== null && l4.length < $4; ) {
      if (s2.nodeType === 1) {
        if (s2.hasAttributes()) {
          let a3 = [];
          for (let d5 of s2.getAttributeNames())
            if (d5.endsWith(B6) || d5.startsWith(_7)) {
              let c7 = A5[n2++];
              if (a3.push(d5), c7 !== void 0) {
                let b6 = s2.getAttribute(c7.toLowerCase() + B6).split(_7), E7 = /([.?@])?(.*)/.exec(c7);
                l4.push({ type: 1, index: r3, name: E7[2], strings: b6, ctor: E7[1] === "." ? R5 : E7[1] === "?" ? L5 : E7[1] === "@" ? D6 : T4 });
              } else
                l4.push({ type: 6, index: r3 });
            }
          for (let d5 of a3)
            s2.removeAttribute(d5);
        }
        if (G7.test(s2.tagName)) {
          let a3 = s2.textContent.split(_7), d5 = a3.length - 1;
          if (d5 > 0) {
            s2.textContent = y11 ? y11.emptyScript : "";
            for (let c7 = 0; c7 < d5; c7++)
              s2.append(a3[c7], M5()), p8.nextNode(), l4.push({ type: 2, index: ++r3 });
            s2.append(a3[d5], M5());
          }
        }
      } else if (s2.nodeType === 8)
        if (s2.data === Y5)
          l4.push({ type: 2, index: r3 });
        else {
          let a3 = -1;
          for (; (a3 = s2.data.indexOf(_7, a3 + 1)) !== -1; )
            l4.push({ type: 7, index: r3 }), a3 += _7.length - 1;
        }
      r3++;
    }
  }
  static createElement(t2, e3) {
    let i3 = g5.createElement("template");
    return i3.innerHTML = t2, i3;
  }
};
function H7(o, t2, e3 = o, i3) {
  var s2, r3, n2, $4;
  if (t2 === m6)
    return t2;
  let l4 = i3 !== void 0 ? (s2 = e3._$Co) === null || s2 === void 0 ? void 0 : s2[i3] : e3._$Cl, h3 = C8(t2) ? void 0 : t2._$litDirective$;
  return l4?.constructor !== h3 && ((r3 = l4?._$AO) === null || r3 === void 0 || r3.call(l4, false), h3 === void 0 ? l4 = void 0 : (l4 = new h3(o), l4._$AT(o, e3, i3)), i3 !== void 0 ? ((n2 = ($4 = e3)._$Co) !== null && n2 !== void 0 ? n2 : $4._$Co = [])[i3] = l4 : e3._$Cl = l4), l4 !== void 0 && (t2 = H7(o, l4._$AS(o, t2.values), l4, i3)), t2;
}
var U5 = class {
  constructor(t2, e3) {
    this._$AV = [], this._$AN = void 0, this._$AD = t2, this._$AM = e3;
  }
  get parentNode() {
    return this._$AM.parentNode;
  }
  get _$AU() {
    return this._$AM._$AU;
  }
  u(t2) {
    var e3;
    let { el: { content: i3 }, parts: s2 } = this._$AD, r3 = ((e3 = t2?.creationScope) !== null && e3 !== void 0 ? e3 : g5).importNode(i3, true);
    p8.currentNode = r3;
    let n2 = p8.nextNode(), $4 = 0, l4 = 0, h3 = s2[0];
    for (; h3 !== void 0; ) {
      if ($4 === h3.index) {
        let A5;
        h3.type === 2 ? A5 = new x5(n2, n2.nextSibling, this, t2) : h3.type === 1 ? A5 = new h3.ctor(n2, h3.name, h3.strings, this, t2) : h3.type === 6 && (A5 = new O5(n2, this, t2)), this._$AV.push(A5), h3 = s2[++l4];
      }
      $4 !== h3?.index && (n2 = p8.nextNode(), $4++);
    }
    return p8.currentNode = g5, r3;
  }
  v(t2) {
    let e3 = 0;
    for (let i3 of this._$AV)
      i3 !== void 0 && (i3.strings !== void 0 ? (i3._$AI(t2, i3, e3), e3 += i3.strings.length - 2) : i3._$AI(t2[e3])), e3++;
  }
};
var x5 = class {
  constructor(t2, e3, i3, s2) {
    var r3;
    this.type = 2, this._$AH = u5, this._$AN = void 0, this._$AA = t2, this._$AB = e3, this._$AM = i3, this.options = s2, this._$Cp = (r3 = s2?.isConnected) === null || r3 === void 0 || r3;
  }
  get _$AU() {
    var t2, e3;
    return (e3 = (t2 = this._$AM) === null || t2 === void 0 ? void 0 : t2._$AU) !== null && e3 !== void 0 ? e3 : this._$Cp;
  }
  get parentNode() {
    let t2 = this._$AA.parentNode, e3 = this._$AM;
    return e3 !== void 0 && t2?.nodeType === 11 && (t2 = e3.parentNode), t2;
  }
  get startNode() {
    return this._$AA;
  }
  get endNode() {
    return this._$AB;
  }
  _$AI(t2, e3 = this) {
    t2 = H7(this, t2, e3), C8(t2) ? t2 === u5 || t2 == null || t2 === "" ? (this._$AH !== u5 && this._$AR(), this._$AH = u5) : t2 !== this._$AH && t2 !== m6 && this._(t2) : t2._$litType$ !== void 0 ? this.g(t2) : t2.nodeType !== void 0 ? this.$(t2) : et6(t2) ? this.T(t2) : this._(t2);
  }
  k(t2) {
    return this._$AA.parentNode.insertBefore(t2, this._$AB);
  }
  $(t2) {
    this._$AH !== t2 && (this._$AR(), this._$AH = this.k(t2));
  }
  _(t2) {
    this._$AH !== u5 && C8(this._$AH) ? this._$AA.nextSibling.data = t2 : this.$(g5.createTextNode(t2)), this._$AH = t2;
  }
  g(t2) {
    var e3;
    let { values: i3, _$litType$: s2 } = t2, r3 = typeof s2 == "number" ? this._$AC(t2) : (s2.el === void 0 && (s2.el = f4.createElement(s2.h, this.options)), s2);
    if (((e3 = this._$AH) === null || e3 === void 0 ? void 0 : e3._$AD) === r3)
      this._$AH.v(i3);
    else {
      let n2 = new U5(r3, this), $4 = n2.u(this.options);
      n2.v(i3), this.$($4), this._$AH = n2;
    }
  }
  _$AC(t2) {
    let e3 = z5.get(t2.strings);
    return e3 === void 0 && z5.set(t2.strings, e3 = new f4(t2)), e3;
  }
  T(t2) {
    q4(this._$AH) || (this._$AH = [], this._$AR());
    let e3 = this._$AH, i3, s2 = 0;
    for (let r3 of t2)
      s2 === e3.length ? e3.push(i3 = new x5(this.k(M5()), this.k(M5()), this, this.options)) : i3 = e3[s2], i3._$AI(r3), s2++;
    s2 < e3.length && (this._$AR(i3 && i3._$AB.nextSibling, s2), e3.length = s2);
  }
  _$AR(t2 = this._$AA.nextSibling, e3) {
    var i3;
    for ((i3 = this._$AP) === null || i3 === void 0 || i3.call(this, false, true, e3); t2 && t2 !== this._$AB; ) {
      let s2 = t2.nextSibling;
      t2.remove(), t2 = s2;
    }
  }
  setConnected(t2) {
    var e3;
    this._$AM === void 0 && (this._$Cp = t2, (e3 = this._$AP) === null || e3 === void 0 || e3.call(this, t2));
  }
};
var T4 = class {
  constructor(t2, e3, i3, s2, r3) {
    this.type = 1, this._$AH = u5, this._$AN = void 0, this.element = t2, this.name = e3, this._$AM = s2, this.options = r3, i3.length > 2 || i3[0] !== "" || i3[1] !== "" ? (this._$AH = Array(i3.length - 1).fill(new String()), this.strings = i3) : this._$AH = u5;
  }
  get tagName() {
    return this.element.tagName;
  }
  get _$AU() {
    return this._$AM._$AU;
  }
  _$AI(t2, e3 = this, i3, s2) {
    let r3 = this.strings, n2 = false;
    if (r3 === void 0)
      t2 = H7(this, t2, e3, 0), n2 = !C8(t2) || t2 !== this._$AH && t2 !== m6, n2 && (this._$AH = t2);
    else {
      let $4 = t2, l4, h3;
      for (t2 = r3[0], l4 = 0; l4 < r3.length - 1; l4++)
        h3 = H7(this, $4[i3 + l4], e3, l4), h3 === m6 && (h3 = this._$AH[l4]), n2 || (n2 = !C8(h3) || h3 !== this._$AH[l4]), h3 === u5 ? t2 = u5 : t2 !== u5 && (t2 += (h3 ?? "") + r3[l4 + 1]), this._$AH[l4] = h3;
    }
    n2 && !s2 && this.j(t2);
  }
  j(t2) {
    t2 === u5 ? this.element.removeAttribute(this.name) : this.element.setAttribute(this.name, t2 ?? "");
  }
};
var R5 = class extends T4 {
  constructor() {
    super(...arguments), this.type = 3;
  }
  j(t2) {
    this.element[this.name] = t2 === u5 ? void 0 : t2;
  }
};
var st5 = y11 ? y11.emptyScript : "";
var L5 = class extends T4 {
  constructor() {
    super(...arguments), this.type = 4;
  }
  j(t2) {
    t2 && t2 !== u5 ? this.element.setAttribute(this.name, st5) : this.element.removeAttribute(this.name);
  }
};
var D6 = class extends T4 {
  constructor(t2, e3, i3, s2, r3) {
    super(t2, e3, i3, s2, r3), this.type = 5;
  }
  _$AI(t2, e3 = this) {
    var i3;
    if ((t2 = (i3 = H7(this, t2, e3, 0)) !== null && i3 !== void 0 ? i3 : u5) === m6)
      return;
    let s2 = this._$AH, r3 = t2 === u5 && s2 !== u5 || t2.capture !== s2.capture || t2.once !== s2.once || t2.passive !== s2.passive, n2 = t2 !== u5 && (s2 === u5 || r3);
    r3 && this.element.removeEventListener(this.name, this, s2), n2 && this.element.addEventListener(this.name, this, t2), this._$AH = t2;
  }
  handleEvent(t2) {
    var e3, i3;
    typeof this._$AH == "function" ? this._$AH.call((i3 = (e3 = this.options) === null || e3 === void 0 ? void 0 : e3.host) !== null && i3 !== void 0 ? i3 : this.element, t2) : this._$AH.handleEvent(t2);
  }
};
var O5 = class {
  constructor(t2, e3, i3) {
    this.element = t2, this.type = 6, this._$AN = void 0, this._$AM = e3, this.options = i3;
  }
  get _$AU() {
    return this._$AM._$AU;
  }
  _$AI(t2) {
    H7(this, t2);
  }
};
var F5 = w6.litHtmlPolyfillSupport;
F5?.(f4, x5), ((P5 = w6.litHtmlVersions) !== null && P5 !== void 0 ? P5 : w6.litHtmlVersions = []).push("2.7.4");
var K6 = { ATTRIBUTE: 1, CHILD: 2, PROPERTY: 3, BOOLEAN_ATTRIBUTE: 4, EVENT: 5, ELEMENT: 6 };
var Q5 = (o) => (...t2) => ({ _$litDirective$: o, values: t2 });
var I4 = class {
  constructor(t2) {
  }
  get _$AU() {
    return this._$AM._$AU;
  }
  _$AT(t2, e3, i3) {
    this._$Ct = t2, this._$AM = e3, this._$Ci = i3;
  }
  _$AS(t2, e3) {
    return this.update(t2, e3);
  }
  update(t2, e3) {
    return this.render(...e3);
  }
};
var X5 = "important";
var nt5 = " !" + X5;
var ut3 = Q5(class extends I4 {
  constructor(o) {
    var t2;
    if (super(o), o.type !== K6.ATTRIBUTE || o.name !== "style" || ((t2 = o.strings) === null || t2 === void 0 ? void 0 : t2.length) > 2)
      throw Error("The `styleMap` directive must be used in the `style` attribute and must be the only part in the attribute.");
  }
  render(o) {
    return Object.keys(o).reduce((t2, e3) => {
      let i3 = o[e3];
      return i3 == null ? t2 : t2 + `${e3 = e3.includes("-") ? e3 : e3.replace(/(?:^(webkit|moz|ms|o)|)(?=[A-Z])/g, "-$&").toLowerCase()}:${i3};`;
    }, "");
  }
  update(o, [t2]) {
    let { style: e3 } = o.element;
    if (this.ut === void 0) {
      this.ut = /* @__PURE__ */ new Set();
      for (let i3 in t2)
        this.ut.add(i3);
      return this.render(t2);
    }
    this.ut.forEach((i3) => {
      t2[i3] == null && (this.ut.delete(i3), i3.includes("-") ? e3.removeProperty(i3) : e3[i3] = "");
    });
    for (let i3 in t2) {
      let s2 = t2[i3];
      if (s2 != null) {
        this.ut.add(i3);
        let r3 = typeof s2 == "string" && s2.endsWith(nt5);
        i3.includes("-") || r3 ? e3.setProperty(i3, r3 ? s2.slice(0, -11) : s2, r3 ? X5 : "") : e3[i3] = s2;
      }
    }
    return m6;
  }
});

// src/emulator/multi-webview-comp-navigator-bar.html.ts
var TAG5 = "multi-webview-comp-navigation-bar";
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
MultiWebviewCompNavigationBar.styles = createAllCSS5();
__decorateClass([
  y6({ type: String })
], MultiWebviewCompNavigationBar.prototype, "_color", 2);
__decorateClass([
  y6({ type: String })
], MultiWebviewCompNavigationBar.prototype, "_style", 2);
__decorateClass([
  y6({ type: Boolean })
], MultiWebviewCompNavigationBar.prototype, "_overlay", 2);
__decorateClass([
  y6({ type: Boolean })
], MultiWebviewCompNavigationBar.prototype, "_visible", 2);
__decorateClass([
  y6({ type: Object })
], MultiWebviewCompNavigationBar.prototype, "_insets", 2);
MultiWebviewCompNavigationBar = __decorateClass([
  c4(TAG5)
], MultiWebviewCompNavigationBar);
function createAllCSS5() {
  return [
    C6`
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
var TAG6 = "multi-webview-comp-share";
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
        ${t(
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
MultiWebviewCompShare.styles = createAllCSS6();
__decorateClass([
  y6({ type: String })
], MultiWebviewCompShare.prototype, "_title", 2);
__decorateClass([
  y6({ type: String })
], MultiWebviewCompShare.prototype, "_text", 2);
__decorateClass([
  y6({ type: String })
], MultiWebviewCompShare.prototype, "_link", 2);
__decorateClass([
  y6({ type: String })
], MultiWebviewCompShare.prototype, "_src", 2);
__decorateClass([
  y6({ type: String })
], MultiWebviewCompShare.prototype, "_filename", 2);
MultiWebviewCompShare = __decorateClass([
  c4(TAG6)
], MultiWebviewCompShare);
function createAllCSS6() {
  return [
    C6`
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

// https://esm.sh/v124/lit-html@2.7.4/deno/directives/class-map.js
var I5;
var w7 = window;
var y12 = w7.trustedTypes;
var O6 = y12 ? y12.createPolicy("lit-html", { createHTML: (r3) => r3 }) : void 0;
var U6 = "$lit$";
var v9 = `lit$${(Math.random() + "").slice(9)}$`;
var Y6 = "?" + v9;
var X6 = `<${Y6}>`;
var g6 = document;
var M6 = () => g6.createComment("");
var C9 = (r3) => r3 === null || typeof r3 != "object" && typeof r3 != "function";
var q5 = Array.isArray;
var tt6 = (r3) => q5(r3) || typeof r3?.[Symbol.iterator] == "function";
var B7 = `[ 	
\f\r]`;
var N6 = /<(?:(!--|\/[^a-zA-Z])|(\/?[a-zA-Z][^>\s]*)|(\/?$))/g;
var k8 = /-->/g;
var V5 = />/g;
var _8 = RegExp(`>|${B7}(?:([^\\s"'>=/]+)(${B7}*=${B7}*(?:[^ 	
\f\r"'\`<>=]|("|')|))|$)`, "g");
var W6 = /'/g;
var Z5 = /"/g;
var G8 = /^(?:script|style|textarea|title)$/i;
var J8 = (r3) => (t2, ...e3) => ({ _$litType$: r3, strings: t2, values: e3 });
var st6 = J8(1);
var nt6 = J8(2);
var m7 = Symbol.for("lit-noChange");
var $3 = Symbol.for("lit-nothing");
var z6 = /* @__PURE__ */ new WeakMap();
var p9 = g6.createTreeWalker(g6, 129, null, false);
var et7 = (r3, t2) => {
  let e3 = r3.length - 1, i3 = [], s2, n2 = t2 === 2 ? "<svg>" : "", o = N6;
  for (let l4 = 0; l4 < e3; l4++) {
    let h3 = r3[l4], A5, a3, d5 = -1, u6 = 0;
    for (; u6 < h3.length && (o.lastIndex = u6, a3 = o.exec(h3), a3 !== null); )
      u6 = o.lastIndex, o === N6 ? a3[1] === "!--" ? o = k8 : a3[1] !== void 0 ? o = V5 : a3[2] !== void 0 ? (G8.test(a3[2]) && (s2 = RegExp("</" + a3[2], "g")), o = _8) : a3[3] !== void 0 && (o = _8) : o === _8 ? a3[0] === ">" ? (o = s2 ?? N6, d5 = -1) : a3[1] === void 0 ? d5 = -2 : (d5 = o.lastIndex - a3[2].length, A5 = a3[1], o = a3[3] === void 0 ? _8 : a3[3] === '"' ? Z5 : W6) : o === Z5 || o === W6 ? o = _8 : o === k8 || o === V5 ? o = N6 : (o = _8, s2 = void 0);
    let b6 = o === _8 && r3[l4 + 1].startsWith("/>") ? " " : "";
    n2 += o === N6 ? h3 + X6 : d5 >= 0 ? (i3.push(A5), h3.slice(0, d5) + U6 + h3.slice(d5) + v9 + b6) : h3 + v9 + (d5 === -2 ? (i3.push(void 0), l4) : b6);
  }
  let c7 = n2 + (r3[e3] || "<?>") + (t2 === 2 ? "</svg>" : "");
  if (!Array.isArray(r3) || !r3.hasOwnProperty("raw"))
    throw Error("invalid template strings array");
  return [O6 !== void 0 ? O6.createHTML(c7) : c7, i3];
};
var f5 = class {
  constructor({ strings: t2, _$litType$: e3 }, i3) {
    let s2;
    this.parts = [];
    let n2 = 0, o = 0, c7 = t2.length - 1, l4 = this.parts, [h3, A5] = et7(t2, e3);
    if (this.el = f5.createElement(h3, i3), p9.currentNode = this.el.content, e3 === 2) {
      let a3 = this.el.content, d5 = a3.firstChild;
      d5.remove(), a3.append(...d5.childNodes);
    }
    for (; (s2 = p9.nextNode()) !== null && l4.length < c7; ) {
      if (s2.nodeType === 1) {
        if (s2.hasAttributes()) {
          let a3 = [];
          for (let d5 of s2.getAttributeNames())
            if (d5.endsWith(U6) || d5.startsWith(v9)) {
              let u6 = A5[o++];
              if (a3.push(d5), u6 !== void 0) {
                let b6 = s2.getAttribute(u6.toLowerCase() + U6).split(v9), E7 = /([.?@])?(.*)/.exec(u6);
                l4.push({ type: 1, index: n2, name: E7[2], strings: b6, ctor: E7[1] === "." ? R6 : E7[1] === "?" ? L6 : E7[1] === "@" ? j8 : T5 });
              } else
                l4.push({ type: 6, index: n2 });
            }
          for (let d5 of a3)
            s2.removeAttribute(d5);
        }
        if (G8.test(s2.tagName)) {
          let a3 = s2.textContent.split(v9), d5 = a3.length - 1;
          if (d5 > 0) {
            s2.textContent = y12 ? y12.emptyScript : "";
            for (let u6 = 0; u6 < d5; u6++)
              s2.append(a3[u6], M6()), p9.nextNode(), l4.push({ type: 2, index: ++n2 });
            s2.append(a3[d5], M6());
          }
        }
      } else if (s2.nodeType === 8)
        if (s2.data === Y6)
          l4.push({ type: 2, index: n2 });
        else {
          let a3 = -1;
          for (; (a3 = s2.data.indexOf(v9, a3 + 1)) !== -1; )
            l4.push({ type: 7, index: n2 }), a3 += v9.length - 1;
        }
      n2++;
    }
  }
  static createElement(t2, e3) {
    let i3 = g6.createElement("template");
    return i3.innerHTML = t2, i3;
  }
};
function H8(r3, t2, e3 = r3, i3) {
  var s2, n2, o, c7;
  if (t2 === m7)
    return t2;
  let l4 = i3 !== void 0 ? (s2 = e3._$Co) === null || s2 === void 0 ? void 0 : s2[i3] : e3._$Cl, h3 = C9(t2) ? void 0 : t2._$litDirective$;
  return l4?.constructor !== h3 && ((n2 = l4?._$AO) === null || n2 === void 0 || n2.call(l4, false), h3 === void 0 ? l4 = void 0 : (l4 = new h3(r3), l4._$AT(r3, e3, i3)), i3 !== void 0 ? ((o = (c7 = e3)._$Co) !== null && o !== void 0 ? o : c7._$Co = [])[i3] = l4 : e3._$Cl = l4), l4 !== void 0 && (t2 = H8(r3, l4._$AS(r3, t2.values), l4, i3)), t2;
}
var P6 = class {
  constructor(t2, e3) {
    this._$AV = [], this._$AN = void 0, this._$AD = t2, this._$AM = e3;
  }
  get parentNode() {
    return this._$AM.parentNode;
  }
  get _$AU() {
    return this._$AM._$AU;
  }
  u(t2) {
    var e3;
    let { el: { content: i3 }, parts: s2 } = this._$AD, n2 = ((e3 = t2?.creationScope) !== null && e3 !== void 0 ? e3 : g6).importNode(i3, true);
    p9.currentNode = n2;
    let o = p9.nextNode(), c7 = 0, l4 = 0, h3 = s2[0];
    for (; h3 !== void 0; ) {
      if (c7 === h3.index) {
        let A5;
        h3.type === 2 ? A5 = new x6(o, o.nextSibling, this, t2) : h3.type === 1 ? A5 = new h3.ctor(o, h3.name, h3.strings, this, t2) : h3.type === 6 && (A5 = new D7(o, this, t2)), this._$AV.push(A5), h3 = s2[++l4];
      }
      c7 !== h3?.index && (o = p9.nextNode(), c7++);
    }
    return p9.currentNode = g6, n2;
  }
  v(t2) {
    let e3 = 0;
    for (let i3 of this._$AV)
      i3 !== void 0 && (i3.strings !== void 0 ? (i3._$AI(t2, i3, e3), e3 += i3.strings.length - 2) : i3._$AI(t2[e3])), e3++;
  }
};
var x6 = class {
  constructor(t2, e3, i3, s2) {
    var n2;
    this.type = 2, this._$AH = $3, this._$AN = void 0, this._$AA = t2, this._$AB = e3, this._$AM = i3, this.options = s2, this._$Cp = (n2 = s2?.isConnected) === null || n2 === void 0 || n2;
  }
  get _$AU() {
    var t2, e3;
    return (e3 = (t2 = this._$AM) === null || t2 === void 0 ? void 0 : t2._$AU) !== null && e3 !== void 0 ? e3 : this._$Cp;
  }
  get parentNode() {
    let t2 = this._$AA.parentNode, e3 = this._$AM;
    return e3 !== void 0 && t2?.nodeType === 11 && (t2 = e3.parentNode), t2;
  }
  get startNode() {
    return this._$AA;
  }
  get endNode() {
    return this._$AB;
  }
  _$AI(t2, e3 = this) {
    t2 = H8(this, t2, e3), C9(t2) ? t2 === $3 || t2 == null || t2 === "" ? (this._$AH !== $3 && this._$AR(), this._$AH = $3) : t2 !== this._$AH && t2 !== m7 && this._(t2) : t2._$litType$ !== void 0 ? this.g(t2) : t2.nodeType !== void 0 ? this.$(t2) : tt6(t2) ? this.T(t2) : this._(t2);
  }
  k(t2) {
    return this._$AA.parentNode.insertBefore(t2, this._$AB);
  }
  $(t2) {
    this._$AH !== t2 && (this._$AR(), this._$AH = this.k(t2));
  }
  _(t2) {
    this._$AH !== $3 && C9(this._$AH) ? this._$AA.nextSibling.data = t2 : this.$(g6.createTextNode(t2)), this._$AH = t2;
  }
  g(t2) {
    var e3;
    let { values: i3, _$litType$: s2 } = t2, n2 = typeof s2 == "number" ? this._$AC(t2) : (s2.el === void 0 && (s2.el = f5.createElement(s2.h, this.options)), s2);
    if (((e3 = this._$AH) === null || e3 === void 0 ? void 0 : e3._$AD) === n2)
      this._$AH.v(i3);
    else {
      let o = new P6(n2, this), c7 = o.u(this.options);
      o.v(i3), this.$(c7), this._$AH = o;
    }
  }
  _$AC(t2) {
    let e3 = z6.get(t2.strings);
    return e3 === void 0 && z6.set(t2.strings, e3 = new f5(t2)), e3;
  }
  T(t2) {
    q5(this._$AH) || (this._$AH = [], this._$AR());
    let e3 = this._$AH, i3, s2 = 0;
    for (let n2 of t2)
      s2 === e3.length ? e3.push(i3 = new x6(this.k(M6()), this.k(M6()), this, this.options)) : i3 = e3[s2], i3._$AI(n2), s2++;
    s2 < e3.length && (this._$AR(i3 && i3._$AB.nextSibling, s2), e3.length = s2);
  }
  _$AR(t2 = this._$AA.nextSibling, e3) {
    var i3;
    for ((i3 = this._$AP) === null || i3 === void 0 || i3.call(this, false, true, e3); t2 && t2 !== this._$AB; ) {
      let s2 = t2.nextSibling;
      t2.remove(), t2 = s2;
    }
  }
  setConnected(t2) {
    var e3;
    this._$AM === void 0 && (this._$Cp = t2, (e3 = this._$AP) === null || e3 === void 0 || e3.call(this, t2));
  }
};
var T5 = class {
  constructor(t2, e3, i3, s2, n2) {
    this.type = 1, this._$AH = $3, this._$AN = void 0, this.element = t2, this.name = e3, this._$AM = s2, this.options = n2, i3.length > 2 || i3[0] !== "" || i3[1] !== "" ? (this._$AH = Array(i3.length - 1).fill(new String()), this.strings = i3) : this._$AH = $3;
  }
  get tagName() {
    return this.element.tagName;
  }
  get _$AU() {
    return this._$AM._$AU;
  }
  _$AI(t2, e3 = this, i3, s2) {
    let n2 = this.strings, o = false;
    if (n2 === void 0)
      t2 = H8(this, t2, e3, 0), o = !C9(t2) || t2 !== this._$AH && t2 !== m7, o && (this._$AH = t2);
    else {
      let c7 = t2, l4, h3;
      for (t2 = n2[0], l4 = 0; l4 < n2.length - 1; l4++)
        h3 = H8(this, c7[i3 + l4], e3, l4), h3 === m7 && (h3 = this._$AH[l4]), o || (o = !C9(h3) || h3 !== this._$AH[l4]), h3 === $3 ? t2 = $3 : t2 !== $3 && (t2 += (h3 ?? "") + n2[l4 + 1]), this._$AH[l4] = h3;
    }
    o && !s2 && this.j(t2);
  }
  j(t2) {
    t2 === $3 ? this.element.removeAttribute(this.name) : this.element.setAttribute(this.name, t2 ?? "");
  }
};
var R6 = class extends T5 {
  constructor() {
    super(...arguments), this.type = 3;
  }
  j(t2) {
    this.element[this.name] = t2 === $3 ? void 0 : t2;
  }
};
var it5 = y12 ? y12.emptyScript : "";
var L6 = class extends T5 {
  constructor() {
    super(...arguments), this.type = 4;
  }
  j(t2) {
    t2 && t2 !== $3 ? this.element.setAttribute(this.name, it5) : this.element.removeAttribute(this.name);
  }
};
var j8 = class extends T5 {
  constructor(t2, e3, i3, s2, n2) {
    super(t2, e3, i3, s2, n2), this.type = 5;
  }
  _$AI(t2, e3 = this) {
    var i3;
    if ((t2 = (i3 = H8(this, t2, e3, 0)) !== null && i3 !== void 0 ? i3 : $3) === m7)
      return;
    let s2 = this._$AH, n2 = t2 === $3 && s2 !== $3 || t2.capture !== s2.capture || t2.once !== s2.once || t2.passive !== s2.passive, o = t2 !== $3 && (s2 === $3 || n2);
    n2 && this.element.removeEventListener(this.name, this, s2), o && this.element.addEventListener(this.name, this, t2), this._$AH = t2;
  }
  handleEvent(t2) {
    var e3, i3;
    typeof this._$AH == "function" ? this._$AH.call((i3 = (e3 = this.options) === null || e3 === void 0 ? void 0 : e3.host) !== null && i3 !== void 0 ? i3 : this.element, t2) : this._$AH.handleEvent(t2);
  }
};
var D7 = class {
  constructor(t2, e3, i3) {
    this.element = t2, this.type = 6, this._$AN = void 0, this._$AM = e3, this.options = i3;
  }
  get _$AU() {
    return this._$AM._$AU;
  }
  _$AI(t2) {
    H8(this, t2);
  }
};
var F6 = w7.litHtmlPolyfillSupport;
F6?.(f5, x6), ((I5 = w7.litHtmlVersions) !== null && I5 !== void 0 ? I5 : w7.litHtmlVersions = []).push("2.7.4");
var K7 = { ATTRIBUTE: 1, CHILD: 2, PROPERTY: 3, BOOLEAN_ATTRIBUTE: 4, EVENT: 5, ELEMENT: 6 };
var Q6 = (r3) => (...t2) => ({ _$litDirective$: r3, values: t2 });
var S6 = class {
  constructor(t2) {
  }
  get _$AU() {
    return this._$AM._$AU;
  }
  _$AT(t2, e3, i3) {
    this._$Ct = t2, this._$AM = e3, this._$Ci = i3;
  }
  _$AS(t2, e3) {
    return this.update(t2, e3);
  }
  update(t2, e3) {
    return this.render(...e3);
  }
};
var at3 = Q6(class extends S6 {
  constructor(r3) {
    var t2;
    if (super(r3), r3.type !== K7.ATTRIBUTE || r3.name !== "class" || ((t2 = r3.strings) === null || t2 === void 0 ? void 0 : t2.length) > 2)
      throw Error("`classMap()` can only be used in the `class` attribute and must be the only part in the attribute.");
  }
  render(r3) {
    return " " + Object.keys(r3).filter((t2) => r3[t2]).join(" ") + " ";
  }
  update(r3, [t2]) {
    var e3, i3;
    if (this.it === void 0) {
      this.it = /* @__PURE__ */ new Set(), r3.strings !== void 0 && (this.nt = new Set(r3.strings.join(" ").split(/\s/).filter((n2) => n2 !== "")));
      for (let n2 in t2)
        t2[n2] && !(!((e3 = this.nt) === null || e3 === void 0) && e3.has(n2)) && this.it.add(n2);
      return this.render(t2);
    }
    let s2 = r3.element.classList;
    this.it.forEach((n2) => {
      n2 in t2 || (s2.remove(n2), this.it.delete(n2));
    });
    for (let n2 in t2) {
      let o = !!t2[n2];
      o === this.it.has(n2) || !((i3 = this.nt) === null || i3 === void 0) && i3.has(n2) || (o ? (s2.add(n2), this.it.add(n2)) : (s2.remove(n2), this.it.delete(n2)));
    }
    return m7;
  }
});

// src/emulator/multi-webview-comp-status-bar.html.ts
var TAG7 = "multi-webview-comp-status-bar";
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
          ${t(
      this._visible,
      () => tt3`<div class="left_container">10:00</div>`
    )}
          <div class="center_container">
            ${t(
      this._torchIsOpen,
      () => tt3`<div class="torch_symbol"></div>`
    )}
          </div>
          ${t(
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
MultiWebviewCompStatusBar.styles = createAllCSS7();
__decorateClass([
  y6({ type: String })
], MultiWebviewCompStatusBar.prototype, "_color", 2);
__decorateClass([
  y6({ type: String })
], MultiWebviewCompStatusBar.prototype, "_style", 2);
__decorateClass([
  y6({ type: Boolean })
], MultiWebviewCompStatusBar.prototype, "_overlay", 2);
__decorateClass([
  y6({ type: Boolean })
], MultiWebviewCompStatusBar.prototype, "_visible", 2);
__decorateClass([
  y6({ type: Object })
], MultiWebviewCompStatusBar.prototype, "_insets", 2);
__decorateClass([
  y6({ type: Boolean })
], MultiWebviewCompStatusBar.prototype, "_torchIsOpen", 2);
MultiWebviewCompStatusBar = __decorateClass([
  c4(TAG7)
], MultiWebviewCompStatusBar);
function createAllCSS7() {
  return [
    C6`
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
var TAG8 = "multi-webview-comp-toast";
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
MultiWebviewCompToast.styles = createAllCSS8();
MultiWebviewCompToast.properties = {
  _beforeEntry: { state: true }
};
__decorateClass([
  y6({ type: String })
], MultiWebviewCompToast.prototype, "_message", 2);
__decorateClass([
  y6({ type: String })
], MultiWebviewCompToast.prototype, "_duration", 2);
__decorateClass([
  y6({ type: String })
], MultiWebviewCompToast.prototype, "_position", 2);
__decorateClass([
  y7()
], MultiWebviewCompToast.prototype, "_beforeEntry", 2);
MultiWebviewCompToast = __decorateClass([
  c4(TAG8)
], MultiWebviewCompToast);
function createAllCSS8() {
  return [
    C6`
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

// https://esm.sh/v124/lit-html@2.7.4/deno/directives/repeat.js
var W7;
var S7 = window;
var P7 = S7.trustedTypes;
var G9 = P7 ? P7.createPolicy("lit-html", { createHTML: (n2) => n2 }) : void 0;
var B8 = "$lit$";
var f6 = `lit$${(Math.random() + "").slice(9)}$`;
var z7 = "?" + f6;
var ut4 = `<${z7}>`;
var x7 = document;
var I6 = () => x7.createComment("");
var M7 = (n2) => n2 === null || typeof n2 != "object" && typeof n2 != "function";
var tt7 = Array.isArray;
var et8 = (n2) => tt7(n2) || typeof n2?.[Symbol.iterator] == "function";
var Z6 = `[ 	
\f\r]`;
var w8 = /<(?:(!--|\/[^a-zA-Z])|(\/?[a-zA-Z][^>\s]*)|(\/?$))/g;
var Q7 = /-->/g;
var Y7 = />/g;
var g7 = RegExp(`>|${Z6}(?:([^\\s"'>=/]+)(${Z6}*=${Z6}*(?:[^ 	
\f\r"'\`<>=]|("|')|))|$)`, "g");
var q6 = /'/g;
var J9 = /"/g;
var it6 = /^(?:script|style|textarea|title)$/i;
var st7 = (n2) => (t2, ...e3) => ({ _$litType$: n2, strings: t2, values: e3 });
var _t3 = st7(1);
var pt3 = st7(2);
var H9 = Symbol.for("lit-noChange");
var v10 = Symbol.for("lit-nothing");
var K8 = /* @__PURE__ */ new WeakMap();
var y13 = x7.createTreeWalker(x7, 129, null, false);
var nt7 = (n2, t2) => {
  let e3 = n2.length - 1, i3 = [], s2, r3 = t2 === 2 ? "<svg>" : "", o = w8;
  for (let h3 = 0; h3 < e3; h3++) {
    let l4 = n2[h3], _9, $4, a3 = -1, u6 = 0;
    for (; u6 < l4.length && (o.lastIndex = u6, $4 = o.exec(l4), $4 !== null); )
      u6 = o.lastIndex, o === w8 ? $4[1] === "!--" ? o = Q7 : $4[1] !== void 0 ? o = Y7 : $4[2] !== void 0 ? (it6.test($4[2]) && (s2 = RegExp("</" + $4[2], "g")), o = g7) : $4[3] !== void 0 && (o = g7) : o === g7 ? $4[0] === ">" ? (o = s2 ?? w8, a3 = -1) : $4[1] === void 0 ? a3 = -2 : (a3 = o.lastIndex - $4[2].length, _9 = $4[1], o = $4[3] === void 0 ? g7 : $4[3] === '"' ? J9 : q6) : o === J9 || o === q6 ? o = g7 : o === Q7 || o === Y7 ? o = w8 : (o = g7, s2 = void 0);
    let c7 = o === g7 && n2[h3 + 1].startsWith("/>") ? " " : "";
    r3 += o === w8 ? l4 + ut4 : a3 >= 0 ? (i3.push(_9), l4.slice(0, a3) + B8 + l4.slice(a3) + f6 + c7) : l4 + f6 + (a3 === -2 ? (i3.push(void 0), h3) : c7);
  }
  let d5 = r3 + (n2[e3] || "<?>") + (t2 === 2 ? "</svg>" : "");
  if (!Array.isArray(n2) || !n2.hasOwnProperty("raw"))
    throw Error("invalid template strings array");
  return [G9 !== void 0 ? G9.createHTML(d5) : d5, i3];
};
var T6 = class {
  constructor({ strings: t2, _$litType$: e3 }, i3) {
    let s2;
    this.parts = [];
    let r3 = 0, o = 0, d5 = t2.length - 1, h3 = this.parts, [l4, _9] = nt7(t2, e3);
    if (this.el = T6.createElement(l4, i3), y13.currentNode = this.el.content, e3 === 2) {
      let $4 = this.el.content, a3 = $4.firstChild;
      a3.remove(), $4.append(...a3.childNodes);
    }
    for (; (s2 = y13.nextNode()) !== null && h3.length < d5; ) {
      if (s2.nodeType === 1) {
        if (s2.hasAttributes()) {
          let $4 = [];
          for (let a3 of s2.getAttributeNames())
            if (a3.endsWith(B8) || a3.startsWith(f6)) {
              let u6 = _9[o++];
              if ($4.push(a3), u6 !== void 0) {
                let c7 = s2.getAttribute(u6.toLowerCase() + B8).split(f6), A5 = /([.?@])?(.*)/.exec(u6);
                h3.push({ type: 1, index: r3, name: A5[2], strings: c7, ctor: A5[1] === "." ? U7 : A5[1] === "?" ? D8 : A5[1] === "@" ? L7 : b5 });
              } else
                h3.push({ type: 6, index: r3 });
            }
          for (let a3 of $4)
            s2.removeAttribute(a3);
        }
        if (it6.test(s2.tagName)) {
          let $4 = s2.textContent.split(f6), a3 = $4.length - 1;
          if (a3 > 0) {
            s2.textContent = P7 ? P7.emptyScript : "";
            for (let u6 = 0; u6 < a3; u6++)
              s2.append($4[u6], I6()), y13.nextNode(), h3.push({ type: 2, index: ++r3 });
            s2.append($4[a3], I6());
          }
        }
      } else if (s2.nodeType === 8)
        if (s2.data === z7)
          h3.push({ type: 2, index: r3 });
        else {
          let $4 = -1;
          for (; ($4 = s2.data.indexOf(f6, $4 + 1)) !== -1; )
            h3.push({ type: 7, index: r3 }), $4 += f6.length - 1;
        }
      r3++;
    }
  }
  static createElement(t2, e3) {
    let i3 = x7.createElement("template");
    return i3.innerHTML = t2, i3;
  }
};
function C10(n2, t2, e3 = n2, i3) {
  var s2, r3, o, d5;
  if (t2 === H9)
    return t2;
  let h3 = i3 !== void 0 ? (s2 = e3._$Co) === null || s2 === void 0 ? void 0 : s2[i3] : e3._$Cl, l4 = M7(t2) ? void 0 : t2._$litDirective$;
  return h3?.constructor !== l4 && ((r3 = h3?._$AO) === null || r3 === void 0 || r3.call(h3, false), l4 === void 0 ? h3 = void 0 : (h3 = new l4(n2), h3._$AT(n2, e3, i3)), i3 !== void 0 ? ((o = (d5 = e3)._$Co) !== null && o !== void 0 ? o : d5._$Co = [])[i3] = h3 : e3._$Cl = h3), h3 !== void 0 && (t2 = C10(n2, h3._$AS(n2, t2.values), h3, i3)), t2;
}
var R7 = class {
  constructor(t2, e3) {
    this._$AV = [], this._$AN = void 0, this._$AD = t2, this._$AM = e3;
  }
  get parentNode() {
    return this._$AM.parentNode;
  }
  get _$AU() {
    return this._$AM._$AU;
  }
  u(t2) {
    var e3;
    let { el: { content: i3 }, parts: s2 } = this._$AD, r3 = ((e3 = t2?.creationScope) !== null && e3 !== void 0 ? e3 : x7).importNode(i3, true);
    y13.currentNode = r3;
    let o = y13.nextNode(), d5 = 0, h3 = 0, l4 = s2[0];
    for (; l4 !== void 0; ) {
      if (d5 === l4.index) {
        let _9;
        l4.type === 2 ? _9 = new N7(o, o.nextSibling, this, t2) : l4.type === 1 ? _9 = new l4.ctor(o, l4.name, l4.strings, this, t2) : l4.type === 6 && (_9 = new V6(o, this, t2)), this._$AV.push(_9), l4 = s2[++h3];
      }
      d5 !== l4?.index && (o = y13.nextNode(), d5++);
    }
    return y13.currentNode = x7, r3;
  }
  v(t2) {
    let e3 = 0;
    for (let i3 of this._$AV)
      i3 !== void 0 && (i3.strings !== void 0 ? (i3._$AI(t2, i3, e3), e3 += i3.strings.length - 2) : i3._$AI(t2[e3])), e3++;
  }
};
var N7 = class {
  constructor(t2, e3, i3, s2) {
    var r3;
    this.type = 2, this._$AH = v10, this._$AN = void 0, this._$AA = t2, this._$AB = e3, this._$AM = i3, this.options = s2, this._$Cp = (r3 = s2?.isConnected) === null || r3 === void 0 || r3;
  }
  get _$AU() {
    var t2, e3;
    return (e3 = (t2 = this._$AM) === null || t2 === void 0 ? void 0 : t2._$AU) !== null && e3 !== void 0 ? e3 : this._$Cp;
  }
  get parentNode() {
    let t2 = this._$AA.parentNode, e3 = this._$AM;
    return e3 !== void 0 && t2?.nodeType === 11 && (t2 = e3.parentNode), t2;
  }
  get startNode() {
    return this._$AA;
  }
  get endNode() {
    return this._$AB;
  }
  _$AI(t2, e3 = this) {
    t2 = C10(this, t2, e3), M7(t2) ? t2 === v10 || t2 == null || t2 === "" ? (this._$AH !== v10 && this._$AR(), this._$AH = v10) : t2 !== this._$AH && t2 !== H9 && this._(t2) : t2._$litType$ !== void 0 ? this.g(t2) : t2.nodeType !== void 0 ? this.$(t2) : et8(t2) ? this.T(t2) : this._(t2);
  }
  k(t2) {
    return this._$AA.parentNode.insertBefore(t2, this._$AB);
  }
  $(t2) {
    this._$AH !== t2 && (this._$AR(), this._$AH = this.k(t2));
  }
  _(t2) {
    this._$AH !== v10 && M7(this._$AH) ? this._$AA.nextSibling.data = t2 : this.$(x7.createTextNode(t2)), this._$AH = t2;
  }
  g(t2) {
    var e3;
    let { values: i3, _$litType$: s2 } = t2, r3 = typeof s2 == "number" ? this._$AC(t2) : (s2.el === void 0 && (s2.el = T6.createElement(s2.h, this.options)), s2);
    if (((e3 = this._$AH) === null || e3 === void 0 ? void 0 : e3._$AD) === r3)
      this._$AH.v(i3);
    else {
      let o = new R7(r3, this), d5 = o.u(this.options);
      o.v(i3), this.$(d5), this._$AH = o;
    }
  }
  _$AC(t2) {
    let e3 = K8.get(t2.strings);
    return e3 === void 0 && K8.set(t2.strings, e3 = new T6(t2)), e3;
  }
  T(t2) {
    tt7(this._$AH) || (this._$AH = [], this._$AR());
    let e3 = this._$AH, i3, s2 = 0;
    for (let r3 of t2)
      s2 === e3.length ? e3.push(i3 = new N7(this.k(I6()), this.k(I6()), this, this.options)) : i3 = e3[s2], i3._$AI(r3), s2++;
    s2 < e3.length && (this._$AR(i3 && i3._$AB.nextSibling, s2), e3.length = s2);
  }
  _$AR(t2 = this._$AA.nextSibling, e3) {
    var i3;
    for ((i3 = this._$AP) === null || i3 === void 0 || i3.call(this, false, true, e3); t2 && t2 !== this._$AB; ) {
      let s2 = t2.nextSibling;
      t2.remove(), t2 = s2;
    }
  }
  setConnected(t2) {
    var e3;
    this._$AM === void 0 && (this._$Cp = t2, (e3 = this._$AP) === null || e3 === void 0 || e3.call(this, t2));
  }
};
var b5 = class {
  constructor(t2, e3, i3, s2, r3) {
    this.type = 1, this._$AH = v10, this._$AN = void 0, this.element = t2, this.name = e3, this._$AM = s2, this.options = r3, i3.length > 2 || i3[0] !== "" || i3[1] !== "" ? (this._$AH = Array(i3.length - 1).fill(new String()), this.strings = i3) : this._$AH = v10;
  }
  get tagName() {
    return this.element.tagName;
  }
  get _$AU() {
    return this._$AM._$AU;
  }
  _$AI(t2, e3 = this, i3, s2) {
    let r3 = this.strings, o = false;
    if (r3 === void 0)
      t2 = C10(this, t2, e3, 0), o = !M7(t2) || t2 !== this._$AH && t2 !== H9, o && (this._$AH = t2);
    else {
      let d5 = t2, h3, l4;
      for (t2 = r3[0], h3 = 0; h3 < r3.length - 1; h3++)
        l4 = C10(this, d5[i3 + h3], e3, h3), l4 === H9 && (l4 = this._$AH[h3]), o || (o = !M7(l4) || l4 !== this._$AH[h3]), l4 === v10 ? t2 = v10 : t2 !== v10 && (t2 += (l4 ?? "") + r3[h3 + 1]), this._$AH[h3] = l4;
    }
    o && !s2 && this.j(t2);
  }
  j(t2) {
    t2 === v10 ? this.element.removeAttribute(this.name) : this.element.setAttribute(this.name, t2 ?? "");
  }
};
var U7 = class extends b5 {
  constructor() {
    super(...arguments), this.type = 3;
  }
  j(t2) {
    this.element[this.name] = t2 === v10 ? void 0 : t2;
  }
};
var ct3 = P7 ? P7.emptyScript : "";
var D8 = class extends b5 {
  constructor() {
    super(...arguments), this.type = 4;
  }
  j(t2) {
    t2 && t2 !== v10 ? this.element.setAttribute(this.name, ct3) : this.element.removeAttribute(this.name);
  }
};
var L7 = class extends b5 {
  constructor(t2, e3, i3, s2, r3) {
    super(t2, e3, i3, s2, r3), this.type = 5;
  }
  _$AI(t2, e3 = this) {
    var i3;
    if ((t2 = (i3 = C10(this, t2, e3, 0)) !== null && i3 !== void 0 ? i3 : v10) === H9)
      return;
    let s2 = this._$AH, r3 = t2 === v10 && s2 !== v10 || t2.capture !== s2.capture || t2.once !== s2.once || t2.passive !== s2.passive, o = t2 !== v10 && (s2 === v10 || r3);
    r3 && this.element.removeEventListener(this.name, this, s2), o && this.element.addEventListener(this.name, this, t2), this._$AH = t2;
  }
  handleEvent(t2) {
    var e3, i3;
    typeof this._$AH == "function" ? this._$AH.call((i3 = (e3 = this.options) === null || e3 === void 0 ? void 0 : e3.host) !== null && i3 !== void 0 ? i3 : this.element, t2) : this._$AH.handleEvent(t2);
  }
};
var V6 = class {
  constructor(t2, e3, i3) {
    this.element = t2, this.type = 6, this._$AN = void 0, this._$AM = e3, this.options = i3;
  }
  get _$AU() {
    return this._$AM._$AU;
  }
  _$AI(t2) {
    C10(this, t2);
  }
};
var ot6 = { O: B8, P: f6, A: z7, C: 1, M: nt7, L: R7, D: et8, R: C10, I: N7, V: b5, H: D8, N: L7, U: U7, F: V6 };
var X7 = S7.litHtmlPolyfillSupport;
X7?.(T6, N7), ((W7 = S7.litHtmlVersions) !== null && W7 !== void 0 ? W7 : S7.litHtmlVersions = []).push("2.7.4");
var rt5 = { ATTRIBUTE: 1, CHILD: 2, PROPERTY: 3, BOOLEAN_ATTRIBUTE: 4, EVENT: 5, ELEMENT: 6 };
var lt3 = (n2) => (...t2) => ({ _$litDirective$: n2, values: t2 });
var j9 = class {
  constructor(t2) {
  }
  get _$AU() {
    return this._$AM._$AU;
  }
  _$AT(t2, e3, i3) {
    this._$Ct = t2, this._$AM = e3, this._$Ci = i3;
  }
  _$AS(t2, e3) {
    return this.update(t2, e3);
  }
  update(t2, e3) {
    return this.render(...e3);
  }
};
var { I: At2 } = ot6;
var ht3 = () => document.createComment("");
var E6 = (n2, t2, e3) => {
  var i3;
  let s2 = n2._$AA.parentNode, r3 = t2 === void 0 ? n2._$AB : t2._$AA;
  if (e3 === void 0) {
    let o = s2.insertBefore(ht3(), r3), d5 = s2.insertBefore(ht3(), r3);
    e3 = new At2(o, d5, n2, n2.options);
  } else {
    let o = e3._$AB.nextSibling, d5 = e3._$AM, h3 = d5 !== n2;
    if (h3) {
      let l4;
      (i3 = e3._$AQ) === null || i3 === void 0 || i3.call(e3, n2), e3._$AM = n2, e3._$AP !== void 0 && (l4 = n2._$AU) !== d5._$AU && e3._$AP(l4);
    }
    if (o !== r3 || h3) {
      let l4 = e3._$AA;
      for (; l4 !== o; ) {
        let _9 = l4.nextSibling;
        s2.insertBefore(l4, r3), l4 = _9;
      }
    }
  }
  return e3;
};
var m8 = (n2, t2, e3 = n2) => (n2._$AI(t2, e3), n2);
var vt2 = {};
var at4 = (n2, t2 = vt2) => n2._$AH = t2;
var dt4 = (n2) => n2._$AH;
var k9 = (n2) => {
  var t2;
  (t2 = n2._$AP) === null || t2 === void 0 || t2.call(n2, false, true);
  let e3 = n2._$AA, i3 = n2._$AB.nextSibling;
  for (; e3 !== i3; ) {
    let s2 = e3.nextSibling;
    e3.remove(), e3 = s2;
  }
};
var $t3 = (n2, t2, e3) => {
  let i3 = /* @__PURE__ */ new Map();
  for (let s2 = t2; s2 <= e3; s2++)
    i3.set(n2[s2], s2);
  return i3;
};
var Ct4 = lt3(class extends j9 {
  constructor(n2) {
    if (super(n2), n2.type !== rt5.CHILD)
      throw Error("repeat() can only be used in text expressions");
  }
  dt(n2, t2, e3) {
    let i3;
    e3 === void 0 ? e3 = t2 : t2 !== void 0 && (i3 = t2);
    let s2 = [], r3 = [], o = 0;
    for (let d5 of n2)
      s2[o] = i3 ? i3(d5, o) : o, r3[o] = e3(d5, o), o++;
    return { values: r3, keys: s2 };
  }
  render(n2, t2, e3) {
    return this.dt(n2, t2, e3).values;
  }
  update(n2, [t2, e3, i3]) {
    var s2;
    let r3 = dt4(n2), { values: o, keys: d5 } = this.dt(t2, e3, i3);
    if (!Array.isArray(r3))
      return this.ht = d5, o;
    let h3 = (s2 = this.ht) !== null && s2 !== void 0 ? s2 : this.ht = [], l4 = [], _9, $4, a3 = 0, u6 = r3.length - 1, c7 = 0, A5 = o.length - 1;
    for (; a3 <= u6 && c7 <= A5; )
      if (r3[a3] === null)
        a3++;
      else if (r3[u6] === null)
        u6--;
      else if (h3[a3] === d5[c7])
        l4[c7] = m8(r3[a3], o[c7]), a3++, c7++;
      else if (h3[u6] === d5[A5])
        l4[A5] = m8(r3[u6], o[A5]), u6--, A5--;
      else if (h3[a3] === d5[A5])
        l4[A5] = m8(r3[a3], o[A5]), E6(n2, l4[A5 + 1], r3[a3]), a3++, A5--;
      else if (h3[u6] === d5[c7])
        l4[c7] = m8(r3[u6], o[c7]), E6(n2, r3[a3], r3[u6]), u6--, c7++;
      else if (_9 === void 0 && (_9 = $t3(d5, c7, A5), $4 = $t3(h3, a3, u6)), _9.has(h3[a3]))
        if (_9.has(h3[u6])) {
          let p10 = $4.get(d5[c7]), O7 = p10 !== void 0 ? r3[p10] : null;
          if (O7 === null) {
            let F7 = E6(n2, r3[a3]);
            m8(F7, o[c7]), l4[c7] = F7;
          } else
            l4[c7] = m8(O7, o[c7]), E6(n2, r3[a3], O7), r3[p10] = null;
          c7++;
        } else
          k9(r3[u6]), u6--;
      else
        k9(r3[a3]), a3++;
    for (; c7 <= A5; ) {
      let p10 = E6(n2, l4[A5 + 1]);
      m8(p10, o[c7]), l4[c7++] = p10;
    }
    for (; a3 <= u6; ) {
      let p10 = r3[a3++];
      p10 !== null && k9(p10);
    }
    return this.ht = d5, at4(n2, l4), H9;
  }
});

// src/emulator/multi-webview-comp-virtual-keyboard.html.ts
var TAG9 = "multi-webview-comp-virtual-keyboard";
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
    ].forEach(([propertyName, n2]) => {
      this._elContainer?.style.setProperty(propertyName, n2 + "px");
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
MultiWebviewCompVirtualKeyboard.styles = createAllCSS9();
__decorateClass([
  y8(".container")
], MultiWebviewCompVirtualKeyboard.prototype, "_elContainer", 2);
__decorateClass([
  y6({ type: Boolean })
], MultiWebviewCompVirtualKeyboard.prototype, "_visible", 2);
__decorateClass([
  y6({ type: Boolean })
], MultiWebviewCompVirtualKeyboard.prototype, "_overlay", 2);
__decorateClass([
  y6({ type: Number })
], MultiWebviewCompVirtualKeyboard.prototype, "_navigation_bar_height", 2);
MultiWebviewCompVirtualKeyboard = __decorateClass([
  c4(TAG9)
], MultiWebviewCompVirtualKeyboard);
function createAllCSS9() {
  return [
    C6`
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
var TAG10 = "root-comp";
var RootComp = class extends n {
  constructor() {
    super(...arguments);
    this.src = "about:blank";
    this.controllers = /* @__PURE__ */ new Set();
    this._bindReloadShortcut = () => {
      debugger;
      this.iframeEle?.contentWindow?.addEventListener("keydown", (e3) => {
        e3 = e3 || window.event;
        if (e3.ctrlKey && e3.keyCode == 82 || //ctrl+R
        e3.keyCode == 116) {
          debugger;
          this.iframeEle?.contentWindow?.location.reload();
        }
      });
    };
    /**statusBar */
    this.statusBarController = this._wc(new StatusBarController());
    /**navigationBar */
    this.navigationController = this._wc(new NavigationBarController());
    /**virtualboard */
    this.virtualKeyboardController = this._wc(
      new VirtualKeyboardController()
    );
    this.torchController = this._wc(new TorchController());
    this.hapticsController = this._wc(new HapticsController());
    this.biometricsController = this._wc(new BiometricsController());
  }
  _wc(c7) {
    c7.onUpdate(() => this.requestUpdate()).onInit((c8) => {
      this.controllers.add(c8);
      this.requestUpdate();
    }).onReady((c8) => {
      this.controllers.delete(c8);
      this.requestUpdate();
    });
    return c7;
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
      <div class="root">
        <emulator-toolbar .url=${this.src}></emulator-toolbar>

        <multi-webview-comp-mobile-shell class="main-view">
          ${t(this.biometricsController.state, () => {
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
          ${t(
      this.controllers.size === 0,
      () => tt3`
              <iframe
                slot="shell-content"
                style="width:100%;height:100%;border:0;"
                src=${this.src}
                @loadstart=${this._bindReloadShortcut}
              ></iframe>
            `,
      () => tt3`<div class="boot-logo" slot="shell-content">开机中</div>`
    )}
          ${t(
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
      </div>
    `;
  }
};
RootComp.styles = createAllCSS10();
__decorateClass([
  y6({ type: String })
], RootComp.prototype, "src", 2);
__decorateClass([
  y7()
], RootComp.prototype, "controllers", 2);
__decorateClass([
  y8("iframe")
], RootComp.prototype, "iframeEle", 2);
RootComp = __decorateClass([
  c4(TAG10)
], RootComp);
function createAllCSS10() {
  return [
    C6`
      :host {
        display: block;
      }
      .root {
        display: flex;
        flex-direction: column;
        height: 100%;
      }
      .main-view {
        flex: 1;
      }

      .boot-logo {
        height: 100%;
        display: grid;
        place-items: center;
        font-size: 32px;
        color: rgba(255, 255, 255, 0.3);
        background: -webkit-linear-gradient(
            -30deg,
            rgba(255, 255, 255, 0) 100px,
            rgba(255, 255, 255, 1) 180px,
            rgba(255, 255, 255, 1) 240px,
            rgba(255, 255, 255, 0) 300px
          ) -300px 0 no-repeat;
        -webkit-background-clip: text;
        animation-name: boot-logo;
        animation-duration: 6000ms;
        animation-iteration-count: infinite;
        /* animation-fill-mode:forward */
      }
      @keyframes boot-logo {
        0% {
          background-position: -300px 0px;
        }
        100% {
          background-position: 1000px 0px;
        }
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
