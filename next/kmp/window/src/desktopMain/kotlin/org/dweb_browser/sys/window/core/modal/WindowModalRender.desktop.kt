package org.dweb_browser.sys.window.core.modal

import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import org.dweb_browser.helper.compose.compositionChainOf
import org.dweb_browser.sys.window.helper.LocalWindowLimits
import kotlin.math.max

@Composable
internal actual fun ModalState.RenderCloseTipImpl(onConfirmToClose: () -> Unit) {
  // no here
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal actual fun BottomSheetsModalState.RenderImpl(
  emitModalVisibilityChange: (state: EmitModalVisibilityState) -> Boolean,
) {
  val mvc = remember {
    ModalViewController.from(this)
  }

  val windowLimits = LocalWindowLimits.current

  /**
   * 桌面端允许更大的宽度
   */
  val sheetMaxWidth = remember(windowLimits) {
    max(BottomSheetDefaults.SheetMaxWidth.value, windowLimits.maxWidth * 0.382f)
  }
  mvc.ShowModal(emitModalVisibilityChange) {
    width = sheetMaxWidth
    height = windowLimits.maxHeight
    alignment = Alignment.BottomCenter
  }
}

internal val LocalEmitModalVisibilityChange =
  compositionChainOf<(state: EmitModalVisibilityState) -> Boolean>("emitModalVisibilityChange")