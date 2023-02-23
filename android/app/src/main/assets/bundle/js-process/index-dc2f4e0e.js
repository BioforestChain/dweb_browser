(function polyfill() {
  const relList = document.createElement("link").relList;
  if (relList && relList.supports && relList.supports("modulepreload")) {
    return;
  }
  for (const link of document.querySelectorAll('link[rel="modulepreload"]')) {
    processPreload(link);
  }
  new MutationObserver((mutations) => {
    for (const mutation of mutations) {
      if (mutation.type !== "childList") {
        continue;
      }
      for (const node of mutation.addedNodes) {
        if (node.tagName === "LINK" && node.rel === "modulepreload")
          processPreload(node);
      }
    }
  }).observe(document, { childList: true, subtree: true });
  function getFetchOpts(script) {
    const fetchOpts = {};
    if (script.integrity)
      fetchOpts.integrity = script.integrity;
    if (script.referrerpolicy)
      fetchOpts.referrerPolicy = script.referrerpolicy;
    if (script.crossorigin === "use-credentials")
      fetchOpts.credentials = "include";
    else if (script.crossorigin === "anonymous")
      fetchOpts.credentials = "omit";
    else
      fetchOpts.credentials = "same-origin";
    return fetchOpts;
  }
  function processPreload(link) {
    if (link.ep)
      return;
    link.ep = true;
    const fetchOpts = getFetchOpts(link);
    fetch(link.href, fetchOpts);
  }
})();
const createSignal = () => {
  return new Signal();
};
class Signal {
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
}
const electron = typeof require !== "undefined" ? require("electron") : function nodeIntegrationWarn() {
  console.error(`If you need to use "electron" in the Renderer process, make sure that "nodeIntegration" is enabled in the Main process.`);
  return {
    // TODO: polyfill
  };
}();
let _ipcRenderer;
if (typeof document === "undefined") {
  _ipcRenderer = {};
  const keys = [
    "invoke",
    "postMessage",
    "send",
    "sendSync",
    "sendTo",
    "sendToHost",
    // propertype
    "addListener",
    "emit",
    "eventNames",
    "getMaxListeners",
    "listenerCount",
    "listeners",
    "off",
    "on",
    "once",
    "prependListener",
    "prependOnceListener",
    "rawListeners",
    "removeAllListeners",
    "removeListener",
    "setMaxListeners"
  ];
  for (const key of keys) {
    _ipcRenderer[key] = () => {
      throw new Error(
        "ipcRenderer doesn't work in a Web Worker.\nYou can see https://github.com/electron-vite/vite-plugin-electron/issues/69"
      );
    };
  }
} else {
  _ipcRenderer = electron.ipcRenderer;
}
electron.clipboard;
electron.contextBridge;
electron.crashReporter;
const ipcRenderer = _ipcRenderer;
electron.nativeImage;
electron.shell;
electron.webFrame;
electron.deprecate;
const updateRenderPort = (port) => {
  updateRenderMessageListener(port, "addEventListener", 1);
  updateRenderMessageListener(port, "removeEventListener", 1);
  updateRenderPostMessage(port);
  return port;
};
const updateRenderMessageListener = (target, method, listener_index) => {
  const source_method = target[method];
  target[method] = function(...args) {
    args[listener_index] = resolveRenderMessageListener(args[listener_index]);
    return source_method.apply(this, args);
  };
  return target;
};
const wm_renderListener = /* @__PURE__ */ new WeakMap();
const resolveRenderMessageListener = (listener) => {
  if (typeof listener === "object") {
    listener.handleEvent = resolveRenderMessageListener(listener.handleEvent);
    return listener;
  }
  let resolveListener = wm_renderListener.get(listener);
  if (resolveListener === void 0) {
    resolveListener = function(event) {
      JSON.stringify(event.data, function resolver(key, value) {
        if (Array.isArray(value) && value[0] === "#PORT#") {
          this[key] = event.ports[value[1]];
          return null;
        }
        return value;
      });
      return listener.call(this, event);
    };
    wm_renderListener.set(listener, resolveListener);
    wm_renderListener.set(resolveListener, listener);
  }
  return resolveListener;
};
const updateRenderPostMessage = (target) => {
  const postMessage = target.postMessage;
  target.postMessage = function(message, transfer2) {
    if (Array.isArray(transfer2)) {
      JSON.stringify(message, function replacer(key, value) {
        if (value && typeof value === "object" && "postMessage" in value) {
          const index = transfer2.indexOf(value);
          if (index !== -1) {
            this[key] = ["#PORT#", index];
            return null;
          }
        }
        return value;
      });
      postMessage.call(this, message, transfer2);
    } else if (transfer2) {
      postMessage.call(this, message, transfer2);
    } else {
      postMessage.call(this, message);
    }
  };
  return target;
};
/**
 * @license
 * Copyright 2019 Google LLC
 * SPDX-License-Identifier: Apache-2.0
 */
