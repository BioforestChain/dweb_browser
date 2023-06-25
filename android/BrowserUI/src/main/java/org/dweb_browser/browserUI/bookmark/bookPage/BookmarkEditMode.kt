package org.dweb_browser.browserUI.bookmark.bookPage

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import org.dweb_browser.browserUI.bookmark.Bookmark

@Composable
fun BookmarkEditModeDropdownMenu(modifier: Modifier = Modifier) {
  var showEditOptions by LocalBookmarkEditModel.current.showMoreOptions
  var showEditDialog by LocalBookmarkEditModel.current.showEditDialog
  val surfaceColor = MaterialTheme.colorScheme.surface;
  MaterialTheme(
    shapes = MaterialTheme.shapes.copy(extraSmall = MaterialTheme.shapes.large),
    colorScheme = MaterialTheme.colorScheme.copy(surface = Color.Transparent)
  ) {
    val density = LocalDensity.current.density;
    DropdownMenu(
      modifier = modifier
        .background(surfaceColor)
        /// 对冲 DropdownMenuVerticalPadding
        .layout { measurable, constraints ->
          val placeable = measurable.measure(constraints)
          layout(
            width = placeable.width,
            height = (placeable.height - 16 * density).toInt(),
          ) {
            placeable.placeRelative(0, (-8 * density).toInt())
          }
        },
      properties = remember { PopupProperties(focusable = true, clippingEnabled = false) },
      expanded = showEditOptions, onDismissRequest = { showEditOptions = false }) {
      DropdownMenuItem(modifier = Modifier,
        text = { Text("编辑") },
        onClick = {
          showEditOptions = false
          showEditDialog = true
        },
        leadingIcon = {
          Icon(
            Icons.Outlined.Edit, contentDescription = "编辑"
          )
        })
      Spacer(Modifier.height(1.dp))
      DropdownMenuItem(
        colors = MenuDefaults.itemColors(
          textColor = MaterialTheme.colorScheme.error,
          leadingIconColor = MaterialTheme.colorScheme.error
        ),
        text = { Text("删除") },
        onClick = { showEditOptions = false },
        leadingIcon = {
          Icon(
            Icons.Outlined.Delete, contentDescription = "删除"
          )
        })
    }
  }
}

@Preview(heightDp = 200, widthDp = 150)
@Composable
fun PreviewBookmarkEditModeDropdownMenu() {
  val bookmarkEditModel = remember {
    BookmarkEditModel(
      Bookmark(
        title = "Example Title", url = "https://google.com",
      )
    )
  }
  bookmarkEditModel.showMoreOptions.value = true
  Box(
    Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.primaryContainer),
    contentAlignment = Alignment.BottomEnd
  ) {
    Box(Modifier.background(Color.Red)) {
      CompositionLocalProvider(LocalBookmarkEditModel provides bookmarkEditModel) {
        BookmarkEditModeDropdownMenu()
      }
    }
  }
}

@Composable
fun BookmarkEditModeEditDialog() {
  var showEditDialog by LocalBookmarkEditModel.current.showEditDialog
  /// 编辑的对话框
  if (showEditDialog) {
    var data by LocalBookmarkEditModel.current.data
    var cloneData by remember { mutableStateOf(data.copy()) }

    AlertDialog(
      onDismissRequest = { showEditDialog = false },
      confirmButton = {
        val changed = cloneData != data
        OutlinedButton(
//          modifier = Modifier.border(
//            width = 1.dp,
//            color = if (changed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
//          ),
          border = if (changed) BorderStroke(
            width = ButtonDefaults.outlinedButtonBorder.width,
            color = MaterialTheme.colorScheme.error
          )
          else ButtonDefaults.outlinedButtonBorder,
          onClick = { showEditDialog = false },
        ) {
          Text("取消")
        }
        Button(enabled = changed, onClick = { data = cloneData }) {
          Text("保存")
        }
      },
      icon = {
        Icon(
          Icons.Outlined.Edit, contentDescription = "编辑"
        )
      },
      text = {
        Column {
          OutlinedTextField(value = cloneData.title,
            onValueChange = { cloneData = cloneData.copy(title = it) },
            label = { Text("标题") })

          OutlinedTextField(value = cloneData.url,
            onValueChange = { cloneData = cloneData.copy(url = it) },
            label = { Text("网址") })
        }
      },
    )
  }
}

@Preview
@Composable
fun PreviewBookmarkEditModeEditDialog() {
  val bookmarkEditModel = remember {
    BookmarkEditModel(
      Bookmark(
        title = "Example Title", url = "https://google.com",
      )
    )
  }
  bookmarkEditModel.showEditDialog.value = true;
  CompositionLocalProvider(LocalBookmarkEditModel provides bookmarkEditModel) {
    BookmarkEditModeEditDialog()
  }
}
