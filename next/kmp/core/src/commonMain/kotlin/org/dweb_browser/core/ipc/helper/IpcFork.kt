package org.dweb_browser.core.ipc.helper

import kotlinx.serialization.Serializable
import org.dweb_browser.core.help.types.CommonAppManifest

@Serializable
class IpcFork(
  val pid: Int,
  val autoStart: Boolean,
  val locale: CommonAppManifest,
  val remote: CommonAppManifest,
) : IpcMessage(IPC_MESSAGE_TYPE.FORK) {
}