const proxyMarker = Symbol("Comlink.proxy");
const createEndpoint = Symbol("Comlink.endpoint");
const releaseProxy = Symbol("Comlink.releaseProxy");
const finalizer = Symbol("Comlink.finalizer");
const throwMarker = Symbol("Comlink.thrown");
const isObject = (val) => typeof val === "object" && val !== null || typeof val === "function";
const proxyTransferHandler = {
  canHandle: (val) => isObject(val) && val[proxyMarker],
  serialize(obj) {
    const { port1, port2 } = new MessageChannel();
    expose(obj, port1);
    return [port2, [port2]];
  },
  deserialize(port) {
    port.start();
    return wrap(port);
  }
};
const throwTransferHandler = {
  canHandle: (value) => isObject(value) && throwMarker in value,
  serialize({ value }) {
    let serialized;
    if (value instanceof Error) {
      serialized = {
        isError: true,
        value: {
          message: value.message,
          name: value.name,
          stack: value.stack
        }
      };
    } else {
      serialized = { isError: false, value };
    }
    return [serialized, []];
  },
  deserialize(serialized) {
    if (serialized.isError) {
      throw Object.assign(new Error(serialized.value.message), serialized.value);
    }
    throw serialized.value;
  }
};
const transferHandlers = /* @__PURE__ */ new Map([
  ["proxy", proxyTransferHandler],
  ["throw", throwTransferHandler]
]);
function isAllowedOrigin(allowedOrigins, origin) {
  for (const allowedOrigin of allowedOrigins) {
    if (origin === allowedOrigin || allowedOrigin === "*") {
      return true;
    }
    if (allowedOrigin instanceof RegExp && allowedOrigin.test(origin)) {
      return true;
    }
  }
  return false;
}
function expose(obj, ep = globalThis, allowedOrigins = ["*"]) {
  ep.addEventListener("message", function callback(ev) {
    if (!ev || !ev.data) {
      return;
    }
    if (!isAllowedOrigin(allowedOrigins, ev.origin)) {
      console.warn(`Invalid origin '${ev.origin}' for comlink proxy`);
      return;
    }
    const { id, type, path } = Object.assign({ path: [] }, ev.data);
    const argumentList = (ev.data.argumentList || []).map(fromWireValue);
    let returnValue;
    try {
      const parent = path.slice(0, -1).reduce((obj2, prop) => obj2[prop], obj);
      const rawValue = path.reduce((obj2, prop) => obj2[prop], obj);
      switch (type) {
        case "GET":
          {
            returnValue = rawValue;
          }
          break;
        case "SET":
          {
            parent[path.slice(-1)[0]] = fromWireValue(ev.data.value);
            returnValue = true;
          }
          break;
        case "APPLY":
          {
            returnValue = rawValue.apply(parent, argumentList);
          }
          break;
        case "CONSTRUCT":
          {
            const value = new rawValue(...argumentList);
            returnValue = proxy(value);
          }
          break;
        case "ENDPOINT":
          {
            const { port1, port2 } = new MessageChannel();
            expose(obj, port2);
            returnValue = transfer(port1, [port1]);
          }
          break;
        case "RELEASE":
          {
            returnValue = void 0;
          }
          break;
        default:
          return;
      }
    } catch (value) {
      returnValue = { value, [throwMarker]: 0 };
    }
    Promise.resolve(returnValue).catch((value) => {
      return { value, [throwMarker]: 0 };
    }).then((returnValue2) => {
      const [wireValue, transferables] = toWireValue(returnValue2);
      ep.postMessage(Object.assign(Object.assign({}, wireValue), { id }), transferables);
      if (type === "RELEASE") {
        ep.removeEventListener("message", callback);
        closeEndPoint(ep);
        if (finalizer in obj && typeof obj[finalizer] === "function") {
          obj[finalizer]();
        }
      }
    }).catch((error) => {
      const [wireValue, transferables] = toWireValue({
        value: new TypeError("Unserializable return value"),
        [throwMarker]: 0
      });
      ep.postMessage(Object.assign(Object.assign({}, wireValue), { id }), transferables);
    });
  });
  if (ep.start) {
    ep.start();
  }
}
function isMessagePort(endpoint) {
  return endpoint.constructor.name === "MessagePort";
}
function closeEndPoint(endpoint) {
  if (isMessagePort(endpoint))
    endpoint.close();
}
function wrap(ep, target) {
  return createProxy(ep, [], target);
}
function throwIfProxyReleased(isReleased) {
  if (isReleased) {
    throw new Error("Proxy has been released and is not useable");
  }
}
function releaseEndpoint(ep) {
  return requestResponseMessage(ep, {
    type: "RELEASE"
  }).then(() => {
    closeEndPoint(ep);
  });
}
const proxyCounter = /* @__PURE__ */ new WeakMap();
const proxyFinalizers = "FinalizationRegistry" in globalThis && new FinalizationRegistry((ep) => {
  const newCount = (proxyCounter.get(ep) || 0) - 1;
  proxyCounter.set(ep, newCount);
  if (newCount === 0) {
    releaseEndpoint(ep);
  }
});
function registerProxy(proxy2, ep) {
  const newCount = (proxyCounter.get(ep) || 0) + 1;
  proxyCounter.set(ep, newCount);
  if (proxyFinalizers) {
    proxyFinalizers.register(proxy2, ep, proxy2);
  }
}
function unregisterProxy(proxy2) {
  if (proxyFinalizers) {
    proxyFinalizers.unregister(proxy2);
  }
}
function createProxy(ep, path = [], target = function() {
}) {
  let isProxyReleased = false;
  const proxy2 = new Proxy(target, {
    get(_target, prop) {
      throwIfProxyReleased(isProxyReleased);
      if (prop === releaseProxy) {
        return () => {
          unregisterProxy(proxy2);
          releaseEndpoint(ep);
          isProxyReleased = true;
        };
      }
      if (prop === "then") {
        if (path.length === 0) {
          return { then: () => proxy2 };
        }
        const r = requestResponseMessage(ep, {
          type: "GET",
          path: path.map((p) => p.toString())
        }).then(fromWireValue);
        return r.then.bind(r);
      }
      return createProxy(ep, [...path, prop]);
    },
    set(_target, prop, rawValue) {
      throwIfProxyReleased(isProxyReleased);
      const [value, transferables] = toWireValue(rawValue);
      return requestResponseMessage(ep, {
        type: "SET",
        path: [...path, prop].map((p) => p.toString()),
        value
      }, transferables).then(fromWireValue);
    },
    apply(_target, _thisArg, rawArgumentList) {
      throwIfProxyReleased(isProxyReleased);
      const last = path[path.length - 1];
      if (last === createEndpoint) {
        return requestResponseMessage(ep, {
          type: "ENDPOINT"
        }).then(fromWireValue);
      }
      if (last === "bind") {
        return createProxy(ep, path.slice(0, -1));
      }
      const [argumentList, transferables] = processArguments(rawArgumentList);
      return requestResponseMessage(ep, {
        type: "APPLY",
        path: path.map((p) => p.toString()),
        argumentList
      }, transferables).then(fromWireValue);
    },
    construct(_target, rawArgumentList) {
      throwIfProxyReleased(isProxyReleased);
      const [argumentList, transferables] = processArguments(rawArgumentList);
      return requestResponseMessage(ep, {
        type: "CONSTRUCT",
        path: path.map((p) => p.toString()),
        argumentList
      }, transferables).then(fromWireValue);
    }
  });
  registerProxy(proxy2, ep);
  return proxy2;
}
function myFlat(arr) {
  return Array.prototype.concat.apply([], arr);
}
function processArguments(argumentList) {
  const processed = argumentList.map(toWireValue);
  return [processed.map((v) => v[0]), myFlat(processed.map((v) => v[1]))];
}
const transferCache = /* @__PURE__ */ new WeakMap();
function transfer(obj, transfers) {
  transferCache.set(obj, transfers);
  return obj;
}
function proxy(obj) {
  return Object.assign(obj, { [proxyMarker]: true });
}
function toWireValue(value) {
  for (const [name, handler] of transferHandlers) {
    if (handler.canHandle(value)) {
      const [serializedValue, transferables] = handler.serialize(value);
      return [
        {
          type: "HANDLER",
          name,
          value: serializedValue
        },
        transferables
      ];
    }
  }
  return [
    {
      type: "RAW",
      value
    },
    transferCache.get(value) || []
  ];
}
function fromWireValue(value) {
  switch (value.type) {
    case "HANDLER":
      return transferHandlers.get(value.name).deserialize(value.value);
    case "RAW":
      return value.value;
  }
}
function requestResponseMessage(ep, msg, transfers) {
  return new Promise((resolve) => {
    const id = generateUUID();
    ep.addEventListener("message", function l(ev) {
      if (!ev.data || !ev.data.id || ev.data.id !== id) {
        return;
      }
      ep.removeEventListener("message", l);
      resolve(ev.data);
    });
    if (ep.start) {
      ep.start();
    }
    ep.postMessage(Object.assign({ id }, msg), transfers);
  });
}
function generateUUID() {
  return new Array(4).fill(0).map(() => Math.floor(Math.random() * Number.MAX_SAFE_INTEGER).toString(16)).join("-");
}
const once = (fn) => {
  let runed = false;
  let result;
  return (...args) => {
    if (runed === false) {
      runed = true;
      result = fn(...args);
    }
    return result;
  };
};
const export_channel = new MessageChannel();
const import_channel = new MessageChannel();
const export_port = export_channel.port1;
const import_port = import_channel.port1;
ipcRenderer.postMessage("renderPort", {}, [
  export_channel.port2,
  import_channel.port2
]);
updateRenderPort(export_port);
updateRenderPort(import_port);
const mainPort = export_port;
let apis = {};
const start = () => {
  expose(apis, mainPort);
};
Object.assign(globalThis, { mainPort, start });
const exportApis = once((APIS2) => {
  apis = APIS2;
  start();
});
wrap(import_port);
class PromiseOut {
  constructor() {
    this.promise = new Promise((resolve, reject) => {
      this.resolve = resolve;
      this.reject = reject;
    });
  }
}
const ALL_PROCESS_MAP = /* @__PURE__ */ new Map();
let acc_process_id = 0;
const allocProcessId = () => acc_process_id++;
const createProcess = async (env_script_url, fetch_port) => {
  const process_id = allocProcessId();
  const worker = new Worker(env_script_url, { type: "module" });
  worker.postMessage(["fetch-ipc-channel", fetch_port], [fetch_port]);
  const env_ready_po = new PromiseOut();
  const onEnvReady = (event) => {
    if (Array.isArray(event.data) && event.data[0] === "env-ready") {
      env_ready_po.resolve();
    }
  };
  worker.addEventListener("message", onEnvReady);
  await env_ready_po.promise;
  worker.removeEventListener("message", onEnvReady);
  ALL_PROCESS_MAP.set(process_id, { worker, fetch_port });
  on_create_process_signal.emit({
    process_id,
    env_script_url
  });
  return {
    process_id
  };
};
const _forceGetProcess = (process_id) => {
  const process = ALL_PROCESS_MAP.get(process_id);
  if (process === void 0) {
    throw new Error(`no found worker by id: ${process_id}`);
  }
  return process;
};
const runProcessMain = (process_id, config) => {
  const process = _forceGetProcess(process_id);
  process.worker.postMessage(["run-main", config]);
};
const createIpc = (process_id) => {
  const process = _forceGetProcess(process_id);
  const channel = new MessageChannel();
  process.worker.postMessage(["ipc-channel", channel.port2], [channel.port2]);
  return channel.port1;
};
const on_create_process_signal = createSignal();
const APIS = {
  createProcess,
  runProcessMain,
  createIpc
};
Object.assign(globalThis, APIS);
const html = String.raw;
on_create_process_signal.listen(({ process_id, env_script_url }) => {
  document.body.innerHTML += html`<div>
    <span>PID:${process_id}</span>
    <span>URL:${env_script_url}</span>
  </div>`;
});
exportApis(APIS);
