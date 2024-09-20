import { Command, EnumType } from "../deps/cliffy.ts";
import { webdav } from "../deps/webdav-server.ts";
import { HostType } from "./type.ts";

/// TOOD:这将 启动一个webadv server 来提供文件上传的钩子，用于快速部署app到应用商城。

export enum EWebAdv {
  webadvServer = "webadv-server",
}

type TWebAdv = {
  port: string;
  host: string;
  downloadDir: string;
};

const webAdvType = new EnumType(EWebAdv);

export const doWebAdvCommand = new Command()
  .type("webAdvType", webAdvType)
  .type("hostType", new HostType())
  .arguments("<type:webAdvType>")
  .description("Set up a hook to deploy to the server.")
  .option("-p --port <port:string>", "service port.", {
    default: "7777",
  })
  .option("-h --host <host:hostType>", "server address.", {
    default: "0.0.0.0",
  })
  .option("-ddir --download-dir <downloadDir:string>", "source address.", {
    default: "./",
  })
  .action((options, type) => {
    if (type === EWebAdv.webadvServer) {
      return createWebAdv(options);
    }

    throw new Error(`not found command .please use --help.`);
  });

const createWebAdv = (argv: TWebAdv) => {
  const server = new webdav.WebDAVServer();
  server.setFileSystem(argv.downloadDir, new webdav.PhysicalFileSystem(argv.downloadDir), (success) => {
    server.start(() => console.log("READY", success));
  });
};
