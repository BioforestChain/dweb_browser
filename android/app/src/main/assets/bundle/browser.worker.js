var __create = Object.create;
var __freeze = Object.freeze;
var __defProp = Object.defineProperty;
var __getOwnPropDesc = Object.getOwnPropertyDescriptor;
var __getOwnPropNames = Object.getOwnPropertyNames;
var __getProtoOf = Object.getPrototypeOf;
var __hasOwnProp = Object.prototype.hasOwnProperty;
var __commonJS = (cb, mod) => function __require() {
  return mod || (0, cb[__getOwnPropNames(cb)[0]])((mod = { exports: {} }).exports, mod), mod.exports;
};
var __copyProps = (to, from, except, desc) => {
  if (from && typeof from === "object" || typeof from === "function") {
    for (let key of __getOwnPropNames(from))
      if (!__hasOwnProp.call(to, key) && key !== except)
        __defProp(to, key, { get: () => from[key], enumerable: !(desc = __getOwnPropDesc(from, key)) || desc.enumerable });
  }
  return to;
};
var __toESM = (mod, isNodeMode, target) => (target = mod != null ? __create(__getProtoOf(mod)) : {}, __copyProps(
  // If the importer is in node compatibility mode or this is not an ESM
  // file that has been converted to a CommonJS file using a Babel-
  // compatible transform (i.e. "__esModule" has not been set), then set
  // "default" to the CommonJS "module.exports" for node compatibility.
  isNodeMode || !mod || !mod.__esModule ? __defProp(target, "default", { value: mod, enumerable: true }) : target,
  mod
));
var __accessCheck = (obj, member, msg) => {
  if (!member.has(obj))
    throw TypeError("Cannot " + msg);
};
var __privateGet = (obj, member, getter) => {
  __accessCheck(obj, member, "read from private field");
  return getter ? getter.call(obj) : member.get(obj);
};
var __privateAdd = (obj, member, value) => {
  if (member.has(obj))
    throw TypeError("Cannot add the same private member more than once");
  member instanceof WeakSet ? member.add(obj) : member.set(obj, value);
};
var __privateSet = (obj, member, value, setter) => {
  __accessCheck(obj, member, "write to private field");
  setter ? setter.call(obj, value) : member.set(obj, value);
  return value;
};
var __template = (cooked, raw) => __freeze(__defProp(cooked, "raw", { value: __freeze(raw || cooked.slice()) }));

// node_modules/lodash/_trimmedEndIndex.js
var require_trimmedEndIndex = __commonJS({
  "node_modules/lodash/_trimmedEndIndex.js"(exports, module) {
    var reWhitespace = /\s/;
    function trimmedEndIndex(string) {
      var index = string.length;
      while (index-- && reWhitespace.test(string.charAt(index))) {
      }
      return index;
    }
    module.exports = trimmedEndIndex;
  }
});

// node_modules/lodash/_baseTrim.js
var require_baseTrim = __commonJS({
  "node_modules/lodash/_baseTrim.js"(exports, module) {
    var trimmedEndIndex = require_trimmedEndIndex();
    var reTrimStart = /^\s+/;
    function baseTrim(string) {
      return string ? string.slice(0, trimmedEndIndex(string) + 1).replace(reTrimStart, "") : string;
    }
    module.exports = baseTrim;
  }
});

// node_modules/lodash/isObject.js
var require_isObject = __commonJS({
  "node_modules/lodash/isObject.js"(exports, module) {
    function isObject(value) {
      var type = typeof value;
      return value != null && (type == "object" || type == "function");
    }
    module.exports = isObject;
  }
});

// node_modules/lodash/_freeGlobal.js
var require_freeGlobal = __commonJS({
  "node_modules/lodash/_freeGlobal.js"(exports, module) {
    var freeGlobal = typeof global == "object" && global && global.Object === Object && global;
    module.exports = freeGlobal;
  }
});

// node_modules/lodash/_root.js
var require_root = __commonJS({
  "node_modules/lodash/_root.js"(exports, module) {
    var freeGlobal = require_freeGlobal();
    var freeSelf = typeof self == "object" && self && self.Object === Object && self;
    var root = freeGlobal || freeSelf || Function("return this")();
    module.exports = root;
  }
});

// node_modules/lodash/_Symbol.js
var require_Symbol = __commonJS({
  "node_modules/lodash/_Symbol.js"(exports, module) {
    var root = require_root();
    var Symbol2 = root.Symbol;
    module.exports = Symbol2;
  }
});

// node_modules/lodash/_getRawTag.js
var require_getRawTag = __commonJS({
  "node_modules/lodash/_getRawTag.js"(exports, module) {
    var Symbol2 = require_Symbol();
    var objectProto = Object.prototype;
    var hasOwnProperty = objectProto.hasOwnProperty;
    var nativeObjectToString = objectProto.toString;
    var symToStringTag = Symbol2 ? Symbol2.toStringTag : void 0;
    function getRawTag(value) {
      var isOwn = hasOwnProperty.call(value, symToStringTag), tag = value[symToStringTag];
      try {
        value[symToStringTag] = void 0;
        var unmasked = true;
      } catch (e) {
      }
      var result = nativeObjectToString.call(value);
      if (unmasked) {
        if (isOwn) {
          value[symToStringTag] = tag;
        } else {
          delete value[symToStringTag];
        }
      }
      return result;
    }
    module.exports = getRawTag;
  }
});

// node_modules/lodash/_objectToString.js
var require_objectToString = __commonJS({
  "node_modules/lodash/_objectToString.js"(exports, module) {
    var objectProto = Object.prototype;
    var nativeObjectToString = objectProto.toString;
    function objectToString(value) {
      return nativeObjectToString.call(value);
    }
    module.exports = objectToString;
  }
});

// node_modules/lodash/_baseGetTag.js
var require_baseGetTag = __commonJS({
  "node_modules/lodash/_baseGetTag.js"(exports, module) {
    var Symbol2 = require_Symbol();
    var getRawTag = require_getRawTag();
    var objectToString = require_objectToString();
    var nullTag = "[object Null]";
    var undefinedTag = "[object Undefined]";
    var symToStringTag = Symbol2 ? Symbol2.toStringTag : void 0;
    function baseGetTag(value) {
      if (value == null) {
        return value === void 0 ? undefinedTag : nullTag;
      }
      return symToStringTag && symToStringTag in Object(value) ? getRawTag(value) : objectToString(value);
    }
    module.exports = baseGetTag;
  }
});

// node_modules/lodash/isObjectLike.js
var require_isObjectLike = __commonJS({
  "node_modules/lodash/isObjectLike.js"(exports, module) {
    function isObjectLike(value) {
      return value != null && typeof value == "object";
    }
    module.exports = isObjectLike;
  }
});

// node_modules/lodash/isSymbol.js
var require_isSymbol = __commonJS({
  "node_modules/lodash/isSymbol.js"(exports, module) {
    var baseGetTag = require_baseGetTag();
    var isObjectLike = require_isObjectLike();
    var symbolTag = "[object Symbol]";
    function isSymbol(value) {
      return typeof value == "symbol" || isObjectLike(value) && baseGetTag(value) == symbolTag;
    }
    module.exports = isSymbol;
  }
});

// node_modules/lodash/toNumber.js
var require_toNumber = __commonJS({
  "node_modules/lodash/toNumber.js"(exports, module) {
    var baseTrim = require_baseTrim();
    var isObject = require_isObject();
    var isSymbol = require_isSymbol();
    var NAN = 0 / 0;
    var reIsBadHex = /^[-+]0x[0-9a-f]+$/i;
    var reIsBinary = /^0b[01]+$/i;
    var reIsOctal = /^0o[0-7]+$/i;
    var freeParseInt = parseInt;
    function toNumber(value) {
      if (typeof value == "number") {
        return value;
      }
      if (isSymbol(value)) {
        return NAN;
      }
      if (isObject(value)) {
        var other = typeof value.valueOf == "function" ? value.valueOf() : value;
        value = isObject(other) ? other + "" : other;
      }
      if (typeof value != "string") {
        return value === 0 ? value : +value;
      }
      value = baseTrim(value);
      var isBinary2 = reIsBinary.test(value);
      return isBinary2 || reIsOctal.test(value) ? freeParseInt(value.slice(2), isBinary2 ? 2 : 8) : reIsBadHex.test(value) ? NAN : +value;
    }
    module.exports = toNumber;
  }
});

// node_modules/lodash/toFinite.js
var require_toFinite = __commonJS({
  "node_modules/lodash/toFinite.js"(exports, module) {
    var toNumber = require_toNumber();
    var INFINITY = 1 / 0;
    var MAX_INTEGER = 17976931348623157e292;
    function toFinite(value) {
      if (!value) {
        return value === 0 ? value : 0;
      }
      value = toNumber(value);
      if (value === INFINITY || value === -INFINITY) {
        var sign = value < 0 ? -1 : 1;
        return sign * MAX_INTEGER;
      }
      return value === value ? value : 0;
    }
    module.exports = toFinite;
  }
});

// node_modules/lodash/toInteger.js
var require_toInteger = __commonJS({
  "node_modules/lodash/toInteger.js"(exports, module) {
    var toFinite = require_toFinite();
    function toInteger(value) {
      var result = toFinite(value), remainder = result % 1;
      return result === result ? remainder ? result - remainder : result : 0;
    }
    module.exports = toInteger;
  }
});

// node_modules/lodash/before.js
var require_before = __commonJS({
  "node_modules/lodash/before.js"(exports, module) {
    var toInteger = require_toInteger();
    var FUNC_ERROR_TEXT = "Expected a function";
    function before(n, func) {
      var result;
      if (typeof func != "function") {
        throw new TypeError(FUNC_ERROR_TEXT);
      }
      n = toInteger(n);
      return function() {
        if (--n > 0) {
          result = func.apply(this, arguments);
        }
        if (n <= 1) {
          func = void 0;
        }
        return result;
      };
    }
    module.exports = before;
  }
});

// node_modules/lodash/once.js
var require_once = __commonJS({
  "node_modules/lodash/once.js"(exports, module) {
    var before = require_before();
    function once6(func) {
      return before(2, func);
    }
    module.exports = once6;
  }
});

