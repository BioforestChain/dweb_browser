import { NativeMicroModule } from "../../../../core/micro-module.native.js";
import { log } from "../../../../helper/devtools.js"
import { show } from "./handlers.js";
export class ToastNMM extends NativeMicroModule {
  // 
  mmid = "toast.sys.dweb" as const;
  _bootstrap = async (context: any) => {
    log.green(`[${this.mmid} _bootstrap]`)
    this.registerCommonIpcOnMessageHandler({
      pathname: "/show",
      matchMode: "full",
      input: {},
      output: "object",
      handler: show.bind(this)
    }); 
  }

  _shutdown = async () => {

  }
}