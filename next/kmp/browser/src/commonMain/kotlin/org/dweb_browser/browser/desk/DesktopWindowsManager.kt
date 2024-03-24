package org.dweb_browser.browser.desk

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import org.dweb_browser.helper.WeakHashMap
import org.dweb_browser.helper.platform.IPureViewBox
import org.dweb_browser.helper.platform.IPureViewController
import org.dweb_browser.helper.removeWhen
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.core.WindowsManager
import org.dweb_browser.sys.window.core.helper.setDefaultFloatWindowBounds
import org.dweb_browser.sys.window.core.windowAdapterManager

expect fun DesktopWindowsManager.Companion.getOrPutInstance(
  platformViewController: IPureViewController,
  viewBox: IPureViewBox,
  onPut: (wm: DesktopWindowsManager) -> Unit
): DesktopWindowsManager

class DesktopWindowsManager internal constructor(
  val viewController: IPureViewController,
  val viewBox: IPureViewBox
) :
  WindowsManager<DesktopWindowController>(viewBox) {

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
          maxWindowSize.width, maxWindowSize.height, allWindows.size.toFloat()
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

  override fun windowToggleKeepBackground(
    win: WindowController,
    keepBackground: Boolean?
  ): Deferred<Unit> =
    /// 内部模块的设置，不允许修改
    if (win.state.constants.owner.let {
        it.endsWith(".browser.dweb") || it.endsWith(".std.dweb") || it.endsWith(".sys.dweb")
      })
      CompletableDeferred(Unit)
    else
      super.windowToggleKeepBackground(win, keepBackground)
}

