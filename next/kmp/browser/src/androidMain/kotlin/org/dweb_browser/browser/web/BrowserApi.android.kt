package org.dweb_browser.browser.web

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissValue
import androidx.compose.material3.rememberSwipeToDismissState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import kotlinx.coroutines.flow.collect
import org.dweb_browser.browser.web.data.DESK_WEBLINK_ICONS
import org.dweb_browser.browser.web.model.BrowserViewModel
import org.dweb_browser.browser.web.view.BrowserViewForWindow
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.module.getAppContext
import org.dweb_browser.helper.BitmapUtil
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.sys.window.core.WindowRenderScope

actual fun ImageBitmap.toImageResource(): ImageResource? {
  val context = NativeMicroModule.getAppContext()
  return BitmapUtil.saveBitmapToIcons(context, this.asAndroidBitmap())?.let { src ->
    ImageResource(src = "$DESK_WEBLINK_ICONS$src")
  }
}

actual fun getImageResourceRootPath(): String {
  return NativeMicroModule.getAppContext().filesDir.absolutePath + "/icons"
}

@Composable
actual fun CommonBrowserView(
  viewModel: BrowserViewModel, modifier: Modifier, windowRenderScope: WindowRenderScope
) {
  BrowserViewForWindow(viewModel, modifier, windowRenderScope)
}

// TODO 由于版本androidx.compose 升级为 1.2.0-beta1 但是jetpack-compose版本没有出来，临时增加
@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun CommonSwipeDismiss(
  background: @Composable RowScope.() -> Unit,
  dismissContent: @Composable RowScope.() -> Unit,
  modifier: Modifier,
  onRemove: () -> Unit
) {
  val dismissState = rememberSwipeToDismissState()

  LaunchedEffect(dismissState) {
    snapshotFlow { dismissState }.collect {
      if (it.currentValue != SwipeToDismissValue.Settled) {
        onRemove()
      }
    }
  }

  SwipeToDismissBox(
    state = dismissState,
    backgroundContent = background,
    modifier = modifier,
    enableDismissFromStartToEnd = true,
    enableDismissFromEndToStart = true,
    content = dismissContent
  )
}