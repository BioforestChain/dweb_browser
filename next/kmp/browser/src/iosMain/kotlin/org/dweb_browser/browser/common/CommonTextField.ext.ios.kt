package org.dweb_browser.browser.common

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActionScope
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue

@Composable
actual fun CommonTextField(
  value: TextFieldValue,
  onValueChange: (TextFieldValue) -> Unit,
  modifier: Modifier,
  enabled: Boolean,
  readOnly: Boolean,
  textStyle: TextStyle,
  singleLine: Boolean,
  maxLines: Int,
  minLines: Int,
  onKeyboardSearch: KeyboardActionScope.() -> Unit,
  decorationBox: @Composable (innerTextField: @Composable () -> Unit) -> Unit,
) {
  BasicTextField(
    value = value,
    onValueChange = onValueChange,
    modifier = modifier,
    enabled = enabled,
    readOnly = readOnly,
    textStyle = textStyle,
    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
    keyboardActions = KeyboardActions(onSearch = onKeyboardSearch),
    singleLine = singleLine,
    maxLines = maxLines,
    minLines = minLines,
    decorationBox = decorationBox
  )
}