import { debounce } from "@dweb-browser/helper/decorator/$debounce.ts";
import { $once } from "@dweb-browser/helper/decorator/$once.ts";
import { fileURLToPath } from "node:url";
import { registryViteBuilder } from "../../scripts/helper/npmBuilder.ts";
import { doBundleServer } from "../plaoc/scripts/bundle-server.ts";
import { plaocCli, plaocIsDweb, plaocPlugins, plaocServer } from "./build_npm.ts";

const plaocExamples = registryViteBuilder({
  inDir: "toolkit/plaoc/examples/plugin-demo",
  outDir: "npm/@plaoc__examples/plugin-demo",
  baseDir: "./",
});

const plaocTasks = [
  //
  plaocServer,
  plaocCli,
  plaocPlugins,
  plaocIsDweb,
  plaocExamples,
  Object.assign($once(doBundleServer), {
    reset() {
      // doBundleServer 自带 watch
    },
  }),
];
export const doPlaocTasks = async () => {
  await Promise.all(plaocTasks.map((task) => task()));
};

if (import.meta.main) {
  if (Deno.args.includes("--watch")) {
    const watchPlaocTasks = debounce(() => {
      plaocTasks.forEach((task) => task.reset());
      doPlaocTasks();
    }, 200);
    watchPlaocTasks();
    for await (const event of Deno.watchFs(fileURLToPath(import.meta.resolve("../")), { recursive: true })) {
      if (
        false ===
        event.paths.every(
          (path) =>
            path.includes("/node_modules") ||
            //
            path.includes("/scripts") ||
            //
            path.includes("/dist")
        )
      ) {
        console.log("file", event.kind, ...event.paths);
        watchPlaocTasks();
      }
    }
  } else {
    doPlaocTasks();
  }
}
