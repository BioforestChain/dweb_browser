package org.dweb_browser.window.render

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.launch

@Composable
fun WindowMenuItem(
  iconVector: ImageVector,
  labelText: String,
  selected: Boolean = false,
  selectedIconVector: ImageVector = iconVector,
  enabled: Boolean = true,
  onClick: suspend () -> Unit = {}
) {
  val scope = rememberCoroutineScope()
  val winTheme = LocalWindowControllerTheme.current

  val winMenuItemColor = remember(winTheme) {
    NavigationRailItemColors(
      unselectedIconColor = winTheme.onThemeContentDisableColor,
      selectedIconColor = winTheme.themeContentColor,
      selectedIndicatorColor = winTheme.onThemeContentColor,

      unselectedTextColor = winTheme.onThemeContentColor,
      selectedTextColor = winTheme.onThemeContentColor,

      disabledIconColor = winTheme.themeContentDisableColor,
      disabledTextColor = winTheme.themeContentDisableColor,
    )
  }

  NavigationRailItem(
    colors = winMenuItemColor,
    icon = {
      Icon(
        if (selected) selectedIconVector else iconVector,
        contentDescription = labelText,
      )
    },
    label = {
      Text(
        labelText,
        fontSize = MaterialTheme.typography.labelSmall.fontSize * 0.8f,
        textAlign = TextAlign.Center
      )
    },
    selected = selected,
    enabled = enabled,
    onClick = {
      scope.launch { onClick() }
    },
  )
}