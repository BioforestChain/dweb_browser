import { npmBuilder } from "../../dweb-core/scripts/npmBuilder.ts";

npmBuilder({
  rootUrl: import.meta.resolve("../"),
  version: Deno.args[0],
  importMap: import.meta.resolve("./import_map.json"),
  entryPointsDirName: "./worker",
});
