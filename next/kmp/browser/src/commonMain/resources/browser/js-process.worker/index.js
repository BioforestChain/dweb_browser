var __defProp = Object.defineProperty;
var __export = (target, all) => {
  for (var name in all)
    __defProp(target, name, {
      get: all[name],
      enumerable: true,
      configurable: true,
      set: (newValue) => all[name] = () => newValue
    });
};
var __legacyDecorateClassTS = function(decorators, target, key, desc) {
  var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
  if (typeof Reflect === "object" && typeof Reflect.decorate === "function")
    r = Reflect.decorate(decorators, target, key, desc);
  else
    for (var i = decorators.length - 1;i >= 0; i--)
      if (d = decorators[i])
        r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
  return c > 3 && r && Object.defineProperty(target, key, r), r;
};

// src/helper/binaryHelper.ts
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

// src/helper/httpHelper.ts
var headersToRecord = (headers) => {
  let record = Object.create(null);
  if (headers) {
    let req_headers;
    if (headers instanceof Array) {
      req_headers = new Headers(headers);
    } else if (headers instanceof Headers) {
      req_headers = headers;
    } else {
      record = headers;
    }
    if (req_headers !== undefined) {
      req_headers.forEach((value, key) => {
        record[key] = value;
      });
    }
  }
  return record;
};
var httpMethodCanOwnBody = (method, headers) => {
  if (headers !== undefined && method === "GET") {
    return isWebSocket(method, headers instanceof Headers ? headers : new Headers(headers));
  }
  return method !== "GET" && method !== "HEAD" && method !== "TRACE" && method !== "OPTIONS";
};

// src/core/ipc/helper/PureMethod.ts
var PURE_METHOD;
(function(PURE_METHOD2) {
  PURE_METHOD2["GET"] = "GET";
  PURE_METHOD2["POST"] = "POST";
  PURE_METHOD2["PUT"] = "PUT";
  PURE_METHOD2["DELETE"] = "DELETE";
  PURE_METHOD2["OPTIONS"] = "OPTIONS";
  PURE_METHOD2["TRACE"] = "TRACE";
  PURE_METHOD2["PATCH"] = "PATCH";
  PURE_METHOD2["PURGE"] = "PURGE";
  PURE_METHOD2["HEAD"] = "HEAD";
})(PURE_METHOD || (PURE_METHOD = {}));
var toPureMethod = (method) => {
  if (method == null) {
    return PURE_METHOD.GET;
  }
  switch (method.toUpperCase()) {
    case PURE_METHOD.GET: {
      return PURE_METHOD.GET;
    }
    case PURE_METHOD.POST: {
      return PURE_METHOD.POST;
    }
    case PURE_METHOD.PUT: {
      return PURE_METHOD.PUT;
    }
    case PURE_METHOD.DELETE: {
      return PURE_METHOD.DELETE;
    }
    case PURE_METHOD.OPTIONS: {
      return PURE_METHOD.OPTIONS;
    }
    case PURE_METHOD.TRACE: {
      return PURE_METHOD.TRACE;
    }
    case PURE_METHOD.PATCH: {
      return PURE_METHOD.PATCH;
    }
    case PURE_METHOD.PURGE: {
      return PURE_METHOD.PURGE;
    }
    case PURE_METHOD.HEAD: {
      return PURE_METHOD.HEAD;
    }
  }
  throw new Error(`invalid method: ${method}`);
};

// src/core/helper/ipcRequestHelper.ts
var $normalizeRequestInitAsIpcRequestArgs = async (request_init) => {
  const method = request_init.method ?? "GET";
  const body = httpMethodCanOwnBody(method, request_init.headers) ? await $bodyInitToIpcBodyArgs(request_init.body) : "";
  const headers = headersToRecord(request_init.headers);
  return { method, body, headers };
};
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
    if (bodyInit.size >= 16777216) {
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
      throw new Error(`unsupport body type: ${bodyInit?.constructor.name}`);
    }
  }
  return body;
};
var isWebSocket = (method, headers) => {
  return method === "GET" && headers.get("Upgrade")?.toLowerCase() === "websocket";
};
var buildRequestX = (url, init = {}) => {
  let method = init.method ?? PURE_METHOD.GET;
  const headers = init.headers instanceof Headers ? init.headers : new Headers(init.headers);
  const isWs = isWebSocket(method, headers);
  let body;
  if (isWs) {
    method = PURE_METHOD.POST;
    body = init.body;
  } else if (httpMethodCanOwnBody(method)) {
    body = init.body;
  }
  const request_init = {
    method,
    headers,
    body,
    duplex: body instanceof ReadableStream ? "half" : undefined
  };
  const request = new Request(url, request_init);
  if (isWs) {
    Object.defineProperty(request, "method", {
      configurable: true,
      enumerable: true,
      writable: false,
      value: "GET"
    });
  }
  if (request_init.body instanceof ReadableStream && request.body != request_init.body) {
    Object.defineProperty(request, "body", {
      configurable: true,
      enumerable: true,
      writable: false,
      value: request_init.body
    });
  }
  return request;
};

// src/helper/fetchExtends/$makeFetchBaseExtends.ts
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

// src/helper/stream/JsonlinesStream.ts
class JsonlinesStream extends TransformStream {
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
}

// src/helper/createSignal.ts
var createSignal = (autoStart) => {
  return new Signal(autoStart);
};

class Signal {
  constructor(autoStart = true) {
    if (autoStart) {
      this._start();
    }
  }
  _cbs = new Set;
  _started = false;
  _cachedActions = [];
  _start() {
    if (this._started) {
      return;
    }
    this._started = true;
    if (this._cachedActions.length) {
      for (const action of this._cachedActions) {
        action();
      }
      this._cachedActions.length = 0;
    }
  }
  _startAction(action) {
    if (this._started) {
      action();
    } else {
      this._cachedActions.push(action);
    }
  }
  listen = (cb) => {
    this._cbs.add(cb);
    this._start();
    return () => this._cbs.delete(cb);
  };
  emit = (...args) => {
    this._startAction(() => {
      this._emit(args, this._cbs);
    });
  };
  emitAndClear = (...args) => {
    this._startAction(() => {
      const cbs = [...this._cbs];
      this._cbs.clear();
      this._emit(args, cbs);
    });
  };
  _emit(args, cbs) {
    for (const cb of cbs) {
      try {
        cb.apply(null, args);
      } catch (reason) {
        console.warn(reason);
      }
    }
  }
  clear = () => {
    this._startAction(() => {
      this._cbs.clear();
    });
  };
}

// src/helper/stream/readableStreamHelper.ts
async function* _doRead(reader, options) {
  const signal = options?.signal;
  if (signal !== undefined) {
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
      console.log(new TextDecoder().decode(cache));
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

class ReadableStreamOut {
  strategy;
  constructor(strategy) {
    this.strategy = strategy;
    this.stream = new ReadableStream({
      cancel: (reason) => {
        this._on_cancel_signal?.emit(reason);
      },
      start: (controller) => {
        this.controller = controller;
      },
      pull: () => {
        this._on_pull_signal?.emit();
      }
    }, this.strategy);
  }
  controller;
  stream;
  _on_cancel_signal;
  get onCancel() {
    return (this._on_cancel_signal ??= createSignal()).listen;
  }
  _on_pull_signal;
  get onPull() {
    return (this._on_pull_signal ??= createSignal()).listen;
  }
}

// src/helper/stream/jsonlinesStreamHelper.ts
var binaryToJsonlinesStream = (stream) => {
  return textToJsonlinesStream(stream.pipeThrough(new TextDecoderStream));
};
var textToJsonlinesStream = (stream) => {
  return stream.pipeThrough(new JsonlinesStream);
};

// src/helper/fetchExtends/$makeFetchStreamExtends.ts
var $makeFetchExtends2 = (exts) => {
  return exts;
};
var fetchStreamExtends = $makeFetchExtends2({
  async jsonlines() {
    return binaryToJsonlinesStream(await this.stream());
  },
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

// src/helper/fetchExtends/index.ts
var fetchExtends = {
  ...fetchBaseExtends,
  ...fetchStreamExtends
};

// src/helper/urlHelper.ts
var getBaseUrl = () => URL_BASE ??= ("document" in globalThis) ? document.baseURI : ("location" in globalThis) && (location.protocol === "http:" || location.protocol === "https:" || location.protocol === "file:" || location.protocol === "chrome-extension:") ? location.href : "file:///";
var URL_BASE;
var parseUrl = (url, base = getBaseUrl()) => {
  return new URL(url, base);
};
var updateUrlOrigin = (url, new_origin) => {
  const { origin, href } = parseUrl(url);
  return new URL(new_origin + href.slice(origin.length));
};
var appendUrlSearchs = (url, extQuerys) => {
  for (const [key, value] of extQuerys) {
    if (url.searchParams.has(key) === false) {
      url.searchParams.set(key, value);
    }
  }
  return url;
};

// src/helper/normalizeFetchArgs.ts
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
  if (_parsed_url === undefined) {
    throw new Error(`no found url for fetch`);
  }
  const parsed_url = _parsed_url;
  const request_init = _request_init ?? {};
  return {
    parsed_url,
    request_init
  };
};

// src/helper/PromiseOut.ts
var isPromiseLike = (value) => {
  return value instanceof Object && typeof value.then === "function";
};
class PromiseOut {
  static resolve(v) {
    const po = new PromiseOut;
    po.resolve(v);
    return po;
  }
  static reject(reason) {
    const po = new PromiseOut;
    po.reject(reason);
    return po;
  }
  static sleep(ms) {
    const po = new PromiseOut;
    let ti = setTimeout(() => {
      ti = undefined;
      po.resolve();
    }, ms);
    po.onFinished(() => ti !== undefined && clearTimeout(ti));
    return po;
  }
  promise;
  is_resolved = false;
  is_rejected = false;
  is_finished = false;
  value;
  reason;
  resolve;
  reject;
  _innerFinally;
  _innerFinallyArg;
  _innerThen;
  _innerCatch;
  constructor() {
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
      this._innerFinally = undefined;
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
}

// src/js-process/worker/std-dweb-core.ts
var exports_std_dweb_core = {};
__export(exports_std_dweb_core, {
  setStreamId: () => {
    {
      return setStreamId;
    }
  },
  pureFrameToIpcEvent: () => {
    {
      return pureFrameToIpcEvent;
    }
  },
  pureChannelToIpcEvent: () => {
    {
      return pureChannelToIpcEvent;
    }
  },
  nativeFetchAdaptersManager: () => {
    {
      return nativeFetchAdaptersManager;
    }
  },
  jsIpcPool: () => {
    {
      return jsIpcPool;
    }
  },
  ipcStreamPulling: () => {
    {
      return ipcStreamPulling;
    }
  },
  ipcStreamPaused: () => {
    {
      return ipcStreamPaused;
    }
  },
  ipcStreamEnd: () => {
    {
      return ipcStreamEnd;
    }
  },
  ipcStreamData: () => {
    {
      return ipcStreamData;
    }
  },
  ipcResponse: () => {
    {
      return ipcResponse;
    }
  },
  ipcRequest: () => {
    {
      return ipcRequest;
    }
  },
  ipcLifecycleOpening: () => {
    {
      return ipcLifecycleOpening;
    }
  },
  ipcLifecycleOpened: () => {
    {
      return ipcLifecycleOpened;
    }
  },
  ipcLifecycleInit: () => {
    {
      return ipcLifecycleInit;
    }
  },
  ipcLifecycleClosing: () => {
    {
      return ipcLifecycleClosing;
    }
  },
  ipcLifecycleClosed: () => {
    {
      return ipcLifecycleClosed;
    }
  },
  ipcLifecycle: () => {
    {
      return ipcLifecycle;
    }
  },
  ipcEventToPureFrame: () => {
    {
      return ipcEventToPureFrame;
    }
  },
  ipcError: () => {
    {
      return ipcError;
    }
  },
  fetchWs: () => {
    {
      return fetchWs;
    }
  },
  fetchMid: () => {
    {
      return fetchMid;
    }
  },
  fetchHanlderFactory: () => {
    {
      return fetchHanlderFactory;
    }
  },
  fetchEnd: () => {
    {
      return fetchEnd;
    }
  },
  endpointLifecycleStateBase: () => {
    {
      return endpointLifecycleStateBase;
    }
  },
  endpointLifecycleOpening: () => {
    {
      return endpointLifecycleOpening;
    }
  },
  endpointLifecycleOpend: () => {
    {
      return endpointLifecycleOpend;
    }
  },
  endpointLifecycleInit: () => {
    {
      return endpointLifecycleInit;
    }
  },
  endpointLifecycleClosing: () => {
    {
      return endpointLifecycleClosing;
    }
  },
  endpointLifecycleClosed: () => {
    {
      return endpointLifecycleClosed;
    }
  },
  endpointLifecycle: () => {
    {
      return endpointLifecycle;
    }
  },
  endpointIpcMessage: () => {
    {
      return endpointIpcMessage;
    }
  },
  createFetchHandler: () => {
    {
      return createFetchHandler;
    }
  },
  cors: () => {
    {
      return cors;
    }
  },
  X_IPC_UPGRADE_KEY: () => {
    {
      return X_IPC_UPGRADE_KEY;
    }
  },
  WebMessageEndpoint: () => {
    {
      return WebMessageEndpoint;
    }
  },
  ReadableStreamOut: () => {
    {
      return ReadableStreamOut;
    }
  },
  PureTextFrame: () => {
    {
      return PureTextFrame;
    }
  },
  PureFrameType: () => {
    {
      return PureFrameType;
    }
  },
  PureFrame: () => {
    {
      return PureFrame;
    }
  },
  PureChannel: () => {
    {
      return PureChannel;
    }
  },
  PureBinaryFrame: () => {
    {
      return PureBinaryFrame;
    }
  },
  PURE_CHANNEL_EVENT_PREFIX: () => {
    {
      return PURE_CHANNEL_EVENT_PREFIX;
    }
  },
  IpcServerRequest: () => {
    {
      return IpcServerRequest;
    }
  },
  IpcResponse: () => {
    {
      return IpcResponse;
    }
  },
  IpcPool: () => {
    {
      return IpcPool;
    }
  },
  IpcHeaders: () => {
    {
      return IpcHeaders;
    }
  },
  IpcFetchEvent: () => {
    {
      return IpcFetchEvent;
    }
  },
  IpcEvent: () => {
    {
      return IpcEvent;
    }
  },
  IpcEndpoint: () => {
    {
      return IpcEndpoint;
    }
  },
  IpcClientRequest: () => {
    {
      return IpcClientRequest;
    }
  },
  IpcBodySender: () => {
    {
      return IpcBodySender;
    }
  },
  IpcBodyReceiver: () => {
    {
      return IpcBodyReceiver;
    }
  },
  IpcBody: () => {
    {
      return IpcBody;
    }
  },
  Ipc: () => {
    {
      return Ipc;
    }
  },
  IPC_HANDLE_EVENT: () => {
    {
      return IPC_HANDLE_EVENT;
    }
  },
  IPC_DATA_ENCODING: () => {
    {
      return IPC_DATA_ENCODING;
    }
  },
  FetchEvent: () => {
    {
      return IpcFetchEvent;
    }
  },
  FetchError: () => {
    {
      return FetchError;
    }
  },
  FETCH_WS_SYMBOL: () => {
    {
      return FETCH_WS_SYMBOL;
    }
  },
  FETCH_MID_SYMBOL: () => {
    {
      return FETCH_MID_SYMBOL;
    }
  },
  FETCH_END_SYMBOL: () => {
    {
      return FETCH_END_SYMBOL;
    }
  },
  ENDPOINT_PROTOCOL: () => {
    {
      return ENDPOINT_PROTOCOL;
    }
  },
  ENDPOINT_LIFECYCLE_STATE: () => {
    {
      return ENDPOINT_LIFECYCLE_STATE;
    }
  },
  BodyHub: () => {
    {
      return BodyHub;
    }
  },
  $serializableEndpointMessage: () => {
    {
      return $serializableEndpointMessage;
    }
  },
  $normalizeIpcMessage: () => {
    {
      return $normalizeIpcMessage;
    }
  },
  $jsonToEndpointMessage: () => {
    {
      return $jsonToEndpointMessage;
    }
  },
  $endpointMessageToJson: () => {
    {
      return $endpointMessageToJson;
    }
  },
  $endpointMessageToCbor: () => {
    {
      return $endpointMessageToCbor;
    }
  },
  $cborToEndpointMessage: () => {
    {
      return $cborToEndpointMessage;
    }
  }
});

// src/helper/logger.ts
var tabify = (str, tabSize = 4) => str.padEnd(Math.ceil(str.length / tabSize) * tabSize, " ");
var logger = (scope) => {
  const prefix = tabify(String(scope)) + "|";
  const logger2 = {
    isEnable: true,
    debug: (tag, ...args) => {
      if (logger2.isEnable)
        console.debug(prefix, tabify(tag) + "|", ...customInspects(args));
    },
    error: (tag, ...args) => {
      if (logger2.isEnable)
        console.error(prefix, tabify(tag) + "|", ...customInspects(args));
    },
    warn: (...args) => {
      return console.warn(prefix, ...customInspects(args));
    },
    debugLazy: (tag, lazy) => {
      if (logger2.isEnable) {
        let args = lazy();
        if (Symbol.iterator in args) {
          console.debug(prefix, tabify(tag) + "|", ...customInspects(args));
        } else {
          console.debug(prefix, tabify(tag) + "|", customInspect(args));
        }
      }
    },
    errorLazy: (tag, lazy) => {
      if (logger2.isEnable) {
        let args = lazy();
        if (Symbol.iterator in args) {
          console.error(prefix, tabify(tag) + "|", ...customInspects(args));
        } else {
          console.error(prefix, tabify(tag) + "|", customInspect(args));
        }
      }
    }
  };
  return logger2;
};
var customInspect = (arg) => typeof arg === "object" && arg !== null && (CUSTOM_INSPECT in arg) ? arg[CUSTOM_INSPECT]() : arg;
var customInspects = (args) => args.map(customInspect);
var CUSTOM_INSPECT = Symbol.for("inspect.custom");

// src/helper/cacheGetter.ts
class CacheGetter {
  getter;
  constructor(getter) {
    this.getter = getter;
  }
  _first = true;
  _value;
  get value() {
    if (this._first) {
      this._first = false;
      this._value = this.getter();
    }
    return this._value;
  }
  reset() {
    this._first = true;
    this._value = undefined;
  }
}

// src/core/ipc/helper/IpcHeaders.ts
class IpcHeaders extends Headers {
  constructor() {
    super(...arguments);
  }
  init(key, value) {
    if (this.has(key) === false) {
      this.set(key, value);
    }
    return this;
  }
  toJSON() {
    const record = {};
    this.forEach((value, key) => {
      record[key.replace(/\w+/g, (w) => w[0].toUpperCase() + w.slice(1))] = value;
    });
    return record;
  }
}
var cors = (headers) => {
  headers.init("Access-Control-Allow-Origin", "*");
  headers.init("Access-Control-Allow-Headers", "*");
  headers.init("Access-Control-Allow-Methods", "*");
  return headers;
};

// src/helper/encoding.ts
var textEncoder = new TextEncoder;
var simpleEncoder = (data, encoding) => {
  if (encoding === "base64") {
    const byteCharacters = atob(data);
    const binary = new Uint8Array(byteCharacters.length);
    for (let i = 0;i < byteCharacters.length; i++) {
      binary[i] = byteCharacters.charCodeAt(i);
    }
    return binary;
  } else if (encoding === "hex") {
    const binary = new Uint8Array(data.length / 2);
    for (let i = 0;i < binary.length; i++) {
      const start = i + i;
      binary[i] = parseInt(data.slice(start, start + 2), 16);
    }
    return binary;
  }
  return textEncoder.encode(data);
};
var textDecoder = new TextDecoder;
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

// src/core/ipc/ipc-message/internal/IpcData.ts
var IPC_DATA_ENCODING;
(function(IPC_DATA_ENCODING2) {
  IPC_DATA_ENCODING2[IPC_DATA_ENCODING2["UTF8"] = 1 << 1] = "UTF8";
  IPC_DATA_ENCODING2[IPC_DATA_ENCODING2["BASE64"] = 1 << 2] = "BASE64";
  IPC_DATA_ENCODING2[IPC_DATA_ENCODING2["BINARY"] = 1 << 3] = "BINARY";
})(IPC_DATA_ENCODING || (IPC_DATA_ENCODING = {}));
var $dataToBinary = (data, encoding2) => {
  switch (encoding2) {
    case IPC_DATA_ENCODING.BINARY: {
      return data;
    }
    case IPC_DATA_ENCODING.BASE64: {
      return simpleEncoder(data, "base64");
    }
    case IPC_DATA_ENCODING.UTF8: {
      return simpleEncoder(data, "utf8");
    }
  }
  throw new Error(`unknown encoding: ${encoding2}`);
};
var $dataToText = (data, encoding2) => {
  switch (encoding2) {
    case IPC_DATA_ENCODING.BINARY: {
      return simpleDecoder(data, "utf8");
    }
    case IPC_DATA_ENCODING.BASE64: {
      return simpleDecoder(simpleEncoder(data, "base64"), "utf8");
    }
    case IPC_DATA_ENCODING.UTF8: {
      return data;
    }
  }
  throw new Error(`unknown encoding: ${encoding2}`);
};

// src/core/ipc/ipc-message/internal/IpcMessage.ts
var IPC_MESSAGE_TYPE;
(function(IPC_MESSAGE_TYPE2) {
  IPC_MESSAGE_TYPE2["LIFECYCLE"] = "life";
  IPC_MESSAGE_TYPE2["REQUEST"] = "req";
  IPC_MESSAGE_TYPE2["RESPONSE"] = "res";
  IPC_MESSAGE_TYPE2["STREAM_DATA"] = "data";
  IPC_MESSAGE_TYPE2["STREAM_PULLING"] = "pull";
  IPC_MESSAGE_TYPE2["STREAM_PAUSED"] = "pause";
  IPC_MESSAGE_TYPE2["STREAM_END"] = "end";
  IPC_MESSAGE_TYPE2["STREAM_ABORT"] = "abo";
  IPC_MESSAGE_TYPE2["EVENT"] = "event";
  IPC_MESSAGE_TYPE2["ERROR"] = "err";
  IPC_MESSAGE_TYPE2["FORK"] = "fork";
})(IPC_MESSAGE_TYPE || (IPC_MESSAGE_TYPE = {}));
var ipcMessageBase = (type) => ({ type });

// src/core/ipc/ipc-message/IpcEvent.ts
var ipcEvent = (name, data, encoding3, orderBy) => ({
  ...ipcMessageBase(IPC_MESSAGE_TYPE.EVENT),
  name,
  data,
  encoding: encoding3,
  orderBy
});
var IpcEvent = Object.assign(ipcEvent, {
  fromBase64(name, data, orderBy) {
    return ipcEvent(name, simpleDecoder(data, "base64"), IPC_DATA_ENCODING.BASE64, orderBy);
  },
  fromBinary(name, data, orderBy) {
    return ipcEvent(name, data, IPC_DATA_ENCODING.BINARY, orderBy);
  },
  fromUtf8(name, data, orderBy) {
    return ipcEvent(name, simpleDecoder(data, "utf8"), IPC_DATA_ENCODING.UTF8, orderBy);
  },
  fromText(name, data, orderBy) {
    return ipcEvent(name, data, IPC_DATA_ENCODING.UTF8, orderBy);
  },
  binary(event) {
    return $dataToBinary(event.data, event.encoding);
  },
  text(event) {
    return $dataToText(event.data, event.encoding);
  }
});

// src/core/ipc/helper/PureChannel.ts
class PureChannel {
  income;
  outgoing;
  constructor(income = new ReadableStreamOut, outgoing = new ReadableStreamOut) {
    this.income = income;
    this.outgoing = outgoing;
  }
  _startLock = new PromiseOut;
  afterStart() {
    return this._startLock.promise;
  }
  start() {
    this._startLock.resolve();
    return {
      incomeController: this.income.controller,
      outgoingStream: this.outgoing.stream
    };
  }
  close() {
    this.income.controller.close();
    this.outgoing.controller.close();
  }
  _reverse;
  reverse() {
    if (this._reverse === undefined) {
      this._reverse = new PureChannel(this.outgoing, this.income);
      this._reverse._reverse = this;
    }
    return this._reverse;
  }
}
var PureFrameType;
(function(PureFrameType2) {
  PureFrameType2[PureFrameType2["Text"] = 0] = "Text";
  PureFrameType2[PureFrameType2["Binary"] = 1] = "Binary";
})(PureFrameType || (PureFrameType = {}));

class PureFrame {
  type;
  constructor(type) {
    this.type = type;
  }
}

class PureTextFrame extends PureFrame {
  data;
  constructor(data) {
    super(PureFrameType.Text);
    this.data = data;
  }
}

class PureBinaryFrame extends PureFrame {
  data;
  constructor(data) {
    super(PureFrameType.Binary);
    this.data = data;
  }
}
var ipcEventToPureFrame = (event) => {
  switch (event.encoding) {
    case IPC_DATA_ENCODING.UTF8:
      return new PureTextFrame(event.data);
    case IPC_DATA_ENCODING.BINARY:
    case IPC_DATA_ENCODING.BASE64:
      return new PureBinaryFrame(IpcEvent.binary(event));
  }
};
var pureFrameToIpcEvent = (eventName, pureFrame, orderBy) => {
  if (pureFrame.type === PureFrameType.Text) {
    return IpcEvent.fromText(eventName, pureFrame.data, orderBy);
  }
  return IpcEvent.fromBinary(eventName, pureFrame.data, orderBy);
};
var pureChannelToIpcEvent = async (channelIpc, pureChannel, debugTag) => {
  const eventData = `${PURE_CHANNEL_EVENT_PREFIX}/data`;
  const orderBy = -1;
  const ipcListenToChannelPo = new PromiseOut;
  const off = channelIpc.onEvent("pureChannelToIpcEvent").collect(async (event) => {
    const ipcEvent2 = event.consumeMapNotNull((ipcEvent3) => {
      if (ipcEvent3.name === eventData) {
        return ipcEvent3;
      }
    });
    if (ipcEvent2 === undefined) {
      return;
    }
    (await ipcListenToChannelPo.promise).enqueue(ipcEventToPureFrame(ipcEvent2));
  });
  const ctx = pureChannel.start();
  ipcListenToChannelPo.resolve(ctx.incomeController);
  const channelReadOut = ctx.outgoingStream;
  await channelIpc.start(undefined, debugTag);
  for await (const pureFrame of streamRead(channelReadOut)) {
    channelIpc.postMessage(pureFrameToIpcEvent(eventData, pureFrame, orderBy));
  }
  off();
};

// src/core/helper/crypto.shims.ts
//!此处为js ipc特有垫片，防止有些webview版本过低，出现无法支持的函数
if (typeof crypto.randomUUID !== "function") {
  crypto.randomUUID = function randomUUID() {
    return "10000000-1000-4000-8000-100000000000".replace(/[018]/g, (_c) => {
      const c = +_c;
      return (c ^ crypto.getRandomValues(new Uint8Array(1))[0] & 15 >> c / 4).toString(16);
    });
  };
}

// src/core/ipc/ipc-message/stream/IpcBody.ts
class IpcBody {
  static CACHE = new class {
    raw_ipcBody_WMap = new WeakMap;
    streamId_receiverIpc_Map = new Map;
    streamId_ipcBodySender_Map = new Map;
  };
  get raw() {
    return this._bodyHub.data;
  }
  async u8a() {
    const bodyHub = this._bodyHub;
    let body_u8a = bodyHub.u8a;
    if (body_u8a === undefined) {
      if (bodyHub.stream) {
        body_u8a = await streamReadAllBuffer(bodyHub.stream);
      } else if (bodyHub.text !== undefined) {
        body_u8a = simpleEncoder(bodyHub.text, "utf8");
      } else {
        throw new Error(`invalid body type`);
      }
      bodyHub.u8a = body_u8a;
      IpcBody.CACHE.raw_ipcBody_WMap.set(body_u8a, this);
    }
    return body_u8a;
  }
  async stream() {
    const bodyHub = this._bodyHub;
    let body_stream = bodyHub.stream;
    if (body_stream === undefined) {
      body_stream = new Blob([await this.u8a()]).stream();
      bodyHub.stream = body_stream;
      IpcBody.CACHE.raw_ipcBody_WMap.set(body_stream, this);
    }
    return body_stream;
  }
  async text() {
    const bodyHub = this._bodyHub;
    let body_text = bodyHub.text;
    if (body_text === undefined) {
      body_text = simpleDecoder(await this.u8a(), "utf8");
      bodyHub.text = body_text;
    }
    return body_text;
  }
}

class BodyHub {
  data;
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
  u8a;
  stream;
  text;
}

// src/core/ipc/ipc-message/stream/IpcStreamData.ts
var _ipcStreamData = (stream_id, data, encoding5) => ({ ...ipcMessageBase(IPC_MESSAGE_TYPE.STREAM_DATA), stream_id, data, encoding: encoding5 });
var ipcStreamData = Object.assign(_ipcStreamData, {
  fromBase64(stream_id, data) {
    return ipcStreamData(stream_id, simpleDecoder(data, "base64"), IPC_DATA_ENCODING.BASE64);
  },
  fromBinary(stream_id, data) {
    return ipcStreamData(stream_id, data, IPC_DATA_ENCODING.BINARY);
  },
  fromUtf8(stream_id, data) {
    return ipcStreamData(stream_id, simpleDecoder(data, "utf8"), IPC_DATA_ENCODING.UTF8);
  },
  binary(streamData) {
    return $dataToBinary(streamData.data, streamData.encoding);
  },
  text(streamData) {
    return $dataToText(streamData.data, streamData.encoding);
  }
});

// src/core/ipc/ipc-message/stream/IpcStreamEnd.ts
var ipcStreamEnd = (stream_id) => ({ ...ipcMessageBase(IPC_MESSAGE_TYPE.STREAM_END), stream_id });

// src/core/ipc/ipc-message/stream/MetaBody.ts
class MetaBody {
  type;
  senderUid;
  data;
  streamId;
  receiverUid;
  constructor(type, senderUid, data, streamId = simpleDecoder(crypto.getRandomValues(new Uint8Array(8)), "base64"), receiverUid) {
    this.type = type;
    this.senderUid = senderUid;
    this.data = data;
    this.streamId = streamId;
    this.receiverUid = receiverUid;
  }
  static fromJSON(metaBody) {
    if (metaBody instanceof MetaBody === false) {
      metaBody = new MetaBody(metaBody.type, metaBody.senderUid, metaBody.data, metaBody.streamId, metaBody.receiverUid);
    }
    return metaBody;
  }
  static fromText(senderUid, data, streamId, receiverUid) {
    return new MetaBody(streamId == null ? IPC_META_BODY_TYPE.INLINE_TEXT : IPC_META_BODY_TYPE.STREAM_WITH_TEXT, senderUid, data, streamId, receiverUid);
  }
  static fromBase64(senderUid, data, streamId, receiverUid) {
    return new MetaBody(streamId == null ? IPC_META_BODY_TYPE.INLINE_BASE64 : IPC_META_BODY_TYPE.STREAM_WITH_BASE64, senderUid, data, streamId, receiverUid);
  }
  static fromBinary(sender, data, streamId, receiverUid) {
    return new MetaBody(streamId == null ? IPC_META_BODY_TYPE.INLINE_BINARY : IPC_META_BODY_TYPE.STREAM_WITH_BINARY, sender, data, streamId, receiverUid);
  }
  #type_encoding = new CacheGetter(() => {
    const encoding6 = this.type & 254;
    switch (encoding6) {
      case IPC_DATA_ENCODING.UTF8:
        return IPC_DATA_ENCODING.UTF8;
      case IPC_DATA_ENCODING.BASE64:
        return IPC_DATA_ENCODING.BASE64;
      case IPC_DATA_ENCODING.BINARY:
        return IPC_DATA_ENCODING.BINARY;
      default:
        return IPC_DATA_ENCODING.UTF8;
    }
  });
  get type_encoding() {
    return this.#type_encoding.value;
  }
  #type_isInline = new CacheGetter(() => (this.type & IPC_META_BODY_TYPE.INLINE) !== 0);
  get type_isInline() {
    return this.#type_isInline.value;
  }
  #type_isStream = new CacheGetter(() => (this.type & IPC_META_BODY_TYPE.INLINE) === 0);
  get type_isStream() {
    return this.#type_isStream.value;
  }
  #jsonAble = new CacheGetter(() => {
    if (this.type_encoding === IPC_DATA_ENCODING.BINARY) {
      return MetaBody.fromBase64(this.senderUid, simpleDecoder(this.data, "base64"), this.streamId, this.receiverUid);
    }
    return this;
  });
  get jsonAble() {
    return this.#jsonAble.value;
  }
  toJSON() {
    return { ...this.jsonAble };
  }
}
var IPC_META_BODY_TYPE;
(function(IPC_META_BODY_TYPE2) {
  IPC_META_BODY_TYPE2[IPC_META_BODY_TYPE2["STREAM_ID"] = 0] = "STREAM_ID";
  IPC_META_BODY_TYPE2[IPC_META_BODY_TYPE2["INLINE"] = 1] = "INLINE";
  IPC_META_BODY_TYPE2[IPC_META_BODY_TYPE2["STREAM_WITH_TEXT"] = IPC_META_BODY_TYPE2.STREAM_ID | IPC_DATA_ENCODING.UTF8] = "STREAM_WITH_TEXT";
  IPC_META_BODY_TYPE2[IPC_META_BODY_TYPE2["STREAM_WITH_BASE64"] = IPC_META_BODY_TYPE2.STREAM_ID | IPC_DATA_ENCODING.BASE64] = "STREAM_WITH_BASE64";
  IPC_META_BODY_TYPE2[IPC_META_BODY_TYPE2["STREAM_WITH_BINARY"] = IPC_META_BODY_TYPE2.STREAM_ID | IPC_DATA_ENCODING.BINARY] = "STREAM_WITH_BINARY";
  IPC_META_BODY_TYPE2[IPC_META_BODY_TYPE2["INLINE_TEXT"] = IPC_META_BODY_TYPE2.INLINE | IPC_DATA_ENCODING.UTF8] = "INLINE_TEXT";
  IPC_META_BODY_TYPE2[IPC_META_BODY_TYPE2["INLINE_BASE64"] = IPC_META_BODY_TYPE2.INLINE | IPC_DATA_ENCODING.BASE64] = "INLINE_BASE64";
  IPC_META_BODY_TYPE2[IPC_META_BODY_TYPE2["INLINE_BINARY"] = IPC_META_BODY_TYPE2.INLINE | IPC_DATA_ENCODING.BINARY] = "INLINE_BINARY";
})(IPC_META_BODY_TYPE || (IPC_META_BODY_TYPE = {}));

