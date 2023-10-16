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
  function getFetchOpts(link) {
    const fetchOpts = {};
    if (link.integrity)
      fetchOpts.integrity = link.integrity;
    if (link.referrerPolicy)
      fetchOpts.referrerPolicy = link.referrerPolicy;
    if (link.crossOrigin === "use-credentials")
      fetchOpts.credentials = "include";
    else if (link.crossOrigin === "anonymous")
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
const scriptRel = "modulepreload";
const assetsURL = function(dep, importerUrl) {
  return new URL(dep, importerUrl).href;
};
const seen = {};
const __vitePreload = function preload(baseModule, deps, importerUrl) {
  if (!deps || deps.length === 0) {
    return baseModule();
  }
  const links = document.getElementsByTagName("link");
  return Promise.all(deps.map((dep) => {
    dep = assetsURL(dep, importerUrl);
    if (dep in seen)
      return;
    seen[dep] = true;
    const isCss = dep.endsWith(".css");
    const cssSelector = isCss ? '[rel="stylesheet"]' : "";
    const isBaseRelative = !!importerUrl;
    if (isBaseRelative) {
      for (let i = links.length - 1; i >= 0; i--) {
        const link2 = links[i];
        if (link2.href === dep && (!isCss || link2.rel === "stylesheet")) {
          return;
        }
      }
    } else if (document.querySelector(`link[href="${dep}"]${cssSelector}`)) {
      return;
    }
    const link = document.createElement("link");
    link.rel = isCss ? "stylesheet" : scriptRel;
    if (!isCss) {
      link.as = "script";
      link.crossOrigin = "";
    }
    link.href = dep;
    document.head.appendChild(link);
    if (isCss) {
      return new Promise((res, rej) => {
        link.addEventListener("load", res);
        link.addEventListener("error", () => rej(new Error(`Unable to preload CSS for ${dep}`)));
      });
    }
  })).then(() => baseModule());
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
function windowEndpoint(w, context = globalThis, targetOrigin = "*") {
  return {
    postMessage: (msg, transferables) => w.postMessage(msg, targetOrigin, transferables),
    addEventListener: context.addEventListener.bind(context),
    removeEventListener: context.removeEventListener.bind(context)
  };
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
const Comlink = /* @__PURE__ */ Object.freeze(/* @__PURE__ */ Object.defineProperty({
  __proto__: null,
  createEndpoint,
  expose,
  finalizer,
  proxy,
  proxyMarker,
  releaseProxy,
  transfer,
  transferHandlers,
  windowEndpoint,
  wrap
}, Symbol.toStringTag, { value: "Module" }));
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
const importApis = once(() => wrap(import_port));
Object.assign(globalThis, { Comlink, importApis });
const openNativeWindow_preload = /* @__PURE__ */ Object.freeze(/* @__PURE__ */ Object.defineProperty({
  __proto__: null,
  exportApis,
  importApis,
  mainPort
}, Symbol.toStringTag, { value: "Module" }));
const allDeviceListMap = /* @__PURE__ */ new Map();
const mainApis = importApis();
const template = document.querySelector(".template");
let setTimeoutId;
let bluetooth;
let bluetoothRemoteGATTServer;
let bluetoothRemoteGATTService;
let bluetoothRemoteGATTCharacteristic;
let bluetoothRemoteGATTDescriptor;
let connectedSuccess;
let connectedFail;
let isConnecting = false;
async function requestDevice(requestDeviceOptions) {
  console.log("requestDeviceOptions", requestDeviceOptions);
  navigator.bluetooth.requestDevice(requestDeviceOptions).then((_bluetooth) => {
    var _a;
    if (_bluetooth !== void 0) {
      bluetooth = _bluetooth;
      return (_a = bluetooth.gatt) == null ? void 0 : _a.connect();
    }
    return Promise.reject(new Error(`_bluettoh === undefined`));
  }).then((server) => {
    console.log("server", server);
    bluetoothRemoteGATTServer = server;
    if (connectedSuccess === void 0)
      throw new Error(`connectedSuccess === undefined`);
    if (server === void 0)
      throw new Error("server === undefined");
    connectedSuccess(server);
    clearTimeout(setTimeoutId);
  }).catch((err) => {
    connectedFail ? connectedFail(err) : "";
    clearTimeout(setTimeoutId);
    console.error(`requestDevice fail: `, err);
  });
}
async function deviceDisconnect(id, resolveId) {
  if (bluetoothRemoteGATTServer === void 0) {
    mainApis.deviceDisconnectCallback(
      {
        success: false,
        error: `bluetoothRemoteGATTServer === undefined`,
        data: void 0
      },
      resolveId
    );
    return;
  }
  if (id === (bluetoothRemoteGATTServer == null ? void 0 : bluetoothRemoteGATTServer.device.id)) {
    bluetoothRemoteGATTServer.disconnect();
    mainApis.deviceDisconnectCallback(
      {
        success: true,
        error: void 0,
        data: true
      },
      resolveId
    );
    Array.from(allDeviceListMap.values()).forEach((item) => {
      item.el.classList.remove("connected");
      item.isConnected = false;
    });
    bluetoothRemoteGATTServer = void 0;
    return;
  }
  mainApis.deviceDisconnectCallback(
    {
      success: false,
      error: `bluetoothRemoteGATTServer.id !== 请求的id`,
      data: void 0
    },
    resolveId
  );
}
async function bluetoothRemoteGATTServerConnect(id, resolveId) {
  if (bluetoothRemoteGATTServer === void 0) {
    mainApis.operationCallback(
      {
        success: false,
        error: `bluetoothRemoteGATTServer === undefined`,
        data: void 0
      },
      resolveId
    );
    return;
  }
  if (id === bluetoothRemoteGATTServer.device.id) {
    bluetoothRemoteGATTServer.connect();
    mainApis.operationCallback(
      {
        success: true,
        error: void 0,
        data: "ok"
      },
      resolveId
    );
    return;
  }
  mainApis.operationCallback(
    {
      success: false,
      error: `bluetoothRemoteGATTServer === undefined`,
      data: void 0
    },
    resolveId
  );
}
async function bluetoothRemoteGATTServerDisconnect(id, resolveId) {
  if (bluetoothRemoteGATTServer === void 0) {
    mainApis.operationCallback(
      {
        success: false,
        error: `bluetoothRemoteGATTServer === undefined`,
        data: void 0
      },
      resolveId
    );
    return;
  }
  if (id === bluetoothRemoteGATTServer.device.id) {
    bluetoothRemoteGATTServer.disconnect();
    mainApis.operationCallback(
      {
        success: true,
        error: void 0,
        data: "ok"
      },
      resolveId
    );
    return;
  }
  mainApis.operationCallback(
    {
      success: false,
      error: `参数 id 没有匹配的 BluetoothRemoteGATTServer`,
      data: void 0
    },
    resolveId
  );
}
function createListItem(name, status) {
  if (template === null)
    throw new Error("tempalte === null");
  const fragment = template.content.cloneNode(true);
  fragment.querySelector(".name").innerText = name;
  fragment.querySelector(".status").innerText = status;
  return fragment.children[0];
}
async function devicesUpdate(list) {
  const ul = document.querySelector(".list_container");
  if (ul === null)
    throw new Error("ul === null");
  list.forEach((device) => {
    if (allDeviceListMap.has(device.deviceId)) {
      return;
    }
    const li = createListItem(device.deviceName, "未连接");
    li.addEventListener("click", async () => {
      console.log("点击了 li");
      if (isConnecting)
        return;
      deviceSelected(device);
    });
    allDeviceListMap.set(device.deviceId, {
      el: li,
      device,
      isConnecting: false,
      isConnected: false
    });
    ul.appendChild(li);
  });
}
async function deviceSelected(device) {
  mainApis.deviceSelected(device);
  isConnecting = true;
  Array.from(allDeviceListMap.values()).forEach((oldDevice) => {
    if (oldDevice.device.deviceId === device.deviceId) {
      oldDevice.el.classList.remove("connected");
      oldDevice.el.classList.add("connecting");
      oldDevice.isConnecting = true;
      oldDevice.isConnected = false;
      connectedFail = (err) => {
        oldDevice.el.classList.remove("connecting");
        oldDevice.isConnecting = false;
        isConnecting = false;
        mainApis.deviceConnectedCallback({
          success: false,
          error: err.message,
          data: void 0
        });
      };
      setTimeoutId = setTimeout(() => {
        oldDevice.el.classList.remove("connecting");
        oldDevice.isConnecting = false;
        isConnecting = false;
        mainApis.deviceConnectedCallback({
          success: false,
          error: `超时`,
          data: void 0
        });
        console.error("连接超时");
      }, 6e3);
      connectedSuccess = (server) => {
        oldDevice.el.classList.remove("connecting");
        oldDevice.el.classList.add("connected");
        oldDevice.isConnecting = false;
        oldDevice.isConnected = true;
        isConnecting = false;
        mainApis.deviceConnectedCallback({
          success: true,
          error: void 0,
          data: {
            device: { id: server.device.id, name: server.device.name }
          }
        });
      };
    } else {
      oldDevice.el.classList.remove("connecting");
      oldDevice.el.classList.remove("connected");
      oldDevice.isConnecting = false;
      oldDevice.isConnected = false;
    }
  });
}
async function bluetoothRemoteGATTServerGetPrimarySevice(uuid, resolveId) {
  if (bluetoothRemoteGATTServer === void 0) {
    mainApis.operationCallback(
      {
        success: false,
        error: `bluetoothRemoteGATTServer === undefined`,
        data: void 0
      },
      resolveId
    );
    return;
  }
  bluetoothRemoteGATTServer.getPrimaryService(uuid).then(
    (_bluetoothRemoteGATTService) => {
      bluetoothRemoteGATTService = _bluetoothRemoteGATTService;
      mainApis.operationCallback(
        {
          success: true,
          error: void 0,
          data: {
            device: {
              id: bluetoothRemoteGATTService.device.id,
              name: bluetoothRemoteGATTService.device.name
            },
            isPrimary: bluetoothRemoteGATTService.isPrimary,
            uuid
          }
        },
        resolveId
      );
    },
    (error) => {
      bluetoothRemoteGATTService = void 0;
      mainApis.operationCallback(
        {
          success: false,
          error: error.message,
          data: void 0
        },
        resolveId
      );
    }
  );
}
async function bluetoothRemoteGATTService_getCharacteristic(uuid, resolveId) {
  if (bluetoothRemoteGATTService === void 0) {
    mainApis.operationCallback(
      {
        success: false,
        error: `bluetoothRemoteGATTService === undefined`,
        data: void 0
      },
      resolveId
    );
    return;
  }
  bluetoothRemoteGATTService.getCharacteristic(uuid).then(
    async (_bluetoothRemoteGATTCharacteristic) => {
      bluetoothRemoteGATTCharacteristic = _bluetoothRemoteGATTCharacteristic;
      mainApis.operationCallback(
        {
          success: true,
          error: void 0,
          data: {
            uuid,
            properties: {
              authenticatedSignedWrites: bluetoothRemoteGATTCharacteristic.properties.authenticatedSignedWrites,
              broadcast: bluetoothRemoteGATTCharacteristic.properties.broadcast,
              indicate: bluetoothRemoteGATTCharacteristic.properties.indicate,
              notify: bluetoothRemoteGATTCharacteristic.properties.notify,
              read: bluetoothRemoteGATTCharacteristic.properties.read,
              reliableWrite: bluetoothRemoteGATTCharacteristic.properties.reliableWrite,
              writableAuxiliaries: bluetoothRemoteGATTCharacteristic.properties.writableAuxiliaries,
              write: bluetoothRemoteGATTCharacteristic.properties.write,
              writeWithoutResponse: bluetoothRemoteGATTCharacteristic.properties.writeWithoutResponse
            },
            value: bluetoothRemoteGATTCharacteristic.value
          }
        },
        resolveId
      );
    },
    (err) => {
      bluetoothRemoteGATTCharacteristic = void 0;
      mainApis.operationCallback(
        {
          success: false,
          error: err.message,
          data: void 0
        },
        resolveId
      );
    }
  );
}
function bluetoothRemoteGATTCharacteristic_readValue(resolveId) {
  if (bluetoothRemoteGATTCharacteristic === void 0) {
    mainApis.operationCallback(
      {
        success: false,
        error: `bluetoothRemoteGATTService === undefined`,
        data: void 0
      },
      resolveId
    );
    return;
  }
  bluetoothRemoteGATTCharacteristic.readValue().then(
    (res) => {
      console.log("readValue: ", res);
      mainApis.operationCallback(
        {
          success: true,
          error: void 0,
          data: res
        },
        resolveId
      );
    },
    (err) => {
      console.error("characteristicRaadValue error", err);
      mainApis.operationCallback(
        {
          success: false,
          error: err.message,
          data: void 0
        },
        resolveId
      );
      return;
    }
  );
}
function BluetoothRemoteGATTCharacteristic_getDescriptor(uuid, resolveId) {
  if (bluetoothRemoteGATTCharacteristic === void 0) {
    mainApis.operationCallback(
      {
        success: false,
        error: `bluetoothRemoteGATTService === undefined`,
        data: void 0
      },
      resolveId
    );
    return;
  }
  bluetoothRemoteGATTCharacteristic.getDescriptor(uuid).then(
    (_bluetoothRemoteGATTDescriptor) => {
      bluetoothRemoteGATTDescriptor = _bluetoothRemoteGATTDescriptor;
      mainApis.operationCallback(
        {
          success: true,
          error: void 0,
          data: {
            uuid,
            value: bluetoothRemoteGATTDescriptor.value
          }
        },
        resolveId
      );
      return;
    },
    (err) => {
      mainApis.operationCallback(
        {
          success: false,
          error: err.message,
          data: void 0
        },
        resolveId
      );
      return;
    }
  );
}
async function descriptorReadValue(resolveId) {
  if (bluetoothRemoteGATTDescriptor === void 0) {
    mainApis.descriptorReadValueCallback(
      {
        success: false,
        error: `bluetoothRemoteGATTDescriptor === undefined`,
        data: void 0
      },
      resolveId
    );
    return;
  }
  bluetoothRemoteGATTDescriptor.readValue().then(
    (value) => {
      mainApis.descriptorReadValueCallback(
        {
          success: false,
          error: void 0,
          data: value
        },
        resolveId
      );
    },
    (err) => {
      mainApis.descriptorReadValueCallback(
        {
          success: false,
          error: err.message,
          data: void 0
        },
        resolveId
      );
    }
  );
}
async function deviceSelectedFailCallback() {
  Array.from(allDeviceListMap.values()).forEach((item) => {
    item.el.classList.remove("connecting");
    item.isConnecting = false;
  });
}
const APIS = {
  devicesUpdate,
  requestDevice,
  deviceDisconnect,
  bluetoothRemoteGATTServerConnect,
  bluetoothRemoteGATTServerDisconnect,
  bluetoothRemoteGATTServerGetPrimarySevice,
  bluetoothRemoteGATTService_getCharacteristic,
  bluetoothRemoteGATTCharacteristic_readValue,
  BluetoothRemoteGATTCharacteristic_getDescriptor,
  deviceSelectedFailCallback,
  descriptorReadValue
};
Object.assign(globalThis, APIS);
if ("ipcRenderer" in self) {
  (async () => {
    const { exportApis: exportApis2 } = await __vitePreload(() => Promise.resolve().then(() => openNativeWindow_preload), true ? void 0 : void 0, import.meta.url);
    exportApis2(globalThis);
  })();
}
