package org.dweb_browser.sys.window.core.modal

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import org.dweb_browser.core.module.NativeMicroModule

class WindowAlertController(
  mm: NativeMicroModule,
  modal: AlertModalState,
  wid: String,
  onCallback: SharedFlow<ModalCallback>,
) : WindowModalController(mm, modal, wid, onCallback) {
  private var _result = false
  val result get() = _result

  init {
    onCallback.map {
      when (it) {
        is OpenModalCallback -> _result = false
        is CloseAlertModalCallback -> _result = it.confirm
        else -> {}
      }
    }.launchIn(mm.ioAsyncScope)
  }
}