import { toFileUrl } from "https://deno.land/std@0.156.0/path/mod.ts";
export const cwdRoot = toFileUrl(Deno.cwd()).href + "/";
export const cwdResolve = (path: string) => new URL(path, cwdRoot).href;
