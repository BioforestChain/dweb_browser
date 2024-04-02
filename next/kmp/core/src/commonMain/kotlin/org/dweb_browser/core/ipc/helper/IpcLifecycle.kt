package org.dweb_browser.core.ipc.helper

import kotlinx.serialization.Serializable
import org.dweb_browser.core.help.types.CommonAppManifest

/**
 * Ipc生命周期控制
 */
@Serializable
sealed class IpcLifecycle(
  val state: LIFECYCLE_STATE,
) : IpcMessage(IPC_MESSAGE_TYPE.LIFECYCLE) {
  @Serializable
  data class Init(
    val pid: Int,
    val locale: CommonAppManifest,
    val remote: CommonAppManifest,
  ) : IpcLifecycle(LIFECYCLE_STATE.INIT)

  // TODO 测试能否 equals？
  @Serializable
  class Opening() : IpcLifecycle(LIFECYCLE_STATE.OPENING)

  // TODO 测试能否 equals？
  @Serializable
  class Opened() : IpcLifecycle(LIFECYCLE_STATE.OPENED)

  @Serializable
  data class Closing(val reason: String? = null) : IpcLifecycle(LIFECYCLE_STATE.CLOSING)

  @Serializable
  data class Closed(val reason: String? = null) : IpcLifecycle(LIFECYCLE_STATE.CLOSED)

}
