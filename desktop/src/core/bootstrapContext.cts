import type { $PromiseMaybe } from "../helper/types.cjs";
import type { MicroModule } from "./micro-module.cjs";
import type { $ConnectResult } from "./nativeConnect.cjs";
export interface $BootstrapContext {
  dns: $DnsMicroModule;
}
export interface $DnsMicroModule {
  install(mm: MicroModule): void;
  uninstall(mm: MicroModule): void;
  connect(mmid: $MMID, reason?: Request): $PromiseMaybe<$ConnectResult>;
}
