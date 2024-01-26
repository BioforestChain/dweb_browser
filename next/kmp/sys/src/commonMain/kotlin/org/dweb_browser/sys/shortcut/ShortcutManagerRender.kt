package org.dweb_browser.sys.shortcut

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AppShortcut
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import org.dweb_browser.helper.compose.LazySwipeAndReorderList
import org.dweb_browser.helper.compose.NoDataRender
import org.dweb_browser.helper.compose.reorder.ItemPosition
import org.dweb_browser.sys.window.core.WindowRenderScope

@Composable
fun ShortcutManagerRender(
  modifier: Modifier = Modifier,
  windowRenderScope: WindowRenderScope,
  shortcutList: MutableList<SystemShortcut>,
  onDragMove: (ItemPosition, ItemPosition) -> Unit,
  onDragEnd: (startIndex: Int, endIndex: Int) -> Unit,
  onRemove: (SystemShortcut) -> Unit
) {
  Box(
    modifier = modifier
      .fillMaxSize()
      .requiredSize(
        width = (windowRenderScope.width / windowRenderScope.scale).dp,
        height = (windowRenderScope.height / windowRenderScope.scale).dp
      ) // 原始大小
      .scale(windowRenderScope.scale)
  ) {
    if (shortcutList.isEmpty()) {
      NoDataRender(ShortcutI18nResource.render_no_data())
    } else {
      ShortcutListView(shortcutList, onDragMove, onDragEnd, onRemove)
    }
  }
}

@Composable
private fun ShortcutListView(
  shortcutList: MutableList<SystemShortcut>,
  onDragMove: (ItemPosition, ItemPosition) -> Unit,
  onDragEnd: (startIndex: Int, endIndex: Int) -> Unit,
  onRemove: (SystemShortcut) -> Unit
) {
  LazySwipeAndReorderList(
    items = shortcutList,
    key = { item -> item.uri },
    modifier = Modifier.fillMaxWidth().height(72.dp),
    onDragMove = onDragMove,
    onDragEnd = onDragEnd,
    onRemove = onRemove,
    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
    headlineContent = { item -> Text(text = item.title) },
    leadingContent = { item ->
      item.iconImage?.let { iconImage ->
        Image(
          modifier = Modifier.size(72.dp),
          bitmap = iconImage,
          contentDescription = "shortcut"
        )
      } ?: Icon(Icons.Default.AppShortcut, contentDescription = "shortcut")
    }
  )
}
