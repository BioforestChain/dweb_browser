package org.dweb_browser.sys.shortcut

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AppShortcut
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.Json
import org.dweb_browser.helper.compose.ListSwipeItem
import org.dweb_browser.helper.compose.reorder.ReorderAbleItem
import org.dweb_browser.helper.compose.reorder.detectReorderAfterLongPress
import org.dweb_browser.helper.compose.reorder.rememberReorderAbleLazyListState
import org.dweb_browser.helper.compose.reorder.reorderAble
import org.dweb_browser.helper.getString
import org.dweb_browser.helper.platform.theme.DwebBrowserAppTheme

class ShortcutManageActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val shortcuts = try {
      Json.decodeFromString<List<SystemShortcut>>(this.getString("shortcuts"))
    } catch (_: Exception) {
      finish(); return
    }
    setContent {
      DwebBrowserAppTheme {
        // VerticalReorderList(shortcuts = shortcuts)
        ShortcutView(shortcuts) { item ->
          startActivity(Intent().apply {
            action = Intent.ACTION_VIEW
            `package` = packageName
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            data = Uri.parse(item.title)
          })
        }
      }
    }
  }

  override fun onPause() {
    super.onPause()
    finish()
  }
}

@Composable
private fun ShortcutView(shortcuts: List<SystemShortcut>, onOpen: (SystemShortcut) -> Unit) {
  LazyColumn {
    items(shortcuts) { item ->
      ListItem(
        modifier = Modifier.fillMaxWidth().height(72.dp).clickable { onOpen(item) },
        headlineContent = {
          Text(item.title)
        },
        supportingContent = {
          Text(item.mmid)
        },
        leadingContent = {
          item.iconImage?.let { Image(bitmap = it, contentDescription = "icon") }
            ?: Icon(Icons.Default.AppShortcut, contentDescription = "shortcut")
        },
      )
    }
  }
}

/**
 * 上下拖动排序 和 左右拖动删除 测试
 */
@Composable
private fun VerticalReorderList(modifier: Modifier = Modifier, shortcuts: List<SystemShortcut>) {
  var shortcutList by remember { mutableStateOf(shortcuts) }
  val state = rememberReorderAbleLazyListState(
    onMove = { from, to ->
      shortcutList = shortcutList.toMutableList().apply {
        add(to.index, removeAt(from.index))
      }
    },
    onDragEnd = { from, to ->
      // 由于 onMove 其实已经做了移动，这边只是对移动结果进行数据存储等操作
    },
    canDragOver = { from, to -> shortcutList.getOrNull(from.index) != null }
  )
  LazyColumn(state = state.listState, modifier = modifier.reorderAble(state)) {

    items(shortcutList, key = { it.title }) { item ->
      ReorderAbleItem(state, item.title) { dragging ->
        val elevation = animateDpAsState(if (dragging) 8.dp else 0.dp, label = "")
        ListSwipeItem(
          modifier = Modifier.detectReorderAfterLongPress(state)
            .shadow(elevation.value).fillMaxWidth().height(72.dp),
          onRemove = { shortcutList = shortcutList.toMutableList().apply { remove(item) } }
        ) {
          ListItem(
            headlineContent = {
              Text(item.title)
            },
            supportingContent = {
              Text(item.mmid)
            },
            leadingContent = {
              item.iconImage?.let { Image(bitmap = it, contentDescription = "icon") }
                ?: Icon(Icons.Default.AppShortcut, contentDescription = "shortcut")
            },
          )
        }

      }
    }
  }
}