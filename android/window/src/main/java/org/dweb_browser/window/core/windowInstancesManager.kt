package org.dweb_browser.window.core

import org.dweb_browser.helper.ChangeableMap
import org.dweb_browser.window.core.constant.UUID

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

  val onAdd = instances.onChange
  val onRemove = instances.onChange
}

val windowInstancesManager = WindowInstancesManager()