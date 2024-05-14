import type { $PromiseMaybe } from "@dweb-browser/helper/$PromiseMaybe.ts";
import type { IpcResponse } from "./ipc/ipc-message/IpcResponse.ts";
import type { Ipc } from "./ipc/ipc.ts";
import type { MICRO_MODULE_CATEGORY } from "./type/category.const.ts";
import type { $MMID, $MicroModuleManifest } from "./types.ts";

export interface $BootstrapContext {
  dns: $DnsMicroModule;
}
export interface $DnsMicroModule {
  install(mm: `${string}.dweb`): Promise<void>;
  /** 卸载由自己发起 install 的 MicroModule */
  uninstall(mm: $MMID): Promise<boolean>;
  /**
   * 打开应用并与之建立链接
   * @param mmid
   * @param reason
   */
  connect(mmid: $MMID, reason?: Request): $PromiseMaybe<Ipc>;
  query(mmid: $MMID): Promise<$MicroModuleManifest | undefined>;
  queryDeeplink(deeplinkHref: string): Promise<$MicroModuleManifest | undefined>;
  /**
   * 根据类目搜索模块
   * > 这里暂时不需要支持复合搜索，未来如果有需要另外开接口
   * @param category
   */
  search(category: MICRO_MODULE_CATEGORY): Promise<$MicroModuleManifest[]>;
  /**
   * 打开应用，如果应用不存在，或者因某种原因（程序错误、或者被限制）启动失败，会返回 false
   * 返回true，说明应用已经在运行
   * @param mmid
   */
  open(mmid: $MMID): Promise<boolean>;
  /** 关闭应用，如果应用不存在，或者用户拒绝关闭、或者因为某种原因（程序错误、或者被限制），会返回false
   * 返回true，说明应用已经停止运行
   */
  close(mmid: $MMID): Promise<boolean>;
  restart(mmid: $MMID): void;
}
