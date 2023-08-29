import { Command } from "../deps.ts";

//TODO 快速部署一个简单的服务器来实现bundle应用的上传和下载
//plaoc bundle-web-hook-server -p 7777 -host 0.0.0.0 --download-dir /usr/xxx/

export const doHookServer =  new Command()
.description("Deploy an application server.")
.option("-p --port <port:string>", "service port.", {
  default: "7777",
})
.option("-h --host <host:string>", "server address.", {
  default: "0.0.0.0",
})
.option("-ddir --download-dir <downloadDir:string>", "source address.", {
  default: "./",
})
.action((_options) => {
  // throw new Error(`not found command for config ${set} ${hooks} ${key}.please use --help.`) 
});
