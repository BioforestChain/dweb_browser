package info.bagen.dwebbrowser.microService.browser.desk

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope
import info.bagen.dwebbrowser.microService.core.windowAdapterManager
import kotlinx.coroutines.launch
import org.dweb_browser.dwebview.some
import org.dweb_browser.helper.ChangeableMap
import org.dweb_browser.helper.ChangeableSet
import org.dweb_browser.helper.OffListener
import org.dweb_browser.helper.removeWhen
import org.dweb_browser.microservice.help.MMID
import java.util.WeakHashMap
import kotlin.math.sqrt

class DesktopWindowsManager(internal val activity: DesktopActivity) {
  companion object {
    private val instances = WeakHashMap<DesktopActivity, DesktopWindowsManager>()
    fun getInstance(activity: DesktopActivity, onPut: (wm: DesktopWindowsManager) -> Unit) =
      instances.getOrPut(activity) {
        DesktopWindowsManager(activity).also { dwm ->
          onPut(dwm);
          activity.onDestroyActivity {
            instances.remove(activity)
          }
        }
      }
  }

  /**
   * 一个已经根据 zIndex 排序完成的只读列表
   */
  val winList = mutableStateOf(listOf<DesktopWindowController>());

  /**
   * 存储最大化的窗口
   */
  val hasMaximizedWins =
    ChangeableSet<DesktopWindowController>(activity.lifecycleScope.coroutineContext)

  /**
   * 窗口在管理时说需要的一些状态机
   */
  data class InManageState(val doDestroy: () -> Unit)

  internal val allWindows =
    ChangeableMap<DesktopWindowController, InManageState>(activity.lifecycleScope.coroutineContext).also {
      it.onChange { wins ->
        /// 从小到大排序
        val newWinList = wins.keys.toList().sortedBy { win -> win.state.zIndex };
        var changed = false
        if (newWinList.size == winList.value.size) {
          for ((index, item) in winList.value.withIndex()) {
            if (item != newWinList[index]) {
              changed = true
            }
          }
        } else {
          changed = true
        }
        if (changed) {
          winList.value = newWinList
        }
      }.also { off ->
        activity.onDestroyActivity {
          off()
        }
      }
    }


  /// 当前记录的聚焦窗口
  private var lastFocusedWin: DesktopWindowController? = null;

  /// 初始化一些监听
  init {
    /// 创建成功，提供适配器来渲染窗口
    windowAdapterManager.append { newWindowState ->
      /// 新窗口的bounds可能都是没有配置的，所以这时候默认给它们设置一个有效的值
      with(newWindowState.bounds) {
        activity.resources.displayMetrics.also { displayMetrics ->
          val displayWidth = displayMetrics.widthPixels / displayMetrics.density
          val displayHeight = displayMetrics.heightPixels / displayMetrics.density
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

      val win = DesktopWindowController(this, newWindowState)
      addNewWindow(win)

      win
    }
      /// Activity 销毁的时候，移除窗口适配器
      .removeWhen(activity.onDestroyActivity)
  }


  /**
   * 将一个窗口添加进来管理
   */
  internal fun addNewWindow(win: DesktopWindowController) {
    /// 对 win 的 manager 进行修改
    win.manager = this;
    /// 对窗口做一些启动准备
    val offListenerList = mutableListOf<OffListener>()
    offListenerList += win.onFocus {
      if (lastFocusedWin != win) {
        lastFocusedWin?.blur()
        lastFocusedWin = win;
        moveToTop(win)
      }
    }
    /// 如果窗口释放聚焦，那么释放引用
    offListenerList += win.onBlur {
      if (lastFocusedWin == win) {
        lastFocusedWin = null
      }
    }
    offListenerList += win.onMaximize {
      debugDesk("maximized")
      hasMaximizedWins.add(win)
    }
    offListenerList += win.onUnMaximize {
      debugDesk("unmaximized")
      hasMaximizedWins.remove(win)
    }
    /// 立即执行
    if (win.isMaximized()) {
      hasMaximizedWins.add(win)
    }
    /// 窗口销毁的时候，做引用释放
    offListenerList += win.onClose {
      removeWindow(win)
    }
    /// 存储窗口与它的 状态机（销毁函数）
    allWindows[win] = InManageState {
      for (off in offListenerList) {
        off()
      }
    }
    /// 第一次装载窗口，默认将它聚焦到最顶层
    focus(win)
  }

  internal fun removeWindow(win: DesktopWindowController) =
    allWindows.remove(win)?.let { inManageState ->
      hasMaximizedWins.remove(win)

      inManageState.doDestroy()
      true
    } ?: false

  private suspend fun reOrderZIndex() {
    for ((index, win) in allWindows.keys.toList().sortedBy { it.state.zIndex }.withIndex()) {
      win.state.zIndex = index
    }
    allWindows.emitChange()
  }

  /**
   * 将指定窗口移动到最上层
   */
  suspend fun moveToTop(win: DesktopWindowController) {
    /// 窗口被聚焦，那么遍历所有的窗口，为它们重新生成zIndex值
    win.state.zIndex += allWindows.size;
    reOrderZIndex()
  }

  fun focus(win: DesktopWindowController) = activity.lifecycleScope.launch {
    win.focus()
  }

  suspend fun focus(mmid: MMID) {
    val windows = findWindows(mmid)
    for (win in windows) {
      win.focus()
    }
  }

  fun findWindows(mmid: MMID) =
    allWindows.keys.filter { win -> win.state.owner == mmid }.sortedBy { it.state.zIndex }

  /**
   * 返回最终 isMaximized 的值
   */
  suspend fun toggleMaximize(mmid: MMID): Boolean {
    val windows = findWindows(mmid)

    /**
     * 只要有一个窗口处于最大化的状态，就当作所有窗口都处于最大化
     */
    val isMaximized = windows.some { win -> win.isMaximized() };
    for (win in windows) {
      if (isMaximized) {
        win.unMaximize()
      } else {
        win.maximize()
      }
    }
    return !isMaximized
  }
}