// bundle/browser.render.txt
var require_browser_render = __commonJS({
  "bundle/browser.render.txt"(exports, module) {
    module.exports = 'var __defProp = Object.defineProperty;\nvar __getOwnPropDesc = Object.getOwnPropertyDescriptor;\nvar __decorateClass = (decorators, target, key, kind) => {\n  var result = kind > 1 ? void 0 : kind ? __getOwnPropDesc(target, key) : target;\n  for (var i7 = decorators.length - 1, decorator; i7 >= 0; i7--)\n    if (decorator = decorators[i7])\n      result = (kind ? decorator(target, key, result) : decorator(result)) || result;\n  if (kind && result)\n    __defProp(target, key, result);\n  return result;\n};\n\n// node_modules/@lit/reactive-element/css-tag.js\nvar t = window;\nvar e = t.ShadowRoot && (void 0 === t.ShadyCSS || t.ShadyCSS.nativeShadow) && "adoptedStyleSheets" in Document.prototype && "replace" in CSSStyleSheet.prototype;\nvar s = Symbol();\nvar n = /* @__PURE__ */ new WeakMap();\nvar o = class {\n  constructor(t4, e8, n6) {\n    if (this._$cssResult$ = true, n6 !== s)\n      throw Error("CSSResult is not constructable. Use `unsafeCSS` or `css` instead.");\n    this.cssText = t4, this.t = e8;\n  }\n  get styleSheet() {\n    let t4 = this.o;\n    const s6 = this.t;\n    if (e && void 0 === t4) {\n      const e8 = void 0 !== s6 && 1 === s6.length;\n      e8 && (t4 = n.get(s6)), void 0 === t4 && ((this.o = t4 = new CSSStyleSheet()).replaceSync(this.cssText), e8 && n.set(s6, t4));\n    }\n    return t4;\n  }\n  toString() {\n    return this.cssText;\n  }\n};\nvar r = (t4) => new o("string" == typeof t4 ? t4 : t4 + "", void 0, s);\nvar i = (t4, ...e8) => {\n  const n6 = 1 === t4.length ? t4[0] : e8.reduce((e9, s6, n7) => e9 + ((t5) => {\n    if (true === t5._$cssResult$)\n      return t5.cssText;\n    if ("number" == typeof t5)\n      return t5;\n    throw Error("Value passed to \'css\' function must be a \'css\' function result: " + t5 + ". Use \'unsafeCSS\' to pass non-literal values, but take care to ensure page security.");\n  })(s6) + t4[n7 + 1], t4[0]);\n  return new o(n6, t4, s);\n};\nvar S = (s6, n6) => {\n  e ? s6.adoptedStyleSheets = n6.map((t4) => t4 instanceof CSSStyleSheet ? t4 : t4.styleSheet) : n6.forEach((e8) => {\n    const n7 = document.createElement("style"), o6 = t.litNonce;\n    void 0 !== o6 && n7.setAttribute("nonce", o6), n7.textContent = e8.cssText, s6.appendChild(n7);\n  });\n};\nvar c = e ? (t4) => t4 : (t4) => t4 instanceof CSSStyleSheet ? ((t5) => {\n  let e8 = "";\n  for (const s6 of t5.cssRules)\n    e8 += s6.cssText;\n  return r(e8);\n})(t4) : t4;\n\n// node_modules/@lit/reactive-element/reactive-element.js\nvar s2;\nvar e2 = window;\nvar r2 = e2.trustedTypes;\nvar h = r2 ? r2.emptyScript : "";\nvar o2 = e2.reactiveElementPolyfillSupport;\nvar n2 = { toAttribute(t4, i7) {\n  switch (i7) {\n    case Boolean:\n      t4 = t4 ? h : null;\n      break;\n    case Object:\n    case Array:\n      t4 = null == t4 ? t4 : JSON.stringify(t4);\n  }\n  return t4;\n}, fromAttribute(t4, i7) {\n  let s6 = t4;\n  switch (i7) {\n    case Boolean:\n      s6 = null !== t4;\n      break;\n    case Number:\n      s6 = null === t4 ? null : Number(t4);\n      break;\n    case Object:\n    case Array:\n      try {\n        s6 = JSON.parse(t4);\n      } catch (t5) {\n        s6 = null;\n      }\n  }\n  return s6;\n} };\nvar a = (t4, i7) => i7 !== t4 && (i7 == i7 || t4 == t4);\nvar l = { attribute: true, type: String, converter: n2, reflect: false, hasChanged: a };\nvar d = class extends HTMLElement {\n  constructor() {\n    super(), this._$Ei = /* @__PURE__ */ new Map(), this.isUpdatePending = false, this.hasUpdated = false, this._$El = null, this.u();\n  }\n  static addInitializer(t4) {\n    var i7;\n    this.finalize(), (null !== (i7 = this.h) && void 0 !== i7 ? i7 : this.h = []).push(t4);\n  }\n  static get observedAttributes() {\n    this.finalize();\n    const t4 = [];\n    return this.elementProperties.forEach((i7, s6) => {\n      const e8 = this._$Ep(s6, i7);\n      void 0 !== e8 && (this._$Ev.set(e8, s6), t4.push(e8));\n    }), t4;\n  }\n  static createProperty(t4, i7 = l) {\n    if (i7.state && (i7.attribute = false), this.finalize(), this.elementProperties.set(t4, i7), !i7.noAccessor && !this.prototype.hasOwnProperty(t4)) {\n      const s6 = "symbol" == typeof t4 ? Symbol() : "__" + t4, e8 = this.getPropertyDescriptor(t4, s6, i7);\n      void 0 !== e8 && Object.defineProperty(this.prototype, t4, e8);\n    }\n  }\n  static getPropertyDescriptor(t4, i7, s6) {\n    return { get() {\n      return this[i7];\n    }, set(e8) {\n      const r5 = this[t4];\n      this[i7] = e8, this.requestUpdate(t4, r5, s6);\n    }, configurable: true, enumerable: true };\n  }\n  static getPropertyOptions(t4) {\n    return this.elementProperties.get(t4) || l;\n  }\n  static finalize() {\n    if (this.hasOwnProperty("finalized"))\n      return false;\n    this.finalized = true;\n    const t4 = Object.getPrototypeOf(this);\n    if (t4.finalize(), void 0 !== t4.h && (this.h = [...t4.h]), this.elementProperties = new Map(t4.elementProperties), this._$Ev = /* @__PURE__ */ new Map(), this.hasOwnProperty("properties")) {\n      const t5 = this.properties, i7 = [...Object.getOwnPropertyNames(t5), ...Object.getOwnPropertySymbols(t5)];\n      for (const s6 of i7)\n        this.createProperty(s6, t5[s6]);\n    }\n    return this.elementStyles = this.finalizeStyles(this.styles), true;\n  }\n  static finalizeStyles(i7) {\n    const s6 = [];\n    if (Array.isArray(i7)) {\n      const e8 = new Set(i7.flat(1 / 0).reverse());\n      for (const i8 of e8)\n        s6.unshift(c(i8));\n    } else\n      void 0 !== i7 && s6.push(c(i7));\n    return s6;\n  }\n  static _$Ep(t4, i7) {\n    const s6 = i7.attribute;\n    return false === s6 ? void 0 : "string" == typeof s6 ? s6 : "string" == typeof t4 ? t4.toLowerCase() : void 0;\n  }\n  u() {\n    var t4;\n    this._$E_ = new Promise((t5) => this.enableUpdating = t5), this._$AL = /* @__PURE__ */ new Map(), this._$Eg(), this.requestUpdate(), null === (t4 = this.constructor.h) || void 0 === t4 || t4.forEach((t5) => t5(this));\n  }\n  addController(t4) {\n    var i7, s6;\n    (null !== (i7 = this._$ES) && void 0 !== i7 ? i7 : this._$ES = []).push(t4), void 0 !== this.renderRoot && this.isConnected && (null === (s6 = t4.hostConnected) || void 0 === s6 || s6.call(t4));\n  }\n  removeController(t4) {\n    var i7;\n    null === (i7 = this._$ES) || void 0 === i7 || i7.splice(this._$ES.indexOf(t4) >>> 0, 1);\n  }\n  _$Eg() {\n    this.constructor.elementProperties.forEach((t4, i7) => {\n      this.hasOwnProperty(i7) && (this._$Ei.set(i7, this[i7]), delete this[i7]);\n    });\n  }\n  createRenderRoot() {\n    var t4;\n    const s6 = null !== (t4 = this.shadowRoot) && void 0 !== t4 ? t4 : this.attachShadow(this.constructor.shadowRootOptions);\n    return S(s6, this.constructor.elementStyles), s6;\n  }\n  connectedCallback() {\n    var t4;\n    void 0 === this.renderRoot && (this.renderRoot = this.createRenderRoot()), this.enableUpdating(true), null === (t4 = this._$ES) || void 0 === t4 || t4.forEach((t5) => {\n      var i7;\n      return null === (i7 = t5.hostConnected) || void 0 === i7 ? void 0 : i7.call(t5);\n    });\n  }\n  enableUpdating(t4) {\n  }\n  disconnectedCallback() {\n    var t4;\n    null === (t4 = this._$ES) || void 0 === t4 || t4.forEach((t5) => {\n      var i7;\n      return null === (i7 = t5.hostDisconnected) || void 0 === i7 ? void 0 : i7.call(t5);\n    });\n  }\n  attributeChangedCallback(t4, i7, s6) {\n    this._$AK(t4, s6);\n  }\n  _$EO(t4, i7, s6 = l) {\n    var e8;\n    const r5 = this.constructor._$Ep(t4, s6);\n    if (void 0 !== r5 && true === s6.reflect) {\n      const h3 = (void 0 !== (null === (e8 = s6.converter) || void 0 === e8 ? void 0 : e8.toAttribute) ? s6.converter : n2).toAttribute(i7, s6.type);\n      this._$El = t4, null == h3 ? this.removeAttribute(r5) : this.setAttribute(r5, h3), this._$El = null;\n    }\n  }\n  _$AK(t4, i7) {\n    var s6;\n    const e8 = this.constructor, r5 = e8._$Ev.get(t4);\n    if (void 0 !== r5 && this._$El !== r5) {\n      const t5 = e8.getPropertyOptions(r5), h3 = "function" == typeof t5.converter ? { fromAttribute: t5.converter } : void 0 !== (null === (s6 = t5.converter) || void 0 === s6 ? void 0 : s6.fromAttribute) ? t5.converter : n2;\n      this._$El = r5, this[r5] = h3.fromAttribute(i7, t5.type), this._$El = null;\n    }\n  }\n  requestUpdate(t4, i7, s6) {\n    let e8 = true;\n    void 0 !== t4 && (((s6 = s6 || this.constructor.getPropertyOptions(t4)).hasChanged || a)(this[t4], i7) ? (this._$AL.has(t4) || this._$AL.set(t4, i7), true === s6.reflect && this._$El !== t4 && (void 0 === this._$EC && (this._$EC = /* @__PURE__ */ new Map()), this._$EC.set(t4, s6))) : e8 = false), !this.isUpdatePending && e8 && (this._$E_ = this._$Ej());\n  }\n  async _$Ej() {\n    this.isUpdatePending = true;\n    try {\n      await this._$E_;\n    } catch (t5) {\n      Promise.reject(t5);\n    }\n    const t4 = this.scheduleUpdate();\n    return null != t4 && await t4, !this.isUpdatePending;\n  }\n  scheduleUpdate() {\n    return this.performUpdate();\n  }\n  performUpdate() {\n    var t4;\n    if (!this.isUpdatePending)\n      return;\n    this.hasUpdated, this._$Ei && (this._$Ei.forEach((t5, i8) => this[i8] = t5), this._$Ei = void 0);\n    let i7 = false;\n    const s6 = this._$AL;\n    try {\n      i7 = this.shouldUpdate(s6), i7 ? (this.willUpdate(s6), null === (t4 = this._$ES) || void 0 === t4 || t4.forEach((t5) => {\n        var i8;\n        return null === (i8 = t5.hostUpdate) || void 0 === i8 ? void 0 : i8.call(t5);\n      }), this.update(s6)) : this._$Ek();\n    } catch (t5) {\n      throw i7 = false, this._$Ek(), t5;\n    }\n    i7 && this._$AE(s6);\n  }\n  willUpdate(t4) {\n  }\n  _$AE(t4) {\n    var i7;\n    null === (i7 = this._$ES) || void 0 === i7 || i7.forEach((t5) => {\n      var i8;\n      return null === (i8 = t5.hostUpdated) || void 0 === i8 ? void 0 : i8.call(t5);\n    }), this.hasUpdated || (this.hasUpdated = true, this.firstUpdated(t4)), this.updated(t4);\n  }\n  _$Ek() {\n    this._$AL = /* @__PURE__ */ new Map(), this.isUpdatePending = false;\n  }\n  get updateComplete() {\n    return this.getUpdateComplete();\n  }\n  getUpdateComplete() {\n    return this._$E_;\n  }\n  shouldUpdate(t4) {\n    return true;\n  }\n  update(t4) {\n    void 0 !== this._$EC && (this._$EC.forEach((t5, i7) => this._$EO(i7, this[i7], t5)), this._$EC = void 0), this._$Ek();\n  }\n  updated(t4) {\n  }\n  firstUpdated(t4) {\n  }\n};\nd.finalized = true, d.elementProperties = /* @__PURE__ */ new Map(), d.elementStyles = [], d.shadowRootOptions = { mode: "open" }, null == o2 || o2({ ReactiveElement: d }), (null !== (s2 = e2.reactiveElementVersions) && void 0 !== s2 ? s2 : e2.reactiveElementVersions = []).push("1.6.1");\n\n// node_modules/lit-html/lit-html.js\nvar t2;\nvar i2 = window;\nvar s3 = i2.trustedTypes;\nvar e3 = s3 ? s3.createPolicy("lit-html", { createHTML: (t4) => t4 }) : void 0;\nvar o3 = `lit$${(Math.random() + "").slice(9)}$`;\nvar n3 = "?" + o3;\nvar l2 = `<${n3}>`;\nvar h2 = document;\nvar r3 = (t4 = "") => h2.createComment(t4);\nvar d2 = (t4) => null === t4 || "object" != typeof t4 && "function" != typeof t4;\nvar u = Array.isArray;\nvar c2 = (t4) => u(t4) || "function" == typeof (null == t4 ? void 0 : t4[Symbol.iterator]);\nvar v = /<(?:(!--|\\/[^a-zA-Z])|(\\/?[a-zA-Z][^>\\s]*)|(\\/?$))/g;\nvar a2 = /-->/g;\nvar f = />/g;\nvar _ = RegExp(`>|[ 	\n\\f\\r](?:([^\\\\s"\'>=/]+)([ 	\n\\f\\r]*=[ 	\n\\f\\r]*(?:[^ 	\n\\f\\r"\'\\`<>=]|("|\')|))|$)`, "g");\nvar m = /\'/g;\nvar p = /"/g;\nvar $ = /^(?:script|style|textarea|title)$/i;\nvar g = (t4) => (i7, ...s6) => ({ _$litType$: t4, strings: i7, values: s6 });\nvar y = g(1);\nvar w = g(2);\nvar x = Symbol.for("lit-noChange");\nvar b = Symbol.for("lit-nothing");\nvar T = /* @__PURE__ */ new WeakMap();\nvar A = h2.createTreeWalker(h2, 129, null, false);\nvar E = (t4, i7) => {\n  const s6 = t4.length - 1, n6 = [];\n  let h3, r5 = 2 === i7 ? "<svg>" : "", d3 = v;\n  for (let i8 = 0; i8 < s6; i8++) {\n    const s7 = t4[i8];\n    let e8, u5, c5 = -1, g2 = 0;\n    for (; g2 < s7.length && (d3.lastIndex = g2, u5 = d3.exec(s7), null !== u5); )\n      g2 = d3.lastIndex, d3 === v ? "!--" === u5[1] ? d3 = a2 : void 0 !== u5[1] ? d3 = f : void 0 !== u5[2] ? ($.test(u5[2]) && (h3 = RegExp("</" + u5[2], "g")), d3 = _) : void 0 !== u5[3] && (d3 = _) : d3 === _ ? ">" === u5[0] ? (d3 = null != h3 ? h3 : v, c5 = -1) : void 0 === u5[1] ? c5 = -2 : (c5 = d3.lastIndex - u5[2].length, e8 = u5[1], d3 = void 0 === u5[3] ? _ : \'"\' === u5[3] ? p : m) : d3 === p || d3 === m ? d3 = _ : d3 === a2 || d3 === f ? d3 = v : (d3 = _, h3 = void 0);\n    const y2 = d3 === _ && t4[i8 + 1].startsWith("/>") ? " " : "";\n    r5 += d3 === v ? s7 + l2 : c5 >= 0 ? (n6.push(e8), s7.slice(0, c5) + "$lit$" + s7.slice(c5) + o3 + y2) : s7 + o3 + (-2 === c5 ? (n6.push(void 0), i8) : y2);\n  }\n  const u4 = r5 + (t4[s6] || "<?>") + (2 === i7 ? "</svg>" : "");\n  if (!Array.isArray(t4) || !t4.hasOwnProperty("raw"))\n    throw Error("invalid template strings array");\n  return [void 0 !== e3 ? e3.createHTML(u4) : u4, n6];\n};\nvar C = class {\n  constructor({ strings: t4, _$litType$: i7 }, e8) {\n    let l6;\n    this.parts = [];\n    let h3 = 0, d3 = 0;\n    const u4 = t4.length - 1, c5 = this.parts, [v2, a3] = E(t4, i7);\n    if (this.el = C.createElement(v2, e8), A.currentNode = this.el.content, 2 === i7) {\n      const t5 = this.el.content, i8 = t5.firstChild;\n      i8.remove(), t5.append(...i8.childNodes);\n    }\n    for (; null !== (l6 = A.nextNode()) && c5.length < u4; ) {\n      if (1 === l6.nodeType) {\n        if (l6.hasAttributes()) {\n          const t5 = [];\n          for (const i8 of l6.getAttributeNames())\n            if (i8.endsWith("$lit$") || i8.startsWith(o3)) {\n              const s6 = a3[d3++];\n              if (t5.push(i8), void 0 !== s6) {\n                const t6 = l6.getAttribute(s6.toLowerCase() + "$lit$").split(o3), i9 = /([.?@])?(.*)/.exec(s6);\n                c5.push({ type: 1, index: h3, name: i9[2], strings: t6, ctor: "." === i9[1] ? M : "?" === i9[1] ? k : "@" === i9[1] ? H : S2 });\n              } else\n                c5.push({ type: 6, index: h3 });\n            }\n          for (const i8 of t5)\n            l6.removeAttribute(i8);\n        }\n        if ($.test(l6.tagName)) {\n          const t5 = l6.textContent.split(o3), i8 = t5.length - 1;\n          if (i8 > 0) {\n            l6.textContent = s3 ? s3.emptyScript : "";\n            for (let s6 = 0; s6 < i8; s6++)\n              l6.append(t5[s6], r3()), A.nextNode(), c5.push({ type: 2, index: ++h3 });\n            l6.append(t5[i8], r3());\n          }\n        }\n      } else if (8 === l6.nodeType)\n        if (l6.data === n3)\n          c5.push({ type: 2, index: h3 });\n        else {\n          let t5 = -1;\n          for (; -1 !== (t5 = l6.data.indexOf(o3, t5 + 1)); )\n            c5.push({ type: 7, index: h3 }), t5 += o3.length - 1;\n        }\n      h3++;\n    }\n  }\n  static createElement(t4, i7) {\n    const s6 = h2.createElement("template");\n    return s6.innerHTML = t4, s6;\n  }\n};\nfunction P(t4, i7, s6 = t4, e8) {\n  var o6, n6, l6, h3;\n  if (i7 === x)\n    return i7;\n  let r5 = void 0 !== e8 ? null === (o6 = s6._$Co) || void 0 === o6 ? void 0 : o6[e8] : s6._$Cl;\n  const u4 = d2(i7) ? void 0 : i7._$litDirective$;\n  return (null == r5 ? void 0 : r5.constructor) !== u4 && (null === (n6 = null == r5 ? void 0 : r5._$AO) || void 0 === n6 || n6.call(r5, false), void 0 === u4 ? r5 = void 0 : (r5 = new u4(t4), r5._$AT(t4, s6, e8)), void 0 !== e8 ? (null !== (l6 = (h3 = s6)._$Co) && void 0 !== l6 ? l6 : h3._$Co = [])[e8] = r5 : s6._$Cl = r5), void 0 !== r5 && (i7 = P(t4, r5._$AS(t4, i7.values), r5, e8)), i7;\n}\nvar V = class {\n  constructor(t4, i7) {\n    this.u = [], this._$AN = void 0, this._$AD = t4, this._$AM = i7;\n  }\n  get parentNode() {\n    return this._$AM.parentNode;\n  }\n  get _$AU() {\n    return this._$AM._$AU;\n  }\n  v(t4) {\n    var i7;\n    const { el: { content: s6 }, parts: e8 } = this._$AD, o6 = (null !== (i7 = null == t4 ? void 0 : t4.creationScope) && void 0 !== i7 ? i7 : h2).importNode(s6, true);\n    A.currentNode = o6;\n    let n6 = A.nextNode(), l6 = 0, r5 = 0, d3 = e8[0];\n    for (; void 0 !== d3; ) {\n      if (l6 === d3.index) {\n        let i8;\n        2 === d3.type ? i8 = new N(n6, n6.nextSibling, this, t4) : 1 === d3.type ? i8 = new d3.ctor(n6, d3.name, d3.strings, this, t4) : 6 === d3.type && (i8 = new I(n6, this, t4)), this.u.push(i8), d3 = e8[++r5];\n      }\n      l6 !== (null == d3 ? void 0 : d3.index) && (n6 = A.nextNode(), l6++);\n    }\n    return o6;\n  }\n  p(t4) {\n    let i7 = 0;\n    for (const s6 of this.u)\n      void 0 !== s6 && (void 0 !== s6.strings ? (s6._$AI(t4, s6, i7), i7 += s6.strings.length - 2) : s6._$AI(t4[i7])), i7++;\n  }\n};\nvar N = class {\n  constructor(t4, i7, s6, e8) {\n    var o6;\n    this.type = 2, this._$AH = b, this._$AN = void 0, this._$AA = t4, this._$AB = i7, this._$AM = s6, this.options = e8, this._$Cm = null === (o6 = null == e8 ? void 0 : e8.isConnected) || void 0 === o6 || o6;\n  }\n  get _$AU() {\n    var t4, i7;\n    return null !== (i7 = null === (t4 = this._$AM) || void 0 === t4 ? void 0 : t4._$AU) && void 0 !== i7 ? i7 : this._$Cm;\n  }\n  get parentNode() {\n    let t4 = this._$AA.parentNode;\n    const i7 = this._$AM;\n    return void 0 !== i7 && 11 === t4.nodeType && (t4 = i7.parentNode), t4;\n  }\n  get startNode() {\n    return this._$AA;\n  }\n  get endNode() {\n    return this._$AB;\n  }\n  _$AI(t4, i7 = this) {\n    t4 = P(this, t4, i7), d2(t4) ? t4 === b || null == t4 || "" === t4 ? (this._$AH !== b && this._$AR(), this._$AH = b) : t4 !== this._$AH && t4 !== x && this.g(t4) : void 0 !== t4._$litType$ ? this.$(t4) : void 0 !== t4.nodeType ? this.T(t4) : c2(t4) ? this.k(t4) : this.g(t4);\n  }\n  O(t4, i7 = this._$AB) {\n    return this._$AA.parentNode.insertBefore(t4, i7);\n  }\n  T(t4) {\n    this._$AH !== t4 && (this._$AR(), this._$AH = this.O(t4));\n  }\n  g(t4) {\n    this._$AH !== b && d2(this._$AH) ? this._$AA.nextSibling.data = t4 : this.T(h2.createTextNode(t4)), this._$AH = t4;\n  }\n  $(t4) {\n    var i7;\n    const { values: s6, _$litType$: e8 } = t4, o6 = "number" == typeof e8 ? this._$AC(t4) : (void 0 === e8.el && (e8.el = C.createElement(e8.h, this.options)), e8);\n    if ((null === (i7 = this._$AH) || void 0 === i7 ? void 0 : i7._$AD) === o6)\n      this._$AH.p(s6);\n    else {\n      const t5 = new V(o6, this), i8 = t5.v(this.options);\n      t5.p(s6), this.T(i8), this._$AH = t5;\n    }\n  }\n  _$AC(t4) {\n    let i7 = T.get(t4.strings);\n    return void 0 === i7 && T.set(t4.strings, i7 = new C(t4)), i7;\n  }\n  k(t4) {\n    u(this._$AH) || (this._$AH = [], this._$AR());\n    const i7 = this._$AH;\n    let s6, e8 = 0;\n    for (const o6 of t4)\n      e8 === i7.length ? i7.push(s6 = new N(this.O(r3()), this.O(r3()), this, this.options)) : s6 = i7[e8], s6._$AI(o6), e8++;\n    e8 < i7.length && (this._$AR(s6 && s6._$AB.nextSibling, e8), i7.length = e8);\n  }\n  _$AR(t4 = this._$AA.nextSibling, i7) {\n    var s6;\n    for (null === (s6 = this._$AP) || void 0 === s6 || s6.call(this, false, true, i7); t4 && t4 !== this._$AB; ) {\n      const i8 = t4.nextSibling;\n      t4.remove(), t4 = i8;\n    }\n  }\n  setConnected(t4) {\n    var i7;\n    void 0 === this._$AM && (this._$Cm = t4, null === (i7 = this._$AP) || void 0 === i7 || i7.call(this, t4));\n  }\n};\nvar S2 = class {\n  constructor(t4, i7, s6, e8, o6) {\n    this.type = 1, this._$AH = b, this._$AN = void 0, this.element = t4, this.name = i7, this._$AM = e8, this.options = o6, s6.length > 2 || "" !== s6[0] || "" !== s6[1] ? (this._$AH = Array(s6.length - 1).fill(new String()), this.strings = s6) : this._$AH = b;\n  }\n  get tagName() {\n    return this.element.tagName;\n  }\n  get _$AU() {\n    return this._$AM._$AU;\n  }\n  _$AI(t4, i7 = this, s6, e8) {\n    const o6 = this.strings;\n    let n6 = false;\n    if (void 0 === o6)\n      t4 = P(this, t4, i7, 0), n6 = !d2(t4) || t4 !== this._$AH && t4 !== x, n6 && (this._$AH = t4);\n    else {\n      const e9 = t4;\n      let l6, h3;\n      for (t4 = o6[0], l6 = 0; l6 < o6.length - 1; l6++)\n        h3 = P(this, e9[s6 + l6], i7, l6), h3 === x && (h3 = this._$AH[l6]), n6 || (n6 = !d2(h3) || h3 !== this._$AH[l6]), h3 === b ? t4 = b : t4 !== b && (t4 += (null != h3 ? h3 : "") + o6[l6 + 1]), this._$AH[l6] = h3;\n    }\n    n6 && !e8 && this.j(t4);\n  }\n  j(t4) {\n    t4 === b ? this.element.removeAttribute(this.name) : this.element.setAttribute(this.name, null != t4 ? t4 : "");\n  }\n};\nvar M = class extends S2 {\n  constructor() {\n    super(...arguments), this.type = 3;\n  }\n  j(t4) {\n    this.element[this.name] = t4 === b ? void 0 : t4;\n  }\n};\nvar R = s3 ? s3.emptyScript : "";\nvar k = class extends S2 {\n  constructor() {\n    super(...arguments), this.type = 4;\n  }\n  j(t4) {\n    t4 && t4 !== b ? this.element.setAttribute(this.name, R) : this.element.removeAttribute(this.name);\n  }\n};\nvar H = class extends S2 {\n  constructor(t4, i7, s6, e8, o6) {\n    super(t4, i7, s6, e8, o6), this.type = 5;\n  }\n  _$AI(t4, i7 = this) {\n    var s6;\n    if ((t4 = null !== (s6 = P(this, t4, i7, 0)) && void 0 !== s6 ? s6 : b) === x)\n      return;\n    const e8 = this._$AH, o6 = t4 === b && e8 !== b || t4.capture !== e8.capture || t4.once !== e8.once || t4.passive !== e8.passive, n6 = t4 !== b && (e8 === b || o6);\n    o6 && this.element.removeEventListener(this.name, this, e8), n6 && this.element.addEventListener(this.name, this, t4), this._$AH = t4;\n  }\n  handleEvent(t4) {\n    var i7, s6;\n    "function" == typeof this._$AH ? this._$AH.call(null !== (s6 = null === (i7 = this.options) || void 0 === i7 ? void 0 : i7.host) && void 0 !== s6 ? s6 : this.element, t4) : this._$AH.handleEvent(t4);\n  }\n};\nvar I = class {\n  constructor(t4, i7, s6) {\n    this.element = t4, this.type = 6, this._$AN = void 0, this._$AM = i7, this.options = s6;\n  }\n  get _$AU() {\n    return this._$AM._$AU;\n  }\n  _$AI(t4) {\n    P(this, t4);\n  }\n};\nvar L = { P: "$lit$", A: o3, M: n3, C: 1, L: E, R: V, D: c2, V: P, I: N, H: S2, N: k, U: H, B: M, F: I };\nvar z = i2.litHtmlPolyfillSupport;\nnull == z || z(C, N), (null !== (t2 = i2.litHtmlVersions) && void 0 !== t2 ? t2 : i2.litHtmlVersions = []).push("2.6.1");\nvar Z = (t4, i7, s6) => {\n  var e8, o6;\n  const n6 = null !== (e8 = null == s6 ? void 0 : s6.renderBefore) && void 0 !== e8 ? e8 : i7;\n  let l6 = n6._$litPart$;\n  if (void 0 === l6) {\n    const t5 = null !== (o6 = null == s6 ? void 0 : s6.renderBefore) && void 0 !== o6 ? o6 : null;\n    n6._$litPart$ = l6 = new N(i7.insertBefore(r3(), t5), t5, void 0, null != s6 ? s6 : {});\n  }\n  return l6._$AI(t4), l6;\n};\n\n// node_modules/lit-element/lit-element.js\nvar l3;\nvar o4;\nvar s4 = class extends d {\n  constructor() {\n    super(...arguments), this.renderOptions = { host: this }, this._$Do = void 0;\n  }\n  createRenderRoot() {\n    var t4, e8;\n    const i7 = super.createRenderRoot();\n    return null !== (t4 = (e8 = this.renderOptions).renderBefore) && void 0 !== t4 || (e8.renderBefore = i7.firstChild), i7;\n  }\n  update(t4) {\n    const i7 = this.render();\n    this.hasUpdated || (this.renderOptions.isConnected = this.isConnected), super.update(t4), this._$Do = Z(i7, this.renderRoot, this.renderOptions);\n  }\n  connectedCallback() {\n    var t4;\n    super.connectedCallback(), null === (t4 = this._$Do) || void 0 === t4 || t4.setConnected(true);\n  }\n  disconnectedCallback() {\n    var t4;\n    super.disconnectedCallback(), null === (t4 = this._$Do) || void 0 === t4 || t4.setConnected(false);\n  }\n  render() {\n    return x;\n  }\n};\ns4.finalized = true, s4._$litElement$ = true, null === (l3 = globalThis.litElementHydrateSupport) || void 0 === l3 || l3.call(globalThis, { LitElement: s4 });\nvar n4 = globalThis.litElementPolyfillSupport;\nnull == n4 || n4({ LitElement: s4 });\n(null !== (o4 = globalThis.litElementVersions) && void 0 !== o4 ? o4 : globalThis.litElementVersions = []).push("3.2.2");\n\n// node_modules/lit-html/directive.js\nvar t3 = { ATTRIBUTE: 1, CHILD: 2, PROPERTY: 3, BOOLEAN_ATTRIBUTE: 4, EVENT: 5, ELEMENT: 6 };\nvar e4 = (t4) => (...e8) => ({ _$litDirective$: t4, values: e8 });\nvar i3 = class {\n  constructor(t4) {\n  }\n  get _$AU() {\n    return this._$AM._$AU;\n  }\n  _$AT(t4, e8, i7) {\n    this._$Ct = t4, this._$AM = e8, this._$Ci = i7;\n  }\n  _$AS(t4, e8) {\n    return this.update(t4, e8);\n  }\n  update(t4, e8) {\n    return this.render(...e8);\n  }\n};\n\n// node_modules/lit-html/directives/style-map.js\nvar i4 = e4(class extends i3 {\n  constructor(t4) {\n    var e8;\n    if (super(t4), t4.type !== t3.ATTRIBUTE || "style" !== t4.name || (null === (e8 = t4.strings) || void 0 === e8 ? void 0 : e8.length) > 2)\n      throw Error("The `styleMap` directive must be used in the `style` attribute and must be the only part in the attribute.");\n  }\n  render(t4) {\n    return Object.keys(t4).reduce((e8, r5) => {\n      const s6 = t4[r5];\n      return null == s6 ? e8 : e8 + `${r5 = r5.replace(/(?:^(webkit|moz|ms|o)|)(?=[A-Z])/g, "-$&").toLowerCase()}:${s6};`;\n    }, "");\n  }\n  update(e8, [r5]) {\n    const { style: s6 } = e8.element;\n    if (void 0 === this.vt) {\n      this.vt = /* @__PURE__ */ new Set();\n      for (const t4 in r5)\n        this.vt.add(t4);\n      return this.render(r5);\n    }\n    this.vt.forEach((t4) => {\n      null == r5[t4] && (this.vt.delete(t4), t4.includes("-") ? s6.removeProperty(t4) : s6[t4] = "");\n    });\n    for (const t4 in r5) {\n      const e9 = r5[t4];\n      null != e9 && (this.vt.add(t4), t4.includes("-") ? s6.setProperty(t4, e9) : s6[t4] = e9);\n    }\n    return x;\n  }\n});\n\n// node_modules/@lit/reactive-element/decorators/custom-element.js\nvar e5 = (e8) => (n6) => "function" == typeof n6 ? ((e9, n7) => (customElements.define(e9, n7), n7))(e8, n6) : ((e9, n7) => {\n  const { kind: t4, elements: s6 } = n7;\n  return { kind: t4, elements: s6, finisher(n8) {\n    customElements.define(e9, n8);\n  } };\n})(e8, n6);\n\n// node_modules/@lit/reactive-element/decorators/property.js\nvar i5 = (i7, e8) => "method" === e8.kind && e8.descriptor && !("value" in e8.descriptor) ? { ...e8, finisher(n6) {\n  n6.createProperty(e8.key, i7);\n} } : { kind: "field", key: Symbol(), placement: "own", descriptor: {}, originalKey: e8.key, initializer() {\n  "function" == typeof e8.initializer && (this[e8.key] = e8.initializer.call(this));\n}, finisher(n6) {\n  n6.createProperty(e8.key, i7);\n} };\nfunction e6(e8) {\n  return (n6, t4) => void 0 !== t4 ? ((i7, e9, n7) => {\n    e9.constructor.createProperty(n7, i7);\n  })(e8, n6, t4) : i5(e8, n6);\n}\n\n// node_modules/@lit/reactive-element/decorators/base.js\nvar o5 = ({ finisher: e8, descriptor: t4 }) => (o6, n6) => {\n  var r5;\n  if (void 0 === n6) {\n    const n7 = null !== (r5 = o6.originalKey) && void 0 !== r5 ? r5 : o6.key, i7 = null != t4 ? { kind: "method", placement: "prototype", key: n7, descriptor: t4(o6.key) } : { ...o6, key: n7 };\n    return null != e8 && (i7.finisher = function(t5) {\n      e8(t5, n7);\n    }), i7;\n  }\n  {\n    const r6 = o6.constructor;\n    void 0 !== t4 && Object.defineProperty(o6, n6, t4(n6)), null == e8 || e8(r6, n6);\n  }\n};\n\n// node_modules/@lit/reactive-element/decorators/query.js\nfunction i6(i7, n6) {\n  return o5({ descriptor: (o6) => {\n    const t4 = { get() {\n      var o7, n7;\n      return null !== (n7 = null === (o7 = this.renderRoot) || void 0 === o7 ? void 0 : o7.querySelector(i7)) && void 0 !== n7 ? n7 : null;\n    }, enumerable: true, configurable: true };\n    if (n6) {\n      const n7 = "symbol" == typeof o6 ? Symbol() : "__" + o6;\n      t4.get = function() {\n        var o7, t5;\n        return void 0 === this[n7] && (this[n7] = null !== (t5 = null === (o7 = this.renderRoot) || void 0 === o7 ? void 0 : o7.querySelector(i7)) && void 0 !== t5 ? t5 : null), this[n7];\n      };\n    }\n    return t4;\n  } });\n}\n\n// node_modules/@lit/reactive-element/decorators/query-assigned-elements.js\nvar n5;\nvar e7 = null != (null === (n5 = window.HTMLSlotElement) || void 0 === n5 ? void 0 : n5.prototype.assignedElements) ? (o6, n6) => o6.assignedElements(n6) : (o6, n6) => o6.assignedNodes(n6).filter((o7) => o7.nodeType === Node.ELEMENT_NODE);\n\n// node_modules/lit-html/directive-helpers.js\nvar { I: l5 } = L;\nvar c3 = () => document.createComment("");\nvar r4 = (o6, t4, i7) => {\n  var n6;\n  const d3 = o6._$AA.parentNode, v2 = void 0 === t4 ? o6._$AB : t4._$AA;\n  if (void 0 === i7) {\n    const t5 = d3.insertBefore(c3(), v2), n7 = d3.insertBefore(c3(), v2);\n    i7 = new l5(t5, n7, o6, o6.options);\n  } else {\n    const l6 = i7._$AB.nextSibling, t5 = i7._$AM, e8 = t5 !== o6;\n    if (e8) {\n      let l7;\n      null === (n6 = i7._$AQ) || void 0 === n6 || n6.call(i7, o6), i7._$AM = o6, void 0 !== i7._$AP && (l7 = o6._$AU) !== t5._$AU && i7._$AP(l7);\n    }\n    if (l6 !== v2 || e8) {\n      let o7 = i7._$AA;\n      for (; o7 !== l6; ) {\n        const l7 = o7.nextSibling;\n        d3.insertBefore(o7, v2), o7 = l7;\n      }\n    }\n  }\n  return i7;\n};\nvar u2 = (o6, l6, t4 = o6) => (o6._$AI(l6, t4), o6);\nvar f2 = {};\nvar s5 = (o6, l6 = f2) => o6._$AH = l6;\nvar m2 = (o6) => o6._$AH;\nvar p2 = (o6) => {\n  var l6;\n  null === (l6 = o6._$AP) || void 0 === l6 || l6.call(o6, false, true);\n  let t4 = o6._$AA;\n  const i7 = o6._$AB.nextSibling;\n  for (; t4 !== i7; ) {\n    const o7 = t4.nextSibling;\n    t4.remove(), t4 = o7;\n  }\n};\n\n// node_modules/lit-html/directives/repeat.js\nvar u3 = (e8, s6, t4) => {\n  const r5 = /* @__PURE__ */ new Map();\n  for (let l6 = s6; l6 <= t4; l6++)\n    r5.set(e8[l6], l6);\n  return r5;\n};\nvar c4 = e4(class extends i3 {\n  constructor(e8) {\n    if (super(e8), e8.type !== t3.CHILD)\n      throw Error("repeat() can only be used in text expressions");\n  }\n  ht(e8, s6, t4) {\n    let r5;\n    void 0 === t4 ? t4 = s6 : void 0 !== s6 && (r5 = s6);\n    const l6 = [], o6 = [];\n    let i7 = 0;\n    for (const s7 of e8)\n      l6[i7] = r5 ? r5(s7, i7) : i7, o6[i7] = t4(s7, i7), i7++;\n    return { values: o6, keys: l6 };\n  }\n  render(e8, s6, t4) {\n    return this.ht(e8, s6, t4).values;\n  }\n  update(s6, [t4, r5, c5]) {\n    var d3;\n    const a3 = m2(s6), { values: p3, keys: v2 } = this.ht(t4, r5, c5);\n    if (!Array.isArray(a3))\n      return this.ut = v2, p3;\n    const h3 = null !== (d3 = this.ut) && void 0 !== d3 ? d3 : this.ut = [], m3 = [];\n    let y2, x2, j = 0, k2 = a3.length - 1, w2 = 0, A2 = p3.length - 1;\n    for (; j <= k2 && w2 <= A2; )\n      if (null === a3[j])\n        j++;\n      else if (null === a3[k2])\n        k2--;\n      else if (h3[j] === v2[w2])\n        m3[w2] = u2(a3[j], p3[w2]), j++, w2++;\n      else if (h3[k2] === v2[A2])\n        m3[A2] = u2(a3[k2], p3[A2]), k2--, A2--;\n      else if (h3[j] === v2[A2])\n        m3[A2] = u2(a3[j], p3[A2]), r4(s6, m3[A2 + 1], a3[j]), j++, A2--;\n      else if (h3[k2] === v2[w2])\n        m3[w2] = u2(a3[k2], p3[w2]), r4(s6, a3[j], a3[k2]), k2--, w2++;\n      else if (void 0 === y2 && (y2 = u3(v2, w2, A2), x2 = u3(h3, j, k2)), y2.has(h3[j]))\n        if (y2.has(h3[k2])) {\n          const e8 = x2.get(v2[w2]), t5 = void 0 !== e8 ? a3[e8] : null;\n          if (null === t5) {\n            const e9 = r4(s6, a3[j]);\n            u2(e9, p3[w2]), m3[w2] = e9;\n          } else\n            m3[w2] = u2(t5, p3[w2]), r4(s6, a3[j], t5), a3[e8] = null;\n          w2++;\n        } else\n          p2(a3[k2]), k2--;\n      else\n        p2(a3[j]), j++;\n    for (; w2 <= A2; ) {\n      const e8 = r4(s6, m3[A2 + 1]);\n      u2(e8, p3[w2]), m3[w2++] = e8;\n    }\n    for (; j <= k2; ) {\n      const e8 = a3[j++];\n      null !== e8 && p2(e8);\n    }\n    return this.ut = v2, s5(s6, m3), x;\n  }\n});\n\n// src/user/browser/assets/browser.render.mts\nvar styles = [\n  i`\n        .page-container{\n            display: flex;\n            flex-direction: column;\n            justify-content: flex-start;\n            align-items: center;\n            box-sizing: border-box;\n            height:100%;\n            \n        }\n\n        .logo-container{\n            display: flex;\n            justify-content: center;\n            align-items:flex-start;\n            margin-top: 30px;\n            width: 100px;\n            height: 60px;\n            background: #0001;\n        }\n\n        .search-container{\n            display: flex;\n            justify-content: center;\n            margin-top: 66px;\n            width: 80%;\n            height: 48px;\n            border-radius: 50px;\n            background: #0001;\n            overflow: hidden;\n            border: 1px solid #ddd;\n        }\n\n        .search-input{\n            box-sizing: border-box;\n            padding: 0px 16px;\n            flex-grow: 1;\n            width: 10px;\n            height: 100%;\n            outline: none;\n            border: none;\n        }\n\n        .search-input::placeholder {\n            color: #ddd;\n            text-align: center;\n          }\n\n        .search-bottom{\n            flex: 0 0 88px;\n            height: 48px;\n            line-height: 48px;\n            text-align: center;\n            color: #666;\n            border: none;\n        }\n\n        .apps-container{\n            width: 80%;\n            height: auto;\n        }\n\n        .row-container{\n            --size: 60px;\n            display: flex;\n            justify-content: flex-start;\n            padding-top: 30px;\n            height: var(--size);\n        }\n\n        .item-container{\n            display: flex;\n            justify-content: center;\n            align-items: center;\n            flex-grow: 0;\n            flex-shrink: 0;\n            box-sizing: border-box;\n            padding:10px;\n            width: var(--size);\n            height: var(--size);\n            border-radius: 16px;\n            background-color: #ddd1;\n            background-position: center;\n            background-size: contain;\n            background-repeat: no-repeat;\n            cursor: pointer;\n        }\n\n        .item-container:nth-of-type(2n){\n            margin: 0px calc((100% - var(--size) * 3) / 2);\n        }\n         \n    `\n];\nvar HomePage = class extends s4 {\n  constructor() {\n    super();\n    this.apps = [];\n    this.getAllAppsInfo();\n  }\n  render() {\n    const arr = toTwoDimensionalArray(this.apps);\n    return y`\n            <div \n                class="page-container"\n            >\n                <div class="logo-container">logo---</div>\n                <div class="search-container">\n                   <input class="search-input" placeholder="search app" value="https://shop.plaoc.com/W85DEFE5/W85DEFE5.bfsa"/>\n                   <button class="search-bottom" @click=${this.onSearch} >DOWNLOAD</button>\n                </div>\n                <div class="apps-container">\n                    ${c4(arr, (rows, index) => index, (rows, index) => y`\n                                <div class="row-container">\n                                    ${c4(rows, (item) => item.bfsAppId, (item) => {\n      return y`\n                                                <app-col .item=${item} class="item-container" @click=${() => this.onOpenApp(item.appId)}></app-col>\n                                            `;\n    })}\n                                </div>\n                            `)}\n                </div>\n\n                <!-- \u5B9E\u9A8C\u8BE5\u6539\u53D8\u72B6\u6001\u680F -->\n                <button @click=${() => this.setStatusbarBackground("#F00F")}>\u8BBE\u7F6E\u72B6\u6001\u680F\u7684\u989C\u8272 === #F00F</button>\n                <button @click=${() => this.setStatusbarBackground("#0F0F")}>\u8BBE\u7F6E\u72B6\u6001\u680F\u7684\u989C\u8272 === #0F0F</button>\n                <button @click=${() => this.setStatusbarBackground("#00FF")}>\u8BBE\u7F6E\u72B6\u6001\u680F\u7684\u989C\u8272 === #00FF</button>\n                <button @click=${() => this.setStatusbarStyle("light")}>\u8BBE\u7F6E\u72B6\u6001\u680F\u7684\u98CE\u683C === light</button>\n                <button @click=${() => this.setStatusbarStyle("dark")}>\u8BBE\u7F6E\u72B6\u6001\u680F\u7684\u98CE\u683C === dark</button>\n                <button @click=${() => this.setStatusbarStyle("default")}>\u8BBE\u7F6E\u72B6\u6001\u680F\u7684\u98CE\u683C === default</button>\n                <button @click=${() => this.getStatusbarStyle()}>\u83B7\u53D6\u72B6\u6001\u680F\u7684\u98CE\u683C</button>\n                <button @click=${() => this.setStatusbarOverlays("0")}>\u83B7\u53D6\u72B6\u6001\u680F\u7684overlays \u4E0D\u8986\u76D6</button>\n                <button @click=${() => this.setStatusbarOverlays("1")}>\u83B7\u53D6\u72B6\u6001\u680F\u7684overlays \u8986\u76D6</button>\n            </div>\n        `;\n  }\n  connectedCallback() {\n    super.connectedCallback();\n  }\n  onSearch() {\n    fetch(`./download?url=${this.elInput?.value}`).then(async (res) => {\n      console.log("\\u4E0B\\u8F7D\\u6210\\u529F\\u4E86---res: ", await res.json());\n    }).then(this.getAllAppsInfo).catch((err) => console.log("\\u4E0B\\u8F7D\\u5931\\u8D25\\u4E86"));\n  }\n  getAllAppsInfo() {\n    console.log("\\u5F00\\u59CB\\u83B7\\u53D6 \\u5168\\u90E8 appsInfo");\n    fetch(`./appsinfo`).then(async (res) => {\n      console.log("res: ", res);\n      const _json = await res.json();\n      this.apps = JSON.parse(_json);\n    }).catch((err) => {\n      console.log("\\u83B7\\u53D6\\u5168\\u90E8 appsInfo error: ", err);\n    });\n  }\n  async onOpenApp(appId) {\n    let response = await fetch(`./install?appId=${appId}`);\n    if (response.status !== 200) {\n      console.error("\\u5B89\\u88C5\\u5E94\\u7528\\u5931\\u8D25 appId: ", appId, response.text());\n      return;\n    }\n    response = await fetch(`./open?appId=${appId}`);\n  }\n  async setStatusbarBackground(color) {\n    const el = document.querySelector("statusbar-dweb");\n    if (el === null)\n      return console.error("\\u8BBE\\u7F6E statusbar\\u9519\\u8BEF el === null");\n    const result = await el.setBackgroundColor(color);\n  }\n  async setStatusbarStyle(value) {\n    const el = document.querySelector("statusbar-dweb");\n    if (el === null)\n      return console.error("\\u8BBE\\u7F6E statusbar\\u9519\\u8BEF el === null");\n    const result = await el.setStyle(value);\n  }\n  async getStatusbarStyle() {\n    const el = document.querySelector("statusbar-dweb");\n    if (el === null)\n      return console.error("\\u8BBE\\u7F6E statusbar\\u9519\\u8BEF el === null");\n    const result = await el.getStyle();\n  }\n  async setStatusbarOverlays(value) {\n    const el = document.querySelector("statusbar-dweb");\n    if (el === null)\n      return console.error("\\u8BBE\\u7F6E statusbar\\u9519\\u8BEF el === null");\n    const result = await el.setOverlaysWebview(value);\n  }\n};\nHomePage.styles = styles;\n__decorateClass([\n  e6()\n], HomePage.prototype, "apps", 2);\n__decorateClass([\n  i6(".search-input")\n], HomePage.prototype, "elInput", 2);\nHomePage = __decorateClass([\n  e5("home-page")\n], HomePage);\nfunction toTwoDimensionalArray(items) {\n  let twoDimensionalArr = [];\n  items.forEach((item, index) => {\n    const rowIndex = Math.floor(index / 3);\n    const colIndex = index % 3;\n    twoDimensionalArr[rowIndex] = twoDimensionalArr[rowIndex] ? twoDimensionalArr[rowIndex] : [];\n    twoDimensionalArr[rowIndex][colIndex] = item;\n  });\n  return twoDimensionalArr;\n}\nvar AppCol = class extends s4 {\n  constructor() {\n    super(...arguments);\n    this.item = void 0;\n  }\n  render() {\n    const _styleMap = i4({\n      backgroundImage: "url(./icon/" + this.item?.bfsAppId + "/sys" + this.item?.icon + ")"\n    });\n    return y`<div class="container" style=${_styleMap} ></div>`;\n  }\n};\nAppCol.styles = [\n  i`\n            .container{\n                display: flex;\n                justify-content: center;\n                align-items: center;\n                box-sizing: border-box;\n                padding:10px;\n                width: 100%;\n                height: 100%;\n                border-radius: 16px;\n                background-color: #ddd1;\n                background-position: center;\n                background-size: contain;\n                background-repeat: no-repeat;\n                cursor: pointer;\n            }\n        `\n];\n__decorateClass([\n  e6()\n], AppCol.prototype, "item", 2);\nAppCol = __decorateClass([\n  e5("app-col")\n], AppCol);\n/*! Bundled license information:\n\n@lit/reactive-element/css-tag.js:\n  (**\n   * @license\n   * Copyright 2019 Google LLC\n   * SPDX-License-Identifier: BSD-3-Clause\n   *)\n\n@lit/reactive-element/reactive-element.js:\n  (**\n   * @license\n   * Copyright 2017 Google LLC\n   * SPDX-License-Identifier: BSD-3-Clause\n   *)\n\nlit-html/lit-html.js:\n  (**\n   * @license\n   * Copyright 2017 Google LLC\n   * SPDX-License-Identifier: BSD-3-Clause\n   *)\n\nlit-element/lit-element.js:\n  (**\n   * @license\n   * Copyright 2017 Google LLC\n   * SPDX-License-Identifier: BSD-3-Clause\n   *)\n\nlit-html/is-server.js:\n  (**\n   * @license\n   * Copyright 2022 Google LLC\n   * SPDX-License-Identifier: BSD-3-Clause\n   *)\n\nlit-html/directive.js:\n  (**\n   * @license\n   * Copyright 2017 Google LLC\n   * SPDX-License-Identifier: BSD-3-Clause\n   *)\n\nlit-html/directives/style-map.js:\n  (**\n   * @license\n   * Copyright 2018 Google LLC\n   * SPDX-License-Identifier: BSD-3-Clause\n   *)\n\n@lit/reactive-element/decorators/custom-element.js:\n  (**\n   * @license\n   * Copyright 2017 Google LLC\n   * SPDX-License-Identifier: BSD-3-Clause\n   *)\n\n@lit/reactive-element/decorators/property.js:\n  (**\n   * @license\n   * Copyright 2017 Google LLC\n   * SPDX-License-Identifier: BSD-3-Clause\n   *)\n\n@lit/reactive-element/decorators/state.js:\n  (**\n   * @license\n   * Copyright 2017 Google LLC\n   * SPDX-License-Identifier: BSD-3-Clause\n   *)\n\n@lit/reactive-element/decorators/base.js:\n  (**\n   * @license\n   * Copyright 2017 Google LLC\n   * SPDX-License-Identifier: BSD-3-Clause\n   *)\n\n@lit/reactive-element/decorators/event-options.js:\n  (**\n   * @license\n   * Copyright 2017 Google LLC\n   * SPDX-License-Identifier: BSD-3-Clause\n   *)\n\n@lit/reactive-element/decorators/query.js:\n  (**\n   * @license\n   * Copyright 2017 Google LLC\n   * SPDX-License-Identifier: BSD-3-Clause\n   *)\n\n@lit/reactive-element/decorators/query-all.js:\n  (**\n   * @license\n   * Copyright 2017 Google LLC\n   * SPDX-License-Identifier: BSD-3-Clause\n   *)\n\n@lit/reactive-element/decorators/query-async.js:\n  (**\n   * @license\n   * Copyright 2017 Google LLC\n   * SPDX-License-Identifier: BSD-3-Clause\n   *)\n\n@lit/reactive-element/decorators/query-assigned-elements.js:\n  (**\n   * @license\n   * Copyright 2021 Google LLC\n   * SPDX-License-Identifier: BSD-3-Clause\n   *)\n\n@lit/reactive-element/decorators/query-assigned-nodes.js:\n  (**\n   * @license\n   * Copyright 2017 Google LLC\n   * SPDX-License-Identifier: BSD-3-Clause\n   *)\n\nlit-html/directive-helpers.js:\n  (**\n   * @license\n   * Copyright 2020 Google LLC\n   * SPDX-License-Identifier: BSD-3-Clause\n   *)\n\nlit-html/directives/repeat.js:\n  (**\n   * @license\n   * Copyright 2017 Google LLC\n   * SPDX-License-Identifier: BSD-3-Clause\n   *)\n*/\n';
  }
});

