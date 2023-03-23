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

// src/user/browser/www-server-on-request.mts
var { IpcResponse, IpcHeaders } = ipc;
async function wwwServerOnRequest(request, ipc2) {
  let pathname = request.parsed_url.pathname;
  pathname = pathname === "/" ? "/index.html" : pathname;
  const url = `file:///app/cot-demo${pathname}?mode=stream`;
  const response = await jsProcess.nativeRequest(url);
  ipc2.postMessage(
    new IpcResponse(
      request.req_id,
      response.statusCode,
      response.headers,
      response.body,
      ipc2
    )
  );
}

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

// src/helper/PromiseOut.cts
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
  static resolve(v) {
    const po = new PromiseOut();
    po.resolve(v);
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
    return (this._on_cancel_signal ??= createSignal()).listen;
  }
  get onPull() {
    return (this._on_pull_signal ??= createSignal()).listen;
  }
};

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
var PromiseOut = class {
  constructor() {
    this.promise = new Promise((resolve, reject) => {
      this.resolve = resolve;
      this.reject = reject;
    }).then((res) => {
      this._value = res;
      return res;
    });
  }
  static resolve(v) {
    const po = new PromiseOut();
    po.resolve(v);
    return po;
  }
  get value() {
    return this._value;
  }
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

// src/helper/devtools.cts
var Log = class {
  log(str) {
    console.log(str);
  }
  red(str) {
    console.log(`\x1B[31m%s\x1B[0m`, str);
  }
  green(str) {
    console.log(`\x1B[32m%s\x1B[0m`, str);
  }
  yellow(str) {
    console.log(`\x1B[33m%s\x1B[0m`, str);
  }
  blue(str) {
    console.log(`\x1B[34m%s\x1B[0m`, str);
  }
  // 品红色
  magenta(str) {
    console.log(`\x1B[35m%s\x1B[0m`, str);
  }
  cyan(str) {
    console.log(`\x1B[36m%s\x1B[0m`, str);
  }
  grey(str) {
    console.log(`\x1B[36m%s\x1B[0m`, str);
  }
};
var log = new Log();

// src/user/browser/api-server-on-request.mts
var symbolETO = Symbol("***eto***");
var { IpcEvent, IpcResponse: IpcResponse2 } = ipc;
async function createApiServerOnRequest(www_server_internal_origin, apiServerUrlInfo) {
  return async (ipcRequest, ipc2) => {
    apiServerOnRequest(ipcRequest, ipc2, www_server_internal_origin, apiServerUrlInfo);
  };
}
async function apiServerOnRequest(ipcRequest, ipc2, www_server_internal_origin, apiServerUrlInfo) {
  const pathname = ipcRequest.parsed_url.pathname;
  console.log("api-server-on-request.mts", ipcRequest.parsed_url);
  switch (pathname) {
    case (pathname.startsWith("/internal") ? pathname : symbolETO):
      apiServerOnRequestInternal(ipcRequest, ipc2, www_server_internal_origin, apiServerUrlInfo);
      break;
    case (pathname.startsWith("/status-bar.sys.dweb") ? pathname : symbolETO):
      apiServerOnRequestStatusbar(ipcRequest, ipc2, pathname, www_server_internal_origin);
      break;
    default:
      throw new Error(`[\u7F3A\u5C11\u5904\u7406\u5668] ${ipcRequest.parsed_url}`);
  }
}
async function apiServerOnRequestInternal(ipcRequest, ipc2, www_server_internal_origin, apiServerUrlInfo) {
  const pathname = ipcRequest.parsed_url.pathname;
  switch (pathname) {
    case "/internal/public-url":
      apiServerOnRequestInternalPublicUrl(ipcRequest, ipc2, www_server_internal_origin, apiServerUrlInfo);
      break;
    case "/internal/observe":
      apiServerOnRequestInternalObserver(ipcRequest, ipc2, www_server_internal_origin, apiServerUrlInfo);
      break;
    default:
      throw new Error(`[\u7F3A\u5C11\u5904\u7406\u5668] ${ipcRequest.parsed_url}`);
  }
}
async function apiServerOnRequestInternalPublicUrl(ipcRequest, ipc2, www_server_internal_origin, apiServerUrlInfo) {
  const ipcResponse = IpcResponse2.fromText(
    ipcRequest.req_id,
    200,
    void 0,
    apiServerUrlInfo.buildPublicUrl(() => {
    }).href,
    ipc2
  );
  ipcResponse.headers.init("Access-Control-Allow-Origin", "*");
  ipcResponse.headers.init("Access-Control-Allow-Headers", "*");
  ipcResponse.headers.init("Access-Control-Allow-Methods", "*");
  ipc2.postMessage(ipcResponse);
}
function apiServerOnRequestInternalObserver(ipcRequest, ipc2, www_server_internal_origin, apiServerUrlInfo) {
  console.error("apiServerOnRequestInternalObserver");
  const url = new URL(ipcRequest.url, apiServerUrlInfo.internal_origin);
  const mmid = url.searchParams.get("mmid");
  if (mmid === null) {
    throw new Error("observe require mmid");
  }
  const streamPo = new ReadableStreamOut();
  const observers = mapHelper.getOrPut(ipcObserversMap, mmid, (mmid2) => {
    const result = { ipc: new PromiseOut(), obs: /* @__PURE__ */ new Set() };
    result.ipc.resolve(jsProcess.connect(mmid2));
    result.ipc.promise.then((ipc3) => {
      console.error("connect\u4E4B\u540E ipc.remote.mmid: ", ipc3.remote.mmid);
      ipc3.postMessage(
        IpcEvent.fromText(
          "send-url",
          www_server_internal_origin
        )
      );
      ipc3.onEvent((event) => {
        console.log("on-event", event);
        if (event.name !== "observe") {
          return;
        }
        const observers2 = ipcObserversMap.get(ipc3.remote.mmid);
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
  const ipcResponse = IpcResponse2.fromStream(
    ipcRequest.req_id,
    200,
    void 0,
    streamPo.stream,
    ipc2
  );
  ipcResponse.headers.init("Access-Control-Allow-Origin", "*");
  ipcResponse.headers.init("Access-Control-Allow-Headers", "*");
  ipcResponse.headers.init("Access-Control-Allow-Methods", "*");
  ipc2.postMessage(ipcResponse);
}
async function apiServerOnRequestStatusbar(ipcRequest, ipc2, pathname, internal_origin) {
  switch (pathname) {
    case (pathname.endsWith("setBackgroundColor") ? pathname : symbolETO):
      statusbarSetBackgroundColor(ipcRequest, ipc2, internal_origin);
      break;
    case (pathname.endsWith("/getInfo") ? pathname : symbolETO):
      apiServerGetBackgroundColor(ipcRequest, ipc2, internal_origin);
      break;
    case (pathname.endsWith(`/setStyle`) ? pathname : symbolETO):
      apiServerSetStyle(ipcRequest, ipc2, internal_origin);
      break;
    default:
      log.red(`\u7F3A\u5C11 statusbar-bar.sys.dweb \u5904\u7406\u5668 ${ipcRequest.parsed_url} pathname === ${pathname}`);
  }
}
async function statusbarSetBackgroundColor(ipcRequest, ipc2, internal_origin) {
  const response = await jsProcess.nativeFetch(`file://status-bar.sys.dweb/operation/set_background_color${ipcRequest.parsed_url.search}&app_url=${internal_origin}`);
  ipc2.postMessage(
    await IpcResponse2.fromResponse(
      ipcRequest.req_id,
      response,
      ipc2
    )
  );
}
async function apiServerGetBackgroundColor(ipcRequest, ipc2, internal_origin) {
  const response = await jsProcess.nativeFetch(`file://status-bar.sys.dweb/operation/get_background_color?app_url=${internal_origin}`);
  ipc2.postMessage(
    await IpcResponse2.fromResponse(
      ipcRequest.req_id,
      response,
      ipc2
    )
  );
}
async function apiServerSetStyle(ipcRequest, ipc2, internal_origin) {
  log.red(`api-server-on-request.mts ipcRequest.parsed_url.search==${ipcRequest.parsed_url.search}`);
  const response = await jsProcess.nativeFetch(`file://status-bar.sys.dweb/operation/set_style${ipcRequest.parsed_url.search}&app_url=${internal_origin}`);
  ipc2.postMessage(
    await IpcResponse2.fromResponse(
      ipcRequest.req_id,
      response,
      ipc2
    )
  );
}
var { IpcHeaders: IpcHeaders2 } = ipc;
var ipcObserversMap = /* @__PURE__ */ new Map();

// src/user/browser/browser.worker.mts
var main = async () => {
  log.green("[browser.worker.mts bootstrap]");
  const { IpcEvent: IpcEvent2 } = ipc;
  const wwwServer = await http.createHttpDwebServer(jsProcess, { subdomain: "www", port: 443 });
  const apiServer = await http.createHttpDwebServer(jsProcess, { subdomain: "api", port: 443 });
  ;
  (await wwwServer.listen()).onRequest(wwwServerOnRequest);
  (await apiServer.listen()).onRequest(await createApiServerOnRequest(wwwServer.startResult.urlInfo.internal_origin, apiServer.startResult.urlInfo));
  {
    const interUrl = wwwServer.startResult.urlInfo.buildInternalUrl((url) => {
      url.pathname = "/index.html";
    }).href;
    console.log("cot#open interUrl=>", interUrl);
    const view_id = await jsProcess.nativeFetch(
      `file://mwebview.sys.dweb/open?url=${encodeURIComponent(interUrl)}`
    ).text();
  }
  {
    jsProcess.onConnect((ipc2) => {
      console.log("browser.worker.mts onConnect");
      ipc2.onEvent((event, ipc3) => {
        console.log("got event:", ipc3.remote.mmid, event.name, event.text);
        setTimeout(() => {
          ipc3.postMessage(IpcEvent2.fromText(event.name, "echo:" + event.text));
        }, 500);
      });
      ipc2.onMessage(() => {
        console.error("ipc onmessage");
      });
    });
  }
};
main();
