package org.dweb_browser.browser.common

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import org.dweb_browser.helper.WARNING

@Composable
actual fun SingleChoiceSegmentedButtonRow(
  modifier: Modifier,
  content: @Composable RowScope.() -> Unit
) {
  WARNING("Not yet implemented SingleChoiceSegmentedButtonRow")
}

@Composable
actual fun RowScope.SegmentedButton(
  selected: Boolean,
  onClick: () -> Unit,
  shape: Shape,
  modifier: Modifier,
  enabled: Boolean,
  icon: @Composable () -> Unit,
  label: @Composable () -> Unit,
) {
  WARNING("Not yet implemented SegmentedButton")
}