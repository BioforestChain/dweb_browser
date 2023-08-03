package info.bagen.dwebbrowser.microService.browser.desk

import android.content.Context
import info.bagen.dwebbrowser.microService.core.WindowController
import info.bagen.dwebbrowser.microService.core.WindowState
import org.dweb_browser.helper.SimpleSignal

class DesktopWindowController(
  override val context: Context, internal val state: WindowState
) : WindowController() {
  override val id = state.wid;
  override fun toJson() = state
  private val _blurSignal = SimpleSignal()
  val onBlur = _blurSignal.toListener()
  private val _focusSignal = SimpleSignal()
  val onFocus = _focusSignal.toListener()
  fun isFocused() = state.focus
  suspend fun focus() {
    if (!state.focus) {
      state.focus = true
      state.emitChange()
      _focusSignal.emit()
    }
  }

  suspend fun blur() {
    if (state.focus) {
      state.focus = false
      state.emitChange()
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
      beforeMaximizeBounds = state.bounds
      state.emitChange()
      _maximizeSignal.emit()
    }
  }

  private var beforeMaximizeBounds: WindowState.WindowBounds? = null

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
          state.bounds.width /= 2
          state.bounds.left = state.bounds.width / 2
          state.bounds.height /= 2
          state.bounds.top = state.bounds.height / 2
        }

        else -> {
          state.bounds = value
          beforeMaximizeBounds = null
        }
      }
      state.maximize = false
      state.emitChange()
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
      state.emitChange()
      _minimizeSignal.emit()
    }
  }
}