import { binaryToU8a } from "./binaryHelper.js";
const textEncoder = new TextEncoder();
export const simpleEncoder = (data, encoding) => {
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
const textDecoder = new TextDecoder();
export const simpleDecoder = (data, encoding) => {
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
export const utf8_to_b64 = (str) => {
    return btoa(unescape(encodeURIComponent(str)));
};
export const b64_to_utf8 = (str) => {
    return decodeURIComponent(escape(atob(str)));
};
export const dataUrlFromUtf8 = (utf8_string, asBase64, mime = "") => {
    const data_url = asBase64
        ? `data:${mime};base64,${utf8_to_b64(utf8_string)}`
        : `data:${mime};charset=UTF-8,${encodeURIComponent(utf8_string)}`;
    return data_url;
};
