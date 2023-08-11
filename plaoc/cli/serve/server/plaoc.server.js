var __defProp = Object.defineProperty;
var __require = /* @__PURE__ */ ((x4) => typeof require !== "undefined" ? require : typeof Proxy !== "undefined" ? new Proxy(x4, {
  get: (a3, b3) => (typeof require !== "undefined" ? require : a3)[b3]
}) : x4)(function(x4) {
  if (typeof require !== "undefined")
    return require.apply(this, arguments);
  throw new Error('Dynamic require of "' + x4 + '" is not supported');
});
var __export = (target, all) => {
  for (var name in all)
    __defProp(target, name, { get: all[name], enumerable: true });
};

// ../desktop-dev/src/helper/binaryHelper.ts
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

// ../desktop-dev/src/helper/encoding.ts
var textEncoder = new TextEncoder();
var simpleEncoder = (data, encoding) => {
  if (encoding === "base64") {
    const byteCharacters = atob(data);
    const binary = new Uint8Array(byteCharacters.length);
    for (let i4 = 0; i4 < byteCharacters.length; i4++) {
      binary[i4] = byteCharacters.charCodeAt(i4);
    }
    return binary;
  } else if (encoding === "hex") {
    const binary = new Uint8Array(data.length / 2);
    for (let i4 = 0; i4 < binary.length; i4++) {
      const start = i4 + i4;
      binary[i4] = parseInt(data.slice(start, start + 2), 16);
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
  } else if (encoding === "hex") {
    let hex = "";
    const bytes = binaryToU8a(data);
    for (const byte of bytes) {
      hex += byte.toString(16).padStart(2, "0");
    }
    return hex;
  }
  return textDecoder.decode(data);
};

// ../desktop-dev/src/helper/createSignal.ts
var createSignal = (autoStart) => {
  return new Signal(autoStart);
};
var Signal = class {
  constructor(autoStart = true) {
    this._cbs = /* @__PURE__ */ new Set();
    this._started = false;
    this._cachedEmits = [];
    this.start = () => {
      if (this._started) {
        return;
      }
      this._started = true;
      if (this._cachedEmits.length) {
        for (const args of this._cachedEmits) {
          this._emit(args, this._cbs);
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
        this._emit(args, this._cbs);
      } else {
        this._cachedEmits.push(args);
      }
    };
    this.emitAndClear = (...args) => {
      if (this._started) {
        const cbs = [...this._cbs];
        this._cbs.clear();
        this._emit(args, cbs);
      } else {
        this._cachedEmits.push(args);
      }
    };
    this._emit = (args, cbs) => {
      for (const cb of cbs) {
        try {
          cb.apply(null, args);
        } catch (reason) {
          console.warn(reason);
        }
      }
    };
    this.clear = () => {
      this._cbs.clear();
    };
    if (autoStart) {
      this.start();
    }
  }
};

// ../desktop-dev/src/helper/stream/readableStreamHelper.ts
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

// ../desktop-dev/src/core/ipc/const.ts
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

// ../desktop-dev/src/core/ipc/IpcBodyReceiver.ts
new WritableStream({});

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
  static resolve(v5) {
    const po = new PromiseOut();
    po.resolve(v5);
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
        console.error("Unhandled promise rejection when running onFinished", innerFinally, err);
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
        console.error("Unhandled promise rejection when running onSuccess", innerThen, err);
      }
    });
  }
  __callInnerCatch(innerCatch) {
    queueMicrotask(async () => {
      try {
        await innerCatch(this.value);
      } catch (err) {
        console.error("Unhandled promise rejection when running onError", innerCatch, err);
      }
    });
  }
};

// ../desktop-dev/src/helper/cacheGetter.ts
var CacheGetter = class {
  constructor(getter) {
    this.getter = getter;
    this._first = true;
  }
  get value() {
    if (this._first) {
      this._first = false;
      this._value = this.getter();
    }
    return this._value;
  }
  reset() {
    this._first = true;
    this._value = void 0;
  }
};

// ../desktop-dev/src/core/ipc/IpcStreamData.ts
var IpcStreamData = class extends IpcMessage {
  constructor(stream_id, data, encoding) {
    super(2 /* STREAM_DATA */);
    this.stream_id = stream_id;
    this.data = data;
    this.encoding = encoding;
  }
  static fromBase64(stream_id, data) {
    return new IpcStreamData(stream_id, simpleDecoder(data, "base64"), 4 /* BASE64 */);
  }
  static fromBinary(stream_id, data) {
    return new IpcStreamData(stream_id, data, 8 /* BINARY */);
  }
  static fromUtf8(stream_id, data) {
    return new IpcStreamData(stream_id, simpleDecoder(data, "utf8"), 2 /* UTF8 */);
  }
  #binary = new CacheGetter(() => $dataToBinary(this.data, this.encoding));
  get binary() {
    return this.#binary.value;
  }
  #text = new CacheGetter(() => $dataToText(this.data, this.encoding));
  get text() {
    return this.#text.value;
  }
  #jsonAble = new CacheGetter(() => {
    if (this.encoding === 8 /* BINARY */) {
      return IpcStreamData.fromBase64(this.stream_id, this.data);
    }
    return this;
  });
  get jsonAble() {
    return this.#jsonAble.value;
  }
  toJSON() {
    return { ...this.jsonAble };
  }
};

// ../desktop-dev/src/core/ipc/IpcStreamEnd.ts
var IpcStreamEnd = class extends IpcMessage {
  constructor(stream_id) {
    super(5 /* STREAM_END */);
    this.stream_id = stream_id;
  }
};

// ../desktop-dev/src/core/ipc/MetaBody.ts
var MetaBody = class {
  constructor(type, senderUid, data, streamId, receiverUid, metaId = simpleDecoder(crypto.getRandomValues(new Uint8Array(8)), "base64")) {
    this.type = type;
    this.senderUid = senderUid;
    this.data = data;
    this.streamId = streamId;
    this.receiverUid = receiverUid;
    this.metaId = metaId;
  }
  static fromJSON(metaBody) {
    if (metaBody instanceof MetaBody === false) {
      metaBody = new MetaBody(
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
    return new MetaBody(
      streamId == null ? IPC_META_BODY_TYPE.INLINE_TEXT : IPC_META_BODY_TYPE.STREAM_WITH_TEXT,
      senderUid,
      data,
      streamId,
      receiverUid
    );
  }
  static fromBase64(senderUid, data, streamId, receiverUid) {
    return new MetaBody(
      streamId == null ? IPC_META_BODY_TYPE.INLINE_BASE64 : IPC_META_BODY_TYPE.STREAM_WITH_BASE64,
      senderUid,
      data,
      streamId,
      receiverUid
    );
  }
  static fromBinary(sender, data, streamId, receiverUid) {
    if (typeof sender === "number") {
      return new MetaBody(
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
    return this.fromBase64(sender.uid, simpleDecoder(data, "base64"), streamId, receiverUid);
  }
  #type_encoding = new CacheGetter(() => {
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
  });
  get type_encoding() {
    return this.#type_encoding.value;
  }
  #type_isInline = new CacheGetter(() => (this.type & 1 /* INLINE */) !== 0);
  get type_isInline() {
    return this.#type_isInline.value;
  }
  #type_isStream = new CacheGetter(() => (this.type & 1 /* INLINE */) === 0);
  get type_isStream() {
    return this.#type_isStream.value;
  }
  #jsonAble = new CacheGetter(() => {
    if (this.type_encoding === 8 /* BINARY */) {
      return MetaBody.fromBase64(
        this.senderUid,
        simpleDecoder(this.data, "base64"),
        this.streamId,
        this.receiverUid
      );
    }
    return this;
  });
  get jsonAble() {
    return this.#jsonAble.value;
  }
  toJSON() {
    return { ...this.jsonAble };
  }
};
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
  constructor(data, ipc2) {
    super();
    this.data = data;
    this.ipc = ipc2;
    this.streamCtorSignal = createSignal();
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
    this.closeSignal = createSignal();
    this.openSignal = createSignal();
    this._isStreamOpened = false;
    this._isStreamClosed = false;
    this._bodyHub = new BodyHub(data);
    this.metaBody = this.$bodyAsMeta(data, ipc2);
    this.isStream = data instanceof ReadableStream;
    if (typeof data !== "string") {
      _IpcBodySender.CACHE.raw_ipcBody_WMap.set(data, this);
    }
    _IpcBodySender.$usableByIpc(ipc2, this);
  }
  static fromAny(data, ipc2) {
    if (typeof data !== "string") {
      const cache = _IpcBodySender.CACHE.raw_ipcBody_WMap.get(data);
      if (cache !== void 0) {
        return cache;
      }
    }
    return new _IpcBodySender(data, ipc2);
  }
  static fromText(raw, ipc2) {
    return this.fromAny(raw, ipc2);
  }
  static fromBinary(raw, ipc2) {
    return this.fromAny(raw, ipc2);
  }
  static fromStream(raw, ipc2) {
    return this.fromAny(raw, ipc2);
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
        this.openSignal.emitAndClear();
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
        this.closeSignal.emitAndClear();
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
          const message = IpcStreamData.fromBinary(stream_id, await reader.readBinary(availableLen));
          for (const ipc3 of this.usedIpcMap.keys()) {
            ipc3.postMessage(message);
          }
        } else if (availableLen === -1) {
          const message = new IpcStreamEnd(stream_id);
          for (const ipc3 of this.usedIpcMap.keys()) {
            ipc3.postMessage(message);
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
    const metaBody = new MetaBody(streamType, ipc2.uid, streamFirstData, stream_id);
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
        this.destroySignal.emitAndClear();
      }
    }
  }
  onDestroy(cb) {
    this.destroySignal.listen(cb);
  }
};
var IpcUsableIpcBodyMap = /* @__PURE__ */ new WeakMap();

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

