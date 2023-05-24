import { $Binary } from "./binaryHelper.js";
export type $SimpleEncoding = "utf8" | "base64";
export declare const simpleEncoder: (data: string, encoding: $SimpleEncoding) => Uint8Array;
export declare const simpleDecoder: (data: $Binary, encoding: $SimpleEncoding) => string;
export declare const utf8_to_b64: (str: string) => string;
export declare const b64_to_utf8: (str: string) => string;
export declare const dataUrlFromUtf8: (utf8_string: string, asBase64: boolean, mime?: string) => string;
