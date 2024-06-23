package org.dweb_browser.sys.shortcut

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AppShortcut
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.dweb_browser.helper.compose.LazySwipeAndReorderList
import org.dweb_browser.helper.compose.reorder.ItemPosition
import org.dweb_browser.sys.window.core.WindowContentRenderScope
import org.dweb_browser.sys.window.core.WindowContentScaffoldWithTitleText

@Composable
fun ShortcutManagerRender(
  modifier: Modifier = Modifier,
  windowRenderScope: WindowContentRenderScope,
  shortcutList: MutableList<SystemShortcut>,
  onDragMove: (ItemPosition, ItemPosition) -> Unit,
  onDragEnd: (startIndex: Int, endIndex: Int) -> Unit,
  onRemove: (SystemShortcut) -> Unit
) {
  windowRenderScope.WindowContentScaffoldWithTitleText(
    modifier,
    topBarTitleText = ShortcutI18nResource.shortcut_title(),
  ) { paddingValues ->
    LazySwipeAndReorderList(
      items = shortcutList,
      key = { item -> item.title },
      modifier = Modifier.padding(paddingValues),
      itemModifier = Modifier.fillMaxWidth().height(72.dp),
      onDragMove = onDragMove,
      onDragEnd = onDragEnd,
      onRemove = onRemove,
      colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
      headlineContent = { item -> Text(text = item.title) },
      leadingContent = { item ->
        item.iconImage?.let { iconImage ->
          Image(
            modifier = Modifier.size(72.dp), bitmap = iconImage, contentDescription = "shortcut"
          )
        } ?: Icon(Icons.Default.AppShortcut, contentDescription = "shortcut")
      },
      trailingContent = { _ ->
        Icon(Icons.Default.Reorder, contentDescription = "reorder")
      },
      noDataValue = ShortcutI18nResource.render_no_data()
    )
  }
}