import type {
  $Schema1,
  $Schema1ToType,
  $TypeName2,
} from "./types.ts";
import type { IpcClientRequest } from "../ipc/index.ts";
import { $typeNameParser } from "./$typeNameParser.ts";

export const $deserializeRequestToParams = <S extends $Schema1>(schema: S) => {
  type I = $Schema1ToType<S>;
  return (request: IpcClientRequest) => {
    const url = request.parsed_url;
    const params = {} as I;
    for (const [key, typeName2] of Object.entries(schema) as [
      keyof I & string,
      $TypeName2
    ][]) {
      params[key] = $typeNameParser(
        key,
        typeName2,
        url.searchParams.get(key)
      ) as never;
    }
    return params;
  };
};
