package org.dweb_browser.browserUI.ui.browser.bottomsheet

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.delay
import org.dweb_browser.browserUI.ui.browser.noLocalProvidedFor

val LocalModalBottomSheet = compositionLocalOf<ModalBottomModel> {
  noLocalProvidedFor("LocalModalBottomSheet")
}

enum class SheetState {
  Hidden, Expanded, PartiallyExpanded;

  fun defaultHeight(maxHeight: Float) = when(this) {
    Hidden -> 0f
    Expanded -> maxHeight
    else -> maxHeight * 2 / 3
  }
}

data class ModalBottomModel(
  val state: MutableState<SheetState>,
) {
  val show: MutableState<Boolean> = mutableStateOf(false)

  suspend fun hide() {
    state.value = SheetState.Hidden
  }

  suspend fun show() {
    show.value = true
    delay(10)
    state.value = SheetState.PartiallyExpanded
  }

  suspend fun animTo() {

  }
}
