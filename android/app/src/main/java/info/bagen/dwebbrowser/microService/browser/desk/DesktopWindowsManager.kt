package info.bagen.dwebbrowser.microService.browser.desk

import org.dweb_browser.helper.platform.PlatformViewController
import org.dweb_browser.helper.removeWhen
import org.dweb_browser.window.core.WindowsManager
import org.dweb_browser.window.core.createWindowAdapterManager
import org.dweb_browser.window.core.helper.setDefaultFloatWindowBounds
import java.util.WeakHashMap
import kotlin.math.sqrt

class DesktopWindowsManager(val viewController: PlatformViewController) :
  WindowsManager<DesktopWindowController>(viewController) {

  companion object {
    private val instances = WeakHashMap<DesktopActivity, DesktopWindowsManager>()
    fun getOrPutInstance(
      activity: DesktopActivity, onPut: (wm: DesktopWindowsManager) -> Unit
    ): DesktopWindowsManager = instances.getOrPut(activity) {
      DesktopWindowsManager(PlatformViewController(activity)).also { dwm ->
        onPut(dwm);
        activity.onDestroyActivity {
          instances.remove(activity)
        }
      }
    }
  }

  /// 初始化一些监听
  init {
    /// 创建成功，提供适配器来渲染窗口
    createWindowAdapterManager.append { newWindowState ->
      /// 新窗口的bounds可能都是没有配置的，所以这时候默认给它们设置一个有效的值

      with(viewController) {
        val displayWidth = getViewWidthPx() / getDisplayDensity()
        val displayHeight = getViewHeightPx() / getDisplayDensity()
        newWindowState.setDefaultFloatWindowBounds(
          displayWidth,
          displayHeight,
          allWindows.size.toFloat()
        )
      }
      newWindowState.updateMutableBounds {
        with(viewController) {
          val displayWidth = getViewWidthPx() / getDisplayDensity()
          val displayHeight = getViewHeightPx() / getDisplayDensity()
          if (width.isNaN()) {
            width = displayWidth / sqrt(3f)
          }
          if (height.isNaN()) {
            height = displayHeight / sqrt(5f)
          }
          /// 在 top 和 left 上，为窗口动态配置坐标，避免层叠在一起
          if (left.isNaN()) {
            val maxLeft = displayWidth - width
            val gapSize = 47f; // 质数
            val gapCount = (maxLeft / gapSize).toInt();

            left = gapSize + (allWindows.size % gapCount) * gapSize
          }
          if (top.isNaN()) {
            val maxTop = displayHeight - height
            val gapSize = 71f; // 质数
            val gapCount = (maxTop / gapSize).toInt();
            top = gapSize + (allWindows.size % gapCount) * gapSize
          }
        }
      }

      /// 添加窗口到列表中
      val win = DesktopWindowController(this, newWindowState)
      addNewWindow(win);

      win
    }
      /// 生命周期销毁的时候，移除窗口适配器
      .removeWhen(viewController.lifecycleScope)

  }
}

