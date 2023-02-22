import type { MicroModule } from "../../core/micro-module.cjs";
import { NativeMicroModule } from "../../core/micro-module.native.cjs";
import type { $MMID } from "../../helper/types.cjs";
import { hookFetch } from "./hookFetch.cjs";
import { JsMicroModule } from "../../sys/micro-module.js.cjs"
import { resolveToRootFile } from "../../helper/createResolveTo.cjs"
import { IpcResponse } from "../../core/ipc/IpcResponse.cjs"
import { IpcHeaders } from "../../core/ipc/IpcHeaders.cjs";


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
      hanlder: async (arg, client_ipc, request) => {
        /// TODO 动态创建 JsMicroModule
        const _url = new URL(request.url)
        let appId = _url.searchParams.get("app_id")
        if(appId === null) return void 0;
        const mmid = `${appId}` as $MMID
        const appJMM = new JsMicroModule(mmid, {
          
          main_url: resolveToRootFile("bundle/common.worker.js").href,
        } as const);
        this.install(appJMM)
        console.log('动态安装 JMM this.apps: resolveToRootFile("bundle/common.worker.js").href', resolveToRootFile("bundle/common.worker.js").href)
        return IpcResponse.fromText(
          request.req_id,
          200,
          "ok",
          new IpcHeaders({
            "Content-Type": "text/json"
          })
        )
        
      },
    });
    this.registerCommonIpcOnMessageHanlder({
      pathname: "/open",
      matchMode: "full",
      input: { app_id: "mmid" },
      output: "boolean",
      hanlder: async (args) => {
        console.log('dns.cts 启动应用： ', args)
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
    console.log('dns 开始打开应用: ', mmid)
    let app = this.running_apps.get(mmid);
    console.log('app: ', app?.mmid)
    if (app === undefined) {
      const mm = await this.query(mmid);
      if (mm === undefined) {
        throw new Error(`no found app: ${mmid}`);
      }
      this.running_apps.set(mmid, mm);
      // @TODO bootstrap 函数应该是 $singleton 修饰
      console.log('【dns.cts】 开始启动应用', mm.mmid)
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
