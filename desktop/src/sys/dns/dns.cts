import type {
  $BootstrapContext,
  $DnsMicroModule,
} from "../../core/bootstrapContext.cjs";
import type { Ipc } from "../../core/ipc/ipc.cjs";
import type { MicroModule } from "../../core/micro-module.cjs";
import { NativeMicroModule } from "../../core/micro-module.native.cjs";
import { $readRequestAsIpcRequest } from "../../helper/$readRequestAsIpcRequest.cjs";
import type { $MMID, $PromiseMaybe } from "../../helper/types.cjs";
import { nativeFetchAdaptersManager } from "./nativeFetch.cjs";

/** DNS 服务，内核！
 * 整个系统都围绕这个 DNS 服务来展开互联
 */
export class DnsNMM extends NativeMicroModule implements $DnsMicroModule {
  mmid = "dns.sys.dweb" as const;
  private apps = new Map<$MMID, MicroModule>();
  private context: $BootstrapContext = {
    dns: this,
  };

  override _bootstrap() {
    this.install(this);
    this.running_apps.set(this.mmid, this);

    this.registerCommonIpcOnMessageHandler({
      pathname: "/open",
      matchMode: "full",
      input: { app_id: "mmid" },
      output: "boolean",
      handler: async (args) => {
        /// TODO 询问用户是否授权该行为
        await this.open(args.app_id);
        return true;
      },
    });

    this.registerCommonIpcOnMessageHandler({
      pathname: "/close",
      matchMode: "full",
      input: { app_id: "mmid" },
      output: "boolean",
      handler: async (args) => {
        /// TODO 关闭应用首先要确保该应用的 parentProcessId 在 processTree 中
        await this.close(args.app_id);
        return true;
      },
    });

    // 拦截 nativeFetch
    const connects = new WeakMap<
      MicroModule,
      Map<
        $MMID,
        $PromiseMaybe<{
          ipc: Ipc;
        }>
      >
    >();
    this._after_shutdown_signal.listen(
      nativeFetchAdaptersManager.append(
        async (fromMM, parsedUrl, requestInit) => {
          if (
            parsedUrl.protocol === "file:" &&
            parsedUrl.hostname.endsWith(".dweb")
          ) {
            const mmid = parsedUrl.hostname as $MMID;
            /// 拦截到了，走自定义总线
            let fromMM_ipcs = connects.get(fromMM);
            if (fromMM_ipcs === undefined) {
              fromMM_ipcs = new Map();
              connects.set(fromMM, fromMM_ipcs);
            }

            /// 与指定应用建立通讯
            let ipc_promise = fromMM_ipcs.get(mmid);
            if (ipc_promise === undefined) {
              /// 初始化互联
              fromMM_ipcs.set(
                mmid,
                (ipc_promise = (async () => {
                  const app = await this.open(parsedUrl.hostname as $MMID);
                  const ipc = await app.connect(fromMM);
                  // 监听生命周期 释放引用
                  ipc.onClose(() => {
                    fromMM_ipcs?.delete(mmid);
                  });
                  return {
                    ipc,
                  };
                })())
              );
            }

            const { ipc } = await ipc_promise;
            const ipc_req_init = await $readRequestAsIpcRequest(requestInit);
            const ipc_response = await ipc.request(
              parsedUrl.href,
              ipc_req_init
            );

            return ipc_response.toResponse(parsedUrl.href);
          }
        }
      )
    );

    //#region 启动引导程序
    return this.open(`boot.sys.dweb`);
    //#endregion
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
  uninstall(mm: MicroModule) {
    this.apps.delete(mm.mmid);
  }
  /** 查询应用 */
  async query(mmid: $MMID) {
    return this.apps.get(mmid);
  }
  private running_apps = new Map<$MMID, MicroModule>();
  /** 打开应用 */
  async open(mmid: $MMID) {
    let app = this.running_apps.get(mmid);
    if (app === undefined) {
      const mm = await this.query(mmid);
      if (mm === undefined) {
        throw new Error(`no found app: ${mmid}`);
      }
      this.running_apps.set(mmid, mm);
      // @TODO bootstrap 函数应该是 $singleton 修饰
      await mm.bootstrap(this.context);
      app = mm;
    }

    return app;
  }
  /** 关闭应用 */
  async close(mmid: $MMID) {
    const app = this.running_apps.get(mmid);
    if (app === undefined) {
      return -1;
    }
    try {
      this.running_apps.delete(mmid);
      await app.shutdown();
      return 0;
    } catch {
      return 1;
    }
  }
}
