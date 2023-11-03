package org.dweb_browser.dwebview.ipcWeb

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dweb_browser.core.help.types.IMicroModuleManifest
import org.dweb_browser.core.ipc.Ipc
import org.dweb_browser.core.ipc.helper.IPC_ROLE
import org.dweb_browser.core.ipc.helper.IpcEvent
import org.dweb_browser.core.ipc.helper.IpcEventJsonAble
import org.dweb_browser.core.ipc.helper.IpcMessage
import org.dweb_browser.core.ipc.helper.IpcReqMessage
import org.dweb_browser.core.ipc.helper.IpcRequest
import org.dweb_browser.core.ipc.helper.IpcResMessage
import org.dweb_browser.core.ipc.helper.IpcResponse
import org.dweb_browser.core.ipc.helper.IpcStreamAbort
import org.dweb_browser.core.ipc.helper.IpcStreamData
import org.dweb_browser.core.ipc.helper.IpcStreamDataJsonAble
import org.dweb_browser.core.ipc.helper.IpcStreamEnd
import org.dweb_browser.core.ipc.helper.IpcStreamPaused
import org.dweb_browser.core.ipc.helper.IpcStreamPulling
import org.dweb_browser.dwebview.IWebMessagePort
import org.dweb_browser.helper.printDebug

fun debugMessagePortIpc(tag: String, msg: Any = "", err: Throwable? = null) =
  printDebug("message-port-ipc", tag, msg, err)

interface IMessagePort {
  companion object {
    fun from(port: IWebMessagePort): IMessagePort {
      TODO("Not yet implemented")
    }
  }

  suspend fun postMessage(data: String)
  fun close()
}

open class DMessagePortIpc(
  open val port: IMessagePort,
  override val remote: IMicroModuleManifest,
  private val roleType: IPC_ROLE,
) : Ipc() {
  companion object {
    fun from(
      port: IWebMessagePort, remote: IMicroModuleManifest, roleType: IPC_ROLE
    ) = DMessagePortIpc(IMessagePort.from(port), remote, roleType)
  }

  override val role get() = roleType.role
  override fun toString(): String {
    return super.toString() + "@MessagePortIpc(${remote.mmid}, $roleType)"
  }

  override suspend fun _doPostMessage(data: IpcMessage) {
    val message = when (data) {
      is IpcRequest -> Json.encodeToString(data.ipcReqMessage)
      is IpcResponse -> Json.encodeToString(data.ipcResMessage)
      is IpcStreamData -> Json.encodeToString(data)
      is IpcEvent -> Json.encodeToString(data)
      is IpcEventJsonAble -> Json.encodeToString(data)
      is IpcReqMessage -> Json.encodeToString(data)
      is IpcResMessage -> Json.encodeToString(data)
      is IpcStreamAbort -> Json.encodeToString(data)
      is IpcStreamDataJsonAble -> Json.encodeToString(data)
      is IpcStreamEnd -> Json.encodeToString(data)
      is IpcStreamPaused -> Json.encodeToString(data)
      is IpcStreamPulling -> Json.encodeToString(data)
      else -> throw Exception("unknown message: $data")
    }
    this.port.postMessage(message)
  }

  override suspend fun _doClose() {
    this.port.postMessage("close")
    this.port.close()
  }

  suspend fun emitClose() {
    closeSignal.emit()
  }
}

