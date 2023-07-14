import type { $PromiseMaybe } from "./helper/types.ts";
import type { MicroModule } from "./micro-module.ts";
import type { $ConnectResult } from "./nativeConnect.ts";
import type { $MMID } from "./types.ts";
export interface $BootstrapContext {
  dns: $DnsMicroModule;
}
export interface $DnsMicroModule {
  install(mm: MicroModule): void;
  uninstall(mm: $MMID): Promise<boolean>;
  connect(mmid: $MMID, reason?: Request): $PromiseMaybe<$ConnectResult>;
  query(mmid: $MMID): Promise<MicroModule | undefined>;
  open(mmid: $MMID): Promise<boolean>;
  close(mmid: $MMID): Promise<boolean>;
  restart(mmid: $MMID): void;
}
