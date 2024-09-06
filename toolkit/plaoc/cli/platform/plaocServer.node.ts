import { createRequire } from "node:module";
import { dirname } from "node:path";

export const getPlaocServerDir = (isLive: boolean) => {
  const internalRequest = createRequire(import.meta.url);
  return dirname(
    internalRequest.resolve(isLive ? "@plaoc/server/plaoc.server.dev.js" : "@plaoc/server/plaoc.server.js")
  );
};
