package org.dweb_browser.browser.web

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import org.dweb_browser.browser.web.model.DESK_WEBLINK_ICONS
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.module.getAppContext
import org.dweb_browser.helper.BitmapUtil
import org.dweb_browser.helper.ImageResource

actual fun ImageBitmap.toImageResource(): ImageResource? {
  val context = NativeMicroModule.getAppContext()
  return BitmapUtil.saveBitmapToIcons(context, this.asAndroidBitmap())?.let { src ->
    ImageResource(src = "$DESK_WEBLINK_ICONS$src")
  }
}

actual fun getImageResourceRootPath(): String {
  return NativeMicroModule.getAppContext().filesDir.absolutePath + "/icons"
}
