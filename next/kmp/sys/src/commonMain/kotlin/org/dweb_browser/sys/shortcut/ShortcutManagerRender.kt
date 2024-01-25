package org.dweb_browser.sys.shortcut

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AppShortcut
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import org.dweb_browser.helper.compose.ListSwipeItem
import org.dweb_browser.helper.compose.NoDataRender
import org.dweb_browser.helper.compose.reorder.ItemPosition
import org.dweb_browser.helper.compose.reorder.ReorderAbleItem
import org.dweb_browser.helper.compose.reorder.detectReorderAfterLongPress
import org.dweb_browser.helper.compose.reorder.rememberReorderAbleLazyListState
import org.dweb_browser.helper.compose.reorder.reorderAble
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
      ShortcutListView(Modifier.fillMaxWidth(), shortcutList, onDragMove, onDragEnd, onRemove)
    }
  }
}

@Composable
private fun ShortcutListView(
  modifier: Modifier = Modifier,
  shortcutList: MutableList<SystemShortcut>,
  onDragMove: (ItemPosition, ItemPosition) -> Unit,
  onDragEnd: (startIndex: Int, endIndex: Int) -> Unit,
  onRemove: (SystemShortcut) -> Unit
) {
  val state = rememberReorderAbleLazyListState(onMove = onDragMove, onDragEnd = onDragEnd)
  LazyColumn(
    state = state.listState,
    modifier = modifier.reorderAble(state)
  ) {
    items(shortcutList, key = { it.uri }) { item ->
      ReorderAbleItem(state, item.uri) { dragging ->
        val elevation = animateDpAsState(if (dragging) 8.dp else 0.dp, label = "")
        ListSwipeItem(
          modifier = Modifier.detectReorderAfterLongPress(state)
            .shadow(elevation.value).fillMaxWidth().height(72.dp),
          onRemove = { onRemove(item) }
        ) {
          ListItem(
            modifier = Modifier.fillMaxSize(),
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
            headlineContent = {
              Text(text = item.title)
            },
            leadingContent = {
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
      }
    }
  }
}
