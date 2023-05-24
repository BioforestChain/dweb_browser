import type { $TypeName2ToType } from "./types.js";
export declare const $typeNameParser: <T extends "string" | "number" | "boolean" | "object" | "mmid" | "void" | "string?" | "number?" | "boolean?" | "object?" | "mmid?" | "void?">(key: string, typeName2: T, value: string | null) => $TypeName2ToType<T>;
