import { NativeMicroModule } from "../../../../core/micro-module.native.ts";
import { log } from "../../../../helper/devtools.ts";
import { toggleTorch, torchState } from "./handlers.ts";

export class TorchNMM extends NativeMicroModule {
  mmid = "torch.nativeui.sys.dweb" as const;

  _bootstrap = () => {
    log.green(`[${this.mmid} _bootstrap]`);

    this.registerCommonIpcOnMessageHandler({
      pathname: "/toggleTorch",
      matchMode: "full",
      input: {},
      output: "object",
      handler: toggleTorch.bind(this),
    });

    this.registerCommonIpcOnMessageHandler({
      pathname: "/torchState",
      matchMode: "full",
      input: {},
      output: "object",
      handler: torchState.bind(this),
    });
  };

  _shutdown = () => {
    throw new Error("[error:]还没有写关闭程序");
  };
}
