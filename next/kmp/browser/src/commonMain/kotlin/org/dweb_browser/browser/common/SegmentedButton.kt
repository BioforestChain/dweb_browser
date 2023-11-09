package org.dweb_browser.browser.common

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape

@Composable
expect fun SingleChoiceSegmentedButtonRow(
  modifier: Modifier = Modifier,
  content: @Composable RowScope.() -> Unit
)

@Composable
expect fun RowScope.SegmentedButton(
  selected: Boolean,
  onClick: () -> Unit,
  shape: Shape,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  icon: @Composable () -> Unit,
  label: @Composable () -> Unit,
)