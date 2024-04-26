// ex. scripts/build_npm.ts
import { npmBuilder } from "./npmBuilder.ts";

// const mappings = Object.fromEntries(
//   Object.keys((await import("../../dweb-helper/npm/package.json", { with: { type: "json" } })).default.exports)
//     .map((specifier) => {
//       if (specifier.endsWith(".ts")) {
//         return [
//           `https://esm.sh/@dweb-browser/helper/${specifier.slice(2)}`,
//           { name: `@dweb-browser/helper`, subPath: `${specifier.slice(2, -2)}js` },
//         ] as const;
//       }
//     })
//     .filter(Boolean) as any
// );
// console.log(mappings);
npmBuilder({
  rootUrl: import.meta.resolve("../"),
  version: Deno.args[0],
  importMap: import.meta.resolve("./import_map.json"),
  options: {
    // mappings,
  },
});
