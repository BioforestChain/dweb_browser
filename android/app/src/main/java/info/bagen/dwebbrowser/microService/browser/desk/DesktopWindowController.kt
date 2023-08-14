package info.bagen.dwebbrowser.microService.browser.desk

import androidx.lifecycle.lifecycleScope
import info.bagen.dwebbrowser.microService.core.WindowController
import info.bagen.dwebbrowser.microService.core.WindowState

class DesktopWindowController(
  internal var manager: DesktopWindowsManager, state: WindowState
) : WindowController(state) {
  override val context get() = manager.activity
  override val coroutineScope get() = context.lifecycleScope

  init {
    state.observable.coroutineScope = manager.activity.lifecycleScope
  }
}