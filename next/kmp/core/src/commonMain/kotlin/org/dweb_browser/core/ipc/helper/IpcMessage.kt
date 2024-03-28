package org.dweb_browser.core.ipc.helper

import kotlinx.serialization.Serializable

/**
 * 总的消息类型抽象
 */
@Serializable
sealed class IpcMessage(val type: IPC_MESSAGE_TYPE)

interface IpcStream {
  val stream_id: String
}

/**分发消息到各个ipc的监听时使用*/
@Serializable
data class IpcPoolPack(val pid: Int, val ipcMessage: IpcMessage)

@Serializable
data class IpcPoolPackString(val pid: Int, val ipcMessage: String)

/**消息传递时包裹pool消息📦*/
@Serializable
class PackIpcMessage(val pid: Int, val messageByteArray: ByteArray) {
  override fun toString(): String {
    return "PackIpcMessage(pid=$pid,messageByteArray:${messageByteArray.size})"
  }
}
