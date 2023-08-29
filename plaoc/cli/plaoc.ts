import { doBundleFlags } from "./bundle.ts";
import { doConfigFlags } from "./config.ts";
import { Command } from "./deps.ts";
import { doServeFlags } from "./serve.ts";
import { doHookServer } from "./server/index.ts";

//node --trace-warnings
// const cliNpm = import.meta.resolve(`../scripts/npm.cli.json`);
// const npmConfigs = (await import(cliNpm, { assert: { type: "json" } })).default;

await new Command()
  .name("@plaoc/cli")
  .description("plaoc front-end and back-end packaging tools.")
  .version(`v0.2.1`)
  .command("serve", doServeFlags)
  .example("developer service", "plaoc serve http://xx.xx.xx.xx:xxxx/")

  .command("bundle", doBundleFlags)
  .example("Packaging can only deliver static folders", "plaoc bundle ./dist")

  .command("config", doConfigFlags)
  .example("Packaging can only deliver static folders", "plaoc bundle ./dist")

  .command("bundle-web-hook-server", doHookServer)
  .example(
    "deploy an application server",
    "plaoc bundle-web-hook-server -p 7777 -host 0.0.0.0 --download-dir /usr/xxx/"
  )
  .parse(Deno.args);
