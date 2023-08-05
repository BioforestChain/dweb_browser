package info.bagen.dwebbrowser.microService.browser.desk

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope
import info.bagen.dwebbrowser.microService.core.windowAdapterManager
import kotlinx.coroutines.launch
import org.dweb_browser.dwebview.some
import org.dweb_browser.helper.ChangeableList
import org.dweb_browser.helper.ChangeableSet
import org.dweb_browser.microservice.help.MMID
import java.util.WeakHashMap
import kotlin.math.sqrt

class DesktopWindowsManager(internal val activity: DesktopActivity) {
  companion object {
    private val instances = WeakHashMap<DesktopActivity, DesktopWindowsManager>()
    fun getInstance(activity: DesktopActivity, onPut: (wm: DesktopWindowsManager) -> Unit) =
      instances.getOrPut(activity) {
        DesktopWindowsManager(activity).also(onPut)
      }
  }

  /**
   * 一个已经根据 zIndex 排序完成的只读列表
   */
  val winList = mutableStateOf(listOf<DesktopWindowController>());

  /**
   * 存储最大化的窗口
   */
  val hasMaximizedWins = ChangeableSet<DesktopWindowController>()

  internal val allWindows = ChangeableList<DesktopWindowController>(activity.lifecycleScope).also {
    it.onChange { wins ->
      /// 从小到大排序
      val newWinList = wins.toList().sortedBy { win -> win.state.zIndex };
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
      Unit;
    }
  }


  /// 当前记录的聚焦窗口
  private var lastFocusedWin: DesktopWindowController? = null;

  /// 初始化一些监听
  init {
    /// 创建成功，提供适配器来渲染窗口
    val offAdapter = windowAdapterManager.append { winState ->
      activity.resources.displayMetrics.also { displayMetrics ->
        val displayWidth = displayMetrics.widthPixels / displayMetrics.density
        val displayHeight = displayMetrics.heightPixels / displayMetrics.density
        with(winState.bounds) {
          if (width.isNaN()) {
            width = displayWidth / sqrt(2f)
          }
          if (height.isNaN()) {
            height = displayHeight / sqrt(3f)
          }
          if (left.isNaN()) {
            val maxLeft = displayWidth - width
            val gapSize = 47f;
            val gapCount = (maxLeft / gapSize).toInt();

            left = gapSize + (allWindows.size % gapCount) * gapSize
          }
          if (top.isNaN()) {
            val maxTop = displayHeight - height
            val gapSize = 71f;
            val gapCount = (maxTop / gapSize).toInt();
            top = gapSize + (allWindows.size % gapCount) * gapSize
          }
        }
      }

      val win = DesktopWindowController(activity, winState)
        .also { win ->
          /// 对窗口做一些启动准备
          val jobs = activity.lifecycleScope.launch {
            launch {
              win.onFocus.toFlow().collect {
                if (lastFocusedWin != win) {
                  lastFocusedWin?.blur()
                  lastFocusedWin = win;
                  moveToTop(win)
                }
              }
            }
            /// 如果窗口释放聚焦，那么释放引用
            launch {
              win.onBlur.toFlow().collect {
                if (lastFocusedWin == win) {
                  lastFocusedWin = null
                }
              }
            }
            launch {
              win.onMaximize {
                hasMaximizedWins.add(win)
              }
              win.onUnMaximize {
                hasMaximizedWins.remove(win)
              }
              win.onDestroy {
                hasMaximizedWins.remove(win)
              }
            }
          }
          /// 第一次装载窗口，默认将它聚焦到最顶层
          focus(win)
          /// 窗口销毁的时候，做引用释放
          win.onDestroy {
            allWindows.remove(win)
            jobs.cancel()
          }
        }
      allWindows.add(win)

      win
    }

    /// Activity销毁的时候，移除窗口
    activity.onDestroyActivity { offAdapter(Unit) }
  }

  private fun reOrderZIndex() {
    for ((index, win) in allWindows.toList().sortedBy { it.state.zIndex }.withIndex()) {
      win.state.zIndex = index
    }
    allWindows.emitChange()
  }

  /**
   * 将指定窗口移动到最上层
   */
  fun moveToTop(win: DesktopWindowController) {
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
    allWindows.filter { win -> win.state.owner == mmid }.sortedBy { it.state.zIndex }

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