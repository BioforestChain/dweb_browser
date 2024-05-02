import { debounce } from "@dweb-browser/helper/decorator/$debounce.ts";
import { $once } from "@dweb-browser/helper/decorator/$once.ts";
import { registryViteBuilder } from "../../scripts/helper/npmBuilder.ts";
import { rootResolve } from "../../scripts/helper/resolver.ts";
import { watchFs } from "../../scripts/helper/watchFs.ts";
import { doBundleServer } from "../plaoc/scripts/bundle-server.ts";
import { plaocCli, plaocIsDweb, plaocPlugins, plaocServer } from "./build_npm.ts";
import { toolkitInit } from "./toolkit-init.ts";

const plaocExamples = registryViteBuilder({
  name: "plaoc:examples:plugin-demo",
  inDir: "./toolkit/plaoc/examples/plugin-demo",
  outDir: "npm/@plaoc__examples/plugin-demo",
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
  await toolkitInit();
  if (Deno.args.includes("--watch")) {
    const watchPlaocTasks = debounce(() => {
      plaocTasks.forEach((task) => task.reset());
      doPlaocTasks();
    }, 300);
    watchPlaocTasks();
    for await (const _event of watchFs(rootResolve("./toolkit"), {
      recursive: true,
      exclude: (path) =>
        path.includes("/node_modules") ||
        //
        path.includes("/scripts") ||
        //
        path.includes("/dist"),
    })) {
      // console.log(_event);
      watchPlaocTasks();
    }
  } else {
    doPlaocTasks();
  }
}
