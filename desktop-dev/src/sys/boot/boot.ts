import { NativeMicroModule } from "../../core/micro-module.native.ts";
import type { $MMID } from "../../helper/types.ts";

export class BootNMM extends NativeMicroModule {
  constructor(private initMmids?: Iterable<$MMID>) {
    super();
    this.registeredMmids = new Set<$MMID>(this.initMmids);
  }
  mmid = "boot.sys.dweb" as const;
  // private registeredMmids = new Set<$MMID>(["desktop.sys.dweb"]); // 被优化
  private readonly registeredMmids: Set<$MMID>;
  async _bootstrap() {
    this.registerCommonIpcOnMessageHandler({
      pathname: "/register",
      matchMode: "full",
      input: {},
      output: "boolean",
      handler: async (args, ipc) => {
        return await this.register(ipc.remote.mmid);
      },
    });
    this.registerCommonIpcOnMessageHandler({
      pathname: "/unregister",
      matchMode: "full",
      input: {},
      output: "boolean",
      handler: async (args, ipc) => {
        return await this.unregister(ipc.remote.mmid);
      },
    });

    /// 开始启动开机项
    for (const mmid of this.registeredMmids) {
      /// TODO 这里应该使用总线进行通讯，而不是拿到core直接调用。在未来分布式系统中，core模块可能是远程模块
      await this.nativeFetch(`file://dns.sys.dweb/open?app_id=${mmid}`);
    }
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
  // $Routers: {
  //    "/register": IO<mmid, boolean>;
  //    "/unregister": IO<mmid, boolean>;
  // };
}
