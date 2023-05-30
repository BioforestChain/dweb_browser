var __defProp = Object.defineProperty;
var __getOwnPropDesc = Object.getOwnPropertyDescriptor;
var __decorateClass = (decorators, target, key, kind) => {
  var result = kind > 1 ? void 0 : kind ? __getOwnPropDesc(target, key) : target;
  for (var i = decorators.length - 1, decorator; i >= 0; i--)
    if (decorator = decorators[i])
      result = (kind ? decorator(target, key, result) : decorator(result)) || result;
  if (kind && result)
    __defProp(target, key, result);
  return result;
};

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

// src/helper/binaryHelper.cts
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

// src/helper/readableStreamHelper.cts
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
    return (this._on_cancel_signal ?? (this._on_cancel_signal = createSignal2())).listen;
  }
  get onPull() {
    return (this._on_pull_signal ?? (this._on_pull_signal = createSignal2())).listen;
  }
};

// src/user/tool/tool.request.mts
var { IpcResponse, Ipc, IpcRequest, IpcHeaders, IPC_METHOD } = ipc;
var hashConnentMap = /* @__PURE__ */ new Set();
var ipcObserversMap = /* @__PURE__ */ new Map();
var INTERNAL_PREFIX = "/internal";
var fetchSignal = createSignal2();
async function onApiRequest(serverurlInfo, request, httpServerIpc) {
  let ipcResponse;
  const url = request.parsed_url;
  try {
    if (url.pathname.startsWith(INTERNAL_PREFIX)) {
      ipcResponse = internalFactory(
        url,
        request.req_id,
        httpServerIpc,
        serverurlInfo
      );
    } else {
      const path = `file:/${url.pathname}${url.search}`;
      const ipcProxyRequest = new IpcRequest(
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
      ipcResponse = new IpcResponse(
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
      ipcResponse = await IpcResponse.fromText(
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
    return IpcResponse.fromText(
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
    const streamPo = observeFactory(mmid);
    return IpcResponse.fromStream(
      req_id,
      200,
      void 0,
      streamPo.stream,
      httpServerIpc
    );
  }
  if (pathname === "/fetch") {
    const streamPo = serviceWorkerFetch();
    return IpcResponse.fromStream(
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
var observeFactory = (mmid) => {
  const streamPo = new ReadableStreamOut();
  const observers = mapHelper.getOrPut(ipcObserversMap, mmid, (mmid2) => {
    const result = { ipc: new PromiseOut2(), obs: /* @__PURE__ */ new Set() };
    result.ipc.resolve(jsProcess.connect(mmid2));
    result.ipc.promise.then((ipc2) => {
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

// src/user/tool/app.handle.mts
var webViewMap = /* @__PURE__ */ new Map();
var closeApp = async (servers, ipcs) => {
  hashConnentMap.clear();
  const serverOp = servers.map(async (server) => {
    await server.close();
  });
  const opcOp = ipcs.map((ipc2) => {
    ipc2.close();
  });
  await Promise.all([serverOp, opcOp]);
  closeFront();
};
var closeFront = () => {
  webViewMap.forEach(async (state) => {
    await closeDwebView(state.webviewId);
  });
  webViewMap.clear();
  return "closeFront ok";
};

// src/user/public-service/public.service.worker.mts
var main = async () => {
  const { IpcEvent } = ipc;
  const mainUrl = new PromiseOut();
  let oldWebviewState = [];
  const multiWebViewIpc = await jsProcess.connect("mwebview.sys.dweb");
  const multiWebViewCloseSignal = createSignal();
  const EXTERNAL_PREFIX = "/external/";
  const tryOpenView = async () => {
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
  };
  const { IpcResponse: IpcResponse2, IpcHeaders: IpcHeaders2 } = ipc;
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
    const url = request.parsed_url;
    if (url.pathname.startsWith("/dns.sys.dweb")) {
      const result = await serviceWorkerFactory(url, ipc2);
      const ipcResponse = IpcResponse2.fromText(
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
  wwwReadableStreamIpc.onRequest(async (request, ipc2) => {
    let pathname = request.parsed_url.pathname;
    if (pathname === "/") {
      pathname = "/index.html";
    }
    const remoteIpcResponse = await jsProcess.nativeRequest(
      `file:///cot-demo${pathname}?mode=stream`
    );
    ipc2.postMessage(
      new IpcResponse2(
        request.req_id,
        remoteIpcResponse.statusCode,
        cros(remoteIpcResponse.headers),
        remoteIpcResponse.body,
        ipc2
      )
    );
  });
  const externalMap = /* @__PURE__ */ new Map();
  externalReadableStreamIpc.onRequest(async (request, ipc2) => {
    const url = request.parsed_url;
    const xHost = decodeURIComponent(url.searchParams.get("X-Dweb-Host") ?? "");
    if (url.pathname.startsWith(EXTERNAL_PREFIX)) {
      const pathname = url.pathname.slice(EXTERNAL_PREFIX.length);
      const externalReqId = parseInt(pathname);
      if (typeof externalReqId !== "number" || isNaN(externalReqId)) {
        return ipc2.postMessage(
          IpcResponse2.fromText(
            request.req_id,
            400,
            request.headers,
            "reqId is NAN",
            ipc2
          )
        );
      }
      const responsePOo = externalMap.get(externalReqId);
      if (!responsePOo) {
        return ipc2.postMessage(
          IpcResponse2.fromText(
            request.req_id,
            500,
            request.headers,
            `not found external requst,req_id ${externalReqId}`,
            ipc2
          )
        );
      }
      responsePOo.resolve(
        new IpcResponse2(externalReqId, 200, request.headers, request.body, ipc2)
      );
      externalMap.delete(externalReqId);
      const icpResponse = IpcResponse2.fromText(
        request.req_id,
        200,
        request.headers,
        "ok",
        ipc2
      );
      cros(icpResponse.headers);
      return ipc2.postMessage(icpResponse);
    }
    if (xHost === externalServer.startResult.urlInfo.host) {
      fetchSignal.emit(request);
      const awaitResponse = new PromiseOut();
      externalMap.set(request.req_id, awaitResponse);
      const ipcResponse = await awaitResponse.promise;
      cros(ipcResponse.headers);
      ipc2.postMessage(ipcResponse);
    }
  });
  const serviceWorkerFactory = async (url, ipc2) => {
    const pathname = url.pathname;
    if (pathname.endsWith("close")) {
      return closeFront();
    }
    if (pathname.endsWith("restart")) {
      multiWebViewCloseSignal.emit();
      closeApp(
        [apiServer, wwwServer, externalServer],
        [apiReadableStreamIpc, wwwReadableStreamIpc, externalReadableStreamIpc]
      );
      jsProcess.restart();
      return "restart ok";
    }
    return "no action for serviceWorker Factory !!!";
  };
  jsProcess.onActivity(async (ipcEvent, ipc2) => {
    await tryOpenView();
    ipc2.postMessage(IpcEvent.fromText("ready", "activity"));
    if (hasActivityEventIpcs.has(ipc2) === false) {
      hasActivityEventIpcs.add(ipc2);
      multiWebViewCloseSignal.listen(() => {
        ipc2.postMessage(IpcEvent.fromText("close", ""));
        ipc2.close();
      });
    }
  });
  const hasActivityEventIpcs = /* @__PURE__ */ new Set();
  jsProcess.onClose(async (event, ipc2) => {
    multiWebViewCloseSignal.emit();
    return closeApp(
      [apiServer, wwwServer, externalServer],
      [apiReadableStreamIpc, wwwReadableStreamIpc, externalReadableStreamIpc]
    );
  });
  multiWebViewIpc.onEvent(async (event, ipc2) => {
    if (event.name === "state" /* State */ && typeof event.data === "string") {
      const newState = JSON.parse(event.data);
      const diff = detailed_default(oldWebviewState, newState);
      oldWebviewState = newState;
      diffFactory(diff);
    }
    multiWebViewCloseSignal.listen(() => {
      ipc2.postMessage(IpcEvent.fromText("close", ""));
      ipc2.close();
    });
  });
  const diffFactory = async (diff) => {
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
  const interUrl = wwwServer.startResult.urlInfo.buildInternalUrl((url) => {
    url.pathname = "/index.html";
  });
  interUrl.searchParams.set("X-Api-Host", apiServer.startResult.urlInfo.host);
  mainUrl.resolve(interUrl.href);
  tryOpenView();
};
main();
