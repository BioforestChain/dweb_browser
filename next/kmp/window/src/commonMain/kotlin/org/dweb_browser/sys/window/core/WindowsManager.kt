package org.dweb_browser.sys.window.core

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.helper.OffListener
import org.dweb_browser.helper.OrderDeferred
import org.dweb_browser.helper.compose.compositionChainOf
import org.dweb_browser.helper.platform.IPureViewBox
import org.dweb_browser.helper.platform.IPureViewController
import org.dweb_browser.helper.some
import org.dweb_browser.helper.trueAlso
import org.dweb_browser.helper.withScope
import org.dweb_browser.sys.window.core.constant.WindowsManagerScope
import org.dweb_browser.sys.window.core.constant.debugWindow

/**
 * 管理行为委托，用来实现多个窗口之间的互动行为
 * 比方说：
 * 1. 当一个窗口聚焦时，其它窗口会发生什么事情
 * 1. 当一个窗口不可见时，其它窗口会发生什么事情
 */
expect class WindowsManagerDelegate<T : WindowController>(
  manager: WindowsManager<T>,
) {
  suspend fun focusWindow(win: WindowController)
  suspend fun focusWindows(windows: List<T>)
  fun focusDesktop()
  suspend fun addedWindow(win: T, offListenerList: MutableList<OffListener<*>>)
}

/**
 * 窗口管理器，存储查询窗口，并根据行为委托器来对窗口进行操控管理
 */
