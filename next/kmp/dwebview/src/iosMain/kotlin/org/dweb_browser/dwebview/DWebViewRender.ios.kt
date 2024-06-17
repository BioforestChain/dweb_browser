package org.dweb_browser.dwebview

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.launch

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun IDWebView.Render(
  modifier: Modifier,
  onCreate: (suspend IDWebView.() -> Unit)?,
  onDispose: (suspend IDWebView.() -> Unit)?,
) {
  require(this is DWebView)
  UIKitView(
    factory = {
      viewWrapper.also {
        onCreate?.also {
          lifecycleScope.launch { onCreate(); }
        }
      }
    },
    modifier,
    update = {},
    onRelease = {
      onDispose?.also {
        lifecycleScope.launch { onDispose(); }
      }
    })
}