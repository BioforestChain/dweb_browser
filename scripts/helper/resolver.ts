import { $once } from "@dweb-browser/helper/decorator/$once.ts";
import fs from "node:fs";
import { createBaseResolveTo } from "./ConTasks.helper.ts";

const rootDir = import.meta.resolve("../../");
export const rootResolve = createBaseResolveTo(rootDir);
export const resolveDenoJson = $once(() => {
  return Function(`return(${fs.readFileSync(rootResolve("./deno.jsonc"))})`)() as {
    imports: Record<string, string>;
  };
});