open class WindowsManager<T : WindowController>(
  internal val viewController: IPureViewController,
  internal val viewBox: IPureViewBox,
) {
  private val delegate by lazy { WindowsManagerDelegate(this) }
  val state = WindowsManagerState(viewBox)
  val allWindowsFlow = MutableStateFlow(mapOf<T, WindowsManagerScope>())
  val allWindows get() = allWindowsFlow.value.keys
  private val reCombFlow = MutableSharedFlow<Unit>()

  /**
   * 一个已经根据 zIndex 排序完成的只读列表
   */
  val winListFlow = allWindowsFlow.combine(reCombFlow) { it, _ -> it }.map { windows ->
    windows.keys.filter { !it.state.alwaysOnTop }.sortedBy { it.state.zIndex }
  }.stateIn(viewBox.lifecycleScope, started = SharingStarted.Eagerly, initialValue = listOf())
  val winList get() = winListFlow.value

  /**
   * 置顶窗口，一个已经根据 zIndex 排序完成的只读列表
   */
  val topWinListFlow = allWindowsFlow.combine(reCombFlow) { it, _ -> it }.map { windows ->
    windows.keys.filter { it.state.alwaysOnTop }.sortedBy { it.state.zIndex }
  }.stateIn(viewBox.lifecycleScope, started = SharingStarted.Eagerly, initialValue = listOf())
  val topWinList = topWinListFlow.value

  /**
   * 存储最大化的窗口
   */
  val maximizedWinsFlow = allWindowsFlow.map { windows ->
    windows.keys.filter { it.isMaximized && it.isVisible }.toSet()
  }
    .stateIn(viewBox.lifecycleScope, started = SharingStarted.Eagerly, initialValue = setOf())
  val maximizedWins get() = maximizedWinsFlow.value

  /**
   * 将一个窗口添加进来管理
   */
  open suspend fun addNewWindow(win: T, autoFocus: Boolean = true) = withWindowLifecycleScope {
    // 更新窗口的管理者角色
    win.upsetManager(this@WindowsManager);
    /// 对窗口做一些启动准备
    val offListenerList = mutableListOf<OffListener<*>>()
    /// 窗口销毁的时候，做引用释放
    offListenerList += win.onClose {
      removeWindow(win)
      delegate.focusWindows(allWindows.filter { it.isVisible && it.isMaximized })
    }
    /// 存储窗口与它的 状态机（销毁函数）
    allWindowsFlow.value += win to WindowsManagerScope {
      for (off in offListenerList) {
        off()
      }
    }
    delegate.addedWindow(win, offListenerList)
    /// 第一次装载窗口，默认将它聚焦到最顶层
    if (autoFocus) {
      delegate.focusWindow(win) // void job
    }
  }

  /**
   * 移除一个窗口
   */
  suspend fun removeWindow(win: T) = removeWindows(setOf(win))

  suspend fun removeWindows(windows: Set<T>) = withWindowLifecycleScope {
    val rmWindows = allWindows.intersect(windows)
    rmWindows.isNotEmpty().trueAlso {
      allWindowsFlow.value = allWindowsFlow.value.filter {
        !(rmWindows.contains(it.key).trueAlso {
          it.value.doDestroy()
        })
      }
      reComb()
    }
  }

  /**
   * 将窗口迁移到另一个管理器中，并且维护这些窗口的状态
   */
  suspend fun moveWindowsTo(other: WindowsManager<T>, windows: Iterable<T>? = null) {
    if (other == this) {
      return
    }
    withWindowLifecycleScope {
      other.withWindowLifecycleScope {
        val moveWins = windows ?: winList
        removeWindows(moveWins.toSet())
        /// 窗口迁移
        for (win in moveWins) {
          other.addNewWindow(win, false)
        }
        reComb()
        other.reComb()
        debugWindow(
          "moveWindows",
          "self:${winList.size}/${topWinList.size} => other: ${other.winList.size}/${other.topWinList.size}"
        )
      }
    }
  }

  /**
   * 依据窗口的属性，对窗口进行分组并排序，并对zIndex属性进行调整
   */
  suspend fun reComb() = withWindowLifecycleScope {
    /// 根据 alwaysOnTop 进行分组
    val winList = mutableListOf<T>()
    val winListTop = mutableListOf<T>()
    for (win in allWindows) {
      if (win.state.alwaysOnTop) {
        winListTop += win
      } else {
        winList += win
      }
    }

    /// 对窗口的 zIndex 进行重新赋值
    fun List<T>.setByZIndex(): Int {
      var changes = 0
      val sortedList = sortedBy { it.state.zIndex }
      for ((index, win) in sortedList.withIndex()) {
        if (win.state.zIndex != index) {
          win.state.zIndex = index
          changes += 1 // 每一次改变都进行一次标记
        }
      }
      return changes
    }
    if (winList.setByZIndex() + winListTop.setByZIndex() != 0) {
      reCombFlow.emit(Unit)
    }
  }

  suspend fun reFocus() {
    delegate.focusWindows(allWindows.filter { it.isVisible && it.isMaximized })
  }

  /**
   * 将指定窗口移动到最上层
   */
  suspend fun moveToTop(win: WindowController) {
    /// 窗口被聚焦，那么遍历所有的窗口，为它们重新生成zIndex值
    win.state.zIndex += allWindows.size
    reComb()
  }

  /**
   * 顺序执行窗口相关的操作，避免并发异常
   */
  private val orderDeferred = OrderDeferred()

  //  internal fun <R> withWindowLifecycleScopeAsync(block: suspend CoroutineScope.() -> R) =
//    viewBox.lifecycleScope.async(start = CoroutineStart.UNDISPATCHED) {
//      orderDeferred.queueAndAwait(null) {
//        // TODO 检测 win 的所属权
//        block()
//      }
//    }
  private var inQueue by atomic(false)
  internal suspend fun <R> withWindowLifecycleScope(block: suspend CoroutineScope.() -> R) =
    withScope(viewBox.lifecycleScope) {
      if (inQueue) {
        block()
      } else {
        inQueue = true
        try {
          orderDeferred.queueAndAwait(null) {
            // TODO 检测 win 的所属权
            block()
          }
        } finally {
          inQueue = false
        }
      }
    }
  /**
   * 对一个窗口做失焦操作
   */
  /**
   * 对一些窗口做聚焦操作
   */
  suspend fun focusWindow(mmid: MMID) {
    val windows = findWindows(mmid)
    delegate.focusWindows(windows)
  }

  open fun findWindows(mmid: MMID) =
    allWindows.filter { win -> win.state.constants.owner == mmid }.sortedBy { it.state.zIndex }

  /**
   * 获取窗口状态
   */
  fun getWindowStates(mmid: MMID): List<WindowState> {
    val states = mutableListOf<WindowState>()
    for (win in allWindows) {
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
        win.unMaximize()
      } else {
        win.maximize()
      }
    }
    return !isMaximized
  }

  suspend fun maximizeWindow(win: WindowController) = withWindowLifecycleScope {
    delegate.focusWindow(win)
    win.maximize()
  }

  fun focusDesktop() {
    delegate.focusDesktop()
  }
}

val LocalWindowsManager = compositionChainOf<WindowsManager<*>>("WindowsManager")
