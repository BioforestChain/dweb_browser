package org.dweb_browser.dwebview

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.launch

/**
 * TODO IOS 如果外部scale了，而IDWebView就得调用 setContentScaleUnsafe，否则IOS平台下，modifier的scale、alpha不能正确适应
 */
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
      onCreate?.also {
        lifecycleScope.launch { onCreate(); }
      }
      viewWrapper
    },
    modifier = modifier,
    update = {},
    onRelease = {
      onDispose?.also {
        lifecycleScope.launch { onDispose(); }
      }
    },
  )
}
