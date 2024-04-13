package org.dweb_browser.core.ipc.helper

import kotlinx.serialization.Serializable
import org.dweb_browser.core.help.types.CommonAppManifest
import org.dweb_browser.helper.OrderBy

/**
 * Ipc生命周期控制
 */
@Serializable
sealed class IpcLifecycle : IpcMessage(IPC_MESSAGE_TYPE.LIFECYCLE), OrderBy {
  abstract val state: LIFECYCLE_STATE
  abstract override val order: Int

  companion object {
    const val DEFAULT_ORDER = -1
  }

  @Serializable
  data class Init internal constructor(
    override val state: LIFECYCLE_STATE,
    val pid: Int,
    val locale: CommonAppManifest,
    val remote: CommonAppManifest,
    override val order: Int,
  ) : IpcLifecycle() {
    constructor(
      pid: Int,
      locale: CommonAppManifest,
      remote: CommonAppManifest,
      orderBy: Int = DEFAULT_ORDER,
    ) : this(
      LIFECYCLE_STATE.INIT, pid, locale, remote, orderBy
    )
  }

  // TODO 测试能否 equals？
  @Serializable
  data class IpcOpening internal constructor(
    override val state: LIFECYCLE_STATE,
    override val order: Int,
  ) : IpcLifecycle() {
    constructor(orderBy: Int = DEFAULT_ORDER) : this(LIFECYCLE_STATE.OPENING, orderBy)
  }

  // TODO 测试能否 equals？
  @Serializable
  data class IpcOpened internal constructor(
    override val state: LIFECYCLE_STATE,
    override val order: Int,
  ) : IpcLifecycle() {
    constructor(
      orderBy: Int = DEFAULT_ORDER,
    ) : this(LIFECYCLE_STATE.OPENED, orderBy)
  }

  @Serializable
  data class IpcClosing internal constructor(
    override val state: LIFECYCLE_STATE,
    val reason: String?,
    override val order: Int,
  ) : IpcLifecycle() {
    constructor(
      reason: String? = null,
      orderBy: Int = DEFAULT_ORDER,
    ) : this(LIFECYCLE_STATE.CLOSING, reason, orderBy)
  }

  @Serializable
  data class IpcClosed internal constructor(
    override val state: LIFECYCLE_STATE,
    val reason: String? = null,
    override val order: Int,
  ) : IpcLifecycle() {
    constructor(
      reason: String? = null,
      orderBy: Int = DEFAULT_ORDER,
    ) : this(LIFECYCLE_STATE.CLOSED, reason, orderBy)
  }


}
