package info.bagen.rust.plaoc.microService.ipc

import org.http4k.core.Request
import info.bagen.rust.plaoc.microService.helper.Method

data class IpcRequest(
    val req_id: Number,
    val method: Method,
    val url: String,
    val headers: IpcHeaders,
    override val rawBody: RawData,
    override val ipc: Ipc
) : IpcBody(rawBody, ipc), IpcMessage {
    override val type = IPC_DATA_TYPE.REQUEST

    fun asRequest() = Request(method.http4kMethod, url).headers(headers.toList()).body(stream())
}
