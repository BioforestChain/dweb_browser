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
        const r2 = requestResponseMessage(ep, {
          type: "GET",
          path: path.map((p2) => p2.toString())
        }).then(fromWireValue);
        return r2.then.bind(r2);
      }
      return createProxy(ep, [...path, prop]);
    },
    set(_target, prop, rawValue) {
      throwIfProxyReleased(isProxyReleased);
      const [value, transferables] = toWireValue(rawValue);
      return requestResponseMessage(ep, {
        type: "SET",
        path: [...path, prop].map((p2) => p2.toString()),
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
        path: path.map((p2) => p2.toString()),
        argumentList
      }, transferables).then(fromWireValue);
    },
    construct(_target, rawArgumentList) {
      throwIfProxyReleased(isProxyReleased);
      const [argumentList, transferables] = processArguments(rawArgumentList);
      return requestResponseMessage(ep, {
        type: "CONSTRUCT",
        path: path.map((p2) => p2.toString()),
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
  return [processed.map((v2) => v2[0]), myFlat(processed.map((v2) => v2[1]))];
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
    ep.addEventListener("message", function l2(ev) {
      if (!ev.data || !ev.data.id || ev.data.id !== id) {
        return;
      }
      ep.removeEventListener("message", l2);
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
/**
 * @license
 * Copyright 2019 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */
const t$2 = window, e$5 = t$2.ShadowRoot && (void 0 === t$2.ShadyCSS || t$2.ShadyCSS.nativeShadow) && "adoptedStyleSheets" in Document.prototype && "replace" in CSSStyleSheet.prototype, s$4 = Symbol(), n$4 = /* @__PURE__ */ new WeakMap();
let o$3 = class o {
  constructor(t2, e2, n2) {
    if (this._$cssResult$ = true, n2 !== s$4)
      throw Error("CSSResult is not constructable. Use `unsafeCSS` or `css` instead.");
    this.cssText = t2, this.t = e2;
  }
  get styleSheet() {
    let t2 = this.o;
    const s3 = this.t;
    if (e$5 && void 0 === t2) {
      const e2 = void 0 !== s3 && 1 === s3.length;
      e2 && (t2 = n$4.get(s3)), void 0 === t2 && ((this.o = t2 = new CSSStyleSheet()).replaceSync(this.cssText), e2 && n$4.set(s3, t2));
    }
    return t2;
  }
  toString() {
    return this.cssText;
  }
};
const r$3 = (t2) => new o$3("string" == typeof t2 ? t2 : t2 + "", void 0, s$4), i$4 = (t2, ...e2) => {
  const n2 = 1 === t2.length ? t2[0] : e2.reduce((e3, s3, n3) => e3 + ((t3) => {
    if (true === t3._$cssResult$)
      return t3.cssText;
    if ("number" == typeof t3)
      return t3;
    throw Error("Value passed to 'css' function must be a 'css' function result: " + t3 + ". Use 'unsafeCSS' to pass non-literal values, but take care to ensure page security.");
  })(s3) + t2[n3 + 1], t2[0]);
  return new o$3(n2, t2, s$4);
}, S$1 = (s3, n2) => {
  e$5 ? s3.adoptedStyleSheets = n2.map((t2) => t2 instanceof CSSStyleSheet ? t2 : t2.styleSheet) : n2.forEach((e2) => {
    const n3 = document.createElement("style"), o3 = t$2.litNonce;
    void 0 !== o3 && n3.setAttribute("nonce", o3), n3.textContent = e2.cssText, s3.appendChild(n3);
  });
}, c$3 = e$5 ? (t2) => t2 : (t2) => t2 instanceof CSSStyleSheet ? ((t3) => {
  let e2 = "";
  for (const s3 of t3.cssRules)
    e2 += s3.cssText;
  return r$3(e2);
})(t2) : t2;
/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */
var s$3;
const e$4 = window, r$2 = e$4.trustedTypes, h$1 = r$2 ? r$2.emptyScript : "", o$2 = e$4.reactiveElementPolyfillSupport, n$3 = { toAttribute(t2, i3) {
  switch (i3) {
    case Boolean:
      t2 = t2 ? h$1 : null;
      break;
    case Object:
    case Array:
      t2 = null == t2 ? t2 : JSON.stringify(t2);
  }
  return t2;
}, fromAttribute(t2, i3) {
  let s3 = t2;
  switch (i3) {
    case Boolean:
      s3 = null !== t2;
      break;
    case Number:
      s3 = null === t2 ? null : Number(t2);
      break;
    case Object:
    case Array:
      try {
        s3 = JSON.parse(t2);
      } catch (t3) {
        s3 = null;
      }
  }
  return s3;
} }, a$1 = (t2, i3) => i3 !== t2 && (i3 == i3 || t2 == t2), l$3 = { attribute: true, type: String, converter: n$3, reflect: false, hasChanged: a$1 };
let d$1 = class d extends HTMLElement {
  constructor() {
    super(), this._$Ei = /* @__PURE__ */ new Map(), this.isUpdatePending = false, this.hasUpdated = false, this._$El = null, this.u();
  }
  static addInitializer(t2) {
    var i3;
    this.finalize(), (null !== (i3 = this.h) && void 0 !== i3 ? i3 : this.h = []).push(t2);
  }
  static get observedAttributes() {
    this.finalize();
    const t2 = [];
    return this.elementProperties.forEach((i3, s3) => {
      const e2 = this._$Ep(s3, i3);
      void 0 !== e2 && (this._$Ev.set(e2, s3), t2.push(e2));
    }), t2;
  }
  static createProperty(t2, i3 = l$3) {
    if (i3.state && (i3.attribute = false), this.finalize(), this.elementProperties.set(t2, i3), !i3.noAccessor && !this.prototype.hasOwnProperty(t2)) {
      const s3 = "symbol" == typeof t2 ? Symbol() : "__" + t2, e2 = this.getPropertyDescriptor(t2, s3, i3);
      void 0 !== e2 && Object.defineProperty(this.prototype, t2, e2);
    }
  }
  static getPropertyDescriptor(t2, i3, s3) {
    return { get() {
      return this[i3];
    }, set(e2) {
      const r2 = this[t2];
      this[i3] = e2, this.requestUpdate(t2, r2, s3);
    }, configurable: true, enumerable: true };
  }
  static getPropertyOptions(t2) {
    return this.elementProperties.get(t2) || l$3;
  }
  static finalize() {
    if (this.hasOwnProperty("finalized"))
      return false;
    this.finalized = true;
    const t2 = Object.getPrototypeOf(this);
    if (t2.finalize(), void 0 !== t2.h && (this.h = [...t2.h]), this.elementProperties = new Map(t2.elementProperties), this._$Ev = /* @__PURE__ */ new Map(), this.hasOwnProperty("properties")) {
      const t3 = this.properties, i3 = [...Object.getOwnPropertyNames(t3), ...Object.getOwnPropertySymbols(t3)];
      for (const s3 of i3)
        this.createProperty(s3, t3[s3]);
    }
    return this.elementStyles = this.finalizeStyles(this.styles), true;
  }
  static finalizeStyles(i3) {
    const s3 = [];
    if (Array.isArray(i3)) {
      const e2 = new Set(i3.flat(1 / 0).reverse());
      for (const i4 of e2)
        s3.unshift(c$3(i4));
    } else
      void 0 !== i3 && s3.push(c$3(i3));
    return s3;
  }
  static _$Ep(t2, i3) {
    const s3 = i3.attribute;
    return false === s3 ? void 0 : "string" == typeof s3 ? s3 : "string" == typeof t2 ? t2.toLowerCase() : void 0;
  }
  u() {
    var t2;
    this._$E_ = new Promise((t3) => this.enableUpdating = t3), this._$AL = /* @__PURE__ */ new Map(), this._$Eg(), this.requestUpdate(), null === (t2 = this.constructor.h) || void 0 === t2 || t2.forEach((t3) => t3(this));
  }
  addController(t2) {
    var i3, s3;
    (null !== (i3 = this._$ES) && void 0 !== i3 ? i3 : this._$ES = []).push(t2), void 0 !== this.renderRoot && this.isConnected && (null === (s3 = t2.hostConnected) || void 0 === s3 || s3.call(t2));
  }
  removeController(t2) {
    var i3;
    null === (i3 = this._$ES) || void 0 === i3 || i3.splice(this._$ES.indexOf(t2) >>> 0, 1);
  }
  _$Eg() {
    this.constructor.elementProperties.forEach((t2, i3) => {
      this.hasOwnProperty(i3) && (this._$Ei.set(i3, this[i3]), delete this[i3]);
    });
  }
  createRenderRoot() {
    var t2;
    const s3 = null !== (t2 = this.shadowRoot) && void 0 !== t2 ? t2 : this.attachShadow(this.constructor.shadowRootOptions);
    return S$1(s3, this.constructor.elementStyles), s3;
  }
  connectedCallback() {
    var t2;
    void 0 === this.renderRoot && (this.renderRoot = this.createRenderRoot()), this.enableUpdating(true), null === (t2 = this._$ES) || void 0 === t2 || t2.forEach((t3) => {
      var i3;
      return null === (i3 = t3.hostConnected) || void 0 === i3 ? void 0 : i3.call(t3);
    });
  }
  enableUpdating(t2) {
  }
  disconnectedCallback() {
    var t2;
    null === (t2 = this._$ES) || void 0 === t2 || t2.forEach((t3) => {
      var i3;
      return null === (i3 = t3.hostDisconnected) || void 0 === i3 ? void 0 : i3.call(t3);
    });
  }
  attributeChangedCallback(t2, i3, s3) {
    this._$AK(t2, s3);
  }
  _$EO(t2, i3, s3 = l$3) {
    var e2;
    const r2 = this.constructor._$Ep(t2, s3);
    if (void 0 !== r2 && true === s3.reflect) {
      const h2 = (void 0 !== (null === (e2 = s3.converter) || void 0 === e2 ? void 0 : e2.toAttribute) ? s3.converter : n$3).toAttribute(i3, s3.type);
      this._$El = t2, null == h2 ? this.removeAttribute(r2) : this.setAttribute(r2, h2), this._$El = null;
    }
  }
  _$AK(t2, i3) {
    var s3;
    const e2 = this.constructor, r2 = e2._$Ev.get(t2);
    if (void 0 !== r2 && this._$El !== r2) {
      const t3 = e2.getPropertyOptions(r2), h2 = "function" == typeof t3.converter ? { fromAttribute: t3.converter } : void 0 !== (null === (s3 = t3.converter) || void 0 === s3 ? void 0 : s3.fromAttribute) ? t3.converter : n$3;
      this._$El = r2, this[r2] = h2.fromAttribute(i3, t3.type), this._$El = null;
    }
  }
  requestUpdate(t2, i3, s3) {
    let e2 = true;
    void 0 !== t2 && (((s3 = s3 || this.constructor.getPropertyOptions(t2)).hasChanged || a$1)(this[t2], i3) ? (this._$AL.has(t2) || this._$AL.set(t2, i3), true === s3.reflect && this._$El !== t2 && (void 0 === this._$EC && (this._$EC = /* @__PURE__ */ new Map()), this._$EC.set(t2, s3))) : e2 = false), !this.isUpdatePending && e2 && (this._$E_ = this._$Ej());
  }
  async _$Ej() {
    this.isUpdatePending = true;
    try {
      await this._$E_;
    } catch (t3) {
      Promise.reject(t3);
    }
    const t2 = this.scheduleUpdate();
    return null != t2 && await t2, !this.isUpdatePending;
  }
  scheduleUpdate() {
    return this.performUpdate();
  }
  performUpdate() {
    var t2;
    if (!this.isUpdatePending)
      return;
    this.hasUpdated, this._$Ei && (this._$Ei.forEach((t3, i4) => this[i4] = t3), this._$Ei = void 0);
    let i3 = false;
    const s3 = this._$AL;
    try {
      i3 = this.shouldUpdate(s3), i3 ? (this.willUpdate(s3), null === (t2 = this._$ES) || void 0 === t2 || t2.forEach((t3) => {
        var i4;
        return null === (i4 = t3.hostUpdate) || void 0 === i4 ? void 0 : i4.call(t3);
      }), this.update(s3)) : this._$Ek();
    } catch (t3) {
      throw i3 = false, this._$Ek(), t3;
    }
    i3 && this._$AE(s3);
  }
  willUpdate(t2) {
  }
  _$AE(t2) {
    var i3;
    null === (i3 = this._$ES) || void 0 === i3 || i3.forEach((t3) => {
      var i4;
      return null === (i4 = t3.hostUpdated) || void 0 === i4 ? void 0 : i4.call(t3);
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
    void 0 !== this._$EC && (this._$EC.forEach((t3, i3) => this._$EO(i3, this[i3], t3)), this._$EC = void 0), this._$Ek();
  }
  updated(t2) {
  }
  firstUpdated(t2) {
  }
};
d$1.finalized = true, d$1.elementProperties = /* @__PURE__ */ new Map(), d$1.elementStyles = [], d$1.shadowRootOptions = { mode: "open" }, null == o$2 || o$2({ ReactiveElement: d$1 }), (null !== (s$3 = e$4.reactiveElementVersions) && void 0 !== s$3 ? s$3 : e$4.reactiveElementVersions = []).push("1.6.1");
/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */
var t$1;
const i$3 = window, s$2 = i$3.trustedTypes, e$3 = s$2 ? s$2.createPolicy("lit-html", { createHTML: (t2) => t2 }) : void 0, o$1 = `lit$${(Math.random() + "").slice(9)}$`, n$2 = "?" + o$1, l$2 = `<${n$2}>`, h = document, r$1 = (t2 = "") => h.createComment(t2), d2 = (t2) => null === t2 || "object" != typeof t2 && "function" != typeof t2, u$2 = Array.isArray, c$2 = (t2) => u$2(t2) || "function" == typeof (null == t2 ? void 0 : t2[Symbol.iterator]), v = /<(?:(!--|\/[^a-zA-Z])|(\/?[a-zA-Z][^>\s]*)|(\/?$))/g, a = /-->/g, f$1 = />/g, _ = RegExp(`>|[ 	
\f\r](?:([^\\s"'>=/]+)([ 	
\f\r]*=[ 	
\f\r]*(?:[^ 	
\f\r"'\`<>=]|("|')|))|$)`, "g"), m$1 = /'/g, p$1 = /"/g, $ = /^(?:script|style|textarea|title)$/i, g = (t2) => (i3, ...s3) => ({ _$litType$: t2, strings: i3, values: s3 }), y = g(1), x = Symbol.for("lit-noChange"), b = Symbol.for("lit-nothing"), T = /* @__PURE__ */ new WeakMap(), A = h.createTreeWalker(h, 129, null, false), E = (t2, i3) => {
  const s3 = t2.length - 1, n2 = [];
  let h2, r2 = 2 === i3 ? "<svg>" : "", d3 = v;
  for (let i4 = 0; i4 < s3; i4++) {
    const s4 = t2[i4];
    let e2, u3, c2 = -1, g2 = 0;
    for (; g2 < s4.length && (d3.lastIndex = g2, u3 = d3.exec(s4), null !== u3); )
      g2 = d3.lastIndex, d3 === v ? "!--" === u3[1] ? d3 = a : void 0 !== u3[1] ? d3 = f$1 : void 0 !== u3[2] ? ($.test(u3[2]) && (h2 = RegExp("</" + u3[2], "g")), d3 = _) : void 0 !== u3[3] && (d3 = _) : d3 === _ ? ">" === u3[0] ? (d3 = null != h2 ? h2 : v, c2 = -1) : void 0 === u3[1] ? c2 = -2 : (c2 = d3.lastIndex - u3[2].length, e2 = u3[1], d3 = void 0 === u3[3] ? _ : '"' === u3[3] ? p$1 : m$1) : d3 === p$1 || d3 === m$1 ? d3 = _ : d3 === a || d3 === f$1 ? d3 = v : (d3 = _, h2 = void 0);
    const y2 = d3 === _ && t2[i4 + 1].startsWith("/>") ? " " : "";
    r2 += d3 === v ? s4 + l$2 : c2 >= 0 ? (n2.push(e2), s4.slice(0, c2) + "$lit$" + s4.slice(c2) + o$1 + y2) : s4 + o$1 + (-2 === c2 ? (n2.push(void 0), i4) : y2);
  }
  const u2 = r2 + (t2[s3] || "<?>") + (2 === i3 ? "</svg>" : "");
  if (!Array.isArray(t2) || !t2.hasOwnProperty("raw"))
    throw Error("invalid template strings array");
  return [void 0 !== e$3 ? e$3.createHTML(u2) : u2, n2];
};
class C {
  constructor({ strings: t2, _$litType$: i3 }, e2) {
    let l2;
    this.parts = [];
    let h2 = 0, d3 = 0;
    const u2 = t2.length - 1, c2 = this.parts, [v2, a2] = E(t2, i3);
    if (this.el = C.createElement(v2, e2), A.currentNode = this.el.content, 2 === i3) {
      const t3 = this.el.content, i4 = t3.firstChild;
      i4.remove(), t3.append(...i4.childNodes);
    }
    for (; null !== (l2 = A.nextNode()) && c2.length < u2; ) {
      if (1 === l2.nodeType) {
        if (l2.hasAttributes()) {
          const t3 = [];
          for (const i4 of l2.getAttributeNames())
            if (i4.endsWith("$lit$") || i4.startsWith(o$1)) {
              const s3 = a2[d3++];
              if (t3.push(i4), void 0 !== s3) {
                const t4 = l2.getAttribute(s3.toLowerCase() + "$lit$").split(o$1), i5 = /([.?@])?(.*)/.exec(s3);
                c2.push({ type: 1, index: h2, name: i5[2], strings: t4, ctor: "." === i5[1] ? M : "?" === i5[1] ? k : "@" === i5[1] ? H : S });
              } else
                c2.push({ type: 6, index: h2 });
            }
          for (const i4 of t3)
            l2.removeAttribute(i4);
        }
        if ($.test(l2.tagName)) {
          const t3 = l2.textContent.split(o$1), i4 = t3.length - 1;
          if (i4 > 0) {
            l2.textContent = s$2 ? s$2.emptyScript : "";
            for (let s3 = 0; s3 < i4; s3++)
              l2.append(t3[s3], r$1()), A.nextNode(), c2.push({ type: 2, index: ++h2 });
            l2.append(t3[i4], r$1());
          }
        }
      } else if (8 === l2.nodeType)
        if (l2.data === n$2)
          c2.push({ type: 2, index: h2 });
        else {
          let t3 = -1;
          for (; -1 !== (t3 = l2.data.indexOf(o$1, t3 + 1)); )
            c2.push({ type: 7, index: h2 }), t3 += o$1.length - 1;
        }
      h2++;
    }
  }
  static createElement(t2, i3) {
    const s3 = h.createElement("template");
    return s3.innerHTML = t2, s3;
  }
}
function P(t2, i3, s3 = t2, e2) {
  var o3, n2, l2, h2;
  if (i3 === x)
    return i3;
  let r2 = void 0 !== e2 ? null === (o3 = s3._$Co) || void 0 === o3 ? void 0 : o3[e2] : s3._$Cl;
  const u2 = d2(i3) ? void 0 : i3._$litDirective$;
  return (null == r2 ? void 0 : r2.constructor) !== u2 && (null === (n2 = null == r2 ? void 0 : r2._$AO) || void 0 === n2 || n2.call(r2, false), void 0 === u2 ? r2 = void 0 : (r2 = new u2(t2), r2._$AT(t2, s3, e2)), void 0 !== e2 ? (null !== (l2 = (h2 = s3)._$Co) && void 0 !== l2 ? l2 : h2._$Co = [])[e2] = r2 : s3._$Cl = r2), void 0 !== r2 && (i3 = P(t2, r2._$AS(t2, i3.values), r2, e2)), i3;
}
class V {
  constructor(t2, i3) {
    this.u = [], this._$AN = void 0, this._$AD = t2, this._$AM = i3;
  }
  get parentNode() {
    return this._$AM.parentNode;
  }
  get _$AU() {
    return this._$AM._$AU;
  }
  v(t2) {
    var i3;
    const { el: { content: s3 }, parts: e2 } = this._$AD, o3 = (null !== (i3 = null == t2 ? void 0 : t2.creationScope) && void 0 !== i3 ? i3 : h).importNode(s3, true);
    A.currentNode = o3;
    let n2 = A.nextNode(), l2 = 0, r2 = 0, d3 = e2[0];
    for (; void 0 !== d3; ) {
      if (l2 === d3.index) {
        let i4;
        2 === d3.type ? i4 = new N(n2, n2.nextSibling, this, t2) : 1 === d3.type ? i4 = new d3.ctor(n2, d3.name, d3.strings, this, t2) : 6 === d3.type && (i4 = new I(n2, this, t2)), this.u.push(i4), d3 = e2[++r2];
      }
      l2 !== (null == d3 ? void 0 : d3.index) && (n2 = A.nextNode(), l2++);
    }
    return o3;
  }
  p(t2) {
    let i3 = 0;
    for (const s3 of this.u)
      void 0 !== s3 && (void 0 !== s3.strings ? (s3._$AI(t2, s3, i3), i3 += s3.strings.length - 2) : s3._$AI(t2[i3])), i3++;
  }
}
class N {
  constructor(t2, i3, s3, e2) {
    var o3;
    this.type = 2, this._$AH = b, this._$AN = void 0, this._$AA = t2, this._$AB = i3, this._$AM = s3, this.options = e2, this._$Cm = null === (o3 = null == e2 ? void 0 : e2.isConnected) || void 0 === o3 || o3;
  }
  get _$AU() {
    var t2, i3;
    return null !== (i3 = null === (t2 = this._$AM) || void 0 === t2 ? void 0 : t2._$AU) && void 0 !== i3 ? i3 : this._$Cm;
  }
  get parentNode() {
    let t2 = this._$AA.parentNode;
    const i3 = this._$AM;
    return void 0 !== i3 && 11 === t2.nodeType && (t2 = i3.parentNode), t2;
  }
  get startNode() {
    return this._$AA;
  }
  get endNode() {
    return this._$AB;
  }
  _$AI(t2, i3 = this) {
    t2 = P(this, t2, i3), d2(t2) ? t2 === b || null == t2 || "" === t2 ? (this._$AH !== b && this._$AR(), this._$AH = b) : t2 !== this._$AH && t2 !== x && this.g(t2) : void 0 !== t2._$litType$ ? this.$(t2) : void 0 !== t2.nodeType ? this.T(t2) : c$2(t2) ? this.k(t2) : this.g(t2);
  }
  O(t2, i3 = this._$AB) {
    return this._$AA.parentNode.insertBefore(t2, i3);
  }
  T(t2) {
    this._$AH !== t2 && (this._$AR(), this._$AH = this.O(t2));
  }
  g(t2) {
    this._$AH !== b && d2(this._$AH) ? this._$AA.nextSibling.data = t2 : this.T(h.createTextNode(t2)), this._$AH = t2;
  }
  $(t2) {
    var i3;
    const { values: s3, _$litType$: e2 } = t2, o3 = "number" == typeof e2 ? this._$AC(t2) : (void 0 === e2.el && (e2.el = C.createElement(e2.h, this.options)), e2);
    if ((null === (i3 = this._$AH) || void 0 === i3 ? void 0 : i3._$AD) === o3)
      this._$AH.p(s3);
    else {
      const t3 = new V(o3, this), i4 = t3.v(this.options);
      t3.p(s3), this.T(i4), this._$AH = t3;
    }
  }
  _$AC(t2) {
    let i3 = T.get(t2.strings);
    return void 0 === i3 && T.set(t2.strings, i3 = new C(t2)), i3;
  }
  k(t2) {
    u$2(this._$AH) || (this._$AH = [], this._$AR());
    const i3 = this._$AH;
    let s3, e2 = 0;
    for (const o3 of t2)
      e2 === i3.length ? i3.push(s3 = new N(this.O(r$1()), this.O(r$1()), this, this.options)) : s3 = i3[e2], s3._$AI(o3), e2++;
    e2 < i3.length && (this._$AR(s3 && s3._$AB.nextSibling, e2), i3.length = e2);
  }
  _$AR(t2 = this._$AA.nextSibling, i3) {
    var s3;
    for (null === (s3 = this._$AP) || void 0 === s3 || s3.call(this, false, true, i3); t2 && t2 !== this._$AB; ) {
      const i4 = t2.nextSibling;
      t2.remove(), t2 = i4;
    }
  }
  setConnected(t2) {
    var i3;
    void 0 === this._$AM && (this._$Cm = t2, null === (i3 = this._$AP) || void 0 === i3 || i3.call(this, t2));
  }
}
class S {
  constructor(t2, i3, s3, e2, o3) {
    this.type = 1, this._$AH = b, this._$AN = void 0, this.element = t2, this.name = i3, this._$AM = e2, this.options = o3, s3.length > 2 || "" !== s3[0] || "" !== s3[1] ? (this._$AH = Array(s3.length - 1).fill(new String()), this.strings = s3) : this._$AH = b;
  }
  get tagName() {
    return this.element.tagName;
  }
  get _$AU() {
    return this._$AM._$AU;
  }
  _$AI(t2, i3 = this, s3, e2) {
    const o3 = this.strings;
    let n2 = false;
    if (void 0 === o3)
      t2 = P(this, t2, i3, 0), n2 = !d2(t2) || t2 !== this._$AH && t2 !== x, n2 && (this._$AH = t2);
    else {
      const e3 = t2;
      let l2, h2;
      for (t2 = o3[0], l2 = 0; l2 < o3.length - 1; l2++)
        h2 = P(this, e3[s3 + l2], i3, l2), h2 === x && (h2 = this._$AH[l2]), n2 || (n2 = !d2(h2) || h2 !== this._$AH[l2]), h2 === b ? t2 = b : t2 !== b && (t2 += (null != h2 ? h2 : "") + o3[l2 + 1]), this._$AH[l2] = h2;
    }
    n2 && !e2 && this.j(t2);
  }
  j(t2) {
    t2 === b ? this.element.removeAttribute(this.name) : this.element.setAttribute(this.name, null != t2 ? t2 : "");
  }
}
class M extends S {
  constructor() {
    super(...arguments), this.type = 3;
  }
  j(t2) {
    this.element[this.name] = t2 === b ? void 0 : t2;
  }
}
const R = s$2 ? s$2.emptyScript : "";
class k extends S {
  constructor() {
    super(...arguments), this.type = 4;
  }
  j(t2) {
    t2 && t2 !== b ? this.element.setAttribute(this.name, R) : this.element.removeAttribute(this.name);
  }
}
class H extends S {
  constructor(t2, i3, s3, e2, o3) {
    super(t2, i3, s3, e2, o3), this.type = 5;
  }
  _$AI(t2, i3 = this) {
    var s3;
    if ((t2 = null !== (s3 = P(this, t2, i3, 0)) && void 0 !== s3 ? s3 : b) === x)
      return;
    const e2 = this._$AH, o3 = t2 === b && e2 !== b || t2.capture !== e2.capture || t2.once !== e2.once || t2.passive !== e2.passive, n2 = t2 !== b && (e2 === b || o3);
    o3 && this.element.removeEventListener(this.name, this, e2), n2 && this.element.addEventListener(this.name, this, t2), this._$AH = t2;
  }
  handleEvent(t2) {
    var i3, s3;
    "function" == typeof this._$AH ? this._$AH.call(null !== (s3 = null === (i3 = this.options) || void 0 === i3 ? void 0 : i3.host) && void 0 !== s3 ? s3 : this.element, t2) : this._$AH.handleEvent(t2);
  }
}
class I {
  constructor(t2, i3, s3) {
    this.element = t2, this.type = 6, this._$AN = void 0, this._$AM = i3, this.options = s3;
  }
  get _$AU() {
    return this._$AM._$AU;
  }
  _$AI(t2) {
    P(this, t2);
  }
}
const L = { P: "$lit$", A: o$1, M: n$2, C: 1, L: E, R: V, D: c$2, V: P, I: N, H: S, N: k, U: H, B: M, F: I }, z = i$3.litHtmlPolyfillSupport;
null == z || z(C, N), (null !== (t$1 = i$3.litHtmlVersions) && void 0 !== t$1 ? t$1 : i$3.litHtmlVersions = []).push("2.6.1");
const Z = (t2, i3, s3) => {
  var e2, o3;
  const n2 = null !== (e2 = null == s3 ? void 0 : s3.renderBefore) && void 0 !== e2 ? e2 : i3;
  let l2 = n2._$litPart$;
  if (void 0 === l2) {
    const t3 = null !== (o3 = null == s3 ? void 0 : s3.renderBefore) && void 0 !== o3 ? o3 : null;
    n2._$litPart$ = l2 = new N(i3.insertBefore(r$1(), t3), t3, void 0, null != s3 ? s3 : {});
  }
  return l2._$AI(t2), l2;
};
/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */
var l$1, o2;
let s$1 = class s extends d$1 {
  constructor() {
    super(...arguments), this.renderOptions = { host: this }, this._$Do = void 0;
  }
  createRenderRoot() {
    var t2, e2;
    const i3 = super.createRenderRoot();
    return null !== (t2 = (e2 = this.renderOptions).renderBefore) && void 0 !== t2 || (e2.renderBefore = i3.firstChild), i3;
  }
  update(t2) {
    const i3 = this.render();
    this.hasUpdated || (this.renderOptions.isConnected = this.isConnected), super.update(t2), this._$Do = Z(i3, this.renderRoot, this.renderOptions);
  }
  connectedCallback() {
    var t2;
    super.connectedCallback(), null === (t2 = this._$Do) || void 0 === t2 || t2.setConnected(true);
  }
  disconnectedCallback() {
    var t2;
    super.disconnectedCallback(), null === (t2 = this._$Do) || void 0 === t2 || t2.setConnected(false);
  }
  render() {
    return x;
  }
};
s$1.finalized = true, s$1._$litElement$ = true, null === (l$1 = globalThis.litElementHydrateSupport) || void 0 === l$1 || l$1.call(globalThis, { LitElement: s$1 });
const n$1 = globalThis.litElementPolyfillSupport;
null == n$1 || n$1({ LitElement: s$1 });
(null !== (o2 = globalThis.litElementVersions) && void 0 !== o2 ? o2 : globalThis.litElementVersions = []).push("3.2.2");
/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */
const e$2 = (e2) => (n2) => "function" == typeof n2 ? ((e3, n3) => (customElements.define(e3, n3), n3))(e2, n2) : ((e3, n3) => {
  const { kind: t2, elements: s3 } = n3;
  return { kind: t2, elements: s3, finisher(n4) {
    customElements.define(e3, n4);
  } };
})(e2, n2);
/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */
const i$2 = (i3, e2) => "method" === e2.kind && e2.descriptor && !("value" in e2.descriptor) ? { ...e2, finisher(n2) {
  n2.createProperty(e2.key, i3);
} } : { kind: "field", key: Symbol(), placement: "own", descriptor: {}, originalKey: e2.key, initializer() {
  "function" == typeof e2.initializer && (this[e2.key] = e2.initializer.call(this));
}, finisher(n2) {
  n2.createProperty(e2.key, i3);
} };
function e$1(e2) {
  return (n2, t2) => void 0 !== t2 ? ((i3, e3, n3) => {
    e3.constructor.createProperty(n3, i3);
  })(e2, n2, t2) : i$2(e2, n2);
}
/**
 * @license
 * Copyright 2021 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */
var n;
null != (null === (n = window.HTMLSlotElement) || void 0 === n ? void 0 : n.prototype.assignedElements) ? (o3, n2) => o3.assignedElements(n2) : (o3, n2) => o3.assignedNodes(n2).filter((o4) => o4.nodeType === Node.ELEMENT_NODE);
/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */
const t = { ATTRIBUTE: 1, CHILD: 2, PROPERTY: 3, BOOLEAN_ATTRIBUTE: 4, EVENT: 5, ELEMENT: 6 }, e = (t2) => (...e2) => ({ _$litDirective$: t2, values: e2 });
let i$1 = class i {
  constructor(t2) {
  }
  get _$AU() {
    return this._$AM._$AU;
  }
  _$AT(t2, e2, i3) {
    this._$Ct = t2, this._$AM = e2, this._$Ci = i3;
  }
  _$AS(t2, e2) {
    return this.update(t2, e2);
  }
  update(t2, e2) {
    return this.render(...e2);
  }
};
/**
 * @license
 * Copyright 2020 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */
const { I: l } = L, c$1 = () => document.createComment(""), r = (o3, t2, i3) => {
  var n2;
  const d3 = o3._$AA.parentNode, v2 = void 0 === t2 ? o3._$AB : t2._$AA;
  if (void 0 === i3) {
    const t3 = d3.insertBefore(c$1(), v2), n3 = d3.insertBefore(c$1(), v2);
    i3 = new l(t3, n3, o3, o3.options);
  } else {
    const l2 = i3._$AB.nextSibling, t3 = i3._$AM, e2 = t3 !== o3;
    if (e2) {
      let l3;
      null === (n2 = i3._$AQ) || void 0 === n2 || n2.call(i3, o3), i3._$AM = o3, void 0 !== i3._$AP && (l3 = o3._$AU) !== t3._$AU && i3._$AP(l3);
    }
    if (l2 !== v2 || e2) {
      let o4 = i3._$AA;
      for (; o4 !== l2; ) {
        const l3 = o4.nextSibling;
        d3.insertBefore(o4, v2), o4 = l3;
      }
    }
  }
  return i3;
}, u$1 = (o3, l2, t2 = o3) => (o3._$AI(l2, t2), o3), f = {}, s2 = (o3, l2 = f) => o3._$AH = l2, m = (o3) => o3._$AH, p = (o3) => {
  var l2;
  null === (l2 = o3._$AP) || void 0 === l2 || l2.call(o3, false, true);
  let t2 = o3._$AA;
  const i3 = o3._$AB.nextSibling;
  for (; t2 !== i3; ) {
    const o4 = t2.nextSibling;
    t2.remove(), t2 = o4;
  }
};
/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */
const u = (e2, s3, t2) => {
  const r2 = /* @__PURE__ */ new Map();
  for (let l2 = s3; l2 <= t2; l2++)
    r2.set(e2[l2], l2);
  return r2;
}, c = e(class extends i$1 {
  constructor(e2) {
    if (super(e2), e2.type !== t.CHILD)
      throw Error("repeat() can only be used in text expressions");
  }
  ht(e2, s3, t2) {
    let r2;
    void 0 === t2 ? t2 = s3 : void 0 !== s3 && (r2 = s3);
    const l2 = [], o3 = [];
    let i3 = 0;
    for (const s4 of e2)
      l2[i3] = r2 ? r2(s4, i3) : i3, o3[i3] = t2(s4, i3), i3++;
    return { values: o3, keys: l2 };
  }
  render(e2, s3, t2) {
    return this.ht(e2, s3, t2).values;
  }
  update(s$12, [t2, r$12, c2]) {
    var d3;
    const a2 = m(s$12), { values: p$12, keys: v2 } = this.ht(t2, r$12, c2);
    if (!Array.isArray(a2))
      return this.ut = v2, p$12;
    const h2 = null !== (d3 = this.ut) && void 0 !== d3 ? d3 : this.ut = [], m$12 = [];
    let y2, x$1, j = 0, k2 = a2.length - 1, w = 0, A2 = p$12.length - 1;
    for (; j <= k2 && w <= A2; )
      if (null === a2[j])
        j++;
      else if (null === a2[k2])
        k2--;
      else if (h2[j] === v2[w])
        m$12[w] = u$1(a2[j], p$12[w]), j++, w++;
      else if (h2[k2] === v2[A2])
        m$12[A2] = u$1(a2[k2], p$12[A2]), k2--, A2--;
      else if (h2[j] === v2[A2])
        m$12[A2] = u$1(a2[j], p$12[A2]), r(s$12, m$12[A2 + 1], a2[j]), j++, A2--;
      else if (h2[k2] === v2[w])
        m$12[w] = u$1(a2[k2], p$12[w]), r(s$12, a2[j], a2[k2]), k2--, w++;
      else if (void 0 === y2 && (y2 = u(v2, w, A2), x$1 = u(h2, j, k2)), y2.has(h2[j]))
        if (y2.has(h2[k2])) {
          const e2 = x$1.get(v2[w]), t3 = void 0 !== e2 ? a2[e2] : null;
          if (null === t3) {
            const e3 = r(s$12, a2[j]);
            u$1(e3, p$12[w]), m$12[w] = e3;
          } else
            m$12[w] = u$1(t3, p$12[w]), r(s$12, a2[j], t3), a2[e2] = null;
          w++;
        } else
          p(a2[k2]), k2--;
      else
        p(a2[j]), j++;
    for (; w <= A2; ) {
      const e2 = r(s$12, m$12[A2 + 1]);
      u$1(e2, p$12[w]), m$12[w++] = e2;
    }
    for (; j <= k2; ) {
      const e2 = a2[j++];
      null !== e2 && p(e2);
    }
    return this.ut = v2, s2(s$12, m$12), x;
  }
});
/**
 * @license
 * Copyright 2018 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */
const i2 = e(class extends i$1 {
  constructor(t$12) {
    var e2;
    if (super(t$12), t$12.type !== t.ATTRIBUTE || "style" !== t$12.name || (null === (e2 = t$12.strings) || void 0 === e2 ? void 0 : e2.length) > 2)
      throw Error("The `styleMap` directive must be used in the `style` attribute and must be the only part in the attribute.");
  }
  render(t2) {
    return Object.keys(t2).reduce((e2, r2) => {
      const s3 = t2[r2];
      return null == s3 ? e2 : e2 + `${r2 = r2.replace(/(?:^(webkit|moz|ms|o)|)(?=[A-Z])/g, "-$&").toLowerCase()}:${s3};`;
    }, "");
  }
  update(e2, [r2]) {
    const { style: s3 } = e2.element;
    if (void 0 === this.vt) {
      this.vt = /* @__PURE__ */ new Set();
      for (const t2 in r2)
        this.vt.add(t2);
      return this.render(r2);
    }
    this.vt.forEach((t2) => {
      null == r2[t2] && (this.vt.delete(t2), t2.includes("-") ? s3.removeProperty(t2) : s3[t2] = "");
    });
    for (const t2 in r2) {
      const e3 = r2[t2];
      null != e3 && (this.vt.add(t2), t2.includes("-") ? s3.setProperty(t2, e3) : s3[t2] = e3);
    }
    return x;
  }
});
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
const mainApis = wrap(import_port);
class PromiseOut {
  constructor() {
    this.promise = new Promise((resolve, reject) => {
      this.resolve = resolve;
      this.reject = reject;
    });
  }
}
var __defProp = Object.defineProperty;
var __getOwnPropDesc = Object.getOwnPropertyDescriptor;
var __decorateClass = (decorators, target, key, kind) => {
  var result = kind > 1 ? void 0 : kind ? __getOwnPropDesc(target, key) : target;
  for (var i3 = decorators.length - 1, decorator; i3 >= 0; i3--)
    if (decorator = decorators[i3])
      result = (kind ? decorator(target, key, result) : decorator(result)) || result;
  if (kind && result)
    __defProp(target, key, result);
  return result;
};
let ViewTree = class extends s$1 {
  constructor() {
    super(...arguments);
    this.name = "Multi Webview";
    this.webviews = [];
    this._id_acc = 0;
  }
  /** 对webview视图进行状态整理 */
  _restateWebviews() {
    let index_acc = 0;
    let closing_acc = 0;
    let opening_acc = 0;
    let scale_sub = 0.05;
    let scale_acc = 1 + scale_sub;
    let opacity_sub = 0.1;
    let opacity_acc = 1 + opacity_sub;
    for (const webview of this.webviews) {
      webview.state.zIndex = this.webviews.length - ++index_acc;
      if (webview.closing) {
        webview.state.closingIndex = closing_acc++;
      } else {
        {
          webview.state.scale = scale_acc -= scale_sub;
          scale_sub = Math.max(0, scale_sub - 0.01);
        }
        {
          webview.state.opacity = opacity_acc - opacity_sub;
          opacity_acc = Math.max(0, opacity_acc - opacity_sub);
        }
        {
          webview.state.openingIndex = opening_acc++;
        }
      }
    }
    this.requestUpdate("webviews");
  }
  openWebview(src) {
    const webview_id = this._id_acc++;
    this.webviews.unshift(new Webview(webview_id, src));
    this._restateWebviews();
    return webview_id;
  }
  closeWebview(webview_id) {
    const webview = this.webviews.find((dialog) => dialog.id === webview_id);
    if (webview === void 0) {
      return false;
    }
    webview.closing = true;
    this._restateWebviews();
    return true;
  }
  _removeWebview(webview) {
    const index = this.webviews.indexOf(webview);
    if (index === -1) {
      return false;
    }
    this.webviews.splice(index, 1);
    this._restateWebviews();
    return true;
  }
  async onWebviewReady(webview, ele) {
    webview.webContentId = ele.getWebContentsId();
    webview.doReady(ele);
    mainApis.denyWindowOpenHandler(
      webview.webContentId,
      proxy((detail) => {
        console.log(detail);
        this.openWebview(detail.url);
      })
    );
    mainApis.onDestroy(
      webview.webContentId,
      proxy(() => {
        this.closeWebview(webview.id);
        console.log("Destroy!!");
      })
    );
    const webcontents = await mainApis.getWenContents(webview.webContentId);
    console.log("webcontents", webcontents);
  }
  async onDevtoolReady(webview, ele_devTool) {
    await webview.ready();
    if (webview.webContentId_devTools === ele_devTool.getWebContentsId()) {
      return;
    }
    webview.webContentId_devTools = ele_devTool.getWebContentsId();
    await mainApis.openDevTools(
      webview.webContentId,
      void 0,
      webview.webContentId_devTools
    );
  }
  async destroyWebview(webview) {
    await mainApis.destroy(webview.webContentId);
  }
  // Render the UI as a function of component state
  render() {
    return y`
      <div class="layer stack">
        ${c(
      this.webviews,
      (dialog) => dialog.id,
      (webview) => {
        return y`
              <div
                class="webview-container ${webview.closing ? `closing` : `opening`}"
                style=${i2({
          "--z-index": webview.state.zIndex + "",
          "--scale": webview.state.scale + "",
          "--opacity": webview.state.opacity + ""
        })}
              >
                <webview
                  id="view-${webview.id}"
                  class="webview ani-view"
                  src=${webview.src}
                  partition="trusted"
                  allownw
                  allowpopups
                  @animationend=${(event) => {
          if (event.animationName === "slideOut" && webview.closing) {
            this._removeWebview(webview);
          }
        }}
                  @dom-ready=${(event) => {
          this.onWebviewReady(webview, event.target);
        }}
                ></webview>
              </div>
            `;
      }
    )}
      </div>
      <div class="layer stack" style="flex: 2.5;">
        ${c(
      this.webviews,
      (dialog) => dialog.id,
      (webview) => {
        return y`
              <div
                class="devtool-container ${webview.closing ? `closing` : `opening`}"
                style=${i2({
          "--z-index": webview.state.zIndex + "",
          "--scale": webview.state.scale + "",
          "--opacity": webview.state.opacity + ""
        })}
              >
                <fieldset class="toolbar" .disabled=${webview.closing}>
                  <button
                    @click=${() => {
          this.destroyWebview(webview);
        }}
                  >
                    销毁Webview
                  </button>
                </fieldset>
                <webview
                  id="tool-${webview.id}"
                  class="devtool ani-view"
                  src="about:blank"
                  partition="trusted"
                  @dom-ready=${(event) => {
          console.log("DevtoolReady", event.target);
          this.onDevtoolReady(webview, event.target);
        }}
                ></webview>
              </div>
            `;
      }
    )}
      </div>
    `;
  }
};
ViewTree.styles = [
  i$4`
      :host {
        display: flex;
        flex-direction: row;

        height: 100%;

        grid-template-areas: "layer";
      }
      * {
        box-sizing: border-box;
      }
      .layer {
        grid-area: layer;

        flex: 1;

        display: grid;
        grid-template-areas: "layer-content";

        height: 100%;
        padding: 0.5em 1em;
      }
      .webview-container {
        grid-area: layer-content;

        height: 100%;

        display: grid;
        grid-template-areas: "webview";
        grid-template-rows: 1fr;
      }
      .devtool-container {
        grid-area: layer-content;

        height: 100%;

        display: grid;
        grid-template-areas: "toolbar" "devtool";
        grid-template-rows: 2em 1fr;
      }
    `,
  i$4`
      .webview {
        grid-area: webview;

        width: 100%;
        height: 100%;
        border: 0;
        outline: 1px solid red;
        border-radius: 1em;
        overflow: hidden;
      }
      .devtool {
        grid-area: devtool;

        width: 100%;
        height: 100%;
        border: 0;
        outline: 1px solid blue;
        border-radius: 1em;
        overflow: hidden;
      }
      .toolbar {
        grid-area: toolbar;

        display: flex;
        height: 100%;
        flex-direction: row;
        align-items: center;
      }
      fieldset {
        border: 0;
        padding: 0;
      }
      fieldset:disabled {
        display: none;
      }
    `,
  i$4`
      :host {
        --easing: cubic-bezier(0.36, 0.66, 0.04, 1);
      }
      .opening > .ani-view {
        animation: slideIn 520ms var(--easing) forwards;
      }
      .closing > .ani-view {
        animation: slideOut 830ms var(--easing) forwards;
      }
      @keyframes slideIn {
        0% {
          transform: translateY(60%) translateZ(0);
          opacity: 0.4;
        }
        100% {
          transform: translateY(0%) translateZ(0);
          opacity: 1;
        }
      }
      @keyframes slideOut {
        0% {
          transform: translateY(0%) translateZ(0);
          opacity: 1;
        }
        30% {
          transform: translateY(-30%) translateZ(0) scale(0.4);
          opacity: 0.6;
        }
        100% {
          transform: translateY(-100%) translateZ(0) scale(0.3);
          opacity: 0.5;
        }
      }
    `,
  i$4`
      .stack {
        display: inline-grid;
        place-items: center;
        align-items: flex-end;
      }
      .stack > * {
        grid-column-start: 1;
        grid-row-start: 1;
        width: 100%;
        transition-duration: 520ms;
        transition-timing-function: var(--easing);
        /* pointer-events: none; */
        transition-property: transform, opacity;
        // backdrop-filter: blur(5px);
        z-index: var(--z-index, 1);
        transform: translateZ(0) scale(var(--scale, 1));
        opacity: var(--opacity, 1);
      }
      .stack > .closing {
        pointer-events: none;
      }
      /* 
      .stack > .opening {
        transform: translateY(min(10%, 10px)) translateZ(0) scale(0.9);
        z-index: 1;
        opacity: 0.6;
      }
      .stack > .opening.opening-2 {
        transform: translateY(min(5%, 5px)) translateZ(0) scale(0.95);
        z-index: 2;
        opacity: 0.8;
      }
      .stack > .opening.opening-1 {
        transform: translateY(0) translateZ(0) scale(1);
        z-index: 3;
        opacity: 1;
        pointer-events: unset;
      } */
    `
];
__decorateClass([
  e$1()
], ViewTree.prototype, "name", 2);
ViewTree = __decorateClass([
  e$2("view-tree")
], ViewTree);
class Webview {
  constructor(id, src) {
    this.id = id;
    this.src = src;
    this.webContentId = -1;
    this.webContentId_devTools = -1;
    this._api_po = new PromiseOut();
    this.closing = false;
    this.state = {
      zIndex: 0,
      openingIndex: 0,
      closingIndex: 0,
      scale: 1,
      opacity: 1
      // translateY: 0,
    };
  }
  get api() {
    return this._api;
  }
  doReady(value) {
    this._api = value;
    this._api_po.resolve(value);
  }
  ready() {
    return this._api_po.promise;
  }
}
const viewTree = new ViewTree();
document.body.appendChild(viewTree);
console.log(viewTree);
const APIS = {
  openWebview: viewTree.openWebview.bind(viewTree),
  closeWebview: viewTree.closeWebview.bind(viewTree)
};
exportApis(APIS);
Object.assign(globalThis, APIS);
