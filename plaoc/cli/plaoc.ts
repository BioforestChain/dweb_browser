import { doBundleFlags } from "./bundle.ts";
import { doConfigFlags } from "./config.ts";
import { Command } from "./deps.ts";
import { doServeFlags } from "./serve.ts";

//node --trace-warnings
// const cliNpm = import.meta.resolve(`../scripts/npm.cli.json`);
// const npmConfigs = (await import(cliNpm, { assert: { type: "json" } })).default;

await new Command()
  .name("@plaoc/cli")
  .description("plaoc front-end and back-end packaging tools.")
  .version(`v0.2.0`)
  .command("serve", doServeFlags)
  .example("developer service", "plaoc serve http://xx.xx.xx.xx:xxxx/")

  .command("bundle", doBundleFlags)
  .example("Packaging can only deliver static folders", "plaoc bundle ./dist")

  .command("config", doConfigFlags)
  .parse(Deno.args);
