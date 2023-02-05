import type { $Method } from "./types.cjs";

export interface $ReqMatcher {
  readonly pathname: string;
  readonly matchMode: "full" | "prefix";
  readonly method?: $Method;
}

export const $isMatchReq = (
  matcher: $ReqMatcher,
  pathname: string,
  method: string = "GET"
) => {
  return (
    (matcher.method ?? "GET") === method &&
    (matcher.matchMode === "full"
      ? pathname === matcher.pathname
      : matcher.matchMode === "prefix"
      ? pathname.startsWith(matcher.pathname)
      : false)
  );
};
