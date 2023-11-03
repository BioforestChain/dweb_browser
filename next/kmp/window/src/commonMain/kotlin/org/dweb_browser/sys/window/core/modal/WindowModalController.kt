package org.dweb_browser.sys.window.core.modal

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.SimpleSignal
import org.dweb_browser.helper.encodeURIComponent
import org.dweb_browser.sys.window.core.CloseAlertModalCallback
import org.dweb_browser.sys.window.core.CloseModalCallback
import org.dweb_browser.sys.window.core.DestroyModalCallback
import org.dweb_browser.sys.window.core.ModalCallback
import org.dweb_browser.sys.window.core.ModalState
import org.dweb_browser.sys.window.core.OpenModalCallback

sealed class WindowModalController(
  val mm: NativeMicroModule,
  private val modal: ModalState,
  onCallback: SharedFlow<ModalCallback>,
) {
  protected val onOpenSignal = SimpleSignal()
  val onOpen = onOpenSignal.toListener()
  protected val onCloseSignal = SimpleSignal()
  val onClose = onCloseSignal.toListener()
  protected val onDestroySignal = SimpleSignal()
  val onDestroy = onDestroySignal.toListener()
  protected val onStateChangeSignal = Signal<WindowModalState>()
  val onStateChange = onStateChangeSignal.toListener()
  protected var _state = WindowModalState.INIT
  protected fun isState(vararg states: WindowModalState) = states.contains(_state)
  protected suspend fun awaitState(waitState: WindowModalState) =
    CompletableDeferred<Unit>().also { waiter ->
      val off = onStateChange {
        if (it == waitState) {
          waiter.complete(Unit)
          offListener()
        }
      }
      if (_state == waitState) {
        waiter.complete(Unit)
        off()
      }
    }.await()

  var state
    get() = _state
    protected set(value) {
      if (_state == value) {
        return
      }
      _state = value
      mm.ioAsyncScope.launch {
        onStateChangeSignal.emit(value)
        when (_state) {
          WindowModalState.OPEN -> onOpenSignal.emit(Unit)
          WindowModalState.CLOSE -> onCloseSignal.emit(Unit)
          WindowModalState.DESTROY -> {
            onCloseSignal.emitAndClear()
            onDestroySignal.emitAndClear()
            onOpenSignal.clear()
          }

          else -> {}
        }
      }
    }

  init {
    onCallback.map {
      state = when (it) {
        is CloseAlertModalCallback -> WindowModalState.CLOSE
        is CloseModalCallback -> WindowModalState.CLOSE
        is OpenModalCallback -> WindowModalState.OPEN
        is DestroyModalCallback -> WindowModalState.DESTROY
      }
    }.launchIn(mm.ioAsyncScope)
  }

  val isDestroyed get() = isState(WindowModalState.DESTROYING, WindowModalState.DESTROY)
  suspend fun open() {
    if (isDestroyed || isState(WindowModalState.OPEN, WindowModalState.OPENING)) {
      return
    }
    state = WindowModalState.OPENING
    mm.nativeFetch("file://window.sys.dweb/openModal?modalId=${modal.modalId.encodeURIComponent()}")
      .boolean()
    awaitState(WindowModalState.OPEN)
  }

  suspend fun close() {
    if (isDestroyed || isState(WindowModalState.CLOSE, WindowModalState.CLOSING)) {
      return
    }
    state = WindowModalState.CLOSING
    mm.nativeFetch("file://window.sys.dweb/closeModal?modalId=${modal.modalId.encodeURIComponent()}")
      .boolean()
    awaitState(WindowModalState.CLOSE)
  }

  suspend fun setCloseTip(closeTip: String?) {
    if (isDestroyed) {
      return
    }
    mm.nativeFetch("file://window.sys.dweb/updateModalCloseTip?modalId=${modal.modalId.encodeURIComponent()}&closeTip=${closeTip?.encodeURIComponent()}")
      .boolean()
  }

  suspend fun destroy() {
    if (isDestroyed) {
      return
    }
    this.state = WindowModalState.DESTROYING;
    mm.nativeFetch("file://window.sys.dweb/removeModal?modalId=${modal.modalId.encodeURIComponent()}")
      .boolean();
    this.state = WindowModalState.DESTROY;
  }
}