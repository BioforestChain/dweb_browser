package org.dweb_browser.browser

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import org.dweb_browser.core.module.getAppContext
import org.dweb_browser.helper.BitmapUtil
import org.dweb_browser.helper.WeakHashMap
import org.dweb_browser.helper.getOrPut
import java.io.File
import java.io.FileOutputStream

/**
 * 避免同一个图片多次重复加载
 */
private val iconResourceMap = WeakHashMap<Int, ImageBitmap?>()
// private val iconResourceMap = WeakHashMap<BrowserIconResource, ImageBitmap?>()

actual fun getIconResource(resource: BrowserIconResource): ImageBitmap? {
  val iconResource = when (resource) {
    BrowserIconResource.WebEngineDefault -> R.drawable.ic_web
    BrowserIconResource.WebEngineBaidu -> R.drawable.ic_engine_baidu
    BrowserIconResource.WebEngineSougou -> R.drawable.ic_engine_sougou
    BrowserIconResource.WebEngine360 -> R.drawable.ic_engine_360
    BrowserIconResource.BrowserStar -> R.drawable.ic_main_star
    BrowserIconResource.BrowserLauncher -> R.mipmap.ic_launcher
  }
  return iconResourceMap.getOrPut(iconResource) {
    BitmapUtil.decodeBitmapFromResource(getAppContext(), iconResource)
      ?.asImageBitmap()
  }
}

actual object LocalBitmapManager {
  private fun getIconPath(id: Long): String {
    return getAppContext().filesDir.absolutePath + File.separator + "fav" + File.separator + "$id.png"
  }

  /**
   * 根据id获取存储的照片信息，用于书签页照片本地化存储
   */
  actual fun loadImageBitmap(id: Long): ImageBitmap? {
    try {
      return BitmapFactory.decodeFile(getIconPath(id)).asImageBitmap()
    } catch (e: Exception) {
      Log.e("LocalBitmapManager", "loadImageBitmap fail!! -> ${e.message}")
    }
    return null
  }

  /**
   * 根据id存储照片信息，存储于本地
   */
  actual fun saveImageBitmap(id: Long, imageBitmap: ImageBitmap): Boolean {
    try {
      val file = File(getIconPath(id))
      if (file.parentFile?.exists() == false) file.parentFile?.mkdirs()
      val fos = FileOutputStream(file)
      imageBitmap.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, fos)
      fos.flush()
      fos.close()
      return true
    } catch (e: Exception) {
      Log.e("LocalBitmapManager", "saveImageBitmap fail!! -> ${e.message}")
    }
    return false
  }

  /**
   * 根据id删除存储的照片信息
   */
  actual fun deleteImageBitmap(id: Long): Boolean {
    return File(getIconPath(id)).deleteRecursively()
  }
}