import { NativeMicroModule } from "../../../../core/micro-module.native.js";
import type { Ipc } from "../../../../core/ipc/ipc.js";
export declare class SafeAreaNMM extends NativeMicroModule {
    mmid: "safe-area.nativeui.sys.dweb";
    httpIpc: Ipc | undefined;
    observes: Map<string /** headers.host */, Ipc>;
    observesState: Map<string /**headers.host */, boolean>;
    encoder: TextEncoder;
    _bootstrap: (context: any) => Promise<void>;
    _onConnect(ipc: Ipc): void;
    _shutdown: () => Promise<void>;
}
