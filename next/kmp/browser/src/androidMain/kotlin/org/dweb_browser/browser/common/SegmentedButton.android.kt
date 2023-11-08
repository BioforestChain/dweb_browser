package org.dweb_browser.browser.common

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun SingleChoiceSegmentedButtonRow(
  modifier: Modifier,
  content: @Composable RowScope.() -> Unit
) {
  androidx.compose.material3.SingleChoiceSegmentedButtonRow(
    modifier = modifier,
    content = { content() }
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun RowScope.SegmentedButton(
  selected: Boolean,
  onClick: () -> Unit,
  shape: Shape,
  modifier: Modifier,
  enabled: Boolean,
  icon: @Composable () -> Unit,
  label: @Composable () -> Unit,
) = with(this as androidx.compose.material3.SingleChoiceSegmentedButtonRowScope) {
  SegmentedButton(
    selected = selected,
    onClick = { onClick() },
    shape = shape,
    modifier = modifier,
    enabled = enabled,
    icon = { icon() },
    label = { label() }
  )
}