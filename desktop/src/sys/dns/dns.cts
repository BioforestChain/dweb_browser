import type {
  $BootstrapContext,
  $DnsMicroModule,
} from "../../core/bootstrapContext.cjs";
import { IpcHeaders } from "../../core/ipc/IpcHeaders.cjs";
import { IpcResponse } from "../../core/ipc/IpcResponse.cjs";
import type { MicroModule } from "../../core/micro-module.cjs";
import { NativeMicroModule } from "../../core/micro-module.native.cjs";
import {
  $ConnectResult,
  connectMicroModules,
} from "../../core/nativeConnect.cjs";
import { $readRequestAsIpcRequest } from "../../helper/$readRequestAsIpcRequest.cjs";
import { mapHelper } from "../../helper/mapHelper.cjs";
import { PromiseOut } from "../../helper/PromiseOut.cjs";
import type { $MMID } from "../../helper/types.cjs";
import { nativeFetchAdaptersManager } from "./nativeFetch.cjs";

class MyDnsMicroModule implements $DnsMicroModule {
  constructor(private dnsNN: DnsNMM, private fromMM: MicroModule) {}
  install(mm: MicroModule): void {
    this.dnsNN.install(mm);
  }
  uninstall(mm: MicroModule): void {
    this.dnsNN.uninstall(mm);
  }
  connect(mmid: $MMID, reason?: Request) {
    return this.dnsNN[connectTo_symbol](
      this.dnsNN,
      mmid,
      reason ?? new Request(`file://${mmid}`)
    );
  }
  // 私有的一对一的连接
  async privateConnect(toMmid: $MMID, reason?: Request){
    return new Promise(async (resolve) => {
      const toMM = await this.dnsNN.open(toMmid);
      const connects = await connectMicroModules(this.fromMM, toMM, reason ? reason : new Request(`file://${toMmid}`));
      resolve(connects)
    })
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
  private mmConnectsMap = new WeakMap<
    MicroModule,
    Map<$MMID, PromiseOut<$ConnectResult>>
  >();
  async [connectTo_symbol](
    fromMM: MicroModule,
    toMmid: $MMID,
    reason: Request
  ) {
    /// 拦截到了，走自定义总线
    // 全部的 connect 都是保存在dns中
    // 多个不同的模块 connect 一个相同模块，这个相同模块如果发送消息，发起连接的不同模块都会受到消息
    const connectsMap = mapHelper.getOrPut(
      this.mmConnectsMap,
      fromMM,
      () => new Map<$MMID, PromiseOut<$ConnectResult>>()
    );

    const po = mapHelper.getOrPut(connectsMap, toMmid, () => {
      const po = new PromiseOut<$ConnectResult>();
      (async () => {
        /// 与指定应用建立通讯
        const toMM = await this.open(toMmid);
        const connects = await connectMicroModules(fromMM, toMM, reason);
        // 监听生命周期 释放引用
        connects[0].onClose(() => {
          connectsMap?.delete(toMmid);
        });
        po.resolve(connects);
      })();
      return po;
    });
    return po.promise;
  }

  override _bootstrap() {
    console.log('[dns.cts _bootstrap]')
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
          // console.log('[dns.cts 适配器执行了] requestInit === ',JSON.stringify(requestInit))
          if (
            parsedUrl.protocol === "file:" &&
            parsedUrl.hostname.endsWith(".dweb")
          ) {
            const mmid = parsedUrl.hostname as $MMID;
            const [ipc] = await this[connectTo_symbol](
              fromMM,
              mmid,
              new Request(parsedUrl, requestInit)
            );
            const ipc_req_init = await $readRequestAsIpcRequest(requestInit);
            // console.log('[dns.cts 适配器执行了] ipc_req_init === ',JSON.stringify(ipc_req_init))
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