// src/core/ipc/ipc-message/stream/IpcBodySender.ts
var STREAM_CTOR_SIGNAL;
(function(STREAM_CTOR_SIGNAL2) {
  STREAM_CTOR_SIGNAL2[STREAM_CTOR_SIGNAL2["PULLING"] = 0] = "PULLING";
  STREAM_CTOR_SIGNAL2[STREAM_CTOR_SIGNAL2["PAUSED"] = 1] = "PAUSED";
  STREAM_CTOR_SIGNAL2[STREAM_CTOR_SIGNAL2["ABORTED"] = 2] = "ABORTED";
})(STREAM_CTOR_SIGNAL || (STREAM_CTOR_SIGNAL = {}));

class IpcBodySender extends IpcBody {
  data;
  ipc;
  static fromAny(data, ipc) {
    if (typeof data !== "string") {
      const cache = IpcBodySender.CACHE.raw_ipcBody_WMap.get(data);
      if (cache !== undefined) {
        return cache;
      }
    }
    return new IpcBodySender(data, ipc);
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
  constructor(data, ipc) {
    super();
    this.data = data;
    this.ipc = ipc;
    this._bodyHub = new BodyHub(data);
    this.metaBody = this.$bodyAsMeta(data, ipc);
    this.isStream = data instanceof ReadableStream;
    if (typeof data !== "string") {
      IpcBodySender.CACHE.raw_ipcBody_WMap.set(data, this);
    }
    IpcBodySender.$usableByIpc(ipc, this);
  }
  isStream;
  streamCtorSignal = createSignal();
  usedIpcMap = new Map;
  UsedIpcInfo = class UsedIpcInfo {
    ipcBody;
    ipc;
    bandwidth;
    fuse;
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
  useByIpc(ipc) {
    const info = this.usedIpcMap.get(ipc);
    if (info !== undefined) {
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
  emitStreamPull(info, message) {
    info.bandwidth = message.bandwidth;
    this.streamCtorSignal.emit(STREAM_CTOR_SIGNAL.PULLING);
  }
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
      this.streamCtorSignal.emit(STREAM_CTOR_SIGNAL.PAUSED);
    }
  }
  emitStreamAborted(info) {
    if (this.usedIpcMap.delete(info.ipc) != null) {
      if (this.usedIpcMap.size === 0) {
        this.streamCtorSignal.emit(STREAM_CTOR_SIGNAL.ABORTED);
      }
    }
  }
  closeSignal = createSignal();
  onStreamClose(cb) {
    return this.closeSignal.listen(cb);
  }
  openSignal = createSignal();
  onStreamOpen(cb) {
    return this.openSignal.listen(cb);
  }
  _isStreamOpened = false;
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
  _isStreamClosed = false;
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
  _bodyHub;
  metaBody;
  $bodyAsMeta(body, ipc) {
    if (typeof body === "string") {
      return MetaBody.fromText(ipc.pool.poolId, body);
    }
    if (body instanceof ReadableStream) {
      return this.$streamAsMeta(body, ipc);
    }
    return MetaBody.fromBinary(ipc.pool.poolId, body);
  }
  $streamAsMeta(stream, ipc) {
    const stream_id = getStreamId(stream);
    let _reader;
    const getReader = () => _reader ??= binaryStreamRead(stream);
    (async () => {
      let pullingLock = new PromiseOut;
      this.streamCtorSignal.listen(async (signal) => {
        switch (signal) {
          case STREAM_CTOR_SIGNAL.PULLING: {
            pullingLock.resolve();
            break;
          }
          case STREAM_CTOR_SIGNAL.PAUSED: {
            if (pullingLock.is_finished) {
              pullingLock = new PromiseOut;
            }
            break;
          }
          case STREAM_CTOR_SIGNAL.ABORTED: {
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
          const message = ipcStreamData.fromBinary(stream_id, await reader.readBinary(availableLen));
          for (const ipc2 of this.usedIpcMap.keys()) {
            ipc2.postMessage(message);
          }
        } else if (availableLen === -1) {
          const message = ipcStreamEnd(stream_id);
          for (const ipc2 of this.usedIpcMap.keys()) {
            ipc2.postMessage(message);
          }
          await stream.cancel();
          this.emitStreamClose();
          break;
        }
      }
    })().catch(console.error);
    const streamType = IPC_META_BODY_TYPE.STREAM_ID;
    const streamFirstData = "";
    if ("preReadableSize" in stream && typeof stream.preReadableSize === "number" && stream.preReadableSize > 0) {
    }
    const metaBody = new MetaBody(streamType, ipc.pool.poolId, streamFirstData, stream_id);
    IpcBodySender.CACHE.streamId_ipcBodySender_Map.set(metaBody.streamId, this);
    this.streamCtorSignal.listen((signal) => {
      if (signal == STREAM_CTOR_SIGNAL.ABORTED) {
        IpcBodySender.CACHE.streamId_ipcBodySender_Map.delete(metaBody.streamId);
      }
    });
    return metaBody;
  }
  static $usableByIpc = (ipc, ipcBody) => {
    if (ipcBody.isStream && !ipcBody._isStreamOpened) {
      const streamId = ipcBody.metaBody.streamId;
      let usableIpcBodyMapper = IpcUsableIpcBodyMap.get(ipc);
      if (usableIpcBodyMapper === undefined) {
        const mapper = new UsableIpcBodyMapper;
        IpcUsableIpcBodyMap.set(ipc, mapper);
        mapper.onDestroy(ipc.onStream("usableByIpc").collect((event) => {
          const message = event.data;
          switch (message.type) {
            case IPC_MESSAGE_TYPE.STREAM_PULLING:
              mapper.get(message.stream_id)?.useByIpc(ipc)?.emitStreamPull(message);
              break;
            case IPC_MESSAGE_TYPE.STREAM_PAUSED:
              mapper.get(message.stream_id)?.useByIpc(ipc)?.emitStreamPaused(message);
              break;
            case IPC_MESSAGE_TYPE.STREAM_ABORT:
              mapper.get(message.stream_id)?.useByIpc(ipc)?.emitStreamAborted();
              break;
            default:
              return;
          }
          event.consume();
        }));
        mapper.onDestroy(() => IpcUsableIpcBodyMap.delete(ipc));
        usableIpcBodyMapper = mapper;
      }
      if (usableIpcBodyMapper.add(streamId, ipcBody)) {
        ipcBody.onStreamClose(() => usableIpcBodyMapper.remove(streamId));
      }
    }
  };
}
var streamIdWM = new WeakMap;
var streamRealmId = crypto.randomUUID();
var stream_id_acc = 0;
var getStreamId = (stream) => {
  let id = streamIdWM.get(stream);
  if (id === undefined) {
    id = `${streamRealmId}-${stream_id_acc++}`;
    streamIdWM.set(stream, id);
  }
  return id;
};
var setStreamId = (stream, cid) => {
  let id = streamIdWM.get(stream);
  if (id === undefined) {
    streamIdWM.set(stream, id = `${streamRealmId}-${stream_id_acc++}[${cid}]`);
  }
  return id;
};

class UsableIpcBodyMapper {
  map = new Map;
  add(streamId, ipcBody) {
    if (this.map.has(streamId)) {
      return false;
    }
    this.map.set(streamId, ipcBody);
    return true;
  }
  get(streamId) {
    return this.map.get(streamId);
  }
  remove(streamId) {
    const ipcBody = this.map.get(streamId);
    if (ipcBody !== undefined) {
      this.map.delete(streamId);
      if (this.map.size === 0) {
        this.destroySignal.emitAndClear();
      }
    }
  }
  destroySignal = createSignal();
  onDestroy(cb) {
    this.destroySignal.listen(cb);
  }
}
var IpcUsableIpcBodyMap = new WeakMap;

// src/core/ipc/ipc-message/IpcRequest.ts
var PURE_CHANNEL_EVENT_PREFIX = "\xA7-";
var X_IPC_UPGRADE_KEY = "X-Dweb-Ipc-Upgrade-Key";
var ipcRequest = (reqId, method, url, headers, metaBody) => ({
  ...ipcMessageBase(IPC_MESSAGE_TYPE.REQUEST),
  reqId,
  method,
  url,
  headers,
  metaBody
});

class IpcRequest2 {
  reqId;
  url;
  method;
  headers;
  body;
  ipc;
  type = IPC_MESSAGE_TYPE.REQUEST;
  constructor(reqId, url, method, headers, body, ipc) {
    this.reqId = reqId;
    this.url = url;
    this.method = method;
    this.headers = headers;
    this.body = body;
    this.ipc = ipc;
    if (body instanceof IpcBodySender) {
      IpcBodySender.$usableByIpc(ipc, body);
    }
  }
  _parsed_url;
  get parsed_url() {
    return this._parsed_url ??= parseUrl(this.url);
  }
  get hasDuplex() {
    return this.duplexIpcId !== undefined;
  }
  lazyDuplexIpcId = new CacheGetter(() => {
    const upgrade_key = this.headers.get(X_IPC_UPGRADE_KEY);
    if (upgrade_key?.startsWith(PURE_CHANNEL_EVENT_PREFIX)) {
      const forkedIpcId = +upgrade_key.slice(PURE_CHANNEL_EVENT_PREFIX.length);
      if (Number.isFinite(forkedIpcId)) {
        return forkedIpcId;
      }
    }
  });
  get duplexIpcId() {
    return this.lazyDuplexIpcId.value;
  }
  getChannel() {
    return this.channel.value;
  }
  toRequest() {
    return buildRequestX(this.url, { method: this.method, headers: this.headers, body: this.body.raw });
  }
  toSerializable() {
    return ipcRequest(this.reqId, this.method, this.url, this.headers.toJSON(), this.body.metaBody);
  }
  toJSON() {
    return this.toSerializable();
  }
}

class IpcClientRequest extends IpcRequest2 {
  constructor() {
    super(...arguments);
  }
  server;
  toServer(serverIpc) {
    return this.server ??= new IpcServerRequest(this, serverIpc);
  }
  static fromText(reqId, url, method = PURE_METHOD.GET, headers = new IpcHeaders, text, ipc) {
    return new IpcClientRequest(reqId, url, method, headers, IpcBodySender.fromText(text, ipc), ipc);
  }
  static fromBinary(reqId, url, method = PURE_METHOD.GET, headers = new IpcHeaders, binary, ipc) {
    headers.init("Content-Type", "application/octet-stream");
    headers.init("Content-Length", binary.byteLength + "");
    return new IpcClientRequest(reqId, url, method, headers, IpcBodySender.fromBinary(binaryToU8a(binary), ipc), ipc);
  }
  static fromStream(reqId, url, method = PURE_METHOD.GET, headers = new IpcHeaders, stream, ipc) {
    headers.init("Content-Type", "application/octet-stream");
    return new IpcClientRequest(reqId, url, method, headers, IpcBodySender.fromStream(stream, ipc), ipc);
  }
  static fromRequest(reqId, ipc, url, init = {}) {
    const method = toPureMethod(init.method);
    const headers = init.headers instanceof IpcHeaders ? init.headers : new IpcHeaders(init.headers);
    let ipcBody;
    if (isBinary(init.body)) {
      ipcBody = IpcBodySender.fromBinary(init.body, ipc);
    } else if (init.body instanceof ReadableStream) {
      ipcBody = IpcBodySender.fromStream(init.body, ipc);
    } else if (init.body instanceof Blob) {
      ipcBody = IpcBodySender.fromStream(init.body.stream(), ipc);
    } else {
      ipcBody = IpcBodySender.fromText(init.body ?? "", ipc);
    }
    return new IpcClientRequest(reqId, url, method, headers, ipcBody, ipc);
  }
  channel = new CacheGetter(() => {
    const channelIpc = this._channelIpc;
    if (channelIpc === undefined) {
      throw new Error("no channel");
    }
    const channel = new PureChannel;
    (async () => {
      const forkedIpc = await channelIpc;
      await pureChannelToIpcEvent(forkedIpc, channel, "IpcClientRequest");
    })();
    return channel;
  });
  _channelIpc;
  async enableChannel() {
    this._channelIpc ??= this._channelIpc = this.ipc.fork().then((ipc) => {
      this.headers.set(X_IPC_UPGRADE_KEY, `${PURE_CHANNEL_EVENT_PREFIX}${ipc.pid}`);
      return ipc;
    });
  }
}

class IpcServerRequest extends IpcRequest2 {
  client;
  constructor(client, ipc) {
    super(client.reqId, client.url, client.method, client.headers, client.body, ipc);
    this.client = client;
  }
  channel = new CacheGetter(() => {
    const pid = this.duplexIpcId;
    const channel = new PureChannel;
    (async () => {
      const forkedIpc = await this.ipc.waitForkedIpc(pid);
      await pureChannelToIpcEvent(forkedIpc, channel, "IpcServerRequest");
    })();
    return channel;
  });
}

// src/core/ipc/ipc-message/IpcResponse.ts
var ipcResponse = (reqId, statusCode, headers, metaBody) => ({
  ...ipcMessageBase(IPC_MESSAGE_TYPE.RESPONSE),
  reqId,
  statusCode,
  headers,
  metaBody
});

class IpcResponse {
  reqId;
  statusCode;
  headers;
  body;
  ipc;
  type = IPC_MESSAGE_TYPE.RESPONSE;
  constructor(reqId, statusCode, headers, body, ipc) {
    this.reqId = reqId;
    this.statusCode = statusCode;
    this.headers = headers;
    this.body = body;
    this.ipc = ipc;
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
    let response;
    if (this.statusCode < 200 || this.statusCode > 599) {
      response = new Response(body, {
        headers: this.headers,
        status: 200
      });
      Object.defineProperty(response, "status", {
        value: this.statusCode,
        enumerable: true,
        configurable: true,
        writable: false
      });
    } else {
      response = new Response(body, {
        headers: this.headers,
        status: this.statusCode
      });
    }
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
  static async fromResponse(reqId, response, ipc, asBinary = false) {
    if (response.bodyUsed) {
      throw new Error("body used");
    }
    let ipcBody;
    if (asBinary || response.body == undefined || parseInt(response.headers.get("Content-Length") || "NaN") < 16777216) {
      ipcBody = IpcBodySender.fromBinary(binaryToU8a(await response.arrayBuffer()), ipc);
    } else {
      setStreamId(response.body, response.url);
      ipcBody = IpcBodySender.fromStream(response.body, ipc);
    }
    const ipcHeaders = new IpcHeaders(response.headers);
    return new IpcResponse(reqId, response.status, ipcHeaders, ipcBody, ipc);
  }
  static fromJson(reqId, statusCode, headers = new IpcHeaders, jsonable, ipc) {
    headers.init("Content-Type", "application/json");
    return this.fromText(reqId, statusCode, headers, JSON.stringify(jsonable), ipc);
  }
  static fromText(reqId, statusCode, headers = new IpcHeaders, text, ipc) {
    headers.init("Content-Type", "text/plain");
    return new IpcResponse(reqId, statusCode, headers, IpcBodySender.fromText(text, ipc), ipc);
  }
  static fromBinary(reqId, statusCode, headers = new IpcHeaders, binary, ipc) {
    headers.init("Content-Type", "application/octet-stream");
    headers.set("Content-Length", binary.byteLength + "");
    return new IpcResponse(reqId, statusCode, headers, IpcBodySender.fromBinary(binaryToU8a(binary), ipc), ipc);
  }
  static fromStream(reqId, statusCode, headers = new IpcHeaders, stream, ipc) {
    headers.init("Content-Type", "application/octet-stream");
    const ipcResponse2 = new IpcResponse(reqId, statusCode, headers, IpcBodySender.fromStream(stream, ipc), ipc);
    return ipcResponse2;
  }
  toSerializable() {
    return ipcResponse(this.reqId, this.statusCode, this.headers.toJSON(), this.body.metaBody);
  }
  toJSON() {
    return this.toSerializable();
  }
}

// src/helper/$once.ts
var $once = (fn) => {
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
var once = () => {
  return (target, prop, desp) => {
    const source_fun = desp.get;
    if (source_fun === undefined) {
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

// src/helper/StateSignal.ts
class StateSignal {
  equals;
  #state;
  get state() {
    return this.#state;
  }
  #signal = new Signal;
  constructor(state, equals = (a, b) => a === b) {
    this.equals = equals;
    this.#state = state;
  }
  emit = (state) => {
    if (!this.equals(state, this.#state)) {
      this.#state = state;
      this.#signal.emit(state);
    }
  };
  emitAndClear = (state) => {
    if (!this.equals(state, this.#state)) {
      this.#state = state;
      this.#signal.emitAndClear(state);
    } else {
      this.#signal.clear();
    }
  };
  clear = () => {
    this.#signal.clear();
  };
  asReadyonly() {
    return this;
  }
  listen = (cb) => {
    cb(this.#state);
    return this.#signal.listen(cb);
  };
}

// src/helper/mapHelper.ts
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
};

// src/helper/promiseSignal.ts
var promiseAsSignalListener = (promise) => {
  return (cb) => {
    promise.then(cb);
  };
};

// src/core/helper/Channel.ts
class Channel {
  streamOut = new ReadableStreamOut;
  controller = this.streamOut.controller;
  get stream() {
    return this.streamOut.stream;
  }
  _isClosedForSend = false;
  get isClosedForSend() {
    return this._isClosedForSend;
  }
  send(value) {
    if (this._isClosedForSend) {
      console.error("Channel send is close!!");
      return;
    }
    this.controller.enqueue(value);
  }
  closeWrite() {
    this._isClosedForSend = true;
  }
  close() {
    this.closeWrite();
    this.controller.close();
  }
  [Symbol.asyncIterator]() {
    return streamRead(this.streamOut.stream);
  }
}

// src/core/helper/Producer.ts
class Producer {
  name;
  constructor(name) {
    this.name = name;
  }
  toString() {
    return `Producer<${this.name}>`;
  }
  console = logger(this);
  static #Event = class Event {
    data;
    producer;
    constructor(data, producer) {
      this.data = data;
      this.producer = producer;
    }
    #consumed = false;
    get consumed() {
      return this.#consumed;
    }
    consume() {
      if (!this.#consumed) {
        this.#consumed = true;
        this.producer.buffers.delete(this);
      }
      return this.data;
    }
    complete() {
      this.producer.buffers.delete(this);
    }
    next() {
    }
    consumeMapNotNull(mapNotNull) {
      const result = mapNotNull(this.data);
      if (result !== null) {
        this.consume();
        return result;
      }
    }
    consumeFilter(filter) {
      if (filter(this.data)) {
        return this.consume();
      }
    }
    emitBy(consumer) {
      if (this.#consumed) {
        return;
      }
      const timeoutId = setTimeout(() => {
        console.warn(`emitBy TIMEOUT!! step=\$i consumer=${consumer} data=${this.data}`);
      }, 1000);
      consumer.input.send(this);
      clearTimeout(timeoutId);
      if (this.#consumed) {
        this.complete();
        this.producer.console.debug("emitBy", `event=${this} consumed by consumer=${consumer}`);
      }
    }
  };
  event(data) {
    return new Producer.#Event(data, this);
  }
  consumers = new Set;
  buffers = new Set;
  send(value) {
    this.#ensureOpen();
    this.#doSend(value);
  }
  #doSend(value) {
    const event = this.event(value);
    this.buffers.add(event);
    if (this.buffers.size > 10) {
      this.console.warn(`${this} buffers overflow maybe leak: ${this.buffers.size}`);
    }
    this.doEmit(event);
  }
  sendBeacon(value) {
    const event = this.event(value);
    this.doEmit(event);
  }
  trySend(value) {
    if (this.isClosedForSend) {
      this.sendBeacon(value);
    } else {
      this.#doSend(value);
    }
  }
  doEmit(event) {
    this.#ensureOpen();
    const consumers = this.consumers;
    for (const consumer of consumers) {
      if (!consumer.started || consumer.startingBuffers?.has(event) == true || consumer.input.isClosedForSend) {
        continue;
      }
      event.emitBy(consumer);
      if (event.consumed) {
        break;
      }
    }
  }
  static #Consumer = class Consumer {
    name;
    input;
    producer;
    constructor(name, input = new Channel, producer) {
      this.name = name;
      this.input = input;
      this.producer = producer;
      producer.consumers.add(this);
    }
    startingBuffers = null;
    #started = false;
    get started() {
      return this.#started;
    }
    #start() {
      this.#started = true;
      const starting = this.producer.buffers;
      this.startingBuffers = starting;
      for (const event of starting) {
        event.emitBy(this);
      }
      this.startingBuffers = null;
    }
    #collectors = new Set;
    #startCollect = $once(() => {
      const job = (async () => {
        for await (const event of this.input) {
          for (const collector of this.#collectors) {
            await collector(event);
          }
        }
      })();
      this.#start();
      return job;
    });
    collect(collector) {
      this.#collectors.add(collector);
      this.#startCollect();
      return () => this.#collectors.delete(collector);
    }
    mapNotNull(transform) {
      const signal = new Signal;
      this.collect((event) => {
        const result = transform(event.data);
        if (result !== undefined) {
          signal.emit(result);
        }
      });
      return signal.listen;
    }
    #destroySignal = createSignal();
    onDestroy = this.#destroySignal.listen;
    cancel = $once(() => {
      this.producer.consumers.delete(this);
      this.#destroySignal.emitAndClear();
    });
  };
  consumer(name) {
    this.#ensureOpen();
    const consumer = new Producer.#Consumer(name, undefined, this);
    return consumer;
  }
  #ensureOpen() {
    if (this.#isClosedForSend) {
      throw new Error(`${this} already close for emit.`);
    }
  }
  #isClosedForSend = false;
  get isClosedForSend() {
    return this.#isClosedForSend;
  }
  closeWrite() {
    if (this.#isClosedForSend) {
      return;
    }
    this.#isClosedForSend = true;
    const bufferEvents = this.buffers;
    for (const event of bufferEvents) {
      if (!event.consumed) {
        event.consume();
      }
    }
  }
  async close(cause) {
    this.closeWrite();
    for (const consumer of this.consumers) {
      consumer.input.close();
    }
    this.consumers.clear();
    this.buffers.clear();
    cause && this.console.debug("producer-close", cause);
  }
  #closeSignal = createSignal(false);
  onClosed = this.#closeSignal.listen;
}

// src/core/ipc/endpoint/internal/EndpointMessage.ts
var ENDPOINT_MESSAGE_TYPE;
(function(ENDPOINT_MESSAGE_TYPE2) {
  ENDPOINT_MESSAGE_TYPE2["LIFECYCLE"] = "life";
  ENDPOINT_MESSAGE_TYPE2["IPC"] = "ipc";
})(ENDPOINT_MESSAGE_TYPE || (ENDPOINT_MESSAGE_TYPE = {}));
var endpointMessageBase = (type) => ({ type });
// src/core/ipc/endpoint/EndpointIpcMessage.ts
var endpointIpcMessage = (pid, ipcMessage) => ({
  ...endpointMessageBase(ENDPOINT_MESSAGE_TYPE.IPC),
  pid,
  ipcMessage
});

// src/core/ipc/ipc-message/IpcFork.ts
var ipcFork = (pid, autoStart, locale, remote, startReason) => ({
  ...ipcMessageBase(IPC_MESSAGE_TYPE.FORK),
  pid,
  autoStart,
  locale,
  remote,
  startReason
});

// src/core/ipc/ipc-message/internal/IpcLifecycle.ts
var IPC_LIFECYCLE_STATE;
(function(IPC_LIFECYCLE_STATE2) {
  IPC_LIFECYCLE_STATE2["INIT"] = "init";
  IPC_LIFECYCLE_STATE2["OPENING"] = "opening";
  IPC_LIFECYCLE_STATE2["OPENED"] = "opened";
  IPC_LIFECYCLE_STATE2["CLOSING"] = "closing";
  IPC_LIFECYCLE_STATE2["CLOSED"] = "closed";
})(IPC_LIFECYCLE_STATE || (IPC_LIFECYCLE_STATE = {}));
var ipcLifecycleStateBase = (name) => ({ name });

// src/core/ipc/ipc-message/IpcLifecycle.ts
var _ipcLifecycle = (state) => ({ ...ipcMessageBase(IPC_MESSAGE_TYPE.LIFECYCLE), state });
var ipcLifecycle = Object.assign(_ipcLifecycle, {
  equals: (a, b) => {
    if (a.state.name !== b.state.name) {
      return false;
    }
    if (a.state.name === IPC_LIFECYCLE_STATE.CLOSING) {
      return a.state.reason === b.state.reason;
    }
    if (a.state.name === IPC_LIFECYCLE_STATE.CLOSED) {
      return a.state.reason === b.state.reason;
    }
    if (a.state.name === IPC_LIFECYCLE_STATE.INIT) {
      return JSON.stringify(a.state) === JSON.stringify(b.state);
    }
    return true;
  }
});
var ipcLifecycleInit = (pid, locale, remote) => ({
  ...ipcLifecycleStateBase(IPC_LIFECYCLE_STATE.INIT),
  pid,
  locale,
  remote
});
var ipcLifecycleOpening = () => ({
  ...ipcLifecycleStateBase(IPC_LIFECYCLE_STATE.OPENING)
});
var ipcLifecycleOpened = () => ({
  ...ipcLifecycleStateBase(IPC_LIFECYCLE_STATE.OPENED)
});
var ipcLifecycleClosing = (reason) => ({
  ...ipcLifecycleStateBase(IPC_LIFECYCLE_STATE.CLOSING),
  reason
});
var ipcLifecycleClosed = (reason) => ({
  ...ipcLifecycleStateBase(IPC_LIFECYCLE_STATE.CLOSED),
  reason
});
// src/core/ipc/ipc.ts
class Ipc {
  pid;
  endpoint;
  locale;
  remote;
  pool;
  debugId;
  constructor(pid, endpoint, locale, remote, pool, debugId = `${endpoint.debugId}/${pid}`) {
    this.pid = pid;
    this.endpoint = endpoint;
    this.locale = locale;
    this.remote = remote;
    this.pool = pool;
    this.debugId = debugId;
  }
  toString() {
    return `Ipc#${this.debugId}`;
  }
  [CUSTOM_INSPECT]() {
    return this.toString();
  }
  console = logger(this);
  #reqIdAcc = 0;
  #messageProducer = this.endpoint.getIpcMessageProducerByIpc(this);
  onMessage(name) {
    return this.#messageProducer.producer.consumer(name);
  }
  #lifecycleLocaleFlow = new StateSignal(ipcLifecycle(ipcLifecycleInit(this.pid, this.locale, this.remote)), ipcLifecycle.equals);
  lifecycleLocaleFlow = this.#lifecycleLocaleFlow.asReadyonly();
  get lifecycle() {
    return this.lifecycleLocaleFlow.state;
  }
  onLifecycle = this.lifecycleLocaleFlow.listen;
  #lifecycleRemoteFlow = this.onMessage(`ipc-lifecycle-remote#${this.pid}`).mapNotNull((message) => {
    if (message.type === IPC_MESSAGE_TYPE.LIFECYCLE) {
      return message;
    }
  });
  lifecycleRemoteFlow = this.#lifecycleRemoteFlow;
  #sendLifecycleToRemote(state) {
    this.console.debug("lifecycle-out", state);
    this.endpoint.postIpcMessage(endpointIpcMessage(this.pid, state));
  }
  get isActivity() {
    return this.endpoint.isActivity;
  }
  async awaitOpen(reason) {
    if (this.lifecycle.state.name === IPC_LIFECYCLE_STATE.OPENED) {
      return this.lifecycle;
    }
    const op = new PromiseOut;
    const off = this.onLifecycle((lifecycle2) => {
      switch (lifecycle2.state.name) {
        case IPC_LIFECYCLE_STATE.OPENED: {
          op.resolve(lifecycle2);
          break;
        }
        case (IPC_LIFECYCLE_STATE.CLOSED, IPC_LIFECYCLE_STATE.CLOSING): {
          op.reject("endpoint already closed");
          break;
        }
      }
    });
    const lifecycle = await op.promise;
    this.console.debug("awaitOpen", lifecycle, reason);
    off();
    return lifecycle;
  }
  async start(isAwait = true, reason) {
    this.console.debug("start", reason);
    if (isAwait) {
      this.endpoint.start(true);
      this.startOnce();
      await this.awaitOpen(`from-start ${reason}`);
    } else {
      this.endpoint.start(true);
      this.startOnce();
    }
  }
  startOnce = $once(() => {
    this.console.debug("startOnce", this.lifecycle);
    if (this.lifecycle.state.name === IPC_LIFECYCLE_STATE.INIT) {
      const opening = ipcLifecycle(ipcLifecycleOpening());
      this.#sendLifecycleToRemote(opening);
      this.#lifecycleLocaleFlow.emit(opening);
    } else {
      throw new Error(`fail to start: ipc=${this} state=${this.lifecycle}`);
    }
    this.#lifecycleRemoteFlow((lifecycleRemote) => {
      this.console.debug("lifecycle-in", `remote=${lifecycleRemote},local=${this.lifecycle}`);
      const doIpcOpened = () => {
        const opend = ipcLifecycle(ipcLifecycleOpened());
        this.#sendLifecycleToRemote(opend);
        this.#lifecycleLocaleFlow.emit(opend);
      };
      switch (lifecycleRemote.state.name) {
        case (IPC_LIFECYCLE_STATE.CLOSING, IPC_LIFECYCLE_STATE.CLOSED): {
          this.close(lifecycleRemote.state.reason);
          break;
        }
        case IPC_LIFECYCLE_STATE.OPENED: {
          if (this.lifecycle.state.name === IPC_LIFECYCLE_STATE.OPENING) {
            doIpcOpened();
          }
          break;
        }
        case IPC_LIFECYCLE_STATE.INIT: {
          this.#sendLifecycleToRemote(this.lifecycle);
          break;
        }
        case IPC_LIFECYCLE_STATE.OPENING: {
          doIpcOpened();
          break;
        }
      }
    });
    this.onMessage(`fork#${this.debugId}`).collect((event) => {
      const ipcFork2 = event.consumeMapNotNull((data) => {
        if (data.type === IPC_MESSAGE_TYPE.FORK) {
          return data;
        }
      });
      if (ipcFork2 === undefined) {
        return;
      }
      const forkedIpc = new Ipc(ipcFork2.pid, this.endpoint, this.locale, this.remote, this.pool);
      this.pool.safeCreatedIpc(forkedIpc, ipcFork2.autoStart, ipcFork2.startReason);
      mapHelper.getOrPut(this.forkedIpcMap, forkedIpc.pid, () => new PromiseOut).resolve(forkedIpc);
      this.#forkProducer.send(forkedIpc);
    });
  });
  forkedIpcMap = new Map;
  waitForkedIpc(pid) {
    return mapHelper.getOrPut(this.forkedIpcMap, pid, () => new PromiseOut).promise;
  }
  async fork(locale = this.locale, remote = this.remote, autoStart = false, startReason) {
    await this.awaitOpen("then-fork");
    const forkedIpc = this.pool.createIpc(this.endpoint, this.endpoint.generatePid(), locale, remote, autoStart, startReason);
    mapHelper.getOrPut(this.forkedIpcMap, forkedIpc.pid, () => new PromiseOut).resolve(forkedIpc);
    this.#forkProducer.send(forkedIpc);
    postMessage(ipcFork(forkedIpc.pid, autoStart, forkedIpc.remote, forkedIpc.locale, startReason));
    return forkedIpc;
  }
  #forkProducer = new Producer(`fork#${this.debugId}`);
  onFork(name) {
    return this.#forkProducer.consumer(name);
  }
  #messagePipeMap(name, mapNotNull) {
    const producer = new Producer(this.#messageProducer.producer.name + "/" + name);
    this.onClosed((reason) => {
      return producer.close(reason);
    });
    const consumer = this.onMessage(name);
    consumer.collect((event) => {
      const result = event.consumeMapNotNull(mapNotNull);
      if (result === undefined) {
        return;
      }
      producer.send(result);
    });
    producer.onClosed(() => {
      consumer.cancel();
    });
    return producer;
  }
  #requestProducer = new CacheGetter(() => this.#messagePipeMap("request", (ipcMessage) => {
    if (ipcMessage instanceof IpcClientRequest) {
      return ipcMessage.toServer(this);
    } else if (ipcMessage instanceof IpcServerRequest) {
      return ipcMessage;
    }
  }));
  onRequest(name) {
    return this.#requestProducer.value.consumer(name);
  }
  #responseProducer = new CacheGetter(() => this.#messagePipeMap("response", (ipcMessage) => {
    if (ipcMessage instanceof IpcResponse) {
      return ipcMessage;
    }
  }));
  onResponse(name) {
    return this.#responseProducer.value.consumer(name);
  }
  #streamProducer = new CacheGetter(() => this.#messagePipeMap("stream", (ipcMessage) => {
    if ("stream_id" in ipcMessage) {
      return ipcMessage;
    }
  }));
  onStream(name) {
    return this.#streamProducer.value.consumer(name);
  }
  #eventProducer = new CacheGetter(() => this.#messagePipeMap("event", (ipcMessage) => {
    if (ipcMessage.type === IPC_MESSAGE_TYPE.EVENT) {
      return ipcMessage;
    }
  }));
  onEvent(name) {
    return this.#eventProducer.value.consumer(name);
  }
  #errorProducer = new CacheGetter(() => this.#messagePipeMap("error", (ipcMessage) => {
    if (ipcMessage.type === IPC_MESSAGE_TYPE.ERROR) {
      return ipcMessage;
    }
  }));
  onError(name) {
    return this.#errorProducer.value.consumer(name);
  }
  #reqResMap = new CacheGetter(() => {
    const reqResMap = new Map;
    this.onResponse("req-res").collect((event) => {
      const response = event.consume();
      const result = mapHelper.getAndRemove(reqResMap, response.reqId);
      if (result === undefined) {
        throw new Error(`no found response by reqId: ${event.data.reqId}`);
      }
      result.resolve(response);
    });
    return reqResMap;
  });
  request(input, init) {
    const ipcRequest2 = input instanceof IpcClientRequest ? input : this.#buildIpcRequest(input, init);
    const result = this.#registerReqId(ipcRequest2.reqId);
    this.postMessage(ipcRequest2);
    return result.promise;
  }
  #registerReqId(reqId = this.#allocReqId()) {
    return mapHelper.getOrPut(this.#reqResMap.value, reqId, () => new PromiseOut);
  }
  #buildIpcRequest(url, init) {
    const reqId = this.#allocReqId();
    const ipcRequest2 = IpcClientRequest.fromRequest(reqId, this, url, init);
    return ipcRequest2;
  }
  #allocReqId() {
    return this.#reqIdAcc++;
  }
  async postMessage(message) {
    try {
      await this.awaitOpen("then-postMessage");
    } catch (e) {
      this.console.debug(`ipc(${this}) fail to poseMessage: ${e}`);
      return;
    }
    this.endpoint.postIpcMessage(endpointIpcMessage(this.pid, message));
  }
  get _closePo() {
    return new PromiseOut;
  }
  awaitClosed() {
    return this._closePo.promise;
  }
  get onClosed() {
    return promiseAsSignalListener(this._closePo.promise);
  }
  get isClosed() {
    return this.lifecycle.state.name == IPC_LIFECYCLE_STATE.CLOSED;
  }
  #closeOnce = $once(async (cause) => {
    this.console.debug("closing", cause);
    {
      const closing = ipcLifecycle(ipcLifecycleClosing(cause));
      this.#lifecycleLocaleFlow.emit(closing);
      this.#sendLifecycleToRemote(closing);
    }
    await this.#messageProducer.producer.close(cause);
    this._closePo.resolve(cause);
    {
      const closed = ipcLifecycle(ipcLifecycleClosed(cause));
      this.#lifecycleLocaleFlow.emitAndClear(closed);
      this.#sendLifecycleToRemote(closed);
    }
  });
  async close(cause) {
    this.#closeOnce(cause);
    this.#destroy();
  }
  _isDestroy = false;
  async#destroy() {
  }
}
__legacyDecorateClassTS([
  once()
], Ipc.prototype, "_closePo", null);
__legacyDecorateClassTS([
  once()
], Ipc.prototype, "onClosed", null);

