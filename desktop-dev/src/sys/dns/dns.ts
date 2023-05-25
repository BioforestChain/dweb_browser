import type {
  $BootstrapContext,
  $DnsMicroModule,
} from "../../core/bootstrapContext.ts";
import { IpcHeaders } from "../../core/ipc/IpcHeaders.ts";
import { IpcResponse } from "../../core/ipc/IpcResponse.ts";
import { NativeMicroModule } from "../../core/micro-module.native.ts";
import type { MicroModule } from "../../core/micro-module.ts";
import {
  $ConnectResult,
  connectMicroModules,
} from "../../core/nativeConnect.ts";
import { $readRequestAsIpcRequest } from "../../helper/$readRequestAsIpcRequest.ts";
import { log } from "../../helper/devtools.ts";
import { mapHelper } from "../../helper/mapHelper.ts";
import { PromiseOut } from "../../helper/PromiseOut.ts";
import type { $MMID } from "../../helper/types.ts";
import { nativeFetchAdaptersManager } from "./nativeFetch.ts";
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
      this.fromMM,
      mmid,
      reason ?? new Request(`file://${mmid}`)
    );
  }

  query(mmid: $MMID) {
    return this.dnsNN.query(mmid);
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

  bootstrap(
    ctx: $BootstrapContext = new MyBootstrapContext(
      new MyDnsMicroModule(this, this)
    )
  ) {
    return super.bootstrap(ctx);
  }

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

  /**
   * 创建通过 MessageChannel 实现同行的 ipc
   * @param fromMM
   * @param toMmid
   * @param reason
   * @returns
   */
  async [connectTo_symbol](
    fromMM: MicroModule,
    toMmid: $MMID,
    reason: Request
  ) {
    // v2.0
    // 创建连接
    const fromMMconnectsMap = mapHelper.getOrPut(
      this.mmConnectsMap,
      fromMM,
      () => new Map<$MMID, PromiseOut<$ConnectResult>>()
    );

    const po = mapHelper.getOrPut(fromMMconnectsMap, toMmid, () => {
      const po = new PromiseOut<$ConnectResult>();
      (async () => {
        /// 与指定应用建立通讯
        const toMM = await this.open(toMmid);

        const result = await connectMicroModules(fromMM, toMM, reason);
        const [ipcForFromMM, ipcForToMM] = result;

        // 监听生命周期 释放引用
        ipcForFromMM.onClose(() => {
          fromMMconnectsMap?.delete(toMmid);
        });

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
              toMMconnectsMap?.delete(fromMM.mmid);
            });
            toMMPromise.resolve(result2);
            return toMMPromise;
          });
        }
        po.resolve(result);
      })();
      return po;
    });
    return po.promise;
  }

  override _bootstrap() {
    log.green(`${this.mmid} _bootstrap`);
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
      pathname: "/open_browser",
      matchMode: "full",
      input: { mmid: "mmid", root: "string", entry: "string" },
      output: "boolean",
      handler: async (args, client_ipc, request) => {
        const { JsMicroModule } = await import("../jmm/micro-module.js.ts");
        const { JmmMetadata } = await import("../jmm/JmmMetadata.ts");
        const metadata = new JmmMetadata({
          id: args.mmid,
          server: { root: args.root, entry: args.entry },
        });

        console.log("metadata: ", metadata);

        // 实例化
        // 安装
        this.install(new JsMicroModule(metadata));

        /// TODO 询问用户是否授权该行为
        const app = await this.open(args.mmid);
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
      output: "number",
      handler: async (args) => {
        /// TODO 关闭应用首先要确保该应用的 parentProcessId 在 processTree 中
        const n = await this.close(args.app_id);
        const result = await this.nativeFetch(
          `file://mwebview.sys.dweb/close/focused_window`
        );
        return n;
      },
    });

    // 检查工具 提供查询 mmConnectsMap  的结果
    this.registerCommonIpcOnMessageHandler({
      pathname: "/query/mm_connects_map",
      matchMode: "full",
      input: { app_id: "mmid" },
      output: "object",
      handler: async (args) => {
        const mm = await this.query(args.app_id);
        if (mm === undefined) {
          throw new Error(`mm === undefined`);
        }
        const _map = this.mmConnectsMap.get(mm);
        return {};
      },
    });

    this.registerCommonIpcOnMessageHandler({
      pathname: "/restart",
      matchMode: "full",
      input: { app_id: "mmid" },
      output: "boolean",
      handler: async (args, ipc, request) => {
        // 需要停止匹配的 jsMicroModule
        const mm = await this.query(args.app_id);
        if (mm === undefined) return false;
        this.close(args.app_id);
        // 关闭当前window对象
        const result = await this.nativeFetch(
          `file://mwebview.sys.dweb/close/focused_window`
        );

        this.install(mm);
        this.open(args.app_id);
        return true;
      },
    });

    this._after_shutdown_signal.listen(
      nativeFetchAdaptersManager.append(
        async (fromMM, parsedUrl, requestInit) => {
          // 测试代码
          // Reflect.set(requestInit, "duplex", "half")
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
      // 关闭失败没有匹配的 microModule 运行
      return -1;
    }
    try {
      this.running_apps.delete(mmid);
      await app.shutdown();
      this.uninstall(app);
      // 关闭成功
      return 0;
    } catch {
      // 关闭失败
      return 1;
    }
  }
}
