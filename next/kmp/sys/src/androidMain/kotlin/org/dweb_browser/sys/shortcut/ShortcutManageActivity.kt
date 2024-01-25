package org.dweb_browser.sys.shortcut

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AppShortcut
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.Json
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
        ShortcutView(shortcuts) { item ->
          startActivity(Intent().apply {
            action = Intent.ACTION_VIEW
            `package` = packageName
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            data = Uri.parse(item.uri)
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
private fun ShortcutView(shortcuts: List<SystemShortcut>, onOpen:(SystemShortcut) -> Unit) {
  LazyColumn {
    itemsIndexed(shortcuts) { index, item ->
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