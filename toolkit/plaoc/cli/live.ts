import { Command } from "./deps/cliffy.ts";
import { SERVE_MODE, type $ServeOptions } from "./helper/const.ts";
import { startServe } from "./serve.ts";

export const doLiveCommand = new Command()
  .arguments("<web_public:string>")
  .description("Developer Service Extension Directive.")
  .option("-p --port <port:string>", "Service port.", {
    default: "8096",
  })
  .option(
    "-c --config-dir <config_dir:string>",
    "The config directory is set to automatically traverse upwards when searching for configuration files (manifest.json/plaoc.json). The default setting for the target directory is <web_public>"
  )
  .option("-s --web-server <serve:string>", "Specify the path of the programmable backend. ")
  .option("-d --dev <dev:boolean>", "Enable development mode.", {
    default: true,
  })
  .action(async (options, arg1) => {
    await startServe({ ...options, webPublic: arg1, mode: SERVE_MODE.LIVE } satisfies $ServeOptions, 8000);
  });
