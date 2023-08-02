package info.bagen.dwebbrowser.microService.browser.desk

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope
import info.bagen.dwebbrowser.microService.core.windowAdapterManager
import kotlinx.coroutines.launch
import org.dweb_browser.helper.ChangeableList
import org.dweb_browser.microservice.help.MMID

class DesktopWindowsManager(private val activity: DesktopActivity) {
  /**
   * 一个已经根据 zIndex 排序完成的只读列表
   */
  val winList = mutableStateOf(listOf<DesktopWindowController>());

  private val allWindows = ChangeableList<DesktopWindowController>(activity.lifecycleScope).also {
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
      with(winState.bounds) {
        left = 150f
        top = 250f
        width = 200f
        height = 300f
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

  private fun reOrderZIdnex() {
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
    reOrderZIdnex()
  }

  fun focus(win: DesktopWindowController) = activity.lifecycleScope.launch {
    win.focus()
    moveToTop(win)
  }

  fun focus(mmid: MMID) {
    var changed = false
    for (win in allWindows) {
      if (win.state.owner == mmid) {
        win.state.zIndex += allWindows.size
        changed = true
      }
    }
    if (changed) {
      reOrderZIdnex()
    }
  }
}