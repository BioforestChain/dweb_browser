package org.dweb_browser.browser

import androidx.compose.ui.graphics.ImageBitmap

enum class BrowserIconResource {
  WebEngineDefault, WebEngineBaidu, WebEngineSougou, WebEngine360,
  BrowserStar, BrowserLauncher
  ;
}

/**
 * 通过定义的 BrowserIconResource 来获取图片
 */
expect fun getIconResource(resource: BrowserIconResource): ImageBitmap?

expect object LocalBitmapManager {
  /**
   * 根据id获取存储的照片信息，用于书签页照片本地化存储
   */
  fun loadImageBitmap(id: Long): ImageBitmap?

  /**
   * 根据id存储照片信息，存储于本地
   */
  fun saveImageBitmap(id: Long, imageBitmap: ImageBitmap): Boolean

  /**
   * 根据id删除存储的照片信息
   */
  fun deleteImageBitmap(id: Long): Boolean
}