// src/core/ipc/IpcPool.ts
class IpcPool {
  poolId;
  constructor(poolId = `js-${crypto.randomUUID()}`) {
    this.poolId = poolId;
  }
  toString() {
    return `IpcPool#${this.poolId}`;
  }
  [CUSTOM_INSPECT]() {
    return this.toString();
  }
  console = logger(this);
  #ipcSet = new Set;
  #streamPool = new Map;
  createIpc(endpoint, pid, locale, remote, autoStart = false, startReason) {
    const ipc2 = new Ipc(pid, endpoint, locale, remote, this);
    this.safeCreatedIpc(ipc2, autoStart, startReason);
    return ipc2;
  }
  safeCreatedIpc(ipc2, autoStart, startReason) {
    this.#ipcSet.add(ipc2);
    if (autoStart) {
      ipc2.start(true, startReason ?? "autoStart");
    }
    ipc2.onClosed(() => {
      this.#ipcSet.delete(ipc2);
      this.console.debug("ipcpool-remote-ipc", ipc2);
    });
  }
  #destroySignal = createSignal();
  onDestory = this.#destroySignal.listen;
  async destroy() {
    this.#destroySignal.emit();
    for (const _ipc of this.#ipcSet) {
      await _ipc.close();
    }
    this.#ipcSet.clear();
  }
}
var jsIpcPool = new IpcPool;
// src/core/ipc/endpoint/internal/EndpointLifecycle.ts
var ENDPOINT_LIFECYCLE_STATE;
(function(ENDPOINT_LIFECYCLE_STATE2) {
  ENDPOINT_LIFECYCLE_STATE2["INIT"] = "init";
  ENDPOINT_LIFECYCLE_STATE2["OPENING"] = "opening";
  ENDPOINT_LIFECYCLE_STATE2["OPENED"] = "opened";
  ENDPOINT_LIFECYCLE_STATE2["CLOSING"] = "closing";
  ENDPOINT_LIFECYCLE_STATE2["CLOSED"] = "closed";
})(ENDPOINT_LIFECYCLE_STATE || (ENDPOINT_LIFECYCLE_STATE = {}));
var ENDPOINT_PROTOCOL;
(function(ENDPOINT_PROTOCOL2) {
  ENDPOINT_PROTOCOL2["JSON"] = "JSON";
  ENDPOINT_PROTOCOL2["CBOR"] = "CBOR";
})(ENDPOINT_PROTOCOL || (ENDPOINT_PROTOCOL = {}));

