package info.bagen.dwebbrowser.microService.browser.desk

import androidx.lifecycle.lifecycleScope
import org.dweb_browser.window.core.WindowController
import org.dweb_browser.window.core.WindowState
import org.dweb_browser.window.core.WindowsManager

class DesktopWindowController(
  manager: DesktopWindowsManager,
  state: WindowState,
) : WindowController(state, manager) {
  override val manager get() = _manager as DesktopWindowsManager
  override val context get() = manager.activity
  override val coroutineScope get() = context.lifecycleScope
  override fun upsetManager(manager: WindowsManager<*>?) {
    when (val deskManager = manager) {
      is DesktopWindowsManager -> {
        super.upsetManager(deskManager)
        state.observable.coroutineScope =
          deskManager.activity.lifecycleScope
      }

      else -> throw Exception("invalid type $manager should be DesktopWindowsManager")
    }
  }
}