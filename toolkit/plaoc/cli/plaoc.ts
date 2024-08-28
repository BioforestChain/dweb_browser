import { doBundleCommand } from "./bundle.ts";
import { doConfigCommand } from "./config.ts";
import { Command } from "./deps/cliffy.ts";
import { fileURLToPath, node_fs } from "./deps/node.ts";
import { doInitCommand } from "./init.ts";
import { doLiveCommand } from "./live.ts";
import { doServeCommand } from "./serve.ts";
import { doWebAdvCommand } from "./web-dav.ts";
// import { doHookServer } from "./server/index.ts";

const findPackageJson = async () => {
  let deep = 1;
  let cur = import.meta.url;
  let pre = "";
  while (pre !== cur) {
    try {
      const newCur = await import.meta.resolve(`${"../".repeat(deep)}package.json`);
      pre = cur;
      cur = newCur;
      return JSON.parse(node_fs.readFileSync(fileURLToPath(cur), "utf-8"));
    } catch {
      deep++;
    }
  }
  return { version: "0.0.0-dev" };
};
const packageJson = await findPackageJson();
const packageVersion = packageJson.version;

await new Command()
  .name("@plaoc/cli")
  .description("plaoc front-end and back-end packaging tools.")
  .version(`v${packageVersion}`)

  .command("init", doInitCommand)
  .example("init app name", "plaoc init xxx-app")
  .example("init current workspace", "plaoc init")

  .command("serve", doServeCommand)
  .example("developer service", "plaoc serve http://xx.xx.xx.xx:xxxx/")

  .command("live", doLiveCommand)
  .example("live edit mode", "plaoc live http://xx.xx.xx.xx:xxxx/")

  .command("bundle", doBundleCommand)
  .example("Packaging can only deliver static folders", "plaoc bundle ./dist")

  .command("config", doConfigCommand)
  .example("set webadv-server-url", "plaoc config set webadv-server-url http://xx.xx.xx.xx:xxxx/")
  .example("set webadv-server-auth", "plaoc config set webadv-server-auth xxx")

  .command("run", doWebAdvCommand)
  .example("deploy web adv server", "plaoc run webadv-server -p 7777 -host 0.0.0.0 --download-dir /usr/xxx/")
  //TODO
  // .command("bundle-web-hook-server", doHookServer)
  // .example(
  //   "deploy an application server",
  //   "plaoc bundle-web-hook-server -p 7777 -host 0.0.0.0 --download-dir /usr/xxx/"
  // )
  .parse(Deno.args);
