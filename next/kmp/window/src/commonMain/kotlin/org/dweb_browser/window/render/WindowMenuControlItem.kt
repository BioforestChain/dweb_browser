package org.dweb_browser.window.render

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
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

  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    val onCheckedChange: (Boolean) -> Unit = {
      scope.launch { onClick() }
    }
    val content = @Composable {
      Icon(
        if (selected) selectedIconVector else iconVector,
        contentDescription = labelText,
      )
    }
    val colors =
      IconButtonDefaults.iconToggleButtonColors(contentColor = winTheme.themeContentColor)
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
      textAlign = TextAlign.Center
    )
  }
//  val winMenuItemColor = remember(winTheme) {
//    NavigationRailItemColors(
//      unselectedIconColor = winTheme.onThemeContentDisableColor,
//      selectedIconColor = winTheme.themeContentColor,
//      selectedIndicatorColor = winTheme.onThemeContentColor,
//
//      unselectedTextColor = winTheme.onThemeContentColor,
//      selectedTextColor = winTheme.onThemeContentColor,
//
//      disabledIconColor = winTheme.themeContentDisableColor,
//      disabledTextColor = winTheme.themeContentDisableColor,
//    )
//  }
//
//  NavigationRailItem(
//    colors = winMenuItemColor,
//    icon = {
//      Icon(
//        if (selected) selectedIconVector else iconVector,
//        contentDescription = labelText,
//      )
//    },
//    label = {
//      Text(
//        labelText,
//        fontSize = MaterialTheme.typography.labelSmall.fontSize * 0.8f,
//        textAlign = TextAlign.Center
//      )
//    },
//    selected = selected,
//    enabled = enabled,
//    onClick = {
//      scope.launch { onClick() }
//    },
//  )
}