export type $MMID = `${string}.dweb`;

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

export interface $MicroModule {
  readonly mmid: $MMID;
  fetch(
    input: RequestInfo | URL,
    init?: RequestInit
  ): Promise<Response> &
    typeof import("./$makeFetchExtends.cjs")["fetchExtends"];
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
