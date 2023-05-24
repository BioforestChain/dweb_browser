import { NativeMicroModule } from "../../core/micro-module.native.js";
/**
 * 将指定的js运行在后台的一个管理器，
 * 注意它们共享一个域，所以要么就关闭
 *
 * 功能：
 * 用来创建 woker.js 线程
 * 用来中转  woker.js 同匹配的 JsMicroModule 通信
 */
export declare class JsProcessNMM extends NativeMicroModule {
    mmid: "js.sys.dweb";
    private JS_PROCESS_WORKER_CODE;
    private INTERNAL_PATH;
    _bootstrap(): Promise<void>;
    _shutdown(): Promise<void>;
    private createProcessAndRun;
    private createIpc;
}
