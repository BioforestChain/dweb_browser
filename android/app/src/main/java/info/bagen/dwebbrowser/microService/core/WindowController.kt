package info.bagen.dwebbrowser.microService.core

import android.content.Context
import org.dweb_browser.helper.SimpleSignal

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

  private val _blurSignal = SimpleSignal()
  val onBlur = _blurSignal.toListener()
  private val _focusSignal = SimpleSignal()
  val onFocus = _focusSignal.toListener()
  fun isFocused() = state.focus
  suspend fun focus() {
    if (!state.focus) {
      state.focus = true
      _focusSignal.emit()
    }
  }

  suspend fun blur() {
    if (state.focus) {
      state.focus = false
      _blurSignal.emit()
    }
  }

  fun isMaximized() = state.maximize

  private val _maximizeSignal = SimpleSignal()
  val onMaximize = _maximizeSignal.toListener()
  suspend fun maximize() {
    if (!state.maximize) {
      state.maximize = true
      state.fullscreen = false
      state.minimize = false
      beforeMaximizeBounds = state.bounds.copy()
      _maximizeSignal.emit()
    }
  }

  private var beforeMaximizeBounds: WindowBounds? = null

  private val _unMaximizeSignal = SimpleSignal()

  /**
   * 当窗口从最大化状态退出时触发
   * Emitted when the window exits from a maximized state.
   */
  val onUnMaximize = _unMaximizeSignal.toListener()

  /**
   * 取消窗口最大化
   */
  suspend fun unMaximize() {
    if (state.maximize) {
      when (val value = beforeMaximizeBounds) {
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
          beforeMaximizeBounds = null
        }
      }
      state.maximize = false
      _unMaximizeSignal.emit()
    }
  }

  private val _minimizeSignal = SimpleSignal()
  val onMinimize = _minimizeSignal.toListener()
  suspend fun minimize() {
    if (!state.minimize) {
      state.minimize = true
      state.maximize = false
      state.fullscreen = false
      _minimizeSignal.emit()
    }
  }

  protected val _closeSignal = SimpleSignal()
  val onClose = _closeSignal.toListener()
  private var _isClosed = false
  fun isClosed() = _isClosed
  suspend fun close(force: Boolean = false) {
    /// 这里的 force 暂时没有作用，未来会加入交互，来阻止窗口关闭
    this._isClosed = true
    this._closeSignal.emitAndClear(Unit)
  }
}