// src/core/ipc/endpoint/EndpointLifecycle.ts
var _endpointLifecycle = (state) => ({
  ...endpointMessageBase(ENDPOINT_MESSAGE_TYPE.LIFECYCLE),
  state
});
var endpointLifecycle = Object.assign(_endpointLifecycle, {
  equals: (a, b) => {
    if (a.state.name !== b.state.name) {
      return false;
    }
    if (a.state.name === ENDPOINT_LIFECYCLE_STATE.CLOSING) {
      return a.state.reason === b.state.reason;
    }
    if (a.state.name === ENDPOINT_LIFECYCLE_STATE.CLOSED) {
      return a.state.reason === b.state.reason;
    }
    if (a.state.name === ENDPOINT_LIFECYCLE_STATE.OPENING || a.state.name === ENDPOINT_LIFECYCLE_STATE.OPENED) {
      return JSON.stringify(a.state) === JSON.stringify(b.state);
    }
    return true;
  }
});
var endpointLifecycleStateBase = (name) => ({ name });
var endpointLifecycleInit = () => endpointLifecycleStateBase(ENDPOINT_LIFECYCLE_STATE.INIT);
var endpointLifecycleOpening = (subProtocols) => ({
  ...endpointLifecycleStateBase(ENDPOINT_LIFECYCLE_STATE.OPENING),
  subProtocols: [...subProtocols]
});
var endpointLifecycleOpend = (subProtocols) => ({
  ...endpointLifecycleStateBase(ENDPOINT_LIFECYCLE_STATE.OPENED),
  subProtocols: [...subProtocols]
});
var endpointLifecycleClosing = (reason) => ({
  ...endpointLifecycleStateBase(ENDPOINT_LIFECYCLE_STATE.CLOSING),
  reason
});
var endpointLifecycleClosed = (reason) => ({
  ...endpointLifecycleStateBase(ENDPOINT_LIFECYCLE_STATE.CLOSED),
  reason
});
// src/helper/setHelper.ts
var setHelper = new class {
  intersect(a, b) {
    const result = new Set(a);
    for (const item of b) {
      result.delete(item);
    }
    return result;
  }
  equals(a, b) {
    const diff = new Set(a);
    for (const item of b) {
      if (diff.delete(item) === false) {
        return false;
      }
    }
    return true;
  }
  add(target, value) {
    if (target.has(value)) {
      return false;
    }
    target.add(value);
    return true;
  }
};

// src/core/ipc/endpoint/IpcEndpoint.ts
class IpcEndpoint {
  debugId;
  constructor(debugId) {
    this.debugId = debugId;
  }
  [CUSTOM_INSPECT]() {
    return this.toString();
  }
  accPid = 0;
  generatePid = () => this.accPid += 2;
  console = logger(this);
  ipcMessageProducers = new Map;
  ipcMessageProducer(pid) {
    const ipcPo = new PromiseOut;
    const producer = new Producer(`ipc-msg/${this.debugId}/${pid}`);
    const consumer = producer.consumer("watch-fork");
    consumer.collect((event) => {
      if (event.data.type === IPC_MESSAGE_TYPE.FORK) {
        this.accPid = Math.max(event.data.pid - 1, this.accPid);
      }
    });
    producer.onClosed(() => {
      this.ipcMessageProducers.delete(pid);
    });
    return { pid, ipcPo, producer };
  }
  getIpcMessageProducer(pid) {
    return mapHelper.getOrPut(this.ipcMessageProducers, pid, () => this.ipcMessageProducer(pid));
  }
  getIpcMessageProducerByIpc(ipc2) {
    const result = this.getIpcMessageProducer(ipc2.pid);
    ipc2.onClosed(() => {
      this.ipcMessageProducers.delete(ipc2.pid);
    });
    result.ipcPo.resolve(ipc2);
    return result;
  }
  lifecycleLocaleFlow = new StateSignal(endpointLifecycle(endpointLifecycleInit()), endpointLifecycle.equals);
  onLifecycle = this.lifecycleLocaleFlow.listen;
  get lifecycle() {
    return this.lifecycleLocaleFlow.state;
  }
  get isActivity() {
    return ENDPOINT_LIFECYCLE_STATE.OPENED == this.lifecycle.state.name;
  }
  async start(isAwait = true) {
    this.startOnce();
    if (isAwait) {
      await this.awaitOpen("from-start");
    }
  }
  startOnce = $once(async () => {
    this.console.debug("startOnce", this.lifecycle);
    await this.doStart();
    let localeSubProtocols = this.getLocaleSubProtocols();
    if (this.lifecycle.state.name === ENDPOINT_LIFECYCLE_STATE.INIT) {
      const opening = endpointLifecycle(endpointLifecycleOpening(localeSubProtocols));
      this.sendLifecycleToRemote(opening);
      this.console.debug("emit-locale-lifecycle", opening);
      this.lifecycleLocaleFlow.emit(opening);
    } else {
      throw new Error(`endpoint state=${this.lifecycle}`);
    }
    this.lifecycleRemoteFlow.listen((lifecycle) => {
      this.console.debug("remote-lifecycle-in", lifecycle);
      switch (lifecycle.state.name) {
        case ENDPOINT_LIFECYCLE_STATE.CLOSING:
        case ENDPOINT_LIFECYCLE_STATE.CLOSED: {
          this.close();
          break;
        }
        case ENDPOINT_LIFECYCLE_STATE.OPENED: {
          const lifecycleLocale = this.lifecycle;
          this.console.debug("remote-opend-&-locale-lifecycle", lifecycleLocale);
          if (lifecycleLocale.state.name === ENDPOINT_LIFECYCLE_STATE.OPENING) {
            const opend = endpointLifecycle(endpointLifecycleOpend(lifecycleLocale.state.subProtocols));
            this.sendLifecycleToRemote(opend);
            this.console.debug("emit-locale-lifecycle", opend);
            this.lifecycleLocaleFlow.emit(opend);
            this.accPid++;
          }
          break;
        }
        case ENDPOINT_LIFECYCLE_STATE.INIT: {
          this.sendLifecycleToRemote(this.lifecycle);
          break;
        }
        case ENDPOINT_LIFECYCLE_STATE.OPENING: {
          let nextState;
          this.console.debug("ENDPOINT_LIFECYCLE_STATE.OPENING", [...localeSubProtocols].sort().join(), lifecycle.state.subProtocols.slice().sort().join());
          if (setHelper.equals(localeSubProtocols, lifecycle.state.subProtocols) === false) {
            localeSubProtocols = setHelper.intersect(localeSubProtocols, lifecycle.state.subProtocols);
            const opening = endpointLifecycle(endpointLifecycleOpening(localeSubProtocols));
            this.lifecycleLocaleFlow.emit(opening);
            nextState = opening;
          } else {
            nextState = endpointLifecycle(endpointLifecycleOpend(localeSubProtocols));
          }
          this.sendLifecycleToRemote(nextState);
          break;
        }
      }
    });
  });
  async awaitOpen(reason) {
    if (this.lifecycle.state.name == ENDPOINT_LIFECYCLE_STATE.OPENED) {
      return this.lifecycle;
    }
    const op = new PromiseOut;
    const off = this.onLifecycle((lifecycle2) => {
      switch (lifecycle2.state.name) {
        case ENDPOINT_LIFECYCLE_STATE.OPENED: {
          op.resolve(lifecycle2);
          break;
        }
        case (ENDPOINT_LIFECYCLE_STATE.CLOSED, ENDPOINT_LIFECYCLE_STATE.CLOSING): {
          op.reject("endpoint already closed");
          break;
        }
      }
    });
    const lifecycle = await op.promise;
    this.console.debug("awaitOpen", lifecycle, reason);
    off();
    return lifecycle;
  }
  _isClose = false;
  get isClose() {
    return this._isClose;
  }
  async close() {
    this._isClose = true;
    await this.doClose();
  }
  async doClose(cause) {
    switch (this.lifecycle.state.name) {
      case ENDPOINT_LIFECYCLE_STATE.OPENED:
      case ENDPOINT_LIFECYCLE_STATE.OPENING: {
        this.sendLifecycleToRemote(endpointLifecycle(endpointLifecycleClosing()));
        break;
      }
      case ENDPOINT_LIFECYCLE_STATE.CLOSED: {
        return;
      }
    }
    this.beforeClose?.();
    for (const channel of this.ipcMessageProducers.values()) {
      await channel.producer.close(cause);
    }
    this.ipcMessageProducers.clear();
    this.sendLifecycleToRemote(endpointLifecycle(endpointLifecycleClosed()));
    this.afterClosed?.();
  }
  beforeClose;
  afterClosed;
}
// node_modules/cbor-x/decode.js
function checkedRead() {
  try {
    let result = read();
    if (bundledStrings) {
      if (position >= bundledStrings.postBundlePosition) {
        let error = new Error("Unexpected bundle position");
        error.incomplete = true;
        throw error;
      }
      position = bundledStrings.postBundlePosition;
      bundledStrings = null;
    }
    if (position == srcEnd) {
      currentStructures = null;
      src = null;
      if (referenceMap)
        referenceMap = null;
    } else if (position > srcEnd) {
      let error = new Error("Unexpected end of CBOR data");
      error.incomplete = true;
      throw error;
    } else if (!sequentialMode) {
      throw new Error("Data read, but end of buffer not reached");
    }
    return result;
  } catch (error) {
    clearSource();
    if (error instanceof RangeError || error.message.startsWith("Unexpected end of buffer")) {
      error.incomplete = true;
    }
    throw error;
  }
}
function read() {
  let token = src[position++];
  let majorType = token >> 5;
  token = token & 31;
  if (token > 23) {
    switch (token) {
      case 24:
        token = src[position++];
        break;
      case 25:
        if (majorType == 7) {
          return getFloat16();
        }
        token = dataView.getUint16(position);
        position += 2;
        break;
      case 26:
        if (majorType == 7) {
          let value = dataView.getFloat32(position);
          if (currentDecoder.useFloat32 > 2) {
            let multiplier = mult10[(src[position] & 127) << 1 | src[position + 1] >> 7];
            position += 4;
            return (multiplier * value + (value > 0 ? 0.5 : -0.5) >> 0) / multiplier;
          }
          position += 4;
          return value;
        }
        token = dataView.getUint32(position);
        position += 4;
        break;
      case 27:
        if (majorType == 7) {
          let value = dataView.getFloat64(position);
          position += 8;
          return value;
        }
        if (majorType > 1) {
          if (dataView.getUint32(position) > 0)
            throw new Error("JavaScript does not support arrays, maps, or strings with length over 4294967295");
          token = dataView.getUint32(position + 4);
        } else if (currentDecoder.int64AsNumber) {
          token = dataView.getUint32(position) * 4294967296;
          token += dataView.getUint32(position + 4);
        } else
          token = dataView.getBigUint64(position);
        position += 8;
        break;
      case 31:
        switch (majorType) {
          case 2:
          case 3:
            throw new Error("Indefinite length not supported for byte or text strings");
          case 4:
            let array = [];
            let value, i = 0;
            while ((value = read()) != STOP_CODE) {
              array[i++] = value;
            }
            return majorType == 4 ? array : majorType == 3 ? array.join("") : Buffer.concat(array);
          case 5:
            let key;
            if (currentDecoder.mapsAsObjects) {
              let object = {};
              if (currentDecoder.keyMap)
                while ((key = read()) != STOP_CODE)
                  object[safeKey(currentDecoder.decodeKey(key))] = read();
              else
                while ((key = read()) != STOP_CODE)
                  object[safeKey(key)] = read();
              return object;
            } else {
              if (restoreMapsAsObject) {
                currentDecoder.mapsAsObjects = true;
                restoreMapsAsObject = false;
              }
              let map = new Map;
              if (currentDecoder.keyMap)
                while ((key = read()) != STOP_CODE)
                  map.set(currentDecoder.decodeKey(key), read());
              else
                while ((key = read()) != STOP_CODE)
                  map.set(key, read());
              return map;
            }
          case 7:
            return STOP_CODE;
          default:
            throw new Error("Invalid major type for indefinite length " + majorType);
        }
      default:
        throw new Error("Unknown token " + token);
    }
  }
  switch (majorType) {
    case 0:
      return token;
    case 1:
      return ~token;
    case 2:
      return readBin(token);
    case 3:
      if (srcStringEnd >= position) {
        return srcString.slice(position - srcStringStart, (position += token) - srcStringStart);
      }
      if (srcStringEnd == 0 && srcEnd < 140 && token < 32) {
        let string = token < 16 ? shortStringInJS(token) : longStringInJS(token);
        if (string != null)
          return string;
      }
      return readFixedString(token);
    case 4:
      let array = new Array(token);
      for (let i = 0;i < token; i++)
        array[i] = read();
      return array;
    case 5:
      if (currentDecoder.mapsAsObjects) {
        let object = {};
        if (currentDecoder.keyMap)
          for (let i = 0;i < token; i++)
            object[safeKey(currentDecoder.decodeKey(read()))] = read();
        else
          for (let i = 0;i < token; i++)
            object[safeKey(read())] = read();
        return object;
      } else {
        if (restoreMapsAsObject) {
          currentDecoder.mapsAsObjects = true;
          restoreMapsAsObject = false;
        }
        let map = new Map;
        if (currentDecoder.keyMap)
          for (let i = 0;i < token; i++)
            map.set(currentDecoder.decodeKey(read()), read());
        else
          for (let i = 0;i < token; i++)
            map.set(read(), read());
        return map;
      }
    case 6:
      if (token >= BUNDLED_STRINGS_ID) {
        let structure = currentStructures[token & 8191];
        if (structure) {
          if (!structure.read)
            structure.read = createStructureReader(structure);
          return structure.read();
        }
        if (token < 65536) {
          if (token == RECORD_INLINE_ID) {
            let length = readJustLength();
            let id = read();
            let structure2 = read();
            recordDefinition(id, structure2);
            let object = {};
            if (currentDecoder.keyMap)
              for (let i = 2;i < length; i++) {
                let key = currentDecoder.decodeKey(structure2[i - 2]);
                object[safeKey(key)] = read();
              }
            else
              for (let i = 2;i < length; i++) {
                let key = structure2[i - 2];
                object[safeKey(key)] = read();
              }
            return object;
          } else if (token == RECORD_DEFINITIONS_ID) {
            let length = readJustLength();
            let id = read();
            for (let i = 2;i < length; i++) {
              recordDefinition(id++, read());
            }
            return read();
          } else if (token == BUNDLED_STRINGS_ID) {
            return readBundleExt();
          }
          if (currentDecoder.getShared) {
            loadShared();
            structure = currentStructures[token & 8191];
            if (structure) {
              if (!structure.read)
                structure.read = createStructureReader(structure);
              return structure.read();
            }
          }
        }
      }
      let extension = currentExtensions[token];
      if (extension) {
        if (extension.handlesRead)
          return extension(read);
        else
          return extension(read());
      } else {
        let input = read();
        for (let i = 0;i < currentExtensionRanges.length; i++) {
          let value = currentExtensionRanges[i](token, input);
          if (value !== undefined)
            return value;
        }
        return new Tag(input, token);
      }
    case 7:
      switch (token) {
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
          let packedValue = (packedValues || getPackedValues())[token];
          if (packedValue !== undefined)
            return packedValue;
          throw new Error("Unknown token " + token);
      }
    default:
      if (isNaN(token)) {
        let error = new Error("Unexpected end of CBOR data");
        error.incomplete = true;
        throw error;
      }
      throw new Error("Unknown CBOR token " + token);
  }
}
var createStructureReader = function(structure) {
  function readObject() {
    let length = src[position++];
    length = length & 31;
    if (length > 23) {
      switch (length) {
        case 24:
          length = src[position++];
          break;
        case 25:
          length = dataView.getUint16(position);
          position += 2;
          break;
        case 26:
          length = dataView.getUint32(position);
          position += 4;
          break;
        default:
          throw new Error("Expected array header, but got " + src[position - 1]);
      }
    }
    let compiledReader = this.compiledReader;
    while (compiledReader) {
      if (compiledReader.propertyCount === length)
        return compiledReader(read);
      compiledReader = compiledReader.next;
    }
    if (this.slowReads++ >= inlineObjectReadThreshold) {
      let array = this.length == length ? this : this.slice(0, length);
      compiledReader = currentDecoder.keyMap ? new Function("r", "return {" + array.map((k) => currentDecoder.decodeKey(k)).map((k) => validName.test(k) ? safeKey(k) + ":r()" : "[" + JSON.stringify(k) + "]:r()").join(",") + "}") : new Function("r", "return {" + array.map((key) => validName.test(key) ? safeKey(key) + ":r()" : "[" + JSON.stringify(key) + "]:r()").join(",") + "}");
      if (this.compiledReader)
        compiledReader.next = this.compiledReader;
      compiledReader.propertyCount = length;
      this.compiledReader = compiledReader;
      return compiledReader(read);
    }
    let object = {};
    if (currentDecoder.keyMap)
      for (let i = 0;i < length; i++)
        object[safeKey(currentDecoder.decodeKey(this[i]))] = read();
    else
      for (let i = 0;i < length; i++) {
        object[safeKey(this[i])] = read();
      }
    return object;
  }
  structure.slowReads = 0;
  return readObject;
};
var safeKey = function(key) {
  if (typeof key === "string")
    return key === "__proto__" ? "__proto_" : key;
  if (typeof key === "number" || typeof key === "boolean" || typeof key === "bigint")
    return key.toString();
  if (key == null)
    return key + "";
  throw new Error("Invalid property name type " + typeof key);
};
var readStringJS = function(length) {
  let result;
  if (length < 16) {
    if (result = shortStringInJS(length))
      return result;
  }
  if (length > 64 && decoder)
    return decoder.decode(src.subarray(position, position += length));
  const end = position + length;
  const units = [];
  result = "";
  while (position < end) {
    const byte1 = src[position++];
    if ((byte1 & 128) === 0) {
      units.push(byte1);
    } else if ((byte1 & 224) === 192) {
      const byte2 = src[position++] & 63;
      units.push((byte1 & 31) << 6 | byte2);
    } else if ((byte1 & 240) === 224) {
      const byte2 = src[position++] & 63;
      const byte3 = src[position++] & 63;
      units.push((byte1 & 31) << 12 | byte2 << 6 | byte3);
    } else if ((byte1 & 248) === 240) {
      const byte2 = src[position++] & 63;
      const byte3 = src[position++] & 63;
      const byte4 = src[position++] & 63;
      let unit = (byte1 & 7) << 18 | byte2 << 12 | byte3 << 6 | byte4;
      if (unit > 65535) {
        unit -= 65536;
        units.push(unit >>> 10 & 1023 | 55296);
        unit = 56320 | unit & 1023;
      }
      units.push(unit);
    } else {
      units.push(byte1);
    }
    if (units.length >= 4096) {
      result += fromCharCode.apply(String, units);
      units.length = 0;
    }
  }
  if (units.length > 0) {
    result += fromCharCode.apply(String, units);
  }
  return result;
};
var longStringInJS = function(length) {
  let start = position;
  let bytes = new Array(length);
  for (let i = 0;i < length; i++) {
    const byte = src[position++];
    if ((byte & 128) > 0) {
      position = start;
      return;
    }
    bytes[i] = byte;
  }
  return fromCharCode.apply(String, bytes);
};
var shortStringInJS = function(length) {
  if (length < 4) {
    if (length < 2) {
      if (length === 0)
        return "";
      else {
        let a = src[position++];
        if ((a & 128) > 1) {
          position -= 1;
          return;
        }
        return fromCharCode(a);
      }
    } else {
      let a = src[position++];
      let b = src[position++];
      if ((a & 128) > 0 || (b & 128) > 0) {
        position -= 2;
        return;
      }
      if (length < 3)
        return fromCharCode(a, b);
      let c = src[position++];
      if ((c & 128) > 0) {
        position -= 3;
        return;
      }
      return fromCharCode(a, b, c);
    }
  } else {
    let a = src[position++];
    let b = src[position++];
    let c = src[position++];
    let d = src[position++];
    if ((a & 128) > 0 || (b & 128) > 0 || (c & 128) > 0 || (d & 128) > 0) {
      position -= 4;
      return;
    }
    if (length < 6) {
      if (length === 4)
        return fromCharCode(a, b, c, d);
      else {
        let e = src[position++];
        if ((e & 128) > 0) {
          position -= 5;
          return;
        }
        return fromCharCode(a, b, c, d, e);
      }
    } else if (length < 8) {
      let e = src[position++];
      let f = src[position++];
      if ((e & 128) > 0 || (f & 128) > 0) {
        position -= 6;
        return;
      }
      if (length < 7)
        return fromCharCode(a, b, c, d, e, f);
      let g = src[position++];
      if ((g & 128) > 0) {
        position -= 7;
        return;
      }
      return fromCharCode(a, b, c, d, e, f, g);
    } else {
      let e = src[position++];
      let f = src[position++];
      let g = src[position++];
      let h = src[position++];
      if ((e & 128) > 0 || (f & 128) > 0 || (g & 128) > 0 || (h & 128) > 0) {
        position -= 8;
        return;
      }
      if (length < 10) {
        if (length === 8)
          return fromCharCode(a, b, c, d, e, f, g, h);
        else {
          let i = src[position++];
          if ((i & 128) > 0) {
            position -= 9;
            return;
          }
          return fromCharCode(a, b, c, d, e, f, g, h, i);
        }
      } else if (length < 12) {
        let i = src[position++];
        let j = src[position++];
        if ((i & 128) > 0 || (j & 128) > 0) {
          position -= 10;
          return;
        }
        if (length < 11)
          return fromCharCode(a, b, c, d, e, f, g, h, i, j);
        let k = src[position++];
        if ((k & 128) > 0) {
          position -= 11;
          return;
        }
        return fromCharCode(a, b, c, d, e, f, g, h, i, j, k);
      } else {
        let i = src[position++];
        let j = src[position++];
        let k = src[position++];
        let l = src[position++];
        if ((i & 128) > 0 || (j & 128) > 0 || (k & 128) > 0 || (l & 128) > 0) {
          position -= 12;
          return;
        }
        if (length < 14) {
          if (length === 12)
            return fromCharCode(a, b, c, d, e, f, g, h, i, j, k, l);
          else {
            let m = src[position++];
            if ((m & 128) > 0) {
              position -= 13;
              return;
            }
            return fromCharCode(a, b, c, d, e, f, g, h, i, j, k, l, m);
          }
        } else {
          let m = src[position++];
          let n = src[position++];
          if ((m & 128) > 0 || (n & 128) > 0) {
            position -= 14;
            return;
          }
          if (length < 15)
            return fromCharCode(a, b, c, d, e, f, g, h, i, j, k, l, m, n);
          let o = src[position++];
          if ((o & 128) > 0) {
            position -= 15;
            return;
          }
          return fromCharCode(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o);
        }
      }
    }
  }
};
var readBin = function(length) {
  return currentDecoder.copyBuffers ? Uint8Array.prototype.slice.call(src, position, position += length) : src.subarray(position, position += length);
};
var getFloat16 = function() {
  let byte0 = src[position++];
  let byte1 = src[position++];
  let exponent = (byte0 & 127) >> 2;
  if (exponent === 31) {
    if (byte1 || byte0 & 3)
      return NaN;
    return byte0 & 128 ? (-Infinity) : Infinity;
  }
  if (exponent === 0) {
    let abs = ((byte0 & 3) << 8 | byte1) / (1 << 24);
    return byte0 & 128 ? -abs : abs;
  }
  u8Array[3] = byte0 & 128 | (exponent >> 1) + 56;
  u8Array[2] = (byte0 & 7) << 5 | byte1 >> 3;
  u8Array[1] = byte1 << 5;
  u8Array[0] = 0;
  return f32Array[0];
};
var combine = function(a, b) {
  if (typeof a === "string")
    return a + b;
  if (a instanceof Array)
    return a.concat(b);
  return Object.assign({}, a, b);
};
var getPackedValues = function() {
  if (!packedValues) {
    if (currentDecoder.getShared)
      loadShared();
    else
      throw new Error("No packed values available");
  }
  return packedValues;
};
var registerTypedArray = function(TypedArray, tag) {
  let dvMethod = "get" + TypedArray.name.slice(0, -5);
  let bytesPerElement;
  if (typeof TypedArray === "function")
    bytesPerElement = TypedArray.BYTES_PER_ELEMENT;
  else
    TypedArray = null;
  for (let littleEndian = 0;littleEndian < 2; littleEndian++) {
    if (!littleEndian && bytesPerElement == 1)
      continue;
    let sizeShift = bytesPerElement == 2 ? 1 : bytesPerElement == 4 ? 2 : bytesPerElement == 8 ? 3 : 0;
    currentExtensions[littleEndian ? tag : tag - 4] = bytesPerElement == 1 || littleEndian == isLittleEndianMachine ? (buffer) => {
      if (!TypedArray)
        throw new Error("Could not find typed array for code " + tag);
      if (!currentDecoder.copyBuffers) {
        if (bytesPerElement === 1 || bytesPerElement === 2 && !(buffer.byteOffset & 1) || bytesPerElement === 4 && !(buffer.byteOffset & 3) || bytesPerElement === 8 && !(buffer.byteOffset & 7))
          return new TypedArray(buffer.buffer, buffer.byteOffset, buffer.byteLength >> sizeShift);
      }
      return new TypedArray(Uint8Array.prototype.slice.call(buffer, 0).buffer);
    } : (buffer) => {
      if (!TypedArray)
        throw new Error("Could not find typed array for code " + tag);
      let dv = new DataView(buffer.buffer, buffer.byteOffset, buffer.byteLength);
      let elements = buffer.length >> sizeShift;
      let ta = new TypedArray(elements);
      let method = dv[dvMethod];
      for (let i = 0;i < elements; i++) {
        ta[i] = method.call(dv, i << sizeShift, littleEndian);
      }
      return ta;
    };
  }
};
var readBundleExt = function() {
  let length = readJustLength();
  let bundlePosition = position + read();
  for (let i = 2;i < length; i++) {
    let bundleLength = readJustLength();
    position += bundleLength;
  }
  let dataPosition = position;
  position = bundlePosition;
  bundledStrings = [readStringJS(readJustLength()), readStringJS(readJustLength())];
  bundledStrings.position0 = 0;
  bundledStrings.position1 = 0;
  bundledStrings.postBundlePosition = position;
  position = dataPosition;
  return read();
};
var readJustLength = function() {
  let token = src[position++] & 31;
  if (token > 23) {
    switch (token) {
      case 24:
        token = src[position++];
        break;
      case 25:
        token = dataView.getUint16(position);
        position += 2;
        break;
      case 26:
        token = dataView.getUint32(position);
        position += 4;
        break;
    }
  }
  return token;
};
var loadShared = function() {
  if (currentDecoder.getShared) {
    let sharedData = saveState(() => {
      src = null;
      return currentDecoder.getShared();
    }) || {};
    let updatedStructures = sharedData.structures || [];
    currentDecoder.sharedVersion = sharedData.version;
    packedValues = currentDecoder.sharedValues = sharedData.packedValues;
    if (currentStructures === true)
      currentDecoder.structures = currentStructures = updatedStructures;
    else
      currentStructures.splice.apply(currentStructures, [0, updatedStructures.length].concat(updatedStructures));
  }
};
var saveState = function(callback) {
  let savedSrcEnd = srcEnd;
  let savedPosition = position;
  let savedStringPosition = stringPosition;
  let savedSrcStringStart = srcStringStart;
  let savedSrcStringEnd = srcStringEnd;
  let savedSrcString = srcString;
  let savedStrings = strings;
  let savedReferenceMap = referenceMap;
  let savedBundledStrings = bundledStrings;
  let savedSrc = new Uint8Array(src.slice(0, srcEnd));
  let savedStructures = currentStructures;
  let savedDecoder = currentDecoder;
  let savedSequentialMode = sequentialMode;
  let value = callback();
  srcEnd = savedSrcEnd;
  position = savedPosition;
  stringPosition = savedStringPosition;
  srcStringStart = savedSrcStringStart;
  srcStringEnd = savedSrcStringEnd;
  srcString = savedSrcString;
  strings = savedStrings;
  referenceMap = savedReferenceMap;
  bundledStrings = savedBundledStrings;
  src = savedSrc;
  sequentialMode = savedSequentialMode;
  currentStructures = savedStructures;
  currentDecoder = savedDecoder;
  dataView = new DataView(src.buffer, src.byteOffset, src.byteLength);
  return value;
};
function clearSource() {
  src = null;
  referenceMap = null;
  currentStructures = null;
}
var decoder;
try {
  decoder = new TextDecoder;
} catch (error) {
}
var src;
var srcEnd;
var position = 0;
var EMPTY_ARRAY = [];
var LEGACY_RECORD_INLINE_ID = 105;
var RECORD_DEFINITIONS_ID = 57342;
var RECORD_INLINE_ID = 57343;
var BUNDLED_STRINGS_ID = 57337;
var PACKED_REFERENCE_TAG_ID = 6;
var STOP_CODE = {};
var strings = EMPTY_ARRAY;
var stringPosition = 0;
var currentDecoder = {};
var currentStructures;
var srcString;
var srcStringStart = 0;
var srcStringEnd = 0;
var bundledStrings;
var referenceMap;
var currentExtensions = [];
var currentExtensionRanges = [];
var packedValues;
var dataView;
var restoreMapsAsObject;
var defaultOptions = {
  useRecords: false,
  mapsAsObjects: true
};
var sequentialMode = false;
var inlineObjectReadThreshold = 2;
try {
  new Function("");
} catch (error) {
  inlineObjectReadThreshold = Infinity;
}

