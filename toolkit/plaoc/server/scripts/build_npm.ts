import { npmBuilder } from "../../../../scripts/helper/npmBuilder.ts";

npmBuilder({
  packageDir: import.meta.resolve("../"),
  importMap: import.meta.resolve("./import_map.json"),
  entryPointsDirName: false,
  options: {
    scriptModule: false,
  },
});
