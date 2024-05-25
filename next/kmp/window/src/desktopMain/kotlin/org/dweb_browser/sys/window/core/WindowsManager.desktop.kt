package org.dweb_browser.sys.window.core

import org.dweb_browser.helper.OffListener
import org.dweb_browser.helper.platform.asDesktop
import org.dweb_browser.helper.some


actual class WindowsManagerDelegate<T : WindowController> actual constructor(
  val manager: WindowsManager<T>,
) {

  actual fun focusDesktop() {
    manager.viewController.asDesktop().getComposeWindowOrNull()?.toFront()
  }

  actual suspend fun focusWindow(win: WindowController) = manager.withWindowLifecycleScope {
    if (!win.isFocused) {
      win.focus()
    }
  }

  actual suspend fun focusWindows(windows: List<T>) = manager.withWindowLifecycleScope {
    if (windows.some { it.isFocused }) {
      return@withWindowLifecycleScope
    }
    windows.maxByOrNull { it.state.zIndex }?.also {
      focusWindow(it)
    }
  }

  actual suspend fun addedWindow(win: T, offListenerList: MutableList<OffListener<*>>) =
    manager.withWindowLifecycleScope {
      /// 窗口聚焦时，需要进行重新排序
      offListenerList += win.onFocus {
        manager.moveToTop(win)
      }
    }
}