package org.dweb_browser.sys.window.core

import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.helper.ChangeableMap
import org.dweb_browser.helper.UUID

/**
 * 窗口实例管理
 */
class WindowInstancesManager {
  /**
   * 所有的窗口实例
   */
  private val instances = ChangeableMap<UUID, WindowController>()
  fun get(id: UUID) = instances[id]
  fun add(window: WindowController) {
    instances[window.id] = window
    window.onClose {
      instances.remove(window.id)
    }
  }

  fun findByOwner(mmid: MMID): WindowController? {
    return instances.firstNotNullOfOrNull { if (it.value.state.constants.owner == mmid) it.value else null }
  }
}

val windowInstancesManager by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
  WindowInstancesManager()
}