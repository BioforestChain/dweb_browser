import { NativeMicroModule } from "../../core/micro-module.native.js";
import { $ConnectResult } from "../../core/nativeConnect.js";
import type { $BootstrapContext } from "../../core/bootstrapContext.js";
import type { MicroModule } from "../../core/micro-module.js";
import type { $MMID } from "../../helper/types.js";
declare const connectTo_symbol: unique symbol;
/** DNS 服务，内核！
 * 整个系统都围绕这个 DNS 服务来展开互联
 */
export declare class DnsNMM extends NativeMicroModule {
    mmid: "dns.sys.dweb";
    private apps;
    bootstrap(ctx?: $BootstrapContext): Promise<void>;
    bootstrapMicroModule(fromMM: MicroModule): Promise<void>;
    private mmConnectsMap;
    /**
     * 创建通过 MessageChannel 实现同行的 ipc
     * @param fromMM
     * @param toMmid
     * @param reason
     * @returns
     */
    [connectTo_symbol](fromMM: MicroModule, toMmid: $MMID, reason: Request): Promise<$ConnectResult>;
    _bootstrap(): Promise<MicroModule>;
    _shutdown(): Promise<void>;
    /** 安装应用 */
    install(mm: MicroModule): void;
    /** 卸载应用 */
    uninstall(mm: MicroModule): void;
    /** 查询应用 */
    query(mmid: $MMID): Promise<MicroModule | undefined>;
    private running_apps;
    /** 打开应用 */
    open(mmid: $MMID): Promise<MicroModule>;
    /** 关闭应用 */
    close(mmid: $MMID): Promise<0 | 1 | -1>;
}
export {};
