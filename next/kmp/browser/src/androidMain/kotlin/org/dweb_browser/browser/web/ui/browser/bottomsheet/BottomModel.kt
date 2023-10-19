package org.dweb_browser.browser.web.ui.browser.bottomsheet

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.delay
import org.dweb_browser.browser.web.ui.browser.model.WebSiteInfo
import org.dweb_browser.browser.web.ui.browser.model.noLocalProvidedFor

val LocalModalBottomSheet = compositionLocalOf<ModalBottomModel> {
  noLocalProvidedFor("LocalModalBottomSheet")
}

enum class SheetState {
  Hidden, Expanded, PartiallyExpanded;

  fun defaultHeight(maxHeight: Float) = when (this) {
    Hidden -> 0f
    Expanded -> maxHeight
    else -> maxHeight * 2 / 3
  }
}

data class ModalBottomModel(
  val state: MutableState<SheetState>,
) {
  val show: MutableState<Boolean> = mutableStateOf(false)
  val tabIndex = mutableIntStateOf(0)
  val pageIndex = mutableIntStateOf(0)
  val webSiteInfo: MutableState<WebSiteInfo?> = mutableStateOf(null)

  suspend fun hide() {
    state.value = SheetState.Hidden
    pageIndex.intValue = 0
    delay(50)
    show.value = false
  }

  suspend fun show() {
    show.value = true
    delay(50)
    state.value = SheetState.PartiallyExpanded
  }

  suspend fun animTo() {

  }
}
