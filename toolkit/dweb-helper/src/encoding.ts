import { binaryToU8a, type $Binary } from "./fun/binaryHelper.ts";

export type $SimpleEncoding = "utf8" | "base64" | "hex";
const textEncoder = new TextEncoder();
export const simpleEncoder = (data: string, encoding: $SimpleEncoding) => {
  if (encoding === "base64") {
    const byteCharacters = atob(data);
    const binary = new Uint8Array(byteCharacters.length);
    for (let i = 0; i < byteCharacters.length; i++) {
      binary[i] = byteCharacters.charCodeAt(i);
    }
    return binary;
  } else if (encoding === "hex") {
    const binary = new Uint8Array(data.length / 2);
    for (let i = 0; i < binary.length; i++) {
      const start = i + i;
      binary[i] = parseInt(data.slice(start, start + 2), 16);
    }
    return binary;
  }
  return textEncoder.encode(data);
};
const textDecoder = new TextDecoder();
export const simpleDecoder = (data: $Binary, encoding: $SimpleEncoding) => {
  if (encoding === "base64") {
    let binary = "";
    const bytes = binaryToU8a(data);
    for (const byte of bytes) {
      binary += String.fromCharCode(byte);
    }
    return btoa(binary);
  } else if (encoding === "hex") {
    let hex = "";
    const bytes = binaryToU8a(data);
    for (const byte of bytes) {
      hex += byte.toString(16).padStart(2, "0");
    }
    return hex;
  }
  return textDecoder.decode(data);
};

export const utf8_to_b64 = (str: string) => {
  return btoa(unescape(encodeURIComponent(str)));
};

export const b64_to_utf8 = (str: string) => {
  return decodeURIComponent(escape(atob(str)));
};

export const dataUrlFromUtf8 = (utf8_string: string, asBase64: boolean, mime = "") => {
  const data_url = asBase64
    ? `data:${mime};base64,${utf8_to_b64(utf8_string)}`
    : `data:${mime};charset=UTF-8,${encodeURIComponent(utf8_string)}`;
  return data_url;
};
export function base64ToBytes(base64: string) {
  const binString = atob(base64);
  return new Uint8Array(Array.from(binString, (m) => m.codePointAt(0)!));
}

export function bytesToBase64(bytes: Uint8Array) {
  const binString = Array.from(bytes, (byte) => String.fromCodePoint(byte)).join("");
  return btoa(binString);
}
