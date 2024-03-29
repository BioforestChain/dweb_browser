import mime from "mime";
import fs from "node:fs";
import type { OutgoingMessage } from "node:http";
import path from "node:path";
import type { $BootstrapContext } from "../../core/bootstrapContext.ts";
import { MICRO_MODULE_CATEGORY } from "../../core/category.const.ts";
import { buildRequestX } from "../../core/helper/ipcRequestHelper.ts";
import { FetchError } from "../../core/ipc/ipc.ts";
import { NativeMicroModule } from "../../core/micro-module.native.ts";
import { $DWEB_DEEPLINK, $MMID } from "../../core/types.ts";
import { fetchMatch } from "../../helper/patternHelper.ts";
import { readableToWeb } from "../../helper/stream/nodejsStreamHelper.ts";
import { z, zq } from "../../helper/zodHelper.ts";
import { createHttpDwebServer, type HttpDwebServer } from "../../std/http/helper/$createHttpDwebServer.ts";
import { nativeFetchAdaptersManager } from "../../sys/dns/nativeFetch.ts";
import { DOWNLOAD_STATUS } from "./const.ts";
import { JMM_APPS_PATH, JMM_DB, createApiServer } from "./jmm.api.serve.ts";
import { JmmServer } from "./jmm.www.serve.ts";
import { JsMMMetadata, JsMicroModule } from "./micro-module.js.ts";
import { NativeWindow } from "../../helper/userAgentDataInject.ts"

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
      filepath = decodeURIComponent(filepath);
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
        console.error("jmm",err);
        return new Response(String(err), { status: 404 });
      }
    }
  }
}, 0);

export class JmmNMM extends NativeMicroModule {
  mmid = "jmm.browser.dweb" as const;
  name = "Js MicroModule Management";
  override short_name = "JMM";
  override dweb_deeplinks = ["dweb://install"] as $DWEB_DEEPLINK[];
  override categories = [MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Hub_Service];
  downloadStatus: DOWNLOAD_STATUS = 0;
  jmmServer: JmmServer | undefined;
  apiServer: HttpDwebServer | undefined;

  constructor() {
    super();
    NativeWindow.brands.push({brand: this.mmid, version: `${JsMicroModule.VERSION}.${JsMicroModule.PATCH}`});
  }

  resume: {
    handler: Function;
    response: OutgoingMessage | undefined;
  } = {
    response: undefined,
    handler: async () => {},
  };

  async _bootstrap(context: $BootstrapContext) {
    /// 注册所有已经下载的应用
    for (const appInfo of await JMM_DB.all()) {
      const metadata = new JsMMMetadata(appInfo);
      const jmm = new JsMicroModule(metadata);
      context.dns.install(jmm);
    }
    // 构建api 服务
    await createApiServer.call(this);
    // 构建html服务
    const wwwServer = await this._createJMMWebServer();
    this.jmmServer = await JmmServer.create(this, wwwServer);
    const query_metadataUrl = zq.object({
      metadataUrl: zq.string(),
    });
    const query_app_id = zq.object({ app_id: zq.mmid() });
    const query_url = zq.object({ url: z.string().url() });

    const onFetchMatcher = fetchMatch()
      .get("/detail", async (event) => {
        const { app_id: mmid } = query_app_id(event.searchParams);
        const appInfo = await JMM_DB.find(mmid);
        if (appInfo === undefined) {
          throw new FetchError(`not found ${mmid}`, { status: 404 });
        }
        return Response.json(appInfo);
      })
      .deeplink("install", async (event) => {
        const { url } = query_url(event.searchParams);
        /// 安装应用并打开
        this.jmmServer?.show(url);
      })
      .get("/install", async (event) => {
        const { metadataUrl } = query_metadataUrl(event.searchParams);
        const res = Response.json(this.jmmServer?.show(metadataUrl));
        return res;
      })
      .get("/uninstall", async (event) => {
        const { app_id: mmid } = query_app_id(event.searchParams);
        await this.uninstallApp(context, mmid);
        return Response.json({ ok: true });
      })
      .get("/pause", async (event) => {
        return Response.json(await this.pauseInstall());
      })
      .get("/resume", async (event) => {
        return Response.json(await this.resumeInstall());
      })
      .get("/cancel", async (event) => {
        return Response.json(await this.cancelInstall());
      });
    this.onFetch(onFetchMatcher.run).internalServerError();
  }

  private openApp = async (context: $BootstrapContext, mmid: $MMID) => {
    return await context.dns.open(mmid);
  };
  private closeApp = async (context: $BootstrapContext, mmid: $MMID) => {
    return await context.dns.close(mmid);
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

  protected _shutdown(): unknown {
    throw new Error("Method not implemented.");
  }
  private async _createJMMWebServer() {
    const wwwServer = await createHttpDwebServer(this, {
      subdomain: "www",
    });
    const serverIpc = await wwwServer.listen();
    serverIpc.onFetch(async (event) => {
      const { pathname } = event.url;
      const url = `file:///sys/browser/jmm${pathname}?mode=stream`;
      const request = buildRequestX(url, {
        method: event.method,
        headers: event.headers,
        body: event.ipcRequest.body.raw,
      });
      // 打开首页的 路径
      return await this.nativeFetch(request);
    });
    return wwwServer;
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
