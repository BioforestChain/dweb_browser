import mime from "mime";
import fs from "node:fs";
import type { OutgoingMessage } from "node:http";
import path from "node:path";
import type { $BootstrapContext } from "../../core/bootstrapContext.ts";
import { FetchError } from "../../core/ipc/ipc.ts";
import { NativeMicroModule } from "../../core/micro-module.native.ts";
import { $DWEB_DEEPLINK, $MMID } from "../../core/types.ts";
import { $Callback, createSignal } from "../../helper/createSignal.ts";
import { readableToWeb } from "../../helper/nodejsStreamHelper.ts";
import { fetchMatch } from "../../helper/patternHelper.ts";
import { parseQuery, z, zq } from "../../helper/zodHelper.ts";
import type { HttpDwebServer } from "../../std/http/helper/$createHttpDwebServer.ts";
import { nativeFetchAdaptersManager } from "../../sys/dns/nativeFetch.ts";
import { JMM_APPS_PATH, JMM_DB, createApiServer, getAllApps } from "./jmm.api.serve.ts";
import { createWWWServer } from "./jmm.www.serve.ts";
import { JsMMMetadata, JsMicroModule } from "./micro-module.js.ts"; // import Nedb from "@seald-io/nedb";

nativeFetchAdaptersManager.append((remote, parsedUrl) => {
  /// fetch("file:///jmm/") 匹配
  if (parsedUrl.protocol === "file:" && parsedUrl.hostname === "") {
    let filepath: undefined | string;
    if (
      parsedUrl.pathname.startsWith("/jmm/") &&
      remote.mmid === "jmm.browser.dweb" /// 只能自己访问
    ) {
      filepath = path.join(JMM_APPS_PATH, parsedUrl.pathname.replace("/jmm/", "/"));
    } else if (parsedUrl.pathname.startsWith("/usr/")) {
      filepath = path.join(JMM_APPS_PATH, remote.mmid, parsedUrl.pathname);
    }
    /// read file
    if (filepath) {
      try {
        const stats = fs.statSync(filepath);
        if (stats.isDirectory()) {
          throw stats;
        }
        const ext = path.extname(filepath);
        return new Response(readableToWeb(fs.createReadStream(filepath)), {
          status: 200,
          headers: {
            "Content-Length": stats.size + "",
            "Content-Type": mime.getType(ext) || "application/octet-stream",
          },
        });
      } catch (err) {
        console.error(err);
        return new Response(String(err), { status: 404 });
      }
    }
  }
}, 0);

export class JmmNMM extends NativeMicroModule {
  mmid = "jmm.browser.dweb" as const;
  override dweb_deeplinks = ["dweb:install"] as $DWEB_DEEPLINK[];
  downloadStatus: DOWNLOAD_STATUS = 0;
  wwwServer: HttpDwebServer | undefined;
  apiServer: HttpDwebServer | undefined;

  resume: {
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

    await createWWWServer.call(this);
    await createApiServer.call(this);

    const query_metadataUrl = z.object({
      metadataUrl: zq.string(),
    });
    const query_app_id = z.object({ app_id: zq.mmid() });
    const query_url = z.object({ url: z.string().url() });

    const onFetchMatcher = fetchMatch()
      .get("/openApp", async (event) => {
        const { app_id: mmid } = parseQuery(event.searchParams, query_app_id);
        return Response.json(await this.openApp(context, mmid));
      })
      .get("/closeApp", async (event) => {
        const { app_id: mmid } = parseQuery(event.searchParams, query_app_id);
        return Response.json(await this.closeApp(context, mmid));
      })
      .get("/detailApp", async (event) => {
        const { app_id: mmid } = parseQuery(event.searchParams, query_app_id);
        const appsInfo = await JMM_DB.find(mmid);
        if (appsInfo === undefined) {
          throw new FetchError(`not found ${mmid}`, { status: 404 });
        }
        return Response.json(appsInfo);
      })
      .get("/install", async (event) => {
        const { metadataUrl } = parseQuery(event.searchParams, query_metadataUrl);
        return Response.json(await this.startInstall(metadataUrl));
      })
      .get("/uninstall", async (event) => {
        const { app_id: mmid } = parseQuery(event.searchParams, query_app_id);
        return Response.json(await this.uninstallApp(context, mmid));
      })
      .get("/pause", async (event) => {
        return Response.json(await this.pauseInstall());
      })
      .get("/resume", async (event) => {
        return Response.json(await this.resumeInstall());
      })
      .get("/cancel", async (event) => {
        return Response.json(await this.cancelInstall());
      })
      .deeplink("install", async (event) => {
        const { url } = parseQuery(event.searchParams, query_url);
        /// 安装应用并打开
        await this.startInstall(url);
        const off = this.onInstalled.listen((info, fromUrl) => {
          if (fromUrl === url) {
            off();
            context.dns.connect(info.id);
          }
        });
      });
    this.onFetch((event) => {
      return onFetchMatcher.run(event);
    });
  }

  private openApp = async (context: $BootstrapContext, mmid: $MMID) => {
    return await context.dns.open(mmid);
  };
  private closeApp = async (context: $BootstrapContext, mmid: $MMID) => {
    return await context.dns.close(mmid);
  };

  private startInstall = async (metadataUrl: string) => {
    // 需要同时查询参数传递进去
    if (this.wwwServer === undefined) throw new Error(`this.wwwServer === undefined`);
    const indexUrl = this.wwwServer.startResult.urlInfo.buildInternalUrl((url) => {
      url.pathname = "/index.html";
      url.searchParams.set("metadataUrl", metadataUrl);
    }).href;
    const openUrl = new URL(`file://mwebview.browser.dweb/open`);
    openUrl.searchParams.set("url", indexUrl);
    await this.nativeFetch(openUrl);
    return true;
  };
  private pauseInstall = async () => {
    console.log("jmm", "................ 下载暂停但是还没有处理");
    return true;
  };

  private resumeInstall = async () => {
    console.log("jmm", "................ 从新下载但是还没有处理");
    return true;
  };

  // 业务逻辑是会 停止下载 立即关闭下载页面
  private cancelInstall = async () => {
    console.log("jmm", "................ 从新下载但是还没有处理");
    return true;
  };

  private uninstallApp = async (context: $BootstrapContext, mmid: $MMID) => {
    await JMM_DB.remove(mmid);
    await context.dns.uninstall(mmid);
  };

  readonly onInstalled = createSignal<$Callback<[$AppMetaData, string]>>();

  protected _shutdown(): unknown {
    throw new Error("Method not implemented.");
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