class Decoder {
  constructor(options) {
    if (options) {
      if ((options.keyMap || options._keyMap) && !options.useRecords) {
        options.useRecords = false;
        options.mapsAsObjects = true;
      }
      if (options.useRecords === false && options.mapsAsObjects === undefined)
        options.mapsAsObjects = true;
      if (options.getStructures)
        options.getShared = options.getStructures;
      if (options.getShared && !options.structures)
        (options.structures = []).uninitialized = true;
      if (options.keyMap) {
        this.mapKey = new Map;
        for (let [k, v] of Object.entries(options.keyMap))
          this.mapKey.set(v, k);
      }
    }
    Object.assign(this, options);
  }
  decodeKey(key) {
    return this.keyMap ? this.mapKey.get(key) || key : key;
  }
  encodeKey(key) {
    return this.keyMap && this.keyMap.hasOwnProperty(key) ? this.keyMap[key] : key;
  }
  encodeKeys(rec) {
    if (!this._keyMap)
      return rec;
    let map = new Map;
    for (let [k, v] of Object.entries(rec))
      map.set(this._keyMap.hasOwnProperty(k) ? this._keyMap[k] : k, v);
    return map;
  }
  decodeKeys(map) {
    if (!this._keyMap || map.constructor.name != "Map")
      return map;
    if (!this._mapKey) {
      this._mapKey = new Map;
      for (let [k, v] of Object.entries(this._keyMap))
        this._mapKey.set(v, k);
    }
    let res = {};
    map.forEach((v, k) => res[safeKey(this._mapKey.has(k) ? this._mapKey.get(k) : k)] = v);
    return res;
  }
  mapDecode(source, end) {
    let res = this.decode(source);
    if (this._keyMap) {
      switch (res.constructor.name) {
        case "Array":
          return res.map((r) => this.decodeKeys(r));
      }
    }
    return res;
  }
  decode(source, end) {
    if (src) {
      return saveState(() => {
        clearSource();
        return this ? this.decode(source, end) : Decoder.prototype.decode.call(defaultOptions, source, end);
      });
    }
    srcEnd = end > -1 ? end : source.length;
    position = 0;
    stringPosition = 0;
    srcStringEnd = 0;
    srcString = null;
    strings = EMPTY_ARRAY;
    bundledStrings = null;
    src = source;
    try {
      dataView = source.dataView || (source.dataView = new DataView(source.buffer, source.byteOffset, source.byteLength));
    } catch (error) {
      src = null;
      if (source instanceof Uint8Array)
        throw error;
      throw new Error("Source must be a Uint8Array or Buffer but was a " + (source && typeof source == "object" ? source.constructor.name : typeof source));
    }
    if (this instanceof Decoder) {
      currentDecoder = this;
      packedValues = this.sharedValues && (this.pack ? new Array(this.maxPrivatePackedValues || 16).concat(this.sharedValues) : this.sharedValues);
      if (this.structures) {
        currentStructures = this.structures;
        return checkedRead();
      } else if (!currentStructures || currentStructures.length > 0) {
        currentStructures = [];
      }
    } else {
      currentDecoder = defaultOptions;
      if (!currentStructures || currentStructures.length > 0)
        currentStructures = [];
      packedValues = null;
    }
    return checkedRead();
  }
  decodeMultiple(source, forEach) {
    let values, lastPosition = 0;
    try {
      let size = source.length;
      sequentialMode = true;
      let value = this ? this.decode(source, size) : defaultDecoder.decode(source, size);
      if (forEach) {
        if (forEach(value) === false) {
          return;
        }
        while (position < size) {
          lastPosition = position;
          if (forEach(checkedRead()) === false) {
            return;
          }
        }
      } else {
        values = [value];
        while (position < size) {
          lastPosition = position;
          values.push(checkedRead());
        }
        return values;
      }
    } catch (error) {
      error.lastPosition = lastPosition;
      error.values = values;
      throw error;
    } finally {
      sequentialMode = false;
      clearSource();
    }
  }
}
var validName = /^[a-zA-Z_$][a-zA-Z\d_$]*$/;
var readFixedString = readStringJS;
var fromCharCode = String.fromCharCode;
var f32Array = new Float32Array(1);
var u8Array = new Uint8Array(f32Array.buffer, 0, 4);
var keyCache = new Array(4096);

class Tag {
  constructor(value, tag) {
    this.value = value;
    this.tag = tag;
  }
}
currentExtensions[0] = (dateString) => {
  return new Date(dateString);
};
currentExtensions[1] = (epochSec) => {
  return new Date(Math.round(epochSec * 1000));
};
currentExtensions[2] = (buffer) => {
  let value = BigInt(0);
  for (let i = 0, l = buffer.byteLength;i < l; i++) {
    value = BigInt(buffer[i]) + value << BigInt(8);
  }
  return value;
};
currentExtensions[3] = (buffer) => {
  return BigInt(-1) - currentExtensions[2](buffer);
};
currentExtensions[4] = (fraction) => {
  return +(fraction[1] + "e" + fraction[0]);
};
currentExtensions[5] = (fraction) => {
  return fraction[1] * Math.exp(fraction[0] * Math.log(2));
};
var recordDefinition = (id, structure) => {
  id = id - 57344;
  let existingStructure = currentStructures[id];
  if (existingStructure && existingStructure.isShared) {
    (currentStructures.restoreStructures || (currentStructures.restoreStructures = []))[id] = existingStructure;
  }
  currentStructures[id] = structure;
  structure.read = createStructureReader(structure);
};
currentExtensions[LEGACY_RECORD_INLINE_ID] = (data) => {
  let length = data.length;
  let structure = data[1];
  recordDefinition(data[0], structure);
  let object = {};
  for (let i = 2;i < length; i++) {
    let key = structure[i - 2];
    object[safeKey(key)] = data[i];
  }
  return object;
};
currentExtensions[14] = (value) => {
  if (bundledStrings)
    return bundledStrings[0].slice(bundledStrings.position0, bundledStrings.position0 += value);
  return new Tag(value, 14);
};
currentExtensions[15] = (value) => {
  if (bundledStrings)
    return bundledStrings[1].slice(bundledStrings.position1, bundledStrings.position1 += value);
  return new Tag(value, 15);
};
var glbl = { Error, RegExp };
currentExtensions[27] = (data) => {
  return (glbl[data[0]] || Error)(data[1], data[2]);
};
var packedTable = (read2) => {
  if (src[position++] != 132) {
    let error = new Error("Packed values structure must be followed by a 4 element array");
    if (src.length < position)
      error.incomplete = true;
    throw error;
  }
  let newPackedValues = read2();
  if (!newPackedValues || !newPackedValues.length) {
    let error = new Error("Packed values structure must be followed by a 4 element array");
    error.incomplete = true;
    throw error;
  }
  packedValues = packedValues ? newPackedValues.concat(packedValues.slice(newPackedValues.length)) : newPackedValues;
  packedValues.prefixes = read2();
  packedValues.suffixes = read2();
  return read2();
};
packedTable.handlesRead = true;
currentExtensions[51] = packedTable;
currentExtensions[PACKED_REFERENCE_TAG_ID] = (data) => {
  if (!packedValues) {
    if (currentDecoder.getShared)
      loadShared();
    else
      return new Tag(data, PACKED_REFERENCE_TAG_ID);
  }
  if (typeof data == "number")
    return packedValues[16 + (data >= 0 ? 2 * data : -2 * data - 1)];
  let error = new Error("No support for non-integer packed references yet");
  if (data === undefined)
    error.incomplete = true;
  throw error;
};
currentExtensions[28] = (read2) => {
  if (!referenceMap) {
    referenceMap = new Map;
    referenceMap.id = 0;
  }
  let id = referenceMap.id++;
  let startingPosition = position;
  let token = src[position];
  let target;
  if (token >> 5 == 4)
    target = [];
  else
    target = {};
  let refEntry = { target };
  referenceMap.set(id, refEntry);
  let targetProperties = read2();
  if (refEntry.used) {
    if (Object.getPrototypeOf(target) !== Object.getPrototypeOf(targetProperties)) {
      position = startingPosition;
      target = targetProperties;
      referenceMap.set(id, { target });
      targetProperties = read2();
    }
    return Object.assign(target, targetProperties);
  }
  refEntry.target = targetProperties;
  return targetProperties;
};
currentExtensions[28].handlesRead = true;
currentExtensions[29] = (id) => {
  let refEntry = referenceMap.get(id);
  refEntry.used = true;
  return refEntry.target;
};
currentExtensions[258] = (array) => new Set(array);
(currentExtensions[259] = (read2) => {
  if (currentDecoder.mapsAsObjects) {
    currentDecoder.mapsAsObjects = false;
    restoreMapsAsObject = true;
  }
  return read2();
}).handlesRead = true;
var SHARED_DATA_TAG_ID = 1399353956;
currentExtensionRanges.push((tag, input) => {
  if (tag >= 225 && tag <= 255)
    return combine(getPackedValues().prefixes[tag - 224], input);
  if (tag >= 28704 && tag <= 32767)
    return combine(getPackedValues().prefixes[tag - 28672], input);
  if (tag >= 1879052288 && tag <= 2147483647)
    return combine(getPackedValues().prefixes[tag - 1879048192], input);
  if (tag >= 216 && tag <= 223)
    return combine(input, getPackedValues().suffixes[tag - 216]);
  if (tag >= 27647 && tag <= 28671)
    return combine(input, getPackedValues().suffixes[tag - 27639]);
  if (tag >= 1811940352 && tag <= 1879048191)
    return combine(input, getPackedValues().suffixes[tag - 1811939328]);
  if (tag == SHARED_DATA_TAG_ID) {
    return {
      packedValues,
      structures: currentStructures.slice(0),
      version: input
    };
  }
  if (tag == 55799)
    return input;
});
var isLittleEndianMachine = new Uint8Array(new Uint16Array([1]).buffer)[0] == 1;
var typedArrays = [
  Uint8Array,
  Uint8ClampedArray,
  Uint16Array,
  Uint32Array,
  typeof BigUint64Array == "undefined" ? { name: "BigUint64Array" } : BigUint64Array,
  Int8Array,
  Int16Array,
  Int32Array,
  typeof BigInt64Array == "undefined" ? { name: "BigInt64Array" } : BigInt64Array,
  Float32Array,
  Float64Array
];
var typedArrayTags = [64, 68, 69, 70, 71, 72, 77, 78, 79, 85, 86];
for (let i = 0;i < typedArrays.length; i++) {
  registerTypedArray(typedArrays[i], typedArrayTags[i]);
}
var mult10 = new Array(147);
for (let i = 0;i < 256; i++) {
  mult10[i] = +("1e" + Math.floor(45.15 - i * 0.30103));
}
var defaultDecoder = new Decoder({ useRecords: false });
var decode = defaultDecoder.decode;
var decodeMultiple = defaultDecoder.decodeMultiple;

// node_modules/cbor-x/encode.js
var writeEntityLength = function(length, majorValue) {
  if (length < 24)
    target[position2++] = majorValue | length;
  else if (length < 256) {
    target[position2++] = majorValue | 24;
    target[position2++] = length;
  } else if (length < 65536) {
    target[position2++] = majorValue | 25;
    target[position2++] = length >> 8;
    target[position2++] = length & 255;
  } else {
    target[position2++] = majorValue | 26;
    targetView.setUint32(position2, length);
    position2 += 4;
  }
};
var writeArrayHeader = function(length) {
  if (length < 24)
    target[position2++] = 128 | length;
  else if (length < 256) {
    target[position2++] = 152;
    target[position2++] = length;
  } else if (length < 65536) {
    target[position2++] = 153;
    target[position2++] = length >> 8;
    target[position2++] = length & 255;
  } else {
    target[position2++] = 154;
    targetView.setUint32(position2, length);
    position2 += 4;
  }
};
var isBlob = function(object) {
  if (object instanceof BlobConstructor)
    return true;
  let tag = object[Symbol.toStringTag];
  return tag === "Blob" || tag === "File";
};
var findRepetitiveStrings = function(value, packedValues2) {
  switch (typeof value) {
    case "string":
      if (value.length > 3) {
        if (packedValues2.objectMap[value] > -1 || packedValues2.values.length >= packedValues2.maxValues)
          return;
        let packedStatus = packedValues2.get(value);
        if (packedStatus) {
          if (++packedStatus.count == 2) {
            packedValues2.values.push(value);
          }
        } else {
          packedValues2.set(value, {
            count: 1
          });
          if (packedValues2.samplingPackedValues) {
            let status = packedValues2.samplingPackedValues.get(value);
            if (status)
              status.count++;
            else
              packedValues2.samplingPackedValues.set(value, {
                count: 1
              });
          }
        }
      }
      break;
    case "object":
      if (value) {
        if (value instanceof Array) {
          for (let i = 0, l = value.length;i < l; i++) {
            findRepetitiveStrings(value[i], packedValues2);
          }
        } else {
          let includeKeys = !packedValues2.encoder.useRecords;
          for (var key in value) {
            if (value.hasOwnProperty(key)) {
              if (includeKeys)
                findRepetitiveStrings(key, packedValues2);
              findRepetitiveStrings(value[key], packedValues2);
            }
          }
        }
      }
      break;
    case "function":
      console.log(value);
  }
};
var typedArrayEncoder = function(tag, size) {
  if (!isLittleEndianMachine2 && size > 1)
    tag -= 4;
  return {
    tag,
    encode: function writeExtBuffer(typedArray, encode) {
      let length = typedArray.byteLength;
      let offset = typedArray.byteOffset || 0;
      let buffer = typedArray.buffer || typedArray;
      encode(hasNodeBuffer ? Buffer2.from(buffer, offset, length) : new Uint8Array(buffer, offset, length));
    }
  };
};
var writeBuffer = function(buffer, makeRoom) {
  let length = buffer.byteLength;
  if (length < 24) {
    target[position2++] = 64 + length;
  } else if (length < 256) {
    target[position2++] = 88;
    target[position2++] = length;
  } else if (length < 65536) {
    target[position2++] = 89;
    target[position2++] = length >> 8;
    target[position2++] = length & 255;
  } else {
    target[position2++] = 90;
    targetView.setUint32(position2, length);
    position2 += 4;
  }
  if (position2 + length >= target.length) {
    makeRoom(position2 + length);
  }
  target.set(buffer.buffer ? buffer : new Uint8Array(buffer), position2);
  position2 += length;
};
var insertIds = function(serialized, idsToInsert) {
  let nextId;
  let distanceToMove = idsToInsert.length * 2;
  let lastEnd = serialized.length - distanceToMove;
  idsToInsert.sort((a, b) => a.offset > b.offset ? 1 : -1);
  for (let id = 0;id < idsToInsert.length; id++) {
    let referee = idsToInsert[id];
    referee.id = id;
    for (let position2 of referee.references) {
      serialized[position2++] = id >> 8;
      serialized[position2] = id & 255;
    }
  }
  while (nextId = idsToInsert.pop()) {
    let offset = nextId.offset;
    serialized.copyWithin(offset + distanceToMove, offset, lastEnd);
    distanceToMove -= 2;
    let position2 = offset + distanceToMove;
    serialized[position2++] = 216;
    serialized[position2++] = 28;
    lastEnd = offset;
  }
  return serialized;
};
var writeBundles = function(start, encode) {
  targetView.setUint32(bundledStrings2.position + start, position2 - bundledStrings2.position - start + 1);
  let writeStrings = bundledStrings2;
  bundledStrings2 = null;
  encode(writeStrings[0]);
  encode(writeStrings[1]);
};
var textEncoder2;
try {
  textEncoder2 = new TextEncoder;
} catch (error) {
}
var extensions;
var extensionClasses;
var Buffer2 = typeof globalThis === "object" && globalThis.Buffer;
var hasNodeBuffer = typeof Buffer2 !== "undefined";
var ByteArrayAllocate = hasNodeBuffer ? Buffer2.allocUnsafeSlow : Uint8Array;
var ByteArray = hasNodeBuffer ? Buffer2 : Uint8Array;
var MAX_STRUCTURES = 256;
var MAX_BUFFER_SIZE = hasNodeBuffer ? 4294967296 : 2144337920;
var throwOnIterable;
var target;
var targetView;
var position2 = 0;
var safeEnd;
var bundledStrings2 = null;
var MAX_BUNDLE_SIZE = 61440;
var hasNonLatin = /[\u0080-\uFFFF]/;
var RECORD_SYMBOL = Symbol("record-id");

