package org.dweb_browser.browser.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
actual fun SingleChoiceSegmentedButtonRow(
  modifier: Modifier,
  content: @Composable RowScope.() -> Unit
) {
  Row(
    modifier = modifier
      .selectableGroup()
      .defaultMinSize(minHeight = 40.dp)
      .width(IntrinsicSize.Min),
    horizontalArrangement = Arrangement.spacedBy((-1).dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    content()
  }
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
  val interactionSource = remember { MutableInteractionSource() }
  Surface(
    modifier = modifier
      .weight(1f)
      .defaultMinSize(
        minWidth = ButtonDefaults.MinWidth,
        minHeight = ButtonDefaults.MinHeight
      )
      .semantics { role = Role.RadioButton },
    selected = selected,
    onClick = onClick,
    enabled = enabled,
    shape = shape,
    color = containerColor(enabled, selected),
    contentColor = contentColor(enabled, selected),
    border = BorderStroke(width = 1.dp, color = borderColor(enabled, selected)),
    interactionSource = interactionSource
  ) {
    Row {
      icon()
      label()
    }
  }
}

private const val CheckedZIndexFactor = 5f
private fun Modifier.interactionZIndex(checked: Boolean, interactionCount: State<Int>) =
  this.layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    layout(placeable.width, placeable.height) {
      val zIndex = interactionCount.value + if (checked) CheckedZIndexFactor else 0f
      placeable.place(0, 0, zIndex)
    }
  }

@Composable
private fun containerColor(enabled: Boolean, active: Boolean): Color {
  return when {
    enabled && active -> MaterialTheme.colorScheme.secondaryContainer
    enabled && !active -> MaterialTheme.colorScheme.surface
    !enabled && active -> MaterialTheme.colorScheme.secondaryContainer
    else -> MaterialTheme.colorScheme.surface
  }
}

@Composable
private fun contentColor(enabled: Boolean, checked: Boolean): Color {
  return when {
    enabled && checked -> MaterialTheme.colorScheme.onSecondaryContainer
    enabled && !checked -> MaterialTheme.colorScheme.onSurface
    !enabled && checked -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
  }
}

@Composable
private fun borderColor(enabled: Boolean, active: Boolean): Color {
  return when {
    enabled && active -> MaterialTheme.colorScheme.outline
    enabled && !active -> MaterialTheme.colorScheme.outline
    !enabled && active -> MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
    else -> MaterialTheme.colorScheme.outline
  }
}