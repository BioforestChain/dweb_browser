import { npmBuilder } from "../../../dweb-core/scripts/npmBuilder.ts";

npmBuilder({
  rootUrl: import.meta.resolve("../"),
  importMap: import.meta.resolve("./import_map.json"),
  entryPointsDirName: false,
  options: {
    scriptModule: false,
  },
});
