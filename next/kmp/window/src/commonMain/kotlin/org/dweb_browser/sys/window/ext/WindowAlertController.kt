package org.dweb_browser.sys.window.ext

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.sys.window.core.AlertModal
import org.dweb_browser.sys.window.core.CloseAlertModalCallback
import org.dweb_browser.sys.window.core.ModalCallback

class WindowAlertController(
  mm: NativeMicroModule,
  modal: AlertModal,
  onCallback: SharedFlow<ModalCallback>,
) : WindowModalController(mm, modal, onCallback) {
  private var _result = false
  val result get() = _result

  init {
    onCallback.map {
      when (it) {
        is CloseAlertModalCallback -> _result = it.confirm
        else -> {}
      }
    }.launchIn(mm.ioAsyncScope)
  }
}