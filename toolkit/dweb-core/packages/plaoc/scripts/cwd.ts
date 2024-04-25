import { pathToFileURL } from "node:url";
export const cwdRoot = pathToFileURL(Deno.cwd()).href + "/";
export const cwdResolve = (path: string) => new URL(path, cwdRoot).href;
