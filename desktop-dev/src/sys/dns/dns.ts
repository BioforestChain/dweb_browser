import process from "node:process";
import type { $BootstrapContext, $DnsMicroModule } from "../../core/bootstrapContext.ts";
import { $normalizeRequestInitAsIpcRequestArgs, buildRequestX } from "../../core/helper/ipcRequestHelper.ts";
import { NativeMicroModule } from "../../core/micro-module.native.ts";
import type { MicroModule } from "../../core/micro-module.ts";
import { $ConnectResult, connectMicroModules } from "../../core/nativeConnect.ts";
import type { $DWEB_DEEPLINK, $MMID } from "../../core/types.ts";
import { mapHelper } from "../../helper/mapHelper.ts";
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

  query(mmid: $MMID) {
    return this.dnsNN.query(mmid);
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
  mmid = "dns.sys.dweb" as const;
  override dweb_deeplinks = ["dweb:open"] as $DWEB_DEEPLINK[];
  private apps = new Map<$MMID, MicroModule>();

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

    this.onFetch(
      async (event) => {
        if (event.url.pathname === "/open") {
          const { app_id } = query_appId(event.searchParams);
          await this.open(app_id);
          return Response.json(true);
        }
      },
      async (event) => {
        if (event.url.pathname === "/close") {
          const { app_id } = query_appId(event.searchParams);
          return Response.json(await this.close(app_id as $MMID));
        }
      },
      /// deeplink
      async (event) => {
        if (event.url.protocol === "dweb:" && event.url.pathname.startsWith("open/")) {
          const app_id = event.url.pathname.replace("open/", "");
          await this.open(app_id as $MMID);
          return Response.json(true);
        }
      }
    ).internalServerError();

    this.onAfterShutdown(
      nativeFetchAdaptersManager.append(async (fromMM, parsedUrl, requestInit) => {
        if (parsedUrl.protocol === "file:" && parsedUrl.hostname.endsWith(".dweb")) {
          const mmid = parsedUrl.hostname as $MMID;
          const reason_request = buildRequestX(parsedUrl.href, requestInit);
          const [ipc] = await this[connectTo_symbol](fromMM, mmid, reason_request);
          const ipc_response = await ipc.request(reason_request.url, reason_request);
          return ipc_response.toResponse(parsedUrl.href);
        }
      })
    );

    //#region 启动引导程序
    await this.open(`boot.sys.dweb`);
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
      const buildReqUrl = () => {
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
      let _req_url: undefined | URL;
      const getReqUrl = () => (_req_url ??= buildReqUrl());

      /// 查询匹配deeplink的程序
      for (const app of this.apps.values()) {
        if (undefined !== app.dweb_deeplinks.find((dl) => dl.startsWith(dweb_deeplink))) {
          const req = buildRequestX(getReqUrl());
          const [ipc] = await context.dns.connect(app.mmid, req);
          const ipc_req_init = await $normalizeRequestInitAsIpcRequestArgs(req);
          /// 发送请求
          await ipc.request(req.url, ipc_req_init);
        }
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
    this.apps.set(mm.mmid, mm);
  }

  /** 卸载应用 */
  uninstall(mmid: $MMID) {
    return this.apps.delete(mmid);
  }

  /** 查询应用 */
  // deno-lint-ignore require-await
  async query(mmid: $MMID) {
    return this.apps.get(mmid);
  }

  readonly running_apps = new Map<$MMID, Promise<MicroModule>>();
  /** 打开应用 */
  async open(mmid: $MMID) {
    const app = await mapHelper.getOrPut(this.running_apps, mmid, async () => {
      const mm = await this.query(mmid);
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
