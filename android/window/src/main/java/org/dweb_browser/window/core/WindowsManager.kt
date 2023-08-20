package org.dweb_browser.window.core

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.dweb_browser.helper.ChangeableMap
import org.dweb_browser.helper.ChangeableSet
import org.dweb_browser.helper.Observable
import org.dweb_browser.helper.OffListener
import org.dweb_browser.helper.android.BaseActivity
import org.dweb_browser.helper.removeWhen
import org.dweb_browser.helper.some
import org.dweb_browser.microservice.help.MMID
import org.dweb_browser.window.core.constant.WindowManagerPropertyKeys
import org.dweb_browser.window.core.constant.WindowsManagerScope
import org.dweb_browser.window.core.constant.debugWindow
import java.util.WeakHashMap
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.reflect.KClass

open class WindowsManager<T : WindowController>(internal val activity: BaseActivity) {
  val state = ManagerState(activity)

  /**
   * 一个已经根据 zIndex 排序完成的只读列表
   */
  val winList = mutableStateOf(emptyList<T>());

  /**
   * 置顶窗口，一个已经根据 zIndex 排序完成的只读列表
   */
  val winListTop = mutableStateOf(emptyList<T>());

  /**
   * 存储最大化的窗口
   */
  val hasMaximizedWins =
    ChangeableSet<T>(activity.lifecycleScope.coroutineContext)

  val allWindows =
    ChangeableMap<T, WindowsManagerScope>(activity.lifecycleScope.coroutineContext);

  /**
   * 寻找最后一个聚焦的窗口
   */
  val lastFocusedWin: T?
    get() {
      /// 从最顶层的窗口往下遍历
      fun findInWinList(winList: List<T>): T? {
        for (win in winList.asReversed()) {
          if (win.isFocused()) {
            /// 如果发现之前赋值过，这时候需要将之前的窗口给blur掉
            return win
          }
        }
        return null
      }

      return findInWinList(winList.value) ?: findInWinList(winListTop.value)
    }

  /**
   * 确保窗口现在只对最后一个元素聚焦
   *
   * 允许不存在聚焦的窗口，聚焦应该由用户行为触发
   */
  internal suspend fun doLastFocusedWin(): T? {
    var lastFocusedWin: T? = null

    /// 从最底层的窗口往上遍历
    suspend fun findInWinList(winList: List<T>) {
      for (win in winList) {
        if (win.isFocused()) {
          /// 如果发现之前赋值过，这时候需要将之前的窗口给blur掉
          lastFocusedWin?.blur()
          lastFocusedWin = win
        }
      }
    }

    findInWinList(winList.value)
    findInWinList(winListTop.value)
    return lastFocusedWin
  }

  /**
   * 将一个窗口添加进来管理
   */
  protected open fun addNewWindow(win: T, autoFocus: Boolean = true) {
    /// 对窗口做一些启动准备
    val offListenerList = mutableListOf<OffListener<*>>()

    /// 窗口聚焦时，需要将其挪到最上层，否则该聚焦会失效
    offListenerList += win.onFocus {
      focusWindow(win)
    }

    offListenerList += win.onMaximize {
      hasMaximizedWins.add(win)
    }
    offListenerList += win.onUnMaximize {
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
    allWindows[win] = WindowsManagerScope {
      for (off in offListenerList) {
        off()
      }
    }
    /// 第一次装载窗口，默认将它聚焦到最顶层
    if (autoFocus) {
      focusWindow(win)
    }
  }

  /**
   * 移除一个窗口
   */
  internal suspend fun removeWindow(win: WindowController, autoFocus: Boolean = true) =
    allWindows.remove(win)?.let { inManageState ->
      /// 移除最大化窗口集合
      hasMaximizedWins.remove(win)

      if (autoFocus) {
        /// 对窗口进行重新排序
        reOrderZIndex()
      }

      /// 最后，销毁绑定事件
      inManageState.doDestroy()
      true
    } ?: false


  /**
   * 将窗口迁移到另一个管理器中，并且维护这些窗口的状态
   */
  suspend fun moveWindows(
    other: WindowsManager<T>,
    windows: Iterable<T> = winList.value.toList()/*拷贝一份避免并发修改导致的问题，这里默认使用 zIndex 的顺序来迁移，可以避免问题*/
  ) {
    /// 窗口迁移
    for (win in windows) {
      removeWindow(win, false)
      other.addNewWindow(win, false)
    }
    reOrderZIndex()
    other.reOrderZIndex()
    debugWindow(
      "moveWindows",
      "self:${this.winList.value.size}/${this.winListTop.value.size} => other: ${other.winList.value.size}/${other.winListTop.value.size}"
    )
  }

  /**
   * 依据窗口的属性，对窗口进行分组并排序，并对zIndex属性进行调整
   */
  private suspend fun reOrderZIndex() {
    /// 根据 alwaysOnTop 进行分组
    val winList = mutableListOf<T>()
    val winListTop = mutableListOf<T>()
    for (win in allWindows.keys) {
      if (win.state.alwaysOnTop) {
        winListTop += win
      } else {
        winList += win
      }
    }

    /// 对窗口的 zIndex 进行重新赋值
    fun resetZIndex(
      list: MutableList<T>, state: MutableState<List<T>>
    ): Int {
      var changes = abs(list.size - state.value.size) // 首先，只要有长度变动，就已经意味着改变了
      val sortedList = list.sortedBy { it.state.zIndex }
      for ((index, win) in sortedList.withIndex()) {
        if (win.state.zIndex != index) {
          win.state.zIndex = index
          changes += 1 // 每一次改变都进行一次标记
        }
      }
      if (changes > 0) {
        state.value = sortedList
      }
      return changes
    }

    val anyChanges = resetZIndex(winList, this.winList) + resetZIndex(winListTop, this.winListTop);

    if (anyChanges > 0) {
      allWindows.emitChange()
    }
  }

  /**
   * 将指定窗口移动到最上层
   */
  suspend fun moveToTop(win: WindowController) {
    /// 窗口被聚焦，那么遍历所有的窗口，为它们重新生成zIndex值
    win.state.zIndex += allWindows.size;
    reOrderZIndex()
  }

  /**
   * 对一个窗口做聚焦操作
   */
  fun focusWindow(win: WindowController) = activity.lifecycleScope.launch {
    if (lastFocusedWin != win) {
      lastFocusedWin?.blur()
      win.focus()
      moveToTop(win)
      doLastFocusedWin()
    }
  }

  /**
   * 对一些窗口做聚焦操作
   */
  suspend fun focusWindow(mmid: MMID) {
    val windows = findWindows(mmid)
    lastFocusedWin?.let {
      if (!windows.contains(it)) {
        it.blur()
      }
    }
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