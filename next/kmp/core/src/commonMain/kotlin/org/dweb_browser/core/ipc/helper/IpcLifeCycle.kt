package org.dweb_browser.core.ipc.helper

class IpcLifeCycle(
  private val channelId: Int,
  val state: IPC_STATE
) : IpcMessage(IPC_MESSAGE_TYPE.LIFE_CYCLE) {
  override fun toString() = "IpcLifeCycle(channelId=$channelId,state=$state)"

}