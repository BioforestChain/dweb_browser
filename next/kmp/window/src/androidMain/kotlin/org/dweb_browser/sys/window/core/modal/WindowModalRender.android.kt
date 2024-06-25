package org.dweb_browser.sys.window.core.modal

import androidx.compose.runtime.Composable

@Composable
internal actual fun ModalState.RenderCloseTipImpl(onConfirmToClose: () -> Unit) {
  CommonRenderCloseTip(onConfirmToClose)
}

@Composable
internal actual fun BottomSheetsModalState.RenderImpl(emitModalVisibilityChange: (state: EmitModalVisibilityState) -> Boolean) {
  CommonRenderImpl(emitModalVisibilityChange)
}
