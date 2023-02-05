import { normalizeFetchArgs } from "../helper/normalizeFetchArgs.cjs";
import { $readRequestAsIpcRequest } from "../helper/$readRequestAsIpcRequest.cjs";
import type { Ipc } from "../core/ipc/index.cjs";
import { MicroModule } from "../core/micro-module.cjs";
import { NativeMicroModule } from "../core/micro-module.native.cjs";
import type { $MMID, $PromiseMaybe } from "../helper/types.cjs";

/** DNS 服务，内核！
 * 整个系统都围绕这个 DNS 服务来展开互联
 */
export class DnsNMM extends NativeMicroModule {
  mmid = "dns.sys.dweb" as const;
  private apps = new Map<$MMID, MicroModule>();
  override _bootstrap() {
    // const parseArgs_open = $parseRequestToParams({ app_id: "mmid" });
    // const parseArgs_close = $parseRequestToParams({ app_id: "mmid" });
    this.install(this);
    this.running_apps.set(this.mmid, this);

    this.registerCommonIpcOnMessageHanlder({
      pathname: "/install-js",
      matchMode: "full",
      input: {},
      output: "void",
      hanlder: () => {
        /// TODO 动态创建 JsMicroModule
      },
    });
    this.registerCommonIpcOnMessageHanlder({
      pathname: "/open",
      matchMode: "full",
      input: { app_id: "mmid" },
      output: "boolean",
      hanlder: async (args) => {
        /// TODO 询问用户是否授权该行为
        await this.open(args.app_id);
        return true;
      },
    });
    this.registerCommonIpcOnMessageHanlder({
      pathname: "/close",
      matchMode: "full",
      input: { app_id: "mmid" },
      output: "boolean",
      hanlder: async (args) => {
        /// TODO 关闭应用首先要确保该应用的 parentProcessId 在 processTree 中
        await this.close(args.app_id);
        return true;
      },
    });

    // 重写 fetch
    hookFetch(this);

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
      await mm.bootstrap();
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

/**
 * 重写全局 fetch 函数，使得支持 fetch("file://*.dweb/*")
 */
const hookFetch = (app_mm: DnsNMM) => {
  const connects = new WeakMap<
    MicroModule,
    Map<
      $MMID,
      $PromiseMaybe<{
        ipc: Ipc;
      }>
    >
  >();
  const native_fetch = globalThis.fetch;
  globalThis.fetch = function fetch(
    this: unknown,
    url: RequestInfo | URL,
    init?: RequestInit
  ) {
    /// 如果上下文是 MicroModule，那么进入特殊的解析模式
    if (this instanceof MicroModule) {
      const from_app = this;
      const args = normalizeFetchArgs(url, init);
      const { parsed_url } = args;
      if (
        parsed_url.protocol === "file:" &&
        parsed_url.hostname.endsWith(".dweb")
      ) {
        const mmid = parsed_url.hostname as $MMID;
        /// 拦截到了，走自定义总线
        let from_app_ipcs = connects.get(from_app);
        if (from_app_ipcs === undefined) {
          from_app_ipcs = new Map();
          connects.set(from_app, from_app_ipcs);
        }

        /// 与指定应用建立通讯
        let ipc_promise = from_app_ipcs.get(mmid);
        if (ipc_promise === undefined) {
          /// 初始化互联
          ipc_promise = (async () => {
            const app = await app_mm.open(parsed_url.hostname as $MMID);
            const ipc = await app.connect(from_app);
            // 监听生命周期 释放引用
            ipc.onClose(() => {
              from_app_ipcs?.delete(mmid);
            });
            return {
              ipc,
            };
          })();
          from_app_ipcs.set(mmid, ipc_promise);
        }

        return (async () => {
          const { ipc } = await ipc_promise;
          const ipc_req_init = await $readRequestAsIpcRequest(args.request_init);
          const ipc_response = await ipc.request(parsed_url.href, ipc_req_init);

          return ipc_response.asResponse();
        })();
      }
    }

    return native_fetch(url, init);
  };
};
