import process from "node:process";
import { JsMicroModule } from "../../browser/jmm/micro-module.js.ts";
import type { $BootstrapContext, $DnsMicroModule } from "../../core/bootstrapContext.ts";
import { MICRO_MODULE_CATEGORY } from "../../core/category.const.ts";
import { $normalizeRequestInitAsIpcRequestArgs, buildRequestX } from "../../core/helper/ipcRequestHelper.ts";
import { IpcEvent, IpcResponse } from "../../core/ipc/index.ts";
import { NativeMicroModule } from "../../core/micro-module.native.ts";
import type { MicroModule } from "../../core/micro-module.ts";
import { $ConnectResult, connectMicroModules } from "../../core/nativeConnect.ts";
import type { $DWEB_DEEPLINK, $MMID } from "../../core/types.ts";
import { ChangeableMap, changeState } from "../../helper/ChangeableMap.ts";
import { simpleEncoder } from "../../helper/encoding.ts";
import { mapHelper } from "../../helper/mapHelper.ts";
import { fetchMatch } from "../../helper/patternHelper.ts";
import { ReadableStreamOut } from "../../helper/stream/readableStreamHelper.ts";
import { zq } from "../../helper/zodHelper.ts";
import { PromiseOut } from "./../../helper/PromiseOut.ts";
import { nativeFetchAdaptersManager } from "./nativeFetch.ts";

class MyDnsMicroModule implements $DnsMicroModule {
  constructor(private dnsNN: DnsNMM, private fromMM: MicroModule) {}

  install(mm: MicroModule): void {
    this.dnsNN.install(mm);
  }

  async uninstall(mmid: $MMID) {
    return this.dnsNN.uninstall(mmid);
  }

  connect(mmid: $MMID, reason?: Request) {
    return this.dnsNN[connectTo_symbol](this.fromMM, mmid, reason ?? new Request(`file://${mmid}`));
  }

  async query(mmid: $MMID) {
    return this.dnsNN.query(mmid)?.toManifest();
  }
  async search(category: MICRO_MODULE_CATEGORY) {
    return [...this.dnsNN.search(category)].map((mm) => mm.toManifest());
  }

  async open(mmid: $MMID) {
    /// 已经在运行中了，直接返回true
    if (this.dnsNN.running_apps.has(mmid)) {
      return true;
    }
    /// 尝试运行，成功就返回true
    try {
      await this.dnsNN.open(mmid);
      return true;
    } catch {
      return false;
    }
  }
  async close(mmid: $MMID) {
    if (this.dnsNN.running_apps.has(mmid)) {
      if ((await this.dnsNN.close(mmid)) === 1) {
        return true;
      }
    }
    return false;
  }
  async restart(mmid: $MMID) {
    await this.close(mmid);
    await this.open(mmid);
  }
}

class MyBootstrapContext implements $BootstrapContext {
  constructor(readonly dns: MyDnsMicroModule) {}
}

const connectTo_symbol = Symbol("connectTo");

/** DNS 服务，内核！
 * 整个系统都围绕这个 DNS 服务来展开互联
 */
export class DnsNMM extends NativeMicroModule {
  mmid = "dns.std.dweb" as const;
  name = "Dweb Name System";
  override short_name = "DNS";
  override dweb_deeplinks = ["dweb:open"] as $DWEB_DEEPLINK[];
  override categories = [MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Routing_Service];
  private installApps = new ChangeableMap<$MMID, MicroModule>();
  readonly running_apps = new ChangeableMap<$MMID, Promise<MicroModule>>();

  override bootstrap(ctx: $BootstrapContext = new MyBootstrapContext(new MyDnsMicroModule(this, this))) {
    return super.bootstrap(ctx);
  }

  bootstrapMicroModule(fromMM: MicroModule) {
    return fromMM.bootstrap(new MyBootstrapContext(new MyDnsMicroModule(this, fromMM)));
  }

  // 拦截 nativeFetch
  private mmConnectsMap = new Map<MicroModule, Map<$MMID, PromiseOut<$ConnectResult>>>();

