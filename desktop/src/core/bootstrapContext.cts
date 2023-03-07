import type { $PromiseMaybe } from "../helper/types.cjs";
import type { Ipc } from "./ipc/ipc.cjs";
import type { MicroModule } from "./micro-module.cjs";
export interface $BootstrapContext {
  dns: $DnsMicroModule;
}
export interface $DnsMicroModule {
  install(mm: MicroModule): void;
  uninstall(mm: MicroModule): void;
  connect(mmid: $MMID): $PromiseMaybe<Ipc>;
}
