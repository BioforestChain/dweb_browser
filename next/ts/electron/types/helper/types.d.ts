export type $MMID = `${string}.dweb`;
export type $PromiseMaybe<T> = Promise<Awaited<T>> | T;
export type $Schema1ToType<S> = {
    [key in keyof S]: $TypeName2ToType<S[key]>;
};
export type $Schema2ToType<S> = S extends string ? $TypeName2ToType<S> : $Schema1ToType<S>;
export type $TypeName1ToType<T> = T extends "mmid" ? $MMID : T extends "string" ? string : T extends "number" ? number : T extends "boolean" ? boolean : T extends "object" ? object : T extends "void" ? void : undefined;
export type $TypeName2ToType<T> = T extends `${infer typeName}${"?"}` ? $TypeName1ToType<typeName> | undefined : $TypeName1ToType<T>;
export type $TypeName1 = "mmid" | "string" | "number" | "boolean" | "object" | "void";
export type $TypeName2 = `${$TypeName1}${"?" | ""}`;
export type $Schema1 = Record<string, $TypeName2>;
export type $Schema2 = Record<string, $TypeName2> | $TypeName2;
/**
 * 通讯支持的传输协议
 */
export interface $IpcSupportProtocols {
    message_pack: boolean;
    protobuf: boolean;
    raw: boolean;
}
export interface $IpcMicroModuleInfo {
    readonly ipc_support_protocols: $IpcSupportProtocols;
    readonly mmid: $MMID;
}
export interface $MicroModule extends $IpcMicroModuleInfo {
    nativeFetch(input: RequestInfo | URL, init?: RequestInit): Promise<Response> & typeof import("./$makeFetchExtends.js")["fetchExtends"];
}
export type $Method = "GET" | "POST" | "PUT" | "PATCH" | "DELETE" | "OPTIONS" | "HEAD" | "CONNECT" | "TRACE";