  /**
   * 创建通过 MessageChannel 实现同行的 ipc
   * @param fromMM
   * @param toMmid
   * @param reason
   * @returns
   */
  [connectTo_symbol](fromMM: MicroModule, toMmid: $MMID, reason: Request) {
    // v2.0
    // 创建连接
    const fromMMconnectsMap = mapHelper.getOrPut(
      this.mmConnectsMap,
      fromMM,
      () => new Map<$MMID, PromiseOut<$ConnectResult>>()
    );

    const fromPo = mapHelper.getOrPut(fromMMconnectsMap, toMmid, () => {
      const po = new PromiseOut<$ConnectResult>();
      (async () => {
        /// 与指定应用建立通讯
        const toMM = await this.open(toMmid);
        const result = await connectMicroModules(fromMM, toMM, reason);
        const [ipcForFromMM, ipcForToMM] = result;

        // 监听生命周期 释放引用
        ipcForFromMM.onClose(() => {
          fromMMconnectsMap.delete(toMmid);
        });
        po.resolve(result);
        // 反向存储 toMM
        if (ipcForToMM) {
          const result2: $ConnectResult = [ipcForToMM, ipcForFromMM];
          const toMMconnectsMap = mapHelper.getOrPut(
            this.mmConnectsMap,
            toMM,
            () => new Map<$MMID, PromiseOut<$ConnectResult>>()
          );

          mapHelper.getOrPut(toMMconnectsMap, fromMM.mmid, () => {
            const toMMPromise = new PromiseOut<$ConnectResult>();
            ipcForToMM.onClose(() => {
              toMMconnectsMap.delete(fromMM.mmid);
            });
            toMMPromise.resolve(result2);
            return toMMPromise;
          });
        }
      })().catch(po.reject);
      return po;
    });
    return fromPo.promise;
  }

  override async _bootstrap(context: $BootstrapContext) {
    this.install(this);
    this.running_apps.set(this.mmid, Promise.resolve(this));

    const query_appId = zq.object({
      app_id: zq.mmid(),
    });
    const onFetchHanlder = fetchMatch()
      .get("/open", async (event) => {
        const { app_id } = query_appId(event.searchParams);
        await this.open(app_id);
        return Response.json(true);
      })
      .get("/close", async (event) => {
        const { app_id } = query_appId(event.searchParams);
        return Response.json(await this.close(app_id as $MMID));
      })
      .get("/query", async (event) => {
        const { app_id } = query_appId(event.searchParams);
        const app = this.query(app_id as $MMID);
        if (!app) return new Response(`not found app for ${app_id}`, { status: 404 });
        if (app instanceof JsMicroModule) {
          return Response.json(app.metadata.config);
        }
        return Response.json(app.toManifest());
      })
      .get("/observe/install-apps", async (event) => {
        const responseBody = new ReadableStreamOut<Uint8Array>();
        const doWriteJsonline = async (state: changeState<string>) => {
          responseBody.controller.enqueue(simpleEncoder(JSON.stringify(state) + "\n", "utf8"));
        };
        /// 监听变更，推送数据
        const off = this.installApps.onChange((state) => doWriteJsonline(state));
        event.ipc.onClose(() => {
          off();
          responseBody.controller.close();
        });
        return { body: responseBody.stream };
      })
      .get("/observe/running-apps", async (event) => {
        const responseBody = new ReadableStreamOut<Uint8Array>();
        const doWriteJsonline = async (state: changeState<string>) => {
          responseBody.controller.enqueue(simpleEncoder(JSON.stringify(state) + "\n", "utf8"));
        };
        /// 监听变更，推送数据
        const off = this.running_apps.onChange((state) => doWriteJsonline(state));
        event.ipc.onClose(() => {
          off();
          responseBody.controller.close();
        });
        return { body: responseBody.stream };
      })
      .deeplink("open", async (event) => {
        const app_id = event.url.pathname.replace("open/", "");
        await this.open(app_id as $MMID);
        return Response.json(true);
      });
    this.onFetch((event) => onFetchHanlder.run(event)).internalServerError();

    this.onAfterShutdown(
      nativeFetchAdaptersManager.append(async (fromMM, parsedUrl, requestInit) => {
        const req_url = parsedUrl.href;
        let ipc_response: undefined | IpcResponse;
        if (parsedUrl.protocol === "file:" && parsedUrl.hostname.endsWith(".dweb")) {
          const mmid = parsedUrl.hostname as $MMID;
          const reason_request = buildRequestX(req_url, requestInit);
          const [ipc] = await this[connectTo_symbol](fromMM, mmid, reason_request);
          ipc_response = await ipc.request(reason_request.url, reason_request);
        } else if (parsedUrl.protocol === "dweb:") {
          ipc_response = await this.postDeeplink(fromMM, req_url);
          if (ipc_response === undefined) {
            return new Response("Dweb Deeplink No Found Matchs", { status: 502, statusText: "Bad Gateway" });
          }
        }
        return ipc_response?.toResponse(req_url);
      })
    );

    //#region 启动引导程序
    (await this.connect(`boot.sys.dweb`))?.postMessage(IpcEvent.fromText("activity", ""));
    //#endregion

    /**
     * 获取应用程序命令行参数： 如果是开发模式，electron 后面需要跟 目录，所以从 2 开始
     * 如果是应用模式，只需要应用程序名，所以从1开始
     */
    const args = process.argv.slice(
      process.argv.findIndex((arg: string) => /^\w+$/.test(arg))
      // path.parse(process.argv0).name.toLowerCase() === "electron" ? 2 : 1
    );

    if (args.length > 0) {
      const [domain, ...deeplink_args] = args;
      const dweb_deeplink = `dweb:${domain}`;
      const buildDeeplinkUrl = () => {
        const normalizePath: string[] = [];
        const normalizeQuery = new URLSearchParams();
        let hasSearch = false;
        for (let i = 0; i < deeplink_args.length; i++) {
          const arg = deeplink_args[i];
          if (arg.startsWith("-")) {
            const k_v = arg.match(/^\-+(.+?)\=(.+)/);
            if (k_v) {
              normalizeQuery.append(k_v[1], k_v[2]);
            } else {
              normalizeQuery.append(arg.replace(/^-+/, ""), deeplink_args[++i]);
            }
            hasSearch = true;
          } else {
            normalizePath.push(arg);
          }
        }
        const pathname = "/" + normalizePath.join("/");
        const url = new URL(
          (dweb_deeplink + pathname).replace(/\/{2,}/g, "/").replace(/\/$/, "") +
            (hasSearch ? "?" + normalizeQuery.toString() : "")
        );
        return url;
      };

      /// 查询匹配deeplink的程序
      void this.postDeeplink(this, buildDeeplinkUrl().href);
    }
  }

