import type { $Method } from "./types.cjs";

export const httpMethodCanOwnBody = (method: $Method | string) => {
  return (
    method !== "GET" &&
    method !== "HEAD" &&
    method !== "TRACE" &&
    method !== "OPTIONS"
  );
};
