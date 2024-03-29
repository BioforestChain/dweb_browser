package org.dweb_browser.sys.window.core.modal

import kotlinx.coroutines.flow.SharedFlow
import org.dweb_browser.core.module.NativeMicroModule

class WindowBottomSheetsController(
  mm: NativeMicroModule.NativeRuntime,
  modal: BottomSheetsModalState,
  wid: String,
  onCallback: SharedFlow<ModalCallback>,
) : WindowModalController(mm, modal, wid, onCallback) {

}