  /** 执行 deeplink 指令 */
  private async postDeeplink(fromMM: MicroModule, deeplinkUrl: string) {
    /// 查询匹配deeplink的程序
    for (const app of this.installApps.values()) {
      if (undefined !== app.dweb_deeplinks.find((dl) => deeplinkUrl.startsWith(dl))) {
        /// 在win平台，deeplink链接会变成 dweb:search/?q=xxx ，需要把/去掉
        if(process.platform === "win32") {
          deeplinkUrl = deeplinkUrl.replace(/\/{1}\?{1}/, "?");
        }
        const req = buildRequestX(deeplinkUrl);
        const [ipc] = await this[connectTo_symbol](fromMM, app.mmid, req);
        const ipc_req_init = await $normalizeRequestInitAsIpcRequestArgs(req);
        /// 发送请求
        return await ipc.request(deeplinkUrl, ipc_req_init);
      }
    }
  }

  async _shutdown() {
    for (const mmid of this.running_apps.keys()) {
      await this.close(mmid);
    }
  }

  /** 安装应用 */
  install(mm: MicroModule) {
    this.installApps.set(mm.mmid, mm);
  }

  /** 卸载应用 */
  uninstall(mmid: $MMID) {
    return this.installApps.delete(mmid);
  }

  /** 查询应用 */
  query(mmid: $MMID) {
    return this.installApps.get(mmid);
  }

  *search(category: MICRO_MODULE_CATEGORY) {
    for (const app of this.installApps.values()) {
      if (app.categories.includes(category)) {
        yield app;
      }
    }
  }

  /** 打开应用 */
  async open(mmid: $MMID) {
    const app = await mapHelper.getOrPut(this.running_apps, mmid, async () => {
      const mm = this.query(mmid);
      if (mm === undefined) {
        console.error("dns", "没有指定的 mm 抛出错误");
        this.running_apps.delete(mmid);
        throw new Error(`no found app: ${mmid}`);
      }
      // @TODO bootstrap 函数应该是 $singleton 修饰
      await this.bootstrapMicroModule(mm);
      mm.onAfterShutdown(() => {
        this._remove_running_apps(mmid);
      });
      return mm;
    });

    return app;
  }

  /** 关闭应用 */
  async close(mmid: $MMID) {
    const app = await this._remove_running_apps(mmid);
    if (app === undefined) {
      // 关闭失败没有匹配的 microModule 运行
      return -1;
    }
    try {
      await app.shutdown();
      return 1;
    } catch {
      return 0;
    }
  }
  private async _remove_running_apps(mmid: $MMID) {
    const app = await this.running_apps.get(mmid);
    if (app !== undefined) {
      this.running_apps.delete(mmid);
      return app;
    }
  }
}
