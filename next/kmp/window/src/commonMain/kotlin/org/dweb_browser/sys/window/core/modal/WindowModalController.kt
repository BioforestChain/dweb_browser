package org.dweb_browser.sys.window.core.modal

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.SharedFlow
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.SimpleSignal
import org.dweb_browser.helper.buildUrlString

sealed class WindowModalController(
  val mm: NativeMicroModule.NativeRuntime,
  val modal: ModalState,
  val wid: String,
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
  protected inline suspend fun awaitState(crossinline isComplete: (WindowModalState) -> Boolean) =
    CompletableDeferred<Unit>().also { waiter ->
      val off = onStateChange {
        if (isComplete(it)) {
          waiter.complete(Unit)
          offListener()
        }
      }
      if (isComplete(_state)) {
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
      // 监听状态更新并且触发相关事件
      mm.scopeLaunch(cancelable = false) {
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
    // 在回调事件触发的时候更新state
    mm.scopeLaunch(cancelable = true) {
      onCallback.collect {
        state = when (it) {
          is CloseAlertModalCallback -> WindowModalState.CLOSE
          is CloseModalCallback -> WindowModalState.CLOSE
          is OpenModalCallback -> WindowModalState.OPEN
          is DestroyModalCallback -> WindowModalState.DESTROY
        }
      }
    }
  }

  val isDestroyed get() = isState(WindowModalState.DESTROYING, WindowModalState.DESTROY)
  suspend fun open() {
    if (isDestroyed || isState(WindowModalState.OPEN, WindowModalState.OPENING)) {
      return
    }
    state = WindowModalState.OPENING
    mm.nativeFetch(buildUrlString("file://window.sys.dweb/openModal") {
      parameters["modalId"] = modal.modalId
      parameters["wid"] = wid
    }).boolean()
  }

  suspend fun close() {
    if (isDestroyed || isState(WindowModalState.CLOSE, WindowModalState.CLOSING)) {
      return
    }
    state = WindowModalState.CLOSING
    mm.nativeFetch(buildUrlString("file://window.sys.dweb/closeModal") {
      parameters["modalId"] = modal.modalId
      parameters["wid"] = wid
    }).boolean()
  }

  suspend fun setCloseTip(closeTip: String?) {
    if (isDestroyed) {
      return
    }
    mm.nativeFetch(buildUrlString("file://window.sys.dweb/updateModalCloseTip") {
      parameters["modalId"] = modal.modalId
      closeTip?.let { parameters["closeTip"] = it }
    }).boolean()
  }

  suspend fun destroy() {
    if (isDestroyed) {
      return
    }
    this.state = WindowModalState.DESTROYING
    mm.nativeFetch(buildUrlString("file://window.sys.dweb/removeModal") {
      parameters["modalId"] = modal.modalId
    }).boolean()
    this.state = WindowModalState.DESTROY;
  }
}