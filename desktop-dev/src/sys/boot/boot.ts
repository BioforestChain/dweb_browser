import { green } from "colors";
import type { $BootstrapContext } from "../../core/bootstrapContext.ts";
import { MICRO_MODULE_CATEGORY } from "../../core/helper/category.const.ts";
import { NativeMicroModule } from "../../core/micro-module.native.ts";
import type { $MMID } from "../../core/types.ts";

export class BootNMM extends NativeMicroModule {
  constructor(private initMmids?: Iterable<$MMID>) {
    super();
    this.registeredMmids = new Set<$MMID>(this.initMmids);
  }
  mmid = "boot.sys.dweb" as const;
  name = "Boot Management";
  override short_name = "Boot";
  override categories = [MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Hub_Service];
  private readonly registeredMmids: Set<$MMID>;
  async _bootstrap(context: $BootstrapContext) {
    this.registerCommonIpcOnMessageHandler({
      pathname: "/register",
      matchMode: "full",
      input: {},
      output: "boolean",
      handler: (args, ipc) => {
        return this.register(ipc.remote.mmid);
      },
    });
    this.registerCommonIpcOnMessageHandler({
      pathname: "/unregister",
      matchMode: "full",
      input: {},
      output: "boolean",
      handler: (args, ipc) => {
        return this.unregister(ipc.remote.mmid);
      },
    });

    /// 基于activity事件来启动开机项
    this.onActivity(async (event, ipc) => {
      // 只响应 dns 模块的激活事件
      if (ipc.remote.mmid !== "dns.std.dweb") {
        return;
      }
      for (const mmid of this.registeredMmids) {
        console.always("开机器启动项", green(mmid));
        const ipc = await this.connect(mmid);
        ipc?.postMessage(event);
      }
    });
  }
  _shutdown() {}
  private register(mmid: $MMID) {
    /// TODO 这里应该有用户授权，允许开机启动
    this.registeredMmids.add(mmid);
    return true;
  }
  private unregister(mmid: $MMID) {
    /// TODO 这里应该有用户授权，取消开机启动
    return this.registeredMmids.delete(mmid);
  }
}
