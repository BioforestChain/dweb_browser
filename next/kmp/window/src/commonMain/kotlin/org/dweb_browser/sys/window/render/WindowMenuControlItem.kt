package org.dweb_browser.sys.window.render

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.IconToggleButtonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.launch
import org.dweb_browser.helper.compose.compositionChainOf
import org.dweb_browser.sys.window.helper.LocalWindowControllerTheme
import org.dweb_browser.sys.window.helper.WindowControllerTheme

internal val LocalWindowMenuItemColor =
  compositionChainOf<IconToggleButtonColors?>("WindowMenuItemColor") { null }

@Composable
internal fun rememberWindowMenuItemColor() =
  LocalWindowMenuItemColor.current ?: LocalWindowControllerTheme.current.toWindowMenuItemColor()

@Composable
internal fun WindowControllerTheme.toWindowMenuItemColor() =
  IconButtonDefaults.iconToggleButtonColors(
    contentColor = onThemeContentColor,
    checkedContentColor = themeContentColor,
    checkedContainerColor = onThemeContentColor,
  )

@Composable
fun WindowMenuItem(
  iconVector: ImageVector,
  labelText: String,
  selected: Boolean = false,
  selectedIconVector: ImageVector = iconVector,
  enabled: Boolean = true,
  onSelectedChange: suspend (Boolean) -> Unit = {},
) {
  val scope = rememberCoroutineScope()
  val winTheme = LocalWindowControllerTheme.current

  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    val onCheckedChange: (Boolean) -> Unit = {
      scope.launch { onSelectedChange(it) }
    }
    val content = @Composable {
      Icon(
        if (selected) selectedIconVector else iconVector,
        contentDescription = labelText,
      )
    }
    val colors = rememberWindowMenuItemColor()
    if (enabled) {
      FilledIconToggleButton(
        checked = selected,
        enabled = enabled,
        colors = colors,
        onCheckedChange = onCheckedChange,
        content = content
      )
    } else {
      IconToggleButton(
        checked = selected,
        enabled = enabled,
        colors = colors,
        onCheckedChange = onCheckedChange,
        content = content
      )
    }
    Text(
      labelText,
      fontSize = MaterialTheme.typography.labelSmall.fontSize * 0.8f,
      textAlign = TextAlign.Center,
      color = if (enabled) winTheme.onThemeContentColor else winTheme.onThemeContentDisableColor,
    )
  }
}