// src/core/ipc/IpcHeaders.cts
var IpcHeaders = class extends Headers {
  init(key, value) {
    if (this.has(key)) {
      return;
    }
    this.set(key, value);
  }
  toJSON() {
    const record = {};
    this.forEach((value, key) => {
      record[key] = value;
    });
    return record;
  }
};

// src/core/ipc/IpcResponse.cts
var import_once = __toESM(require_once());

// src/helper/binaryHelper.cts
var isBinary = (data) => data instanceof ArrayBuffer || ArrayBuffer.isView(data);
var binaryToU8a = (binary) => {
  if (binary instanceof ArrayBuffer) {
    return new Uint8Array(binary);
  }
  if (binary instanceof Uint8Array) {
    return binary;
  }
  return new Uint8Array(binary.buffer, binary.byteOffset, binary.byteLength);
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

// src/core/ipc/const.cts
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
var $metaBodyToBinary = (metaBody) => {
  const [type, data] = metaBody;
  switch (type) {
    case IPC_META_BODY_TYPE.BINARY: {
      return data;
    }
    case IPC_META_BODY_TYPE.BASE64: {
      return simpleEncoder(data, "base64");
    }
    case IPC_META_BODY_TYPE.TEXT: {
      return simpleEncoder(data, "utf8");
    }
  }
  throw new Error(`invalid metaBody.type :${type}`);
};
var IPC_META_BODY_TYPE = /* @__PURE__ */ ((IPC_META_BODY_TYPE2) => {
  IPC_META_BODY_TYPE2[IPC_META_BODY_TYPE2["STREAM_ID"] = 0] = "STREAM_ID";
  IPC_META_BODY_TYPE2[IPC_META_BODY_TYPE2["INLINE"] = 1] = "INLINE";
  IPC_META_BODY_TYPE2[IPC_META_BODY_TYPE2["TEXT"] = 3] = "TEXT";
  IPC_META_BODY_TYPE2[IPC_META_BODY_TYPE2["BASE64"] = 5] = "BASE64";
  IPC_META_BODY_TYPE2[IPC_META_BODY_TYPE2["BINARY"] = 9] = "BINARY";
  return IPC_META_BODY_TYPE2;
})(IPC_META_BODY_TYPE || {});
var IpcMessage = class {
  constructor(type) {
    this.type = type;
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
async function* _doRead(reader) {
  try {
    while (true) {
      const item = await reader.read();
      if (item.done) {
        break;
      }
      yield item.value;
    }
  } finally {
    reader.releaseLock();
  }
}
var streamRead = (stream, options = {}) => {
  return _doRead(stream.getReader());
};
var binaryStreamRead = (stream, options = {}) => {
  const reader = streamRead(stream, options);
  var done = false;
  var cache = new Uint8Array(0);
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
  const items = [];
  const maps = [];
  for await (const item of _doRead(stream.getReader())) {
    items.push(item);
    if (options.map) {
      maps.push(options.map(item));
    }
  }
  const result = options.complete?.(items, maps);
  return {
    items,
    maps,
    result
  };
};
var streamReadAllBuffer = async (stream) => {
  return (await streamReadAll(stream, {
    complete(items) {
      return u8aConcat(items);
    }
  })).result;
};
var ReadableStreamOut = class {
  constructor() {
    this.stream = new ReadableStream({
      start: (controller) => {
        this.controller = controller;
      },
      pull: () => {
        this._on_pull_signal?.emit();
      }
    });
  }
  get onPull() {
    return (this._on_pull_signal ??= createSignal()).listen;
  }
};

// src/core/ipc/IpcBody.cts
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
      _IpcBody.wm.set(body_u8a, this);
    }
    return body_u8a;
  }
  async stream() {
    const bodyHub = this._bodyHub;
    let body_stream = bodyHub.stream;
    if (body_stream === void 0) {
      body_stream = new Blob([await this.u8a()]).stream();
      bodyHub.stream = body_stream;
      _IpcBody.wm.set(body_stream, this);
    }
    return body_stream;
  }
  async text() {
    const bodyHub = await this._bodyHub;
    let body_text = bodyHub.text;
    if (body_text === void 0) {
      body_text = simpleDecoder(await this.u8a(), "utf8");
      bodyHub.text = body_text;
    }
    return body_text;
  }
};
var IpcBody = _IpcBody;
IpcBody.wm = /* @__PURE__ */ new WeakMap();
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

