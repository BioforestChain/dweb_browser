package org.dweb_browser.browser.desk

import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.core.WindowState
import org.dweb_browser.sys.window.core.WindowsManager

class DesktopWindowController(
  manager: DesktopWindowsManager,
  state: WindowState,
) : WindowController(state, manager.viewBox) {

  override fun getManager(): DesktopWindowsManager {
    return super.getManager() as DesktopWindowsManager
  }

  override fun upsetManager(manager: WindowsManager<*>?) {
    super.upsetManager(manager)

    if (manager != null) {
      when (manager) {
        is DesktopWindowsManager ->
          state.observable.coroutineScope = manager.viewController.lifecycleScope

        else -> throw Exception("invalid type $manager should be DesktopWindowsManager")
      }
    }
    super.upsetManager(manager)
  }

  override suspend fun toggleKeepBackground(keepBackground: Boolean?) {
    /// 内部模块的设置，不允许修改
    if (state.constants.owner.let {
        it.endsWith(".browser.dweb") || it.endsWith(".std.dweb") || it.endsWith(".sys.dweb")
      }) {
      return
    }
    super.toggleKeepBackground(keepBackground)
  }
}