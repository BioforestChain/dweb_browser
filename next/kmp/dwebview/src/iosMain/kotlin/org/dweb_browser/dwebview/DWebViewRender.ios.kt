package org.dweb_browser.dwebview

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.launch
import org.dweb_browser.sys.window.render.LocalWindowContentStyle
import org.dweb_browser.sys.window.render.UIKitViewInWindow

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
  val windowContentStyle = LocalWindowContentStyle.current
  viewWrapper.UIKitViewInWindow(
    modifier = modifier,
    style = windowContentStyle.frameStyle,
    onInit = {
      onCreate?.also {
        lifecycleScope.launch { onCreate(); }
      }
    },
    update = {},
    onRelease = {
      onDispose?.also {
        lifecycleScope.launch { onDispose(); }
      }
    },
  )
}
