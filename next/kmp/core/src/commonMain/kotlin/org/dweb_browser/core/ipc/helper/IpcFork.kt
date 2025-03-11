package org.dweb_browser.core.ipc.helper

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.dweb_browser.core.help.types.CommonAppManifest

/**
 * 这里会告知 fork的发起者 是否是自动启动，以及自启动的原因
 * 接受者可以用来参考，但无需遵循一致，唯一需要一致的只有 pid
 */
@Serializable
@SerialName(IPC_MESSAGE_TYPE_FORK)
data class IpcFork(
  val pid: Int,
  val autoStart: Boolean,
  val startReason: String? = null,
  val locale: CommonAppManifest,
  val remote: CommonAppManifest,
) : IpcRawMessage, IpcMessage {
}