// src/core/ipc/IpcStreamAbort.cts
var IpcStreamAbort = class extends IpcMessage {
  constructor(stream_id) {
    super(5 /* STREAM_ABORT */);
    this.stream_id = stream_id;
  }
};

// src/core/ipc/IpcStreamData.cts
var IpcStreamData = class extends IpcMessage {
  constructor(stream_id, data, encoding) {
    super(2 /* STREAM_DATA */);
    this.stream_id = stream_id;
    this.data = data;
    this.encoding = encoding;
  }
  static asBase64(stream_id, data) {
    return new IpcStreamData(
      stream_id,
      simpleDecoder(data, "base64"),
      4 /* BASE64 */
    );
  }
  static asBinary(stream_id, data) {
    return new IpcStreamData(stream_id, data, 8 /* BINARY */);
  }
  static asUtf8(stream_id, data) {
    return new IpcStreamData(
      stream_id,
      simpleDecoder(data, "utf8"),
      2 /* UTF8 */
    );
  }
  get binary() {
    switch (this.encoding) {
      case 8 /* BINARY */: {
        return this.data;
      }
      case 4 /* BASE64 */: {
        return simpleEncoder(this.data, "base64");
      }
      case 2 /* UTF8 */: {
        return simpleEncoder(this.data, "utf8");
      }
    }
  }
};

// src/core/ipc/IpcStreamEnd.cts
var IpcStreamEnd = class extends IpcMessage {
  constructor(stream_id) {
    super(4 /* STREAM_END */);
    this.stream_id = stream_id;
  }
};

// src/core/ipc/IpcStreamPull.cts
var IpcStreamPull = class extends IpcMessage {
  constructor(stream_id, desiredSize) {
    super(3 /* STREAM_PULL */);
    this.stream_id = stream_id;
    if (desiredSize == null) {
      desiredSize = 1;
    } else if (Number.isFinite(desiredSize) === false) {
      desiredSize = 1;
    } else if (desiredSize < 1) {
      desiredSize = 1;
    }
    this.desiredSize = desiredSize;
  }
};

