package org.dweb_browser.core.ipc.helper

import kotlinx.serialization.Serializable
import org.dweb_browser.core.help.types.CommonAppManifest

/**
 * Ipc生命周期控制
 */
@Serializable
sealed class IpcLifecycle : IpcMessage(IPC_MESSAGE_TYPE.LIFECYCLE) {
  abstract val state: LIFECYCLE_STATE
  @Serializable
  data class Init internal constructor(
    override val state: LIFECYCLE_STATE,
    val pid: Int,
    val locale: CommonAppManifest,
    val remote: CommonAppManifest,
  ) : IpcLifecycle() {
    constructor(pid: Int, locale: CommonAppManifest, remote: CommonAppManifest) : this(
      LIFECYCLE_STATE.INIT, pid, locale, remote
    )
  }

  // TODO 测试能否 equals？
  @Serializable
  data class IpcOpening internal constructor(override val state: LIFECYCLE_STATE) : IpcLifecycle() {
    constructor() : this(LIFECYCLE_STATE.OPENING)
  }

  // TODO 测试能否 equals？
  @Serializable
  data class IpcOpened internal constructor(override val state: LIFECYCLE_STATE) : IpcLifecycle() {
    constructor() : this(LIFECYCLE_STATE.OPENED)
  }

  @Serializable
  data class IpcClosing internal constructor(
    override val state: LIFECYCLE_STATE,
    val reason: String?,
  ) : IpcLifecycle() {
    constructor(reason: String? = null) : this(LIFECYCLE_STATE.CLOSING, reason)
  }

  @Serializable
  data class IpcClosed internal constructor(
    override val state: LIFECYCLE_STATE,
    val reason: String? = null,
  ) : IpcLifecycle() {
    constructor(reason: String? = null) : this(LIFECYCLE_STATE.CLOSED, reason)
  }


}
