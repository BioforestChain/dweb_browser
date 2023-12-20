package org.dweb_browser.sys.window.core.modal

import kotlinx.coroutines.flow.SharedFlow
import org.dweb_browser.core.module.NativeMicroModule

class WindowBottomSheetsController(
  mm: NativeMicroModule,
  modal: BottomSheetsModal,
  wid: String,
  onCallback: SharedFlow<ModalCallback>,
) : WindowModalController(mm, modal, wid, onCallback) {

}

