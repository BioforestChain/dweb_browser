import {
  normalizeFetchArgs,
  PromiseOut,
  readRequestAsIpcRequest,
} from "../core/helper.cjs";
import { Ipc, IpcRequest, IpcResponse } from "../core/ipc.cjs";
import { MicroModule } from "../core/micro-module.cjs";
import { NativeMicroModule } from "../core/micro-module.native.cjs";
import { $MMID, $PromiseMaybe } from "../core/types.cjs";

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

    //#region 重写 fetch

    const app_mm = this;
    const connects = new WeakMap<
      MicroModule,
      Map<
        $MMID,
        $PromiseMaybe<{
          ipc: Ipc;
          reqresMap: Map<number, PromiseOut<IpcResponse>>;
          allocReqId: () => number;
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
              let req_id = 0;
              const allocReqId = () => req_id++;
              const ipc = await app.connect(from_app);
              const reqresMap = new Map<number, PromiseOut<IpcResponse>>();
              /// 监听回调
              ipc.onMessage((message) => {
                if (message instanceof IpcResponse) {
                  const response_po = reqresMap.get(message.req_id);
                  if (response_po) {
                    reqresMap.delete(message.req_id);
                    response_po.resolve(message);
                  }
                }
              });
              ipc.onClose(() => {
                from_app_ipcs?.delete(mmid);
              });
              return {
                ipc,
                reqresMap,
                allocReqId,
              };
            })();
            from_app_ipcs.set(mmid, ipc_promise);
          }

          return (async () => {
            const { ipc, reqresMap, allocReqId } = await ipc_promise;
            const { body, method, headers } = await readRequestAsIpcRequest(
              args.request_init
            );

            /// 注册回调
            const req_id = allocReqId();
            const response_po = new PromiseOut<IpcResponse>();
            reqresMap.set(req_id, response_po);

            /// 发送
            ipc.postMessage(
              new IpcRequest(req_id, method, parsed_url.href, body, headers)
            );
            const ipc_response = await response_po.promise;
            return new Response(ipc_response.body, {
              headers: ipc_response.headers,
              status: ipc_response.statusCode,
            });
          })();
        }
      }

      return native_fetch(url, init);
    };
    //#endregion

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