class Encoder extends Decoder {
  constructor(options) {
    super(options);
    this.offset = 0;
    let typeBuffer;
    let start;
    let sharedStructures;
    let hasSharedUpdate;
    let structures;
    let referenceMap2;
    options = options || {};
    let encodeUtf8 = ByteArray.prototype.utf8Write ? function(string, position3, maxBytes) {
      return target.utf8Write(string, position3, maxBytes);
    } : textEncoder2 && textEncoder2.encodeInto ? function(string, position3) {
      return textEncoder2.encodeInto(string, target.subarray(position3)).written;
    } : false;
    let encoder = this;
    let hasSharedStructures = options.structures || options.saveStructures;
    let maxSharedStructures = options.maxSharedStructures;
    if (maxSharedStructures == null)
      maxSharedStructures = hasSharedStructures ? 128 : 0;
    if (maxSharedStructures > 8190)
      throw new Error("Maximum maxSharedStructure is 8190");
    let isSequential = options.sequential;
    if (isSequential) {
      maxSharedStructures = 0;
    }
    if (!this.structures)
      this.structures = [];
    if (this.saveStructures)
      this.saveShared = this.saveStructures;
    let samplingPackedValues, packedObjectMap2, sharedValues = options.sharedValues;
    let sharedPackedObjectMap2;
    if (sharedValues) {
      sharedPackedObjectMap2 = Object.create(null);
      for (let i = 0, l = sharedValues.length;i < l; i++) {
        sharedPackedObjectMap2[sharedValues[i]] = i;
      }
    }
    let recordIdsToRemove = [];
    let transitionsCount = 0;
    let serializationsSinceTransitionRebuild = 0;
    this.mapEncode = function(value, encodeOptions) {
      if (this._keyMap && !this._mapped) {
        switch (value.constructor.name) {
          case "Array":
            value = value.map((r) => this.encodeKeys(r));
            break;
        }
      }
      return this.encode(value, encodeOptions);
    };
    this.encode = function(value, encodeOptions) {
      if (!target) {
        target = new ByteArrayAllocate(8192);
        targetView = new DataView(target.buffer, 0, 8192);
        position2 = 0;
      }
      safeEnd = target.length - 10;
      if (safeEnd - position2 < 2048) {
        target = new ByteArrayAllocate(target.length);
        targetView = new DataView(target.buffer, 0, target.length);
        safeEnd = target.length - 10;
        position2 = 0;
      } else if (encodeOptions === REUSE_BUFFER_MODE)
        position2 = position2 + 7 & 2147483640;
      start = position2;
      if (encoder.useSelfDescribedHeader) {
        targetView.setUint32(position2, 3654940416);
        position2 += 3;
      }
      referenceMap2 = encoder.structuredClone ? new Map : null;
      if (encoder.bundleStrings && typeof value !== "string") {
        bundledStrings2 = [];
        bundledStrings2.size = Infinity;
      } else
        bundledStrings2 = null;
      sharedStructures = encoder.structures;
      if (sharedStructures) {
        if (sharedStructures.uninitialized) {
          let sharedData = encoder.getShared() || {};
          encoder.structures = sharedStructures = sharedData.structures || [];
          encoder.sharedVersion = sharedData.version;
          let sharedValues2 = encoder.sharedValues = sharedData.packedValues;
          if (sharedValues2) {
            sharedPackedObjectMap2 = {};
            for (let i = 0, l = sharedValues2.length;i < l; i++)
              sharedPackedObjectMap2[sharedValues2[i]] = i;
          }
        }
        let sharedStructuresLength = sharedStructures.length;
        if (sharedStructuresLength > maxSharedStructures && !isSequential)
          sharedStructuresLength = maxSharedStructures;
        if (!sharedStructures.transitions) {
          sharedStructures.transitions = Object.create(null);
          for (let i = 0;i < sharedStructuresLength; i++) {
            let keys = sharedStructures[i];
            if (!keys)
              continue;
            let nextTransition, transition = sharedStructures.transitions;
            for (let j = 0, l = keys.length;j < l; j++) {
              if (transition[RECORD_SYMBOL] === undefined)
                transition[RECORD_SYMBOL] = i;
              let key = keys[j];
              nextTransition = transition[key];
              if (!nextTransition) {
                nextTransition = transition[key] = Object.create(null);
              }
              transition = nextTransition;
            }
            transition[RECORD_SYMBOL] = i | 1048576;
          }
        }
        if (!isSequential)
          sharedStructures.nextId = sharedStructuresLength;
      }
      if (hasSharedUpdate)
        hasSharedUpdate = false;
      structures = sharedStructures || [];
      packedObjectMap2 = sharedPackedObjectMap2;
      if (options.pack) {
        let packedValues2 = new Map;
        packedValues2.values = [];
        packedValues2.encoder = encoder;
        packedValues2.maxValues = options.maxPrivatePackedValues || (sharedPackedObjectMap2 ? 16 : Infinity);
        packedValues2.objectMap = sharedPackedObjectMap2 || false;
        packedValues2.samplingPackedValues = samplingPackedValues;
        findRepetitiveStrings(value, packedValues2);
        if (packedValues2.values.length > 0) {
          target[position2++] = 216;
          target[position2++] = 51;
          writeArrayHeader(4);
          let valuesArray = packedValues2.values;
          encode(valuesArray);
          writeArrayHeader(0);
          writeArrayHeader(0);
          packedObjectMap2 = Object.create(sharedPackedObjectMap2 || null);
          for (let i = 0, l = valuesArray.length;i < l; i++) {
            packedObjectMap2[valuesArray[i]] = i;
          }
        }
      }
      throwOnIterable = encodeOptions & THROW_ON_ITERABLE;
      try {
        if (throwOnIterable)
          return;
        encode(value);
        if (bundledStrings2) {
          writeBundles(start, encode);
        }
        encoder.offset = position2;
        if (referenceMap2 && referenceMap2.idsToInsert) {
          position2 += referenceMap2.idsToInsert.length * 2;
          if (position2 > safeEnd)
            makeRoom(position2);
          encoder.offset = position2;
          let serialized = insertIds(target.subarray(start, position2), referenceMap2.idsToInsert);
          referenceMap2 = null;
          return serialized;
        }
        if (encodeOptions & REUSE_BUFFER_MODE) {
          target.start = start;
          target.end = position2;
          return target;
        }
        return target.subarray(start, position2);
      } finally {
        if (sharedStructures) {
          if (serializationsSinceTransitionRebuild < 10)
            serializationsSinceTransitionRebuild++;
          if (sharedStructures.length > maxSharedStructures)
            sharedStructures.length = maxSharedStructures;
          if (transitionsCount > 1e4) {
            sharedStructures.transitions = null;
            serializationsSinceTransitionRebuild = 0;
            transitionsCount = 0;
            if (recordIdsToRemove.length > 0)
              recordIdsToRemove = [];
          } else if (recordIdsToRemove.length > 0 && !isSequential) {
            for (let i = 0, l = recordIdsToRemove.length;i < l; i++) {
              recordIdsToRemove[i][RECORD_SYMBOL] = undefined;
            }
            recordIdsToRemove = [];
          }
        }
        if (hasSharedUpdate && encoder.saveShared) {
          if (encoder.structures.length > maxSharedStructures) {
            encoder.structures = encoder.structures.slice(0, maxSharedStructures);
          }
          let returnBuffer = target.subarray(start, position2);
          if (encoder.updateSharedData() === false)
            return encoder.encode(value);
          return returnBuffer;
        }
        if (encodeOptions & RESET_BUFFER_MODE)
          position2 = start;
      }
    };
    this.findCommonStringsToPack = () => {
      samplingPackedValues = new Map;
      if (!sharedPackedObjectMap2)
        sharedPackedObjectMap2 = Object.create(null);
      return (options2) => {
        let threshold = options2 && options2.threshold || 4;
        let position3 = this.pack ? options2.maxPrivatePackedValues || 16 : 0;
        if (!sharedValues)
          sharedValues = this.sharedValues = [];
        for (let [key, status] of samplingPackedValues) {
          if (status.count > threshold) {
            sharedPackedObjectMap2[key] = position3++;
            sharedValues.push(key);
            hasSharedUpdate = true;
          }
        }
        while (this.saveShared && this.updateSharedData() === false) {
        }
        samplingPackedValues = null;
      };
    };
    const encode = (value) => {
      if (position2 > safeEnd)
        target = makeRoom(position2);
      var type = typeof value;
      var length;
      if (type === "string") {
        if (packedObjectMap2) {
          let packedPosition = packedObjectMap2[value];
          if (packedPosition >= 0) {
            if (packedPosition < 16)
              target[position2++] = packedPosition + 224;
            else {
              target[position2++] = 198;
              if (packedPosition & 1)
                encode(15 - packedPosition >> 1);
              else
                encode(packedPosition - 16 >> 1);
            }
            return;
          } else if (samplingPackedValues && !options.pack) {
            let status = samplingPackedValues.get(value);
            if (status)
              status.count++;
            else
              samplingPackedValues.set(value, {
                count: 1
              });
          }
        }
        let strLength = value.length;
        if (bundledStrings2 && strLength >= 4 && strLength < 1024) {
          if ((bundledStrings2.size += strLength) > MAX_BUNDLE_SIZE) {
            let extStart;
            let maxBytes2 = (bundledStrings2[0] ? bundledStrings2[0].length * 3 + bundledStrings2[1].length : 0) + 10;
            if (position2 + maxBytes2 > safeEnd)
              target = makeRoom(position2 + maxBytes2);
            target[position2++] = 217;
            target[position2++] = 223;
            target[position2++] = 249;
            target[position2++] = bundledStrings2.position ? 132 : 130;
            target[position2++] = 26;
            extStart = position2 - start;
            position2 += 4;
            if (bundledStrings2.position) {
              writeBundles(start, encode);
            }
            bundledStrings2 = ["", ""];
            bundledStrings2.size = 0;
            bundledStrings2.position = extStart;
          }
          let twoByte = hasNonLatin.test(value);
          bundledStrings2[twoByte ? 0 : 1] += value;
          target[position2++] = twoByte ? 206 : 207;
          encode(strLength);
          return;
        }
        let headerSize;
        if (strLength < 32) {
          headerSize = 1;
        } else if (strLength < 256) {
          headerSize = 2;
        } else if (strLength < 65536) {
          headerSize = 3;
        } else {
          headerSize = 5;
        }
        let maxBytes = strLength * 3;
        if (position2 + maxBytes > safeEnd)
          target = makeRoom(position2 + maxBytes);
        if (strLength < 64 || !encodeUtf8) {
          let i, c1, c2, strPosition = position2 + headerSize;
          for (i = 0;i < strLength; i++) {
            c1 = value.charCodeAt(i);
            if (c1 < 128) {
              target[strPosition++] = c1;
            } else if (c1 < 2048) {
              target[strPosition++] = c1 >> 6 | 192;
              target[strPosition++] = c1 & 63 | 128;
            } else if ((c1 & 64512) === 55296 && ((c2 = value.charCodeAt(i + 1)) & 64512) === 56320) {
              c1 = 65536 + ((c1 & 1023) << 10) + (c2 & 1023);
              i++;
              target[strPosition++] = c1 >> 18 | 240;
              target[strPosition++] = c1 >> 12 & 63 | 128;
              target[strPosition++] = c1 >> 6 & 63 | 128;
              target[strPosition++] = c1 & 63 | 128;
            } else {
              target[strPosition++] = c1 >> 12 | 224;
              target[strPosition++] = c1 >> 6 & 63 | 128;
              target[strPosition++] = c1 & 63 | 128;
            }
          }
          length = strPosition - position2 - headerSize;
        } else {
          length = encodeUtf8(value, position2 + headerSize, maxBytes);
        }
        if (length < 24) {
          target[position2++] = 96 | length;
        } else if (length < 256) {
          if (headerSize < 2) {
            target.copyWithin(position2 + 2, position2 + 1, position2 + 1 + length);
          }
          target[position2++] = 120;
          target[position2++] = length;
        } else if (length < 65536) {
          if (headerSize < 3) {
            target.copyWithin(position2 + 3, position2 + 2, position2 + 2 + length);
          }
          target[position2++] = 121;
          target[position2++] = length >> 8;
          target[position2++] = length & 255;
        } else {
          if (headerSize < 5) {
            target.copyWithin(position2 + 5, position2 + 3, position2 + 3 + length);
          }
          target[position2++] = 122;
          targetView.setUint32(position2, length);
          position2 += 4;
        }
        position2 += length;
      } else if (type === "number") {
        if (!this.alwaysUseFloat && value >>> 0 === value) {
          if (value < 24) {
            target[position2++] = value;
          } else if (value < 256) {
            target[position2++] = 24;
            target[position2++] = value;
          } else if (value < 65536) {
            target[position2++] = 25;
            target[position2++] = value >> 8;
            target[position2++] = value & 255;
          } else {
            target[position2++] = 26;
            targetView.setUint32(position2, value);
            position2 += 4;
          }
        } else if (!this.alwaysUseFloat && value >> 0 === value) {
          if (value >= -24) {
            target[position2++] = 31 - value;
          } else if (value >= -256) {
            target[position2++] = 56;
            target[position2++] = ~value;
          } else if (value >= -65536) {
            target[position2++] = 57;
            targetView.setUint16(position2, ~value);
            position2 += 2;
          } else {
            target[position2++] = 58;
            targetView.setUint32(position2, ~value);
            position2 += 4;
          }
        } else {
          let useFloat32;
          if ((useFloat32 = this.useFloat32) > 0 && value < 4294967296 && value >= -2147483648) {
            target[position2++] = 250;
            targetView.setFloat32(position2, value);
            let xShifted;
            if (useFloat32 < 4 || (xShifted = value * mult10[(target[position2] & 127) << 1 | target[position2 + 1] >> 7]) >> 0 === xShifted) {
              position2 += 4;
              return;
            } else
              position2--;
          }
          target[position2++] = 251;
          targetView.setFloat64(position2, value);
          position2 += 8;
        }
      } else if (type === "object") {
        if (!value)
          target[position2++] = 246;
        else {
          if (referenceMap2) {
            let referee = referenceMap2.get(value);
            if (referee) {
              target[position2++] = 216;
              target[position2++] = 29;
              target[position2++] = 25;
              if (!referee.references) {
                let idsToInsert = referenceMap2.idsToInsert || (referenceMap2.idsToInsert = []);
                referee.references = [];
                idsToInsert.push(referee);
              }
              referee.references.push(position2 - start);
              position2 += 2;
              return;
            } else
              referenceMap2.set(value, { offset: position2 - start });
          }
          let constructor = value.constructor;
          if (constructor === Object) {
            writeObject(value);
          } else if (constructor === Array) {
            length = value.length;
            if (length < 24) {
              target[position2++] = 128 | length;
            } else {
              writeArrayHeader(length);
            }
            for (let i = 0;i < length; i++) {
              encode(value[i]);
            }
          } else if (constructor === Map) {
            if (this.mapsAsObjects ? this.useTag259ForMaps !== false : this.useTag259ForMaps) {
              target[position2++] = 217;
              target[position2++] = 1;
              target[position2++] = 3;
            }
            length = value.size;
            if (length < 24) {
              target[position2++] = 160 | length;
            } else if (length < 256) {
              target[position2++] = 184;
              target[position2++] = length;
            } else if (length < 65536) {
              target[position2++] = 185;
              target[position2++] = length >> 8;
              target[position2++] = length & 255;
            } else {
              target[position2++] = 186;
              targetView.setUint32(position2, length);
              position2 += 4;
            }
            if (encoder.keyMap) {
              for (let [key, entryValue] of value) {
                encode(encoder.encodeKey(key));
                encode(entryValue);
              }
            } else {
              for (let [key, entryValue] of value) {
                encode(key);
                encode(entryValue);
              }
            }
          } else {
            for (let i = 0, l = extensions.length;i < l; i++) {
              let extensionClass = extensionClasses[i];
              if (value instanceof extensionClass) {
                let extension = extensions[i];
                let tag = extension.tag;
                if (tag == undefined)
                  tag = extension.getTag && extension.getTag.call(this, value);
                if (tag < 24) {
                  target[position2++] = 192 | tag;
                } else if (tag < 256) {
                  target[position2++] = 216;
                  target[position2++] = tag;
                } else if (tag < 65536) {
                  target[position2++] = 217;
                  target[position2++] = tag >> 8;
                  target[position2++] = tag & 255;
                } else if (tag > -1) {
                  target[position2++] = 218;
                  targetView.setUint32(position2, tag);
                  position2 += 4;
                }
                extension.encode.call(this, value, encode, makeRoom);
                return;
              }
            }
            if (value[Symbol.iterator]) {
              if (throwOnIterable) {
                let error = new Error("Iterable should be serialized as iterator");
                error.iteratorNotHandled = true;
                throw error;
              }
              target[position2++] = 159;
              for (let entry of value) {
                encode(entry);
              }
              target[position2++] = 255;
              return;
            }
            if (value[Symbol.asyncIterator] || isBlob(value)) {
              let error = new Error("Iterable/blob should be serialized as iterator");
              error.iteratorNotHandled = true;
              throw error;
            }
            if (this.useToJSON && value.toJSON) {
              const json = value.toJSON();
              if (json !== value)
                return encode(json);
            }
            writeObject(value);
          }
        }
      } else if (type === "boolean") {
        target[position2++] = value ? 245 : 244;
      } else if (type === "bigint") {
        if (value < BigInt(1) << BigInt(64) && value >= 0) {
          target[position2++] = 27;
          targetView.setBigUint64(position2, value);
        } else if (value > -(BigInt(1) << BigInt(64)) && value < 0) {
          target[position2++] = 59;
          targetView.setBigUint64(position2, -value - BigInt(1));
        } else {
          if (this.largeBigIntToFloat) {
            target[position2++] = 251;
            targetView.setFloat64(position2, Number(value));
          } else {
            throw new RangeError(value + " was too large to fit in CBOR 64-bit integer format, set largeBigIntToFloat to convert to float-64");
          }
        }
        position2 += 8;
      } else if (type === "undefined") {
        target[position2++] = 247;
      } else {
        throw new Error("Unknown type: " + type);
      }
    };
    const writeObject = this.useRecords === false ? this.variableMapSize ? (object) => {
      let keys = Object.keys(object);
      let vals = Object.values(object);
      let length = keys.length;
      if (length < 24) {
        target[position2++] = 160 | length;
      } else if (length < 256) {
        target[position2++] = 184;
        target[position2++] = length;
      } else if (length < 65536) {
        target[position2++] = 185;
        target[position2++] = length >> 8;
        target[position2++] = length & 255;
      } else {
        target[position2++] = 186;
        targetView.setUint32(position2, length);
        position2 += 4;
      }
      let key;
      if (encoder.keyMap) {
        for (let i = 0;i < length; i++) {
          encode(encoder.encodeKey(keys[i]));
          encode(vals[i]);
        }
      } else {
        for (let i = 0;i < length; i++) {
          encode(keys[i]);
          encode(vals[i]);
        }
      }
    } : (object) => {
      target[position2++] = 185;
      let objectOffset = position2 - start;
      position2 += 2;
      let size = 0;
      if (encoder.keyMap) {
        for (let key in object)
          if (typeof object.hasOwnProperty !== "function" || object.hasOwnProperty(key)) {
            encode(encoder.encodeKey(key));
            encode(object[key]);
            size++;
          }
      } else {
        for (let key in object)
          if (typeof object.hasOwnProperty !== "function" || object.hasOwnProperty(key)) {
            encode(key);
            encode(object[key]);
            size++;
          }
      }
      target[objectOffset++ + start] = size >> 8;
      target[objectOffset + start] = size & 255;
    } : (object, skipValues) => {
      let nextTransition, transition = structures.transitions || (structures.transitions = Object.create(null));
      let newTransitions = 0;
      let length = 0;
      let parentRecordId;
      let keys;
      if (this.keyMap) {
        keys = Object.keys(object).map((k) => this.encodeKey(k));
        length = keys.length;
        for (let i = 0;i < length; i++) {
          let key = keys[i];
          nextTransition = transition[key];
          if (!nextTransition) {
            nextTransition = transition[key] = Object.create(null);
            newTransitions++;
          }
          transition = nextTransition;
        }
      } else {
        for (let key in object)
          if (typeof object.hasOwnProperty !== "function" || object.hasOwnProperty(key)) {
            nextTransition = transition[key];
            if (!nextTransition) {
              if (transition[RECORD_SYMBOL] & 1048576) {
                parentRecordId = transition[RECORD_SYMBOL] & 65535;
              }
              nextTransition = transition[key] = Object.create(null);
              newTransitions++;
            }
            transition = nextTransition;
            length++;
          }
      }
      let recordId = transition[RECORD_SYMBOL];
      if (recordId !== undefined) {
        recordId &= 65535;
        target[position2++] = 217;
        target[position2++] = recordId >> 8 | 224;
        target[position2++] = recordId & 255;
      } else {
        if (!keys)
          keys = transition.__keys__ || (transition.__keys__ = Object.keys(object));
        if (parentRecordId === undefined) {
          recordId = structures.nextId++;
          if (!recordId) {
            recordId = 0;
            structures.nextId = 1;
          }
          if (recordId >= MAX_STRUCTURES) {
            structures.nextId = (recordId = maxSharedStructures) + 1;
          }
        } else {
          recordId = parentRecordId;
        }
        structures[recordId] = keys;
        if (recordId < maxSharedStructures) {
          target[position2++] = 217;
          target[position2++] = recordId >> 8 | 224;
          target[position2++] = recordId & 255;
          transition = structures.transitions;
          for (let i = 0;i < length; i++) {
            if (transition[RECORD_SYMBOL] === undefined || transition[RECORD_SYMBOL] & 1048576)
              transition[RECORD_SYMBOL] = recordId;
            transition = transition[keys[i]];
          }
          transition[RECORD_SYMBOL] = recordId | 1048576;
          hasSharedUpdate = true;
        } else {
          transition[RECORD_SYMBOL] = recordId;
          targetView.setUint32(position2, 3655335680);
          position2 += 3;
          if (newTransitions)
            transitionsCount += serializationsSinceTransitionRebuild * newTransitions;
          if (recordIdsToRemove.length >= MAX_STRUCTURES - maxSharedStructures)
            recordIdsToRemove.shift()[RECORD_SYMBOL] = undefined;
          recordIdsToRemove.push(transition);
          writeArrayHeader(length + 2);
          encode(57344 + recordId);
          encode(keys);
          if (skipValues)
            return;
          for (let key in object)
            if (typeof object.hasOwnProperty !== "function" || object.hasOwnProperty(key))
              encode(object[key]);
          return;
        }
      }
      if (length < 24) {
        target[position2++] = 128 | length;
      } else {
        writeArrayHeader(length);
      }
      if (skipValues)
        return;
      for (let key in object)
        if (typeof object.hasOwnProperty !== "function" || object.hasOwnProperty(key))
          encode(object[key]);
    };
    const makeRoom = (end) => {
      let newSize;
      if (end > 16777216) {
        if (end - start > MAX_BUFFER_SIZE)
          throw new Error("Encoded buffer would be larger than maximum buffer size");
        newSize = Math.min(MAX_BUFFER_SIZE, Math.round(Math.max((end - start) * (end > 67108864 ? 1.25 : 2), 4194304) / 4096) * 4096);
      } else
        newSize = (Math.max(end - start << 2, target.length - 1) >> 12) + 1 << 12;
      let newBuffer = new ByteArrayAllocate(newSize);
      targetView = new DataView(newBuffer.buffer, 0, newSize);
      if (target.copy)
        target.copy(newBuffer, 0, start, end);
      else
        newBuffer.set(target.slice(start, end));
      position2 -= start;
      start = 0;
      safeEnd = newBuffer.length - 10;
      return target = newBuffer;
    };
    let chunkThreshold = 100;
    let continuedChunkThreshold = 1000;
    this.encodeAsIterable = function(value, options2) {
      return startEncoding(value, options2, encodeObjectAsIterable);
    };
    this.encodeAsAsyncIterable = function(value, options2) {
      return startEncoding(value, options2, encodeObjectAsAsyncIterable);
    };
    function* encodeObjectAsIterable(object, iterateProperties, finalIterable) {
      let constructor = object.constructor;
      if (constructor === Object) {
        let useRecords = encoder.useRecords !== false;
        if (useRecords)
          writeObject(object, true);
        else
          writeEntityLength(Object.keys(object).length, 160);
        for (let key in object) {
          let value = object[key];
          if (!useRecords)
            encode(key);
          if (value && typeof value === "object") {
            if (iterateProperties[key])
              yield* encodeObjectAsIterable(value, iterateProperties[key]);
            else
              yield* tryEncode(value, iterateProperties, key);
          } else
            encode(value);
        }
      } else if (constructor === Array) {
        let length = object.length;
        writeArrayHeader(length);
        for (let i = 0;i < length; i++) {
          let value = object[i];
          if (value && (typeof value === "object" || position2 - start > chunkThreshold)) {
            if (iterateProperties.element)
              yield* encodeObjectAsIterable(value, iterateProperties.element);
            else
              yield* tryEncode(value, iterateProperties, "element");
          } else
            encode(value);
        }
      } else if (object[Symbol.iterator]) {
        target[position2++] = 159;
        for (let value of object) {
          if (value && (typeof value === "object" || position2 - start > chunkThreshold)) {
            if (iterateProperties.element)
              yield* encodeObjectAsIterable(value, iterateProperties.element);
            else
              yield* tryEncode(value, iterateProperties, "element");
          } else
            encode(value);
        }
        target[position2++] = 255;
      } else if (isBlob(object)) {
        writeEntityLength(object.size, 64);
        yield target.subarray(start, position2);
        yield object;
        restartEncoding();
      } else if (object[Symbol.asyncIterator]) {
        target[position2++] = 159;
        yield target.subarray(start, position2);
        yield object;
        restartEncoding();
        target[position2++] = 255;
      } else {
        encode(object);
      }
      if (finalIterable && position2 > start)
        yield target.subarray(start, position2);
      else if (position2 - start > chunkThreshold) {
        yield target.subarray(start, position2);
        restartEncoding();
      }
    }
    function* tryEncode(value, iterateProperties, key) {
      let restart = position2 - start;
      try {
        encode(value);
        if (position2 - start > chunkThreshold) {
          yield target.subarray(start, position2);
          restartEncoding();
        }
      } catch (error) {
        if (error.iteratorNotHandled) {
          iterateProperties[key] = {};
          position2 = start + restart;
          yield* encodeObjectAsIterable.call(this, value, iterateProperties[key]);
        } else
          throw error;
      }
    }
    function restartEncoding() {
      chunkThreshold = continuedChunkThreshold;
      encoder.encode(null, THROW_ON_ITERABLE);
    }
    function startEncoding(value, options2, encodeIterable) {
      if (options2 && options2.chunkThreshold)
        chunkThreshold = continuedChunkThreshold = options2.chunkThreshold;
      else
        chunkThreshold = 100;
      if (value && typeof value === "object") {
        encoder.encode(null, THROW_ON_ITERABLE);
        return encodeIterable(value, encoder.iterateProperties || (encoder.iterateProperties = {}), true);
      }
      return [encoder.encode(value)];
    }
    async function* encodeObjectAsAsyncIterable(value, iterateProperties) {
      for (let encodedValue of encodeObjectAsIterable(value, iterateProperties, true)) {
        let constructor = encodedValue.constructor;
        if (constructor === ByteArray || constructor === Uint8Array)
          yield encodedValue;
        else if (isBlob(encodedValue)) {
          let reader = encodedValue.stream().getReader();
          let next;
          while (!(next = await reader.read()).done) {
            yield next.value;
          }
        } else if (encodedValue[Symbol.asyncIterator]) {
          for await (let asyncValue of encodedValue) {
            restartEncoding();
            if (asyncValue)
              yield* encodeObjectAsAsyncIterable(asyncValue, iterateProperties.async || (iterateProperties.async = {}));
            else
              yield encoder.encode(asyncValue);
          }
        } else {
          yield encodedValue;
        }
      }
    }
  }
  useBuffer(buffer) {
    target = buffer;
    targetView = new DataView(target.buffer, target.byteOffset, target.byteLength);
    position2 = 0;
  }
  clearSharedData() {
    if (this.structures)
      this.structures = [];
    if (this.sharedValues)
      this.sharedValues = undefined;
  }
  updateSharedData() {
    let lastVersion = this.sharedVersion || 0;
    this.sharedVersion = lastVersion + 1;
    let structuresCopy = this.structures.slice(0);
    let sharedData = new SharedData(structuresCopy, this.sharedValues, this.sharedVersion);
    let saveResults = this.saveShared(sharedData, (existingShared) => (existingShared && existingShared.version || 0) == lastVersion);
    if (saveResults === false) {
      sharedData = this.getShared() || {};
      this.structures = sharedData.structures || [];
      this.sharedValues = sharedData.packedValues;
      this.sharedVersion = sharedData.version;
      this.structures.nextId = this.structures.length;
    } else {
      structuresCopy.forEach((structure, i) => this.structures[i] = structure);
    }
    return saveResults;
  }
}

