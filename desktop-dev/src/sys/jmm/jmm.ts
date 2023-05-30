import type { OutgoingMessage } from "node:http";
import type { $BootstrapContext } from "../../core/bootstrapContext.ts";
import { NativeMicroModule } from "../../core/micro-module.native.ts";
import { $Callback, createSignal } from "../../helper/createSignal.ts";
import { $DWEB_DEEPLINK, $MMID } from "../../helper/types.ts";
import type { HttpDwebServer } from "../http-server/$createHttpDwebServer.ts";
import { createApiServer } from "./jmm.api.serve.ts";
import { cancel, install, pause, resume } from "./jmm.handler.ts";
import { createWWWServer } from "./jmm.www.serve.ts";
import { JsMicroModule } from "../../core/micro-module.js.ts";
import { $JsMMMetadata, JsMMMetadata } from "../../core/micro-module.js.ts"

export class JmmNMM extends NativeMicroModule {
  mmid = "jmm.sys.dweb" as const;
  dweb_deeplinks = ["dweb:install"] as $DWEB_DEEPLINK[];
  downloadStatus: DOWNLOAD_STATUS = 0;
  wwwServer: HttpDwebServer | undefined;
  apiServer: HttpDwebServer | undefined;
  donwloadStramController: ReadableStreamDefaultController | undefined;
  downloadStream: ReadableStream | undefined;

  resume: {
    handler: Function;
    response: OutgoingMessage | undefined;
  } = {
    response: undefined,
    handler: async () => {},
  };

  async _bootstrap(context: $BootstrapContext) {
    console.always(`[${this.mmid}] _bootstrap`);

    await createWWWServer.bind(this)();
    await createApiServer.bind(this)();
    //  安装 第三方 app
    this.registerCommonIpcOnMessageHandler({
      pathname: "/install",
      matchMode: "full",
      input: { metadataUrl: "string" },
      output: "boolean",
      handler: async (args) => {
        return await install(this, args);
      },
    });
    // 下载暂停
    this.registerCommonIpcOnMessageHandler({
      pathname: "/pause",
      matchMode: "full",
      input: {},
      output: "boolean",
      handler: (args) => pause(this, args),
    });
    this.registerCommonIpcOnMessageHandler({
      pathname: "/resume",
      matchMode: "full",
      input: {},
      output: "boolean",
      handler: (args) => resume(this, args),
    });
    this.registerCommonIpcOnMessageHandler({
      pathname: "/cancel",
      matchMode: "full",
      input: {},
      output: "boolean",
      handler: (args) => cancel(this, args),
    });

    /// dweb deeplink
    this.registerCommonIpcOnMessageHandler({
      protocol: "dweb:",
      pathname: "install",
      matchMode: "full",
      input: { url: "string" },
      output: "void",
      handler: async (args) => {
        console.log("jmm","!!!! install", args);
        await new Promise((resolve) => setTimeout(resolve, 1000));
        /// 安装应用并打开
        await install(this, { metadataUrl: args.url });
        const off = this.onInstalled.listen((info, fromUrl) => {
          if (fromUrl === args.url) {
            off();
            context.dns.connect(info.id);
          }
        });
      },
    });
  }

  readonly onInstalled = createSignal<$Callback<[$AppMetaData, string]>>();

  protected _shutdown(): unknown {
    throw new Error("Method not implemented.");
  }

  private async openInstallPage(metadataUrl: string) {
    const config = await this.nativeFetch(metadataUrl).object<$JsMMMetadata>();
    // TODO 打开安装页面

    /// 成功安装
    new JsMicroModule(new JsMMMetadata(config));
    return config;
  }
}

export interface $State {
  percent: number; // Overall percent (between 0 to 1)
  speed: number; // The download speed in bytes/sec
  size: {
    total: number; // The total payload size in bytes
    transferred: number; // The transferred payload size in bytes
  };
  time: {
    elapsed: number; // The total elapsed seconds since the start (3 decimals)
    remaining: number; // The remaining seconds to finish (3 decimals)
  };
}

export interface $AppMetaData {
  title: string;
  subtitle: string;
  id: $MMID;
  downloadUrl: string;
  icon: string;
  images: string[];
  introduction: string;
  author: string[];
  version: string;
  keywords: string[];
  home: string;
  mainUrl: string;
  server: {
    root: string;
    entry: string;
  };
  splashScreen: { entry: string };
  staticWebServers: $StaticWebServers[];
  openWebViewList: [];
  size: string;
  fileHash: "";
  permissions: string[];
  plugins: string[];
  releaseDate: string[];
}

export interface $StaticWebServers {
  root: string;
  entry: string;
  subdomain: string;
  port: number;
}

export enum DOWNLOAD_STATUS {
  DOWNLOAD,
  PAUSE,
  CANCEL,
}
