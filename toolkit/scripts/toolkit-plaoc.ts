import { debounce } from "@dweb-browser/helper/decorator/$debounce.ts";
import { $once } from "@dweb-browser/helper/decorator/$once.ts";
import { registryViteBuilder } from "../../scripts/helper/npmBuilder.ts";
import { rootResolve } from "../../scripts/helper/resolver.ts";
import { watchFs } from "../../scripts/helper/watchFs.ts";
import { doBundleServer } from "../plaoc/scripts/bundle-server.ts";
import {
  dwebCore,
  dwebHelper,
  dwebJsProcess,
  dwebPolyfill,
  dwebSign,
  dwebTranslate,
  plaocCli,
  plaocIsDweb,
  plaocPlugins,
  plaocServer,
} from "./build_npm.ts";
import { toolkitInit } from "./toolkit-init.ts";

const plaocExamples = registryViteBuilder({
  name: "plaoc:examples:plugin-demo",
  inDir: "./toolkit/plaoc/examples/plugin-demo",
  outDir: "npm/@plaoc__examples/plugin-demo",
});
// 可编程后端
const plaocServerExamples = registryViteBuilder({
  name: "plaoc:examples:server-demo",
  inDir: "./toolkit/plaoc/examples/plaoc-server",
  outDir: "npm/@plaoc__examples/plaoc-server",
});
const htmlExamples = registryViteBuilder({
  name: "plaoc:examples:html-demo",
  inDir: "./toolkit/plaoc/examples/html-demo",
  outDir: "npm/@plaoc__examples/html-demo",
});

/// 这里要包含所有的任务，以确保 reset 能正确工作
const plaocTasks = [
  plaocServer,
  plaocCli,
  plaocPlugins,
  plaocIsDweb,
  dwebTranslate,
  dwebSign,
  Object.assign(
    $once(() => plaocExamples(Deno.args.includes("--watch") ? ["--dev"] : [])),
    {
      reset() {
        // 同步复制配置文件
      },
    }
  ),
  Object.assign(
    $once(() => plaocServerExamples(Deno.args.includes("--watch") ? ["--dev"] : [])),
    {
      reset() {
        // 同步复制配置文件
      },
    }
  ),
  Object.assign(
    $once(() => htmlExamples(Deno.args.includes("--watch") ? ["--dev"] : [])),
    {
      reset() {
        // 同步复制配置文件
      },
    }
  ),
  Object.assign(
    $once(() => doBundleServer()),
    {
      reset() {
        // doBundleServer 自带 watch
      },
    }
  ),
];
export const doPlaocTasks = async () => {
  // 核心依赖要先安装完
  await dwebHelper();
  await dwebPolyfill();
  await dwebCore();
  await dwebJsProcess();
  await Promise.all(plaocTasks.map((task) => task()));
};

if (import.meta.main) {
  // console.log("PATH:", Deno.env.get("PATH"));
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
