package org.dweb_browser.sys.window.core

import org.dweb_browser.helper.OffListener
import org.dweb_browser.helper.some


actual class WindowsManagerDelegate<T : WindowController> actual constructor(
  val manager: WindowsManager<T>,
) {
  actual fun focusDesktop() {}

  /**
   * 对一个窗口做聚焦操作
   */
  actual suspend fun focusWindow(win: WindowController) = manager.withWindowLifecycleScope {
    // 要聚焦窗口，首先切换它的可见性
    win.show()

    /**
     * 确保窗口现在只对最后一个元素聚焦
     *
     * 允许不存在聚焦的窗口，聚焦应该由用户行为触发
     */
    for (other in manager.allWindows) {
      if (other != win && other.isFocused) {
        other.blur()
      }
    }
    win.focus()
    manager.moveToTop(win)
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
      /// 窗口隐藏时，如果存在最大化的窗口，那么对其进行自动化聚焦
      fun List<T>.findFocusable() =
        filter { it.isVisible && it.isMaximized }.maxByOrNull { it.state.zIndex }
      offListenerList += win.onHidden {
        val focusableWin = manager.topWinList.findFocusable() ?: manager.winList.findFocusable()
        focusableWin?.also { focusWindow(it) }
      }


      /// 窗口聚焦时，需要将其挪到最上层，否则该聚焦会失效
      offListenerList += win.onFocus {
        focusWindow(win)
      }
    }
}