export type $MMID = `${string}.dweb`;
export type $DWEB_DEEPLINK = `dweb:${string}`;

export type $PromiseMaybe<T> = Promise<Awaited<T>> | T;
export type $Schema1ToType<S> = {
  [key in keyof S]: $TypeName2ToType<S[key]>;
};
export type $Schema2ToType<S> = S extends string
  ? $TypeName2ToType<S>
  : $Schema1ToType<S>;
export type $TypeName1ToType<T> = T extends "mmid"
  ? $MMID
  : T extends "string"
  ? string
  : T extends "number"
  ? number
  : T extends "boolean"
  ? boolean
  : T extends "object"
  ? object
  : T extends "void"
  ? void
  : undefined /* 包括 void */;
export type $TypeName2ToType<T> = T extends `${infer typeName}${"?"}`
  ? $TypeName1ToType<typeName> | undefined
  : $TypeName1ToType<T>;
export type $TypeName1 =
  | "mmid"
  | "string"
  | "number"
  | "boolean"
  | "object"
  | "void";
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
  /** 模块id */
  readonly mmid: $MMID;
  /** 对通讯协议的支持情况 */
  readonly ipc_support_protocols: $IpcSupportProtocols;
  /**
   * 匹配的“DWEB深层链接”
   * 取代明确的 mmid，dweb-deeplinks 可以用来表征一种特性、一种共识，它必须是 'dweb:{domain}[/pathname[/pathname...]]' 的格式规范
   * 为了交付给用户清晰的可管理的模式，这里的 deeplink 仅仅允许精确的前缀匹配，因此我们通常会规范它的动作层级依次精确
   *
   * 比如说：'dweb:mailto'，那么在面对 'dweb:mailto?address=someone@mail.com&title=xxx' 的链接时，该链接会被打包成一个 IpcRequest 消息传输过来
   * 比如说：'dweb:open/file/image'，那么就会匹配这样的链接 'dweb:open/file/image/svg?uri=file:///example.svg'
   *
   * dweb_deeplinks 由 dns 模块进行统一管理，也由它提供相关的管理界面、控制策略
   */
  readonly dweb_deeplinks: $DWEB_DEEPLINK[];
}
export interface $MicroModule extends $IpcMicroModuleInfo {
  nativeFetch(
    input: RequestInfo | URL,
    init?: RequestInit
  ): Promise<Response> &
    typeof import("../../helper/fetchExtends/index.ts")["fetchExtends"];
}

export type $Method =
  | "GET" // 查
  | "POST" // 增
  | "PUT" // 改：替换
  | "PATCH" // 改：局部更新
  | "DELETE" // 删
  | "OPTIONS" //  嗅探
  | "HEAD" // 预查
  | "CONNECT" // 双工
  | "TRACE"; // 调试
