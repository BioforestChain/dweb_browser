package org.dweb_browser.browser.desk

import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.core.WindowState
import org.dweb_browser.sys.window.core.WindowsManager

class DesktopWindowController(
  manager: DesktopWindowsManager,
  state: WindowState,
) : WindowController(state, manager) {
  override val manager get() = _manager as DesktopWindowsManager
  override val viewBox = manager.viewBox
  override val lifecycleScope get() = viewBox.lifecycleScope
  override fun upsetManager(manager: WindowsManager<*>?) {
    when (val deskManager = manager) {
      is DesktopWindowsManager -> {
        super.upsetManager(deskManager)
        state.observable.coroutineScope =
          deskManager.viewController.lifecycleScope
      }

      else -> throw Exception("invalid type $manager should be DesktopWindowsManager")
    }
  }
}