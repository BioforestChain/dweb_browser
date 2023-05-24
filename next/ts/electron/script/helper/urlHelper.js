"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || function (mod) {
    if (mod && mod.__esModule) return mod;
    var result = {};
    if (mod != null) for (var k in mod) if (k !== "default" && Object.prototype.hasOwnProperty.call(mod, k)) __createBinding(result, mod, k);
    __setModuleDefault(result, mod);
    return result;
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.buildUrl = exports.updateUrlOrigin = exports.parseUrl = void 0;
const dntShim = __importStar(require("../_dnt.shims.js"));
const URL_BASE = "document" in dntShim.dntGlobalThis
    ? document.baseURI
    : "location" in dntShim.dntGlobalThis &&
        (location.protocol === "http:" ||
            location.protocol === "https:" ||
            location.protocol === "file:" ||
            location.protocol === "chrome-extension:")
        ? location.href
        : "file:///";
const parseUrl = (url, base = URL_BASE) => {
    return new URL(url, base);
};
exports.parseUrl = parseUrl;
const updateUrlOrigin = (url, new_origin) => {
    const { origin, href } = (0, exports.parseUrl)(url);
    return new URL(new_origin + href.slice(origin.length));
};
exports.updateUrlOrigin = updateUrlOrigin;
const buildUrl = (url, ext) => {
    if (ext.pathname !== undefined) {
        url.pathname = ext.pathname;
    }
    if (ext.search) {
        if (ext.search instanceof URLSearchParams) {
            url.search = ext.search.toString();
        }
        else if (typeof ext.search === "string") {
            url.search = ext.search.toString();
        }
        else {
            url.search = new URLSearchParams(Object.entries(ext.search).map(([key, value]) => {
                return [
                    key,
                    typeof value === "string" ? value : JSON.stringify(value),
                ];
            })).toString();
        }
    }
    return url;
};
exports.buildUrl = buildUrl;
