package info.bagen.dwebbrowser.microService.browser.desk

import androidx.lifecycle.lifecycleScope
import org.dweb_browser.window.core.WindowController
import org.dweb_browser.window.core.WindowState

class DesktopWindowController(
  var manager: DesktopWindowsManager,
  state: WindowState,
) : WindowController(state) {
  override val context get() = manager.activity
  override val coroutineScope get() = context.lifecycleScope

  init {
    state.observable.coroutineScope = manager.activity.lifecycleScope
  }
}