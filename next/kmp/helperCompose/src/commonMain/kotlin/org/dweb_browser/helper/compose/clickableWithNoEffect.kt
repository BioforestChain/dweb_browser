package org.dweb_browser.helper.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

@Composable
fun Modifier.clickableWithNoEffect(enabled: Boolean = true, onClick: () -> Unit) = clickable(
  indication = null,
  enabled = enabled,
  onClick = onClick,
  interactionSource = remember { MutableInteractionSource() },
)