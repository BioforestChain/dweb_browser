package org.dweb_browser.core.ipc.helper

import kotlinx.serialization.Serializable

/**
 * 总的消息类型抽象
 */
@Serializable
sealed interface IpcRawMessage {
}

sealed interface IpcMessage {
}


const val IPC_MESSAGE_TYPE_LIFECYCLE = "life"
const val IPC_MESSAGE_TYPE_REQUEST = "req"
const val IPC_MESSAGE_TYPE_RESPONSE = "res"
const val IPC_MESSAGE_TYPE_STREAM_DATA = "data"
const val IPC_MESSAGE_TYPE_STREAM_PULL = "pull"
const val IPC_MESSAGE_TYPE_STREAM_PAUSED = "pause"
const val IPC_MESSAGE_TYPE_STREAM_END = "end"
const val IPC_MESSAGE_TYPE_STREAM_ABORT = "abo"
const val IPC_MESSAGE_TYPE_EVENT = "event"
const val IPC_MESSAGE_TYPE_ERROR = "err"
const val IPC_MESSAGE_TYPE_FORK = "fork"

@Serializable
sealed interface IpcStream {
  val stream_id: String
}

