import type {
  $BootstrapContext,
  $DnsMicroModule,
} from "../../core/bootstrapContext.cjs";
import type { Ipc } from "../../core/ipc/ipc.cjs";
import { IpcHeaders } from "../../core/ipc/IpcHeaders.cjs";
import { IpcResponse } from "../../core/ipc/IpcResponse.cjs";
import type { MicroModule } from "../../core/micro-module.cjs";
import { NativeMicroModule } from "../../core/micro-module.native.cjs";
import { $readRequestAsIpcRequest } from "../../helper/$readRequestAsIpcRequest.cjs";
import type { $MMID, $PromiseMaybe } from "../../helper/types.cjs";
import { nativeFetchAdaptersManager } from "./nativeFetch.cjs";

class MyDnsMicroModule implements $DnsMicroModule {
  constructor(private dnsNN: DnsNMM, private fromMM: MicroModule) {}
  install(mm: MicroModule): void {
    this.dnsNN.install(mm);
  }
  uninstall(mm: MicroModule): void {
    this.dnsNN.uninstall(mm);
  }
  connect(mmid: any) {
    return this.dnsNN[connectTo_symbol](this.dnsNN, mmid);
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
  private apps = new Map<$MMID, MicroModule>();

  bootstrapMicroModule(fromMM: MicroModule) {
    return fromMM.bootstrap(
      new MyBootstrapContext(new MyDnsMicroModule(this, fromMM))
    );
  }

  // 拦截 nativeFetch
  private connects = new WeakMap<
    MicroModule,
    Map<
      $MMID,
      $PromiseMaybe<{
        ipc: Ipc;
      }>
    >
  >();
  async [connectTo_symbol](fromMM: MicroModule, toMmid: $MMID) {
    /// 拦截到了，走自定义总线
    let fromMM_ipcs = this.connects.get(fromMM);
    if (fromMM_ipcs === undefined) {
      fromMM_ipcs = new Map();
      this.connects.set(fromMM, fromMM_ipcs);
    }

    /// 与指定应用建立通讯
    let ipc_promise = fromMM_ipcs.get(toMmid);
    if (ipc_promise === undefined) {
      /// 初始化互联
      fromMM_ipcs.set(
        toMmid,
        (ipc_promise = (async () => {
          const toMM = await this.open(toMmid);
          const ipc = await toMM.beConnect(fromMM);
          // 监听生命周期 释放引用
          ipc.onClose(() => {
            fromMM_ipcs?.delete(toMmid);
          });
          return {
            ipc,
          };
        })())
      );
    }

    const { ipc } = await ipc_promise;
    return ipc;
  }

  override _bootstrap() {
    this.install(this);
    this.running_apps.set(this.mmid, this);

    this.registerCommonIpcOnMessageHandler({
      pathname: "/open",
      matchMode: "full",
      input: { app_id: "mmid" },
      output: "boolean",
      handler: async (args, client_ipc, request) => {
        /// TODO 询问用户是否授权该行为
        const app = await this.open(args.app_id);
        return IpcResponse.fromJson(
          request.req_id,
          200,
          new IpcHeaders({
            "Content-Type": "application/json; charset=UTF-8",
          }),
          JSON.stringify(app),
          client_ipc
        );
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

    this._after_shutdown_signal.listen(
      nativeFetchAdaptersManager.append(
        async (fromMM, parsedUrl, requestInit) => {
          if (
            parsedUrl.protocol === "file:" &&
            parsedUrl.hostname.endsWith(".dweb")
          ) {
            const mmid = parsedUrl.hostname as $MMID;
            const ipc = await this[connectTo_symbol](fromMM, mmid);
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
      await this.bootstrapMicroModule(mm);
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
