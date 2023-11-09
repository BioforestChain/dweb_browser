package org.dweb_browser.sys.window.core.modal

import kotlinx.coroutines.flow.SharedFlow
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.sys.window.core.BottomSheetsModal
import org.dweb_browser.sys.window.core.ModalCallback

class WindowBottomSheetsController(
  mm: NativeMicroModule,
  modal: BottomSheetsModal,
  onCallback: SharedFlow<ModalCallback>,
) : WindowModalController(mm, modal, onCallback) {

}

