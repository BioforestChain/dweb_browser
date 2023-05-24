import { NativeMicroModule } from "../../../../core/micro-module.native.js";
export declare class ToastNMM extends NativeMicroModule {
    mmid: "toast.sys.dweb";
    _bootstrap: (context: any) => Promise<void>;
    _shutdown: () => Promise<void>;
}
