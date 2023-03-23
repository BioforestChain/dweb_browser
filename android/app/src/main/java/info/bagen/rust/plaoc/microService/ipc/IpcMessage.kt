package info.bagen.rust.plaoc.microService.ipc

/**
 * TODO 所有的消息都应该带上 headers？而不仅仅是 request和response
 */
open class IpcMessage(val type: IPC_MESSAGE_TYPE)

interface IpcStream {
    val stream_id: String
}