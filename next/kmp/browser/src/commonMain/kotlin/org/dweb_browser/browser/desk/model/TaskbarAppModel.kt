package org.dweb_browser.browser.desk.model

import androidx.compose.ui.graphics.ImageBitmap
import org.dweb_browser.helper.StrictImageResource

internal data class TaskbarAppModel(
  val mmid: String,
  val icon: StrictImageResource?,
  val running: Boolean,
  var isShowClose: Boolean = false,
) {
  companion object {
    internal val iconCache = mutableMapOf<String, ImageBitmap>()
    fun getCacheIcon(mmid: String) = iconCache[mmid]
    fun setCacheIcon(mmid: String, image: ImageBitmap) {
      iconCache[mmid] = image
    }
  }
}