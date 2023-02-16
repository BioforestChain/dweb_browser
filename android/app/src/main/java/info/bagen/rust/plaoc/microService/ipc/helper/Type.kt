package info.bagen.rust.plaoc.microService.ipc.helper

import info.bagen.rust.plaoc.microService.Mmid
import info.bagen.rust.plaoc.microService.ipc.Ipc
import info.bagen.rust.plaoc.microService.ipc.IpcRequest
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpResponse

typealias IpcMessage = IpcRequest

typealias OnIpcMessage = (
/// 这里只会有两种类型的数据
message: IpcMessage,
ipc: Ipc
) ->Any

typealias OnIpcRequestMessage = (
/// 这里只会有两种类型的数据
message: IpcRequest,
ipc: Ipc
) -> Any;

enum class IPC_DATA_TYPE() {
    // /** 特殊位：结束符 */
    // END = 1,
    /** 类型：请求 */
    REQUEST,
    /** 类型：相应 */
    RESPONSE,
    /** 类型：流数据，发送方 */
    STREAM_DATA,
    /** 类型：流拉取，请求方 */
    STREAM_PULL,
    /** 类型：流关闭，发送方
     * 可能是发送完成了，也有可能是被中断了
     */
    STREAM_END,
    /** 类型：流中断，请求方 */
    STREAM_ABORT,
}


enum class IPC_ROLE(type:String) {
    SERVER("server"),
    CLIENT("client"),
}

 class  TMicroModule(
)  {
    val mmid: Mmid? = null
    fun fetch( input: HttpRequest ) : HttpResponse? {
        return null
    }

 }