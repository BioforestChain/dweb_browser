import type { $Method } from "./types.js";

export const httpMethodCanOwnBody = (method: $Method | string) => {
  return (
    method !== "GET" &&
    method !== "HEAD" &&
    method !== "TRACE" &&
    method !== "OPTIONS"
  );
};
