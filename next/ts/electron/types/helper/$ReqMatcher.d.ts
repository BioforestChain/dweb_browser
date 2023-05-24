import type { $Method } from "./types.js";
export interface $ReqMatcher {
    readonly pathname: string;
    readonly matchMode: "full" | "prefix";
    readonly method?: $Method;
}
export declare const $isMatchReq: (matcher: $ReqMatcher, pathname: string, method?: string) => boolean;
