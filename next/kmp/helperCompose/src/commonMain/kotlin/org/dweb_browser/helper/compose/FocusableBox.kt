package org.dweb_browser.helper.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.PointerIcon

private val defaultFocusState = object : FocusState {
  override val hasFocus: Boolean = false
  override val isCaptured: Boolean = false
  override val isFocused: Boolean = false
}

@Composable
fun FocusableBox(
  modifier: Modifier = Modifier,
  content: @Composable FocusableBoxScope.() -> Unit,
) {
  val focusRequester = remember { FocusRequester() }
  var focusState by remember { mutableStateOf<FocusState>(defaultFocusState) }
  BasicTextField("", {},
    readOnly = true,
    modifier = modifier
      .focusRequester(focusRequester)
      .onFocusChanged {
        focusState = it
      }
  ) {
    Box(Modifier.hoverCursor(PointerIcon.Default)) {
      FocusableBoxScope(
        focusRequester = focusRequester,
        focusState = focusState,
        boxScope = this
      ).content()
    }
  }
}

class FocusableBoxScope(
  val focusRequester: FocusRequester,
  focusState: FocusState,
  boxScope: BoxScope,
) : BoxScope by boxScope, FocusState by focusState {
  override fun toString(): String {
    return "FocusableBoxScope(isFocused=$isFocused; hasFocus=$hasFocus; isCaptured=$isCaptured)"
  }
}