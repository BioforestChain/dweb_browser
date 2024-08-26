import { Command } from "./deps/cliffy.ts";
import { SERVE_MODE, type $LiveOptions } from "./helper/const.ts";
import { startServe } from "./serve.ts";

export const doLiveCommand = new Command()
  .arguments("<source_dir:string>")
  .description("Developer Service Extension Directive.")
  .option("-p --port <port:string>", "Specify the service port. default:8096.", {
    default: "8096",
  })
  .option(
    "-c --config-dir <config_dir:string>",
    "The config directory is set to automatically traverse upwards when searching for configuration files (manifest.json/plaoc.json). The default setting for the target directory is <web_public>"
  )
  .option("-s --web-server <serve:string>", "Specify the path of the programmable backend. ")
  .action((options, arg1) => {
    startLive({ ...options, webPublic: arg1, mode: SERVE_MODE.USR_WWW } satisfies $LiveOptions);
  });

const startLive = (flags: $LiveOptions) => {
  startServe(flags, 8000);
};
