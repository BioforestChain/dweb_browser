package org.dweb_browser.helper.compose

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged

@Composable
fun FocusableBox(
  modifier: Modifier = Modifier,
  content: @Composable FocusableBoxScope.() -> Unit,
) {
  val focusRequester = remember { FocusRequester() }
  var isFocus by remember { mutableStateOf(false) }
  BasicTextField("", {},
    readOnly = true,
    modifier = modifier
      .focusRequester(focusRequester)
      .onFocusChanged {
        isFocus = it.isFocused
      }
  ) {
    FocusableBoxScope(focusRequester, isFocus).content()
  }
}

class FocusableBoxScope(val focusRequester: FocusRequester, val isFocused: Boolean)