class SharedData {
  constructor(structures, values, version) {
    this.structures = structures;
    this.packedValues = values;
    this.version = version;
  }
}
var BlobConstructor = typeof Blob === "undefined" ? function() {
} : Blob;
var isLittleEndianMachine2 = new Uint8Array(new Uint16Array([1]).buffer)[0] == 1;
extensionClasses = [
  Date,
  Set,
  Error,
  RegExp,
  Tag,
  ArrayBuffer,
  Uint8Array,
  Uint8ClampedArray,
  Uint16Array,
  Uint32Array,
  typeof BigUint64Array == "undefined" ? function() {
  } : BigUint64Array,
  Int8Array,
  Int16Array,
  Int32Array,
  typeof BigInt64Array == "undefined" ? function() {
  } : BigInt64Array,
  Float32Array,
  Float64Array,
  SharedData
];
extensions = [
  {
    tag: 1,
    encode(date, encode) {
      let seconds = date.getTime() / 1000;
      if ((this.useTimestamp32 || date.getMilliseconds() === 0) && seconds >= 0 && seconds < 4294967296) {
        target[position2++] = 26;
        targetView.setUint32(position2, seconds);
        position2 += 4;
      } else {
        target[position2++] = 251;
        targetView.setFloat64(position2, seconds);
        position2 += 8;
      }
    }
  },
  {
    tag: 258,
    encode(set, encode) {
      let array = Array.from(set);
      encode(array);
    }
  },
  {
    tag: 27,
    encode(error, encode) {
      encode([error.name, error.message]);
    }
  },
  {
    tag: 27,
    encode(regex, encode) {
      encode(["RegExp", regex.source, regex.flags]);
    }
  },
  {
    getTag(tag) {
      return tag.tag;
    },
    encode(tag, encode) {
      encode(tag.value);
    }
  },
  {
    encode(arrayBuffer, encode, makeRoom) {
      writeBuffer(arrayBuffer, makeRoom);
    }
  },
  {
    getTag(typedArray) {
      if (typedArray.constructor === Uint8Array) {
        if (this.tagUint8Array || hasNodeBuffer && this.tagUint8Array !== false)
          return 64;
      }
    },
    encode(typedArray, encode, makeRoom) {
      writeBuffer(typedArray, makeRoom);
    }
  },
  typedArrayEncoder(68, 1),
  typedArrayEncoder(69, 2),
  typedArrayEncoder(70, 4),
  typedArrayEncoder(71, 8),
  typedArrayEncoder(72, 1),
  typedArrayEncoder(77, 2),
  typedArrayEncoder(78, 4),
  typedArrayEncoder(79, 8),
  typedArrayEncoder(85, 4),
  typedArrayEncoder(86, 8),
  {
    encode(sharedData, encode) {
      let packedValues2 = sharedData.packedValues || [];
      let sharedStructures = sharedData.structures || [];
      if (packedValues2.values.length > 0) {
        target[position2++] = 216;
        target[position2++] = 51;
        writeArrayHeader(4);
        let valuesArray = packedValues2.values;
        encode(valuesArray);
        writeArrayHeader(0);
        writeArrayHeader(0);
        packedObjectMap = Object.create(sharedPackedObjectMap || null);
        for (let i = 0, l = valuesArray.length;i < l; i++) {
          packedObjectMap[valuesArray[i]] = i;
        }
      }
      if (sharedStructures) {
        targetView.setUint32(position2, 3655335424);
        position2 += 3;
        let definitions = sharedStructures.slice(0);
        definitions.unshift(57344);
        definitions.push(new Tag(sharedData.version, 1399353956));
        encode(definitions);
      } else
        encode(new Tag(sharedData.version, 1399353956));
    }
  }
];
var defaultEncoder = new Encoder({ useRecords: false });
var encode = defaultEncoder.encode;
var encodeAsIterable = defaultEncoder.encodeAsIterable;
var encodeAsAsyncIterable = defaultEncoder.encodeAsAsyncIterable;
var REUSE_BUFFER_MODE = 512;
var RESET_BUFFER_MODE = 1024;
var THROW_ON_ITERABLE = 2048;
// src/core/ipc/ipc-message/stream/IpcStreamAbort.ts
var ipcStreamAbort = (stream_id) => ({ ...ipcMessageBase(IPC_MESSAGE_TYPE.STREAM_ABORT), stream_id });

// src/core/ipc/ipc-message/stream/IpcStreamPulling.ts
var ipcStreamPulling = (stream_id, bandwidth) => ({
  ...ipcMessageBase(IPC_MESSAGE_TYPE.STREAM_PULLING),
  stream_id,
  bandwidth: bandwidth ?? 0
});

// src/core/ipc/ipc-message/stream/IpcBodyReceiver.ts
class IpcBodyReceiver extends IpcBody {
  metaBody;
  static from(metaBody, ipc2) {
    return IpcBodyReceiver.CACHE.streamId_ipcBodySender_Map.get(metaBody.streamId) ?? new IpcBodyReceiver(metaBody, ipc2);
  }
  constructor(metaBody, ipc2) {
    super();
    this.metaBody = metaBody;
    if (metaBody.type_isStream) {
      const streamId = metaBody.streamId;
      if (IpcBodyReceiver.CACHE.streamId_receiverIpc_Map.has(streamId) === false) {
        ipc2.onClosed(() => {
          IpcBodyReceiver.CACHE.streamId_receiverIpc_Map.delete(streamId);
        });
        IpcBodyReceiver.CACHE.streamId_receiverIpc_Map.set(streamId, ipc2);
        metaBody.receiverUid = ipc2.pool.poolId;
      }
      const receiver = IpcBodyReceiver.CACHE.streamId_receiverIpc_Map.get(streamId);
      if (receiver === undefined) {
        throw new Error(`no found ipc by streamId:${streamId}`);
      }
      ipc2 = receiver;
      this._bodyHub = new BodyHub($metaToStream(this.metaBody, ipc2));
    } else
      switch (metaBody.type_encoding) {
        case IPC_DATA_ENCODING.UTF8:
          this._bodyHub = new BodyHub(metaBody.data);
          break;
        case IPC_DATA_ENCODING.BASE64:
          this._bodyHub = new BodyHub(simpleEncoder(metaBody.data, "base64"));
          break;
        case IPC_DATA_ENCODING.BINARY:
          this._bodyHub = new BodyHub(metaBody.data);
          break;
        default:
          throw new Error(`invalid metaBody type: ${metaBody.type}`);
      }
  }
  _bodyHub;
}
var $metaToStream = (metaBody, ipc2) => {
  if (ipc2 == null) {
    throw new Error(`miss ipc when ipc-response has stream-body`);
  }
  const stream_ipc = ipc2;
  const stream_id = metaBody.streamId;
  let paused = true;
  const stream = new ReadableStream({
    start(controller) {
      ipc2.onClosed(() => {
        try {
          controller.close();
        } catch {
        }
      });
      let firstData;
      switch (metaBody.type_encoding) {
        case IPC_DATA_ENCODING.UTF8:
          firstData = simpleEncoder(metaBody.data, "utf8");
          break;
        case IPC_DATA_ENCODING.BASE64:
          firstData = simpleEncoder(metaBody.data, "base64");
          break;
        case IPC_DATA_ENCODING.BINARY:
          firstData = metaBody.data;
          break;
      }
      if (firstData) {
        controller.enqueue(firstData);
      }
      const off = ipc2.onStream("metaToStream").collect((event) => {
        const message = event.consumeMapNotNull((message2) => {
          if (message2.stream_id === stream_id) {
            return message2;
          }
        });
        if (message === undefined) {
          return;
        }
        switch (message.type) {
          case IPC_MESSAGE_TYPE.STREAM_DATA:
            controller.enqueue(ipcStreamData.binary(message));
            break;
          case IPC_MESSAGE_TYPE.STREAM_END:
            controller.close();
            off();
            break;
        }
      });
    },
    pull(_controller) {
      if (paused) {
        paused = false;
        stream_ipc.postMessage(ipcStreamPulling(stream_id));
      }
    },
    cancel() {
      stream_ipc.postMessage(ipcStreamAbort(stream_id));
    }
  }, {
    highWaterMark: 0
  });
  return stream;
};
new WritableStream({});

// src/core/ipc/helper/$messageToIpcMessage.ts
var $endpointMessageToCbor = (message) => encode($serializableEndpointMessage(message));
var $endpointMessageToJson = (message) => JSON.stringify($serializableEndpointMessage(message));
var $cborToEndpointMessage = (data) => decode(data);
var $jsonToEndpointMessage = (data) => JSON.parse(data);
var $normalizeIpcMessage = (ipcMessage, ipc2) => {
  switch (ipcMessage.type) {
    case IPC_MESSAGE_TYPE.REQUEST: {
      return new IpcClientRequest(ipcMessage.reqId, ipcMessage.url, ipcMessage.method, new IpcHeaders(ipcMessage.headers), IpcBodyReceiver.from(MetaBody.fromJSON(ipcMessage.metaBody), ipc2), ipc2);
    }
    case IPC_MESSAGE_TYPE.RESPONSE: {
      return new IpcResponse(ipcMessage.reqId, ipcMessage.statusCode, new IpcHeaders(ipcMessage.headers), IpcBodyReceiver.from(MetaBody.fromJSON(ipcMessage.metaBody), ipc2), ipc2);
    }
    default:
      return ipcMessage;
  }
};
var $serializableEndpointMessage = (message) => {
  switch (message.type) {
    case ENDPOINT_MESSAGE_TYPE.LIFECYCLE:
      return message;
    case ENDPOINT_MESSAGE_TYPE.IPC:
      switch (message.ipcMessage.type) {
        case IPC_MESSAGE_TYPE.REQUEST:
          return {
            ...message,
            ipcMessage: message.ipcMessage.toSerializable()
          };
        case IPC_MESSAGE_TYPE.RESPONSE:
          return {
            ...message,
            ipcMessage: message.ipcMessage.toSerializable()
          };
        default:
          return message;
      }
  }
};

// src/core/ipc/endpoint/CommonEndpoint.ts
class CommonEndpoint extends IpcEndpoint {
  constructor() {
    super(...arguments);
  }
  #protocol = ENDPOINT_PROTOCOL.JSON;
  get protocol() {
    return this.#protocol;
  }
  endpointMsgChannel = new Channel;
  #lifecycleRemoteMutableFlow = new StateSignal(endpointLifecycle(endpointLifecycleInit()), endpointLifecycle.equals);
  lifecycleRemoteFlow = this.#lifecycleRemoteMutableFlow.asReadyonly();
  getLocaleSubProtocols() {
    return new Set([ENDPOINT_PROTOCOL.JSON, ENDPOINT_PROTOCOL.CBOR]);
  }
  sendLifecycleToRemote(state) {
    this.console.debug("lifecycle-out", state);
    if (ENDPOINT_PROTOCOL.CBOR === this.protocol) {
      return this.postBinaryMessage(encode(state));
    }
    if (ENDPOINT_PROTOCOL.JSON === this.protocol) {
      return this.postTextMessage(JSON.stringify(state));
    }
  }
  async doStart() {
    this.lifecycleLocaleFlow.listen((lifecycle) => {
      if (lifecycle.state.name === ENDPOINT_LIFECYCLE_STATE.OPENED) {
        if (lifecycle.state.subProtocols.includes(ENDPOINT_PROTOCOL.CBOR)) {
          this.#protocol = ENDPOINT_PROTOCOL.CBOR;
        }
      }
    });
    (async () => {
      for await (const endpointMessage of this.endpointMsgChannel) {
        switch (endpointMessage.type) {
          case ENDPOINT_MESSAGE_TYPE.IPC: {
            const producer = this.getIpcMessageProducer(endpointMessage.pid);
            const ipc2 = await producer.ipcPo.promise;
            producer.producer.trySend($normalizeIpcMessage(endpointMessage.ipcMessage, await producer.ipcPo.promise));
            break;
          }
          case ENDPOINT_MESSAGE_TYPE.LIFECYCLE: {
            this.#lifecycleRemoteMutableFlow.emit(endpointMessage);
            break;
          }
        }
      }
    })();
  }
  async postIpcMessage(msg) {
    await this.awaitOpen("then-postIpcMessage");
    switch (this.#protocol) {
      case ENDPOINT_PROTOCOL.JSON:
        this.postTextMessage($endpointMessageToJson(msg));
        break;
      case ENDPOINT_PROTOCOL.CBOR:
        this.postBinaryMessage($endpointMessageToCbor(msg));
        break;
    }
  }
}

// src/core/ipc/endpoint/WebMessageEndpoint.ts
class WebMessageEndpoint extends CommonEndpoint {
  port;
  toString() {
    return `WebMessageEndpoint#${this.debugId}`;
  }
  constructor(port, debugId) {
    super(debugId);
    this.port = port;
    port.addEventListener("message", (event) => {
      const rawData = event.data;
      let message;
      if (this.protocol === ENDPOINT_PROTOCOL.CBOR && typeof rawData !== "string") {
        message = $cborToEndpointMessage(rawData);
      } else {
        message = $jsonToEndpointMessage(rawData);
      }
      this.endpointMsgChannel.send(message);
    });
    port.start();
  }
  doStart() {
    this.port.start();
    return super.doStart();
  }
  postTextMessage(data) {
    this.port.postMessage(data);
  }
  postBinaryMessage(data) {
    this.port.postMessage(data);
  }
  beforeClose = () => {
  };
}
// src/core/ipc/ipc-message/IpcError.ts
var ipcError = Object.assign((errorCode, message) => ({
  ...ipcMessageBase(IPC_MESSAGE_TYPE.ERROR),
  errorCode,
  message
}), {
  internalServer: (message) => ipcError(500, message)
});
// src/core/ipc/ipc-message/stream/IpcStreamPaused.ts
var ipcStreamPaused = (stream_id, fuse) => ({ ...ipcMessageBase(IPC_MESSAGE_TYPE.STREAM_PAUSED), stream_id, fuse: fuse ?? 1 });
// src/core/helper/ipcFetchHelper.ts
var fetchMid = (handler) => Object.assign(handler, { [FETCH_MID_SYMBOL]: true });
var FETCH_MID_SYMBOL = Symbol("fetch.middleware");
var fetchEnd = (handler) => Object.assign(handler, { [FETCH_END_SYMBOL]: true });
var FETCH_END_SYMBOL = Symbol("fetch.end");
var fetchWs = (handler) => Object.assign((event) => {
  if (isWebSocket(event.method, event.headers)) {
    return handler(event);
  }
}, { [FETCH_WS_SYMBOL]: true });
var FETCH_WS_SYMBOL = Symbol("fetch.websocket");
var $throw = (err) => {
  throw err;
};
var fetchHanlderFactory = {
  NoFound: () => fetchEnd((_event, res) => res ?? $throw(new FetchError("No Found", { status: 404 }))),
  Forbidden: () => fetchEnd((_event, res) => res ?? $throw(new FetchError("Forbidden", { status: 403 }))),
  BadRequest: () => fetchEnd((_event, res) => res ?? $throw(new FetchError("Bad Request", { status: 400 }))),
  InternalServerError: (message = "Internal Server Error") => fetchEnd((_event, res) => res ?? $throw(new FetchError(message, { status: 500 })))
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
      cors: (config = {}) => {
        onFetchHanlders.unshift((event) => {
          if (event.method === "OPTIONS") {
            return { body: "" };
          }
        });
        onFetchHanlders.push(fetchMid((res) => {
          res?.headers.init("Access-Control-Allow-Origin", config.origin ?? "*").init("Access-Control-Allow-Headers", config.headers ?? "*").init("Access-Control-Allow-Methods", config.methods ?? "*");
          return res;
        }));
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
  const onRequest = async (request) => {
    const ipc4 = request.ipc;
    const event = new IpcFetchEvent(request, ipc4);
    let res;
    for (const handler of onFetchHanlders) {
      try {
        let result = undefined;
        if (FETCH_MID_SYMBOL in handler) {
          if (res !== undefined) {
            result = await handler(res, event);
          }
        } else if (FETCH_END_SYMBOL in handler) {
          result = await handler(event, res);
        } else {
          if (res === undefined) {
            result = await handler(event);
          }
        }
        if (result instanceof IpcResponse) {
          res = result;
        } else if (result instanceof Response) {
          res = await IpcResponse.fromResponse(request.reqId, result, ipc4);
        } else if (typeof result === "object") {
          const reqId = request.reqId;
          const status = result.status ?? 200;
          const headers = new IpcHeaders(result.headers);
          if (result.body instanceof IpcBody) {
            res = new IpcResponse(reqId, status, headers, result.body, ipc4);
          } else {
            const body = await $bodyInitToIpcBodyArgs(result.body, (bodyInit) => {
              if (headers.has("Content-Type") === false || headers.get("Content-Type").startsWith("application/javascript")) {
                headers.init("Content-Type", "application/javascript;charset=utf8");
                return JSON.stringify(bodyInit);
              }
              return String(bodyInit);
            });
            if (typeof body === "string") {
              res = IpcResponse.fromText(reqId, status, headers, body, ipc4);
            } else if (isBinary(body)) {
              res = IpcResponse.fromBinary(reqId, status, headers, body, ipc4);
            } else if (body instanceof ReadableStream) {
              res = IpcResponse.fromStream(reqId, status, headers, body, ipc4);
            }
          }
        }
      } catch (err) {
        if (err instanceof Response) {
          res = await IpcResponse.fromResponse(request.reqId, err, ipc4);
        } else {
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
            res = IpcResponse.fromJson(request.reqId, err_code, new IpcHeaders().init("Content-Type", "text/html;charset=utf8"), { message: err_message, detail: err_detail }, ipc4);
          } else {
            res = IpcResponse.fromText(request.reqId, err_code, new IpcHeaders().init("Content-Type", "text/html;charset=utf8"), err instanceof Error ? `<h1>${err.message}</h1><hr/><pre>${err.stack}</pre>` : String(err), ipc4);
          }
        }
      }
    }
    if (res) {
      ipc4.postMessage(res);
      return res;
    }
  };
  return extendsTo(onRequest);
};

