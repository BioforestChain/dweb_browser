import { Command } from "./deps/cliffy.ts";
import { type $LiveOptions } from "./helper/const.ts";
import { startStaticFileServer } from "./helper/http-static-helper.ts";
import { startServe } from "./serve.ts";

export const doLiveCommand = new Command()
  .arguments("<source_dir:string>")
  .description("Developer Service Extension Directive.")
  .option("-p --port <port:string>", "Specify the service port. default:8096.", {
    default: "8096",
  })
  .option("-p2 --static-port <static_port:string>", "Static service port. default:6666.", {
    default: "8111",
  })
  .option(
    "-c --config-dir <config_dir:string>",
    "The config directory is set to automatically traverse upwards when searching for configuration files (manifest.json/plaoc.json). The default setting for the target directory is <web_public>"
  )
  .option("-s --web-server <serve:string>", "Specify the path of the programmable backend. ")
  .action((options, arg1) => {
    startLive({ ...options, webPublic: arg1 } satisfies $LiveOptions);
  });

const startLive = (flags: $LiveOptions) => {
  // 启动静态文件服务器
  const staticPort = +flags.staticPort;
  startStaticFileServer(flags.webPublic, staticPort, (address) => {
    flags.webPublic = address;
    startServe(flags);
  });
};
