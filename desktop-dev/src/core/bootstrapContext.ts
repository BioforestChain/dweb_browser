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
  /**
   * 打开应用，如果应用不存在，或者因某种原因（程序错误、或者被限制）启动失败，会返回 false
   * @param mmid
   */
  open(mmid: $MMID): Promise<boolean>;
  /** 关闭应用，如果应用不存在，或者用户拒绝关闭、或者因为某种原因（程序错误、或者被限制），会返回false */
  close(mmid: $MMID): Promise<boolean>;
  restart(mmid: $MMID): void;
}
