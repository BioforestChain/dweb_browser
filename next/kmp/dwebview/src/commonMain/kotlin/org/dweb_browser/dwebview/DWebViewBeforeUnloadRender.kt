package org.dweb_browser.dwebview

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue

@Composable
fun IDWebView.BeforeUnloadDialog() {
  var beforeUnloadMessageHook by remember { mutableStateOf<WebBeforeUnloadHook?>(null) }
  DisposableEffect(this) {
    val off = onBeforeUnload {
      beforeUnloadMessageHook = it.hook(this@BeforeUnloadDialog)
    }
    onDispose {
      off()
    }
  }
  val hook = beforeUnloadMessageHook ?: return

  AlertDialog(
    title = {
      Text(hook.message)// TODO i18n
    },
    text = { Text("系统可能不会保存您所做的更改。") },
    onDismissRequest = {
      hook.unloadDocument()
      beforeUnloadMessageHook = null
    },
    confirmButton = {
      Button(onClick = {
        hook.unloadDocument()
        beforeUnloadMessageHook = null
      }) {
        Text("确定")// TODO i18n
      }
    },
    dismissButton = {
      Button(onClick = {
        hook.keepDocument()
        beforeUnloadMessageHook = null
      }) {
        Text("留下")// TODO i18n
      }
    })
}