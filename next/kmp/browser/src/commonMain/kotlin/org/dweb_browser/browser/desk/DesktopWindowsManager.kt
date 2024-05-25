package org.dweb_browser.browser.desk

import org.dweb_browser.helper.WeakHashMap
import org.dweb_browser.helper.platform.IPureViewBox
import org.dweb_browser.helper.platform.IPureViewController
import org.dweb_browser.helper.removeWhen
import org.dweb_browser.sys.window.core.WindowsManager
import org.dweb_browser.sys.window.core.helper.setDefaultFloatWindowBounds
import org.dweb_browser.sys.window.core.windowAdapterManager

expect fun DesktopWindowsManager.Companion.getOrPutInstance(
  platformViewController: IPureViewController,
  viewBox: IPureViewBox,
  onPut: (wm: DesktopWindowsManager) -> Unit,
): DesktopWindowsManager

class DesktopWindowsManager internal constructor(
  val viewController: IPureViewController,
  val viewBox: IPureViewBox,
) : WindowsManager<DesktopWindowController>(viewController, viewBox) {

  companion object {
    internal val instances = WeakHashMap<IPureViewController, DesktopWindowsManager>()
  }

  /// 初始化一些监听
  init {
    /// 创建成功，提供适配器来渲染窗口
    windowAdapterManager.append { newWindowState ->
      /// 新窗口的bounds可能都是没有配置的，所以这时候默认给它们设置一个有效的值

      with(viewBox) {
        val maxWindowSize = getViewControllerMaxBounds()
        newWindowState.setDefaultFloatWindowBounds(
          maxWindowSize.width, maxWindowSize.height, allWindowsFlow.value.size.toFloat()
        )
      }

      /// 添加窗口到列表中
      val win = DesktopWindowController(this, newWindowState)
      addNewWindow(win);

      win
    }
      /// 生命周期销毁的时候，移除窗口适配器
      .removeWhen(viewBox.lifecycleScope)

  }
}

