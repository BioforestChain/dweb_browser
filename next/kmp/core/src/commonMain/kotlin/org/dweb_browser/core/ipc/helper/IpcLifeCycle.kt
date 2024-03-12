package org.dweb_browser.core.ipc.helper

import kotlinx.serialization.Serializable

/**
 * Ipc生命周期控制
 */
@Serializable
class IpcLifeCycle(
  val state: IPC_STATE,
  val encoding: List<IPC_DATA_ENCODING> = listOf()
) : IpcMessage(IPC_MESSAGE_TYPE.LIFE_CYCLE) {
  override fun toString() = "IpcLifeCycle(state=$state)"

  companion object {
    fun opening() = IpcLifeCycle(IPC_STATE.OPENING)
    fun open() = IpcLifeCycle(IPC_STATE.OPEN)

    fun closing() = IpcLifeCycle(IPC_STATE.OPENING)
    fun close() = IpcLifeCycle(IPC_STATE.CLOSED)
  }

}

