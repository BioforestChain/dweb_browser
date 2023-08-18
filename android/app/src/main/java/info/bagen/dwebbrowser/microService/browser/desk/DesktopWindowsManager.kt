package info.bagen.dwebbrowser.microService.browser.desk

import org.dweb_browser.helper.android.BaseActivity
import org.dweb_browser.helper.removeWhen
import org.dweb_browser.window.core.WindowsManager
import org.dweb_browser.window.core.createWindowAdapterManager
import java.util.WeakHashMap
import kotlin.math.sqrt

class DesktopWindowsManager(internal val activity: DesktopActivity) :
  WindowsManager<DesktopWindowController>(activity) {

  companion object {
    private val instances = WeakHashMap<DesktopActivity, DesktopWindowsManager>()
    fun getInstance(
      activity: DesktopActivity,
      onPut: (wm: DesktopWindowsManager) -> Unit
    ): DesktopWindowsManager = instances.getOrPut(activity) {
      DesktopWindowsManager(activity).also { dwm ->
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
      newWindowState.updateMutableBounds {
        with(activity.resources.displayMetrics) {
          val displayWidth = widthPixels / density
          val displayHeight = heightPixels / density
          if (width.isNaN()) {
            width = displayWidth / sqrt(2f)
          }
          if (height.isNaN()) {
            height = displayHeight / sqrt(3f)
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
      /// Activity 销毁的时候，移除窗口适配器
      .removeWhen(activity.onDestroyActivity)
  }

  override fun addNewWindow(win: DesktopWindowController, autoFocus: Boolean) {
    /// 对 win 的 manager 进行修改
    win.manager = this;
    super.addNewWindow(win, autoFocus)
  }
}

