package info.bagen.dwebbrowser.microService.core

import android.content.Context
import org.dweb_browser.helper.Observable

abstract class WindowController(
  /**
   * 窗口的基本信息
   */
  val state: WindowState,
) {
  /**
   * 在Android中，一个窗口对象必然附加在某一个Context/Activity中
   */
  abstract val context: Context
  val id = state.wid;
  fun toJson() = state


  //#region WindowMode相关的控制函数
  private fun <R> createStateListener(
    key: WindowPropertyKeys,
    filter: (WindowState.(Observable.Change<WindowPropertyKeys, *>) -> Boolean)? = null,
    map: (Observable.Change<WindowPropertyKeys, *>) -> R
  ) = state.observable.changeSignal.createChild(
    { (it.key == key) && (filter?.invoke(state, it) != false) }, map
  ).toListener()

  val onBlur = createStateListener(WindowPropertyKeys.Focus, { focus }) {}
  val onFocus = createStateListener(WindowPropertyKeys.Focus, { !focus }) {}
  fun isFocused() = state.focus
  suspend fun focus() {
    state.focus = true
  }

  suspend fun blur() {
    state.focus = false
  }

  val onModeChange = createStateListener(WindowPropertyKeys.Mode) {};

  fun isMaximized(mode: WindowMode = state.mode) =
    mode == WindowMode.MAXIMIZE || mode == WindowMode.FULLSCREEN

  val onMaximize = createStateListener(WindowPropertyKeys.Mode, { isMaximized(mode) }) {}
  suspend fun maximize() {
    if (!isMaximized()) {
      _beforeMaximizeBounds = state.bounds.copy()
      state.mode = WindowMode.MAXIMIZE
    }
  }

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
  }) {}

  /**
   * 取消窗口最大化
   */
  suspend fun unMaximize() {
    if (isMaximized()) {
      when (val value = _beforeMaximizeBounds) {
        null -> {
          state.bounds = with(state.bounds) {
            copy(
              left = width / 2,
              top = height / 2,
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

  fun isMinimize(mode: WindowMode = state.mode) =
    mode == WindowMode.MAXIMIZE || mode == WindowMode.FULLSCREEN

  val onMinimize = createStateListener(WindowPropertyKeys.Mode, { mode == WindowMode.MINIMIZE }) {}
  suspend fun minimize() {
    state.mode = WindowMode.MINIMIZE
  }

  val onClose = createStateListener(WindowPropertyKeys.Mode, { mode == WindowMode.CLOSED }) {}
  fun isClosed() = state.mode == WindowMode.CLOSED
  suspend fun close(force: Boolean = false) {
    /// 这里的 force 暂时没有作用，未来会加入交互，来阻止窗口关闭
    state.mode = WindowMode.CLOSED
  }

  //#endregion

  //#region 窗口样式修饰
  suspend fun setTopBarStyle(
    contentColor: String? = null,
    backgroundColor: String? = null,
    overlay: Boolean? = null
  ) {
    state.topBarContentColor = contentColor
    state.topBarBackgroundColor = backgroundColor
    state.overlayTopBar = overlay ?: state.overlayTopBar
  }
  //#endregion
}