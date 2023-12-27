package org.dweb_browser.browser.web

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import org.dweb_browser.browser.web.model.DESK_WEBLINK_ICONS
import org.dweb_browser.browser.web.ui.view.BrowserViewForWindow
import org.dweb_browser.browser.web.ui.model.BrowserViewModel
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