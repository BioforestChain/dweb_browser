package org.dweb_browser.sys.window.core

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.helper.ChangeableMap
import org.dweb_browser.helper.ChangeableSet
import org.dweb_browser.helper.OffListener
import org.dweb_browser.helper.platform.PlatformViewController
import org.dweb_browser.helper.some
import org.dweb_browser.sys.window.core.constant.WindowColorScheme
import org.dweb_browser.sys.window.core.constant.WindowStyle
import org.dweb_browser.sys.window.core.constant.WindowsManagerScope
import org.dweb_browser.sys.window.core.constant.debugWindow
import kotlin.math.abs

open class WindowsManager<T : WindowController>(internal val viewController: PlatformViewController) {
  val state = WindowsManagerState(viewController)

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
  val hasMaximizedWins = ChangeableSet<T>(viewController.lifecycleScope.coroutineContext)

  val allWindows =
    ChangeableMap<T, WindowsManagerScope>(viewController.lifecycleScope.coroutineContext);

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
          lastFocusedWin?.simpleBlur()
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
  @OptIn(ExperimentalCoroutinesApi::class)
  protected open fun addNewWindow(win: T, autoFocus: Boolean = true) {
    // 更新窗口的管理者角色
    win.upsetManager(this);
    /// 对窗口做一些启动准备
    val offListenerList = mutableListOf<OffListener<*>>()

    /// 窗口聚焦时，需要将其挪到最上层，否则该聚焦会失效
    offListenerList += win.onFocus {
      focusWindow(win).join()
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
      focusWindow(win) // void job
    }
  }

  /**
   * 移除一个窗口
   */
  internal suspend fun removeWindow(win: T, autoFocus: Boolean = true) =
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
    var winList = mutableListOf<T>()
    var winListTop = mutableListOf<T>()
    for (win in allWindows.keys) {
      if (win.state.alwaysOnTop) {
        winListTop += win
      } else {
        winList += win
      }
    }

    /// 对窗口的 zIndex 进行重新赋值
    fun resetZIndex(
      origin: MutableState<List<T>>, baseList: List<T>, setList: (newList: MutableList<T>) -> Unit
    ): Int {
      var changes = abs(baseList.size - origin.value.size) // 首先，只要有长度变动，就已经意味着改变了
      val sortedList = baseList.sortedBy { it.state.zIndex }
      for ((index, win) in sortedList.withIndex()) {
        if (win.state.zIndex != index) {
          win.state.zIndex = index
          changes += 1 // 每一次改变都进行一次标记
        }
      }
      if (changes > 0) {
        origin.value = sortedList
        setList(sortedList.toMutableList())
      }
      return changes
    }

    val anyChanges = resetZIndex(this.winList, winList) { winList = it } +  // changes 1
        resetZIndex(this.winListTop, winListTop) { winListTop = it } // changes 2

    if (anyChanges > 0) {
      allWindows.emitChange()
    }

    /// 最后，查询是否有窗口处于聚焦状态，如果没有，那么强制进行聚焦
    fun hasFocus(list: List<T>): Boolean {
      val lastWin = list.lastOrNull()
      return lastWin?.isFocused() ?: false
    }
    if (hasFocus(winListTop) || hasFocus(winList)) {
      return
    }

    suspend fun reFocus(writeableList: MutableList<T>): Boolean {
      val lastWin = writeableList.lastOrNull()
      if (lastWin != null) {
        if (!lastWin.isFocused()) {
          lastWin.focus()
        }
        return true
      }
      return false
    }
    if (!reFocus(winListTop)) {
      reFocus(winList)
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

  private fun <T : WindowController, R> winLifecycleScopeAsync(
    win: T, block: suspend CoroutineScope.() -> R
  ) = viewController.lifecycleScope.async {
    // TODO 检测 win 的所属权
    block()
  }

  /**
   * 对一个窗口做聚焦操作
   */
  fun focusWindow(win: WindowController) = winLifecycleScopeAsync(win) {
    when (val preFocusedWin = lastFocusedWin) {
      win -> false
      else -> {
        preFocusedWin?.simpleBlur()
        win.simpleFocus()
        moveToTop(win)
        doLastFocusedWin()
        true
      }
    }
  }

  /**
   * 对一个窗口做失焦操作
   */
  fun blurWindow(win: WindowController) = winLifecycleScopeAsync(win) {
    if (lastFocusedWin == win) {
      win.simpleBlur()
      true
    } else false
  }

  /**
   * 对一些窗口做聚焦操作
   */
  suspend fun focusWindow(mmid: MMID) {
    val windows = findWindows(mmid)
    lastFocusedWin?.let {
      if (!windows.contains(it)) {
        it.simpleBlur()
      }
    }
    for (win in windows) {
      focusWindow(win).join()
    }
  }

  fun findWindows(mmid: MMID) =
    allWindows.keys.filter { win -> win.state.constants.owner == mmid }.sortedBy { it.state.zIndex }

  /**
   * 获取窗口状态
   */
  fun getWindowStates(mmid: MMID): MutableList<WindowState> {
    val states = mutableListOf<WindowState>()
    for (win in allWindows.keys) {
      if (win.state.constants.owner == mmid) {
        states.add(win.state)
      }
    }
    return states
  }

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
        win.simpleUnMaximize()
      } else {
        win.simpleMaximize()
      }
    }
    return !isMaximized
  }

  fun maximizeWindow(win: WindowController) = winLifecycleScopeAsync(win) {
    focusWindow(win).join()
    win.simpleMaximize()
  }

  fun unMaximizeWindow(win: WindowController) = winLifecycleScopeAsync(win) {
    win.simpleUnMaximize()
  }

  fun toggleVisibleWindow(win: WindowController, visible: Boolean? = null) =
    winLifecycleScopeAsync(win) {
      win.simpleToggleVisible(visible)
    }

  fun closeWindow(win: WindowController, force: Boolean = false) = winLifecycleScopeAsync(win) {
    win.simpleClose(force)
  }

  fun windowSetStyle(
    win: WindowController,
    style: WindowStyle,
  ) = viewController.lifecycleScope.async {
    win.simpleSetStyle(style)
  }

  fun windowEmitGoBack(win: WindowController) = viewController.lifecycleScope.async {
    win.simpleEmitGoBack()
  }

  fun windowEmitGoForward(win: WindowController) = viewController.lifecycleScope.async {
    win.simpleEmitGoForward()
  }

  fun windowHideCloseTip(win: WindowController) = viewController.lifecycleScope.async {
    win.simpleHideCloseTip()
  }

  fun windowToggleMenuPanel(win: WindowController, show: Boolean?) =
    viewController.lifecycleScope.async {
      win.simpleToggleMenuPanel(show)
    }

  fun windowToggleAlwaysOnTop(win: WindowController, onTop: Boolean?) =
    viewController.lifecycleScope.async {
      win.simpleToggleAlwaysOnTop(onTop)
    }

  fun windowToggleColorScheme(win: WindowController, colorScheme: WindowColorScheme?) =
    viewController.lifecycleScope.async {
      win.simpleToggleColorScheme(colorScheme)
    }
}