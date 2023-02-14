package info.bagen.rust.plaoc.microService.ipc.helder

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

enum class IPC_DATA_TYPE(type: Number) {
    REQUEST(0),
    RESPONSE(1)
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