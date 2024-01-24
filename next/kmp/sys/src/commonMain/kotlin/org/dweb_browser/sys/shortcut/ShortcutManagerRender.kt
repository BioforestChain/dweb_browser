package org.dweb_browser.sys.shortcut

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Shortcut
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import org.dweb_browser.helper.compose.clickableWithNoEffect
import org.dweb_browser.sys.window.core.WindowRenderScope

@Composable
fun ShortcutManagerRender(
  modifier: Modifier,
  windowRenderScope: WindowRenderScope,
  shortcutList: MutableList<SystemShortcut>,
  onSwapItem: (Int, Int) -> Unit
) {
  Box(
    modifier = modifier
      .fillMaxSize()
      .requiredSize(
        (windowRenderScope.width / windowRenderScope.scale).dp,
        (windowRenderScope.height / windowRenderScope.scale).dp
      ) // 原始大小
      .scale(windowRenderScope.scale)
  ) {
    val size = shortcutList.size
    if (size == 0) {
      Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Icon(
            Icons.Rounded.Info,
            "no data",
            modifier = Modifier.size(42.dp),
            tint = LocalContentColor.current.copy(alpha = 0.5f)
          )
          Spacer(Modifier.height(24.dp))
          Text(
            text = ShortcutI18nResource.render_no_data(),
            style = MaterialTheme.typography.labelMedium
          )
        }
      }
      return
    }

    LazyColumn {
      itemsIndexed(shortcutList) { index, item ->
        debugShortcut("RenderLazy", "$index=>$item")
        val color =
          if (index % 2 == 0) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.background
        ListItem(
          modifier = Modifier.fillMaxWidth().height(72.dp).background(color),
          headlineContent = {
            Text(item.title)
          },
          leadingContent = {
            item.iconImage?.let { iconImage ->
              Image(
                modifier = Modifier.size(72.dp),
                bitmap = iconImage,
                contentDescription = "shortcut"
              )
            } ?: Icon(Icons.Default.Shortcut, contentDescription = "shortcut")
          },
          trailingContent = {
            Row(
              modifier = Modifier.width(64.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                modifier = Modifier.size(32.dp).clickableWithNoEffect {
                  if (index > 0) { onSwapItem(index, index - 1) }
                },
                imageVector = Icons.Default.ArrowUpward,
                contentDescription = "MoveUp",
                tint = if (index == 0) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.onBackground
              )
              Icon(
                modifier = Modifier.size(32.dp).clickableWithNoEffect {
                  if (index < size - 1) { onSwapItem(index, index + 1) }
                },
                imageVector = Icons.Default.ArrowDownward,
                contentDescription = "MoveDown",
                tint = if (index == size - 1) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.onBackground
              )
            }
          }
        )
      }
    }
  }
}