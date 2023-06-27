import type { $Method } from "./types.ts";

export const httpMethodCanOwnBody = (method: $Method | (string & {})) => {
  return (
    method !== "GET" &&
    method !== "HEAD" &&
    method !== "TRACE" &&
    method !== "OPTIONS"
  );
};
