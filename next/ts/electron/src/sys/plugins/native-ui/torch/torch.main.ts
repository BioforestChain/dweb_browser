import { NativeMicroModule } from "../../../../core/micro-module.native.js";
import { log } from "../../../../helper/devtools.js"
import {toggleTorch, torchState } from "./handlers.js"
 
export class TorchNMM extends NativeMicroModule {
  mmid = "torch.nativeui.sys.dweb" as const;

  _bootstrap = async (context: any) => {
    log.green(`[${this.mmid} _bootstrap]`)
     
    this.registerCommonIpcOnMessageHandler({
      pathname: "/toggleTorch",
      matchMode: "full",
      input: {},
      output: "object",
      handler: toggleTorch.bind(this)
    });    
    
    this.registerCommonIpcOnMessageHandler({
      pathname: "/torchState",
      matchMode: "full",
      input: {},
      output: "object",
      handler: torchState.bind(this)
    });
  }

  _shutdown = async () => {

  }
}