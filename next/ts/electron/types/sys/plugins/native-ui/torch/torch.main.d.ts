import { NativeMicroModule } from "../../../../core/micro-module.native.js";
export declare class TorchNMM extends NativeMicroModule {
    mmid: "torch.nativeui.sys.dweb";
    _bootstrap: (context: any) => Promise<void>;
    _shutdown: () => Promise<void>;
}
