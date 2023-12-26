package org.dweb_browser.sys.media

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.module.getAppContext
import org.dweb_browser.helper.FilesUtil
import org.dweb_browser.helper.platform.MultiPartFile
import org.dweb_browser.helper.platform.MultiPartFileEncode
import org.dweb_browser.helper.toBase64ByteArray
import org.dweb_browser.helper.toUtf8ByteArray
import java.io.File


actual suspend fun savePictures(saveLocation: String?, files: List<MultiPartFile>) {
  files.forEach { multiPartFile ->
    savePicture(multiPartFile, saveLocation)
  }
}

private suspend fun savePicture(
  multiPartFile: MultiPartFile, saveLocation: String?
) {
  val data = when (multiPartFile.encode) {
    MultiPartFileEncode.UTF8 -> multiPartFile.data.toUtf8ByteArray()
    MultiPartFileEncode.BASE64 -> multiPartFile.data.toBase64ByteArray()
  }

  // TODO 存储到系统
  val rootPath = Environment.getExternalStoragePublicDirectory(
    when (saveLocation) {
      null -> Environment.DIRECTORY_DCIM
      else -> Environment.DIRECTORY_DCIM + "/$saveLocation"
    }
  ).absolutePath
  val filePath = rootPath + File.separator + multiPartFile.name
  FilesUtil.writeFileContent(filePath, data.decodeToString()) // 保存到文件
  // TODO 更新相册系统库
  val values = ContentValues()
  values.put(MediaStore.Images.Media.TITLE, "Image")
  values.put(MediaStore.Images.Media.DESCRIPTION, "Image Description")
  values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis())
  values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
  values.put(MediaStore.Images.Media.DATA, filePath)

  val context = NativeMicroModule.getAppContext()
  // 插入图片到系统图库
  context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
  // 发送广播通知图库更新
  context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse(filePath)))
}