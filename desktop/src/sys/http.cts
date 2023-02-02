import { NativeMicroModule } from "../core/micro-module.native.cjs";
import { $MMID } from "../core/types.cjs";

export class HttpNMM extends NativeMicroModule {
  mmid = "http.sys.dweb" as const;
  private listenMap = new Map<
    /* ipc-id */ number,
    Map</* port */ number, /* domain */ string>
  >();
  async _bootstrap() {
    this.registerCommonIpcOnMessageHanlder({
      pathname: "/listen",
      matchMode: "full",
      input: { port: "mmid" },
      output: "boolean",
      hanlder: async (args) => {
        return await this.register(args.app_id);
      },
    });
    this.registerCommonIpcOnMessageHanlder({
      pathname: "/unregister",
      matchMode: "full",
      input: { app_id: "mmid" },
      output: "boolean",
      hanlder: async (args) => {
        return await this.unregister(args.app_id);
      },
    });
  }
  _shutdown() {}
  register(mmid: $MMID) {
    /// TODO 这里应该有用户授权，允许开机启动
    this.registeredMmids.add(mmid);
    return true;
  }
  unregister(mmid: $MMID) {
    /// TODO 这里应该有用户授权，取消开机启动
    return this.registeredMmids.delete(mmid);
  }
  // $Routers: {
  //    "/register": IO<mmid, boolean>;
  //    "/unregister": IO<mmid, boolean>;
  // };
}
