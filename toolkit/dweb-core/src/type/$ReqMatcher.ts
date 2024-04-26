import type { $Method } from "../ipc/helper/httpHelper.ts";

export interface $ReqMatcher {
  readonly pathname: string;
  readonly matchMode: "full" | "prefix";
  readonly method?: $Method;
  readonly protocol?: string;
}

export const $isMatchReq = (
  matcher: $ReqMatcher,
  pathname: string,
  method: (string & {}) | $Method = "GET",
  protocol?: string
) => {
  if (protocol !== undefined && matcher.protocol !== undefined && matcher.protocol !== protocol) {
    return false;
  }
  return (
    (matcher.method ?? "GET") === method &&
    (matcher.matchMode === "full"
      ? pathname === matcher.pathname
      : matcher.matchMode === "prefix"
      ? pathname.startsWith(matcher.pathname)
      : false)
  );
};
