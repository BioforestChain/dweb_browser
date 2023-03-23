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
    return (this._on_cancel_signal ?? (this._on_cancel_signal = createSignal())).listen;
  }
  get onPull() {
    return (this._on_pull_signal ?? (this._on_pull_signal = createSignal())).listen;
  }
};

// src/user/cot-demo/cotDemo.request.mts
var { IpcResponse, Ipc, IpcRequest, IpcHeaders } = ipc;
var ipcObserversMap = /* @__PURE__ */ new Map();
var INTERNAL_PREFIX = "/internal";
async function onApiRequest(serverurlInfo, request, httpServerIpc) {
  let ipcResponse;
  try {
    const url = new URL(request.url, serverurlInfo.internal_origin);
    console.log("cotDemo#onApiRequest=>", url.href, request.method);
    if (url.pathname.startsWith(INTERNAL_PREFIX)) {
      const pathname = url.pathname.slice(INTERNAL_PREFIX.length);
      if (pathname === "/public-url") {
        ipcResponse = IpcResponse.fromText(
          request.req_id,
          200,
          void 0,
          serverurlInfo.buildPublicUrl(() => {
          }).href,
          httpServerIpc
        );
      } else if (pathname === "/observe") {
        const mmid = url.searchParams.get("mmid");
        if (mmid === null) {
          throw new Error("observe require mmid");
        }
        const streamPo = new ReadableStreamOut();
        const observers = mapHelper.getOrPut(ipcObserversMap, mmid, (mmid2) => {
          const result = { ipc: new PromiseOut2(), obs: /* @__PURE__ */ new Set() };
          result.ipc.resolve(jsProcess.connect(mmid2));
          result.ipc.promise.then((ipc2) => {
            ipc2.onEvent((event) => {
              console.log("on-event", event);
              if (event.name !== "observe") {
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
        ipcResponse = IpcResponse.fromStream(
          request.req_id,
          200,
          void 0,
          streamPo.stream,
          httpServerIpc
        );
      } else {
        throw new Error(`unknown gateway: ${url.search}`);
      }
    } else {
      const path = `file:/${url.pathname}${url.search}`;
      console.log("onRequestPath: ", path, request.method, request.body);
      if (request.method === "POST") {
        const response = await jsProcess.nativeFetch(path, {
          body: request.body.raw,
          method: request.method
        });
        ipcResponse = await IpcResponse.fromResponse(
          request.req_id,
          response,
          httpServerIpc
          // true
        );
      } else {
        const response = await jsProcess.nativeFetch(path);
        ipcResponse = await IpcResponse.fromResponse(
          request.req_id,
          response,
          httpServerIpc
          // true
        );
      }
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
var cros = (headers) => {
  headers.init("Access-Control-Allow-Origin", "*");
  headers.init("Access-Control-Allow-Headers", "*");
  headers.init("Access-Control-Allow-Methods", "*");
  return headers;
};

// src/user/cot-demo/cotDemo.worker.mts
var main = async () => {
  const { IpcEvent } = ipc;
  const mainUrl = new PromiseOut();
  const webviewSet = /* @__PURE__ */ new Set();
  const tryOpenView = async (webview_id) => {
    console.log("tryOpenView", webview_id);
    if (webview_id && webviewSet.has(webview_id)) {
      const result = await jsProcess.nativeFetch(
        `file://mwebview.sys.dweb/reOpen?webview_id=${encodeURIComponent(webview_id)}`
      ).text();
      return result;
    }
    const url = await mainUrl.promise;
    const view_id = await jsProcess.nativeFetch(
      `file://mwebview.sys.dweb/open?url=${encodeURIComponent(url)}`
    ).text();
    webviewSet.add(view_id);
    return view_id;
  };
  let hasActivity = false;
  jsProcess.onConnect((ipc2) => {
    console.log("on connect", ipc2);
    ipc2.onEvent(async (event) => {
      console.log("cotDemo.worker => ", event.name, typeof event.data === "string");
      if (event.name === "activity" && typeof event.data === "string") {
        hasActivity = true;
        const view_id = await tryOpenView(event.data);
        console.log("cotDemo.worker => activity", view_id);
        ipc2.postMessage(IpcEvent.fromText("ready", view_id));
        return;
      }
    });
  });
  console.log("[cotDemo.worker.mts] main");
  const { IpcResponse: IpcResponse2, IpcHeaders: IpcHeaders2 } = ipc;
  const wwwServer = await http.createHttpDwebServer(jsProcess, {
    subdomain: "www",
    port: 443
  });
  const apiServer = await http.createHttpDwebServer(jsProcess, {
    subdomain: "api",
    port: 443
  });
  (await apiServer.listen()).onRequest(async (request, ipc2) => {
    onApiRequest(apiServer.startResult.urlInfo, request, ipc2);
  });
  (await wwwServer.listen()).onRequest(async (request, ipc2) => {
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
      new IpcResponse2(
        request.req_id,
        remoteIpcResponse.statusCode,
        cros(remoteIpcResponse.headers),
        remoteIpcResponse.body,
        ipc2
      )
    );
  });
  {
    const interUrl = wwwServer.startResult.urlInfo.buildInternalUrl((url) => {
      url.pathname = "/index.html";
    }).href;
    mainUrl.resolve(interUrl);
    if (hasActivity === false) {
      await tryOpenView();
    }
  }
  {
  }
};
main();
