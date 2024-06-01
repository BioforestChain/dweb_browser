package org.dweb_browser.browser.common

import androidx.compose.foundation.text.KeyboardActionScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue

@Composable
expect fun CommonTextField(
  value: TextFieldValue,
  onValueChange: (TextFieldValue) -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  readOnly: Boolean = false,
  textStyle: TextStyle = TextStyle.Default,
  singleLine: Boolean = false,
  maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
  minLines: Int = 1,
  onKeyboardSearch: KeyboardActionScope.() -> Unit,
  decorationBox: @Composable (innerTextField: @Composable () -> Unit) -> Unit =
    @Composable { innerTextField -> innerTextField() },
)