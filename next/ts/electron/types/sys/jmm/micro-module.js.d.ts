import type { $BootstrapContext } from "../../core/bootstrapContext.js";
import { MicroModule } from "../../core/micro-module.js";
import type { $IpcSupportProtocols } from "../../helper/types.js";
import type { JmmMetadata } from "./JmmMetadata.js";
/**
 * 所有的js程序都只有这么一个动态的构造器
 */
export declare class JsMicroModule extends MicroModule {
    /**
     * js程序是动态外挂的
     * 所以需要传入一份配置信息
     */
    readonly metadata: JmmMetadata;
    readonly ipc_support_protocols: $IpcSupportProtocols;
    constructor(
    /**
     * js程序是动态外挂的
     * 所以需要传入一份配置信息
     */
    metadata: JmmMetadata);
    get mmid(): `${string}.dweb`;
    /**
     * 和 dweb 的 port 一样，pid 是我们自己定义的，它跟我们的 mmid 关联在一起
     * 所以不会和其它程序所使用的 pid 冲突
     */
    private _process_id?;
    /**
     * 一个 jsMM 可能连接多个模块
     */
    private _remoteIpcs;
    private _workerIpc;
    private _connecting_ipcs;
    /** 每个 JMM 启动都要依赖于某一个js */
    _bootstrap(context: $BootstrapContext): Promise<void>;
    _shutdown(): void;
}
