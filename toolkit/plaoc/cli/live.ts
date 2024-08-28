import { colors, Command } from "./deps/cliffy.ts";
import { node_path, node_process } from "./deps/node.ts";
import { type $LiveOptions } from "./helper/const.ts";
import { getLocalIP, startStaticFileServer } from "./helper/http-static-helper.ts";
import { startServe } from "./serve.ts";
import { createListenScoket } from "./ws/socketServer.ts";

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
  // 拿到静态文件服务端口
  const staticPort = +flags.staticPort;
  // 获取要监听的文件夹位置
  const baseDir = node_path.resolve(node_process.cwd(), flags.webPublic);

  // 验证文件夹是否存在
  try {
    Deno.statSync(baseDir);
  } catch {
    console.log(colors.red(`The delivered folder does not exist:${baseDir}`));
    return;
  }
  node_process.env.NODE_TLS_REJECT_UNAUTHORIZED = "0";
  // 获取本机ip
  const hostname = getLocalIP();
  // 启动静态文件服务
  startStaticFileServer(baseDir, hostname, staticPort, (address) => {
    flags.webPublic = address;
    startServe(flags);
  });
  // 启动socket Server 监听文件服务变化
  createListenScoket(hostname, staticPort, baseDir);
};
