import { NativeMicroModule } from "../../../../core/micro-module.native.ts";
import { log } from "../../../../helper/devtools.ts"
import { show } from "./handlers.ts";
export class ToastNMM extends NativeMicroModule {
  mmid = "toast.sys.dweb" as const;
  _bootstrap = () => {
    log.green(`[${this.mmid} _bootstrap]`)
    this.registerCommonIpcOnMessageHandler({
      pathname: "/show",
      matchMode: "full",
      input: {},
      output: "object",
      handler: show.bind(this)
    }); 
  }

  _shutdown = () => {
    throw new Error(`_shutdown 还没有处理`)
  }
}