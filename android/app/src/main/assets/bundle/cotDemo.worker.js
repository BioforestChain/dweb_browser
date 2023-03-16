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
          const result = { ipc: new PromiseOut(), obs: /* @__PURE__ */ new Set() };
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
    console.log(`${remoteIpcResponse.statusCode} ${JSON.stringify(remoteIpcResponse.headers.toJSON())}`);
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
    console.log("cot#open interUrl=>", interUrl);
    const view_id = await jsProcess.nativeFetch(
      `file://mwebview.sys.dweb/open?url=${encodeURIComponent(interUrl)}`
    ).text();
  }
  {
  }
};
main();