// src/core/ipc/IpcBodySender.cts
var _IpcBodySender = class extends IpcBody {
  constructor(data, ipc) {
    super();
    this.data = data;
    this.ipc = ipc;
    this.isStream = this.data instanceof ReadableStream;
    this.pullSignal = createSignal();
    this.abortSignal = createSignal();
    /**
     * 被哪些 ipc 所真正使用，使用的进度分别是多少
     *
     * 这个进度 用于 类似流的 多发
     */
    this.usedIpcMap = /* @__PURE__ */ new Map();
    /**
     * 当前收到拉取的请求数
     */
    this.curPulledTimes = 0;
    this.closeSignal = createSignal();
    this.openSignal = createSignal();
    this._isStreamOpened = false;
    this._isStreamClosed = false;
    /// bodyAsMeta
    this._bodyHub = new BodyHub(this.data);
    this.metaBody = this.$bodyAsMeta(this.data, this.ipc);
    if (typeof data !== "string") {
      IpcBody.wm.set(data, this);
    }
    _IpcBodySender.$usableByIpc(ipc, this);
  }
  static from(data, ipc) {
    if (typeof data !== "string") {
      const cache = IpcBody.wm.get(data);
      if (cache !== void 0) {
        return cache;
      }
    }
    return new _IpcBodySender(data, ipc);
  }
  /**
   * 绑定使用
   */
  useByIpc(ipc) {
    if (this.usedIpcMap.has(ipc)) {
      return true;
    }
    if (this.isStream && !this._isStreamOpened) {
      this.usedIpcMap.set(ipc, 0);
      this.closeSignal.listen(() => {
        this.unuseByIpc(ipc);
      });
      return true;
    }
    return false;
  }
  /**
   * 拉取数据
   */
  emitStreamPull(message, ipc) {
    const pulledSize = this.usedIpcMap.get(ipc) + message.desiredSize;
    this.usedIpcMap.set(ipc, pulledSize);
    this.pullSignal.emit();
  }
  /**
   * 解绑使用
   */
  unuseByIpc(ipc) {
    if (this.usedIpcMap.delete(ipc) != null) {
      if (this.usedIpcMap.size === 0) {
        this.abortSignal.emit();
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
      return [3 /* TEXT */, body, ipc.uid];
    }
    if (body instanceof ReadableStream) {
      return this.$streamAsMeta(body, ipc);
    }
    return ipc.support_binary ? [9 /* BINARY */, binaryToU8a(body), ipc.uid] : [5 /* BASE64 */, simpleDecoder(body, "base64"), ipc.uid];
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
    const reader = binaryStreamRead(stream);
    const sender = async () => {
      if (this.curPulledTimes++ > 0) {
        return;
      }
      while (this.curPulledTimes > 0) {
        const availableLen = await reader.available();
        switch (availableLen) {
          case -1:
          case 0:
            {
              const message = new IpcStreamEnd(stream_id);
              for (const ipc2 of this.usedIpcMap.keys()) {
                ipc2.postMessage(message);
              }
              this.emitStreamClose();
            }
            break;
          default: {
            this.isStreamOpened = true;
            const data = await reader.readBinary(availableLen);
            let binary_message;
            let base64_message;
            for (const ipc2 of this.usedIpcMap.keys()) {
              const message = ipc2.support_binary ? binary_message ??= IpcStreamData.asBinary(stream_id, data) : base64_message ??= IpcStreamData.asBase64(stream_id, data);
              ipc2.postMessage(message);
            }
          }
        }
        this.curPulledTimes = 0;
      }
    };
    this.pullSignal.listen(() => {
      void sender();
    });
    this.abortSignal.listen(() => {
      reader.return();
      this.emitStreamClose();
    });
    return [0 /* STREAM_ID */, stream_id, ipc.uid];
  }
};
var IpcBodySender = _IpcBodySender;
/**
 * ipc 将会使用它
 */
IpcBodySender.$usableByIpc = (ipc, ipcBody) => {
  if (ipcBody.isStream && !ipcBody._isStreamOpened) {
    const streamId = ipcBody.metaBody[1];
    let usableIpcBodyMapper = IpcUsableIpcBodyMap.get(ipc);
    if (usableIpcBodyMapper === void 0) {
      const mapper = new UsableIpcBodyMapper();
      mapper.onDestroy(
        ipc.onMessage((message) => {
          if (message instanceof IpcStreamPull) {
            const ipcBody2 = mapper.get(message.stream_id);
            if (ipcBody2?.useByIpc(ipc)) {
              ipcBody2.emitStreamPull(message, ipc);
            }
          } else if (message instanceof IpcStreamAbort) {
            const ipcBody2 = mapper.get(message.stream_id);
            ipcBody2?.unuseByIpc(ipc);
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

// src/core/ipc/IpcResponse.cts
var _ipcHeaders;
var _IpcResponse = class extends IpcMessage {
  constructor(req_id, statusCode, headers, body, ipc) {
    super(1 /* RESPONSE */);
    this.req_id = req_id;
    this.statusCode = statusCode;
    this.headers = headers;
    this.body = body;
    this.ipc = ipc;
    __privateAdd(this, _ipcHeaders, void 0);
    this.ipcResMessage = (0, import_once.default)(
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
  get ipcHeaders() {
    return __privateGet(this, _ipcHeaders) ?? __privateSet(this, _ipcHeaders, new IpcHeaders(this.headers));
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
  static async fromResponse(req_id, response, ipc) {
    let ipcBody;
    if (response.body) {
      ipcBody = IpcBodySender.from(response.body, ipc);
    } else {
      ipcBody = IpcBodySender.from(
        binaryToU8a(await response.arrayBuffer()),
        ipc
      );
    }
    return new _IpcResponse(
      req_id,
      response.status,
      new IpcHeaders(response.headers),
      ipcBody,
      ipc
    );
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
    return new _IpcResponse(
      req_id,
      statusCode,
      headers,
      IpcBodySender.from(text, ipc),
      ipc
    );
  }
  static fromBinary(req_id, statusCode, headers, binary, ipc) {
    headers.init("Content-Type", "application/octet-stream");
    headers.init("Content-Length", binary.byteLength + "");
    return new _IpcResponse(
      req_id,
      statusCode,
      headers,
      IpcBodySender.from(binaryToU8a(binary), ipc),
      ipc
    );
  }
  static fromStream(req_id, statusCode, headers = new IpcHeaders(), stream, ipc) {
    headers.init("Content-Type", "application/octet-stream");
    const ipcResponse = new _IpcResponse(
      req_id,
      statusCode,
      headers,
      IpcBodySender.from(stream, ipc),
      ipc
    );
    return ipcResponse;
  }
  toJSON() {
    return this.ipcResMessage();
  }
};
var IpcResponse = _IpcResponse;
_ipcHeaders = new WeakMap();
var IpcResMessage = class extends IpcMessage {
  constructor(req_id, statusCode, headers, metaBody) {
    super(1 /* RESPONSE */);
    this.req_id = req_id;
    this.statusCode = statusCode;
    this.headers = headers;
    this.metaBody = metaBody;
  }
};

// src/sys/http-server/$createHttpDwebServer.cts
var import_once5 = __toESM(require_once());

// node_modules/@msgpack/msgpack/dist.es5+esm/utils/int.mjs
var UINT32_MAX = 4294967295;
function setUint64(view, offset, value) {
  var high = value / 4294967296;
  var low = value;
  view.setUint32(offset, high);
  view.setUint32(offset + 4, low);
}
function setInt64(view, offset, value) {
  var high = Math.floor(value / 4294967296);
  var low = value;
  view.setUint32(offset, high);
  view.setUint32(offset + 4, low);
}
function getInt64(view, offset) {
  var high = view.getInt32(offset);
  var low = view.getUint32(offset + 4);
  return high * 4294967296 + low;
}
function getUint64(view, offset) {
  var high = view.getUint32(offset);
  var low = view.getUint32(offset + 4);
  return high * 4294967296 + low;
}

// node_modules/@msgpack/msgpack/dist.es5+esm/utils/utf8.mjs
var _a;
var _b;
var _c;
var TEXT_ENCODING_AVAILABLE = (typeof process === "undefined" || ((_a = process === null || process === void 0 ? void 0 : process.env) === null || _a === void 0 ? void 0 : _a["TEXT_ENCODING"]) !== "never") && typeof TextEncoder !== "undefined" && typeof TextDecoder !== "undefined";
function utf8Count(str) {
  var strLength = str.length;
  var byteLength = 0;
  var pos = 0;
  while (pos < strLength) {
    var value = str.charCodeAt(pos++);
    if ((value & 4294967168) === 0) {
      byteLength++;
      continue;
    } else if ((value & 4294965248) === 0) {
      byteLength += 2;
    } else {
      if (value >= 55296 && value <= 56319) {
        if (pos < strLength) {
          var extra = str.charCodeAt(pos);
          if ((extra & 64512) === 56320) {
            ++pos;
            value = ((value & 1023) << 10) + (extra & 1023) + 65536;
          }
        }
      }
      if ((value & 4294901760) === 0) {
        byteLength += 3;
      } else {
        byteLength += 4;
      }
    }
  }
  return byteLength;
}
function utf8EncodeJs(str, output, outputOffset) {
  var strLength = str.length;
  var offset = outputOffset;
  var pos = 0;
  while (pos < strLength) {
    var value = str.charCodeAt(pos++);
    if ((value & 4294967168) === 0) {
      output[offset++] = value;
      continue;
    } else if ((value & 4294965248) === 0) {
      output[offset++] = value >> 6 & 31 | 192;
    } else {
      if (value >= 55296 && value <= 56319) {
        if (pos < strLength) {
          var extra = str.charCodeAt(pos);
          if ((extra & 64512) === 56320) {
            ++pos;
            value = ((value & 1023) << 10) + (extra & 1023) + 65536;
          }
        }
      }
      if ((value & 4294901760) === 0) {
        output[offset++] = value >> 12 & 15 | 224;
        output[offset++] = value >> 6 & 63 | 128;
      } else {
        output[offset++] = value >> 18 & 7 | 240;
        output[offset++] = value >> 12 & 63 | 128;
        output[offset++] = value >> 6 & 63 | 128;
      }
    }
    output[offset++] = value & 63 | 128;
  }
}
var sharedTextEncoder = TEXT_ENCODING_AVAILABLE ? new TextEncoder() : void 0;
var TEXT_ENCODER_THRESHOLD = !TEXT_ENCODING_AVAILABLE ? UINT32_MAX : typeof process !== "undefined" && ((_b = process === null || process === void 0 ? void 0 : process.env) === null || _b === void 0 ? void 0 : _b["TEXT_ENCODING"]) !== "force" ? 200 : 0;
function utf8EncodeTEencode(str, output, outputOffset) {
  output.set(sharedTextEncoder.encode(str), outputOffset);
}
function utf8EncodeTEencodeInto(str, output, outputOffset) {
  sharedTextEncoder.encodeInto(str, output.subarray(outputOffset));
}
var utf8EncodeTE = (sharedTextEncoder === null || sharedTextEncoder === void 0 ? void 0 : sharedTextEncoder.encodeInto) ? utf8EncodeTEencodeInto : utf8EncodeTEencode;
var CHUNK_SIZE = 4096;
function utf8DecodeJs(bytes, inputOffset, byteLength) {
  var offset = inputOffset;
  var end = offset + byteLength;
  var units = [];
  var result = "";
  while (offset < end) {
    var byte1 = bytes[offset++];
    if ((byte1 & 128) === 0) {
      units.push(byte1);
    } else if ((byte1 & 224) === 192) {
      var byte2 = bytes[offset++] & 63;
      units.push((byte1 & 31) << 6 | byte2);
    } else if ((byte1 & 240) === 224) {
      var byte2 = bytes[offset++] & 63;
      var byte3 = bytes[offset++] & 63;
      units.push((byte1 & 31) << 12 | byte2 << 6 | byte3);
    } else if ((byte1 & 248) === 240) {
      var byte2 = bytes[offset++] & 63;
      var byte3 = bytes[offset++] & 63;
      var byte4 = bytes[offset++] & 63;
      var unit = (byte1 & 7) << 18 | byte2 << 12 | byte3 << 6 | byte4;
      if (unit > 65535) {
        unit -= 65536;
        units.push(unit >>> 10 & 1023 | 55296);
        unit = 56320 | unit & 1023;
      }
      units.push(unit);
    } else {
      units.push(byte1);
    }
    if (units.length >= CHUNK_SIZE) {
      result += String.fromCharCode.apply(String, units);
      units.length = 0;
    }
  }
  if (units.length > 0) {
    result += String.fromCharCode.apply(String, units);
  }
  return result;
}
var sharedTextDecoder = TEXT_ENCODING_AVAILABLE ? new TextDecoder() : null;
var TEXT_DECODER_THRESHOLD = !TEXT_ENCODING_AVAILABLE ? UINT32_MAX : typeof process !== "undefined" && ((_c = process === null || process === void 0 ? void 0 : process.env) === null || _c === void 0 ? void 0 : _c["TEXT_DECODER"]) !== "force" ? 200 : 0;
function utf8DecodeTD(bytes, inputOffset, byteLength) {
  var stringBytes = bytes.subarray(inputOffset, inputOffset + byteLength);
  return sharedTextDecoder.decode(stringBytes);
}

// node_modules/@msgpack/msgpack/dist.es5+esm/ExtData.mjs
var ExtData = (
  /** @class */
  function() {
    function ExtData2(type, data) {
      this.type = type;
      this.data = data;
    }
    return ExtData2;
  }()
);

// node_modules/@msgpack/msgpack/dist.es5+esm/DecodeError.mjs
var __extends = function() {
  var extendStatics = function(d, b) {
    extendStatics = Object.setPrototypeOf || { __proto__: [] } instanceof Array && function(d2, b2) {
      d2.__proto__ = b2;
    } || function(d2, b2) {
      for (var p in b2)
        if (Object.prototype.hasOwnProperty.call(b2, p))
          d2[p] = b2[p];
    };
    return extendStatics(d, b);
  };
  return function(d, b) {
    if (typeof b !== "function" && b !== null)
      throw new TypeError("Class extends value " + String(b) + " is not a constructor or null");
    extendStatics(d, b);
    function __() {
      this.constructor = d;
    }
    d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
  };
}();
var DecodeError = (
  /** @class */
  function(_super) {
    __extends(DecodeError2, _super);
    function DecodeError2(message) {
      var _this = _super.call(this, message) || this;
      var proto = Object.create(DecodeError2.prototype);
      Object.setPrototypeOf(_this, proto);
      Object.defineProperty(_this, "name", {
        configurable: true,
        enumerable: false,
        value: DecodeError2.name
      });
      return _this;
    }
    return DecodeError2;
  }(Error)
);

// node_modules/@msgpack/msgpack/dist.es5+esm/timestamp.mjs
var EXT_TIMESTAMP = -1;
var TIMESTAMP32_MAX_SEC = 4294967296 - 1;
var TIMESTAMP64_MAX_SEC = 17179869184 - 1;
function encodeTimeSpecToTimestamp(_a3) {
  var sec = _a3.sec, nsec = _a3.nsec;
  if (sec >= 0 && nsec >= 0 && sec <= TIMESTAMP64_MAX_SEC) {
    if (nsec === 0 && sec <= TIMESTAMP32_MAX_SEC) {
      var rv = new Uint8Array(4);
      var view = new DataView(rv.buffer);
      view.setUint32(0, sec);
      return rv;
    } else {
      var secHigh = sec / 4294967296;
      var secLow = sec & 4294967295;
      var rv = new Uint8Array(8);
      var view = new DataView(rv.buffer);
      view.setUint32(0, nsec << 2 | secHigh & 3);
      view.setUint32(4, secLow);
      return rv;
    }
  } else {
    var rv = new Uint8Array(12);
    var view = new DataView(rv.buffer);
    view.setUint32(0, nsec);
    setInt64(view, 4, sec);
    return rv;
  }
}
function encodeDateToTimeSpec(date) {
  var msec = date.getTime();
  var sec = Math.floor(msec / 1e3);
  var nsec = (msec - sec * 1e3) * 1e6;
  var nsecInSec = Math.floor(nsec / 1e9);
  return {
    sec: sec + nsecInSec,
    nsec: nsec - nsecInSec * 1e9
  };
}
function encodeTimestampExtension(object) {
  if (object instanceof Date) {
    var timeSpec = encodeDateToTimeSpec(object);
    return encodeTimeSpecToTimestamp(timeSpec);
  } else {
    return null;
  }
}
function decodeTimestampToTimeSpec(data) {
  var view = new DataView(data.buffer, data.byteOffset, data.byteLength);
  switch (data.byteLength) {
    case 4: {
      var sec = view.getUint32(0);
      var nsec = 0;
      return { sec, nsec };
    }
    case 8: {
      var nsec30AndSecHigh2 = view.getUint32(0);
      var secLow32 = view.getUint32(4);
      var sec = (nsec30AndSecHigh2 & 3) * 4294967296 + secLow32;
      var nsec = nsec30AndSecHigh2 >>> 2;
      return { sec, nsec };
    }
    case 12: {
      var sec = getInt64(view, 4);
      var nsec = view.getUint32(0);
      return { sec, nsec };
    }
    default:
      throw new DecodeError("Unrecognized data size for timestamp (expected 4, 8, or 12): ".concat(data.length));
  }
}
function decodeTimestampExtension(data) {
  var timeSpec = decodeTimestampToTimeSpec(data);
  return new Date(timeSpec.sec * 1e3 + timeSpec.nsec / 1e6);
}
var timestampExtension = {
  type: EXT_TIMESTAMP,
  encode: encodeTimestampExtension,
  decode: decodeTimestampExtension
};

// node_modules/@msgpack/msgpack/dist.es5+esm/ExtensionCodec.mjs
var ExtensionCodec = (
  /** @class */
  function() {
    function ExtensionCodec2() {
      this.builtInEncoders = [];
      this.builtInDecoders = [];
      this.encoders = [];
      this.decoders = [];
      this.register(timestampExtension);
    }
    ExtensionCodec2.prototype.register = function(_a3) {
      var type = _a3.type, encode2 = _a3.encode, decode2 = _a3.decode;
      if (type >= 0) {
        this.encoders[type] = encode2;
        this.decoders[type] = decode2;
      } else {
        var index = 1 + type;
        this.builtInEncoders[index] = encode2;
        this.builtInDecoders[index] = decode2;
      }
    };
    ExtensionCodec2.prototype.tryToEncode = function(object, context) {
      for (var i = 0; i < this.builtInEncoders.length; i++) {
        var encodeExt = this.builtInEncoders[i];
        if (encodeExt != null) {
          var data = encodeExt(object, context);
          if (data != null) {
            var type = -1 - i;
            return new ExtData(type, data);
          }
        }
      }
      for (var i = 0; i < this.encoders.length; i++) {
        var encodeExt = this.encoders[i];
        if (encodeExt != null) {
          var data = encodeExt(object, context);
          if (data != null) {
            var type = i;
            return new ExtData(type, data);
          }
        }
      }
      if (object instanceof ExtData) {
        return object;
      }
      return null;
    };
    ExtensionCodec2.prototype.decode = function(data, type, context) {
      var decodeExt = type < 0 ? this.builtInDecoders[-1 - type] : this.decoders[type];
      if (decodeExt) {
        return decodeExt(data, type, context);
      } else {
        return new ExtData(type, data);
      }
    };
    ExtensionCodec2.defaultCodec = new ExtensionCodec2();
    return ExtensionCodec2;
  }()
);

// node_modules/@msgpack/msgpack/dist.es5+esm/utils/typedArrays.mjs
function ensureUint8Array(buffer) {
  if (buffer instanceof Uint8Array) {
    return buffer;
  } else if (ArrayBuffer.isView(buffer)) {
    return new Uint8Array(buffer.buffer, buffer.byteOffset, buffer.byteLength);
  } else if (buffer instanceof ArrayBuffer) {
    return new Uint8Array(buffer);
  } else {
    return Uint8Array.from(buffer);
  }
}
function createDataView(buffer) {
  if (buffer instanceof ArrayBuffer) {
    return new DataView(buffer);
  }
  var bufferView = ensureUint8Array(buffer);
  return new DataView(bufferView.buffer, bufferView.byteOffset, bufferView.byteLength);
}

// node_modules/@msgpack/msgpack/dist.es5+esm/Encoder.mjs
var DEFAULT_MAX_DEPTH = 100;
var DEFAULT_INITIAL_BUFFER_SIZE = 2048;
var Encoder = (
  /** @class */
  function() {
    function Encoder2(extensionCodec, context, maxDepth, initialBufferSize, sortKeys, forceFloat32, ignoreUndefined, forceIntegerToFloat) {
      if (extensionCodec === void 0) {
        extensionCodec = ExtensionCodec.defaultCodec;
      }
      if (context === void 0) {
        context = void 0;
      }
      if (maxDepth === void 0) {
        maxDepth = DEFAULT_MAX_DEPTH;
      }
      if (initialBufferSize === void 0) {
        initialBufferSize = DEFAULT_INITIAL_BUFFER_SIZE;
      }
      if (sortKeys === void 0) {
        sortKeys = false;
      }
      if (forceFloat32 === void 0) {
        forceFloat32 = false;
      }
      if (ignoreUndefined === void 0) {
        ignoreUndefined = false;
      }
      if (forceIntegerToFloat === void 0) {
        forceIntegerToFloat = false;
      }
      this.extensionCodec = extensionCodec;
      this.context = context;
      this.maxDepth = maxDepth;
      this.initialBufferSize = initialBufferSize;
      this.sortKeys = sortKeys;
      this.forceFloat32 = forceFloat32;
      this.ignoreUndefined = ignoreUndefined;
      this.forceIntegerToFloat = forceIntegerToFloat;
      this.pos = 0;
      this.view = new DataView(new ArrayBuffer(this.initialBufferSize));
      this.bytes = new Uint8Array(this.view.buffer);
    }
    Encoder2.prototype.reinitializeState = function() {
      this.pos = 0;
    };
    Encoder2.prototype.encodeSharedRef = function(object) {
      this.reinitializeState();
      this.doEncode(object, 1);
      return this.bytes.subarray(0, this.pos);
    };
    Encoder2.prototype.encode = function(object) {
      this.reinitializeState();
      this.doEncode(object, 1);
      return this.bytes.slice(0, this.pos);
    };
    Encoder2.prototype.doEncode = function(object, depth) {
      if (depth > this.maxDepth) {
        throw new Error("Too deep objects in depth ".concat(depth));
      }
      if (object == null) {
        this.encodeNil();
      } else if (typeof object === "boolean") {
        this.encodeBoolean(object);
      } else if (typeof object === "number") {
        this.encodeNumber(object);
      } else if (typeof object === "string") {
        this.encodeString(object);
      } else {
        this.encodeObject(object, depth);
      }
    };
    Encoder2.prototype.ensureBufferSizeToWrite = function(sizeToWrite) {
      var requiredSize = this.pos + sizeToWrite;
      if (this.view.byteLength < requiredSize) {
        this.resizeBuffer(requiredSize * 2);
      }
    };
    Encoder2.prototype.resizeBuffer = function(newSize) {
      var newBuffer = new ArrayBuffer(newSize);
      var newBytes = new Uint8Array(newBuffer);
      var newView = new DataView(newBuffer);
      newBytes.set(this.bytes);
      this.view = newView;
      this.bytes = newBytes;
    };
    Encoder2.prototype.encodeNil = function() {
      this.writeU8(192);
    };
    Encoder2.prototype.encodeBoolean = function(object) {
      if (object === false) {
        this.writeU8(194);
      } else {
        this.writeU8(195);
      }
    };
    Encoder2.prototype.encodeNumber = function(object) {
      if (Number.isSafeInteger(object) && !this.forceIntegerToFloat) {
        if (object >= 0) {
          if (object < 128) {
            this.writeU8(object);
          } else if (object < 256) {
            this.writeU8(204);
            this.writeU8(object);
          } else if (object < 65536) {
            this.writeU8(205);
            this.writeU16(object);
          } else if (object < 4294967296) {
            this.writeU8(206);
            this.writeU32(object);
          } else {
            this.writeU8(207);
            this.writeU64(object);
          }
        } else {
          if (object >= -32) {
            this.writeU8(224 | object + 32);
          } else if (object >= -128) {
            this.writeU8(208);
            this.writeI8(object);
          } else if (object >= -32768) {
            this.writeU8(209);
            this.writeI16(object);
          } else if (object >= -2147483648) {
            this.writeU8(210);
            this.writeI32(object);
          } else {
            this.writeU8(211);
            this.writeI64(object);
          }
        }
      } else {
        if (this.forceFloat32) {
          this.writeU8(202);
          this.writeF32(object);
        } else {
          this.writeU8(203);
          this.writeF64(object);
        }
      }
    };
    Encoder2.prototype.writeStringHeader = function(byteLength) {
      if (byteLength < 32) {
        this.writeU8(160 + byteLength);
      } else if (byteLength < 256) {
        this.writeU8(217);
        this.writeU8(byteLength);
      } else if (byteLength < 65536) {
        this.writeU8(218);
        this.writeU16(byteLength);
      } else if (byteLength < 4294967296) {
        this.writeU8(219);
        this.writeU32(byteLength);
      } else {
        throw new Error("Too long string: ".concat(byteLength, " bytes in UTF-8"));
      }
    };
    Encoder2.prototype.encodeString = function(object) {
      var maxHeaderSize = 1 + 4;
      var strLength = object.length;
      if (strLength > TEXT_ENCODER_THRESHOLD) {
        var byteLength = utf8Count(object);
        this.ensureBufferSizeToWrite(maxHeaderSize + byteLength);
        this.writeStringHeader(byteLength);
        utf8EncodeTE(object, this.bytes, this.pos);
        this.pos += byteLength;
      } else {
        var byteLength = utf8Count(object);
        this.ensureBufferSizeToWrite(maxHeaderSize + byteLength);
        this.writeStringHeader(byteLength);
        utf8EncodeJs(object, this.bytes, this.pos);
        this.pos += byteLength;
      }
    };
    Encoder2.prototype.encodeObject = function(object, depth) {
      var ext = this.extensionCodec.tryToEncode(object, this.context);
      if (ext != null) {
        this.encodeExtension(ext);
      } else if (Array.isArray(object)) {
        this.encodeArray(object, depth);
      } else if (ArrayBuffer.isView(object)) {
        this.encodeBinary(object);
      } else if (typeof object === "object") {
        this.encodeMap(object, depth);
      } else {
        throw new Error("Unrecognized object: ".concat(Object.prototype.toString.apply(object)));
      }
    };
    Encoder2.prototype.encodeBinary = function(object) {
      var size = object.byteLength;
      if (size < 256) {
        this.writeU8(196);
        this.writeU8(size);
      } else if (size < 65536) {
        this.writeU8(197);
        this.writeU16(size);
      } else if (size < 4294967296) {
        this.writeU8(198);
        this.writeU32(size);
      } else {
        throw new Error("Too large binary: ".concat(size));
      }
      var bytes = ensureUint8Array(object);
      this.writeU8a(bytes);
    };
    Encoder2.prototype.encodeArray = function(object, depth) {
      var size = object.length;
      if (size < 16) {
        this.writeU8(144 + size);
      } else if (size < 65536) {
        this.writeU8(220);
        this.writeU16(size);
      } else if (size < 4294967296) {
        this.writeU8(221);
        this.writeU32(size);
      } else {
        throw new Error("Too large array: ".concat(size));
      }
      for (var _i = 0, object_1 = object; _i < object_1.length; _i++) {
        var item = object_1[_i];
        this.doEncode(item, depth + 1);
      }
    };
    Encoder2.prototype.countWithoutUndefined = function(object, keys) {
      var count = 0;
      for (var _i = 0, keys_1 = keys; _i < keys_1.length; _i++) {
        var key = keys_1[_i];
        if (object[key] !== void 0) {
          count++;
        }
      }
      return count;
    };
    Encoder2.prototype.encodeMap = function(object, depth) {
      var keys = Object.keys(object);
      if (this.sortKeys) {
        keys.sort();
      }
      var size = this.ignoreUndefined ? this.countWithoutUndefined(object, keys) : keys.length;
      if (size < 16) {
        this.writeU8(128 + size);
      } else if (size < 65536) {
        this.writeU8(222);
        this.writeU16(size);
      } else if (size < 4294967296) {
        this.writeU8(223);
        this.writeU32(size);
      } else {
        throw new Error("Too large map object: ".concat(size));
      }
      for (var _i = 0, keys_2 = keys; _i < keys_2.length; _i++) {
        var key = keys_2[_i];
        var value = object[key];
        if (!(this.ignoreUndefined && value === void 0)) {
          this.encodeString(key);
          this.doEncode(value, depth + 1);
        }
      }
    };
    Encoder2.prototype.encodeExtension = function(ext) {
      var size = ext.data.length;
      if (size === 1) {
        this.writeU8(212);
      } else if (size === 2) {
        this.writeU8(213);
      } else if (size === 4) {
        this.writeU8(214);
      } else if (size === 8) {
        this.writeU8(215);
      } else if (size === 16) {
        this.writeU8(216);
      } else if (size < 256) {
        this.writeU8(199);
        this.writeU8(size);
      } else if (size < 65536) {
        this.writeU8(200);
        this.writeU16(size);
      } else if (size < 4294967296) {
        this.writeU8(201);
        this.writeU32(size);
      } else {
        throw new Error("Too large extension object: ".concat(size));
      }
      this.writeI8(ext.type);
      this.writeU8a(ext.data);
    };
    Encoder2.prototype.writeU8 = function(value) {
      this.ensureBufferSizeToWrite(1);
      this.view.setUint8(this.pos, value);
      this.pos++;
    };
    Encoder2.prototype.writeU8a = function(values) {
      var size = values.length;
      this.ensureBufferSizeToWrite(size);
      this.bytes.set(values, this.pos);
      this.pos += size;
    };
    Encoder2.prototype.writeI8 = function(value) {
      this.ensureBufferSizeToWrite(1);
      this.view.setInt8(this.pos, value);
      this.pos++;
    };
    Encoder2.prototype.writeU16 = function(value) {
      this.ensureBufferSizeToWrite(2);
      this.view.setUint16(this.pos, value);
      this.pos += 2;
    };
    Encoder2.prototype.writeI16 = function(value) {
      this.ensureBufferSizeToWrite(2);
      this.view.setInt16(this.pos, value);
      this.pos += 2;
    };
    Encoder2.prototype.writeU32 = function(value) {
      this.ensureBufferSizeToWrite(4);
      this.view.setUint32(this.pos, value);
      this.pos += 4;
    };
    Encoder2.prototype.writeI32 = function(value) {
      this.ensureBufferSizeToWrite(4);
      this.view.setInt32(this.pos, value);
      this.pos += 4;
    };
    Encoder2.prototype.writeF32 = function(value) {
      this.ensureBufferSizeToWrite(4);
      this.view.setFloat32(this.pos, value);
      this.pos += 4;
    };
    Encoder2.prototype.writeF64 = function(value) {
      this.ensureBufferSizeToWrite(8);
      this.view.setFloat64(this.pos, value);
      this.pos += 8;
    };
    Encoder2.prototype.writeU64 = function(value) {
      this.ensureBufferSizeToWrite(8);
      setUint64(this.view, this.pos, value);
      this.pos += 8;
    };
    Encoder2.prototype.writeI64 = function(value) {
      this.ensureBufferSizeToWrite(8);
      setInt64(this.view, this.pos, value);
      this.pos += 8;
    };
    return Encoder2;
  }()
);

// node_modules/@msgpack/msgpack/dist.es5+esm/encode.mjs
var defaultEncodeOptions = {};
function encode(value, options) {
  if (options === void 0) {
    options = defaultEncodeOptions;
  }
  var encoder = new Encoder(options.extensionCodec, options.context, options.maxDepth, options.initialBufferSize, options.sortKeys, options.forceFloat32, options.ignoreUndefined, options.forceIntegerToFloat);
  return encoder.encodeSharedRef(value);
}

// node_modules/@msgpack/msgpack/dist.es5+esm/utils/prettyByte.mjs
function prettyByte(byte) {
  return "".concat(byte < 0 ? "-" : "", "0x").concat(Math.abs(byte).toString(16).padStart(2, "0"));
}

// node_modules/@msgpack/msgpack/dist.es5+esm/CachedKeyDecoder.mjs
var DEFAULT_MAX_KEY_LENGTH = 16;
var DEFAULT_MAX_LENGTH_PER_KEY = 16;
var CachedKeyDecoder = (
  /** @class */
  function() {
    function CachedKeyDecoder2(maxKeyLength, maxLengthPerKey) {
      if (maxKeyLength === void 0) {
        maxKeyLength = DEFAULT_MAX_KEY_LENGTH;
      }
      if (maxLengthPerKey === void 0) {
        maxLengthPerKey = DEFAULT_MAX_LENGTH_PER_KEY;
      }
      this.maxKeyLength = maxKeyLength;
      this.maxLengthPerKey = maxLengthPerKey;
      this.hit = 0;
      this.miss = 0;
      this.caches = [];
      for (var i = 0; i < this.maxKeyLength; i++) {
        this.caches.push([]);
      }
    }
    CachedKeyDecoder2.prototype.canBeCached = function(byteLength) {
      return byteLength > 0 && byteLength <= this.maxKeyLength;
    };
    CachedKeyDecoder2.prototype.find = function(bytes, inputOffset, byteLength) {
      var records = this.caches[byteLength - 1];
      FIND_CHUNK:
        for (var _i = 0, records_1 = records; _i < records_1.length; _i++) {
          var record = records_1[_i];
          var recordBytes = record.bytes;
          for (var j = 0; j < byteLength; j++) {
            if (recordBytes[j] !== bytes[inputOffset + j]) {
              continue FIND_CHUNK;
            }
          }
          return record.str;
        }
      return null;
    };
    CachedKeyDecoder2.prototype.store = function(bytes, value) {
      var records = this.caches[bytes.length - 1];
      var record = { bytes, str: value };
      if (records.length >= this.maxLengthPerKey) {
        records[Math.random() * records.length | 0] = record;
      } else {
        records.push(record);
      }
    };
    CachedKeyDecoder2.prototype.decode = function(bytes, inputOffset, byteLength) {
      var cachedValue = this.find(bytes, inputOffset, byteLength);
      if (cachedValue != null) {
        this.hit++;
        return cachedValue;
      }
      this.miss++;
      var str = utf8DecodeJs(bytes, inputOffset, byteLength);
      var slicedCopyOfBytes = Uint8Array.prototype.slice.call(bytes, inputOffset, inputOffset + byteLength);
      this.store(slicedCopyOfBytes, str);
      return str;
    };
    return CachedKeyDecoder2;
  }()
);

// node_modules/@msgpack/msgpack/dist.es5+esm/Decoder.mjs
var __awaiter = function(thisArg, _arguments, P, generator) {
  function adopt(value) {
    return value instanceof P ? value : new P(function(resolve) {
      resolve(value);
    });
  }
  return new (P || (P = Promise))(function(resolve, reject) {
    function fulfilled(value) {
      try {
        step(generator.next(value));
      } catch (e) {
        reject(e);
      }
    }
    function rejected(value) {
      try {
        step(generator["throw"](value));
      } catch (e) {
        reject(e);
      }
    }
    function step(result) {
      result.done ? resolve(result.value) : adopt(result.value).then(fulfilled, rejected);
    }
    step((generator = generator.apply(thisArg, _arguments || [])).next());
  });
};
var __generator = function(thisArg, body) {
  var _ = { label: 0, sent: function() {
    if (t[0] & 1)
      throw t[1];
    return t[1];
  }, trys: [], ops: [] }, f, y, t, g;
  return g = { next: verb(0), "throw": verb(1), "return": verb(2) }, typeof Symbol === "function" && (g[Symbol.iterator] = function() {
    return this;
  }), g;
  function verb(n) {
    return function(v) {
      return step([n, v]);
    };
  }
  function step(op) {
    if (f)
      throw new TypeError("Generator is already executing.");
    while (_)
      try {
        if (f = 1, y && (t = op[0] & 2 ? y["return"] : op[0] ? y["throw"] || ((t = y["return"]) && t.call(y), 0) : y.next) && !(t = t.call(y, op[1])).done)
          return t;
        if (y = 0, t)
          op = [op[0] & 2, t.value];
        switch (op[0]) {
          case 0:
          case 1:
            t = op;
            break;
          case 4:
            _.label++;
            return { value: op[1], done: false };
          case 5:
            _.label++;
            y = op[1];
            op = [0];
            continue;
          case 7:
            op = _.ops.pop();
            _.trys.pop();
            continue;
          default:
            if (!(t = _.trys, t = t.length > 0 && t[t.length - 1]) && (op[0] === 6 || op[0] === 2)) {
              _ = 0;
              continue;
            }
            if (op[0] === 3 && (!t || op[1] > t[0] && op[1] < t[3])) {
              _.label = op[1];
              break;
            }
            if (op[0] === 6 && _.label < t[1]) {
              _.label = t[1];
              t = op;
              break;
            }
            if (t && _.label < t[2]) {
              _.label = t[2];
              _.ops.push(op);
              break;
            }
            if (t[2])
              _.ops.pop();
            _.trys.pop();
            continue;
        }
        op = body.call(thisArg, _);
      } catch (e) {
        op = [6, e];
        y = 0;
      } finally {
        f = t = 0;
      }
    if (op[0] & 5)
      throw op[1];
    return { value: op[0] ? op[1] : void 0, done: true };
  }
};
var __asyncValues = function(o) {
  if (!Symbol.asyncIterator)
    throw new TypeError("Symbol.asyncIterator is not defined.");
  var m = o[Symbol.asyncIterator], i;
  return m ? m.call(o) : (o = typeof __values === "function" ? __values(o) : o[Symbol.iterator](), i = {}, verb("next"), verb("throw"), verb("return"), i[Symbol.asyncIterator] = function() {
    return this;
  }, i);
  function verb(n) {
    i[n] = o[n] && function(v) {
      return new Promise(function(resolve, reject) {
        v = o[n](v), settle(resolve, reject, v.done, v.value);
      });
    };
  }
  function settle(resolve, reject, d, v) {
    Promise.resolve(v).then(function(v2) {
      resolve({ value: v2, done: d });
    }, reject);
  }
};
var __await = function(v) {
  return this instanceof __await ? (this.v = v, this) : new __await(v);
};
var __asyncGenerator = function(thisArg, _arguments, generator) {
  if (!Symbol.asyncIterator)
    throw new TypeError("Symbol.asyncIterator is not defined.");
  var g = generator.apply(thisArg, _arguments || []), i, q = [];
  return i = {}, verb("next"), verb("throw"), verb("return"), i[Symbol.asyncIterator] = function() {
    return this;
  }, i;
  function verb(n) {
    if (g[n])
      i[n] = function(v) {
        return new Promise(function(a, b) {
          q.push([n, v, a, b]) > 1 || resume(n, v);
        });
      };
  }
  function resume(n, v) {
    try {
      step(g[n](v));
    } catch (e) {
      settle(q[0][3], e);
    }
  }
  function step(r) {
    r.value instanceof __await ? Promise.resolve(r.value.v).then(fulfill, reject) : settle(q[0][2], r);
  }
  function fulfill(value) {
    resume("next", value);
  }
  function reject(value) {
    resume("throw", value);
  }
  function settle(f, v) {
    if (f(v), q.shift(), q.length)
      resume(q[0][0], q[0][1]);
  }
};
var isValidMapKeyType = function(key) {
  var keyType = typeof key;
  return keyType === "string" || keyType === "number";
};
var HEAD_BYTE_REQUIRED = -1;
var EMPTY_VIEW = new DataView(new ArrayBuffer(0));
var EMPTY_BYTES = new Uint8Array(EMPTY_VIEW.buffer);
var DataViewIndexOutOfBoundsError = function() {
  try {
    EMPTY_VIEW.getInt8(0);
  } catch (e) {
    return e.constructor;
  }
  throw new Error("never reached");
}();
var MORE_DATA = new DataViewIndexOutOfBoundsError("Insufficient data");
var sharedCachedKeyDecoder = new CachedKeyDecoder();
var Decoder = (
  /** @class */
  function() {
    function Decoder2(extensionCodec, context, maxStrLength, maxBinLength, maxArrayLength, maxMapLength, maxExtLength, keyDecoder) {
      if (extensionCodec === void 0) {
        extensionCodec = ExtensionCodec.defaultCodec;
      }
      if (context === void 0) {
        context = void 0;
      }
      if (maxStrLength === void 0) {
        maxStrLength = UINT32_MAX;
      }
      if (maxBinLength === void 0) {
        maxBinLength = UINT32_MAX;
      }
      if (maxArrayLength === void 0) {
        maxArrayLength = UINT32_MAX;
      }
      if (maxMapLength === void 0) {
        maxMapLength = UINT32_MAX;
      }
      if (maxExtLength === void 0) {
        maxExtLength = UINT32_MAX;
      }
      if (keyDecoder === void 0) {
        keyDecoder = sharedCachedKeyDecoder;
      }
      this.extensionCodec = extensionCodec;
      this.context = context;
      this.maxStrLength = maxStrLength;
      this.maxBinLength = maxBinLength;
      this.maxArrayLength = maxArrayLength;
      this.maxMapLength = maxMapLength;
      this.maxExtLength = maxExtLength;
      this.keyDecoder = keyDecoder;
      this.totalPos = 0;
      this.pos = 0;
      this.view = EMPTY_VIEW;
      this.bytes = EMPTY_BYTES;
      this.headByte = HEAD_BYTE_REQUIRED;
      this.stack = [];
    }
    Decoder2.prototype.reinitializeState = function() {
      this.totalPos = 0;
      this.headByte = HEAD_BYTE_REQUIRED;
      this.stack.length = 0;
    };
    Decoder2.prototype.setBuffer = function(buffer) {
      this.bytes = ensureUint8Array(buffer);
      this.view = createDataView(this.bytes);
      this.pos = 0;
    };
    Decoder2.prototype.appendBuffer = function(buffer) {
      if (this.headByte === HEAD_BYTE_REQUIRED && !this.hasRemaining(1)) {
        this.setBuffer(buffer);
      } else {
        var remainingData = this.bytes.subarray(this.pos);
        var newData = ensureUint8Array(buffer);
        var newBuffer = new Uint8Array(remainingData.length + newData.length);
        newBuffer.set(remainingData);
        newBuffer.set(newData, remainingData.length);
        this.setBuffer(newBuffer);
      }
    };
    Decoder2.prototype.hasRemaining = function(size) {
      return this.view.byteLength - this.pos >= size;
    };
    Decoder2.prototype.createExtraByteError = function(posToShow) {
      var _a3 = this, view = _a3.view, pos = _a3.pos;
      return new RangeError("Extra ".concat(view.byteLength - pos, " of ").concat(view.byteLength, " byte(s) found at buffer[").concat(posToShow, "]"));
    };
    Decoder2.prototype.decode = function(buffer) {
      this.reinitializeState();
      this.setBuffer(buffer);
      var object = this.doDecodeSync();
      if (this.hasRemaining(1)) {
        throw this.createExtraByteError(this.pos);
      }
      return object;
    };
    Decoder2.prototype.decodeMulti = function(buffer) {
      return __generator(this, function(_a3) {
        switch (_a3.label) {
          case 0:
            this.reinitializeState();
            this.setBuffer(buffer);
            _a3.label = 1;
          case 1:
            if (!this.hasRemaining(1))
              return [3, 3];
            return [4, this.doDecodeSync()];
          case 2:
            _a3.sent();
            return [3, 1];
          case 3:
            return [
              2
              /*return*/
            ];
        }
      });
    };
    Decoder2.prototype.decodeAsync = function(stream) {
      var stream_1, stream_1_1;
      var e_1, _a3;
      return __awaiter(this, void 0, void 0, function() {
        var decoded, object, buffer, e_1_1, _b2, headByte, pos, totalPos;
        return __generator(this, function(_c2) {
          switch (_c2.label) {
            case 0:
              decoded = false;
              _c2.label = 1;
            case 1:
              _c2.trys.push([1, 6, 7, 12]);
              stream_1 = __asyncValues(stream);
              _c2.label = 2;
            case 2:
              return [4, stream_1.next()];
            case 3:
              if (!(stream_1_1 = _c2.sent(), !stream_1_1.done))
                return [3, 5];
              buffer = stream_1_1.value;
              if (decoded) {
                throw this.createExtraByteError(this.totalPos);
              }
              this.appendBuffer(buffer);
              try {
                object = this.doDecodeSync();
                decoded = true;
              } catch (e) {
                if (!(e instanceof DataViewIndexOutOfBoundsError)) {
                  throw e;
                }
              }
              this.totalPos += this.pos;
              _c2.label = 4;
            case 4:
              return [3, 2];
            case 5:
              return [3, 12];
            case 6:
              e_1_1 = _c2.sent();
              e_1 = { error: e_1_1 };
              return [3, 12];
            case 7:
              _c2.trys.push([7, , 10, 11]);
              if (!(stream_1_1 && !stream_1_1.done && (_a3 = stream_1.return)))
                return [3, 9];
              return [4, _a3.call(stream_1)];
            case 8:
              _c2.sent();
              _c2.label = 9;
            case 9:
              return [3, 11];
            case 10:
              if (e_1)
                throw e_1.error;
              return [
                7
                /*endfinally*/
              ];
            case 11:
              return [
                7
                /*endfinally*/
              ];
            case 12:
              if (decoded) {
                if (this.hasRemaining(1)) {
                  throw this.createExtraByteError(this.totalPos);
                }
                return [2, object];
              }
              _b2 = this, headByte = _b2.headByte, pos = _b2.pos, totalPos = _b2.totalPos;
              throw new RangeError("Insufficient data in parsing ".concat(prettyByte(headByte), " at ").concat(totalPos, " (").concat(pos, " in the current buffer)"));
          }
        });
      });
    };
    Decoder2.prototype.decodeArrayStream = function(stream) {
      return this.decodeMultiAsync(stream, true);
    };
    Decoder2.prototype.decodeStream = function(stream) {
      return this.decodeMultiAsync(stream, false);
    };
    Decoder2.prototype.decodeMultiAsync = function(stream, isArray) {
      return __asyncGenerator(this, arguments, function decodeMultiAsync_1() {
        var isArrayHeaderRequired, arrayItemsLeft, stream_2, stream_2_1, buffer, e_2, e_3_1;
        var e_3, _a3;
        return __generator(this, function(_b2) {
          switch (_b2.label) {
            case 0:
              isArrayHeaderRequired = isArray;
              arrayItemsLeft = -1;
              _b2.label = 1;
            case 1:
              _b2.trys.push([1, 13, 14, 19]);
              stream_2 = __asyncValues(stream);
              _b2.label = 2;
            case 2:
              return [4, __await(stream_2.next())];
            case 3:
              if (!(stream_2_1 = _b2.sent(), !stream_2_1.done))
                return [3, 12];
              buffer = stream_2_1.value;
              if (isArray && arrayItemsLeft === 0) {
                throw this.createExtraByteError(this.totalPos);
              }
              this.appendBuffer(buffer);
              if (isArrayHeaderRequired) {
                arrayItemsLeft = this.readArraySize();
                isArrayHeaderRequired = false;
                this.complete();
              }
              _b2.label = 4;
            case 4:
              _b2.trys.push([4, 9, , 10]);
              _b2.label = 5;
            case 5:
              if (false)
                return [3, 8];
              return [4, __await(this.doDecodeSync())];
            case 6:
              return [4, _b2.sent()];
            case 7:
              _b2.sent();
              if (--arrayItemsLeft === 0) {
                return [3, 8];
              }
              return [3, 5];
            case 8:
              return [3, 10];
            case 9:
              e_2 = _b2.sent();
              if (!(e_2 instanceof DataViewIndexOutOfBoundsError)) {
                throw e_2;
              }
              return [3, 10];
            case 10:
              this.totalPos += this.pos;
              _b2.label = 11;
            case 11:
              return [3, 2];
            case 12:
              return [3, 19];
            case 13:
              e_3_1 = _b2.sent();
              e_3 = { error: e_3_1 };
              return [3, 19];
            case 14:
              _b2.trys.push([14, , 17, 18]);
              if (!(stream_2_1 && !stream_2_1.done && (_a3 = stream_2.return)))
                return [3, 16];
              return [4, __await(_a3.call(stream_2))];
            case 15:
              _b2.sent();
              _b2.label = 16;
            case 16:
              return [3, 18];
            case 17:
              if (e_3)
                throw e_3.error;
              return [
                7
                /*endfinally*/
              ];
            case 18:
              return [
                7
                /*endfinally*/
              ];
            case 19:
              return [
                2
                /*return*/
              ];
          }
        });
      });
    };
    Decoder2.prototype.doDecodeSync = function() {
      DECODE:
        while (true) {
          var headByte = this.readHeadByte();
          var object = void 0;
          if (headByte >= 224) {
            object = headByte - 256;
          } else if (headByte < 192) {
            if (headByte < 128) {
              object = headByte;
            } else if (headByte < 144) {
              var size = headByte - 128;
              if (size !== 0) {
                this.pushMapState(size);
                this.complete();
                continue DECODE;
              } else {
                object = {};
              }
            } else if (headByte < 160) {
              var size = headByte - 144;
              if (size !== 0) {
                this.pushArrayState(size);
                this.complete();
                continue DECODE;
              } else {
                object = [];
              }
            } else {
              var byteLength = headByte - 160;
              object = this.decodeUtf8String(byteLength, 0);
            }
          } else if (headByte === 192) {
            object = null;
          } else if (headByte === 194) {
            object = false;
          } else if (headByte === 195) {
            object = true;
          } else if (headByte === 202) {
            object = this.readF32();
          } else if (headByte === 203) {
            object = this.readF64();
          } else if (headByte === 204) {
            object = this.readU8();
          } else if (headByte === 205) {
            object = this.readU16();
          } else if (headByte === 206) {
            object = this.readU32();
          } else if (headByte === 207) {
            object = this.readU64();
          } else if (headByte === 208) {
            object = this.readI8();
          } else if (headByte === 209) {
            object = this.readI16();
          } else if (headByte === 210) {
            object = this.readI32();
          } else if (headByte === 211) {
            object = this.readI64();
          } else if (headByte === 217) {
            var byteLength = this.lookU8();
            object = this.decodeUtf8String(byteLength, 1);
          } else if (headByte === 218) {
            var byteLength = this.lookU16();
            object = this.decodeUtf8String(byteLength, 2);
          } else if (headByte === 219) {
            var byteLength = this.lookU32();
            object = this.decodeUtf8String(byteLength, 4);
          } else if (headByte === 220) {
            var size = this.readU16();
            if (size !== 0) {
              this.pushArrayState(size);
              this.complete();
              continue DECODE;
            } else {
              object = [];
            }
          } else if (headByte === 221) {
            var size = this.readU32();
            if (size !== 0) {
              this.pushArrayState(size);
              this.complete();
              continue DECODE;
            } else {
              object = [];
            }
          } else if (headByte === 222) {
            var size = this.readU16();
            if (size !== 0) {
              this.pushMapState(size);
              this.complete();
              continue DECODE;
            } else {
              object = {};
            }
          } else if (headByte === 223) {
            var size = this.readU32();
            if (size !== 0) {
              this.pushMapState(size);
              this.complete();
              continue DECODE;
            } else {
              object = {};
            }
          } else if (headByte === 196) {
            var size = this.lookU8();
            object = this.decodeBinary(size, 1);
          } else if (headByte === 197) {
            var size = this.lookU16();
            object = this.decodeBinary(size, 2);
          } else if (headByte === 198) {
            var size = this.lookU32();
            object = this.decodeBinary(size, 4);
          } else if (headByte === 212) {
            object = this.decodeExtension(1, 0);
          } else if (headByte === 213) {
            object = this.decodeExtension(2, 0);
          } else if (headByte === 214) {
            object = this.decodeExtension(4, 0);
          } else if (headByte === 215) {
            object = this.decodeExtension(8, 0);
          } else if (headByte === 216) {
            object = this.decodeExtension(16, 0);
          } else if (headByte === 199) {
            var size = this.lookU8();
            object = this.decodeExtension(size, 1);
          } else if (headByte === 200) {
            var size = this.lookU16();
            object = this.decodeExtension(size, 2);
          } else if (headByte === 201) {
            var size = this.lookU32();
            object = this.decodeExtension(size, 4);
          } else {
            throw new DecodeError("Unrecognized type byte: ".concat(prettyByte(headByte)));
          }
          this.complete();
          var stack = this.stack;
          while (stack.length > 0) {
            var state = stack[stack.length - 1];
            if (state.type === 0) {
              state.array[state.position] = object;
              state.position++;
              if (state.position === state.size) {
                stack.pop();
                object = state.array;
              } else {
                continue DECODE;
              }
            } else if (state.type === 1) {
              if (!isValidMapKeyType(object)) {
                throw new DecodeError("The type of key must be string or number but " + typeof object);
              }
              if (object === "__proto__") {
                throw new DecodeError("The key __proto__ is not allowed");
              }
              state.key = object;
              state.type = 2;
              continue DECODE;
            } else {
              state.map[state.key] = object;
              state.readCount++;
              if (state.readCount === state.size) {
                stack.pop();
                object = state.map;
              } else {
                state.key = null;
                state.type = 1;
                continue DECODE;
              }
            }
          }
          return object;
        }
    };
    Decoder2.prototype.readHeadByte = function() {
      if (this.headByte === HEAD_BYTE_REQUIRED) {
        this.headByte = this.readU8();
      }
      return this.headByte;
    };
    Decoder2.prototype.complete = function() {
      this.headByte = HEAD_BYTE_REQUIRED;
    };
    Decoder2.prototype.readArraySize = function() {
      var headByte = this.readHeadByte();
      switch (headByte) {
        case 220:
          return this.readU16();
        case 221:
          return this.readU32();
        default: {
          if (headByte < 160) {
            return headByte - 144;
          } else {
            throw new DecodeError("Unrecognized array type byte: ".concat(prettyByte(headByte)));
          }
        }
      }
    };
    Decoder2.prototype.pushMapState = function(size) {
      if (size > this.maxMapLength) {
        throw new DecodeError("Max length exceeded: map length (".concat(size, ") > maxMapLengthLength (").concat(this.maxMapLength, ")"));
      }
      this.stack.push({
        type: 1,
        size,
        key: null,
        readCount: 0,
        map: {}
      });
    };
    Decoder2.prototype.pushArrayState = function(size) {
      if (size > this.maxArrayLength) {
        throw new DecodeError("Max length exceeded: array length (".concat(size, ") > maxArrayLength (").concat(this.maxArrayLength, ")"));
      }
      this.stack.push({
        type: 0,
        size,
        array: new Array(size),
        position: 0
      });
    };
    Decoder2.prototype.decodeUtf8String = function(byteLength, headerOffset) {
      var _a3;
      if (byteLength > this.maxStrLength) {
        throw new DecodeError("Max length exceeded: UTF-8 byte length (".concat(byteLength, ") > maxStrLength (").concat(this.maxStrLength, ")"));
      }
      if (this.bytes.byteLength < this.pos + headerOffset + byteLength) {
        throw MORE_DATA;
      }
      var offset = this.pos + headerOffset;
      var object;
      if (this.stateIsMapKey() && ((_a3 = this.keyDecoder) === null || _a3 === void 0 ? void 0 : _a3.canBeCached(byteLength))) {
        object = this.keyDecoder.decode(this.bytes, offset, byteLength);
      } else if (byteLength > TEXT_DECODER_THRESHOLD) {
        object = utf8DecodeTD(this.bytes, offset, byteLength);
      } else {
        object = utf8DecodeJs(this.bytes, offset, byteLength);
      }
      this.pos += headerOffset + byteLength;
      return object;
    };
    Decoder2.prototype.stateIsMapKey = function() {
      if (this.stack.length > 0) {
        var state = this.stack[this.stack.length - 1];
        return state.type === 1;
      }
      return false;
    };
    Decoder2.prototype.decodeBinary = function(byteLength, headOffset) {
      if (byteLength > this.maxBinLength) {
        throw new DecodeError("Max length exceeded: bin length (".concat(byteLength, ") > maxBinLength (").concat(this.maxBinLength, ")"));
      }
      if (!this.hasRemaining(byteLength + headOffset)) {
        throw MORE_DATA;
      }
      var offset = this.pos + headOffset;
      var object = this.bytes.subarray(offset, offset + byteLength);
      this.pos += headOffset + byteLength;
      return object;
    };
    Decoder2.prototype.decodeExtension = function(size, headOffset) {
      if (size > this.maxExtLength) {
        throw new DecodeError("Max length exceeded: ext length (".concat(size, ") > maxExtLength (").concat(this.maxExtLength, ")"));
      }
      var extType = this.view.getInt8(this.pos + headOffset);
      var data = this.decodeBinary(
        size,
        headOffset + 1
        /* extType */
      );
      return this.extensionCodec.decode(data, extType, this.context);
    };
    Decoder2.prototype.lookU8 = function() {
      return this.view.getUint8(this.pos);
    };
    Decoder2.prototype.lookU16 = function() {
      return this.view.getUint16(this.pos);
    };
    Decoder2.prototype.lookU32 = function() {
      return this.view.getUint32(this.pos);
    };
    Decoder2.prototype.readU8 = function() {
      var value = this.view.getUint8(this.pos);
      this.pos++;
      return value;
    };
    Decoder2.prototype.readI8 = function() {
      var value = this.view.getInt8(this.pos);
      this.pos++;
      return value;
    };
    Decoder2.prototype.readU16 = function() {
      var value = this.view.getUint16(this.pos);
      this.pos += 2;
      return value;
    };
    Decoder2.prototype.readI16 = function() {
      var value = this.view.getInt16(this.pos);
      this.pos += 2;
      return value;
    };
    Decoder2.prototype.readU32 = function() {
      var value = this.view.getUint32(this.pos);
      this.pos += 4;
      return value;
    };
    Decoder2.prototype.readI32 = function() {
      var value = this.view.getInt32(this.pos);
      this.pos += 4;
      return value;
    };
    Decoder2.prototype.readU64 = function() {
      var value = getUint64(this.view, this.pos);
      this.pos += 8;
      return value;
    };
    Decoder2.prototype.readI64 = function() {
      var value = getInt64(this.view, this.pos);
      this.pos += 8;
      return value;
    };
    Decoder2.prototype.readF32 = function() {
      var value = this.view.getFloat32(this.pos);
      this.pos += 4;
      return value;
    };
    Decoder2.prototype.readF64 = function() {
      var value = this.view.getFloat64(this.pos);
      this.pos += 8;
      return value;
    };
    return Decoder2;
  }()
);

// node_modules/@msgpack/msgpack/dist.es5+esm/decode.mjs
var defaultDecodeOptions = {};
function decode(buffer, options) {
  if (options === void 0) {
    options = defaultDecodeOptions;
  }
  var decoder = new Decoder(options.extensionCodec, options.context, options.maxStrLength, options.maxBinLength, options.maxArrayLength, options.maxMapLength, options.maxExtLength);
  return decoder.decode(buffer);
}

// src/core/ipc-web/ReadableStreamIpc.cts
var import_once4 = __toESM(require_once());

// src/core/ipc/ipc.cts
var import_once3 = __toESM(require_once());

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

// src/core/ipc/IpcRequest.cts
var import_once2 = __toESM(require_once());

// src/helper/urlHelper.cts
var URL_BASE = "document" in globalThis ? document.baseURI : "location" in globalThis && (location.protocol === "http:" || location.protocol === "https:" || location.protocol === "file:" || location.protocol === "chrome-extension:") ? location.href : "file:///";
var parseUrl = (url, base = URL_BASE) => {
  return new URL(url, base);
};
var buildUrl = (url, ext) => {
  if (ext.pathname !== void 0) {
    url.pathname = ext.pathname;
  }
  if (ext.search) {
    if (ext.search instanceof URLSearchParams) {
      url.search = ext.search.toString();
    } else if (typeof ext.search === "string") {
      url.search = ext.search.toString();
    } else {
      url.search = new URLSearchParams(
        Object.entries(ext.search).map(([key, value]) => {
          return [
            key,
            typeof value === "string" ? value : JSON.stringify(value)
          ];
        })
      ).toString();
    }
  }
  return url;
};

// src/core/ipc/IpcRequest.cts
var _parsed_url;
var _IpcRequest = class extends IpcMessage {
  constructor(req_id, url, method, headers, body, ipc) {
    super(0 /* REQUEST */);
    this.req_id = req_id;
    this.url = url;
    this.method = method;
    this.headers = headers;
    this.body = body;
    this.ipc = ipc;
    __privateAdd(this, _parsed_url, void 0);
    this.ipcReqMessage = (0, import_once2.default)(
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
  get parsed_url() {
    return __privateGet(this, _parsed_url) ?? __privateSet(this, _parsed_url, parseUrl(this.url));
  }
  static fromText(req_id, url, method = "GET" /* GET */, headers = new IpcHeaders(), text, ipc) {
    return new _IpcRequest(
      req_id,
      url,
      method,
      headers,
      IpcBodySender.from(text, ipc),
      ipc
    );
  }
  static fromBinary(req_id, url, method = "GET" /* GET */, headers = new IpcHeaders(), binary, ipc) {
    headers.init("Content-Type", "application/octet-stream");
    headers.init("Content-Length", binary.byteLength + "");
    return new _IpcRequest(
      req_id,
      url,
      method,
      headers,
      IpcBodySender.from(binaryToU8a(binary), ipc),
      ipc
    );
  }
  static fromStream(req_id, url, method = "GET" /* GET */, headers = new IpcHeaders(), stream, ipc) {
    headers.init("Content-Type", "application/octet-stream");
    return new _IpcRequest(
      req_id,
      url,
      method,
      headers,
      IpcBodySender.from(stream, ipc),
      ipc
    );
  }
  static fromRequest(req_id, ipc, url, init = {}) {
    const method = toIpcMethod(init.method);
    const headers = init.headers instanceof IpcHeaders ? init.headers : new IpcHeaders(init.headers);
    let ipcBody;
    if (isBinary(init.body)) {
      ipcBody = IpcBodySender.from(init.body, ipc);
    } else if (init.body instanceof ReadableStream) {
      ipcBody = IpcBodySender.from(init.body, ipc);
    } else {
      ipcBody = IpcBodySender.from(init.body ?? "", ipc);
    }
    return new _IpcRequest(req_id, url, method, headers, ipcBody, ipc);
  }
  toRequest() {
    const { method } = this;
    let body;
    if ((method === "GET" /* GET */ || method === "HEAD" /* HEAD */) === false) {
      body = this.body.raw;
    }
    return new Request(this.url, {
      method,
      headers: this.headers,
      body
    });
  }
  toJSON() {
    return this.ipcReqMessage();
  }
};
var IpcRequest = _IpcRequest;
_parsed_url = new WeakMap();
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

// src/core/ipc/ipc.cts
var ipc_uid_acc = 0;
var Ipc = class {
  constructor() {
    this.uid = ipc_uid_acc++;
    this._support_message_pack = false;
    this._support_protobuf = false;
    this._support_raw = false;
    this._support_binary = false;
    this._messageSignal = createSignal();
    this.onMessage = this._messageSignal.listen;
    this._getOnRequestListener = (0, import_once3.default)(() => {
      const signal = createSignal();
      this.onMessage((request, ipc) => {
        if (request.type === 0 /* REQUEST */) {
          signal.emit(request, ipc);
        }
      });
      return signal.listen;
    });
    this._closed = false;
    this._closeSignal = createSignal();
    this.onClose = this._closeSignal.listen;
    this._reqresMap = /* @__PURE__ */ new Map();
    this._req_id_acc = 0;
    this._inited_req_res = false;
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
  postMessage(message) {
    if (this._closed) {
      return;
    }
    this._doPostMessage(message);
  }
  onRequest(cb) {
    return this._getOnRequestListener()(cb);
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
  allocReqId(url) {
    return this._req_id_acc++;
  }
  _initReqRes() {
    if (this._inited_req_res) {
      return;
    }
    this._inited_req_res = true;
    this.onMessage((message) => {
      if (message.type === 1 /* RESPONSE */) {
        const response_po = this._reqresMap.get(message.req_id);
        if (response_po) {
          this._reqresMap.delete(message.req_id);
          response_po.resolve(message);
        } else {
          throw new Error(`no found response by req_id: ${message.req_id}`);
        }
      }
    });
  }
  // 先找到错误的位置
  // 需要确定两个问题
  // 是否是应为报错导致无法响应后面的请求
  // 如果是是否可以避免报错？？
  /** 发起请求并等待响应 */
  // 会提供给 http-server模块的 gateway.listener.hookHttpRequest
  request(url, init) {
    const req_id = this.allocReqId();
    const ipcRequest = IpcRequest.fromRequest(req_id, this, url, init);
    this.postMessage(ipcRequest);
    return this.registerReqId(req_id).promise;
  }
  /** 自定义注册 请求与响应 的id */
  registerReqId(req_id = this.allocReqId()) {
    const response_po = new PromiseOut();
    this._reqresMap.set(req_id, response_po);
    this._initReqRes();
    return response_po;
  }
};

// src/core/ipc/IpcBodyReceiver.cts
var _IpcBodyReceiver = class extends IpcBody {
  constructor(metaBody, ipc) {
    super();
    this.metaBody = metaBody;
    switch (metaBody[0]) {
      case 0 /* STREAM_ID */:
        {
          const streamId = metaBody[1];
          const senderIpcUid = metaBody[2];
          const metaId = `${senderIpcUid}/${streamId}`;
          if (_IpcBodyReceiver.metaIdIpcMap.has(metaId) === false) {
            ipc.onClose(() => {
              _IpcBodyReceiver.metaIdIpcMap.delete(metaId);
            });
            _IpcBodyReceiver.metaIdIpcMap.set(metaId, ipc);
          }
          const receiver = _IpcBodyReceiver.metaIdIpcMap.get(metaId);
          if (receiver === void 0) {
            throw new Error(`no found ipc by metaId:${metaId}`);
          }
          ipc = receiver;
          this._bodyHub = new BodyHub($metaToStream(this.metaBody, ipc));
        }
        break;
      case 3 /* TEXT */:
        {
          this._bodyHub = new BodyHub(metaBody[1]);
        }
        break;
      default:
        {
          this._bodyHub = new BodyHub($metaBodyToBinary(metaBody));
        }
        break;
    }
  }
};
var IpcBodyReceiver = _IpcBodyReceiver;
IpcBodyReceiver.metaIdIpcMap = /* @__PURE__ */ new Map();
var $metaToStream = (rawBody, ipc) => {
  if (ipc == null) {
    throw new Error(`miss ipc when ipc-response has stream-body`);
  }
  const stream_ipc = ipc;
  const stream_id = rawBody[1];
  const stream = new ReadableStream({
    start(controller) {
      const off = ipc.onMessage((message) => {
        if ("stream_id" in message && message.stream_id === stream_id) {
          if (message.type === 2 /* STREAM_DATA */) {
            controller.enqueue(message.binary);
          } else if (message.type === 4 /* STREAM_END */) {
            controller.close();
            off();
          }
        }
      });
    },
    pull(controller) {
      stream_ipc.postMessage(
        new IpcStreamPull(stream_id, controller.desiredSize)
      );
    }
  });
  return stream;
};

// src/core/ipc-web/$messageToIpcMessage.cts
var isIpcSignalMessage = (msg) => msg === "close" || msg === "ping" || msg === "pong";
var $messageToIpcMessage = (data, ipc) => {
  if (isIpcSignalMessage(data)) {
    return data;
  }
  let message;
  if (data.type === 0 /* REQUEST */) {
    message = new IpcRequest(
      data.req_id,
      data.url,
      data.method,
      new IpcHeaders(data.headers),
      new IpcBodyReceiver(data.metaBody, ipc),
      ipc
    );
  } else if (data.type === 1 /* RESPONSE */) {
    message = new IpcResponse(
      data.req_id,
      data.statusCode,
      new IpcHeaders(data.headers),
      new IpcBodyReceiver(data.metaBody, ipc),
      ipc
    );
  } else if (data.type === 2 /* STREAM_DATA */) {
    message = new IpcStreamData(data.stream_id, data.data, data.encoding);
  } else if (data.type === 3 /* STREAM_PULL */) {
    message = new IpcStreamPull(data.stream_id, data.desiredSize);
  } else if (data.type === 4 /* STREAM_END */) {
    message = new IpcStreamEnd(data.stream_id);
  }
  return message;
};

// src/core/ipc-web/$jsonToIpcMessage.cts
var $jsonToIpcMessage = (data, ipc) => {
  return $messageToIpcMessage(
    isIpcSignalMessage(data) ? data : JSON.parse(data),
    ipc
  );
};

// src/core/ipc-web/$messagePackToIpcMessage.cts
var $messagePackToIpcMessage = (data, ipc) => {
  return $messageToIpcMessage(
    decode(data),
    ipc
  );
};

// src/core/ipc-web/ReadableStreamIpc.cts
var _rso;
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
    __privateAdd(this, _rso, new ReadableStreamOut());
    this.PONG_DATA = (0, import_once4.default)(() => {
      const pong = simpleEncoder("pong", "utf8");
      this._len[0] = pong.length;
      return u8aConcat([this._len_u8a, pong]);
    });
    this._len = new Uint32Array(1);
    this._len_u8a = new Uint8Array(this._len.buffer);
    this._support_message_pack = self_support_protocols.message_pack && remote.ipc_support_protocols.message_pack;
  }
  /** 这是输出流，给外部读取用的 */
  get stream() {
    return __privateGet(this, _rso).stream;
  }
  get controller() {
    return __privateGet(this, _rso).controller;
  }
  /**
   * 输入流要额外绑定
   * 注意，非必要不要 await 这个promise
   */
  async bindIncomeStream(stream) {
    if (this._incomne_stream !== void 0) {
      throw new Error("in come stream alreay binded.");
    }
    this._incomne_stream = await stream;
    const reader = binaryStreamRead(this._incomne_stream);
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
  }
  _doPostMessage(message) {
    var message_raw;
    if (message instanceof IpcRequest) {
      message_raw = message.ipcReqMessage();
    } else if (message instanceof IpcResponse) {
      message_raw = message.ipcResMessage();
    } else {
      message_raw = message;
    }
    const message_data = this.support_message_pack ? encode(message_raw) : simpleEncoder(JSON.stringify(message_raw), "utf8");
    this._len[0] = message_data.length;
    const chunk = u8aConcat([this._len_u8a, message_data]);
    this.controller.enqueue(chunk);
  }
  _doClose() {
    this.controller.close();
  }
};
_rso = new WeakMap();

// src/sys/http-server/const.ts
var ServerUrlInfo = class {
  constructor(host, internal_origin, public_origin) {
    this.host = host;
    this.internal_origin = internal_origin;
    this.public_origin = public_origin;
  }
  buildUrl(origin, builder) {
    if (typeof builder === "string") {
      return new URL(builder, origin);
    }
    const url = new URL(origin);
    url.searchParams.set("X-Dweb-Host", this.host);
    return builder(url) ?? url;
  }
  buildPublicUrl(builder) {
    return this.buildUrl(this.public_origin, builder);
  }
  buildInternalUrl(builder) {
    return this.buildUrl(this.internal_origin, builder);
  }
};
var ServerStartResult = class {
  constructor(token, urlInfo) {
    this.token = token;
    this.urlInfo = urlInfo;
  }
};

// src/sys/http-server/$createHttpDwebServer.cts
var createHttpDwebServer = async (microModule, options) => {
  const startResult = await startHttpDwebServer(microModule, options);
  console.log("\u83B7\u5F97\u57DF\u540D\u6388\u6743\uFF1A", startResult);
  return new HttpDwebServer(microModule, options, startResult);
};
var HttpDwebServer = class {
  constructor(nmm, options, startResult) {
    this.nmm = nmm;
    this.options = options;
    this.startResult = startResult;
    /** 关闭监听 */
    this.close = (0, import_once5.default)(() => closeHttpDwebServer(this.nmm, this.options));
  }
  /** 开始处理请求 */
  async listen(routes = [
    {
      pathname: "/",
      matchMode: "prefix",
      method: "GET"
    },
    {
      pathname: "/",
      matchMode: "prefix",
      method: "POST"
    },
    {
      pathname: "/",
      matchMode: "prefix",
      method: "PUT"
    },
    {
      pathname: "/",
      matchMode: "prefix",
      method: "DELETE"
    }
  ]) {
    return listenHttpDwebServer(this.nmm, this.startResult.token, routes);
  }
};
var listenHttpDwebServer = async (microModule, token, routes = [
  /** 定义了路由的方法 */
  { pathname: "/", matchMode: "prefix", method: "GET" },
  { pathname: "/", matchMode: "prefix", method: "POST" },
  { pathname: "/", matchMode: "prefix", method: "PUT" },
  { pathname: "/", matchMode: "prefix", method: "DELETE" },
  { pathname: "/", matchMode: "prefix", method: "PATCH" },
  { pathname: "/", matchMode: "prefix", method: "OPTIONS" },
  { pathname: "/", matchMode: "prefix", method: "HEAD" },
  { pathname: "/", matchMode: "prefix", method: "CONNECT" },
  { pathname: "/", matchMode: "prefix", method: "TRACE" }
]) => {
  const httpServerIpc = new ReadableStreamIpc(microModule, "client" /* CLIENT */);
  const url = new URL(`file://http.sys.dweb`);
  const ext = {
    pathname: "/listen",
    search: {
      token,
      routes
    }
  };
  const buildUrlValue = buildUrl(url, ext);
  const int = { method: "POST", body: httpServerIpc.stream };
  const httpIncomeRequestStream = await microModule.fetch(buildUrlValue, int).stream();
  console.log("\u5F00\u59CB\u54CD\u5E94\u670D\u52A1\u8BF7\u6C42");
  httpServerIpc.bindIncomeStream(httpIncomeRequestStream);
  return httpServerIpc;
};
var startHttpDwebServer = (microModule, options) => {
  return microModule.fetch(
    buildUrl(new URL(`file://http.sys.dweb/start`), {
      search: options
    })
  ).object().then((obj) => {
    console.log(obj);
    const { urlInfo, token } = obj;
    const serverUrlInfo = new ServerUrlInfo(
      urlInfo.host,
      urlInfo.internal_origin,
      urlInfo.public_origin
    );
    return new ServerStartResult(token, serverUrlInfo);
  });
};
var closeHttpDwebServer = async (microModule, options) => {
  return microModule.fetch(
    buildUrl(new URL(`file://http.sys.dweb/close`), {
      search: options
    })
  ).boolean();
};

// src/user/browser/assets/browser.web.cts
var txt = require_browser_render();
var CODE = async (require2) => {
  return txt;
};

// src/user/browser/assets/index.html.cts
var _a2;
var CODE2 = async (request) => String.raw(_a2 || (_a2 = __template(['\n  <!DOCTYPE html>\n  <html lang="en">\n    <head>\n      <meta charset="UTF-8" />\n      <meta http-equiv="X-UA-Compatible" content="IE=edge" />\n      <meta name="viewport" content="width=device-width, initial-scale=1.0" />\n      <title>Desktop</title>\n      <style>\n        :root {\n          background: rgba(255, 255, 255, 0.9);\n        }\n        li {\n          word-break: break-all;\n        }\n      </style>\n    </head>\n    <body>\n      <script type="text/javascript" src="./browser.web.mjs"><\/script>\n      <home-page></home-pagep>\n    </body>\n  </html>\n'])));

// src/user/browser/browser.worker.mts
var main = async () => {
  const dwebServer = await createHttpDwebServer(jsProcess, {});
  (await dwebServer.listen()).onRequest(
    async (request, httpServerIpc) => onRequest(request, httpServerIpc)
  );
  jsProcess.fetch(`file://statusbar.sys.dweb/`);
  await openIndexHtmlAtMWebview(
    dwebServer.startResult.urlInfo.buildInternalUrl((url) => {
      url.pathname = "/index.html";
    }).href
  );
};
main().catch(console.error);
async function onRequest(request, httpServerIpc) {
  console.log("\u63A5\u53D7\u5230\u4E86\u8BF7\u6C42\uFF1A request.parsed_url\uFF1A ", request.parsed_url);
  debugger;
  switch (request.parsed_url.pathname) {
    case "/":
    case "/index.html":
      onRequestPathNameIndexHtml(request, httpServerIpc);
      break;
    case "/browser.web.mjs":
      onRequestPathNameBroserWebMjs(request, httpServerIpc);
      break;
    case "/download":
      onRequestPathNameDownload(request, httpServerIpc);
      break;
    case "/appsinfo":
      onRequestPathNameAppsInfo(request, httpServerIpc);
      break;
    case `${request.parsed_url.pathname.startsWith("/icon") ? request.parsed_url.pathname : "**eot**"}`:
      onRequestPathNameIcon(request, httpServerIpc);
      break;
    case `/install`:
      onRequestPathNameInstall(request, httpServerIpc);
      break;
    case `/open`:
      onRequestPathNameOpen(request, httpServerIpc);
      break;
    case "/operation_from_plugins":
      onRequestPathOperation(request, httpServerIpc);
      break;
    default:
      onRequestPathNameNoMatch(request, httpServerIpc);
      break;
  }
}
async function onRequestPathNameIndexHtml(request, httpServerIpc) {
  const url = `file://plugins.sys.dweb/get`;
  const result = `<body><script type="text/javascript">${await jsProcess.fetch(url).text()}<\/script>`;
  let html = (await CODE2(request)).replace("<body>", result);
  httpServerIpc.postMessage(
    IpcResponse.fromText(
      request.req_id,
      200,
      new IpcHeaders({
        "Content-Type": "text/html"
      }),
      html,
      httpServerIpc
    )
  );
}
async function onRequestPathNameBroserWebMjs(request, httpServerIpc) {
  httpServerIpc.postMessage(
    IpcResponse.fromText(
      request.req_id,
      200,
      new IpcHeaders({
        "Content-Type": "application/javascript"
      }),
      await CODE(request),
      httpServerIpc
    )
  );
}
async function onRequestPathNameDownload(request, httpServerIpc) {
  const url = `file://file.sys.dweb${request.url}`;
  jsProcess.fetch(url).then(async (res) => {
    httpServerIpc.postMessage(
      await IpcResponse.fromResponse(request.req_id, res, httpServerIpc)
    );
  }).catch((err) => console.log("\u8BF7\u6C42\u5931\u8D25\uFF1A ", err));
}
async function onRequestPathNameAppsInfo(request, httpServerIpc) {
  const url = `file://file.sys.dweb/appsinfo`;
  jsProcess;
  fetch(url).then(async (res) => {
    httpServerIpc.postMessage(
      await IpcResponse.fromResponse(request.req_id, res, httpServerIpc)
    );
  }).catch((err) => {
    console.log("\u83B7\u53D6\u5168\u90E8\u7684 appsInfo \u5931\u8D25\uFF1A ", err);
  });
}
async function onRequestPathNameIcon(request, httpServerIpc) {
  console.log("\u83B7\u53D6icon");
  const path = request.parsed_url.pathname;
  const arr = path.split("/");
  console.log("arr:", arr);
  const id = arr[2];
  const iconname = arr[4];
  const url = `file://file.sys.dweb/icon?appId=${id}&name=${iconname}`;
  jsProcess;
  fetch(url).then(async (res) => {
    console.log("\u8F6C\u53D1\u56FE\u7247\u8D44\u6E90: ", res);
    httpServerIpc.postMessage(
      await IpcResponse.fromResponse(request.req_id, res, httpServerIpc)
    );
  }).catch((err) => {
    console.log("\u83B7\u53D6icon \u8D44\u6E90 \u5931\u8D25\uFF1A ", err);
  });
}
async function onRequestPathNameInstall(request, httpServerIpc) {
  const _url = `file://jmm.sys.dweb${request.url}`;
  jsProcess;
  fetch(_url).then(async (res) => {
    httpServerIpc.postMessage(
      await IpcResponse.fromResponse(request.req_id, res, httpServerIpc)
    );
  });
}
async function onRequestPathNameOpen(request, httpServerIpc) {
  const _url = `file://jmm.sys.dweb${request.url}`;
  jsProcess;
  fetch(_url).then(async (res) => {
    httpServerIpc.postMessage(
      await IpcResponse.fromResponse(request.req_id, res, httpServerIpc)
    );
  });
}
async function onRequestPathOperation(request, httpServerIpc) {
  const _path = request.headers.get("plugin-target");
  const _appUrl = request.parsed_url.searchParams.get("app_url");
  const _url = `file://api.sys.dweb/${_path}?app_url=${_appUrl}`;
  jsProcess;
  fetch(_url, {
    method: request.method,
    body: request.body.raw,
    headers: request.headers
  }).then(async (res) => {
    console.log("[browser.worker.mts onRequestPathOperation res:]", res);
    httpServerIpc.postMessage(
      await IpcResponse.fromResponse(request.req_id, res, httpServerIpc)
    );
  }).then(async (err) => {
    console.log("[browser.worker.mts onRequestPathOperation err:]", err);
  });
}
async function onRequestPathNameNoMatch(request, httpServerIpc) {
  httpServerIpc.postMessage(
    IpcResponse.fromText(
      request.req_id,
      404,
      void 0,
      "No Found",
      httpServerIpc
    )
  );
}
async function openIndexHtmlAtMWebview(origin) {
  console.log("--------broser.worker.mts, origin: ", origin);
  debugger;
  const view_id = await jsProcess.fetch(`file://mwebview.sys.dweb/open?url=${encodeURIComponent(origin)}`).text();
  return view_id;
}
export {
  main
};
