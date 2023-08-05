package info.bagen.dwebbrowser.microService.core

import android.content.Context
import org.dweb_browser.helper.SimpleCallback
import org.dweb_browser.helper.SimpleSignal

abstract class WindowController {
  abstract val id: UUID

  /**
   * 在Android中，一个窗口对象必然附加在某一个Context/Activity中
   */
  abstract val context: Context
  abstract fun toJson(): WindowState

  protected val _destorySignal = SimpleSignal()
  fun onDestroy(cb: SimpleCallback) = _destorySignal.listen(cb)
  private var _isDestroyed = false
  fun isDestroyed() = _isDestroyed
  suspend fun close(force: Boolean = false) {
    /// 这里的 force 暂时没有作用，未来会加入交互，来阻止窗口关闭

    this._isDestroyed = true
    this._destorySignal.emitAndClear(Unit)
  }
}