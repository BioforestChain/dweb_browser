export * from "ts-pattern";
import { match } from "ts-pattern";
import type { string as P_string } from "ts-pattern/dist/patterns.d.ts";
import type { Pattern } from "ts-pattern/dist/types/Pattern.d.ts";

import { PURE_METHOD } from "../core/ipc/helper/const.ts";
import { $OnFetch, FetchEvent } from "../core/ipc/index.ts";
export type $PatternPathname =
  | string
  | typeof P_string
  | ReturnType<(typeof P_string)["startsWith" | "endsWith" | "maxLength" | "minLength" | "regex"]>;

export const fetchMatch = () => {
  const withList: [p: Pattern<FetchEvent>, cb: $OnFetch][] = [];
  return {
    get(pathname: $PatternPathname, callback: $OnFetch) {
      withList.push([{ method: PURE_METHOD.GET, pathname }, callback]);
      return this;
    },
    post(pathname: $PatternPathname, callback: $OnFetch) {
      withList.push([{ method: PURE_METHOD.POST, pathname }, callback]);
      return this;
    },
    put(pathname: $PatternPathname, callback: $OnFetch) {
      withList.push([{ method: PURE_METHOD.PUT, pathname }, callback]);
      return this;
    },
    delete(pathname: $PatternPathname, callback: $OnFetch) {
      withList.push([{ method: PURE_METHOD.DELETE, pathname }, callback]);
      return this;
    },
    deeplink(pathname: $PatternPathname, callback: $OnFetch) {
      withList.push([{ url: { protocol: "dweb:" }, pathname: pathname }, callback]);
      return this;
    },
    duplex(pathname: $PatternPathname, callback: $OnFetch) {
      withList.push([{ method: PURE_METHOD.GET, ipcRequest: { hasDuplex: true }, pathname }, callback]);
      return this;
    },
    run(value: FetchEvent) {
      let fm: any = match(value);
      for (const withItem of withList) {
        fm = fm.with(withItem[0], withItem[1]);
      }

      return fm.run() as ReturnType<$OnFetch>;
    },
  };
};