class IpcFetchEvent {
  ipcRequest2;
  ipc4;
  constructor(ipcRequest2, ipc4) {
    this.ipcRequest = ipcRequest2;
    this.ipc = ipc4;
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
  get body() {
    return this.request.body;
  }
  get bodyUsed() {
    return this.request.bodyUsed;
  }
  arrayBuffer() {
    return this.request.arrayBuffer();
  }
  async typedArray() {
    return new Uint8Array(await this.request.arrayBuffer());
  }
  blob() {
    return this.request.blob();
  }
  formData() {
    return this.request.formData();
  }
  json() {
    return this.request.json();
  }
  text() {
    return this.request.text();
  }
  get headers() {
    return this.ipcRequest.headers;
  }
  get method() {
    return this.ipcRequest.method;
  }
  get href() {
    return this.url.href;
  }
  get reqId() {
    return this.ipcRequest.reqId;
  }
}

class FetchError extends Error {
  constructor(message, options) {
    super(message);
    this.code = options?.status ?? 500;
  }
  code;
}
// src/helper/AdaptersManager.ts
class AdaptersManager {
  adapterOrderMap = new Map;
  orderdAdapters = [];
  _reorder() {
    this.orderdAdapters = [...this.adapterOrderMap].sort((a, b) => b[1] - a[1]).map((a) => a[0]);
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
}

// src/core/types.ts
var IPC_HANDLE_EVENT;
(function(IPC_HANDLE_EVENT2) {
  IPC_HANDLE_EVENT2["Activity"] = "activity";
  IPC_HANDLE_EVENT2["Renderer"] = "renderer";
  IPC_HANDLE_EVENT2["RendererDestroy"] = "renderer-destroy";
  IPC_HANDLE_EVENT2["Shortcut"] = "shortcut";
})(IPC_HANDLE_EVENT || (IPC_HANDLE_EVENT = {}));
var nativeFetchAdaptersManager = new AdaptersManager;
// src/js-process/worker/std-dweb-http.ts
var exports_std_dweb_http = {};
__export(exports_std_dweb_http, {
  ServerUrlInfo: () => {
    {
      return ServerUrlInfo;
    }
  },
  ServerStartResult: () => {
    {
      return ServerStartResult;
    }
  }
});

// src/core/http/const.ts
class ServerUrlInfo {
  host;
  internal_origin;
  public_origin;
  buildExtQuerys;
  constructor(host, internal_origin, public_origin, buildExtQuerys = new Map) {
    this.host = host;
    this.internal_origin = internal_origin;
    this.public_origin = public_origin;
    this.buildExtQuerys = buildExtQuerys;
  }
  _buildUrl(origin, builder) {
    if (typeof builder === "string") {
      return appendUrlSearchs(new URL(builder, origin), this.buildExtQuerys);
    }
    const url = new URL(origin);
    return appendUrlSearchs(builder?.(url) ?? url, this.buildExtQuerys);
  }
  buildDwebUrl(builder) {
    return this._buildUrl(`https://${this.host}`, builder);
  }
  buildPublicUrl(builder) {
    return appendUrlSearchs(this._buildUrl(this.public_origin, builder), [["X-Dweb-Host", this.host]]);
  }
  buildPublicHtmlUrl(builder) {
    return this._buildUrl(this._buildUrl(this.public_origin, (url) => {
      url.host = `${this.host}.${url.host}`;
    }).toString(), builder);
  }
  buildInternalUrl(builder) {
    return this._buildUrl(this.internal_origin, builder);
  }
  buildUrl(usePub = false, builder) {
    if (usePub) {
      return this.buildPublicUrl(builder);
    } else {
      return this.buildInternalUrl(builder);
    }
  }
  buildHtmlUrl(usePub = false, builder) {
    if (usePub) {
      return this.buildPublicHtmlUrl(builder);
    } else {
      return this.buildInternalUrl(builder);
    }
  }
}

class ServerStartResult {
  token;
  urlInfo;
  constructor(token, urlInfo) {
    this.token = token;
    this.urlInfo = urlInfo;
  }
}
// src/core/internal/ipcEventExt.ts
var onSomeEvent = (runtime, eventName, cb) => {
  runtime.onConnect.collect((onConnectEvent) => {
    onConnectEvent.data.onEvent(`on-${eventName}`).collect((onIpcEventEvent) => {
      const ipcSomeEvent = onIpcEventEvent.consumeFilter((event) => event.name === eventName);
      if (ipcSomeEvent !== undefined) {
        cb(ipcSomeEvent);
      }
    });
  });
};

// src/core/ipcEventOnActivity.ts
var ACTIVITY_EVENT = "activity";
var onActivity = (runtime, cb) => {
  return onSomeEvent(runtime, ACTIVITY_EVENT, cb);
};

// src/core/ipcEventOnRender.ts
var RENDERER_EVENT;
(function(RENDERER_EVENT2) {
  RENDERER_EVENT2["START"] = "renderer";
  RENDERER_EVENT2["DESTROY"] = "renderer-destroy";
})(RENDERER_EVENT || (RENDERER_EVENT = {}));
var onRenderer = (runtime, cb) => {
  return onSomeEvent(runtime, RENDERER_EVENT.START, cb);
};
var onRendererDestroy = (runtime, cb) => {
  return onSomeEvent(runtime, RENDERER_EVENT.DESTROY, cb);
};

// src/core/ipcEventOnShortcut.ts
var SHORTCUT_EVENT = "shortcut";
var onShortcut = (runtime, cb) => {
  runtime.onConnect.collect((onConnectEvent) => {
    onConnectEvent.data.onEvent("onShortcut").collect((onIpcEventEvent) => {
      const ipcRendererStartEvent = onIpcEventEvent.consumeFilter((event) => event.name === SHORTCUT_EVENT);
      if (ipcRendererStartEvent !== undefined) {
        cb(ipcRendererStartEvent);
      }
    });
  });
};

// src/helper/Mutex.ts
class Locker {
  po = new PromiseOut;
  constructor(pre_locker) {
    this.prev = pre_locker?.curr;
    this.curr = this.prev?.then(() => this.po.promise) ?? this.po.promise;
  }
  prev;
  curr;
}

class Mutex {
  constructor(lock) {
    if (lock) {
      this.lock();
    }
  }
  get isLocked() {
    return this._lockers.length > 0;
  }
  _lockers = [];
  get _lastLocker() {
    return this._lockers[this._lockers.length - 1];
  }
  lock() {
    const locker = new Locker(this._lastLocker);
    this._lockers.push(locker);
    return locker.prev;
  }
  unlock() {
    const locker = this._lockers.shift();
    locker?.po.resolve();
  }
  async withLock(cb) {
    await this.lock();
    try {
      return await cb();
    } finally {
      this.unlock();
    }
  }
}

// src/core/MicroModule.ts
var MMState;
(function(MMState2) {
  MMState2[MMState2["BOOTSTRAP"] = 0] = "BOOTSTRAP";
  MMState2[MMState2["SHUTDOWN"] = 1] = "SHUTDOWN";
})(MMState || (MMState = {}));

class MicroModule {
  get console() {
    return logger(this);
  }
  toString() {
    return `MicroModule(${this.manifest.mmid})`;
  }
  get isRunning() {
    return this.#runtime?.isRunning === true;
  }
  #runtime;
  get runtime() {
    const runtime = this.#runtime;
    if (runtime === undefined) {
      throw new Error(`${this.manifest.mmid} is no running`);
    }
    return runtime;
  }
  async bootstrap(bootstrapContext) {
    if (this.#runtime === undefined) {
      const runtime = this.createRuntime(bootstrapContext);
      runtime.onShutdown(() => {
        this.#runtime = undefined;
      });
      await runtime.bootstrap();
      this.#runtime = runtime;
    }
    return this.#runtime;
  }
}
__legacyDecorateClassTS([
  once()
], MicroModule.prototype, "console", null);

class MicroModuleRuntime {
  stateLock = new Mutex;
  state = MMState.SHUTDOWN;
  connectionLinks = new Set;
  connectionMap = new Map;
  get console() {
    return this.microModule.console;
  }
  #ipcConnectedProducer = new Producer("ipcConnect");
  onConnect = this.#ipcConnectedProducer.consumer("for-internal");
  get isRunning() {
    return this.state === MMState.BOOTSTRAP;
  }
  bootstrap() {
    return this.stateLock.withLock(async () => {
      if (this.state != MMState.BOOTSTRAP) {
        this.console.debug("bootstrap-start");
        await this._bootstrap();
        this.console.debug("bootstrap-end");
      } else {
        this.console.debug("bootstrap", `${this.mmid} already running`);
      }
      this.state = MMState.BOOTSTRAP;
    });
  }
  get beforeShotdownPo() {
    return new PromiseOut;
  }
  get onBeforeShutdown() {
    return promiseAsSignalListener(this.beforeShotdownPo.promise);
  }
  get shutdownPo() {
    return new PromiseOut;
  }
  get onShutdown() {
    return promiseAsSignalListener(this.shutdownPo.promise);
  }
  shutdown() {
    return this.stateLock.withLock(async () => {
      this.beforeShotdownPo.resolve(undefined);
      await this._shutdown();
      this.shutdownPo.resolve(undefined);
      this.#ipcConnectedProducer.close();
    });
  }
  connect(mmid) {
    return mapHelper.getOrPut(this.connectionMap, mmid, () => {
      const po = new PromiseOut;
      po.resolve(this.bootstrapContext.dns.connect(mmid));
      return po;
    }).promise;
  }
  async beConnect(ipc5, reason) {
    if (setHelper.add(this.connectionLinks, ipc5)) {
      this.console.debug("beConnect", ipc5);
      ipc5.onFork("beConnect").collect(async (forkEvent) => {
        ipc5.console.debug("onFork", forkEvent.data);
        await this.beConnect(forkEvent.consume());
      });
      this.onBeforeShutdown(() => {
        return ipc5.close();
      });
      ipc5.onClosed(() => {
        this.connectionLinks.delete(ipc5);
      });
      if (this.connectionMap.has(ipc5.remote.mmid) === false) {
        this.connectionMap.set(ipc5.remote.mmid, PromiseOut.resolve(ipc5));
      }
      this.#ipcConnectedProducer.send(ipc5);
    }
  }
  get _manifest() {
    return {
      mmid: this.mmid,
      name: this.name,
      short_name: this.short_name,
      ipc_support_protocols: this.ipc_support_protocols,
      dweb_deeplinks: this.dweb_deeplinks,
      categories: this.categories,
      dir: this.dir,
      lang: this.lang,
      description: this.description,
      icons: this.icons,
      screenshots: this.screenshots,
      display: this.display,
      orientation: this.orientation,
      theme_color: this.theme_color,
      background_color: this.background_color,
      shortcuts: this.shortcuts
    };
  }
  toManifest() {
    return this._manifest;
  }
}
__legacyDecorateClassTS([
  once()
], MicroModuleRuntime.prototype, "console", null);
__legacyDecorateClassTS([
  once()
], MicroModuleRuntime.prototype, "beforeShotdownPo", null);
__legacyDecorateClassTS([
  once()
], MicroModuleRuntime.prototype, "onBeforeShutdown", null);
__legacyDecorateClassTS([
  once()
], MicroModuleRuntime.prototype, "shutdownPo", null);
__legacyDecorateClassTS([
  once()
], MicroModuleRuntime.prototype, "onShutdown", null);
__legacyDecorateClassTS([
  once()
], MicroModuleRuntime.prototype, "_manifest", null);

// src/js-process/worker/index.ts
var workerGlobal = self;

class Metadata {
  data;
  env;
  constructor(data, env) {
    this.data = data;
    this.env = env;
  }
  envString(key) {
    const val = this.envStringOrNull(key);
    if (val == null) {
      throw new Error(`no found (string) ${key}`);
    }
    return val;
  }
  envStringOrNull(key) {
    const val = this.env[key];
    if (val == null) {
      return;
    }
    return val;
  }
  envBoolean(key) {
    const val = this.envBooleanOrNull(key);
    if (val == null) {
      throw new Error(`no found (boolean) ${key}`);
    }
    return val;
  }
  envBooleanOrNull(key) {
    const val = this.envStringOrNull(key);
    if (val == null) {
      return;
    }
    return val === "true";
  }
}

class JsProcessMicroModule extends MicroModule {
  meta;
  nativeFetchPort;
  manifest = {
    mmid: this.meta.data.mmid,
    ipc_support_protocols: {
      json: false,
      cbor: true,
      protobuf: false
    },
    dweb_deeplinks: [],
    categories: [],
    name: this.meta.data.mmid
  };
  ipcPool = new IpcPool(this.meta.data.mmid);
  fetchIpc = this.ipcPool.createIpc(new WebMessageEndpoint(this.nativeFetchPort, "fetch"), 0, this.manifest, this.manifest, true);
  createRuntime(context) {
    return new JsProcessMicroModuleRuntime(this, context);
  }
  get bootstrapContext() {
    const ctx = {
      dns: {
        install: function(mm) {
          throw new Error("jmm dns.install not implemented.");
        },
        uninstall: function(mm) {
          throw new Error("jmm dns.uninstall not implemented.");
        },
        connect: (mmid, reason) => {
          const po = new PromiseOut;
          this.fetchIpc.postMessage(IpcEvent.fromText(`dns/connect/${mmid}`, JSON.stringify({
            mmid,
            ipc_support_protocols: this.manifest.ipc_support_protocols
          })));
          const _beConnect = (event) => {
            const data = event.data;
            if (Array.isArray(data) === false) {
              return;
            }
            if (data[0] === `ipc-connect/${mmid}`) {
              const port = event.ports[0];
              const endpoint = new WebMessageEndpoint(port, mmid);
              const manifest = data[1];
              const env = data[2];
              Object.defineProperty(manifest, "env", { value: Object.freeze(env) });
              const ipc5 = this.ipcPool.createIpc(endpoint, 0, manifest, manifest, false);
              po.resolve(ipc5);
              workerGlobal.removeEventListener("message", _beConnect);
            }
          };
          workerGlobal.addEventListener("message", _beConnect);
          return po.promise;
        },
        query: (mmid) => {
          throw new Error("dns.query not implemented.");
        },
        search: (category) => {
          throw new Error("dns.search not implemented.");
        },
        open: (mmid) => {
          throw new Error("dns.open not implemented.");
        },
        close: (mmid) => {
          throw new Error("dns.close not implemented.");
        },
        restart: (mmid) => {
          throw new Error("dns.restart not implemented.");
        }
      }
    };
    this.fetchIpc.onClosed(() => {
      workerGlobal.close();
    });
    return ctx;
  }
  constructor(meta, nativeFetchPort) {
    super();
    this.meta = meta;
    this.nativeFetchPort = nativeFetchPort;
  }
  async bootstrap() {
    return await super.bootstrap(this.bootstrapContext);
  }
}

class JsProcessMicroModuleRuntime extends MicroModuleRuntime {
  microModule;
  bootstrapContext;
  _bootstrap() {
  }
  async _shutdown() {
    await this.fetchIpc.close();
  }
  mmid;
  name;
  host;
  dweb_deeplinks = [];
  categories = [];
  dir;
  lang;
  short_name;
  description;
  icons;
  screenshots;
  display;
  orientation;
  theme_color;
  background_color;
  shortcuts;
  get ipc_support_protocols() {
    return {
      json: true,
      cbor: true,
      protobuf: false
    };
  }
  ipcPool = this.microModule.ipcPool;
  fetchIpc = this.microModule.fetchIpc;
  meta = this.microModule.meta;
  constructor(microModule, bootstrapContext) {
    super();
    this.microModule = microModule;
    this.bootstrapContext = bootstrapContext;
    this.mmid = this.meta.data.mmid;
    this.name = `js process of ${this.mmid}`;
    this.host = this.meta.envString("host");
  }
  get onActivity() {
    return onActivity.bind(null, this);
  }
  get onRenderer() {
    return onRenderer.bind(null, this);
  }
  get onRendererDestroy() {
    return onRendererDestroy.bind(null, this);
  }
  get onShortcut() {
    return onShortcut.bind(null, this);
  }
  async _nativeFetch(url, init) {
    const args = normalizeFetchArgs(url, init);
    const hostName = args.parsed_url.hostname;
    if (!(hostName.endsWith(".dweb") && args.parsed_url.protocol === "file:")) {
      const ipc_response2 = await this._nativeRequest(args.parsed_url, args.request_init);
      return ipc_response2.toResponse(args.parsed_url.href);
    }
    const ipc5 = await this.connect(hostName);
    const ipc_req_init = await $normalizeRequestInitAsIpcRequestArgs(args.request_init);
    let ipc_response = await ipc5.request(args.parsed_url.href, ipc_req_init);
    if (ipc_response.statusCode === 401) {
      try {
        const permissions = await ipc_response.body.text();
        if (await this.requestDwebPermissions(permissions)) {
          ipc_response = await ipc5.request(args.parsed_url.href, ipc_req_init);
        }
      } catch (e) {
        console.error("fail to request permission:", e);
      }
    }
    return ipc_response.toResponse(args.parsed_url.href);
  }
  async requestDwebPermissions(permissions) {
    const res = await (await this.nativeFetch(new URL(`file://permission.std.dweb/request?permissions=${encodeURIComponent(permissions)}`))).text();
    const requestPermissionResult = JSON.parse(res);
    return Object.values(requestPermissionResult).every((status) => status === "granted");
  }
  nativeFetch(url, init) {
    return Object.assign(this._nativeFetch(url, init), fetchExtends);
  }
  async _nativeRequest(parsed_url, request_init) {
    const ipc_req_init = await $normalizeRequestInitAsIpcRequestArgs(request_init);
    return await this.fetchIpc.request(parsed_url.href, ipc_req_init);
  }
  nativeRequest(url, init) {
    const args = normalizeFetchArgs(url, init);
    return this._nativeRequest(args.parsed_url, args.request_init);
  }
  get routes() {
    const routes = createFetchHandler([]);
    this.onConnect.collect((ipcConnectEvent) => {
      ipcConnectEvent.data.onRequest("onFetch").collect((ipcRequestEvent) => {
        const ipcRequest2 = ipcRequestEvent.consume();
        routes(ipcRequest2);
      });
    });
    return routes;
  }
  async close(cause) {
    await this.fetchIpc.close(cause);
    this.ipcPool.destroy();
  }
}
__legacyDecorateClassTS([
  once()
], JsProcessMicroModuleRuntime.prototype, "ipc_support_protocols", null);
__legacyDecorateClassTS([
  once()
], JsProcessMicroModuleRuntime.prototype, "routes", null);
var waitFetchPort = () => {
  return new Promise((resolve) => {
    workerGlobal.addEventListener("message", function onFetchIpcChannel(event) {
      const data = event.data;
      if (Array.isArray(event.data) === false) {
        return;
      }
      if (data[0] === "fetch-ipc-channel") {
        resolve(data[1]);
        workerGlobal.removeEventListener("message", onFetchIpcChannel);
      }
    });
  });
};
var originalFetch = fetch;
var httpFetch = (input, init) => {
  let inputUrl = "https://http.std.dweb/fetch";
  const searchParams = new URLSearchParams;
  if (input instanceof Request) {
    searchParams.set("url", input.url);
    searchParams.set("credentials", input.credentials);
  } else if (typeof input === "string") {
    searchParams.set("url", input);
  } else if (input instanceof URL) {
    searchParams.set("url", input.href);
  }
  inputUrl += `?${searchParams.toString()}`;
  return originalFetch(inputUrl, init);
};

class DwebXMLHttpRequest extends XMLHttpRequest {
  constructor() {
    super(...arguments);
  }
  #inputUrl = "https://http.std.dweb/fetch";
  open(method, url, async, username, password) {
    let input;
    if (typeof url === "string") {
      input = new URL(url);
    } else if (url instanceof URL) {
      input = url;
    }
    this.#inputUrl += `?url=${input.href}`;
    super.open(method, this.#inputUrl, async ? true : false, username ? username : null, password ? password : null);
  }
}
var installEnv = async (metadata, gatewayPort) => {
  const jmm = new JsProcessMicroModule(metadata, await waitFetchPort());
  const jsProcess = await jmm.bootstrap();
  const jsMicroModule = metadata.envString("jsMicroModule");
  const [version, patch] = jsMicroModule.split(".").map((v) => parseInt(v));
  const dweb = {
    jsProcess,
    core: exports_std_dweb_core,
    ipc: exports_std_dweb_core,
    http: exports_std_dweb_http,
    versions: { jsMicroModule },
    version,
    patch
  };
  Object.assign(navigator, { dweb });
  Object.defineProperties(globalThis, {
    fetch: {
      value: httpFetch
    },
    XMLHttpRequest: {
      value: DwebXMLHttpRequest
    },
    WebSocket: {
      value: class extends WebSocket {
        constructor(url, protocols) {
          let input = "wss://http.std.dweb/websocket";
          if (/iPhone|iPad|iPod/i.test(navigator.userAgent)) {
            input = `ws://localhost:${gatewayPort}?X-Dweb-Url=${input.replace("wss:", "ws:")}`;
          }
          if (typeof url === "string") {
            input += `?url=${url}`;
          } else if (url instanceof URL) {
            input += `?url=${url.href}`;
          }
          super(input, protocols);
        }
      }
    }
  });
  workerGlobal.postMessage(["env-ready"]);
  workerGlobal.addEventListener("message", async function runMain(event) {
    const data = event.data;
    if (Array.isArray(event.data) === false) {
      return;
    }
    if (data[0] === "run-main") {
      const config = data[1];
      const main_parsed_url = updateUrlOrigin(config.main_url, `${self.location.href.startsWith("blob:https:") ? "https" : "http"}://${jsProcess.host}`);
      const location2 = {
        hash: main_parsed_url.hash,
        host: main_parsed_url.host,
        hostname: main_parsed_url.hostname,
        href: main_parsed_url.href,
        origin: main_parsed_url.origin,
        pathname: main_parsed_url.pathname,
        port: main_parsed_url.port,
        protocol: main_parsed_url.protocol,
        search: main_parsed_url.search,
        toString() {
          return main_parsed_url.href;
        }
      };
      Object.setPrototypeOf(location2, WorkerLocation.prototype);
      Object.freeze(location2);
      Object.defineProperty(workerGlobal, "location", {
        value: location2,
        configurable: false,
        enumerable: false,
        writable: false
      });
      await import(config.main_url);
      workerGlobal.removeEventListener("message", runMain);
    }
  });
  return jsProcess;
};
export {
  installEnv,
  Metadata,
  JsProcessMicroModuleRuntime,
  JsProcessMicroModule
};
