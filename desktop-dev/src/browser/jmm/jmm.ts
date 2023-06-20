import type { OutgoingMessage } from "node:http";
import type { $BootstrapContext } from "../../core/bootstrapContext.ts";
import { NativeMicroModule } from "../../core/micro-module.native.ts";
import { $Callback, createSignal } from "../../helper/createSignal.ts";
import { $DWEB_DEEPLINK, $MMID } from "../../helper/types.ts";
import type { HttpDwebServer } from "../../sys/http-server/$createHttpDwebServer.ts";
import { createApiServer, getAllApps } from "./jmm.api.serve.ts";
import { cancel, install, pause, resume } from "./jmm.handler.ts";
import { createWWWServer } from "./jmm.www.serve.ts";
import {
  $JsMMMetadata,
  JsMMMetadata,
  JsMicroModule,
} from "./micro-module.js.ts";

export class JmmNMM extends NativeMicroModule {
  mmid = "jmm.browser.dweb" as const;
  dweb_deeplinks = ["dweb:install"] as $DWEB_DEEPLINK[];
  downloadStatus: DOWNLOAD_STATUS = 0;
  wwwServer: HttpDwebServer | undefined;
  apiServer: HttpDwebServer | undefined;

  resume: {
    // deno-lint-ignore ban-types
    handler: Function;
    response: OutgoingMessage | undefined;
  } = {
    response: undefined,
    handler: async () => {},
  };

  async _bootstrap(context: $BootstrapContext) {
    /// 注册所有已经下载的应用
    for (const appInfo of await getAllApps()) {
      const metadata = new JsMMMetadata(appInfo);
      const jmm = new JsMicroModule(metadata);
      context.dns.install(jmm);
    }
    // console.always(`[${this.mmid}] _bootstrap`);

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
    //   获取全部的 appsInfo
    this.registerCommonIpcOnMessageHandler({
      pathname: "/appsinfo",
      matchMode: "full",
      input: {},
      output: "object",
      handler: async (_args, _client_ipc, _request) => {
        const appsInfo = await getAllApps();
        return appsInfo;
      },
    });

    /// dweb deeplink
    this.registerCommonIpcOnMessageHandler({
      protocol: "dweb:",
      pathname: "install",
      matchMode: "full",
      input: { url: "string" },
      output: "void",
      handler: async (args) => {
        console.log("jmm", "!!!! install", args);
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
  name: string;
  short_name: string;
  id: $MMID;
  bundle_url: string;
  bundle_hash: string;
  bundle_size: number;
  icon: string;
  images: string[];
  description: string;
  author: string[];
  version: string;
  categories: string[];
  home: string;
  server: {
    root: string;
    entry: string;
  };
  permissions: string[];
  plugins: string[];
  release_date: string;
}

export enum DOWNLOAD_STATUS {
  DOWNLOAD,
  PAUSE,
  CANCEL,
}
