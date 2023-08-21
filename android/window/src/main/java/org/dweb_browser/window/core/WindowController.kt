package org.dweb_browser.window.core

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import org.dweb_browser.helper.Observable
import org.dweb_browser.helper.defaultAsyncExceptionHandler
import org.dweb_browser.window.core.constant.WindowBottomBarStyle
import org.dweb_browser.window.core.constant.WindowBottomBarTheme
import org.dweb_browser.window.core.constant.WindowMode
import org.dweb_browser.window.core.constant.WindowPropertyKeys
import org.dweb_browser.window.core.constant.WindowTopBarStyle
import org.dweb_browser.window.core.constant.debugWindow

abstract class WindowController(
  /**
   * 窗口的基本信息
   */
  val state: WindowState,
  /**
   * 传入多窗口管理器，可以不提供，那么由 Controller 自身以缺省的逻辑对 WindowState 进行修改
   */
  manager: WindowsManager<*>? = null,
) {
  protected var _manager: WindowsManager<*>? = null

  /**
   * 窗口管理器
   * 默认情况下，WindowController 的接口只对 Manager 开放，所以如果有需要，请调用 manager 的接口来控制窗口
   */
  open val manager get() = _manager
  open fun upsetManager(manager: WindowsManager<*>?) {
    _manager = manager
  }

  private suspend fun <R> managerRunOr(
    withManager: (manager: WindowsManager<*>) -> Deferred<R>,
    orNull: suspend () -> R
  ) = when (_manager) {
    null -> orNull()
    else -> withManager(_manager!!).await()
  }

  init {
    this.upsetManager(manager)
  }

  /**
   * 在Android中，一个窗口对象必然附加在某一个Context/Activity中
   */
  abstract val context: Context

  /**
   * 需要提供一个生命周期对象
   */
  abstract val coroutineScope: CoroutineScope
  val id = state.wid;
  fun toJsonAble() = state


  //#region WindowMode相关的控制函数
  private fun <R> createStateListener(
    key: WindowPropertyKeys,
    filter: (WindowState.(Observable.Change<WindowPropertyKeys, *>) -> Boolean)? = null,
    map: (Observable.Change<WindowPropertyKeys, *>) -> R
  ) = state.observable.changeSignal.createChild(
    { (it.key == key) && (filter?.invoke(state, it) != false) }, map
  ).toListener()

  val onFocus =
    createStateListener(WindowPropertyKeys.Focus, { focus }) { debugWindow("emit onFocus", id) }
  val onBlur =
    createStateListener(WindowPropertyKeys.Focus, { !focus }) { debugWindow("emit onBlur", id) }

  fun isFocused() = state.focus
  internal open suspend fun simpleFocus() {
    state.focus = true
    // 如果窗口聚焦，那么要同时取消最小化的状态
    simpleUnMinimize()
  }

  suspend fun focus() = managerRunOr({ it.focusWindow(this) }, { simpleFocus() })


  internal open suspend fun simpleBlur() {
    state.focus = false
  }

  suspend fun blur() = managerRunOr({ it.focusWindow(this) }, { simpleBlur() })

  val onModeChange =
    createStateListener(WindowPropertyKeys.Mode) { debugWindow("emit onModeChange", "$id $it") };

  fun isMaximized(mode: WindowMode = state.mode) =
    mode == WindowMode.MAXIMIZE || mode == WindowMode.FULLSCREEN

  val onMaximize = createStateListener(WindowPropertyKeys.Mode,
    { isMaximized(mode) }) { debugWindow("emit onMaximize", id) }

  internal open suspend fun simpleMaximize() {
    if (!isMaximized()) {
      _beforeMaximizeBounds = state.bounds.copy()
      state.mode = WindowMode.MAXIMIZE
    }
  }

  suspend fun maximize() = managerRunOr({ it.maximizeWindow(this) }, { simpleMaximize() })

  private var _beforeMaximizeBounds: WindowBounds? = null

  /**
   * 记忆窗口最大化之前的记忆
   */
  val beforeMaximizeBounds get() = _beforeMaximizeBounds;


  /**
   * 当窗口从最大化状态退出时触发
   * Emitted when the window exits from a maximized state.
   */
  val onUnMaximize = createStateListener(WindowPropertyKeys.Mode, {
    !isMaximized(mode) && isMaximized(it.oldValue as WindowMode)
  }) { debugWindow("emit onUnMaximize", id) }

  /**
   * 取消窗口最大化
   */
  internal open suspend fun simpleUnMaximize() {
    if (isMaximized()) {
      when (val value = _beforeMaximizeBounds) {
        null -> {
          state.bounds = with(state.bounds) {
            copy(
              left = width / 4,
              top = height / 4,
              width = width / 2,
              height = height / 2,
            )
          }
        }

        else -> {
          state.bounds = value
          _beforeMaximizeBounds = null
        }
      }
      state.mode = WindowMode.FLOATING
    }
  }

  suspend fun unMaximize() = managerRunOr({ it.unMaximizeWindow(this) }, { simpleUnMaximize() })

  fun isMinimize(mode: WindowMode = state.mode) = mode == WindowMode.MINIMIZE

  private var _beforeMinimizeMode: WindowMode? = null

  val onMinimize = createStateListener(WindowPropertyKeys.Mode,
    { mode == WindowMode.MINIMIZE }) { debugWindow("emit onMinimize", id) }

  internal open suspend fun simpleUnMinimize() {
    if (isMinimize()) {
      state.mode = _beforeMinimizeMode ?: WindowMode.FLOATING
      _beforeMinimizeMode = null
    }
  }

  suspend fun unMinimize() = managerRunOr({ it.unMinimizeWindow(this) }, { simpleUnMinimize() })

  internal open suspend fun simpleMinimize() {
    state.mode = WindowMode.MINIMIZE
  }

  suspend fun minimize() = managerRunOr({ it.minimizeWindow(this) }, { simpleMinimize() })

  val onClose = createStateListener(WindowPropertyKeys.Mode,
    { mode == WindowMode.CLOSED }) { debugWindow("emit onClose", id) }

  fun isClosed() = state.mode == WindowMode.CLOSED
  internal open suspend fun simpleClose(force: Boolean = false) {
    /// 这里的 force 暂时没有作用，未来会加入交互，来阻止窗口关闭
    state.mode = WindowMode.CLOSED
  }

  suspend fun close(force: Boolean = false) =
    managerRunOr({ it.closeWindow(this, force) }, { simpleClose(force) })

//#endregion

  //#region 窗口样式修饰
  internal open suspend fun simpleSetTopBarStyle(style: WindowTopBarStyle) {
    with(style) {
      contentColor?.also { state.topBarContentColor = it }
      backgroundColor?.also { state.topBarBackgroundColor = it }
      overlay?.also { state.topBarOverlay = it }
    }
  }

  suspend fun setTopBarStyle(style: WindowTopBarStyle) = managerRunOr({
    it.windowSetTopBarStyle(
      this,
      style
    )
  }, { simpleSetTopBarStyle(style) })

  internal open suspend fun simpleSetBottomBarStyle(style: WindowBottomBarStyle) {
    with(style) {
      theme?.also { state.bottomBarTheme = WindowBottomBarTheme.from(it) }
      contentColor?.also { state.bottomBarContentColor = it }
      backgroundColor?.also { state.bottomBarBackgroundColor = it }
      overlay?.also { state.bottomBarOverlay = it }
    }
  }


  suspend fun setBottomBarStyle(style: WindowBottomBarStyle) =
    managerRunOr(
      { it.windowSetBottomBarStyle(this, style) },
      { simpleSetBottomBarStyle(style) })

//#endregion
}