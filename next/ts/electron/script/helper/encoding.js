"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.dataUrlFromUtf8 = exports.b64_to_utf8 = exports.utf8_to_b64 = exports.simpleDecoder = exports.simpleEncoder = void 0;
const binaryHelper_js_1 = require("./binaryHelper.js");
const textEncoder = new TextEncoder();
const simpleEncoder = (data, encoding) => {
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
exports.simpleEncoder = simpleEncoder;
const textDecoder = new TextDecoder();
const simpleDecoder = (data, encoding) => {
    if (encoding === "base64") {
        let binary = "";
        const bytes = (0, binaryHelper_js_1.binaryToU8a)(data);
        for (const byte of bytes) {
            binary += String.fromCharCode(byte);
        }
        return btoa(binary);
    }
    return textDecoder.decode(data);
};
exports.simpleDecoder = simpleDecoder;
const utf8_to_b64 = (str) => {
    return btoa(unescape(encodeURIComponent(str)));
};
exports.utf8_to_b64 = utf8_to_b64;
const b64_to_utf8 = (str) => {
    return decodeURIComponent(escape(atob(str)));
};
exports.b64_to_utf8 = b64_to_utf8;
const dataUrlFromUtf8 = (utf8_string, asBase64, mime = "") => {
    const data_url = asBase64
        ? `data:${mime};base64,${(0, exports.utf8_to_b64)(utf8_string)}`
        : `data:${mime};charset=UTF-8,${encodeURIComponent(utf8_string)}`;
    return data_url;
};
exports.dataUrlFromUtf8 = dataUrlFromUtf8;
