import { doBundleCommand } from "./bundle.ts";
import { doConfigCommand } from "./config.ts";
import { Command } from "./deps.ts";
import { doServeCommand } from "./serve.ts";
import { doWebAdvCommand } from "./web-dav.ts";
// import { doHookServer } from "./server/index.ts";

//node --trace-warnings
// const cliNpm = import.meta.resolve(`../scripts/npm.cli.json`);
// const npmConfigs = (await import(cliNpm, { assert: { type: "json" } })).default;

await new Command()
  .name("@plaoc/cli")
  .description("plaoc front-end and back-end packaging tools.")
  .version(`v0.2.9`)
  .command("serve", doServeCommand)
  .example("developer service", "plaoc serve http://xx.xx.xx.xx:xxxx/")

  .command("bundle", doBundleCommand)
  .example("Packaging can only deliver static folders", "plaoc bundle ./dist")

  .command("config", doConfigCommand)
  .example("set webadv-server-url", "plaoc config set webadv-server-url http://xx.xx.xx.xx:xxxx/")
  .example("set webadv-server-auth","plaoc config set webadv-server-auth xxx")

  
  .command("run", doWebAdvCommand)
  .example(
    "deploy web adv server",
    "plaoc run webadv-server -p 7777 -host 0.0.0.0 --download-dir /usr/xxx/"
  )
  //TODO
  // .command("bundle-web-hook-server", doHookServer)
  // .example(
  //   "deploy an application server",
  //   "plaoc bundle-web-hook-server -p 7777 -host 0.0.0.0 --download-dir /usr/xxx/"
  // )
  .parse(Deno.args);
