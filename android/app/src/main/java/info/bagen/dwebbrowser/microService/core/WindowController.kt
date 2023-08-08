package info.bagen.dwebbrowser.microService.core

import android.content.Context
import org.dweb_browser.helper.SimpleSignal

abstract class WindowController {
  abstract val id: UUID

  /**
   * 在Android中，一个窗口对象必然附加在某一个Context/Activity中
   */
  abstract val context: Context
  abstract fun toJson(): WindowState

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