// ../desktop-dev/src/helper/stream/JsonlinesStream.ts
var JsonlinesStream = class extends TransformStream {
  constructor(onError) {
    let json = "";
    const try_enqueue = (controller, jsonline) => {
      try {
        controller.enqueue(JSON.parse(jsonline));
      } catch (err) {
        onError ? onError(err, controller) : controller.error(err);
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

// ../desktop-dev/src/helper/stream/jsonlinesStreamHelper.ts
var toJsonlinesStream = (stream) => {
  return stream.pipeThrough(new TextDecoderStream()).pipeThrough(new JsonlinesStream());
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
      toJsonlinesStream(await this.stream())
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

// ../desktop-dev/src/helper/AdaptersManager.ts
var AdaptersManager = class {
  constructor() {
    this.adapterOrderMap = /* @__PURE__ */ new Map();
    this.orderdAdapters = [];
  }
  _reorder() {
    this.orderdAdapters = [...this.adapterOrderMap].sort((a3, b3) => b3[1] - a3[1]).map((a3) => a3[0]);
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

// ../desktop-dev/src/core/helper/ipcFetchHelper.ts
var FETCH_MID_SYMBOL = Symbol("fetch.middleware");
var FETCH_END_SYMBOL = Symbol("fetch.end");
var FETCH_WS_SYMBOL = Symbol("fetch.websocket");
var FetchError = class extends Error {
  constructor(message, options) {
    super(message, options);
    this.code = options?.status ?? 500;
  }
};

// https://esm.sh/v124/ieee754@1.2.1/es2022/ieee754.mjs
var ieee754_exports = {};
__export(ieee754_exports, {
  default: () => O,
  read: () => H,
  write: () => J
});
var y = Object.create;
var v = Object.defineProperty;
var z = Object.getOwnPropertyDescriptor;
var A = Object.getOwnPropertyNames;
var C = Object.getPrototypeOf;
var D = Object.prototype.hasOwnProperty;
var F = (a3, r2) => () => (r2 || a3((r2 = { exports: {} }).exports, r2), r2.exports);
var G = (a3, r2) => {
  for (var i4 in r2)
    v(a3, i4, { get: r2[i4], enumerable: true });
};
var e = (a3, r2, i4, f) => {
  if (r2 && typeof r2 == "object" || typeof r2 == "function")
    for (let o of A(r2))
      !D.call(a3, o) && o !== i4 && v(a3, o, { get: () => r2[o], enumerable: !(f = z(r2, o)) || f.enumerable });
  return a3;
};
var _ = (a3, r2, i4) => (e(a3, r2, "default"), i4 && e(i4, r2, "default"));
var B = (a3, r2, i4) => (i4 = a3 != null ? y(C(a3)) : {}, e(r2 || !a3 || !a3.__esModule ? v(i4, "default", { value: a3, enumerable: true }) : i4, a3));
var g = F((I2) => {
  I2.read = function(a3, r2, i4, f, o) {
    var h2, t, w3 = o * 8 - f - 1, s = (1 << w3) - 1, N3 = s >> 1, M4 = -7, p4 = i4 ? o - 1 : 0, c3 = i4 ? -1 : 1, d3 = a3[r2 + p4];
    for (p4 += c3, h2 = d3 & (1 << -M4) - 1, d3 >>= -M4, M4 += w3; M4 > 0; h2 = h2 * 256 + a3[r2 + p4], p4 += c3, M4 -= 8)
      ;
    for (t = h2 & (1 << -M4) - 1, h2 >>= -M4, M4 += f; M4 > 0; t = t * 256 + a3[r2 + p4], p4 += c3, M4 -= 8)
      ;
    if (h2 === 0)
      h2 = 1 - N3;
    else {
      if (h2 === s)
        return t ? NaN : (d3 ? -1 : 1) * (1 / 0);
      t = t + Math.pow(2, f), h2 = h2 - N3;
    }
    return (d3 ? -1 : 1) * t * Math.pow(2, h2 - f);
  };
  I2.write = function(a3, r2, i4, f, o, h2) {
    var t, w3, s, N3 = h2 * 8 - o - 1, M4 = (1 << N3) - 1, p4 = M4 >> 1, c3 = o === 23 ? Math.pow(2, -24) - Math.pow(2, -77) : 0, d3 = f ? 0 : h2 - 1, n2 = f ? 1 : -1, q2 = r2 < 0 || r2 === 0 && 1 / r2 < 0 ? 1 : 0;
    for (r2 = Math.abs(r2), isNaN(r2) || r2 === 1 / 0 ? (w3 = isNaN(r2) ? 1 : 0, t = M4) : (t = Math.floor(Math.log(r2) / Math.LN2), r2 * (s = Math.pow(2, -t)) < 1 && (t--, s *= 2), t + p4 >= 1 ? r2 += c3 / s : r2 += c3 * Math.pow(2, 1 - p4), r2 * s >= 2 && (t++, s /= 2), t + p4 >= M4 ? (w3 = 0, t = M4) : t + p4 >= 1 ? (w3 = (r2 * s - 1) * Math.pow(2, o), t = t + p4) : (w3 = r2 * Math.pow(2, p4 - 1) * Math.pow(2, o), t = 0)); o >= 8; a3[i4 + d3] = w3 & 255, d3 += n2, w3 /= 256, o -= 8)
      ;
    for (t = t << o | w3, N3 += o; N3 > 0; a3[i4 + d3] = t & 255, d3 += n2, t /= 256, N3 -= 8)
      ;
    a3[i4 + d3 - n2] |= q2 * 128;
  };
});
var x = {};
G(x, { default: () => O, read: () => H, write: () => J });
var k = B(g());
_(x, B(g()));
var { read: H, write: J } = k;
var { default: j, ...K } = k;
var O = j !== void 0 ? j : K;

// https://esm.sh/v124/base64-js@1.5.1/es2022/base64-js.mjs
var base64_js_exports = {};
__export(base64_js_exports, {
  byteLength: () => G2,
  default: () => N,
  fromByteArray: () => K2,
  toByteArray: () => J2
});
var B2 = Object.create;
var l = Object.defineProperty;
var _2 = Object.getOwnPropertyDescriptor;
var k2 = Object.getOwnPropertyNames;
var w = Object.getPrototypeOf;
var j2 = Object.prototype.hasOwnProperty;
var H2 = (r2, e2) => () => (e2 || r2((e2 = { exports: {} }).exports, e2), e2.exports);
var U = (r2, e2) => {
  for (var t in e2)
    l(r2, t, { get: e2[t], enumerable: true });
};
var A2 = (r2, e2, t, a3) => {
  if (e2 && typeof e2 == "object" || typeof e2 == "function")
    for (let o of k2(e2))
      !j2.call(r2, o) && o !== t && l(r2, o, { get: () => e2[o], enumerable: !(a3 = _2(e2, o)) || a3.enumerable });
  return r2;
};
var u = (r2, e2, t) => (A2(r2, e2, "default"), t && A2(t, e2, "default"));
var C2 = (r2, e2, t) => (t = r2 != null ? B2(w(r2)) : {}, A2(e2 || !r2 || !r2.__esModule ? l(t, "default", { value: r2, enumerable: true }) : t, r2));
var p = H2((y4) => {
  "use strict";
  y4.byteLength = I2;
  y4.toByteArray = T3;
  y4.fromByteArray = D5;
  var h2 = [], d3 = [], E4 = typeof Uint8Array < "u" ? Uint8Array : Array, s = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
  for (F3 = 0, L3 = s.length; F3 < L3; ++F3)
    h2[F3] = s[F3], d3[s.charCodeAt(F3)] = F3;
  var F3, L3;
  d3["-".charCodeAt(0)] = 62;
  d3["_".charCodeAt(0)] = 63;
  function g3(r2) {
    var e2 = r2.length;
    if (e2 % 4 > 0)
      throw new Error("Invalid string. Length must be a multiple of 4");
    var t = r2.indexOf("=");
    t === -1 && (t = e2);
    var a3 = t === e2 ? 0 : 4 - t % 4;
    return [t, a3];
  }
  function I2(r2) {
    var e2 = g3(r2), t = e2[0], a3 = e2[1];
    return (t + a3) * 3 / 4 - a3;
  }
  function O3(r2, e2, t) {
    return (e2 + t) * 3 / 4 - t;
  }
  function T3(r2) {
    var e2, t = g3(r2), a3 = t[0], o = t[1], n2 = new E4(O3(r2, a3, o)), v5 = 0, x4 = o > 0 ? a3 - 4 : a3, f;
    for (f = 0; f < x4; f += 4)
      e2 = d3[r2.charCodeAt(f)] << 18 | d3[r2.charCodeAt(f + 1)] << 12 | d3[r2.charCodeAt(f + 2)] << 6 | d3[r2.charCodeAt(f + 3)], n2[v5++] = e2 >> 16 & 255, n2[v5++] = e2 >> 8 & 255, n2[v5++] = e2 & 255;
    return o === 2 && (e2 = d3[r2.charCodeAt(f)] << 2 | d3[r2.charCodeAt(f + 1)] >> 4, n2[v5++] = e2 & 255), o === 1 && (e2 = d3[r2.charCodeAt(f)] << 10 | d3[r2.charCodeAt(f + 1)] << 4 | d3[r2.charCodeAt(f + 2)] >> 2, n2[v5++] = e2 >> 8 & 255, n2[v5++] = e2 & 255), n2;
  }
  function q2(r2) {
    return h2[r2 >> 18 & 63] + h2[r2 >> 12 & 63] + h2[r2 >> 6 & 63] + h2[r2 & 63];
  }
  function z3(r2, e2, t) {
    for (var a3, o = [], n2 = e2; n2 < t; n2 += 3)
      a3 = (r2[n2] << 16 & 16711680) + (r2[n2 + 1] << 8 & 65280) + (r2[n2 + 2] & 255), o.push(q2(a3));
    return o.join("");
  }
  function D5(r2) {
    for (var e2, t = r2.length, a3 = t % 3, o = [], n2 = 16383, v5 = 0, x4 = t - a3; v5 < x4; v5 += n2)
      o.push(z3(r2, v5, v5 + n2 > x4 ? x4 : v5 + n2));
    return a3 === 1 ? (e2 = r2[t - 1], o.push(h2[e2 >> 2] + h2[e2 << 4 & 63] + "==")) : a3 === 2 && (e2 = (r2[t - 2] << 8) + r2[t - 1], o.push(h2[e2 >> 10] + h2[e2 >> 4 & 63] + h2[e2 << 2 & 63] + "=")), o.join("");
  }
});
var c = {};
U(c, { byteLength: () => G2, default: () => N, fromByteArray: () => K2, toByteArray: () => J2 });
var i = C2(p());
u(c, C2(p()));
var { byteLength: G2, toByteArray: J2, fromByteArray: K2 } = i;
var { default: m, ...M } = i;
var N = m !== void 0 ? m : M;

// https://esm.sh/v124/buffer@6.0.3/es2022/buffer.bundle.mjs
var v2 = ((s) => typeof __require < "u" ? __require : typeof Proxy < "u" ? new Proxy(s, { get: (h2, c3) => (typeof __require < "u" ? __require : h2)[c3] }) : s)(function(s) {
  if (typeof __require < "u")
    return __require.apply(this, arguments);
  throw new Error('Dynamic require of "' + s + '" is not supported');
});
var Lt = O ?? ieee754_exports;
var xt = N ?? base64_js_exports;
var $t = Object.create;
var k3 = Object.defineProperty;
var Pt = Object.getOwnPropertyDescriptor;
var Ct = Object.getOwnPropertyNames;
var Mt = Object.getPrototypeOf;
var Nt = Object.prototype.hasOwnProperty;
var Wt = ((s) => typeof v2 < "u" ? v2 : typeof Proxy < "u" ? new Proxy(s, { get: (h2, c3) => (typeof v2 < "u" ? v2 : h2)[c3] }) : s)(function(s) {
  if (typeof v2 < "u")
    return v2.apply(this, arguments);
  throw new Error('Dynamic require of "' + s + '" is not supported');
});
var kt = (s, h2) => () => (h2 || s((h2 = { exports: {} }).exports, h2), h2.exports);
var jt = (s, h2) => {
  for (var c3 in h2)
    k3(s, c3, { get: h2[c3], enumerable: true });
};
var C3 = (s, h2, c3, R3) => {
  if (h2 && typeof h2 == "object" || typeof h2 == "function")
    for (let b3 of Ct(h2))
      !Nt.call(s, b3) && b3 !== c3 && k3(s, b3, { get: () => h2[b3], enumerable: !(R3 = Pt(h2, b3)) || R3.enumerable });
  return s;
};
var Ft = (s, h2, c3) => (C3(s, h2, "default"), c3 && C3(c3, h2, "default"));
var nt = (s, h2, c3) => (c3 = s != null ? $t(Mt(s)) : {}, C3(h2 || !s || !s.__esModule ? k3(c3, "default", { value: s, enumerable: true }) : c3, s));
var rt = kt((s) => {
  "use strict";
  var h2 = xt, c3 = Lt, R3 = typeof Symbol == "function" && typeof Symbol.for == "function" ? Symbol.for("nodejs.util.inspect.custom") : null;
  s.Buffer = i4, s.SlowBuffer = at3, s.INSPECT_MAX_BYTES = 50;
  var b3 = 2147483647;
  s.kMaxLength = b3, i4.TYPED_ARRAY_SUPPORT = ft3(), !i4.TYPED_ARRAY_SUPPORT && typeof console < "u" && typeof console.error == "function" && console.error("This browser lacks typed array (Uint8Array) support which is required by `buffer` v5.x. Use `buffer` v4.x if you require old browser support.");
  function ft3() {
    try {
      let t = new Uint8Array(1), e2 = { foo: function() {
        return 42;
      } };
      return Object.setPrototypeOf(e2, Uint8Array.prototype), Object.setPrototypeOf(t, e2), t.foo() === 42;
    } catch {
      return false;
    }
  }
  Object.defineProperty(i4.prototype, "parent", { enumerable: true, get: function() {
    if (i4.isBuffer(this))
      return this.buffer;
  } }), Object.defineProperty(i4.prototype, "offset", { enumerable: true, get: function() {
    if (i4.isBuffer(this))
      return this.byteOffset;
  } });
  function m4(t) {
    if (t > b3)
      throw new RangeError('The value "' + t + '" is invalid for option "size"');
    let e2 = new Uint8Array(t);
    return Object.setPrototypeOf(e2, i4.prototype), e2;
  }
  function i4(t, e2, n2) {
    if (typeof t == "number") {
      if (typeof e2 == "string")
        throw new TypeError('The "string" argument must be of type string. Received type number');
      return _4(t);
    }
    return j6(t, e2, n2);
  }
  i4.poolSize = 8192;
  function j6(t, e2, n2) {
    if (typeof t == "string")
      return st3(t, e2);
    if (ArrayBuffer.isView(t))
      return ht3(t);
    if (t == null)
      throw new TypeError("The first argument must be one of type string, Buffer, ArrayBuffer, Array, or Array-like Object. Received type " + typeof t);
    if (B4(t, ArrayBuffer) || t && B4(t.buffer, ArrayBuffer) || typeof SharedArrayBuffer < "u" && (B4(t, SharedArrayBuffer) || t && B4(t.buffer, SharedArrayBuffer)))
      return D5(t, e2, n2);
    if (typeof t == "number")
      throw new TypeError('The "value" argument must not be of type number. Received type number');
    let r2 = t.valueOf && t.valueOf();
    if (r2 != null && r2 !== t)
      return i4.from(r2, e2, n2);
    let o = lt3(t);
    if (o)
      return o;
    if (typeof Symbol < "u" && Symbol.toPrimitive != null && typeof t[Symbol.toPrimitive] == "function")
      return i4.from(t[Symbol.toPrimitive]("string"), e2, n2);
    throw new TypeError("The first argument must be one of type string, Buffer, ArrayBuffer, Array, or Array-like Object. Received type " + typeof t);
  }
  i4.from = function(t, e2, n2) {
    return j6(t, e2, n2);
  }, Object.setPrototypeOf(i4.prototype, Uint8Array.prototype), Object.setPrototypeOf(i4, Uint8Array);
  function F3(t) {
    if (typeof t != "number")
      throw new TypeError('"size" argument must be of type number');
    if (t < 0)
      throw new RangeError('The value "' + t + '" is invalid for option "size"');
  }
  function ut3(t, e2, n2) {
    return F3(t), t <= 0 ? m4(t) : e2 !== void 0 ? typeof n2 == "string" ? m4(t).fill(e2, n2) : m4(t).fill(e2) : m4(t);
  }
  i4.alloc = function(t, e2, n2) {
    return ut3(t, e2, n2);
  };
  function _4(t) {
    return F3(t), m4(t < 0 ? 0 : L3(t) | 0);
  }
  i4.allocUnsafe = function(t) {
    return _4(t);
  }, i4.allocUnsafeSlow = function(t) {
    return _4(t);
  };
  function st3(t, e2) {
    if ((typeof e2 != "string" || e2 === "") && (e2 = "utf8"), !i4.isEncoding(e2))
      throw new TypeError("Unknown encoding: " + e2);
    let n2 = Y3(t, e2) | 0, r2 = m4(n2), o = r2.write(t, e2);
    return o !== n2 && (r2 = r2.slice(0, o)), r2;
  }
  function S3(t) {
    let e2 = t.length < 0 ? 0 : L3(t.length) | 0, n2 = m4(e2);
    for (let r2 = 0; r2 < e2; r2 += 1)
      n2[r2] = t[r2] & 255;
    return n2;
  }
  function ht3(t) {
    if (B4(t, Uint8Array)) {
      let e2 = new Uint8Array(t);
      return D5(e2.buffer, e2.byteOffset, e2.byteLength);
    }
    return S3(t);
  }
  function D5(t, e2, n2) {
    if (e2 < 0 || t.byteLength < e2)
      throw new RangeError('"offset" is outside of buffer bounds');
    if (t.byteLength < e2 + (n2 || 0))
      throw new RangeError('"length" is outside of buffer bounds');
    let r2;
    return e2 === void 0 && n2 === void 0 ? r2 = new Uint8Array(t) : n2 === void 0 ? r2 = new Uint8Array(t, e2) : r2 = new Uint8Array(t, e2, n2), Object.setPrototypeOf(r2, i4.prototype), r2;
  }
  function lt3(t) {
    if (i4.isBuffer(t)) {
      let e2 = L3(t.length) | 0, n2 = m4(e2);
      return n2.length === 0 || t.copy(n2, 0, 0, e2), n2;
    }
    if (t.length !== void 0)
      return typeof t.length != "number" || P4(t.length) ? m4(0) : S3(t);
    if (t.type === "Buffer" && Array.isArray(t.data))
      return S3(t.data);
  }
  function L3(t) {
    if (t >= b3)
      throw new RangeError("Attempt to allocate Buffer larger than maximum size: 0x" + b3.toString(16) + " bytes");
    return t | 0;
  }
  function at3(t) {
    return +t != t && (t = 0), i4.alloc(+t);
  }
  i4.isBuffer = function(t) {
    return t != null && t._isBuffer === true && t !== i4.prototype;
  }, i4.compare = function(t, e2) {
    if (B4(t, Uint8Array) && (t = i4.from(t, t.offset, t.byteLength)), B4(e2, Uint8Array) && (e2 = i4.from(e2, e2.offset, e2.byteLength)), !i4.isBuffer(t) || !i4.isBuffer(e2))
      throw new TypeError('The "buf1", "buf2" arguments must be one of type Buffer or Uint8Array');
    if (t === e2)
      return 0;
    let n2 = t.length, r2 = e2.length;
    for (let o = 0, f = Math.min(n2, r2); o < f; ++o)
      if (t[o] !== e2[o]) {
        n2 = t[o], r2 = e2[o];
        break;
      }
    return n2 < r2 ? -1 : r2 < n2 ? 1 : 0;
  }, i4.isEncoding = function(t) {
    switch (String(t).toLowerCase()) {
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
  }, i4.concat = function(t, e2) {
    if (!Array.isArray(t))
      throw new TypeError('"list" argument must be an Array of Buffers');
    if (t.length === 0)
      return i4.alloc(0);
    let n2;
    if (e2 === void 0)
      for (e2 = 0, n2 = 0; n2 < t.length; ++n2)
        e2 += t[n2].length;
    let r2 = i4.allocUnsafe(e2), o = 0;
    for (n2 = 0; n2 < t.length; ++n2) {
      let f = t[n2];
      if (B4(f, Uint8Array))
        o + f.length > r2.length ? (i4.isBuffer(f) || (f = i4.from(f)), f.copy(r2, o)) : Uint8Array.prototype.set.call(r2, f, o);
      else if (i4.isBuffer(f))
        f.copy(r2, o);
      else
        throw new TypeError('"list" argument must be an Array of Buffers');
      o += f.length;
    }
    return r2;
  };
  function Y3(t, e2) {
    if (i4.isBuffer(t))
      return t.length;
    if (ArrayBuffer.isView(t) || B4(t, ArrayBuffer))
      return t.byteLength;
    if (typeof t != "string")
      throw new TypeError('The "string" argument must be one of type string, Buffer, or ArrayBuffer. Received type ' + typeof t);
    let n2 = t.length, r2 = arguments.length > 2 && arguments[2] === true;
    if (!r2 && n2 === 0)
      return 0;
    let o = false;
    for (; ; )
      switch (e2) {
        case "ascii":
        case "latin1":
        case "binary":
          return n2;
        case "utf8":
        case "utf-8":
          return $2(t).length;
        case "ucs2":
        case "ucs-2":
        case "utf16le":
        case "utf-16le":
          return n2 * 2;
        case "hex":
          return n2 >>> 1;
        case "base64":
          return tt3(t).length;
        default:
          if (o)
            return r2 ? -1 : $2(t).length;
          e2 = ("" + e2).toLowerCase(), o = true;
      }
  }
  i4.byteLength = Y3;
  function pt3(t, e2, n2) {
    let r2 = false;
    if ((e2 === void 0 || e2 < 0) && (e2 = 0), e2 > this.length || ((n2 === void 0 || n2 > this.length) && (n2 = this.length), n2 <= 0) || (n2 >>>= 0, e2 >>>= 0, n2 <= e2))
      return "";
    for (t || (t = "utf8"); ; )
      switch (t) {
        case "hex":
          return It2(this, e2, n2);
        case "utf8":
        case "utf-8":
          return G5(this, e2, n2);
        case "ascii":
          return mt3(this, e2, n2);
        case "latin1":
        case "binary":
          return Et2(this, e2, n2);
        case "base64":
          return bt3(this, e2, n2);
        case "ucs2":
        case "ucs-2":
        case "utf16le":
        case "utf-16le":
          return vt2(this, e2, n2);
        default:
          if (r2)
            throw new TypeError("Unknown encoding: " + t);
          t = (t + "").toLowerCase(), r2 = true;
      }
  }
  i4.prototype._isBuffer = true;
  function I2(t, e2, n2) {
    let r2 = t[e2];
    t[e2] = t[n2], t[n2] = r2;
  }
  i4.prototype.swap16 = function() {
    let t = this.length;
    if (t % 2 !== 0)
      throw new RangeError("Buffer size must be a multiple of 16-bits");
    for (let e2 = 0; e2 < t; e2 += 2)
      I2(this, e2, e2 + 1);
    return this;
  }, i4.prototype.swap32 = function() {
    let t = this.length;
    if (t % 4 !== 0)
      throw new RangeError("Buffer size must be a multiple of 32-bits");
    for (let e2 = 0; e2 < t; e2 += 4)
      I2(this, e2, e2 + 3), I2(this, e2 + 1, e2 + 2);
    return this;
  }, i4.prototype.swap64 = function() {
    let t = this.length;
    if (t % 8 !== 0)
      throw new RangeError("Buffer size must be a multiple of 64-bits");
    for (let e2 = 0; e2 < t; e2 += 8)
      I2(this, e2, e2 + 7), I2(this, e2 + 1, e2 + 6), I2(this, e2 + 2, e2 + 5), I2(this, e2 + 3, e2 + 4);
    return this;
  }, i4.prototype.toString = function() {
    let t = this.length;
    return t === 0 ? "" : arguments.length === 0 ? G5(this, 0, t) : pt3.apply(this, arguments);
  }, i4.prototype.toLocaleString = i4.prototype.toString, i4.prototype.equals = function(t) {
    if (!i4.isBuffer(t))
      throw new TypeError("Argument must be a Buffer");
    return this === t ? true : i4.compare(this, t) === 0;
  }, i4.prototype.inspect = function() {
    let t = "", e2 = s.INSPECT_MAX_BYTES;
    return t = this.toString("hex", 0, e2).replace(/(.{2})/g, "$1 ").trim(), this.length > e2 && (t += " ... "), "<Buffer " + t + ">";
  }, R3 && (i4.prototype[R3] = i4.prototype.inspect), i4.prototype.compare = function(t, e2, n2, r2, o) {
    if (B4(t, Uint8Array) && (t = i4.from(t, t.offset, t.byteLength)), !i4.isBuffer(t))
      throw new TypeError('The "target" argument must be one of type Buffer or Uint8Array. Received type ' + typeof t);
    if (e2 === void 0 && (e2 = 0), n2 === void 0 && (n2 = t ? t.length : 0), r2 === void 0 && (r2 = 0), o === void 0 && (o = this.length), e2 < 0 || n2 > t.length || r2 < 0 || o > this.length)
      throw new RangeError("out of range index");
    if (r2 >= o && e2 >= n2)
      return 0;
    if (r2 >= o)
      return -1;
    if (e2 >= n2)
      return 1;
    if (e2 >>>= 0, n2 >>>= 0, r2 >>>= 0, o >>>= 0, this === t)
      return 0;
    let f = o - r2, u4 = n2 - e2, l2 = Math.min(f, u4), w3 = this.slice(r2, o), p4 = t.slice(e2, n2);
    for (let a3 = 0; a3 < l2; ++a3)
      if (w3[a3] !== p4[a3]) {
        f = w3[a3], u4 = p4[a3];
        break;
      }
    return f < u4 ? -1 : u4 < f ? 1 : 0;
  };
  function q2(t, e2, n2, r2, o) {
    if (t.length === 0)
      return -1;
    if (typeof n2 == "string" ? (r2 = n2, n2 = 0) : n2 > 2147483647 ? n2 = 2147483647 : n2 < -2147483648 && (n2 = -2147483648), n2 = +n2, P4(n2) && (n2 = o ? 0 : t.length - 1), n2 < 0 && (n2 = t.length + n2), n2 >= t.length) {
      if (o)
        return -1;
      n2 = t.length - 1;
    } else if (n2 < 0)
      if (o)
        n2 = 0;
      else
        return -1;
    if (typeof e2 == "string" && (e2 = i4.from(e2, r2)), i4.isBuffer(e2))
      return e2.length === 0 ? -1 : z3(t, e2, n2, r2, o);
    if (typeof e2 == "number")
      return e2 = e2 & 255, typeof Uint8Array.prototype.indexOf == "function" ? o ? Uint8Array.prototype.indexOf.call(t, e2, n2) : Uint8Array.prototype.lastIndexOf.call(t, e2, n2) : z3(t, [e2], n2, r2, o);
    throw new TypeError("val must be string, number or Buffer");
  }
  function z3(t, e2, n2, r2, o) {
    let f = 1, u4 = t.length, l2 = e2.length;
    if (r2 !== void 0 && (r2 = String(r2).toLowerCase(), r2 === "ucs2" || r2 === "ucs-2" || r2 === "utf16le" || r2 === "utf-16le")) {
      if (t.length < 2 || e2.length < 2)
        return -1;
      f = 2, u4 /= 2, l2 /= 2, n2 /= 2;
    }
    function w3(a3, y4) {
      return f === 1 ? a3[y4] : a3.readUInt16BE(y4 * f);
    }
    let p4;
    if (o) {
      let a3 = -1;
      for (p4 = n2; p4 < u4; p4++)
        if (w3(t, p4) === w3(e2, a3 === -1 ? 0 : p4 - a3)) {
          if (a3 === -1 && (a3 = p4), p4 - a3 + 1 === l2)
            return a3 * f;
        } else
          a3 !== -1 && (p4 -= p4 - a3), a3 = -1;
    } else
      for (n2 + l2 > u4 && (n2 = u4 - l2), p4 = n2; p4 >= 0; p4--) {
        let a3 = true;
        for (let y4 = 0; y4 < l2; y4++)
          if (w3(t, p4 + y4) !== w3(e2, y4)) {
            a3 = false;
            break;
          }
        if (a3)
          return p4;
      }
    return -1;
  }
  i4.prototype.includes = function(t, e2, n2) {
    return this.indexOf(t, e2, n2) !== -1;
  }, i4.prototype.indexOf = function(t, e2, n2) {
    return q2(this, t, e2, n2, true);
  }, i4.prototype.lastIndexOf = function(t, e2, n2) {
    return q2(this, t, e2, n2, false);
  };
  function ct3(t, e2, n2, r2) {
    n2 = Number(n2) || 0;
    let o = t.length - n2;
    r2 ? (r2 = Number(r2), r2 > o && (r2 = o)) : r2 = o;
    let f = e2.length;
    r2 > f / 2 && (r2 = f / 2);
    let u4;
    for (u4 = 0; u4 < r2; ++u4) {
      let l2 = parseInt(e2.substr(u4 * 2, 2), 16);
      if (P4(l2))
        return u4;
      t[n2 + u4] = l2;
    }
    return u4;
  }
  function yt2(t, e2, n2, r2) {
    return T3($2(e2, t.length - n2), t, n2, r2);
  }
  function gt3(t, e2, n2, r2) {
    return T3(Ot2(e2), t, n2, r2);
  }
  function wt3(t, e2, n2, r2) {
    return T3(tt3(e2), t, n2, r2);
  }
  function dt3(t, e2, n2, r2) {
    return T3(Tt3(e2, t.length - n2), t, n2, r2);
  }
  i4.prototype.write = function(t, e2, n2, r2) {
    if (e2 === void 0)
      r2 = "utf8", n2 = this.length, e2 = 0;
    else if (n2 === void 0 && typeof e2 == "string")
      r2 = e2, n2 = this.length, e2 = 0;
    else if (isFinite(e2))
      e2 = e2 >>> 0, isFinite(n2) ? (n2 = n2 >>> 0, r2 === void 0 && (r2 = "utf8")) : (r2 = n2, n2 = void 0);
    else
      throw new Error("Buffer.write(string, encoding, offset[, length]) is no longer supported");
    let o = this.length - e2;
    if ((n2 === void 0 || n2 > o) && (n2 = o), t.length > 0 && (n2 < 0 || e2 < 0) || e2 > this.length)
      throw new RangeError("Attempt to write outside buffer bounds");
    r2 || (r2 = "utf8");
    let f = false;
    for (; ; )
      switch (r2) {
        case "hex":
          return ct3(this, t, e2, n2);
        case "utf8":
        case "utf-8":
          return yt2(this, t, e2, n2);
        case "ascii":
        case "latin1":
        case "binary":
          return gt3(this, t, e2, n2);
        case "base64":
          return wt3(this, t, e2, n2);
        case "ucs2":
        case "ucs-2":
        case "utf16le":
        case "utf-16le":
          return dt3(this, t, e2, n2);
        default:
          if (f)
            throw new TypeError("Unknown encoding: " + r2);
          r2 = ("" + r2).toLowerCase(), f = true;
      }
  }, i4.prototype.toJSON = function() {
    return { type: "Buffer", data: Array.prototype.slice.call(this._arr || this, 0) };
  };
  function bt3(t, e2, n2) {
    return e2 === 0 && n2 === t.length ? h2.fromByteArray(t) : h2.fromByteArray(t.slice(e2, n2));
  }
  function G5(t, e2, n2) {
    n2 = Math.min(t.length, n2);
    let r2 = [], o = e2;
    for (; o < n2; ) {
      let f = t[o], u4 = null, l2 = f > 239 ? 4 : f > 223 ? 3 : f > 191 ? 2 : 1;
      if (o + l2 <= n2) {
        let w3, p4, a3, y4;
        switch (l2) {
          case 1:
            f < 128 && (u4 = f);
            break;
          case 2:
            w3 = t[o + 1], (w3 & 192) === 128 && (y4 = (f & 31) << 6 | w3 & 63, y4 > 127 && (u4 = y4));
            break;
          case 3:
            w3 = t[o + 1], p4 = t[o + 2], (w3 & 192) === 128 && (p4 & 192) === 128 && (y4 = (f & 15) << 12 | (w3 & 63) << 6 | p4 & 63, y4 > 2047 && (y4 < 55296 || y4 > 57343) && (u4 = y4));
            break;
          case 4:
            w3 = t[o + 1], p4 = t[o + 2], a3 = t[o + 3], (w3 & 192) === 128 && (p4 & 192) === 128 && (a3 & 192) === 128 && (y4 = (f & 15) << 18 | (w3 & 63) << 12 | (p4 & 63) << 6 | a3 & 63, y4 > 65535 && y4 < 1114112 && (u4 = y4));
        }
      }
      u4 === null ? (u4 = 65533, l2 = 1) : u4 > 65535 && (u4 -= 65536, r2.push(u4 >>> 10 & 1023 | 55296), u4 = 56320 | u4 & 1023), r2.push(u4), o += l2;
    }
    return Bt2(r2);
  }
  var X3 = 4096;
  function Bt2(t) {
    let e2 = t.length;
    if (e2 <= X3)
      return String.fromCharCode.apply(String, t);
    let n2 = "", r2 = 0;
    for (; r2 < e2; )
      n2 += String.fromCharCode.apply(String, t.slice(r2, r2 += X3));
    return n2;
  }
  function mt3(t, e2, n2) {
    let r2 = "";
    n2 = Math.min(t.length, n2);
    for (let o = e2; o < n2; ++o)
      r2 += String.fromCharCode(t[o] & 127);
    return r2;
  }
  function Et2(t, e2, n2) {
    let r2 = "";
    n2 = Math.min(t.length, n2);
    for (let o = e2; o < n2; ++o)
      r2 += String.fromCharCode(t[o]);
    return r2;
  }
  function It2(t, e2, n2) {
    let r2 = t.length;
    (!e2 || e2 < 0) && (e2 = 0), (!n2 || n2 < 0 || n2 > r2) && (n2 = r2);
    let o = "";
    for (let f = e2; f < n2; ++f)
      o += _t3[t[f]];
    return o;
  }
  function vt2(t, e2, n2) {
    let r2 = t.slice(e2, n2), o = "";
    for (let f = 0; f < r2.length - 1; f += 2)
      o += String.fromCharCode(r2[f] + r2[f + 1] * 256);
    return o;
  }
  i4.prototype.slice = function(t, e2) {
    let n2 = this.length;
    t = ~~t, e2 = e2 === void 0 ? n2 : ~~e2, t < 0 ? (t += n2, t < 0 && (t = 0)) : t > n2 && (t = n2), e2 < 0 ? (e2 += n2, e2 < 0 && (e2 = 0)) : e2 > n2 && (e2 = n2), e2 < t && (e2 = t);
    let r2 = this.subarray(t, e2);
    return Object.setPrototypeOf(r2, i4.prototype), r2;
  };
  function g3(t, e2, n2) {
    if (t % 1 !== 0 || t < 0)
      throw new RangeError("offset is not uint");
    if (t + e2 > n2)
      throw new RangeError("Trying to access beyond buffer length");
  }
  i4.prototype.readUintLE = i4.prototype.readUIntLE = function(t, e2, n2) {
    t = t >>> 0, e2 = e2 >>> 0, n2 || g3(t, e2, this.length);
    let r2 = this[t], o = 1, f = 0;
    for (; ++f < e2 && (o *= 256); )
      r2 += this[t + f] * o;
    return r2;
  }, i4.prototype.readUintBE = i4.prototype.readUIntBE = function(t, e2, n2) {
    t = t >>> 0, e2 = e2 >>> 0, n2 || g3(t, e2, this.length);
    let r2 = this[t + --e2], o = 1;
    for (; e2 > 0 && (o *= 256); )
      r2 += this[t + --e2] * o;
    return r2;
  }, i4.prototype.readUint8 = i4.prototype.readUInt8 = function(t, e2) {
    return t = t >>> 0, e2 || g3(t, 1, this.length), this[t];
  }, i4.prototype.readUint16LE = i4.prototype.readUInt16LE = function(t, e2) {
    return t = t >>> 0, e2 || g3(t, 2, this.length), this[t] | this[t + 1] << 8;
  }, i4.prototype.readUint16BE = i4.prototype.readUInt16BE = function(t, e2) {
    return t = t >>> 0, e2 || g3(t, 2, this.length), this[t] << 8 | this[t + 1];
  }, i4.prototype.readUint32LE = i4.prototype.readUInt32LE = function(t, e2) {
    return t = t >>> 0, e2 || g3(t, 4, this.length), (this[t] | this[t + 1] << 8 | this[t + 2] << 16) + this[t + 3] * 16777216;
  }, i4.prototype.readUint32BE = i4.prototype.readUInt32BE = function(t, e2) {
    return t = t >>> 0, e2 || g3(t, 4, this.length), this[t] * 16777216 + (this[t + 1] << 16 | this[t + 2] << 8 | this[t + 3]);
  }, i4.prototype.readBigUInt64LE = E4(function(t) {
    t = t >>> 0, U3(t, "offset");
    let e2 = this[t], n2 = this[t + 7];
    (e2 === void 0 || n2 === void 0) && O3(t, this.length - 8);
    let r2 = e2 + this[++t] * 2 ** 8 + this[++t] * 2 ** 16 + this[++t] * 2 ** 24, o = this[++t] + this[++t] * 2 ** 8 + this[++t] * 2 ** 16 + n2 * 2 ** 24;
    return BigInt(r2) + (BigInt(o) << BigInt(32));
  }), i4.prototype.readBigUInt64BE = E4(function(t) {
    t = t >>> 0, U3(t, "offset");
    let e2 = this[t], n2 = this[t + 7];
    (e2 === void 0 || n2 === void 0) && O3(t, this.length - 8);
    let r2 = e2 * 2 ** 24 + this[++t] * 2 ** 16 + this[++t] * 2 ** 8 + this[++t], o = this[++t] * 2 ** 24 + this[++t] * 2 ** 16 + this[++t] * 2 ** 8 + n2;
    return (BigInt(r2) << BigInt(32)) + BigInt(o);
  }), i4.prototype.readIntLE = function(t, e2, n2) {
    t = t >>> 0, e2 = e2 >>> 0, n2 || g3(t, e2, this.length);
    let r2 = this[t], o = 1, f = 0;
    for (; ++f < e2 && (o *= 256); )
      r2 += this[t + f] * o;
    return o *= 128, r2 >= o && (r2 -= Math.pow(2, 8 * e2)), r2;
  }, i4.prototype.readIntBE = function(t, e2, n2) {
    t = t >>> 0, e2 = e2 >>> 0, n2 || g3(t, e2, this.length);
    let r2 = e2, o = 1, f = this[t + --r2];
    for (; r2 > 0 && (o *= 256); )
      f += this[t + --r2] * o;
    return o *= 128, f >= o && (f -= Math.pow(2, 8 * e2)), f;
  }, i4.prototype.readInt8 = function(t, e2) {
    return t = t >>> 0, e2 || g3(t, 1, this.length), this[t] & 128 ? (255 - this[t] + 1) * -1 : this[t];
  }, i4.prototype.readInt16LE = function(t, e2) {
    t = t >>> 0, e2 || g3(t, 2, this.length);
    let n2 = this[t] | this[t + 1] << 8;
    return n2 & 32768 ? n2 | 4294901760 : n2;
  }, i4.prototype.readInt16BE = function(t, e2) {
    t = t >>> 0, e2 || g3(t, 2, this.length);
    let n2 = this[t + 1] | this[t] << 8;
    return n2 & 32768 ? n2 | 4294901760 : n2;
  }, i4.prototype.readInt32LE = function(t, e2) {
    return t = t >>> 0, e2 || g3(t, 4, this.length), this[t] | this[t + 1] << 8 | this[t + 2] << 16 | this[t + 3] << 24;
  }, i4.prototype.readInt32BE = function(t, e2) {
    return t = t >>> 0, e2 || g3(t, 4, this.length), this[t] << 24 | this[t + 1] << 16 | this[t + 2] << 8 | this[t + 3];
  }, i4.prototype.readBigInt64LE = E4(function(t) {
    t = t >>> 0, U3(t, "offset");
    let e2 = this[t], n2 = this[t + 7];
    (e2 === void 0 || n2 === void 0) && O3(t, this.length - 8);
    let r2 = this[t + 4] + this[t + 5] * 2 ** 8 + this[t + 6] * 2 ** 16 + (n2 << 24);
    return (BigInt(r2) << BigInt(32)) + BigInt(e2 + this[++t] * 2 ** 8 + this[++t] * 2 ** 16 + this[++t] * 2 ** 24);
  }), i4.prototype.readBigInt64BE = E4(function(t) {
    t = t >>> 0, U3(t, "offset");
    let e2 = this[t], n2 = this[t + 7];
    (e2 === void 0 || n2 === void 0) && O3(t, this.length - 8);
    let r2 = (e2 << 24) + this[++t] * 2 ** 16 + this[++t] * 2 ** 8 + this[++t];
    return (BigInt(r2) << BigInt(32)) + BigInt(this[++t] * 2 ** 24 + this[++t] * 2 ** 16 + this[++t] * 2 ** 8 + n2);
  }), i4.prototype.readFloatLE = function(t, e2) {
    return t = t >>> 0, e2 || g3(t, 4, this.length), c3.read(this, t, true, 23, 4);
  }, i4.prototype.readFloatBE = function(t, e2) {
    return t = t >>> 0, e2 || g3(t, 4, this.length), c3.read(this, t, false, 23, 4);
  }, i4.prototype.readDoubleLE = function(t, e2) {
    return t = t >>> 0, e2 || g3(t, 8, this.length), c3.read(this, t, true, 52, 8);
  }, i4.prototype.readDoubleBE = function(t, e2) {
    return t = t >>> 0, e2 || g3(t, 8, this.length), c3.read(this, t, false, 52, 8);
  };
  function d3(t, e2, n2, r2, o, f) {
    if (!i4.isBuffer(t))
      throw new TypeError('"buffer" argument must be a Buffer instance');
    if (e2 > o || e2 < f)
      throw new RangeError('"value" argument is out of bounds');
    if (n2 + r2 > t.length)
      throw new RangeError("Index out of range");
  }
  i4.prototype.writeUintLE = i4.prototype.writeUIntLE = function(t, e2, n2, r2) {
    if (t = +t, e2 = e2 >>> 0, n2 = n2 >>> 0, !r2) {
      let u4 = Math.pow(2, 8 * n2) - 1;
      d3(this, t, e2, n2, u4, 0);
    }
    let o = 1, f = 0;
    for (this[e2] = t & 255; ++f < n2 && (o *= 256); )
      this[e2 + f] = t / o & 255;
    return e2 + n2;
  }, i4.prototype.writeUintBE = i4.prototype.writeUIntBE = function(t, e2, n2, r2) {
    if (t = +t, e2 = e2 >>> 0, n2 = n2 >>> 0, !r2) {
      let u4 = Math.pow(2, 8 * n2) - 1;
      d3(this, t, e2, n2, u4, 0);
    }
    let o = n2 - 1, f = 1;
    for (this[e2 + o] = t & 255; --o >= 0 && (f *= 256); )
      this[e2 + o] = t / f & 255;
    return e2 + n2;
  }, i4.prototype.writeUint8 = i4.prototype.writeUInt8 = function(t, e2, n2) {
    return t = +t, e2 = e2 >>> 0, n2 || d3(this, t, e2, 1, 255, 0), this[e2] = t & 255, e2 + 1;
  }, i4.prototype.writeUint16LE = i4.prototype.writeUInt16LE = function(t, e2, n2) {
    return t = +t, e2 = e2 >>> 0, n2 || d3(this, t, e2, 2, 65535, 0), this[e2] = t & 255, this[e2 + 1] = t >>> 8, e2 + 2;
  }, i4.prototype.writeUint16BE = i4.prototype.writeUInt16BE = function(t, e2, n2) {
    return t = +t, e2 = e2 >>> 0, n2 || d3(this, t, e2, 2, 65535, 0), this[e2] = t >>> 8, this[e2 + 1] = t & 255, e2 + 2;
  }, i4.prototype.writeUint32LE = i4.prototype.writeUInt32LE = function(t, e2, n2) {
    return t = +t, e2 = e2 >>> 0, n2 || d3(this, t, e2, 4, 4294967295, 0), this[e2 + 3] = t >>> 24, this[e2 + 2] = t >>> 16, this[e2 + 1] = t >>> 8, this[e2] = t & 255, e2 + 4;
  }, i4.prototype.writeUint32BE = i4.prototype.writeUInt32BE = function(t, e2, n2) {
    return t = +t, e2 = e2 >>> 0, n2 || d3(this, t, e2, 4, 4294967295, 0), this[e2] = t >>> 24, this[e2 + 1] = t >>> 16, this[e2 + 2] = t >>> 8, this[e2 + 3] = t & 255, e2 + 4;
  };
  function V2(t, e2, n2, r2, o) {
    Q3(e2, r2, o, t, n2, 7);
    let f = Number(e2 & BigInt(4294967295));
    t[n2++] = f, f = f >> 8, t[n2++] = f, f = f >> 8, t[n2++] = f, f = f >> 8, t[n2++] = f;
    let u4 = Number(e2 >> BigInt(32) & BigInt(4294967295));
    return t[n2++] = u4, u4 = u4 >> 8, t[n2++] = u4, u4 = u4 >> 8, t[n2++] = u4, u4 = u4 >> 8, t[n2++] = u4, n2;
  }
  function W4(t, e2, n2, r2, o) {
    Q3(e2, r2, o, t, n2, 7);
    let f = Number(e2 & BigInt(4294967295));
    t[n2 + 7] = f, f = f >> 8, t[n2 + 6] = f, f = f >> 8, t[n2 + 5] = f, f = f >> 8, t[n2 + 4] = f;
    let u4 = Number(e2 >> BigInt(32) & BigInt(4294967295));
    return t[n2 + 3] = u4, u4 = u4 >> 8, t[n2 + 2] = u4, u4 = u4 >> 8, t[n2 + 1] = u4, u4 = u4 >> 8, t[n2] = u4, n2 + 8;
  }
  i4.prototype.writeBigUInt64LE = E4(function(t, e2 = 0) {
    return V2(this, t, e2, BigInt(0), BigInt("0xffffffffffffffff"));
  }), i4.prototype.writeBigUInt64BE = E4(function(t, e2 = 0) {
    return W4(this, t, e2, BigInt(0), BigInt("0xffffffffffffffff"));
  }), i4.prototype.writeIntLE = function(t, e2, n2, r2) {
    if (t = +t, e2 = e2 >>> 0, !r2) {
      let l2 = Math.pow(2, 8 * n2 - 1);
      d3(this, t, e2, n2, l2 - 1, -l2);
    }
    let o = 0, f = 1, u4 = 0;
    for (this[e2] = t & 255; ++o < n2 && (f *= 256); )
      t < 0 && u4 === 0 && this[e2 + o - 1] !== 0 && (u4 = 1), this[e2 + o] = (t / f >> 0) - u4 & 255;
    return e2 + n2;
  }, i4.prototype.writeIntBE = function(t, e2, n2, r2) {
    if (t = +t, e2 = e2 >>> 0, !r2) {
      let l2 = Math.pow(2, 8 * n2 - 1);
      d3(this, t, e2, n2, l2 - 1, -l2);
    }
    let o = n2 - 1, f = 1, u4 = 0;
    for (this[e2 + o] = t & 255; --o >= 0 && (f *= 256); )
      t < 0 && u4 === 0 && this[e2 + o + 1] !== 0 && (u4 = 1), this[e2 + o] = (t / f >> 0) - u4 & 255;
    return e2 + n2;
  }, i4.prototype.writeInt8 = function(t, e2, n2) {
    return t = +t, e2 = e2 >>> 0, n2 || d3(this, t, e2, 1, 127, -128), t < 0 && (t = 255 + t + 1), this[e2] = t & 255, e2 + 1;
  }, i4.prototype.writeInt16LE = function(t, e2, n2) {
    return t = +t, e2 = e2 >>> 0, n2 || d3(this, t, e2, 2, 32767, -32768), this[e2] = t & 255, this[e2 + 1] = t >>> 8, e2 + 2;
  }, i4.prototype.writeInt16BE = function(t, e2, n2) {
    return t = +t, e2 = e2 >>> 0, n2 || d3(this, t, e2, 2, 32767, -32768), this[e2] = t >>> 8, this[e2 + 1] = t & 255, e2 + 2;
  }, i4.prototype.writeInt32LE = function(t, e2, n2) {
    return t = +t, e2 = e2 >>> 0, n2 || d3(this, t, e2, 4, 2147483647, -2147483648), this[e2] = t & 255, this[e2 + 1] = t >>> 8, this[e2 + 2] = t >>> 16, this[e2 + 3] = t >>> 24, e2 + 4;
  }, i4.prototype.writeInt32BE = function(t, e2, n2) {
    return t = +t, e2 = e2 >>> 0, n2 || d3(this, t, e2, 4, 2147483647, -2147483648), t < 0 && (t = 4294967295 + t + 1), this[e2] = t >>> 24, this[e2 + 1] = t >>> 16, this[e2 + 2] = t >>> 8, this[e2 + 3] = t & 255, e2 + 4;
  }, i4.prototype.writeBigInt64LE = E4(function(t, e2 = 0) {
    return V2(this, t, e2, -BigInt("0x8000000000000000"), BigInt("0x7fffffffffffffff"));
  }), i4.prototype.writeBigInt64BE = E4(function(t, e2 = 0) {
    return W4(this, t, e2, -BigInt("0x8000000000000000"), BigInt("0x7fffffffffffffff"));
  });
  function J5(t, e2, n2, r2, o, f) {
    if (n2 + r2 > t.length)
      throw new RangeError("Index out of range");
    if (n2 < 0)
      throw new RangeError("Index out of range");
  }
  function Z2(t, e2, n2, r2, o) {
    return e2 = +e2, n2 = n2 >>> 0, o || J5(t, e2, n2, 4, 34028234663852886e22, -34028234663852886e22), c3.write(t, e2, n2, r2, 23, 4), n2 + 4;
  }
  i4.prototype.writeFloatLE = function(t, e2, n2) {
    return Z2(this, t, e2, true, n2);
  }, i4.prototype.writeFloatBE = function(t, e2, n2) {
    return Z2(this, t, e2, false, n2);
  };
  function H5(t, e2, n2, r2, o) {
    return e2 = +e2, n2 = n2 >>> 0, o || J5(t, e2, n2, 8, 17976931348623157e292, -17976931348623157e292), c3.write(t, e2, n2, r2, 52, 8), n2 + 8;
  }
  i4.prototype.writeDoubleLE = function(t, e2, n2) {
    return H5(this, t, e2, true, n2);
  }, i4.prototype.writeDoubleBE = function(t, e2, n2) {
    return H5(this, t, e2, false, n2);
  }, i4.prototype.copy = function(t, e2, n2, r2) {
    if (!i4.isBuffer(t))
      throw new TypeError("argument should be a Buffer");
    if (n2 || (n2 = 0), !r2 && r2 !== 0 && (r2 = this.length), e2 >= t.length && (e2 = t.length), e2 || (e2 = 0), r2 > 0 && r2 < n2 && (r2 = n2), r2 === n2 || t.length === 0 || this.length === 0)
      return 0;
    if (e2 < 0)
      throw new RangeError("targetStart out of bounds");
    if (n2 < 0 || n2 >= this.length)
      throw new RangeError("Index out of range");
    if (r2 < 0)
      throw new RangeError("sourceEnd out of bounds");
    r2 > this.length && (r2 = this.length), t.length - e2 < r2 - n2 && (r2 = t.length - e2 + n2);
    let o = r2 - n2;
    return this === t && typeof Uint8Array.prototype.copyWithin == "function" ? this.copyWithin(e2, n2, r2) : Uint8Array.prototype.set.call(t, this.subarray(n2, r2), e2), o;
  }, i4.prototype.fill = function(t, e2, n2, r2) {
    if (typeof t == "string") {
      if (typeof e2 == "string" ? (r2 = e2, e2 = 0, n2 = this.length) : typeof n2 == "string" && (r2 = n2, n2 = this.length), r2 !== void 0 && typeof r2 != "string")
        throw new TypeError("encoding must be a string");
      if (typeof r2 == "string" && !i4.isEncoding(r2))
        throw new TypeError("Unknown encoding: " + r2);
      if (t.length === 1) {
        let f = t.charCodeAt(0);
        (r2 === "utf8" && f < 128 || r2 === "latin1") && (t = f);
      }
    } else
      typeof t == "number" ? t = t & 255 : typeof t == "boolean" && (t = Number(t));
    if (e2 < 0 || this.length < e2 || this.length < n2)
      throw new RangeError("Out of range index");
    if (n2 <= e2)
      return this;
    e2 = e2 >>> 0, n2 = n2 === void 0 ? this.length : n2 >>> 0, t || (t = 0);
    let o;
    if (typeof t == "number")
      for (o = e2; o < n2; ++o)
        this[o] = t;
    else {
      let f = i4.isBuffer(t) ? t : i4.from(t, r2), u4 = f.length;
      if (u4 === 0)
        throw new TypeError('The value "' + t + '" is invalid for argument "value"');
      for (o = 0; o < n2 - e2; ++o)
        this[o + e2] = f[o % u4];
    }
    return this;
  };
  var A4 = {};
  function x4(t, e2, n2) {
    A4[t] = class extends n2 {
      constructor() {
        super(), Object.defineProperty(this, "message", { value: e2.apply(this, arguments), writable: true, configurable: true }), this.name = `${this.name} [${t}]`, this.stack, delete this.name;
      }
      get code() {
        return t;
      }
      set code(r2) {
        Object.defineProperty(this, "code", { configurable: true, enumerable: true, value: r2, writable: true });
      }
      toString() {
        return `${this.name} [${t}]: ${this.message}`;
      }
    };
  }
  x4("ERR_BUFFER_OUT_OF_BOUNDS", function(t) {
    return t ? `${t} is outside of buffer bounds` : "Attempt to access memory outside buffer bounds";
  }, RangeError), x4("ERR_INVALID_ARG_TYPE", function(t, e2) {
    return `The "${t}" argument must be of type number. Received type ${typeof e2}`;
  }, TypeError), x4("ERR_OUT_OF_RANGE", function(t, e2, n2) {
    let r2 = `The value of "${t}" is out of range.`, o = n2;
    return Number.isInteger(n2) && Math.abs(n2) > 2 ** 32 ? o = K4(String(n2)) : typeof n2 == "bigint" && (o = String(n2), (n2 > BigInt(2) ** BigInt(32) || n2 < -(BigInt(2) ** BigInt(32))) && (o = K4(o)), o += "n"), r2 += ` It must be ${e2}. Received ${o}`, r2;
  }, RangeError);
  function K4(t) {
    let e2 = "", n2 = t.length, r2 = t[0] === "-" ? 1 : 0;
    for (; n2 >= r2 + 4; n2 -= 3)
      e2 = `_${t.slice(n2 - 3, n2)}${e2}`;
    return `${t.slice(0, n2)}${e2}`;
  }
  function At2(t, e2, n2) {
    U3(e2, "offset"), (t[e2] === void 0 || t[e2 + n2] === void 0) && O3(e2, t.length - (n2 + 1));
  }
  function Q3(t, e2, n2, r2, o, f) {
    if (t > n2 || t < e2) {
      let u4 = typeof e2 == "bigint" ? "n" : "", l2;
      throw f > 3 ? e2 === 0 || e2 === BigInt(0) ? l2 = `>= 0${u4} and < 2${u4} ** ${(f + 1) * 8}${u4}` : l2 = `>= -(2${u4} ** ${(f + 1) * 8 - 1}${u4}) and < 2 ** ${(f + 1) * 8 - 1}${u4}` : l2 = `>= ${e2}${u4} and <= ${n2}${u4}`, new A4.ERR_OUT_OF_RANGE("value", l2, t);
    }
    At2(r2, o, f);
  }
  function U3(t, e2) {
    if (typeof t != "number")
      throw new A4.ERR_INVALID_ARG_TYPE(e2, "number", t);
  }
  function O3(t, e2, n2) {
    throw Math.floor(t) !== t ? (U3(t, n2), new A4.ERR_OUT_OF_RANGE(n2 || "offset", "an integer", t)) : e2 < 0 ? new A4.ERR_BUFFER_OUT_OF_BOUNDS() : new A4.ERR_OUT_OF_RANGE(n2 || "offset", `>= ${n2 ? 1 : 0} and <= ${e2}`, t);
  }
  var Ut = /[^+/0-9A-Za-z-_]/g;
  function Rt2(t) {
    if (t = t.split("=")[0], t = t.trim().replace(Ut, ""), t.length < 2)
      return "";
    for (; t.length % 4 !== 0; )
      t = t + "=";
    return t;
  }
  function $2(t, e2) {
    e2 = e2 || 1 / 0;
    let n2, r2 = t.length, o = null, f = [];
    for (let u4 = 0; u4 < r2; ++u4) {
      if (n2 = t.charCodeAt(u4), n2 > 55295 && n2 < 57344) {
        if (!o) {
          if (n2 > 56319) {
            (e2 -= 3) > -1 && f.push(239, 191, 189);
            continue;
          } else if (u4 + 1 === r2) {
            (e2 -= 3) > -1 && f.push(239, 191, 189);
            continue;
          }
          o = n2;
          continue;
        }
        if (n2 < 56320) {
          (e2 -= 3) > -1 && f.push(239, 191, 189), o = n2;
          continue;
        }
        n2 = (o - 55296 << 10 | n2 - 56320) + 65536;
      } else
        o && (e2 -= 3) > -1 && f.push(239, 191, 189);
      if (o = null, n2 < 128) {
        if ((e2 -= 1) < 0)
          break;
        f.push(n2);
      } else if (n2 < 2048) {
        if ((e2 -= 2) < 0)
          break;
        f.push(n2 >> 6 | 192, n2 & 63 | 128);
      } else if (n2 < 65536) {
        if ((e2 -= 3) < 0)
          break;
        f.push(n2 >> 12 | 224, n2 >> 6 & 63 | 128, n2 & 63 | 128);
      } else if (n2 < 1114112) {
        if ((e2 -= 4) < 0)
          break;
        f.push(n2 >> 18 | 240, n2 >> 12 & 63 | 128, n2 >> 6 & 63 | 128, n2 & 63 | 128);
      } else
        throw new Error("Invalid code point");
    }
    return f;
  }
  function Ot2(t) {
    let e2 = [];
    for (let n2 = 0; n2 < t.length; ++n2)
      e2.push(t.charCodeAt(n2) & 255);
    return e2;
  }
  function Tt3(t, e2) {
    let n2, r2, o, f = [];
    for (let u4 = 0; u4 < t.length && !((e2 -= 2) < 0); ++u4)
      n2 = t.charCodeAt(u4), r2 = n2 >> 8, o = n2 % 256, f.push(o), f.push(r2);
    return f;
  }
  function tt3(t) {
    return h2.toByteArray(Rt2(t));
  }
  function T3(t, e2, n2, r2) {
    let o;
    for (o = 0; o < r2 && !(o + n2 >= e2.length || o >= t.length); ++o)
      e2[o + n2] = t[o];
    return o;
  }
  function B4(t, e2) {
    return t instanceof e2 || t != null && t.constructor != null && t.constructor.name != null && t.constructor.name === e2.name;
  }
  function P4(t) {
    return t !== t;
  }
  var _t3 = function() {
    let t = "0123456789abcdef", e2 = new Array(256);
    for (let n2 = 0; n2 < 16; ++n2) {
      let r2 = n2 * 16;
      for (let o = 0; o < 16; ++o)
        e2[r2 + o] = t[n2] + t[o];
    }
    return e2;
  }();
  function E4(t) {
    return typeof BigInt > "u" ? St3 : t;
  }
  function St3() {
    throw new Error("BigInt not supported");
  }
});
var ot = {};
jt(ot, { Buffer: () => Dt, INSPECT_MAX_BYTES: () => qt, SlowBuffer: () => Yt, default: () => Xt, kMaxLength: () => zt });
var it = nt(rt());
Ft(ot, nt(rt()));
var { Buffer: Dt, SlowBuffer: Yt, INSPECT_MAX_BYTES: qt, kMaxLength: zt } = it;
var { default: et, ...Gt } = it;
var Xt = et !== void 0 ? et : Gt;

// https://esm.sh/v124/cbor-x@1.5.3/es2022/cbor-x.mjs
var Ue;
try {
  Ue = new TextDecoder();
} catch {
}
var y2;
var v3;
var a = 0;
var ve = [];
var ut = 105;
var dt = 57342;
var xt2 = 57343;
var qe = 57337;
var Ze = 6;
var re = {};
var Me = ve;
var _e = 0;
var E = {};
var _3;
var ye;
var pe = 0;
var ae = 0;
var T;
var W;
var R = [];
var Re = [];
var P;
var C4;
var oe;
var Xe = { useRecords: false, mapsAsObjects: true };
var ce = false;
var et2 = 2;
try {
  new Function("");
} catch {
  et2 = 1 / 0;
}
var Y = class {
  constructor(t) {
    if (t && ((t.keyMap || t._keyMap) && !t.useRecords && (t.useRecords = false, t.mapsAsObjects = true), t.useRecords === false && t.mapsAsObjects === void 0 && (t.mapsAsObjects = true), t.getStructures && (t.getShared = t.getStructures), t.getShared && !t.structures && ((t.structures = []).uninitialized = true), t.keyMap)) {
      this.mapKey = /* @__PURE__ */ new Map();
      for (let [l2, n2] of Object.entries(t.keyMap))
        this.mapKey.set(n2, l2);
    }
    Object.assign(this, t);
  }
  decodeKey(t) {
    return this.keyMap && this.mapKey.get(t) || t;
  }
  encodeKey(t) {
    return this.keyMap && this.keyMap.hasOwnProperty(t) ? this.keyMap[t] : t;
  }
  encodeKeys(t) {
    if (!this._keyMap)
      return t;
    let l2 = /* @__PURE__ */ new Map();
    for (let [n2, f] of Object.entries(t))
      l2.set(this._keyMap.hasOwnProperty(n2) ? this._keyMap[n2] : n2, f);
    return l2;
  }
  decodeKeys(t) {
    if (!this._keyMap || t.constructor.name != "Map")
      return t;
    if (!this._mapKey) {
      this._mapKey = /* @__PURE__ */ new Map();
      for (let [n2, f] of Object.entries(this._keyMap))
        this._mapKey.set(f, n2);
    }
    let l2 = {};
    return t.forEach((n2, f) => l2[j3(this._mapKey.has(f) ? this._mapKey.get(f) : f)] = n2), l2;
  }
  mapDecode(t, l2) {
    let n2 = this.decode(t);
    if (this._keyMap)
      switch (n2.constructor.name) {
        case "Array":
          return n2.map((f) => this.decodeKeys(f));
      }
    return n2;
  }
  decode(t, l2) {
    if (y2)
      return nt2(() => (ge(), this ? this.decode(t, l2) : Y.prototype.decode.call(Xe, t, l2)));
    v3 = l2 > -1 ? l2 : t.length, a = 0, _e = 0, ae = 0, ye = null, Me = ve, T = null, y2 = t;
    try {
      C4 = t.dataView || (t.dataView = new DataView(t.buffer, t.byteOffset, t.byteLength));
    } catch (n2) {
      throw y2 = null, t instanceof Uint8Array ? n2 : new Error("Source must be a Uint8Array or Buffer but was a " + (t && typeof t == "object" ? t.constructor.name : typeof t));
    }
    if (this instanceof Y) {
      if (E = this, P = this.sharedValues && (this.pack ? new Array(this.maxPrivatePackedValues || 16).concat(this.sharedValues) : this.sharedValues), this.structures)
        return _3 = this.structures, he();
      (!_3 || _3.length > 0) && (_3 = []);
    } else
      E = Xe, (!_3 || _3.length > 0) && (_3 = []), P = null;
    return he();
  }
  decodeMultiple(t, l2) {
    let n2, f = 0;
    try {
      let o = t.length;
      ce = true;
      let d3 = this ? this.decode(t, o) : Ce.decode(t, o);
      if (l2) {
        if (l2(d3) === false)
          return;
        for (; a < o; )
          if (f = a, l2(he()) === false)
            return;
      } else {
        for (n2 = [d3]; a < o; )
          f = a, n2.push(he());
        return n2;
      }
    } catch (o) {
      throw o.lastPosition = f, o.values = n2, o;
    } finally {
      ce = false, ge();
    }
  }
};
function he() {
  try {
    let e2 = S();
    if (T) {
      if (a >= T.postBundlePosition) {
        let t = new Error("Unexpected bundle position");
        throw t.incomplete = true, t;
      }
      a = T.postBundlePosition, T = null;
    }
    if (a == v3)
      _3 = null, y2 = null, W && (W = null);
    else if (a > v3) {
      let t = new Error("Unexpected end of CBOR data");
      throw t.incomplete = true, t;
    } else if (!ce)
      throw new Error("Data read, but end of buffer not reached");
    return e2;
  } catch (e2) {
    throw ge(), (e2 instanceof RangeError || e2.message.startsWith("Unexpected end of buffer")) && (e2.incomplete = true), e2;
  }
}
function S() {
  let e2 = y2[a++], t = e2 >> 5;
  if (e2 = e2 & 31, e2 > 23)
    switch (e2) {
      case 24:
        e2 = y2[a++];
        break;
      case 25:
        if (t == 7)
          return wt();
        e2 = C4.getUint16(a), a += 2;
        break;
      case 26:
        if (t == 7) {
          let l2 = C4.getFloat32(a);
          if (E.useFloat32 > 2) {
            let n2 = ue[(y2[a] & 127) << 1 | y2[a + 1] >> 7];
            return a += 4, (n2 * l2 + (l2 > 0 ? 0.5 : -0.5) >> 0) / n2;
          }
          return a += 4, l2;
        }
        e2 = C4.getUint32(a), a += 4;
        break;
      case 27:
        if (t == 7) {
          let l2 = C4.getFloat64(a);
          return a += 8, l2;
        }
        if (t > 1) {
          if (C4.getUint32(a) > 0)
            throw new Error("JavaScript does not support arrays, maps, or strings with length over 4294967295");
          e2 = C4.getUint32(a + 4);
        } else
          E.int64AsNumber ? (e2 = C4.getUint32(a) * 4294967296, e2 += C4.getUint32(a + 4)) : e2 = C4.getBigUint64(a);
        a += 8;
        break;
      case 31:
        switch (t) {
          case 2:
          case 3:
            throw new Error("Indefinite length not supported for byte or text strings");
          case 4:
            let l2 = [], n2, f = 0;
            for (; (n2 = S()) != re; )
              l2[f++] = n2;
            return t == 4 ? l2 : t == 3 ? l2.join("") : Dt.concat(l2);
          case 5:
            let o;
            if (E.mapsAsObjects) {
              let d3 = {};
              if (E.keyMap)
                for (; (o = S()) != re; )
                  d3[j3(E.decodeKey(o))] = S();
              else
                for (; (o = S()) != re; )
                  d3[j3(o)] = S();
              return d3;
            } else {
              oe && (E.mapsAsObjects = true, oe = false);
              let d3 = /* @__PURE__ */ new Map();
              if (E.keyMap)
                for (; (o = S()) != re; )
                  d3.set(E.decodeKey(o), S());
              else
                for (; (o = S()) != re; )
                  d3.set(o, S());
              return d3;
            }
          case 7:
            return re;
          default:
            throw new Error("Invalid major type for indefinite length " + t);
        }
      default:
        throw new Error("Unknown token " + e2);
    }
  switch (t) {
    case 0:
      return e2;
    case 1:
      return ~e2;
    case 2:
      return gt(e2);
    case 3:
      if (ae >= a)
        return ye.slice(a - pe, (a += e2) - pe);
      if (ae == 0 && v3 < 140 && e2 < 32) {
        let f = e2 < 16 ? tt(e2) : pt(e2);
        if (f != null)
          return f;
      }
      return ht(e2);
    case 4:
      let l2 = new Array(e2);
      for (let f = 0; f < e2; f++)
        l2[f] = S();
      return l2;
    case 5:
      if (E.mapsAsObjects) {
        let f = {};
        if (E.keyMap)
          for (let o = 0; o < e2; o++)
            f[j3(E.decodeKey(S()))] = S();
        else
          for (let o = 0; o < e2; o++)
            f[j3(S())] = S();
        return f;
      } else {
        oe && (E.mapsAsObjects = true, oe = false);
        let f = /* @__PURE__ */ new Map();
        if (E.keyMap)
          for (let o = 0; o < e2; o++)
            f.set(E.decodeKey(S()), S());
        else
          for (let o = 0; o < e2; o++)
            f.set(S(), S());
        return f;
      }
    case 6:
      if (e2 >= qe) {
        let f = _3[e2 & 8191];
        if (f)
          return f.read || (f.read = Be(f)), f.read();
        if (e2 < 65536) {
          if (e2 == xt2) {
            let o = se(), d3 = S(), w3 = S();
            De(d3, w3);
            let U3 = {};
            if (E.keyMap)
              for (let p4 = 2; p4 < o; p4++) {
                let B4 = E.decodeKey(w3[p4 - 2]);
                U3[j3(B4)] = S();
              }
            else
              for (let p4 = 2; p4 < o; p4++) {
                let B4 = w3[p4 - 2];
                U3[j3(B4)] = S();
              }
            return U3;
          } else if (e2 == dt) {
            let o = se(), d3 = S();
            for (let w3 = 2; w3 < o; w3++)
              De(d3++, S());
            return S();
          } else if (e2 == qe)
            return It();
          if (E.getShared && (Ve(), f = _3[e2 & 8191], f))
            return f.read || (f.read = Be(f)), f.read();
        }
      }
      let n2 = R[e2];
      if (n2)
        return n2.handlesRead ? n2(S) : n2(S());
      {
        let f = S();
        for (let o = 0; o < Re.length; o++) {
          let d3 = Re[o](e2, f);
          if (d3 !== void 0)
            return d3;
        }
        return new H3(f, e2);
      }
    case 7:
      switch (e2) {
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
          let f = (P || Q())[e2];
          if (f !== void 0)
            return f;
          throw new Error("Unknown token " + e2);
      }
    default:
      if (isNaN(e2)) {
        let f = new Error("Unexpected end of CBOR data");
        throw f.incomplete = true, f;
      }
      throw new Error("Unknown CBOR token " + e2);
  }
}
var $e = /^[a-zA-Z_$][a-zA-Z\d_$]*$/;
function Be(e2) {
  function t() {
    let l2 = y2[a++];
    if (l2 = l2 & 31, l2 > 23)
      switch (l2) {
        case 24:
          l2 = y2[a++];
          break;
        case 25:
          l2 = C4.getUint16(a), a += 2;
          break;
        case 26:
          l2 = C4.getUint32(a), a += 4;
          break;
        default:
          throw new Error("Expected array header, but got " + y2[a - 1]);
      }
    let n2 = this.compiledReader;
    for (; n2; ) {
      if (n2.propertyCount === l2)
        return n2(S);
      n2 = n2.next;
    }
    if (this.slowReads++ >= et2) {
      let o = this.length == l2 ? this : this.slice(0, l2);
      return n2 = E.keyMap ? new Function("r", "return {" + o.map((d3) => E.decodeKey(d3)).map((d3) => $e.test(d3) ? j3(d3) + ":r()" : "[" + JSON.stringify(d3) + "]:r()").join(",") + "}") : new Function("r", "return {" + o.map((d3) => $e.test(d3) ? j3(d3) + ":r()" : "[" + JSON.stringify(d3) + "]:r()").join(",") + "}"), this.compiledReader && (n2.next = this.compiledReader), n2.propertyCount = l2, this.compiledReader = n2, n2(S);
    }
    let f = {};
    if (E.keyMap)
      for (let o = 0; o < l2; o++)
        f[j3(E.decodeKey(this[o]))] = S();
    else
      for (let o = 0; o < l2; o++)
        f[j3(this[o])] = S();
    return f;
  }
  return e2.slowReads = 0, t;
}
function j3(e2) {
  return e2 === "__proto__" ? "__proto_" : e2;
}
var ht = Te;
function Te(e2) {
  let t;
  if (e2 < 16 && (t = tt(e2)))
    return t;
  if (e2 > 64 && Ue)
    return Ue.decode(y2.subarray(a, a += e2));
  let l2 = a + e2, n2 = [];
  for (t = ""; a < l2; ) {
    let f = y2[a++];
    if (!(f & 128))
      n2.push(f);
    else if ((f & 224) === 192) {
      let o = y2[a++] & 63;
      n2.push((f & 31) << 6 | o);
    } else if ((f & 240) === 224) {
      let o = y2[a++] & 63, d3 = y2[a++] & 63;
      n2.push((f & 31) << 12 | o << 6 | d3);
    } else if ((f & 248) === 240) {
      let o = y2[a++] & 63, d3 = y2[a++] & 63, w3 = y2[a++] & 63, U3 = (f & 7) << 18 | o << 12 | d3 << 6 | w3;
      U3 > 65535 && (U3 -= 65536, n2.push(U3 >>> 10 & 1023 | 55296), U3 = 56320 | U3 & 1023), n2.push(U3);
    } else
      n2.push(f);
    n2.length >= 4096 && (t += F2.apply(String, n2), n2.length = 0);
  }
  return n2.length > 0 && (t += F2.apply(String, n2)), t;
}
var F2 = String.fromCharCode;
function pt(e2) {
  let t = a, l2 = new Array(e2);
  for (let n2 = 0; n2 < e2; n2++) {
    let f = y2[a++];
    if ((f & 128) > 0) {
      a = t;
      return;
    }
    l2[n2] = f;
  }
  return F2.apply(String, l2);
}
function tt(e2) {
  if (e2 < 4)
    if (e2 < 2) {
      if (e2 === 0)
        return "";
      {
        let t = y2[a++];
        if ((t & 128) > 1) {
          a -= 1;
          return;
        }
        return F2(t);
      }
    } else {
      let t = y2[a++], l2 = y2[a++];
      if ((t & 128) > 0 || (l2 & 128) > 0) {
        a -= 2;
        return;
      }
      if (e2 < 3)
        return F2(t, l2);
      let n2 = y2[a++];
      if ((n2 & 128) > 0) {
        a -= 3;
        return;
      }
      return F2(t, l2, n2);
    }
  else {
    let t = y2[a++], l2 = y2[a++], n2 = y2[a++], f = y2[a++];
    if ((t & 128) > 0 || (l2 & 128) > 0 || (n2 & 128) > 0 || (f & 128) > 0) {
      a -= 4;
      return;
    }
    if (e2 < 6) {
      if (e2 === 4)
        return F2(t, l2, n2, f);
      {
        let o = y2[a++];
        if ((o & 128) > 0) {
          a -= 5;
          return;
        }
        return F2(t, l2, n2, f, o);
      }
    } else if (e2 < 8) {
      let o = y2[a++], d3 = y2[a++];
      if ((o & 128) > 0 || (d3 & 128) > 0) {
        a -= 6;
        return;
      }
      if (e2 < 7)
        return F2(t, l2, n2, f, o, d3);
      let w3 = y2[a++];
      if ((w3 & 128) > 0) {
        a -= 7;
        return;
      }
      return F2(t, l2, n2, f, o, d3, w3);
    } else {
      let o = y2[a++], d3 = y2[a++], w3 = y2[a++], U3 = y2[a++];
      if ((o & 128) > 0 || (d3 & 128) > 0 || (w3 & 128) > 0 || (U3 & 128) > 0) {
        a -= 8;
        return;
      }
      if (e2 < 10) {
        if (e2 === 8)
          return F2(t, l2, n2, f, o, d3, w3, U3);
        {
          let p4 = y2[a++];
          if ((p4 & 128) > 0) {
            a -= 9;
            return;
          }
          return F2(t, l2, n2, f, o, d3, w3, U3, p4);
        }
      } else if (e2 < 12) {
        let p4 = y2[a++], B4 = y2[a++];
        if ((p4 & 128) > 0 || (B4 & 128) > 0) {
          a -= 10;
          return;
        }
        if (e2 < 11)
          return F2(t, l2, n2, f, o, d3, w3, U3, p4, B4);
        let O3 = y2[a++];
        if ((O3 & 128) > 0) {
          a -= 11;
          return;
        }
        return F2(t, l2, n2, f, o, d3, w3, U3, p4, B4, O3);
      } else {
        let p4 = y2[a++], B4 = y2[a++], O3 = y2[a++], N3 = y2[a++];
        if ((p4 & 128) > 0 || (B4 & 128) > 0 || (O3 & 128) > 0 || (N3 & 128) > 0) {
          a -= 12;
          return;
        }
        if (e2 < 14) {
          if (e2 === 12)
            return F2(t, l2, n2, f, o, d3, w3, U3, p4, B4, O3, N3);
          {
            let V2 = y2[a++];
            if ((V2 & 128) > 0) {
              a -= 13;
              return;
            }
            return F2(t, l2, n2, f, o, d3, w3, U3, p4, B4, O3, N3, V2);
          }
        } else {
          let V2 = y2[a++], K4 = y2[a++];
          if ((V2 & 128) > 0 || (K4 & 128) > 0) {
            a -= 14;
            return;
          }
          if (e2 < 15)
            return F2(t, l2, n2, f, o, d3, w3, U3, p4, B4, O3, N3, V2, K4);
          let q2 = y2[a++];
          if ((q2 & 128) > 0) {
            a -= 15;
            return;
          }
          return F2(t, l2, n2, f, o, d3, w3, U3, p4, B4, O3, N3, V2, K4, q2);
        }
      }
    }
  }
}
function gt(e2) {
  return E.copyBuffers ? Uint8Array.prototype.slice.call(y2, a, a += e2) : y2.subarray(a, a += e2);
}
var Fe = new Float32Array(1);
var ie = new Uint8Array(Fe.buffer, 0, 4);
function wt() {
  let e2 = y2[a++], t = y2[a++], l2 = (e2 & 127) >> 2;
  if (l2 === 31)
    return t || e2 & 3 ? NaN : e2 & 128 ? -1 / 0 : 1 / 0;
  if (l2 === 0) {
    let n2 = ((e2 & 3) << 8 | t) / 16777216;
    return e2 & 128 ? -n2 : n2;
  }
  return ie[3] = e2 & 128 | (l2 >> 1) + 56, ie[2] = (e2 & 7) << 5 | t >> 3, ie[1] = t << 5, ie[0] = 0, Fe[0];
}
var Jt = new Array(4096);
var H3 = class {
  constructor(t, l2) {
    this.value = t, this.tag = l2;
  }
};
R[0] = (e2) => new Date(e2);
R[1] = (e2) => new Date(Math.round(e2 * 1e3));
R[2] = (e2) => {
  let t = BigInt(0);
  for (let l2 = 0, n2 = e2.byteLength; l2 < n2; l2++)
    t = BigInt(e2[l2]) + t << BigInt(8);
  return t;
};
R[3] = (e2) => BigInt(-1) - R[2](e2);
R[4] = (e2) => +(e2[1] + "e" + e2[0]);
R[5] = (e2) => e2[1] * Math.exp(e2[0] * Math.log(2));
var De = (e2, t) => {
  e2 = e2 - 57344;
  let l2 = _3[e2];
  l2 && l2.isShared && ((_3.restoreStructures || (_3.restoreStructures = []))[e2] = l2), _3[e2] = t, t.read = Be(t);
};
R[ut] = (e2) => {
  let t = e2.length, l2 = e2[1];
  De(e2[0], l2);
  let n2 = {};
  for (let f = 2; f < t; f++) {
    let o = l2[f - 2];
    n2[j3(o)] = e2[f];
  }
  return n2;
};
R[14] = (e2) => T ? T[0].slice(T.position0, T.position0 += e2) : new H3(e2, 14);
R[15] = (e2) => T ? T[1].slice(T.position1, T.position1 += e2) : new H3(e2, 15);
var bt = { Error, RegExp };
R[27] = (e2) => (bt[e2[0]] || Error)(e2[1], e2[2]);
var rt2 = (e2) => {
  if (y2[a++] != 132)
    throw new Error("Packed values structure must be followed by a 4 element array");
  let t = e2();
  return P = P ? t.concat(P.slice(t.length)) : t, P.prefixes = e2(), P.suffixes = e2(), e2();
};
rt2.handlesRead = true;
R[51] = rt2;
R[Ze] = (e2) => {
  if (!P)
    if (E.getShared)
      Ve();
    else
      return new H3(e2, Ze);
  if (typeof e2 == "number")
    return P[16 + (e2 >= 0 ? 2 * e2 : -2 * e2 - 1)];
  throw new Error("No support for non-integer packed references yet");
};
R[28] = (e2) => {
  W || (W = /* @__PURE__ */ new Map(), W.id = 0);
  let t = W.id++, l2 = y2[a], n2;
  l2 >> 5 == 4 ? n2 = [] : n2 = {};
  let f = { target: n2 };
  W.set(t, f);
  let o = e2();
  return f.used ? Object.assign(n2, o) : (f.target = o, o);
};
R[28].handlesRead = true;
R[29] = (e2) => {
  let t = W.get(e2);
  return t.used = true, t.target;
};
R[258] = (e2) => new Set(e2);
(R[259] = (e2) => (E.mapsAsObjects && (E.mapsAsObjects = false, oe = true), e2())).handlesRead = true;
function ne(e2, t) {
  return typeof e2 == "string" ? e2 + t : e2 instanceof Array ? e2.concat(t) : Object.assign({}, e2, t);
}
function Q() {
  if (!P)
    if (E.getShared)
      Ve();
    else
      throw new Error("No packed values available");
  return P;
}
var mt = 1399353956;
Re.push((e2, t) => {
  if (e2 >= 225 && e2 <= 255)
    return ne(Q().prefixes[e2 - 224], t);
  if (e2 >= 28704 && e2 <= 32767)
    return ne(Q().prefixes[e2 - 28672], t);
  if (e2 >= 1879052288 && e2 <= 2147483647)
    return ne(Q().prefixes[e2 - 1879048192], t);
  if (e2 >= 216 && e2 <= 223)
    return ne(t, Q().suffixes[e2 - 216]);
  if (e2 >= 27647 && e2 <= 28671)
    return ne(t, Q().suffixes[e2 - 27639]);
  if (e2 >= 1811940352 && e2 <= 1879048191)
    return ne(t, Q().suffixes[e2 - 1811939328]);
  if (e2 == mt)
    return { packedValues: P, structures: _3.slice(0), version: t };
  if (e2 == 55799)
    return t;
});
var At = new Uint8Array(new Uint16Array([1]).buffer)[0] == 1;
var Qe = [Uint8Array, Uint8ClampedArray, Uint16Array, Uint32Array, typeof BigUint64Array > "u" ? { name: "BigUint64Array" } : BigUint64Array, Int8Array, Int16Array, Int32Array, typeof BigInt64Array > "u" ? { name: "BigInt64Array" } : BigInt64Array, Float32Array, Float64Array];
var St = [64, 68, 69, 70, 71, 72, 77, 78, 79, 85, 86];
for (let e2 = 0; e2 < Qe.length; e2++)
  Et(Qe[e2], St[e2]);
function Et(e2, t) {
  let l2 = "get" + e2.name.slice(0, -5), n2;
  typeof e2 == "function" ? n2 = e2.BYTES_PER_ELEMENT : e2 = null;
  for (let f = 0; f < 2; f++) {
    if (!f && n2 == 1)
      continue;
    let o = n2 == 2 ? 1 : n2 == 4 ? 2 : 3;
    R[f ? t : t - 4] = n2 == 1 || f == At ? (d3) => {
      if (!e2)
        throw new Error("Could not find typed array for code " + t);
      return new e2(Uint8Array.prototype.slice.call(d3, 0).buffer);
    } : (d3) => {
      if (!e2)
        throw new Error("Could not find typed array for code " + t);
      let w3 = new DataView(d3.buffer, d3.byteOffset, d3.byteLength), U3 = d3.length >> o, p4 = new e2(U3), B4 = w3[l2];
      for (let O3 = 0; O3 < U3; O3++)
        p4[O3] = B4.call(w3, O3 << o, f);
      return p4;
    };
  }
}
function It() {
  let e2 = se(), t = a + S();
  for (let n2 = 2; n2 < e2; n2++) {
    let f = se();
    a += f;
  }
  let l2 = a;
  return a = t, T = [Te(se()), Te(se())], T.position0 = 0, T.position1 = 0, T.postBundlePosition = a, a = l2, S();
}
function se() {
  let e2 = y2[a++] & 31;
  if (e2 > 23)
    switch (e2) {
      case 24:
        e2 = y2[a++];
        break;
      case 25:
        e2 = C4.getUint16(a), a += 2;
        break;
      case 26:
        e2 = C4.getUint32(a), a += 4;
        break;
    }
  return e2;
}
function Ve() {
  if (E.getShared) {
    let e2 = nt2(() => (y2 = null, E.getShared())) || {}, t = e2.structures || [];
    E.sharedVersion = e2.version, P = E.sharedValues = e2.packedValues, _3 === true ? E.structures = _3 = t : _3.splice.apply(_3, [0, t.length].concat(t));
  }
}
function nt2(e2) {
  let t = v3, l2 = a, n2 = _e, f = pe, o = ae, d3 = ye, w3 = Me, U3 = W, p4 = T, B4 = new Uint8Array(y2.slice(0, v3)), O3 = _3, N3 = E, V2 = ce, K4 = e2();
  return v3 = t, a = l2, _e = n2, pe = f, ae = o, ye = d3, Me = w3, W = U3, T = p4, y2 = B4, ce = V2, _3 = O3, E = N3, C4 = new DataView(y2.buffer, y2.byteOffset, y2.byteLength), K4;
}
function ge() {
  y2 = null, W = null, _3 = null;
}
var ue = new Array(147);
for (let e2 = 0; e2 < 256; e2++)
  ue[e2] = +("1e" + Math.floor(45.15 - e2 * 0.30103));
var Ce = new Y({ useRecords: false });
var kt2 = Ce.decode;
var Ot = Ce.decodeMultiple;
var we = { NEVER: 0, ALWAYS: 1, DECIMAL_ROUND: 3, DECIMAL_FIT: 4 };
var be;
try {
  be = new TextEncoder();
} catch {
}
var Ae;
var ze;
var Ee = typeof globalThis == "object" && globalThis.Buffer;
var de = typeof Ee < "u";
var Pe = de ? Ee.allocUnsafeSlow : Uint8Array;
var st = de ? Ee : Uint8Array;
var lt = 256;
var ft = de ? 4294967296 : 2144337920;
var Le;
var i2;
var M2;
var r = 0;
var X;
var D2 = null;
var Mt2 = 61440;
var _t = /[\u0080-\uFFFF]/;
var L = Symbol("record-id");
var ee = class extends Y {
  constructor(t) {
    super(t), this.offset = 0;
    let l2, n2, f, o, d3, w3;
    t = t || {};
    let U3 = st.prototype.utf8Write ? function(s, h2, c3) {
      return i2.utf8Write(s, h2, c3);
    } : be && be.encodeInto ? function(s, h2) {
      return be.encodeInto(s, i2.subarray(h2)).written;
    } : false, p4 = this, B4 = t.structures || t.saveStructures, O3 = t.maxSharedStructures;
    if (O3 == null && (O3 = B4 ? 128 : 0), O3 > 8190)
      throw new Error("Maximum maxSharedStructure is 8190");
    let N3 = t.sequential;
    N3 && (O3 = 0), this.structures || (this.structures = []), this.saveStructures && (this.saveShared = this.saveStructures);
    let V2, K4, q2 = t.sharedValues, z3;
    if (q2) {
      z3 = /* @__PURE__ */ Object.create(null);
      for (let s = 0, h2 = q2.length; s < h2; s++)
        z3[q2[s]] = s;
    }
    let Z2 = [], Ie2 = 0, xe2 = 0;
    this.mapEncode = function(s, h2) {
      if (this._keyMap && !this._mapped)
        switch (s.constructor.name) {
          case "Array":
            s = s.map((c3) => this.encodeKeys(c3));
            break;
        }
      return this.encode(s, h2);
    }, this.encode = function(s, h2) {
      if (i2 || (i2 = new Pe(8192), M2 = new DataView(i2.buffer, 0, 8192), r = 0), X = i2.length - 10, X - r < 2048 ? (i2 = new Pe(i2.length), M2 = new DataView(i2.buffer, 0, i2.length), X = i2.length - 10, r = 0) : h2 === Ke && (r = r + 7 & 2147483640), n2 = r, p4.useSelfDescribedHeader && (M2.setUint32(r, 3654940416), r += 3), w3 = p4.structuredClone ? /* @__PURE__ */ new Map() : null, p4.bundleStrings && typeof s != "string" ? (D2 = [], D2.size = 1 / 0) : D2 = null, f = p4.structures, f) {
        if (f.uninitialized) {
          let x4 = p4.getShared() || {};
          p4.structures = f = x4.structures || [], p4.sharedVersion = x4.version;
          let u4 = p4.sharedValues = x4.packedValues;
          if (u4) {
            z3 = {};
            for (let g3 = 0, b3 = u4.length; g3 < b3; g3++)
              z3[u4[g3]] = g3;
          }
        }
        let c3 = f.length;
        if (c3 > O3 && !N3 && (c3 = O3), !f.transitions) {
          f.transitions = /* @__PURE__ */ Object.create(null);
          for (let x4 = 0; x4 < c3; x4++) {
            let u4 = f[x4];
            if (!u4)
              continue;
            let g3, b3 = f.transitions;
            for (let m4 = 0, A4 = u4.length; m4 < A4; m4++) {
              b3[L] === void 0 && (b3[L] = x4);
              let I2 = u4[m4];
              g3 = b3[I2], g3 || (g3 = b3[I2] = /* @__PURE__ */ Object.create(null)), b3 = g3;
            }
            b3[L] = x4 | 1048576;
          }
        }
        N3 || (f.nextId = c3);
      }
      if (o && (o = false), d3 = f || [], K4 = z3, t.pack) {
        let c3 = /* @__PURE__ */ new Map();
        if (c3.values = [], c3.encoder = p4, c3.maxValues = t.maxPrivatePackedValues || (z3 ? 16 : 1 / 0), c3.objectMap = z3 || false, c3.samplingPackedValues = V2, me(s, c3), c3.values.length > 0) {
          i2[r++] = 216, i2[r++] = 51, G3(4);
          let x4 = c3.values;
          k5(x4), G3(0), G3(0), K4 = Object.create(z3 || null);
          for (let u4 = 0, g3 = x4.length; u4 < g3; u4++)
            K4[x4[u4]] = u4;
        }
      }
      Le = h2 & je;
      try {
        if (Le)
          return;
        if (k5(s), D2 && ct(n2, k5), p4.offset = r, w3 && w3.idsToInsert) {
          r += w3.idsToInsert.length * 2, r > X && le2(r), p4.offset = r;
          let c3 = Tt(i2.subarray(n2, r), w3.idsToInsert);
          return w3 = null, c3;
        }
        return h2 & Ke ? (i2.start = n2, i2.end = r, i2) : i2.subarray(n2, r);
      } finally {
        if (f) {
          if (xe2 < 10 && xe2++, f.length > O3 && (f.length = O3), Ie2 > 1e4)
            f.transitions = null, xe2 = 0, Ie2 = 0, Z2.length > 0 && (Z2 = []);
          else if (Z2.length > 0 && !N3) {
            for (let c3 = 0, x4 = Z2.length; c3 < x4; c3++)
              Z2[c3][L] = void 0;
            Z2 = [];
          }
        }
        if (o && p4.saveShared) {
          p4.structures.length > O3 && (p4.structures = p4.structures.slice(0, O3));
          let c3 = i2.subarray(n2, r);
          return p4.updateSharedData() === false ? p4.encode(s) : c3;
        }
        h2 & Kt && (r = n2);
      }
    }, this.findCommonStringsToPack = () => (V2 = /* @__PURE__ */ new Map(), z3 || (z3 = /* @__PURE__ */ Object.create(null)), (s) => {
      let h2 = s && s.threshold || 4, c3 = this.pack ? s.maxPrivatePackedValues || 16 : 0;
      q2 || (q2 = this.sharedValues = []);
      for (let [x4, u4] of V2)
        u4.count > h2 && (z3[x4] = c3++, q2.push(x4), o = true);
      for (; this.saveShared && this.updateSharedData() === false; )
        ;
      V2 = null;
    });
    let k5 = (s) => {
      r > X && (i2 = le2(r));
      var h2 = typeof s, c3;
      if (h2 === "string") {
        if (K4) {
          let b3 = K4[s];
          if (b3 >= 0) {
            b3 < 16 ? i2[r++] = b3 + 224 : (i2[r++] = 198, b3 & 1 ? k5(15 - b3 >> 1) : k5(b3 - 16 >> 1));
            return;
          } else if (V2 && !t.pack) {
            let m4 = V2.get(s);
            m4 ? m4.count++ : V2.set(s, { count: 1 });
          }
        }
        let x4 = s.length;
        if (D2 && x4 >= 4 && x4 < 1024) {
          if ((D2.size += x4) > Mt2) {
            let m4, A4 = (D2[0] ? D2[0].length * 3 + D2[1].length : 0) + 10;
            r + A4 > X && (i2 = le2(r + A4)), i2[r++] = 217, i2[r++] = 223, i2[r++] = 249, i2[r++] = D2.position ? 132 : 130, i2[r++] = 26, m4 = r - n2, r += 4, D2.position && ct(n2, k5), D2 = ["", ""], D2.size = 0, D2.position = m4;
          }
          let b3 = _t.test(s);
          D2[b3 ? 0 : 1] += s, i2[r++] = b3 ? 206 : 207, k5(x4);
          return;
        }
        let u4;
        x4 < 32 ? u4 = 1 : x4 < 256 ? u4 = 2 : x4 < 65536 ? u4 = 3 : u4 = 5;
        let g3 = x4 * 3;
        if (r + g3 > X && (i2 = le2(r + g3)), x4 < 64 || !U3) {
          let b3, m4, A4, I2 = r + u4;
          for (b3 = 0; b3 < x4; b3++)
            m4 = s.charCodeAt(b3), m4 < 128 ? i2[I2++] = m4 : m4 < 2048 ? (i2[I2++] = m4 >> 6 | 192, i2[I2++] = m4 & 63 | 128) : (m4 & 64512) === 55296 && ((A4 = s.charCodeAt(b3 + 1)) & 64512) === 56320 ? (m4 = 65536 + ((m4 & 1023) << 10) + (A4 & 1023), b3++, i2[I2++] = m4 >> 18 | 240, i2[I2++] = m4 >> 12 & 63 | 128, i2[I2++] = m4 >> 6 & 63 | 128, i2[I2++] = m4 & 63 | 128) : (i2[I2++] = m4 >> 12 | 224, i2[I2++] = m4 >> 6 & 63 | 128, i2[I2++] = m4 & 63 | 128);
          c3 = I2 - r - u4;
        } else
          c3 = U3(s, r + u4, g3);
        c3 < 24 ? i2[r++] = 96 | c3 : c3 < 256 ? (u4 < 2 && i2.copyWithin(r + 2, r + 1, r + 1 + c3), i2[r++] = 120, i2[r++] = c3) : c3 < 65536 ? (u4 < 3 && i2.copyWithin(r + 3, r + 2, r + 2 + c3), i2[r++] = 121, i2[r++] = c3 >> 8, i2[r++] = c3 & 255) : (u4 < 5 && i2.copyWithin(r + 5, r + 3, r + 3 + c3), i2[r++] = 122, M2.setUint32(r, c3), r += 4), r += c3;
      } else if (h2 === "number")
        if (!this.alwaysUseFloat && s >>> 0 === s)
          s < 24 ? i2[r++] = s : s < 256 ? (i2[r++] = 24, i2[r++] = s) : s < 65536 ? (i2[r++] = 25, i2[r++] = s >> 8, i2[r++] = s & 255) : (i2[r++] = 26, M2.setUint32(r, s), r += 4);
        else if (!this.alwaysUseFloat && s >> 0 === s)
          s >= -24 ? i2[r++] = 31 - s : s >= -256 ? (i2[r++] = 56, i2[r++] = ~s) : s >= -65536 ? (i2[r++] = 57, M2.setUint16(r, ~s), r += 2) : (i2[r++] = 58, M2.setUint32(r, ~s), r += 4);
        else {
          let x4;
          if ((x4 = this.useFloat32) > 0 && s < 4294967296 && s >= -2147483648) {
            i2[r++] = 250, M2.setFloat32(r, s);
            let u4;
            if (x4 < 4 || (u4 = s * ue[(i2[r] & 127) << 1 | i2[r + 1] >> 7]) >> 0 === u4) {
              r += 4;
              return;
            } else
              r--;
          }
          i2[r++] = 251, M2.setFloat64(r, s), r += 8;
        }
      else if (h2 === "object")
        if (!s)
          i2[r++] = 246;
        else {
          if (w3) {
            let u4 = w3.get(s);
            if (u4) {
              if (i2[r++] = 216, i2[r++] = 29, i2[r++] = 25, !u4.references) {
                let g3 = w3.idsToInsert || (w3.idsToInsert = []);
                u4.references = [], g3.push(u4);
              }
              u4.references.push(r - n2), r += 2;
              return;
            } else
              w3.set(s, { offset: r - n2 });
          }
          let x4 = s.constructor;
          if (x4 === Object)
            ke2(s, true);
          else if (x4 === Array) {
            c3 = s.length, c3 < 24 ? i2[r++] = 128 | c3 : G3(c3);
            for (let u4 = 0; u4 < c3; u4++)
              k5(s[u4]);
          } else if (x4 === Map)
            if ((this.mapsAsObjects ? this.useTag259ForMaps !== false : this.useTag259ForMaps) && (i2[r++] = 217, i2[r++] = 1, i2[r++] = 3), c3 = s.size, c3 < 24 ? i2[r++] = 160 | c3 : c3 < 256 ? (i2[r++] = 184, i2[r++] = c3) : c3 < 65536 ? (i2[r++] = 185, i2[r++] = c3 >> 8, i2[r++] = c3 & 255) : (i2[r++] = 186, M2.setUint32(r, c3), r += 4), p4.keyMap)
              for (let [u4, g3] of s)
                k5(p4.encodeKey(u4)), k5(g3);
            else
              for (let [u4, g3] of s)
                k5(u4), k5(g3);
          else {
            for (let u4 = 0, g3 = Ae.length; u4 < g3; u4++) {
              let b3 = ze[u4];
              if (s instanceof b3) {
                let m4 = Ae[u4], A4 = m4.tag;
                A4 == null && (A4 = m4.getTag && m4.getTag.call(this, s)), A4 < 24 ? i2[r++] = 192 | A4 : A4 < 256 ? (i2[r++] = 216, i2[r++] = A4) : A4 < 65536 ? (i2[r++] = 217, i2[r++] = A4 >> 8, i2[r++] = A4 & 255) : A4 > -1 && (i2[r++] = 218, M2.setUint32(r, A4), r += 4), m4.encode.call(this, s, k5, le2);
                return;
              }
            }
            if (s[Symbol.iterator]) {
              if (Le) {
                let u4 = new Error("Iterable should be serialized as iterator");
                throw u4.iteratorNotHandled = true, u4;
              }
              i2[r++] = 159;
              for (let u4 of s)
                k5(u4);
              i2[r++] = 255;
              return;
            }
            if (s[Symbol.asyncIterator] || Ne(s)) {
              let u4 = new Error("Iterable/blob should be serialized as iterator");
              throw u4.iteratorNotHandled = true, u4;
            }
            ke2(s, !s.hasOwnProperty);
          }
        }
      else if (h2 === "boolean")
        i2[r++] = s ? 245 : 244;
      else if (h2 === "bigint") {
        if (s < BigInt(1) << BigInt(64) && s >= 0)
          i2[r++] = 27, M2.setBigUint64(r, s);
        else if (s > -(BigInt(1) << BigInt(64)) && s < 0)
          i2[r++] = 59, M2.setBigUint64(r, -s - BigInt(1));
        else if (this.largeBigIntToFloat)
          i2[r++] = 251, M2.setFloat64(r, Number(s));
        else
          throw new RangeError(s + " was too large to fit in CBOR 64-bit integer format, set largeBigIntToFloat to convert to float-64");
        r += 8;
      } else if (h2 === "undefined")
        i2[r++] = 247;
      else
        throw new Error("Unknown type: " + h2);
    }, ke2 = this.useRecords === false ? this.variableMapSize ? (s) => {
      let h2 = Object.keys(s), c3 = Object.values(s), x4 = h2.length;
      x4 < 24 ? i2[r++] = 160 | x4 : x4 < 256 ? (i2[r++] = 184, i2[r++] = x4) : x4 < 65536 ? (i2[r++] = 185, i2[r++] = x4 >> 8, i2[r++] = x4 & 255) : (i2[r++] = 186, M2.setUint32(r, x4), r += 4);
      let u4;
      if (p4.keyMap)
        for (let g3 = 0; g3 < x4; g3++)
          k5(encodeKey(h2[g3])), k5(c3[g3]);
      else
        for (let g3 = 0; g3 < x4; g3++)
          k5(h2[g3]), k5(c3[g3]);
    } : (s, h2) => {
      i2[r++] = 185;
      let c3 = r - n2;
      r += 2;
      let x4 = 0;
      if (p4.keyMap)
        for (let u4 in s)
          (h2 || s.hasOwnProperty(u4)) && (k5(p4.encodeKey(u4)), k5(s[u4]), x4++);
      else
        for (let u4 in s)
          (h2 || s.hasOwnProperty(u4)) && (k5(u4), k5(s[u4]), x4++);
      i2[c3++ + n2] = x4 >> 8, i2[c3 + n2] = x4 & 255;
    } : (s, h2) => {
      let c3, x4 = d3.transitions || (d3.transitions = /* @__PURE__ */ Object.create(null)), u4 = 0, g3 = 0, b3, m4;
      if (this.keyMap) {
        m4 = Object.keys(s).map((I2) => this.encodeKey(I2)), g3 = m4.length;
        for (let I2 = 0; I2 < g3; I2++) {
          let Ge2 = m4[I2];
          c3 = x4[Ge2], c3 || (c3 = x4[Ge2] = /* @__PURE__ */ Object.create(null), u4++), x4 = c3;
        }
      } else
        for (let I2 in s)
          (h2 || s.hasOwnProperty(I2)) && (c3 = x4[I2], c3 || (x4[L] & 1048576 && (b3 = x4[L] & 65535), c3 = x4[I2] = /* @__PURE__ */ Object.create(null), u4++), x4 = c3, g3++);
      let A4 = x4[L];
      if (A4 !== void 0)
        A4 &= 65535, i2[r++] = 217, i2[r++] = A4 >> 8 | 224, i2[r++] = A4 & 255;
      else if (m4 || (m4 = x4.__keys__ || (x4.__keys__ = Object.keys(s))), b3 === void 0 ? (A4 = d3.nextId++, A4 || (A4 = 0, d3.nextId = 1), A4 >= lt && (d3.nextId = (A4 = O3) + 1)) : A4 = b3, d3[A4] = m4, A4 < O3) {
        i2[r++] = 217, i2[r++] = A4 >> 8 | 224, i2[r++] = A4 & 255, x4 = d3.transitions;
        for (let I2 = 0; I2 < g3; I2++)
          (x4[L] === void 0 || x4[L] & 1048576) && (x4[L] = A4), x4 = x4[m4[I2]];
        x4[L] = A4 | 1048576, o = true;
      } else {
        if (x4[L] = A4, M2.setUint32(r, 3655335680), r += 3, u4 && (Ie2 += xe2 * u4), Z2.length >= lt - O3 && (Z2.shift()[L] = void 0), Z2.push(x4), G3(g3 + 2), k5(57344 + A4), k5(m4), h2 === null)
          return;
        for (let I2 in s)
          (h2 || s.hasOwnProperty(I2)) && k5(s[I2]);
        return;
      }
      if (g3 < 24 ? i2[r++] = 128 | g3 : G3(g3), h2 !== null)
        for (let I2 in s)
          (h2 || s.hasOwnProperty(I2)) && k5(s[I2]);
    }, le2 = (s) => {
      let h2;
      if (s > 16777216) {
        if (s - n2 > ft)
          throw new Error("Encoded buffer would be larger than maximum buffer size");
        h2 = Math.min(ft, Math.round(Math.max((s - n2) * (s > 67108864 ? 1.25 : 2), 4194304) / 4096) * 4096);
      } else
        h2 = (Math.max(s - n2 << 2, i2.length - 1) >> 12) + 1 << 12;
      let c3 = new Pe(h2);
      return M2 = new DataView(c3.buffer, 0, h2), i2.copy ? i2.copy(c3, 0, n2, s) : c3.set(i2.slice(n2, s)), r -= n2, n2 = 0, X = c3.length - 10, i2 = c3;
    }, $2 = 100, Ye2 = 1e3;
    this.encodeAsIterable = function(s, h2) {
      return He2(s, h2, te2);
    }, this.encodeAsAsyncIterable = function(s, h2) {
      return He2(s, h2, Je2);
    };
    function* te2(s, h2, c3) {
      let x4 = s.constructor;
      if (x4 === Object) {
        let u4 = p4.useRecords !== false;
        u4 ? ke2(s, null) : ot2(Object.keys(s).length, 160);
        for (let g3 in s) {
          let b3 = s[g3];
          u4 || k5(g3), b3 && typeof b3 == "object" ? h2[g3] ? yield* te2(b3, h2[g3]) : yield* Oe2(b3, h2, g3) : k5(b3);
        }
      } else if (x4 === Array) {
        let u4 = s.length;
        G3(u4);
        for (let g3 = 0; g3 < u4; g3++) {
          let b3 = s[g3];
          b3 && (typeof b3 == "object" || r - n2 > $2) ? h2.element ? yield* te2(b3, h2.element) : yield* Oe2(b3, h2, "element") : k5(b3);
        }
      } else if (s[Symbol.iterator]) {
        i2[r++] = 159;
        for (let u4 of s)
          u4 && (typeof u4 == "object" || r - n2 > $2) ? h2.element ? yield* te2(u4, h2.element) : yield* Oe2(u4, h2, "element") : k5(u4);
        i2[r++] = 255;
      } else
        Ne(s) ? (ot2(s.size, 64), yield i2.subarray(n2, r), yield s, fe2()) : s[Symbol.asyncIterator] ? (i2[r++] = 159, yield i2.subarray(n2, r), yield s, fe2(), i2[r++] = 255) : k5(s);
      c3 && r > n2 ? yield i2.subarray(n2, r) : r - n2 > $2 && (yield i2.subarray(n2, r), fe2());
    }
    function* Oe2(s, h2, c3) {
      let x4 = r - n2;
      try {
        k5(s), r - n2 > $2 && (yield i2.subarray(n2, r), fe2());
      } catch (u4) {
        if (u4.iteratorNotHandled)
          h2[c3] = {}, r = n2 + x4, yield* te2.call(this, s, h2[c3]);
        else
          throw u4;
      }
    }
    function fe2() {
      $2 = Ye2, p4.encode(null, je);
    }
    function He2(s, h2, c3) {
      return h2 && h2.chunkThreshold ? $2 = Ye2 = h2.chunkThreshold : $2 = 100, s && typeof s == "object" ? (p4.encode(null, je), c3(s, p4.iterateProperties || (p4.iterateProperties = {}), true)) : [p4.encode(s)];
    }
    async function* Je2(s, h2) {
      for (let c3 of te2(s, h2, true)) {
        let x4 = c3.constructor;
        if (x4 === st || x4 === Uint8Array)
          yield c3;
        else if (Ne(c3)) {
          let u4 = c3.stream().getReader(), g3;
          for (; !(g3 = await u4.read()).done; )
            yield g3.value;
        } else if (c3[Symbol.asyncIterator])
          for await (let u4 of c3)
            fe2(), u4 ? yield* Je2(u4, h2.async || (h2.async = {})) : yield p4.encode(u4);
        else
          yield c3;
      }
    }
  }
  useBuffer(t) {
    i2 = t, M2 = new DataView(i2.buffer, i2.byteOffset, i2.byteLength), r = 0;
  }
  clearSharedData() {
    this.structures && (this.structures = []), this.sharedValues && (this.sharedValues = void 0);
  }
  updateSharedData() {
    let t = this.sharedVersion || 0;
    this.sharedVersion = t + 1;
    let l2 = this.structures.slice(0), n2 = new Se(l2, this.sharedValues, this.sharedVersion), f = this.saveShared(n2, (o) => (o && o.version || 0) == t);
    return f === false ? (n2 = this.getShared() || {}, this.structures = n2.structures || [], this.sharedValues = n2.packedValues, this.sharedVersion = n2.version, this.structures.nextId = this.structures.length) : l2.forEach((o, d3) => this.structures[d3] = o), f;
  }
};
function ot2(e2, t) {
  e2 < 24 ? i2[r++] = t | e2 : e2 < 256 ? (i2[r++] = t | 24, i2[r++] = e2) : e2 < 65536 ? (i2[r++] = t | 25, i2[r++] = e2 >> 8, i2[r++] = e2 & 255) : (i2[r++] = t | 26, M2.setUint32(r, e2), r += 4);
}
var Se = class {
  constructor(t, l2, n2) {
    this.structures = t, this.packedValues = l2, this.version = n2;
  }
};
function G3(e2) {
  e2 < 24 ? i2[r++] = 128 | e2 : e2 < 256 ? (i2[r++] = 152, i2[r++] = e2) : e2 < 65536 ? (i2[r++] = 153, i2[r++] = e2 >> 8, i2[r++] = e2 & 255) : (i2[r++] = 154, M2.setUint32(r, e2), r += 4);
}
var Rt = typeof Blob > "u" ? function() {
} : Blob;
function Ne(e2) {
  if (e2 instanceof Rt)
    return true;
  let t = e2[Symbol.toStringTag];
  return t === "Blob" || t === "File";
}
function me(e2, t) {
  switch (typeof e2) {
    case "string":
      if (e2.length > 3) {
        if (t.objectMap[e2] > -1 || t.values.length >= t.maxValues)
          return;
        let n2 = t.get(e2);
        if (n2)
          ++n2.count == 2 && t.values.push(e2);
        else if (t.set(e2, { count: 1 }), t.samplingPackedValues) {
          let f = t.samplingPackedValues.get(e2);
          f ? f.count++ : t.samplingPackedValues.set(e2, { count: 1 });
        }
      }
      break;
    case "object":
      if (e2)
        if (e2 instanceof Array)
          for (let n2 = 0, f = e2.length; n2 < f; n2++)
            me(e2[n2], t);
        else {
          let n2 = !t.encoder.useRecords;
          for (var l2 in e2)
            e2.hasOwnProperty(l2) && (n2 && me(l2, t), me(e2[l2], t));
        }
      break;
    case "function":
      console.log(e2);
  }
}
var Bt = new Uint8Array(new Uint16Array([1]).buffer)[0] == 1;
ze = [Date, Set, Error, RegExp, H3, ArrayBuffer, Uint8Array, Uint8ClampedArray, Uint16Array, Uint32Array, typeof BigUint64Array > "u" ? function() {
} : BigUint64Array, Int8Array, Int16Array, Int32Array, typeof BigInt64Array > "u" ? function() {
} : BigInt64Array, Float32Array, Float64Array, Se];
Ae = [{ tag: 1, encode(e2, t) {
  let l2 = e2.getTime() / 1e3;
  (this.useTimestamp32 || e2.getMilliseconds() === 0) && l2 >= 0 && l2 < 4294967296 ? (i2[r++] = 26, M2.setUint32(r, l2), r += 4) : (i2[r++] = 251, M2.setFloat64(r, l2), r += 8);
} }, { tag: 258, encode(e2, t) {
  let l2 = Array.from(e2);
  t(l2);
} }, { tag: 27, encode(e2, t) {
  t([e2.name, e2.message]);
} }, { tag: 27, encode(e2, t) {
  t(["RegExp", e2.source, e2.flags]);
} }, { getTag(e2) {
  return e2.tag;
}, encode(e2, t) {
  t(e2.value);
} }, { encode(e2, t, l2) {
  at(e2, l2);
} }, { getTag(e2) {
  if (e2.constructor === Uint8Array && (this.tagUint8Array || de && this.tagUint8Array !== false))
    return 64;
}, encode(e2, t, l2) {
  at(e2, l2);
} }, J3(68, 1), J3(69, 2), J3(70, 4), J3(71, 8), J3(72, 1), J3(77, 2), J3(78, 4), J3(79, 8), J3(85, 4), J3(86, 8), { encode(e2, t) {
  let l2 = e2.packedValues || [], n2 = e2.structures || [];
  if (l2.values.length > 0) {
    i2[r++] = 216, i2[r++] = 51, G3(4);
    let f = l2.values;
    t(f), G3(0), G3(0), packedObjectMap = Object.create(sharedPackedObjectMap || null);
    for (let o = 0, d3 = f.length; o < d3; o++)
      packedObjectMap[f[o]] = o;
  }
  if (n2) {
    M2.setUint32(r, 3655335424), r += 3;
    let f = n2.slice(0);
    f.unshift(57344), f.push(new H3(e2.version, 1399353956)), t(f);
  } else
    t(new H3(e2.version, 1399353956));
} }];
function J3(e2, t) {
  return !Bt && t > 1 && (e2 -= 4), { tag: e2, encode: function(n2, f) {
    let o = n2.byteLength, d3 = n2.byteOffset || 0, w3 = n2.buffer || n2;
    f(de ? Ee.from(w3, d3, o) : new Uint8Array(w3, d3, o));
  } };
}
function at(e2, t) {
  let l2 = e2.byteLength;
  l2 < 24 ? i2[r++] = 64 + l2 : l2 < 256 ? (i2[r++] = 88, i2[r++] = l2) : l2 < 65536 ? (i2[r++] = 89, i2[r++] = l2 >> 8, i2[r++] = l2 & 255) : (i2[r++] = 90, M2.setUint32(r, l2), r += 4), r + l2 >= i2.length && t(r + l2), i2.set(e2.buffer ? e2 : new Uint8Array(e2), r), r += l2;
}
function Tt(e2, t) {
  let l2, n2 = t.length * 2, f = e2.length - n2;
  t.sort((o, d3) => o.offset > d3.offset ? 1 : -1);
  for (let o = 0; o < t.length; o++) {
    let d3 = t[o];
    d3.id = o;
    for (let w3 of d3.references)
      e2[w3++] = o >> 8, e2[w3] = o & 255;
  }
  for (; l2 = t.pop(); ) {
    let o = l2.offset;
    e2.copyWithin(o + n2, o, f), n2 -= 2;
    let d3 = o + n2;
    e2[d3++] = 216, e2[d3++] = 28, f = o;
  }
  return e2;
}
function ct(e2, t) {
  M2.setUint32(D2.position + e2, r - D2.position - e2 + 1);
  let l2 = D2;
  D2 = null, t(l2[0]), t(l2[1]);
}
var We = new ee({ useRecords: false });
var Ft2 = We.encode;
var Vt = We.encodeAsIterable;
var Ct2 = We.encodeAsAsyncIterable;
var { NEVER: Pt2, ALWAYS: Lt2, DECIMAL_ROUND: Nt2, DECIMAL_FIT: jt2 } = we;
var Ke = 512;
var Kt = 1024;
var je = 2048;

// ../desktop-dev/src/core/ipc-web/$messageToIpcMessage.ts
var textDecoder2 = new TextDecoder();

// https://esm.sh/v124/zod@3.21.4/denonext/zod.mjs
var g2;
(function(s) {
  s.assertEqual = (n2) => n2;
  function e2(n2) {
  }
  s.assertIs = e2;
  function t(n2) {
    throw new Error();
  }
  s.assertNever = t, s.arrayToEnum = (n2) => {
    let a3 = {};
    for (let i4 of n2)
      a3[i4] = i4;
    return a3;
  }, s.getValidEnumValues = (n2) => {
    let a3 = s.objectKeys(n2).filter((o) => typeof n2[n2[o]] != "number"), i4 = {};
    for (let o of a3)
      i4[o] = n2[o];
    return s.objectValues(i4);
  }, s.objectValues = (n2) => s.objectKeys(n2).map(function(a3) {
    return n2[a3];
  }), s.objectKeys = typeof Object.keys == "function" ? (n2) => Object.keys(n2) : (n2) => {
    let a3 = [];
    for (let i4 in n2)
      Object.prototype.hasOwnProperty.call(n2, i4) && a3.push(i4);
    return a3;
  }, s.find = (n2, a3) => {
    for (let i4 of n2)
      if (a3(i4))
        return i4;
  }, s.isInteger = typeof Number.isInteger == "function" ? (n2) => Number.isInteger(n2) : (n2) => typeof n2 == "number" && isFinite(n2) && Math.floor(n2) === n2;
  function r2(n2, a3 = " | ") {
    return n2.map((i4) => typeof i4 == "string" ? `'${i4}'` : i4).join(a3);
  }
  s.joinValues = r2, s.jsonStringifyReplacer = (n2, a3) => typeof a3 == "bigint" ? a3.toString() : a3;
})(g2 || (g2 = {}));
var me2;
(function(s) {
  s.mergeShapes = (e2, t) => ({ ...e2, ...t });
})(me2 || (me2 = {}));
var d = g2.arrayToEnum(["string", "nan", "number", "integer", "float", "boolean", "date", "bigint", "symbol", "function", "undefined", "null", "array", "object", "unknown", "promise", "void", "never", "map", "set"]);
var P2 = (s) => {
  switch (typeof s) {
    case "undefined":
      return d.undefined;
    case "string":
      return d.string;
    case "number":
      return isNaN(s) ? d.nan : d.number;
    case "boolean":
      return d.boolean;
    case "function":
      return d.function;
    case "bigint":
      return d.bigint;
    case "symbol":
      return d.symbol;
    case "object":
      return Array.isArray(s) ? d.array : s === null ? d.null : s.then && typeof s.then == "function" && s.catch && typeof s.catch == "function" ? d.promise : typeof Map < "u" && s instanceof Map ? d.map : typeof Set < "u" && s instanceof Set ? d.set : typeof Date < "u" && s instanceof Date ? d.date : d.object;
    default:
      return d.unknown;
  }
};
var c2 = g2.arrayToEnum(["invalid_type", "invalid_literal", "custom", "invalid_union", "invalid_union_discriminator", "invalid_enum_value", "unrecognized_keys", "invalid_arguments", "invalid_return_type", "invalid_date", "invalid_string", "too_small", "too_big", "invalid_intersection_types", "not_multiple_of", "not_finite"]);
var Ne2 = (s) => JSON.stringify(s, null, 2).replace(/"([^"]+)":/g, "$1:");
var T2 = class extends Error {
  constructor(e2) {
    super(), this.issues = [], this.addIssue = (r2) => {
      this.issues = [...this.issues, r2];
    }, this.addIssues = (r2 = []) => {
      this.issues = [...this.issues, ...r2];
    };
    let t = new.target.prototype;
    Object.setPrototypeOf ? Object.setPrototypeOf(this, t) : this.__proto__ = t, this.name = "ZodError", this.issues = e2;
  }
  get errors() {
    return this.issues;
  }
  format(e2) {
    let t = e2 || function(a3) {
      return a3.message;
    }, r2 = { _errors: [] }, n2 = (a3) => {
      for (let i4 of a3.issues)
        if (i4.code === "invalid_union")
          i4.unionErrors.map(n2);
        else if (i4.code === "invalid_return_type")
          n2(i4.returnTypeError);
        else if (i4.code === "invalid_arguments")
          n2(i4.argumentsError);
        else if (i4.path.length === 0)
          r2._errors.push(t(i4));
        else {
          let o = r2, f = 0;
          for (; f < i4.path.length; ) {
            let l2 = i4.path[f];
            f === i4.path.length - 1 ? (o[l2] = o[l2] || { _errors: [] }, o[l2]._errors.push(t(i4))) : o[l2] = o[l2] || { _errors: [] }, o = o[l2], f++;
          }
        }
    };
    return n2(this), r2;
  }
  toString() {
    return this.message;
  }
  get message() {
    return JSON.stringify(this.issues, g2.jsonStringifyReplacer, 2);
  }
  get isEmpty() {
    return this.issues.length === 0;
  }
  flatten(e2 = (t) => t.message) {
    let t = {}, r2 = [];
    for (let n2 of this.issues)
      n2.path.length > 0 ? (t[n2.path[0]] = t[n2.path[0]] || [], t[n2.path[0]].push(e2(n2))) : r2.push(e2(n2));
    return { formErrors: r2, fieldErrors: t };
  }
  get formErrors() {
    return this.flatten();
  }
};
T2.create = (s) => new T2(s);
var oe2 = (s, e2) => {
  let t;
  switch (s.code) {
    case c2.invalid_type:
      s.received === d.undefined ? t = "Required" : t = `Expected ${s.expected}, received ${s.received}`;
      break;
    case c2.invalid_literal:
      t = `Invalid literal value, expected ${JSON.stringify(s.expected, g2.jsonStringifyReplacer)}`;
      break;
    case c2.unrecognized_keys:
      t = `Unrecognized key(s) in object: ${g2.joinValues(s.keys, ", ")}`;
      break;
    case c2.invalid_union:
      t = "Invalid input";
      break;
    case c2.invalid_union_discriminator:
      t = `Invalid discriminator value. Expected ${g2.joinValues(s.options)}`;
      break;
    case c2.invalid_enum_value:
      t = `Invalid enum value. Expected ${g2.joinValues(s.options)}, received '${s.received}'`;
      break;
    case c2.invalid_arguments:
      t = "Invalid function arguments";
      break;
    case c2.invalid_return_type:
      t = "Invalid function return type";
      break;
    case c2.invalid_date:
      t = "Invalid date";
      break;
    case c2.invalid_string:
      typeof s.validation == "object" ? "includes" in s.validation ? (t = `Invalid input: must include "${s.validation.includes}"`, typeof s.validation.position == "number" && (t = `${t} at one or more positions greater than or equal to ${s.validation.position}`)) : "startsWith" in s.validation ? t = `Invalid input: must start with "${s.validation.startsWith}"` : "endsWith" in s.validation ? t = `Invalid input: must end with "${s.validation.endsWith}"` : g2.assertNever(s.validation) : s.validation !== "regex" ? t = `Invalid ${s.validation}` : t = "Invalid";
      break;
    case c2.too_small:
      s.type === "array" ? t = `Array must contain ${s.exact ? "exactly" : s.inclusive ? "at least" : "more than"} ${s.minimum} element(s)` : s.type === "string" ? t = `String must contain ${s.exact ? "exactly" : s.inclusive ? "at least" : "over"} ${s.minimum} character(s)` : s.type === "number" ? t = `Number must be ${s.exact ? "exactly equal to " : s.inclusive ? "greater than or equal to " : "greater than "}${s.minimum}` : s.type === "date" ? t = `Date must be ${s.exact ? "exactly equal to " : s.inclusive ? "greater than or equal to " : "greater than "}${new Date(Number(s.minimum))}` : t = "Invalid input";
      break;
    case c2.too_big:
      s.type === "array" ? t = `Array must contain ${s.exact ? "exactly" : s.inclusive ? "at most" : "less than"} ${s.maximum} element(s)` : s.type === "string" ? t = `String must contain ${s.exact ? "exactly" : s.inclusive ? "at most" : "under"} ${s.maximum} character(s)` : s.type === "number" ? t = `Number must be ${s.exact ? "exactly" : s.inclusive ? "less than or equal to" : "less than"} ${s.maximum}` : s.type === "bigint" ? t = `BigInt must be ${s.exact ? "exactly" : s.inclusive ? "less than or equal to" : "less than"} ${s.maximum}` : s.type === "date" ? t = `Date must be ${s.exact ? "exactly" : s.inclusive ? "smaller than or equal to" : "smaller than"} ${new Date(Number(s.maximum))}` : t = "Invalid input";
      break;
    case c2.custom:
      t = "Invalid input";
      break;
    case c2.invalid_intersection_types:
      t = "Intersection results could not be merged";
      break;
    case c2.not_multiple_of:
      t = `Number must be a multiple of ${s.multipleOf}`;
      break;
    case c2.not_finite:
      t = "Number must be finite";
      break;
    default:
      t = e2.defaultError, g2.assertNever(s);
  }
  return { message: t };
};
var ke = oe2;
function Ee2(s) {
  ke = s;
}
function de2() {
  return ke;
}
var ue2 = (s) => {
  let { data: e2, path: t, errorMaps: r2, issueData: n2 } = s, a3 = [...t, ...n2.path || []], i4 = { ...n2, path: a3 }, o = "", f = r2.filter((l2) => !!l2).slice().reverse();
  for (let l2 of f)
    o = l2(i4, { data: e2, defaultError: o }).message;
  return { ...n2, path: a3, message: n2.message || o };
};
var Ie = [];
function u2(s, e2) {
  let t = ue2({ issueData: e2, data: s.data, path: s.path, errorMaps: [s.common.contextualErrorMap, s.schemaErrorMap, de2(), oe2].filter((r2) => !!r2) });
  s.common.issues.push(t);
}
var k4 = class {
  constructor() {
    this.value = "valid";
  }
  dirty() {
    this.value === "valid" && (this.value = "dirty");
  }
  abort() {
    this.value !== "aborted" && (this.value = "aborted");
  }
  static mergeArray(e2, t) {
    let r2 = [];
    for (let n2 of t) {
      if (n2.status === "aborted")
        return m2;
      n2.status === "dirty" && e2.dirty(), r2.push(n2.value);
    }
    return { status: e2.value, value: r2 };
  }
  static async mergeObjectAsync(e2, t) {
    let r2 = [];
    for (let n2 of t)
      r2.push({ key: await n2.key, value: await n2.value });
    return k4.mergeObjectSync(e2, r2);
  }
  static mergeObjectSync(e2, t) {
    let r2 = {};
    for (let n2 of t) {
      let { key: a3, value: i4 } = n2;
      if (a3.status === "aborted" || i4.status === "aborted")
        return m2;
      a3.status === "dirty" && e2.dirty(), i4.status === "dirty" && e2.dirty(), (typeof i4.value < "u" || n2.alwaysSet) && (r2[a3.value] = i4.value);
    }
    return { status: e2.value, value: r2 };
  }
};
var m2 = Object.freeze({ status: "aborted" });
var be2 = (s) => ({ status: "dirty", value: s });
var b = (s) => ({ status: "valid", value: s });
var ye2 = (s) => s.status === "aborted";
var ve2 = (s) => s.status === "dirty";
var le = (s) => s.status === "valid";
var fe = (s) => typeof Promise < "u" && s instanceof Promise;
var h;
(function(s) {
  s.errToObj = (e2) => typeof e2 == "string" ? { message: e2 } : e2 || {}, s.toString = (e2) => typeof e2 == "string" ? e2 : e2?.message;
})(h || (h = {}));
var O2 = class {
  constructor(e2, t, r2, n2) {
    this._cachedPath = [], this.parent = e2, this.data = t, this._path = r2, this._key = n2;
  }
  get path() {
    return this._cachedPath.length || (this._key instanceof Array ? this._cachedPath.push(...this._path, ...this._key) : this._cachedPath.push(...this._path, this._key)), this._cachedPath;
  }
};
var ge2 = (s, e2) => {
  if (le(e2))
    return { success: true, data: e2.value };
  if (!s.common.issues.length)
    throw new Error("Validation failed but no issues detected.");
  return { success: false, get error() {
    if (this._error)
      return this._error;
    let t = new T2(s.common.issues);
    return this._error = t, this._error;
  } };
};
function y3(s) {
  if (!s)
    return {};
  let { errorMap: e2, invalid_type_error: t, required_error: r2, description: n2 } = s;
  if (e2 && (t || r2))
    throw new Error(`Can't use "invalid_type_error" or "required_error" in conjunction with custom error map.`);
  return e2 ? { errorMap: e2, description: n2 } : { errorMap: (i4, o) => i4.code !== "invalid_type" ? { message: o.defaultError } : typeof o.data > "u" ? { message: r2 ?? o.defaultError } : { message: t ?? o.defaultError }, description: n2 };
}
var v4 = class {
  constructor(e2) {
    this.spa = this.safeParseAsync, this._def = e2, this.parse = this.parse.bind(this), this.safeParse = this.safeParse.bind(this), this.parseAsync = this.parseAsync.bind(this), this.safeParseAsync = this.safeParseAsync.bind(this), this.spa = this.spa.bind(this), this.refine = this.refine.bind(this), this.refinement = this.refinement.bind(this), this.superRefine = this.superRefine.bind(this), this.optional = this.optional.bind(this), this.nullable = this.nullable.bind(this), this.nullish = this.nullish.bind(this), this.array = this.array.bind(this), this.promise = this.promise.bind(this), this.or = this.or.bind(this), this.and = this.and.bind(this), this.transform = this.transform.bind(this), this.brand = this.brand.bind(this), this.default = this.default.bind(this), this.catch = this.catch.bind(this), this.describe = this.describe.bind(this), this.pipe = this.pipe.bind(this), this.isNullable = this.isNullable.bind(this), this.isOptional = this.isOptional.bind(this);
  }
  get description() {
    return this._def.description;
  }
  _getType(e2) {
    return P2(e2.data);
  }
  _getOrReturnCtx(e2, t) {
    return t || { common: e2.parent.common, data: e2.data, parsedType: P2(e2.data), schemaErrorMap: this._def.errorMap, path: e2.path, parent: e2.parent };
  }
  _processInputParams(e2) {
    return { status: new k4(), ctx: { common: e2.parent.common, data: e2.data, parsedType: P2(e2.data), schemaErrorMap: this._def.errorMap, path: e2.path, parent: e2.parent } };
  }
  _parseSync(e2) {
    let t = this._parse(e2);
    if (fe(t))
      throw new Error("Synchronous parse encountered promise.");
    return t;
  }
  _parseAsync(e2) {
    let t = this._parse(e2);
    return Promise.resolve(t);
  }
  parse(e2, t) {
    let r2 = this.safeParse(e2, t);
    if (r2.success)
      return r2.data;
    throw r2.error;
  }
  safeParse(e2, t) {
    var r2;
    let n2 = { common: { issues: [], async: (r2 = t?.async) !== null && r2 !== void 0 ? r2 : false, contextualErrorMap: t?.errorMap }, path: t?.path || [], schemaErrorMap: this._def.errorMap, parent: null, data: e2, parsedType: P2(e2) }, a3 = this._parseSync({ data: e2, path: n2.path, parent: n2 });
    return ge2(n2, a3);
  }
  async parseAsync(e2, t) {
    let r2 = await this.safeParseAsync(e2, t);
    if (r2.success)
      return r2.data;
    throw r2.error;
  }
  async safeParseAsync(e2, t) {
    let r2 = { common: { issues: [], contextualErrorMap: t?.errorMap, async: true }, path: t?.path || [], schemaErrorMap: this._def.errorMap, parent: null, data: e2, parsedType: P2(e2) }, n2 = this._parse({ data: e2, path: r2.path, parent: r2 }), a3 = await (fe(n2) ? n2 : Promise.resolve(n2));
    return ge2(r2, a3);
  }
  refine(e2, t) {
    let r2 = (n2) => typeof t == "string" || typeof t > "u" ? { message: t } : typeof t == "function" ? t(n2) : t;
    return this._refinement((n2, a3) => {
      let i4 = e2(n2), o = () => a3.addIssue({ code: c2.custom, ...r2(n2) });
      return typeof Promise < "u" && i4 instanceof Promise ? i4.then((f) => f ? true : (o(), false)) : i4 ? true : (o(), false);
    });
  }
  refinement(e2, t) {
    return this._refinement((r2, n2) => e2(r2) ? true : (n2.addIssue(typeof t == "function" ? t(r2, n2) : t), false));
  }
  _refinement(e2) {
    return new C5({ schema: this, typeName: p2.ZodEffects, effect: { type: "refinement", refinement: e2 } });
  }
  superRefine(e2) {
    return this._refinement(e2);
  }
  optional() {
    return E2.create(this, this._def);
  }
  nullable() {
    return $.create(this, this._def);
  }
  nullish() {
    return this.nullable().optional();
  }
  array() {
    return S2.create(this, this._def);
  }
  promise() {
    return D3.create(this, this._def);
  }
  or(e2) {
    return q.create([this, e2], this._def);
  }
  and(e2) {
    return J4.create(this, e2, this._def);
  }
  transform(e2) {
    return new C5({ ...y3(this._def), schema: this, typeName: p2.ZodEffects, effect: { type: "transform", transform: e2 } });
  }
  default(e2) {
    let t = typeof e2 == "function" ? e2 : () => e2;
    return new K3({ ...y3(this._def), innerType: this, defaultValue: t, typeName: p2.ZodDefault });
  }
  brand() {
    return new he2({ typeName: p2.ZodBranded, type: this, ...y3(this._def) });
  }
  catch(e2) {
    let t = typeof e2 == "function" ? e2 : () => e2;
    return new ae2({ ...y3(this._def), innerType: this, catchValue: t, typeName: p2.ZodCatch });
  }
  describe(e2) {
    let t = this.constructor;
    return new t({ ...this._def, description: e2 });
  }
  pipe(e2) {
    return Q2.create(this, e2);
  }
  isOptional() {
    return this.safeParse(void 0).success;
  }
  isNullable() {
    return this.safeParse(null).success;
  }
};
var je2 = /^c[^\s-]{8,}$/i;
var Re2 = /^[a-z][a-z0-9]*$/;
var Ae2 = /[0-9A-HJKMNP-TV-Z]{26}/;
var Ze2 = /^([a-f0-9]{8}-[a-f0-9]{4}-[1-5][a-f0-9]{3}-[a-f0-9]{4}-[a-f0-9]{12}|00000000-0000-0000-0000-000000000000)$/i;
var Me2 = /^(([^<>()[\]\\.,;:\s@\"]+(\.[^<>()[\]\\.,;:\s@\"]+)*)|(\".+\"))@((\[(((25[0-5])|(2[0-4][0-9])|(1[0-9]{2})|([0-9]{1,2}))\.){3}((25[0-5])|(2[0-4][0-9])|(1[0-9]{2})|([0-9]{1,2}))\])|(\[IPv6:(([a-f0-9]{1,4}:){7}|::([a-f0-9]{1,4}:){0,6}|([a-f0-9]{1,4}:){1}:([a-f0-9]{1,4}:){0,5}|([a-f0-9]{1,4}:){2}:([a-f0-9]{1,4}:){0,4}|([a-f0-9]{1,4}:){3}:([a-f0-9]{1,4}:){0,3}|([a-f0-9]{1,4}:){4}:([a-f0-9]{1,4}:){0,2}|([a-f0-9]{1,4}:){5}:([a-f0-9]{1,4}:){0,1})([a-f0-9]{1,4}|(((25[0-5])|(2[0-4][0-9])|(1[0-9]{2})|([0-9]{1,2}))\.){3}((25[0-5])|(2[0-4][0-9])|(1[0-9]{2})|([0-9]{1,2})))\])|([A-Za-z0-9]([A-Za-z0-9-]*[A-Za-z0-9])*(\.[A-Za-z]{2,})+))$/;
var Ve2 = /^(\p{Extended_Pictographic}|\p{Emoji_Component})+$/u;
var $e2 = /^(((25[0-5])|(2[0-4][0-9])|(1[0-9]{2})|([0-9]{1,2}))\.){3}((25[0-5])|(2[0-4][0-9])|(1[0-9]{2})|([0-9]{1,2}))$/;
var Pe2 = /^(([a-f0-9]{1,4}:){7}|::([a-f0-9]{1,4}:){0,6}|([a-f0-9]{1,4}:){1}:([a-f0-9]{1,4}:){0,5}|([a-f0-9]{1,4}:){2}:([a-f0-9]{1,4}:){0,4}|([a-f0-9]{1,4}:){3}:([a-f0-9]{1,4}:){0,3}|([a-f0-9]{1,4}:){4}:([a-f0-9]{1,4}:){0,2}|([a-f0-9]{1,4}:){5}:([a-f0-9]{1,4}:){0,1})([a-f0-9]{1,4}|(((25[0-5])|(2[0-4][0-9])|(1[0-9]{2})|([0-9]{1,2}))\.){3}((25[0-5])|(2[0-4][0-9])|(1[0-9]{2})|([0-9]{1,2})))$/;
var Le2 = (s) => s.precision ? s.offset ? new RegExp(`^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{${s.precision}}(([+-]\\d{2}(:?\\d{2})?)|Z)$`) : new RegExp(`^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{${s.precision}}Z$`) : s.precision === 0 ? s.offset ? new RegExp("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(([+-]\\d{2}(:?\\d{2})?)|Z)$") : new RegExp("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z$") : s.offset ? new RegExp("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?(([+-]\\d{2}(:?\\d{2})?)|Z)$") : new RegExp("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z$");
function ze2(s, e2) {
  return !!((e2 === "v4" || !e2) && $e2.test(s) || (e2 === "v6" || !e2) && Pe2.test(s));
}
var w2 = class extends v4 {
  constructor() {
    super(...arguments), this._regex = (e2, t, r2) => this.refinement((n2) => e2.test(n2), { validation: t, code: c2.invalid_string, ...h.errToObj(r2) }), this.nonempty = (e2) => this.min(1, h.errToObj(e2)), this.trim = () => new w2({ ...this._def, checks: [...this._def.checks, { kind: "trim" }] }), this.toLowerCase = () => new w2({ ...this._def, checks: [...this._def.checks, { kind: "toLowerCase" }] }), this.toUpperCase = () => new w2({ ...this._def, checks: [...this._def.checks, { kind: "toUpperCase" }] });
  }
  _parse(e2) {
    if (this._def.coerce && (e2.data = String(e2.data)), this._getType(e2) !== d.string) {
      let a3 = this._getOrReturnCtx(e2);
      return u2(a3, { code: c2.invalid_type, expected: d.string, received: a3.parsedType }), m2;
    }
    let r2 = new k4(), n2;
    for (let a3 of this._def.checks)
      if (a3.kind === "min")
        e2.data.length < a3.value && (n2 = this._getOrReturnCtx(e2, n2), u2(n2, { code: c2.too_small, minimum: a3.value, type: "string", inclusive: true, exact: false, message: a3.message }), r2.dirty());
      else if (a3.kind === "max")
        e2.data.length > a3.value && (n2 = this._getOrReturnCtx(e2, n2), u2(n2, { code: c2.too_big, maximum: a3.value, type: "string", inclusive: true, exact: false, message: a3.message }), r2.dirty());
      else if (a3.kind === "length") {
        let i4 = e2.data.length > a3.value, o = e2.data.length < a3.value;
        (i4 || o) && (n2 = this._getOrReturnCtx(e2, n2), i4 ? u2(n2, { code: c2.too_big, maximum: a3.value, type: "string", inclusive: true, exact: true, message: a3.message }) : o && u2(n2, { code: c2.too_small, minimum: a3.value, type: "string", inclusive: true, exact: true, message: a3.message }), r2.dirty());
      } else if (a3.kind === "email")
        Me2.test(e2.data) || (n2 = this._getOrReturnCtx(e2, n2), u2(n2, { validation: "email", code: c2.invalid_string, message: a3.message }), r2.dirty());
      else if (a3.kind === "emoji")
        Ve2.test(e2.data) || (n2 = this._getOrReturnCtx(e2, n2), u2(n2, { validation: "emoji", code: c2.invalid_string, message: a3.message }), r2.dirty());
      else if (a3.kind === "uuid")
        Ze2.test(e2.data) || (n2 = this._getOrReturnCtx(e2, n2), u2(n2, { validation: "uuid", code: c2.invalid_string, message: a3.message }), r2.dirty());
      else if (a3.kind === "cuid")
        je2.test(e2.data) || (n2 = this._getOrReturnCtx(e2, n2), u2(n2, { validation: "cuid", code: c2.invalid_string, message: a3.message }), r2.dirty());
      else if (a3.kind === "cuid2")
        Re2.test(e2.data) || (n2 = this._getOrReturnCtx(e2, n2), u2(n2, { validation: "cuid2", code: c2.invalid_string, message: a3.message }), r2.dirty());
      else if (a3.kind === "ulid")
        Ae2.test(e2.data) || (n2 = this._getOrReturnCtx(e2, n2), u2(n2, { validation: "ulid", code: c2.invalid_string, message: a3.message }), r2.dirty());
      else if (a3.kind === "url")
        try {
          new URL(e2.data);
        } catch {
          n2 = this._getOrReturnCtx(e2, n2), u2(n2, { validation: "url", code: c2.invalid_string, message: a3.message }), r2.dirty();
        }
      else
        a3.kind === "regex" ? (a3.regex.lastIndex = 0, a3.regex.test(e2.data) || (n2 = this._getOrReturnCtx(e2, n2), u2(n2, { validation: "regex", code: c2.invalid_string, message: a3.message }), r2.dirty())) : a3.kind === "trim" ? e2.data = e2.data.trim() : a3.kind === "includes" ? e2.data.includes(a3.value, a3.position) || (n2 = this._getOrReturnCtx(e2, n2), u2(n2, { code: c2.invalid_string, validation: { includes: a3.value, position: a3.position }, message: a3.message }), r2.dirty()) : a3.kind === "toLowerCase" ? e2.data = e2.data.toLowerCase() : a3.kind === "toUpperCase" ? e2.data = e2.data.toUpperCase() : a3.kind === "startsWith" ? e2.data.startsWith(a3.value) || (n2 = this._getOrReturnCtx(e2, n2), u2(n2, { code: c2.invalid_string, validation: { startsWith: a3.value }, message: a3.message }), r2.dirty()) : a3.kind === "endsWith" ? e2.data.endsWith(a3.value) || (n2 = this._getOrReturnCtx(e2, n2), u2(n2, { code: c2.invalid_string, validation: { endsWith: a3.value }, message: a3.message }), r2.dirty()) : a3.kind === "datetime" ? Le2(a3).test(e2.data) || (n2 = this._getOrReturnCtx(e2, n2), u2(n2, { code: c2.invalid_string, validation: "datetime", message: a3.message }), r2.dirty()) : a3.kind === "ip" ? ze2(e2.data, a3.version) || (n2 = this._getOrReturnCtx(e2, n2), u2(n2, { validation: "ip", code: c2.invalid_string, message: a3.message }), r2.dirty()) : g2.assertNever(a3);
    return { status: r2.value, value: e2.data };
  }
  _addCheck(e2) {
    return new w2({ ...this._def, checks: [...this._def.checks, e2] });
  }
  email(e2) {
    return this._addCheck({ kind: "email", ...h.errToObj(e2) });
  }
  url(e2) {
    return this._addCheck({ kind: "url", ...h.errToObj(e2) });
  }
  emoji(e2) {
    return this._addCheck({ kind: "emoji", ...h.errToObj(e2) });
  }
  uuid(e2) {
    return this._addCheck({ kind: "uuid", ...h.errToObj(e2) });
  }
  cuid(e2) {
    return this._addCheck({ kind: "cuid", ...h.errToObj(e2) });
  }
  cuid2(e2) {
    return this._addCheck({ kind: "cuid2", ...h.errToObj(e2) });
  }
  ulid(e2) {
    return this._addCheck({ kind: "ulid", ...h.errToObj(e2) });
  }
  ip(e2) {
    return this._addCheck({ kind: "ip", ...h.errToObj(e2) });
  }
  datetime(e2) {
    var t;
    return typeof e2 == "string" ? this._addCheck({ kind: "datetime", precision: null, offset: false, message: e2 }) : this._addCheck({ kind: "datetime", precision: typeof e2?.precision > "u" ? null : e2?.precision, offset: (t = e2?.offset) !== null && t !== void 0 ? t : false, ...h.errToObj(e2?.message) });
  }
  regex(e2, t) {
    return this._addCheck({ kind: "regex", regex: e2, ...h.errToObj(t) });
  }
  includes(e2, t) {
    return this._addCheck({ kind: "includes", value: e2, position: t?.position, ...h.errToObj(t?.message) });
  }
  startsWith(e2, t) {
    return this._addCheck({ kind: "startsWith", value: e2, ...h.errToObj(t) });
  }
  endsWith(e2, t) {
    return this._addCheck({ kind: "endsWith", value: e2, ...h.errToObj(t) });
  }
  min(e2, t) {
    return this._addCheck({ kind: "min", value: e2, ...h.errToObj(t) });
  }
  max(e2, t) {
    return this._addCheck({ kind: "max", value: e2, ...h.errToObj(t) });
  }
  length(e2, t) {
    return this._addCheck({ kind: "length", value: e2, ...h.errToObj(t) });
  }
  get isDatetime() {
    return !!this._def.checks.find((e2) => e2.kind === "datetime");
  }
  get isEmail() {
    return !!this._def.checks.find((e2) => e2.kind === "email");
  }
  get isURL() {
    return !!this._def.checks.find((e2) => e2.kind === "url");
  }
  get isEmoji() {
    return !!this._def.checks.find((e2) => e2.kind === "emoji");
  }
  get isUUID() {
    return !!this._def.checks.find((e2) => e2.kind === "uuid");
  }
  get isCUID() {
    return !!this._def.checks.find((e2) => e2.kind === "cuid");
  }
  get isCUID2() {
    return !!this._def.checks.find((e2) => e2.kind === "cuid2");
  }
  get isULID() {
    return !!this._def.checks.find((e2) => e2.kind === "ulid");
  }
  get isIP() {
    return !!this._def.checks.find((e2) => e2.kind === "ip");
  }
  get minLength() {
    let e2 = null;
    for (let t of this._def.checks)
      t.kind === "min" && (e2 === null || t.value > e2) && (e2 = t.value);
    return e2;
  }
  get maxLength() {
    let e2 = null;
    for (let t of this._def.checks)
      t.kind === "max" && (e2 === null || t.value < e2) && (e2 = t.value);
    return e2;
  }
};
w2.create = (s) => {
  var e2;
  return new w2({ checks: [], typeName: p2.ZodString, coerce: (e2 = s?.coerce) !== null && e2 !== void 0 ? e2 : false, ...y3(s) });
};
function De2(s, e2) {
  let t = (s.toString().split(".")[1] || "").length, r2 = (e2.toString().split(".")[1] || "").length, n2 = t > r2 ? t : r2, a3 = parseInt(s.toFixed(n2).replace(".", "")), i4 = parseInt(e2.toFixed(n2).replace(".", ""));
  return a3 % i4 / Math.pow(10, n2);
}
var j4 = class extends v4 {
  constructor() {
    super(...arguments), this.min = this.gte, this.max = this.lte, this.step = this.multipleOf;
  }
  _parse(e2) {
    if (this._def.coerce && (e2.data = Number(e2.data)), this._getType(e2) !== d.number) {
      let a3 = this._getOrReturnCtx(e2);
      return u2(a3, { code: c2.invalid_type, expected: d.number, received: a3.parsedType }), m2;
    }
    let r2, n2 = new k4();
    for (let a3 of this._def.checks)
      a3.kind === "int" ? g2.isInteger(e2.data) || (r2 = this._getOrReturnCtx(e2, r2), u2(r2, { code: c2.invalid_type, expected: "integer", received: "float", message: a3.message }), n2.dirty()) : a3.kind === "min" ? (a3.inclusive ? e2.data < a3.value : e2.data <= a3.value) && (r2 = this._getOrReturnCtx(e2, r2), u2(r2, { code: c2.too_small, minimum: a3.value, type: "number", inclusive: a3.inclusive, exact: false, message: a3.message }), n2.dirty()) : a3.kind === "max" ? (a3.inclusive ? e2.data > a3.value : e2.data >= a3.value) && (r2 = this._getOrReturnCtx(e2, r2), u2(r2, { code: c2.too_big, maximum: a3.value, type: "number", inclusive: a3.inclusive, exact: false, message: a3.message }), n2.dirty()) : a3.kind === "multipleOf" ? De2(e2.data, a3.value) !== 0 && (r2 = this._getOrReturnCtx(e2, r2), u2(r2, { code: c2.not_multiple_of, multipleOf: a3.value, message: a3.message }), n2.dirty()) : a3.kind === "finite" ? Number.isFinite(e2.data) || (r2 = this._getOrReturnCtx(e2, r2), u2(r2, { code: c2.not_finite, message: a3.message }), n2.dirty()) : g2.assertNever(a3);
    return { status: n2.value, value: e2.data };
  }
  gte(e2, t) {
    return this.setLimit("min", e2, true, h.toString(t));
  }
  gt(e2, t) {
    return this.setLimit("min", e2, false, h.toString(t));
  }
  lte(e2, t) {
    return this.setLimit("max", e2, true, h.toString(t));
  }
  lt(e2, t) {
    return this.setLimit("max", e2, false, h.toString(t));
  }
  setLimit(e2, t, r2, n2) {
    return new j4({ ...this._def, checks: [...this._def.checks, { kind: e2, value: t, inclusive: r2, message: h.toString(n2) }] });
  }
  _addCheck(e2) {
    return new j4({ ...this._def, checks: [...this._def.checks, e2] });
  }
  int(e2) {
    return this._addCheck({ kind: "int", message: h.toString(e2) });
  }
  positive(e2) {
    return this._addCheck({ kind: "min", value: 0, inclusive: false, message: h.toString(e2) });
  }
  negative(e2) {
    return this._addCheck({ kind: "max", value: 0, inclusive: false, message: h.toString(e2) });
  }
  nonpositive(e2) {
    return this._addCheck({ kind: "max", value: 0, inclusive: true, message: h.toString(e2) });
  }
  nonnegative(e2) {
    return this._addCheck({ kind: "min", value: 0, inclusive: true, message: h.toString(e2) });
  }
  multipleOf(e2, t) {
    return this._addCheck({ kind: "multipleOf", value: e2, message: h.toString(t) });
  }
  finite(e2) {
    return this._addCheck({ kind: "finite", message: h.toString(e2) });
  }
  safe(e2) {
    return this._addCheck({ kind: "min", inclusive: true, value: Number.MIN_SAFE_INTEGER, message: h.toString(e2) })._addCheck({ kind: "max", inclusive: true, value: Number.MAX_SAFE_INTEGER, message: h.toString(e2) });
  }
  get minValue() {
    let e2 = null;
    for (let t of this._def.checks)
      t.kind === "min" && (e2 === null || t.value > e2) && (e2 = t.value);
    return e2;
  }
  get maxValue() {
    let e2 = null;
    for (let t of this._def.checks)
      t.kind === "max" && (e2 === null || t.value < e2) && (e2 = t.value);
    return e2;
  }
  get isInt() {
    return !!this._def.checks.find((e2) => e2.kind === "int" || e2.kind === "multipleOf" && g2.isInteger(e2.value));
  }
  get isFinite() {
    let e2 = null, t = null;
    for (let r2 of this._def.checks) {
      if (r2.kind === "finite" || r2.kind === "int" || r2.kind === "multipleOf")
        return true;
      r2.kind === "min" ? (t === null || r2.value > t) && (t = r2.value) : r2.kind === "max" && (e2 === null || r2.value < e2) && (e2 = r2.value);
    }
    return Number.isFinite(t) && Number.isFinite(e2);
  }
};
j4.create = (s) => new j4({ checks: [], typeName: p2.ZodNumber, coerce: s?.coerce || false, ...y3(s) });
var R2 = class extends v4 {
  constructor() {
    super(...arguments), this.min = this.gte, this.max = this.lte;
  }
  _parse(e2) {
    if (this._def.coerce && (e2.data = BigInt(e2.data)), this._getType(e2) !== d.bigint) {
      let a3 = this._getOrReturnCtx(e2);
      return u2(a3, { code: c2.invalid_type, expected: d.bigint, received: a3.parsedType }), m2;
    }
    let r2, n2 = new k4();
    for (let a3 of this._def.checks)
      a3.kind === "min" ? (a3.inclusive ? e2.data < a3.value : e2.data <= a3.value) && (r2 = this._getOrReturnCtx(e2, r2), u2(r2, { code: c2.too_small, type: "bigint", minimum: a3.value, inclusive: a3.inclusive, message: a3.message }), n2.dirty()) : a3.kind === "max" ? (a3.inclusive ? e2.data > a3.value : e2.data >= a3.value) && (r2 = this._getOrReturnCtx(e2, r2), u2(r2, { code: c2.too_big, type: "bigint", maximum: a3.value, inclusive: a3.inclusive, message: a3.message }), n2.dirty()) : a3.kind === "multipleOf" ? e2.data % a3.value !== BigInt(0) && (r2 = this._getOrReturnCtx(e2, r2), u2(r2, { code: c2.not_multiple_of, multipleOf: a3.value, message: a3.message }), n2.dirty()) : g2.assertNever(a3);
    return { status: n2.value, value: e2.data };
  }
  gte(e2, t) {
    return this.setLimit("min", e2, true, h.toString(t));
  }
  gt(e2, t) {
    return this.setLimit("min", e2, false, h.toString(t));
  }
  lte(e2, t) {
    return this.setLimit("max", e2, true, h.toString(t));
  }
  lt(e2, t) {
    return this.setLimit("max", e2, false, h.toString(t));
  }
  setLimit(e2, t, r2, n2) {
    return new R2({ ...this._def, checks: [...this._def.checks, { kind: e2, value: t, inclusive: r2, message: h.toString(n2) }] });
  }
  _addCheck(e2) {
    return new R2({ ...this._def, checks: [...this._def.checks, e2] });
  }
  positive(e2) {
    return this._addCheck({ kind: "min", value: BigInt(0), inclusive: false, message: h.toString(e2) });
  }
  negative(e2) {
    return this._addCheck({ kind: "max", value: BigInt(0), inclusive: false, message: h.toString(e2) });
  }
  nonpositive(e2) {
    return this._addCheck({ kind: "max", value: BigInt(0), inclusive: true, message: h.toString(e2) });
  }
  nonnegative(e2) {
    return this._addCheck({ kind: "min", value: BigInt(0), inclusive: true, message: h.toString(e2) });
  }
  multipleOf(e2, t) {
    return this._addCheck({ kind: "multipleOf", value: e2, message: h.toString(t) });
  }
  get minValue() {
    let e2 = null;
    for (let t of this._def.checks)
      t.kind === "min" && (e2 === null || t.value > e2) && (e2 = t.value);
    return e2;
  }
  get maxValue() {
    let e2 = null;
    for (let t of this._def.checks)
      t.kind === "max" && (e2 === null || t.value < e2) && (e2 = t.value);
    return e2;
  }
};
R2.create = (s) => {
  var e2;
  return new R2({ checks: [], typeName: p2.ZodBigInt, coerce: (e2 = s?.coerce) !== null && e2 !== void 0 ? e2 : false, ...y3(s) });
};
var U2 = class extends v4 {
  _parse(e2) {
    if (this._def.coerce && (e2.data = !!e2.data), this._getType(e2) !== d.boolean) {
      let r2 = this._getOrReturnCtx(e2);
      return u2(r2, { code: c2.invalid_type, expected: d.boolean, received: r2.parsedType }), m2;
    }
    return b(e2.data);
  }
};
U2.create = (s) => new U2({ typeName: p2.ZodBoolean, coerce: s?.coerce || false, ...y3(s) });
var M3 = class extends v4 {
  _parse(e2) {
    if (this._def.coerce && (e2.data = new Date(e2.data)), this._getType(e2) !== d.date) {
      let a3 = this._getOrReturnCtx(e2);
      return u2(a3, { code: c2.invalid_type, expected: d.date, received: a3.parsedType }), m2;
    }
    if (isNaN(e2.data.getTime())) {
      let a3 = this._getOrReturnCtx(e2);
      return u2(a3, { code: c2.invalid_date }), m2;
    }
    let r2 = new k4(), n2;
    for (let a3 of this._def.checks)
      a3.kind === "min" ? e2.data.getTime() < a3.value && (n2 = this._getOrReturnCtx(e2, n2), u2(n2, { code: c2.too_small, message: a3.message, inclusive: true, exact: false, minimum: a3.value, type: "date" }), r2.dirty()) : a3.kind === "max" ? e2.data.getTime() > a3.value && (n2 = this._getOrReturnCtx(e2, n2), u2(n2, { code: c2.too_big, message: a3.message, inclusive: true, exact: false, maximum: a3.value, type: "date" }), r2.dirty()) : g2.assertNever(a3);
    return { status: r2.value, value: new Date(e2.data.getTime()) };
  }
  _addCheck(e2) {
    return new M3({ ...this._def, checks: [...this._def.checks, e2] });
  }
  min(e2, t) {
    return this._addCheck({ kind: "min", value: e2.getTime(), message: h.toString(t) });
  }
  max(e2, t) {
    return this._addCheck({ kind: "max", value: e2.getTime(), message: h.toString(t) });
  }
  get minDate() {
    let e2 = null;
    for (let t of this._def.checks)
      t.kind === "min" && (e2 === null || t.value > e2) && (e2 = t.value);
    return e2 != null ? new Date(e2) : null;
  }
  get maxDate() {
    let e2 = null;
    for (let t of this._def.checks)
      t.kind === "max" && (e2 === null || t.value < e2) && (e2 = t.value);
    return e2 != null ? new Date(e2) : null;
  }
};
M3.create = (s) => new M3({ checks: [], coerce: s?.coerce || false, typeName: p2.ZodDate, ...y3(s) });
var te = class extends v4 {
  _parse(e2) {
    if (this._getType(e2) !== d.symbol) {
      let r2 = this._getOrReturnCtx(e2);
      return u2(r2, { code: c2.invalid_type, expected: d.symbol, received: r2.parsedType }), m2;
    }
    return b(e2.data);
  }
};
te.create = (s) => new te({ typeName: p2.ZodSymbol, ...y3(s) });
var B3 = class extends v4 {
  _parse(e2) {
    if (this._getType(e2) !== d.undefined) {
      let r2 = this._getOrReturnCtx(e2);
      return u2(r2, { code: c2.invalid_type, expected: d.undefined, received: r2.parsedType }), m2;
    }
    return b(e2.data);
  }
};
B3.create = (s) => new B3({ typeName: p2.ZodUndefined, ...y3(s) });
var W2 = class extends v4 {
  _parse(e2) {
    if (this._getType(e2) !== d.null) {
      let r2 = this._getOrReturnCtx(e2);
      return u2(r2, { code: c2.invalid_type, expected: d.null, received: r2.parsedType }), m2;
    }
    return b(e2.data);
  }
};
W2.create = (s) => new W2({ typeName: p2.ZodNull, ...y3(s) });
var z2 = class extends v4 {
  constructor() {
    super(...arguments), this._any = true;
  }
  _parse(e2) {
    return b(e2.data);
  }
};
z2.create = (s) => new z2({ typeName: p2.ZodAny, ...y3(s) });
var Z = class extends v4 {
  constructor() {
    super(...arguments), this._unknown = true;
  }
  _parse(e2) {
    return b(e2.data);
  }
};
Z.create = (s) => new Z({ typeName: p2.ZodUnknown, ...y3(s) });
var I = class extends v4 {
  _parse(e2) {
    let t = this._getOrReturnCtx(e2);
    return u2(t, { code: c2.invalid_type, expected: d.never, received: t.parsedType }), m2;
  }
};
I.create = (s) => new I({ typeName: p2.ZodNever, ...y3(s) });
var se2 = class extends v4 {
  _parse(e2) {
    if (this._getType(e2) !== d.undefined) {
      let r2 = this._getOrReturnCtx(e2);
      return u2(r2, { code: c2.invalid_type, expected: d.void, received: r2.parsedType }), m2;
    }
    return b(e2.data);
  }
};
se2.create = (s) => new se2({ typeName: p2.ZodVoid, ...y3(s) });
var S2 = class extends v4 {
  _parse(e2) {
    let { ctx: t, status: r2 } = this._processInputParams(e2), n2 = this._def;
    if (t.parsedType !== d.array)
      return u2(t, { code: c2.invalid_type, expected: d.array, received: t.parsedType }), m2;
    if (n2.exactLength !== null) {
      let i4 = t.data.length > n2.exactLength.value, o = t.data.length < n2.exactLength.value;
      (i4 || o) && (u2(t, { code: i4 ? c2.too_big : c2.too_small, minimum: o ? n2.exactLength.value : void 0, maximum: i4 ? n2.exactLength.value : void 0, type: "array", inclusive: true, exact: true, message: n2.exactLength.message }), r2.dirty());
    }
    if (n2.minLength !== null && t.data.length < n2.minLength.value && (u2(t, { code: c2.too_small, minimum: n2.minLength.value, type: "array", inclusive: true, exact: false, message: n2.minLength.message }), r2.dirty()), n2.maxLength !== null && t.data.length > n2.maxLength.value && (u2(t, { code: c2.too_big, maximum: n2.maxLength.value, type: "array", inclusive: true, exact: false, message: n2.maxLength.message }), r2.dirty()), t.common.async)
      return Promise.all([...t.data].map((i4, o) => n2.type._parseAsync(new O2(t, i4, t.path, o)))).then((i4) => k4.mergeArray(r2, i4));
    let a3 = [...t.data].map((i4, o) => n2.type._parseSync(new O2(t, i4, t.path, o)));
    return k4.mergeArray(r2, a3);
  }
  get element() {
    return this._def.type;
  }
  min(e2, t) {
    return new S2({ ...this._def, minLength: { value: e2, message: h.toString(t) } });
  }
  max(e2, t) {
    return new S2({ ...this._def, maxLength: { value: e2, message: h.toString(t) } });
  }
  length(e2, t) {
    return new S2({ ...this._def, exactLength: { value: e2, message: h.toString(t) } });
  }
  nonempty(e2) {
    return this.min(1, e2);
  }
};
S2.create = (s, e2) => new S2({ type: s, minLength: null, maxLength: null, exactLength: null, typeName: p2.ZodArray, ...y3(e2) });
function ee2(s) {
  if (s instanceof x2) {
    let e2 = {};
    for (let t in s.shape) {
      let r2 = s.shape[t];
      e2[t] = E2.create(ee2(r2));
    }
    return new x2({ ...s._def, shape: () => e2 });
  } else
    return s instanceof S2 ? new S2({ ...s._def, type: ee2(s.element) }) : s instanceof E2 ? E2.create(ee2(s.unwrap())) : s instanceof $ ? $.create(ee2(s.unwrap())) : s instanceof N2 ? N2.create(s.items.map((e2) => ee2(e2))) : s;
}
var x2 = class extends v4 {
  constructor() {
    super(...arguments), this._cached = null, this.nonstrict = this.passthrough, this.augment = this.extend;
  }
  _getCached() {
    if (this._cached !== null)
      return this._cached;
    let e2 = this._def.shape(), t = g2.objectKeys(e2);
    return this._cached = { shape: e2, keys: t };
  }
  _parse(e2) {
    if (this._getType(e2) !== d.object) {
      let l2 = this._getOrReturnCtx(e2);
      return u2(l2, { code: c2.invalid_type, expected: d.object, received: l2.parsedType }), m2;
    }
    let { status: r2, ctx: n2 } = this._processInputParams(e2), { shape: a3, keys: i4 } = this._getCached(), o = [];
    if (!(this._def.catchall instanceof I && this._def.unknownKeys === "strip"))
      for (let l2 in n2.data)
        i4.includes(l2) || o.push(l2);
    let f = [];
    for (let l2 of i4) {
      let _4 = a3[l2], F3 = n2.data[l2];
      f.push({ key: { status: "valid", value: l2 }, value: _4._parse(new O2(n2, F3, n2.path, l2)), alwaysSet: l2 in n2.data });
    }
    if (this._def.catchall instanceof I) {
      let l2 = this._def.unknownKeys;
      if (l2 === "passthrough")
        for (let _4 of o)
          f.push({ key: { status: "valid", value: _4 }, value: { status: "valid", value: n2.data[_4] } });
      else if (l2 === "strict")
        o.length > 0 && (u2(n2, { code: c2.unrecognized_keys, keys: o }), r2.dirty());
      else if (l2 !== "strip")
        throw new Error("Internal ZodObject error: invalid unknownKeys value.");
    } else {
      let l2 = this._def.catchall;
      for (let _4 of o) {
        let F3 = n2.data[_4];
        f.push({ key: { status: "valid", value: _4 }, value: l2._parse(new O2(n2, F3, n2.path, _4)), alwaysSet: _4 in n2.data });
      }
    }
    return n2.common.async ? Promise.resolve().then(async () => {
      let l2 = [];
      for (let _4 of f) {
        let F3 = await _4.key;
        l2.push({ key: F3, value: await _4.value, alwaysSet: _4.alwaysSet });
      }
      return l2;
    }).then((l2) => k4.mergeObjectSync(r2, l2)) : k4.mergeObjectSync(r2, f);
  }
  get shape() {
    return this._def.shape();
  }
  strict(e2) {
    return h.errToObj, new x2({ ...this._def, unknownKeys: "strict", ...e2 !== void 0 ? { errorMap: (t, r2) => {
      var n2, a3, i4, o;
      let f = (i4 = (a3 = (n2 = this._def).errorMap) === null || a3 === void 0 ? void 0 : a3.call(n2, t, r2).message) !== null && i4 !== void 0 ? i4 : r2.defaultError;
      return t.code === "unrecognized_keys" ? { message: (o = h.errToObj(e2).message) !== null && o !== void 0 ? o : f } : { message: f };
    } } : {} });
  }
  strip() {
    return new x2({ ...this._def, unknownKeys: "strip" });
  }
  passthrough() {
    return new x2({ ...this._def, unknownKeys: "passthrough" });
  }
  extend(e2) {
    return new x2({ ...this._def, shape: () => ({ ...this._def.shape(), ...e2 }) });
  }
  merge(e2) {
    return new x2({ unknownKeys: e2._def.unknownKeys, catchall: e2._def.catchall, shape: () => ({ ...this._def.shape(), ...e2._def.shape() }), typeName: p2.ZodObject });
  }
  setKey(e2, t) {
    return this.augment({ [e2]: t });
  }
  catchall(e2) {
    return new x2({ ...this._def, catchall: e2 });
  }
  pick(e2) {
    let t = {};
    return g2.objectKeys(e2).forEach((r2) => {
      e2[r2] && this.shape[r2] && (t[r2] = this.shape[r2]);
    }), new x2({ ...this._def, shape: () => t });
  }
  omit(e2) {
    let t = {};
    return g2.objectKeys(this.shape).forEach((r2) => {
      e2[r2] || (t[r2] = this.shape[r2]);
    }), new x2({ ...this._def, shape: () => t });
  }
  deepPartial() {
    return ee2(this);
  }
  partial(e2) {
    let t = {};
    return g2.objectKeys(this.shape).forEach((r2) => {
      let n2 = this.shape[r2];
      e2 && !e2[r2] ? t[r2] = n2 : t[r2] = n2.optional();
    }), new x2({ ...this._def, shape: () => t });
  }
  required(e2) {
    let t = {};
    return g2.objectKeys(this.shape).forEach((r2) => {
      if (e2 && !e2[r2])
        t[r2] = this.shape[r2];
      else {
        let a3 = this.shape[r2];
        for (; a3 instanceof E2; )
          a3 = a3._def.innerType;
        t[r2] = a3;
      }
    }), new x2({ ...this._def, shape: () => t });
  }
  keyof() {
    return we2(g2.objectKeys(this.shape));
  }
};
x2.create = (s, e2) => new x2({ shape: () => s, unknownKeys: "strip", catchall: I.create(), typeName: p2.ZodObject, ...y3(e2) });
x2.strictCreate = (s, e2) => new x2({ shape: () => s, unknownKeys: "strict", catchall: I.create(), typeName: p2.ZodObject, ...y3(e2) });
x2.lazycreate = (s, e2) => new x2({ shape: s, unknownKeys: "strip", catchall: I.create(), typeName: p2.ZodObject, ...y3(e2) });
var q = class extends v4 {
  _parse(e2) {
    let { ctx: t } = this._processInputParams(e2), r2 = this._def.options;
    function n2(a3) {
      for (let o of a3)
        if (o.result.status === "valid")
          return o.result;
      for (let o of a3)
        if (o.result.status === "dirty")
          return t.common.issues.push(...o.ctx.common.issues), o.result;
      let i4 = a3.map((o) => new T2(o.ctx.common.issues));
      return u2(t, { code: c2.invalid_union, unionErrors: i4 }), m2;
    }
    if (t.common.async)
      return Promise.all(r2.map(async (a3) => {
        let i4 = { ...t, common: { ...t.common, issues: [] }, parent: null };
        return { result: await a3._parseAsync({ data: t.data, path: t.path, parent: i4 }), ctx: i4 };
      })).then(n2);
    {
      let a3, i4 = [];
      for (let f of r2) {
        let l2 = { ...t, common: { ...t.common, issues: [] }, parent: null }, _4 = f._parseSync({ data: t.data, path: t.path, parent: l2 });
        if (_4.status === "valid")
          return _4;
        _4.status === "dirty" && !a3 && (a3 = { result: _4, ctx: l2 }), l2.common.issues.length && i4.push(l2.common.issues);
      }
      if (a3)
        return t.common.issues.push(...a3.ctx.common.issues), a3.result;
      let o = i4.map((f) => new T2(f));
      return u2(t, { code: c2.invalid_union, unionErrors: o }), m2;
    }
  }
  get options() {
    return this._def.options;
  }
};
q.create = (s, e2) => new q({ options: s, typeName: p2.ZodUnion, ...y3(e2) });
var ce2 = (s) => s instanceof H4 ? ce2(s.schema) : s instanceof C5 ? ce2(s.innerType()) : s instanceof G4 ? [s.value] : s instanceof A3 ? s.options : s instanceof X2 ? Object.keys(s.enum) : s instanceof K3 ? ce2(s._def.innerType) : s instanceof B3 ? [void 0] : s instanceof W2 ? [null] : null;
var re2 = class extends v4 {
  _parse(e2) {
    let { ctx: t } = this._processInputParams(e2);
    if (t.parsedType !== d.object)
      return u2(t, { code: c2.invalid_type, expected: d.object, received: t.parsedType }), m2;
    let r2 = this.discriminator, n2 = t.data[r2], a3 = this.optionsMap.get(n2);
    return a3 ? t.common.async ? a3._parseAsync({ data: t.data, path: t.path, parent: t }) : a3._parseSync({ data: t.data, path: t.path, parent: t }) : (u2(t, { code: c2.invalid_union_discriminator, options: Array.from(this.optionsMap.keys()), path: [r2] }), m2);
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
  static create(e2, t, r2) {
    let n2 = /* @__PURE__ */ new Map();
    for (let a3 of t) {
      let i4 = ce2(a3.shape[e2]);
      if (!i4)
        throw new Error(`A discriminator value for key \`${e2}\` could not be extracted from all schema options`);
      for (let o of i4) {
        if (n2.has(o))
          throw new Error(`Discriminator property ${String(e2)} has duplicate value ${String(o)}`);
        n2.set(o, a3);
      }
    }
    return new re2({ typeName: p2.ZodDiscriminatedUnion, discriminator: e2, options: t, optionsMap: n2, ...y3(r2) });
  }
};
function _e2(s, e2) {
  let t = P2(s), r2 = P2(e2);
  if (s === e2)
    return { valid: true, data: s };
  if (t === d.object && r2 === d.object) {
    let n2 = g2.objectKeys(e2), a3 = g2.objectKeys(s).filter((o) => n2.indexOf(o) !== -1), i4 = { ...s, ...e2 };
    for (let o of a3) {
      let f = _e2(s[o], e2[o]);
      if (!f.valid)
        return { valid: false };
      i4[o] = f.data;
    }
    return { valid: true, data: i4 };
  } else if (t === d.array && r2 === d.array) {
    if (s.length !== e2.length)
      return { valid: false };
    let n2 = [];
    for (let a3 = 0; a3 < s.length; a3++) {
      let i4 = s[a3], o = e2[a3], f = _e2(i4, o);
      if (!f.valid)
        return { valid: false };
      n2.push(f.data);
    }
    return { valid: true, data: n2 };
  } else
    return t === d.date && r2 === d.date && +s == +e2 ? { valid: true, data: s } : { valid: false };
}
var J4 = class extends v4 {
  _parse(e2) {
    let { status: t, ctx: r2 } = this._processInputParams(e2), n2 = (a3, i4) => {
      if (ye2(a3) || ye2(i4))
        return m2;
      let o = _e2(a3.value, i4.value);
      return o.valid ? ((ve2(a3) || ve2(i4)) && t.dirty(), { status: t.value, value: o.data }) : (u2(r2, { code: c2.invalid_intersection_types }), m2);
    };
    return r2.common.async ? Promise.all([this._def.left._parseAsync({ data: r2.data, path: r2.path, parent: r2 }), this._def.right._parseAsync({ data: r2.data, path: r2.path, parent: r2 })]).then(([a3, i4]) => n2(a3, i4)) : n2(this._def.left._parseSync({ data: r2.data, path: r2.path, parent: r2 }), this._def.right._parseSync({ data: r2.data, path: r2.path, parent: r2 }));
  }
};
J4.create = (s, e2, t) => new J4({ left: s, right: e2, typeName: p2.ZodIntersection, ...y3(t) });
var N2 = class extends v4 {
  _parse(e2) {
    let { status: t, ctx: r2 } = this._processInputParams(e2);
    if (r2.parsedType !== d.array)
      return u2(r2, { code: c2.invalid_type, expected: d.array, received: r2.parsedType }), m2;
    if (r2.data.length < this._def.items.length)
      return u2(r2, { code: c2.too_small, minimum: this._def.items.length, inclusive: true, exact: false, type: "array" }), m2;
    !this._def.rest && r2.data.length > this._def.items.length && (u2(r2, { code: c2.too_big, maximum: this._def.items.length, inclusive: true, exact: false, type: "array" }), t.dirty());
    let a3 = [...r2.data].map((i4, o) => {
      let f = this._def.items[o] || this._def.rest;
      return f ? f._parse(new O2(r2, i4, r2.path, o)) : null;
    }).filter((i4) => !!i4);
    return r2.common.async ? Promise.all(a3).then((i4) => k4.mergeArray(t, i4)) : k4.mergeArray(t, a3);
  }
  get items() {
    return this._def.items;
  }
  rest(e2) {
    return new N2({ ...this._def, rest: e2 });
  }
};
N2.create = (s, e2) => {
  if (!Array.isArray(s))
    throw new Error("You must pass an array of schemas to z.tuple([ ... ])");
  return new N2({ items: s, typeName: p2.ZodTuple, rest: null, ...y3(e2) });
};
var Y2 = class extends v4 {
  get keySchema() {
    return this._def.keyType;
  }
  get valueSchema() {
    return this._def.valueType;
  }
  _parse(e2) {
    let { status: t, ctx: r2 } = this._processInputParams(e2);
    if (r2.parsedType !== d.object)
      return u2(r2, { code: c2.invalid_type, expected: d.object, received: r2.parsedType }), m2;
    let n2 = [], a3 = this._def.keyType, i4 = this._def.valueType;
    for (let o in r2.data)
      n2.push({ key: a3._parse(new O2(r2, o, r2.path, o)), value: i4._parse(new O2(r2, r2.data[o], r2.path, o)) });
    return r2.common.async ? k4.mergeObjectAsync(t, n2) : k4.mergeObjectSync(t, n2);
  }
  get element() {
    return this._def.valueType;
  }
  static create(e2, t, r2) {
    return t instanceof v4 ? new Y2({ keyType: e2, valueType: t, typeName: p2.ZodRecord, ...y3(r2) }) : new Y2({ keyType: w2.create(), valueType: e2, typeName: p2.ZodRecord, ...y3(t) });
  }
};
var ne2 = class extends v4 {
  _parse(e2) {
    let { status: t, ctx: r2 } = this._processInputParams(e2);
    if (r2.parsedType !== d.map)
      return u2(r2, { code: c2.invalid_type, expected: d.map, received: r2.parsedType }), m2;
    let n2 = this._def.keyType, a3 = this._def.valueType, i4 = [...r2.data.entries()].map(([o, f], l2) => ({ key: n2._parse(new O2(r2, o, r2.path, [l2, "key"])), value: a3._parse(new O2(r2, f, r2.path, [l2, "value"])) }));
    if (r2.common.async) {
      let o = /* @__PURE__ */ new Map();
      return Promise.resolve().then(async () => {
        for (let f of i4) {
          let l2 = await f.key, _4 = await f.value;
          if (l2.status === "aborted" || _4.status === "aborted")
            return m2;
          (l2.status === "dirty" || _4.status === "dirty") && t.dirty(), o.set(l2.value, _4.value);
        }
        return { status: t.value, value: o };
      });
    } else {
      let o = /* @__PURE__ */ new Map();
      for (let f of i4) {
        let l2 = f.key, _4 = f.value;
        if (l2.status === "aborted" || _4.status === "aborted")
          return m2;
        (l2.status === "dirty" || _4.status === "dirty") && t.dirty(), o.set(l2.value, _4.value);
      }
      return { status: t.value, value: o };
    }
  }
};
ne2.create = (s, e2, t) => new ne2({ valueType: e2, keyType: s, typeName: p2.ZodMap, ...y3(t) });
var V = class extends v4 {
  _parse(e2) {
    let { status: t, ctx: r2 } = this._processInputParams(e2);
    if (r2.parsedType !== d.set)
      return u2(r2, { code: c2.invalid_type, expected: d.set, received: r2.parsedType }), m2;
    let n2 = this._def;
    n2.minSize !== null && r2.data.size < n2.minSize.value && (u2(r2, { code: c2.too_small, minimum: n2.minSize.value, type: "set", inclusive: true, exact: false, message: n2.minSize.message }), t.dirty()), n2.maxSize !== null && r2.data.size > n2.maxSize.value && (u2(r2, { code: c2.too_big, maximum: n2.maxSize.value, type: "set", inclusive: true, exact: false, message: n2.maxSize.message }), t.dirty());
    let a3 = this._def.valueType;
    function i4(f) {
      let l2 = /* @__PURE__ */ new Set();
      for (let _4 of f) {
        if (_4.status === "aborted")
          return m2;
        _4.status === "dirty" && t.dirty(), l2.add(_4.value);
      }
      return { status: t.value, value: l2 };
    }
    let o = [...r2.data.values()].map((f, l2) => a3._parse(new O2(r2, f, r2.path, l2)));
    return r2.common.async ? Promise.all(o).then((f) => i4(f)) : i4(o);
  }
  min(e2, t) {
    return new V({ ...this._def, minSize: { value: e2, message: h.toString(t) } });
  }
  max(e2, t) {
    return new V({ ...this._def, maxSize: { value: e2, message: h.toString(t) } });
  }
  size(e2, t) {
    return this.min(e2, t).max(e2, t);
  }
  nonempty(e2) {
    return this.min(1, e2);
  }
};
V.create = (s, e2) => new V({ valueType: s, minSize: null, maxSize: null, typeName: p2.ZodSet, ...y3(e2) });
var L2 = class extends v4 {
  constructor() {
    super(...arguments), this.validate = this.implement;
  }
  _parse(e2) {
    let { ctx: t } = this._processInputParams(e2);
    if (t.parsedType !== d.function)
      return u2(t, { code: c2.invalid_type, expected: d.function, received: t.parsedType }), m2;
    function r2(o, f) {
      return ue2({ data: o, path: t.path, errorMaps: [t.common.contextualErrorMap, t.schemaErrorMap, de2(), oe2].filter((l2) => !!l2), issueData: { code: c2.invalid_arguments, argumentsError: f } });
    }
    function n2(o, f) {
      return ue2({ data: o, path: t.path, errorMaps: [t.common.contextualErrorMap, t.schemaErrorMap, de2(), oe2].filter((l2) => !!l2), issueData: { code: c2.invalid_return_type, returnTypeError: f } });
    }
    let a3 = { errorMap: t.common.contextualErrorMap }, i4 = t.data;
    return this._def.returns instanceof D3 ? b(async (...o) => {
      let f = new T2([]), l2 = await this._def.args.parseAsync(o, a3).catch((pe2) => {
        throw f.addIssue(r2(o, pe2)), f;
      }), _4 = await i4(...l2);
      return await this._def.returns._def.type.parseAsync(_4, a3).catch((pe2) => {
        throw f.addIssue(n2(_4, pe2)), f;
      });
    }) : b((...o) => {
      let f = this._def.args.safeParse(o, a3);
      if (!f.success)
        throw new T2([r2(o, f.error)]);
      let l2 = i4(...f.data), _4 = this._def.returns.safeParse(l2, a3);
      if (!_4.success)
        throw new T2([n2(l2, _4.error)]);
      return _4.data;
    });
  }
  parameters() {
    return this._def.args;
  }
  returnType() {
    return this._def.returns;
  }
  args(...e2) {
    return new L2({ ...this._def, args: N2.create(e2).rest(Z.create()) });
  }
  returns(e2) {
    return new L2({ ...this._def, returns: e2 });
  }
  implement(e2) {
    return this.parse(e2);
  }
  strictImplement(e2) {
    return this.parse(e2);
  }
  static create(e2, t, r2) {
    return new L2({ args: e2 || N2.create([]).rest(Z.create()), returns: t || Z.create(), typeName: p2.ZodFunction, ...y3(r2) });
  }
};
var H4 = class extends v4 {
  get schema() {
    return this._def.getter();
  }
  _parse(e2) {
    let { ctx: t } = this._processInputParams(e2);
    return this._def.getter()._parse({ data: t.data, path: t.path, parent: t });
  }
};
H4.create = (s, e2) => new H4({ getter: s, typeName: p2.ZodLazy, ...y3(e2) });
var G4 = class extends v4 {
  _parse(e2) {
    if (e2.data !== this._def.value) {
      let t = this._getOrReturnCtx(e2);
      return u2(t, { received: t.data, code: c2.invalid_literal, expected: this._def.value }), m2;
    }
    return { status: "valid", value: e2.data };
  }
  get value() {
    return this._def.value;
  }
};
G4.create = (s, e2) => new G4({ value: s, typeName: p2.ZodLiteral, ...y3(e2) });
function we2(s, e2) {
  return new A3({ values: s, typeName: p2.ZodEnum, ...y3(e2) });
}
var A3 = class extends v4 {
  _parse(e2) {
    if (typeof e2.data != "string") {
      let t = this._getOrReturnCtx(e2), r2 = this._def.values;
      return u2(t, { expected: g2.joinValues(r2), received: t.parsedType, code: c2.invalid_type }), m2;
    }
    if (this._def.values.indexOf(e2.data) === -1) {
      let t = this._getOrReturnCtx(e2), r2 = this._def.values;
      return u2(t, { received: t.data, code: c2.invalid_enum_value, options: r2 }), m2;
    }
    return b(e2.data);
  }
  get options() {
    return this._def.values;
  }
  get enum() {
    let e2 = {};
    for (let t of this._def.values)
      e2[t] = t;
    return e2;
  }
  get Values() {
    let e2 = {};
    for (let t of this._def.values)
      e2[t] = t;
    return e2;
  }
  get Enum() {
    let e2 = {};
    for (let t of this._def.values)
      e2[t] = t;
    return e2;
  }
  extract(e2) {
    return A3.create(e2);
  }
  exclude(e2) {
    return A3.create(this.options.filter((t) => !e2.includes(t)));
  }
};
A3.create = we2;
var X2 = class extends v4 {
  _parse(e2) {
    let t = g2.getValidEnumValues(this._def.values), r2 = this._getOrReturnCtx(e2);
    if (r2.parsedType !== d.string && r2.parsedType !== d.number) {
      let n2 = g2.objectValues(t);
      return u2(r2, { expected: g2.joinValues(n2), received: r2.parsedType, code: c2.invalid_type }), m2;
    }
    if (t.indexOf(e2.data) === -1) {
      let n2 = g2.objectValues(t);
      return u2(r2, { received: r2.data, code: c2.invalid_enum_value, options: n2 }), m2;
    }
    return b(e2.data);
  }
  get enum() {
    return this._def.values;
  }
};
X2.create = (s, e2) => new X2({ values: s, typeName: p2.ZodNativeEnum, ...y3(e2) });
var D3 = class extends v4 {
  unwrap() {
    return this._def.type;
  }
  _parse(e2) {
    let { ctx: t } = this._processInputParams(e2);
    if (t.parsedType !== d.promise && t.common.async === false)
      return u2(t, { code: c2.invalid_type, expected: d.promise, received: t.parsedType }), m2;
    let r2 = t.parsedType === d.promise ? t.data : Promise.resolve(t.data);
    return b(r2.then((n2) => this._def.type.parseAsync(n2, { path: t.path, errorMap: t.common.contextualErrorMap })));
  }
};
D3.create = (s, e2) => new D3({ type: s, typeName: p2.ZodPromise, ...y3(e2) });
var C5 = class extends v4 {
  innerType() {
    return this._def.schema;
  }
  sourceType() {
    return this._def.schema._def.typeName === p2.ZodEffects ? this._def.schema.sourceType() : this._def.schema;
  }
  _parse(e2) {
    let { status: t, ctx: r2 } = this._processInputParams(e2), n2 = this._def.effect || null;
    if (n2.type === "preprocess") {
      let i4 = n2.transform(r2.data);
      return r2.common.async ? Promise.resolve(i4).then((o) => this._def.schema._parseAsync({ data: o, path: r2.path, parent: r2 })) : this._def.schema._parseSync({ data: i4, path: r2.path, parent: r2 });
    }
    let a3 = { addIssue: (i4) => {
      u2(r2, i4), i4.fatal ? t.abort() : t.dirty();
    }, get path() {
      return r2.path;
    } };
    if (a3.addIssue = a3.addIssue.bind(a3), n2.type === "refinement") {
      let i4 = (o) => {
        let f = n2.refinement(o, a3);
        if (r2.common.async)
          return Promise.resolve(f);
        if (f instanceof Promise)
          throw new Error("Async refinement encountered during synchronous parse operation. Use .parseAsync instead.");
        return o;
      };
      if (r2.common.async === false) {
        let o = this._def.schema._parseSync({ data: r2.data, path: r2.path, parent: r2 });
        return o.status === "aborted" ? m2 : (o.status === "dirty" && t.dirty(), i4(o.value), { status: t.value, value: o.value });
      } else
        return this._def.schema._parseAsync({ data: r2.data, path: r2.path, parent: r2 }).then((o) => o.status === "aborted" ? m2 : (o.status === "dirty" && t.dirty(), i4(o.value).then(() => ({ status: t.value, value: o.value }))));
    }
    if (n2.type === "transform")
      if (r2.common.async === false) {
        let i4 = this._def.schema._parseSync({ data: r2.data, path: r2.path, parent: r2 });
        if (!le(i4))
          return i4;
        let o = n2.transform(i4.value, a3);
        if (o instanceof Promise)
          throw new Error("Asynchronous transform encountered during synchronous parse operation. Use .parseAsync instead.");
        return { status: t.value, value: o };
      } else
        return this._def.schema._parseAsync({ data: r2.data, path: r2.path, parent: r2 }).then((i4) => le(i4) ? Promise.resolve(n2.transform(i4.value, a3)).then((o) => ({ status: t.value, value: o })) : i4);
    g2.assertNever(n2);
  }
};
C5.create = (s, e2, t) => new C5({ schema: s, typeName: p2.ZodEffects, effect: e2, ...y3(t) });
C5.createWithPreprocess = (s, e2, t) => new C5({ schema: e2, effect: { type: "preprocess", transform: s }, typeName: p2.ZodEffects, ...y3(t) });
var E2 = class extends v4 {
  _parse(e2) {
    return this._getType(e2) === d.undefined ? b(void 0) : this._def.innerType._parse(e2);
  }
  unwrap() {
    return this._def.innerType;
  }
};
E2.create = (s, e2) => new E2({ innerType: s, typeName: p2.ZodOptional, ...y3(e2) });
var $ = class extends v4 {
  _parse(e2) {
    return this._getType(e2) === d.null ? b(null) : this._def.innerType._parse(e2);
  }
  unwrap() {
    return this._def.innerType;
  }
};
$.create = (s, e2) => new $({ innerType: s, typeName: p2.ZodNullable, ...y3(e2) });
var K3 = class extends v4 {
  _parse(e2) {
    let { ctx: t } = this._processInputParams(e2), r2 = t.data;
    return t.parsedType === d.undefined && (r2 = this._def.defaultValue()), this._def.innerType._parse({ data: r2, path: t.path, parent: t });
  }
  removeDefault() {
    return this._def.innerType;
  }
};
K3.create = (s, e2) => new K3({ innerType: s, typeName: p2.ZodDefault, defaultValue: typeof e2.default == "function" ? e2.default : () => e2.default, ...y3(e2) });
var ae2 = class extends v4 {
  _parse(e2) {
    let { ctx: t } = this._processInputParams(e2), r2 = { ...t, common: { ...t.common, issues: [] } }, n2 = this._def.innerType._parse({ data: r2.data, path: r2.path, parent: { ...r2 } });
    return fe(n2) ? n2.then((a3) => ({ status: "valid", value: a3.status === "valid" ? a3.value : this._def.catchValue({ get error() {
      return new T2(r2.common.issues);
    }, input: r2.data }) })) : { status: "valid", value: n2.status === "valid" ? n2.value : this._def.catchValue({ get error() {
      return new T2(r2.common.issues);
    }, input: r2.data }) };
  }
  removeCatch() {
    return this._def.innerType;
  }
};
ae2.create = (s, e2) => new ae2({ innerType: s, typeName: p2.ZodCatch, catchValue: typeof e2.catch == "function" ? e2.catch : () => e2.catch, ...y3(e2) });
var ie2 = class extends v4 {
  _parse(e2) {
    if (this._getType(e2) !== d.nan) {
      let r2 = this._getOrReturnCtx(e2);
      return u2(r2, { code: c2.invalid_type, expected: d.nan, received: r2.parsedType }), m2;
    }
    return { status: "valid", value: e2.data };
  }
};
ie2.create = (s) => new ie2({ typeName: p2.ZodNaN, ...y3(s) });
var Ue2 = Symbol("zod_brand");
var he2 = class extends v4 {
  _parse(e2) {
    let { ctx: t } = this._processInputParams(e2), r2 = t.data;
    return this._def.type._parse({ data: r2, path: t.path, parent: t });
  }
  unwrap() {
    return this._def.type;
  }
};
var Q2 = class extends v4 {
  _parse(e2) {
    let { status: t, ctx: r2 } = this._processInputParams(e2);
    if (r2.common.async)
      return (async () => {
        let a3 = await this._def.in._parseAsync({ data: r2.data, path: r2.path, parent: r2 });
        return a3.status === "aborted" ? m2 : a3.status === "dirty" ? (t.dirty(), be2(a3.value)) : this._def.out._parseAsync({ data: a3.value, path: r2.path, parent: r2 });
      })();
    {
      let n2 = this._def.in._parseSync({ data: r2.data, path: r2.path, parent: r2 });
      return n2.status === "aborted" ? m2 : n2.status === "dirty" ? (t.dirty(), { status: "dirty", value: n2.value }) : this._def.out._parseSync({ data: n2.value, path: r2.path, parent: r2 });
    }
  }
  static create(e2, t) {
    return new Q2({ in: e2, out: t, typeName: p2.ZodPipeline });
  }
};
var Te2 = (s, e2 = {}, t) => s ? z2.create().superRefine((r2, n2) => {
  var a3, i4;
  if (!s(r2)) {
    let o = typeof e2 == "function" ? e2(r2) : typeof e2 == "string" ? { message: e2 } : e2, f = (i4 = (a3 = o.fatal) !== null && a3 !== void 0 ? a3 : t) !== null && i4 !== void 0 ? i4 : true, l2 = typeof o == "string" ? { message: o } : o;
    n2.addIssue({ code: "custom", ...l2, fatal: f });
  }
}) : z2.create();
var Be2 = { object: x2.lazycreate };
var p2;
(function(s) {
  s.ZodString = "ZodString", s.ZodNumber = "ZodNumber", s.ZodNaN = "ZodNaN", s.ZodBigInt = "ZodBigInt", s.ZodBoolean = "ZodBoolean", s.ZodDate = "ZodDate", s.ZodSymbol = "ZodSymbol", s.ZodUndefined = "ZodUndefined", s.ZodNull = "ZodNull", s.ZodAny = "ZodAny", s.ZodUnknown = "ZodUnknown", s.ZodNever = "ZodNever", s.ZodVoid = "ZodVoid", s.ZodArray = "ZodArray", s.ZodObject = "ZodObject", s.ZodUnion = "ZodUnion", s.ZodDiscriminatedUnion = "ZodDiscriminatedUnion", s.ZodIntersection = "ZodIntersection", s.ZodTuple = "ZodTuple", s.ZodRecord = "ZodRecord", s.ZodMap = "ZodMap", s.ZodSet = "ZodSet", s.ZodFunction = "ZodFunction", s.ZodLazy = "ZodLazy", s.ZodLiteral = "ZodLiteral", s.ZodEnum = "ZodEnum", s.ZodEffects = "ZodEffects", s.ZodNativeEnum = "ZodNativeEnum", s.ZodOptional = "ZodOptional", s.ZodNullable = "ZodNullable", s.ZodDefault = "ZodDefault", s.ZodCatch = "ZodCatch", s.ZodPromise = "ZodPromise", s.ZodBranded = "ZodBranded", s.ZodPipeline = "ZodPipeline";
})(p2 || (p2 = {}));
var We2 = (s, e2 = { message: `Input not instance of ${s.name}` }) => Te2((t) => t instanceof s, e2);
var Se2 = w2.create;
var Ce2 = j4.create;
var qe2 = ie2.create;
var Je = R2.create;
var Oe = U2.create;
var Ye = M3.create;
var He = te.create;
var Ge = B3.create;
var Xe2 = W2.create;
var Ke2 = z2.create;
var Qe2 = Z.create;
var Fe2 = I.create;
var et3 = se2.create;
var tt2 = S2.create;
var st2 = x2.create;
var rt3 = x2.strictCreate;
var nt3 = q.create;
var at2 = re2.create;
var it2 = J4.create;
var ot3 = N2.create;
var ct2 = Y2.create;
var dt2 = ne2.create;
var ut2 = V.create;
var lt2 = L2.create;
var ft2 = H4.create;
var ht2 = G4.create;
var pt2 = A3.create;
var mt2 = X2.create;
var yt = D3.create;
var xe = C5.create;
var vt = E2.create;
var _t2 = $.create;
var gt2 = C5.createWithPreprocess;
var xt3 = Q2.create;
var kt3 = () => Se2().optional();
var bt2 = () => Ce2().optional();
var wt2 = () => Oe().optional();
var Tt2 = { string: (s) => w2.create({ ...s, coerce: true }), number: (s) => j4.create({ ...s, coerce: true }), boolean: (s) => U2.create({ ...s, coerce: true }), bigint: (s) => R2.create({ ...s, coerce: true }), date: (s) => M3.create({ ...s, coerce: true }) };
var St2 = m2;
var Ct3 = Object.freeze({ __proto__: null, defaultErrorMap: oe2, setErrorMap: Ee2, getErrorMap: de2, makeIssue: ue2, EMPTY_PATH: Ie, addIssueToContext: u2, ParseStatus: k4, INVALID: m2, DIRTY: be2, OK: b, isAborted: ye2, isDirty: ve2, isValid: le, isAsync: fe, get util() {
  return g2;
}, get objectUtil() {
  return me2;
}, ZodParsedType: d, getParsedType: P2, ZodType: v4, ZodString: w2, ZodNumber: j4, ZodBigInt: R2, ZodBoolean: U2, ZodDate: M3, ZodSymbol: te, ZodUndefined: B3, ZodNull: W2, ZodAny: z2, ZodUnknown: Z, ZodNever: I, ZodVoid: se2, ZodArray: S2, ZodObject: x2, ZodUnion: q, ZodDiscriminatedUnion: re2, ZodIntersection: J4, ZodTuple: N2, ZodRecord: Y2, ZodMap: ne2, ZodSet: V, ZodFunction: L2, ZodLazy: H4, ZodLiteral: G4, ZodEnum: A3, ZodNativeEnum: X2, ZodPromise: D3, ZodEffects: C5, ZodTransformer: C5, ZodOptional: E2, ZodNullable: $, ZodDefault: K3, ZodCatch: ae2, ZodNaN: ie2, BRAND: Ue2, ZodBranded: he2, ZodPipeline: Q2, custom: Te2, Schema: v4, ZodSchema: v4, late: Be2, get ZodFirstPartyTypeKind() {
  return p2;
}, coerce: Tt2, any: Ke2, array: tt2, bigint: Je, boolean: Oe, date: Ye, discriminatedUnion: at2, effect: xe, enum: pt2, function: lt2, instanceof: We2, intersection: it2, lazy: ft2, literal: ht2, map: dt2, nan: qe2, nativeEnum: mt2, never: Fe2, null: Xe2, nullable: _t2, number: Ce2, object: st2, oboolean: wt2, onumber: bt2, optional: vt, ostring: kt3, pipeline: xt3, preprocess: gt2, promise: yt, record: ct2, set: ut2, strictObject: rt3, string: Se2, symbol: He, transformer: xe, tuple: ot3, undefined: Ge, union: nt3, unknown: Qe2, void: et3, NEVER: St2, ZodIssueCode: c2, quotelessJson: Ne2, ZodError: T2 });

// src/server/deps.ts
var { jsProcess, http, ipc } = navigator.dweb;
var { ServerUrlInfo: ServerUrlInfo2, ServerStartResult: ServerStartResult2 } = http;
var { IpcHeaders: IpcHeaders2, IpcResponse: IpcResponse2, Ipc: Ipc3, IpcRequest: IpcRequest3, IpcEvent: IpcEvent2, IPC_METHOD: IPC_METHOD2 } = ipc;

// src/server/http-helper.ts
var { IpcHeaders: IpcHeaders3 } = navigator.dweb.ipc;
var cors = (headers) => {
  headers.init("Access-Control-Allow-Origin", "*");
  headers.init("Access-Control-Allow-Headers", "*");
  headers.init("Access-Control-Allow-Methods", "*");
  return headers;
};
var HttpServer = class {
  constructor() {
    this._serverP = http.createHttpDwebServer(jsProcess, this._getOptions());
    this._listener = this.getServer().then((server) => server.listen());
  }
  getServer() {
    return this._serverP;
  }
  getStartResult() {
    return this._serverP.then((server) => server.startResult);
  }
  async stop() {
    const server = await this._serverP;
    return await server.close();
  }
};

// https://esm.sh/v126/deep-object-diff@1.1.9/denonext/deep-object-diff.mjs
var u3 = (t) => t instanceof Date;
var m3 = (t) => Object.keys(t).length === 0;
var i3 = (t) => t != null && typeof t == "object";
var n = (t, ...e2) => Object.prototype.hasOwnProperty.call(t, ...e2);
var d2 = (t) => i3(t) && m3(t);
var p3 = () => /* @__PURE__ */ Object.create(null);
var D4 = (t, e2) => t === e2 || !i3(t) || !i3(e2) ? {} : Object.keys(e2).reduce((o, r2) => {
  if (n(t, r2)) {
    let f = D4(t[r2], e2[r2]);
    return i3(f) && m3(f) || (o[r2] = f), o;
  }
  return o[r2] = e2[r2], o;
}, p3());
var a2 = D4;
var x3 = (t, e2) => t === e2 || !i3(t) || !i3(e2) ? {} : Object.keys(t).reduce((o, r2) => {
  if (n(e2, r2)) {
    let f = x3(t[r2], e2[r2]);
    return i3(f) && m3(f) || (o[r2] = f), o;
  }
  return o[r2] = void 0, o;
}, p3());
var b2 = x3;
var P3 = (t, e2) => t === e2 ? {} : !i3(t) || !i3(e2) ? e2 : u3(t) || u3(e2) ? t.valueOf() == e2.valueOf() ? {} : e2 : Object.keys(e2).reduce((o, r2) => {
  if (n(t, r2)) {
    let f = P3(t[r2], e2[r2]);
    return d2(f) && !u3(f) && (d2(t[r2]) || !d2(e2[r2])) || (o[r2] = f), o;
  }
  return o;
}, p3());
var j5 = P3;
var E3 = (t, e2) => ({ added: a2(t, e2), deleted: b2(t, e2), updated: j5(t, e2) });
var W3 = E3;

// src/server/mwebview-helper.ts
var mwebview_open = async (url) => {
  return await jsProcess.nativeFetch(`file://mwebview.browser.dweb/open?url=${encodeURIComponent(url)}`).text();
};
var mwebview_activate = async () => {
  return await jsProcess.nativeFetch(`file://mwebview.browser.dweb/activate`).text();
};
var mwebview_destroy = async () => {
  return await jsProcess.nativeFetch(`file://mwebview.browser.dweb/close/app`).boolean();
};
var all_webview_status = new class extends Map {
  last() {
    return [...this.entries()].at(-1);
  }
  /**
   * 对比状态的更新
   * @param diff
   */
  diffFactory(diff) {
    for (const id in diff.added) {
      this.set(id, diff.added[id]);
    }
    for (const id in diff.deleted) {
      this.delete(id);
    }
    for (const id in diff.updated) {
      this.set(id, diff.updated[id]);
    }
  }
}();
var _false = true;
var sync_mwebview_status = async () => {
  if (_false === false) {
    return;
  }
  _false = false;
  const ipc2 = await navigator.dweb.jsProcess.connect("mwebview.browser.dweb");
  let oldWebviewState = [];
  ipc2.onEvent((ipcEvent) => {
    if (ipcEvent.name === "state") {
      const newState = JSON.parse(ipcEvent.text);
      const diff = W3(oldWebviewState, newState);
      oldWebviewState = newState;
      all_webview_status.diffFactory(diff);
    } else if (ipcEvent.name === "diff-state") {
      throw new Error("no implement");
    }
  });
};

// src/server/http-api-server.ts
var DNS_PREFIX = "/dns.std.dweb/";
var Server_api = class extends HttpServer {
  _getOptions() {
    return {
      subdomain: "api",
      port: 443
    };
  }
  async start() {
    const serverIpc = await this._listener;
    return serverIpc.onFetch(this._provider.bind(this)).internalServerError().cors();
  }
  async _provider(event) {
    if (event.pathname.startsWith(DNS_PREFIX)) {
      return this._onDns(event);
    }
    return this._onApi(event);
  }
  async _onDns(event) {
    const url = new URL("file:/" + event.pathname + event.search);
    const pathname = url.pathname;
    const result = async () => {
      if (pathname === "/restart") {
        jsProcess.restart();
        return "restart ok";
      }
      if (pathname === "/close") {
        mwebview_destroy();
        return "window close";
      }
      return "no action for serviceWorker Factory !!!";
    };
    return new Response(await result());
  }
  /**
   * request 事件处理器
   */
  async _onApi(event, connect = (mmid) => jsProcess.connect(mmid), useIpcBody = true) {
    const { pathname, search } = event;
    const path = `file:/${pathname}${search}`;
    const mmid = new URL(path).host;
    const targetIpc = await connect(mmid);
    const ipcProxyRequest = useIpcBody ? new IpcRequest3(
      //
      targetIpc.allocReqId(),
      path,
      event.method,
      event.headers,
      event.ipcRequest.body,
      targetIpc
    ) : IpcRequest3.fromStream(
      targetIpc.allocReqId(),
      path,
      event.method,
      event.headers,
      await event.ipcRequest.body.stream(),
      targetIpc
    );
    targetIpc.postMessage(ipcProxyRequest);
    const ipcProxyResponse = await targetIpc.registerReqId(ipcProxyRequest.req_id).promise;
    return ipcProxyResponse.toResponse();
  }
};

// src/server/http-external-server.ts
var Server_external = class extends HttpServer {
  constructor() {
    super(...arguments);
    /**
     * 这个token是内部使用的，就作为 特殊的 url.pathname 来处理内部操作
     */
    this.token = crypto.randomUUID();
    this.responseMap = /* @__PURE__ */ new Map();
    // 拿到fetch的请求
    this.fetchSignal = createSignal();
    // 等待listen触发
    this.waitListener = new PromiseOut();
  }
  _getOptions() {
    return {
      subdomain: "external",
      port: 443
    };
  }
  async start() {
    console.log("external start");
    const serverIpc = await this._listener;
    return serverIpc.onFetch(this._provider.bind(this)).internalServerError().cors();
  }
  async _provider(event) {
    const { pathname } = event;
    console.log("external provider=>", this.token, event.pathname);
    if (pathname.startsWith(`/${this.token}`)) {
      const action = event.searchParams.get("action");
      console.log("action=>", jsProcess.mmid, action);
      if (action === "listen") {
        const streamPo = new ReadableStreamOut();
        const ob = { controller: streamPo.controller };
        this.fetchSignal.listen((ipcRequest) => {
          const jsonlineEnd = simpleEncoder("\n", "utf8");
          const json = ipcRequest.toJSON();
          const uint8 = simpleEncoder(JSON.stringify(json), "utf8");
          ob.controller.enqueue(u8aConcat([uint8, jsonlineEnd]));
        });
        console.log(`${jsProcess.mmid}fetch\u76D1\u542C\u5DF2\u7ECF\u5EFA\u7ACB`);
        this.waitListener.resolve(true);
        return { body: streamPo.stream };
      }
      if (action === "request") {
        const mmid = event.searchParams.get("mmid");
        let pathname2 = event.searchParams.get("pathname") ?? "";
        event.searchParams.delete("mmid");
        event.searchParams.delete("X-Dweb-Host");
        event.searchParams.delete("action");
        event.searchParams.delete("pathname");
        pathname2 = pathname2 + event.search;
        if (!mmid) {
          throw new FetchError("mmid must be passed", { status: 400 });
        }
        const jsIpc = await jsProcess.connect(mmid);
        const req_id = jsIpc.allocReqId();
        const base = event.ipcRequest.parsed_url.origin;
        const ipcRequest = IpcRequest3.fromRequest(req_id, jsIpc, `${base}${pathname2}`, {
          method: event.method,
          headers: event.headers,
          body: event.body
        });
        const result = jsIpc.registerReqId(req_id);
        jsIpc.postMessage(ipcRequest);
        const response = await result.promise;
        const ipcResponse = new IpcResponse2(
          event.req_id,
          response.statusCode,
          response.headers,
          response.body,
          event.ipc
        );
        cors(ipcResponse.headers);
        return ipcResponse;
      }
      if (action === "response") {
        const externalReqId = +(event.searchParams.get("id") ?? "");
        if (isNaN(externalReqId)) {
          throw new FetchError("reqId is NAN", { status: 400 });
        }
        const responsePOo = this.responseMap.get(externalReqId);
        if (!responsePOo) {
          throw new FetchError(`not found response by req_id ${externalReqId}`, { status: 500 });
        }
        responsePOo.resolve(new IpcResponse2(externalReqId, 200, cors(event.headers), event.ipcRequest.body, event.ipc));
        this.responseMap.delete(externalReqId);
        const icpResponse = IpcResponse2.fromText(event.req_id, 200, event.headers, "ok", event.ipc);
        cors(icpResponse.headers);
        return icpResponse;
      }
      throw new FetchError(`unknown action: ${action}`, { status: 502 });
    }
  }
};

// src/server/http-www-server.ts
var Server_www = class extends HttpServer {
  _getOptions() {
    return {
      subdomain: "www",
      port: 443
    };
  }
  async start() {
    const serverIpc = await this._listener;
    return serverIpc.onFetch(this._provider.bind(this)).noFound();
  }
  async _provider(request, root = "www") {
    let { pathname } = request;
    if (pathname === "/") {
      pathname = "/index.html";
    }
    console.log("www=>", request, pathname);
    const remoteIpcResponse = await jsProcess.nativeRequest(
      `file:///usr/${root}${pathname}?mode=stream`
    );
    const ipcResponse = new IpcResponse2(
      request.req_id,
      remoteIpcResponse.statusCode,
      cors(remoteIpcResponse.headers),
      remoteIpcResponse.body,
      request.ipc
    );
    return ipcResponse;
  }
};

// src/server/polyfill.ts
if (typeof crypto.randomUUID !== "function") {
  crypto.randomUUID = function randomUUID() {
    return "10000000-1000-4000-8000-100000000000".replace(/[018]/g, (_c) => {
      const c3 = +_c;
      return (c3 ^ crypto.getRandomValues(new Uint8Array(1))[0] & 15 >> c3 / 4).toString(16);
    });
  };
}

// src/server/index.ts
var main = async () => {
  const indexUrlPo = new PromiseOut();
  const tryOpenView = async () => {
    const url = await indexUrlPo.promise;
    if (all_webview_status.size === 0) {
      await sync_mwebview_status();
      await mwebview_open(url);
    } else {
      await mwebview_activate();
    }
  };
  tryOpenView();
  jsProcess.onActivity(async (_ipcEvent, ipc2) => {
    tryOpenView();
    ipc2.postMessage(IpcEvent2.fromText("ready", "activity"));
  });
  const wwwServer = new Server_www();
  const externalServer = new Server_external();
  const apiServer = new Server_api();
  void wwwServer.start();
  void externalServer.start();
  void apiServer.start();
  jsProcess.onRequest(async (ipcRequest, ipc2) => {
    const timeOut = setTimeout(() => {
      ipc2.postMessage(IpcEvent2.fromText("Not found", "The target app is not listening for any requests"));
      externalServer.waitListener.reject();
    }, 5e3);
    await externalServer.waitListener.promise;
    clearTimeout(timeOut);
    externalServer.fetchSignal.emit(ipcRequest);
    const awaitResponse = new PromiseOut();
    externalServer.responseMap.set(ipcRequest.req_id, awaitResponse);
    const ipcResponse = await awaitResponse.promise;
    cors(ipcResponse.headers);
    return ipc2.postMessage(ipcResponse);
  });
  {
    const wwwStartResult = await wwwServer.getStartResult();
    const apiStartResult = await apiServer.getStartResult();
    const indexUrl = wwwStartResult.urlInfo.buildInternalUrl((url) => {
      url.pathname = "/index.html";
      url.searchParams.set("X-Plaoc-Internal-Url" /* API_INTERNAL_URL */, apiStartResult.urlInfo.buildInternalUrl().href);
      url.searchParams.set("X-Plaoc-Public-Url" /* API_PUBLIC_URL */, apiStartResult.urlInfo.buildPublicUrl().href);
      url.searchParams.set("X-Plaoc-External-Url" /* EXTERNAL_URL */, externalServer.token);
    });
    console.log("open in browser:", indexUrl.href);
    indexUrlPo.resolve(indexUrl.href);
  }
};
main();
export {
  main
};
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
//! use zod error: Relative import path "zod" not prefixed with / or ./ or ../ only remote
//! https://github.com/denoland/deno/issues/17598
