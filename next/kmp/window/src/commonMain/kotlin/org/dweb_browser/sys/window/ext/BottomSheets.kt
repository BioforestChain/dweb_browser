package org.dweb_browser.sys.window.ext

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.launch
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.helper.SimpleSignal
import org.dweb_browser.helper.encodeURIComponent
import org.dweb_browser.helper.ifFalse
import org.dweb_browser.sys.window.core.BottomSheetsModal

enum class BottomSheetsState {
  INIT, OPENING, OPEN, CLOSING, CLOSE, DESTROYING, DESTROY,
}

class BottomSheets(
  val modal: BottomSheetsModal,
  onDismiss: Deferred<Unit>,
  val mm: NativeMicroModule,
) {
  private val onOpenSignal = SimpleSignal()
  private val onCloseSignal = SimpleSignal()
  private val onDestroySignal = SimpleSignal()
  private var _state = BottomSheetsState.INIT
  private fun isState(vararg states: BottomSheetsState) = states.contains(_state)
  var state
    get() = _state
    private set(value) {
      if (_state == value) {
        return
      }
      _state = value
      mm.ioAsyncScope.launch {
        when (_state) {
          BottomSheetsState.OPEN -> onOpenSignal.emit(Unit)
          BottomSheetsState.CLOSE -> onCloseSignal.emit(Unit)
          BottomSheetsState.DESTROY -> onDestroySignal.emit(Unit)
          else -> {}
        }
      }
    }

  init {
    onDismiss.invokeOnCompletion {
      isState(
        BottomSheetsState.CLOSE,
        BottomSheetsState.CLOSING,
        BottomSheetsState.DESTROYING,
        BottomSheetsState.DESTROY
      ).ifFalse {
        state = BottomSheetsState.DESTROY
      }
    }
  }

  val isDestroyed get() = isState(BottomSheetsState.DESTROYING, BottomSheetsState.DESTROY)
  suspend fun open() {
    if (isDestroyed || isState(BottomSheetsState.OPEN, BottomSheetsState.OPENING)) {
      return
    }
    state = BottomSheetsState.OPENING
    mm.nativeFetch("file://window.sys.dweb/openModal?modalId=${modal.modalId.encodeURIComponent()}")
      .boolean()
    state = BottomSheetsState.OPEN
  }

  suspend fun close() {
    if (isDestroyed || isState(BottomSheetsState.CLOSE, BottomSheetsState.CLOSING)) {
      return
    }
    state = BottomSheetsState.CLOSING
    mm.nativeFetch("file://window.sys.dweb/closeModal?modalId=${modal.modalId.encodeURIComponent()}")
      .boolean()
    state = BottomSheetsState.CLOSE
  }

  suspend fun destroy() {
    if (isDestroyed) {
      return
    }
    this.state = BottomSheetsState.DESTROYING;
    mm.nativeFetch("file://window.sys.dweb/removeModal?modalId=${modal.modalId.encodeURIComponent()}")
      .boolean();
    this.state = BottomSheetsState.DESTROY;
  }
}