package org.dweb_browser.browser

import androidx.compose.ui.graphics.ImageBitmap
import org.dweb_browser.helper.eprintln

actual fun getIconResource(resource: BrowserIconResource): ImageBitmap? {
//  resource()
//  val iconResource = when (resource) {
//    BrowserIconResource.WebEngineDefault -> R.drawable.ic_web
//    BrowserIconResource.WebEngineBaidu -> R.drawable.ic_engine_baidu
//    BrowserIconResource.WebEngineSougou -> R.drawable.ic_engine_sougou
//    BrowserIconResource.WebEngine360 -> R.drawable.ic_engine_360
//    BrowserIconResource.BrowserStar -> R.drawable.ic_main_star
//  }
//  return iconResourceMap.getOrPut(iconResource) {
//    BitmapUtil.decodeBitmapFromResource(NativeMicroModule.getAppContext(), iconResource)
//      ?.asImageBitmap()
//  }
  eprintln("Not yet implemented getIconResource")
  return null
}

actual object LocalBitmapManager {
  /**
   * 根据id获取存储的照片信息，用于书签页照片本地化存储
   */
  actual fun loadImageBitmap(id: Long): ImageBitmap? {
    TODO("Not yet implement loadImageBitmap")
  }

  /**
   * 根据id存储照片信息，存储于本地
   */
  actual fun saveImageBitmap(id: Long, imageBitmap: ImageBitmap): Boolean {
    TODO("Not yet implement saveImageBitmap")
  }

  /**
   * 根据id删除存储的照片信息
   */
  actual fun deleteImageBitmap(id: Long): Boolean {
    TODO("Not yet implement deleteImageBitmap")
  }
}