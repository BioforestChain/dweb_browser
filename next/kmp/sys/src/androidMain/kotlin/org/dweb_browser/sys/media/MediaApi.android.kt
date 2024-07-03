package org.dweb_browser.sys.media

import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import org.dweb_browser.helper.base64Binary
import org.dweb_browser.helper.getAppContextUnsafe
import org.dweb_browser.helper.platform.MultiPartFile
import org.dweb_browser.helper.platform.MultipartFieldDescription
import java.io.File
import java.io.OutputStream

actual suspend fun savePictures(saveLocation: String, files: List<MultiPartFile>) {
  files.forEach { multiPartFile ->
    debugMedia("savePictures", multiPartFile)
    savePicture(multiPartFile, saveLocation)
  }
}

private suspend fun savePicture(
  multiPartFile: MultiPartFile, saveLocation: String,
) {
  // TODO 存储到系统
  val contentValues = ContentValues().apply {
    put(MediaStore.MediaColumns.DISPLAY_NAME, multiPartFile.name)
    put(MediaStore.MediaColumns.MIME_TYPE, multiPartFile.type)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      put(
        MediaStore.MediaColumns.RELATIVE_PATH,
        Environment.DIRECTORY_DCIM + File.separator + saveLocation
      )
      put(MediaStore.Video.Media.IS_PENDING, 1)
    }
  }
  val context = getAppContextUnsafe()
  // 插入图片到系统图库
  val imageUri =
    context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
  imageUri?.let {
    val outputStream = context.contentResolver.openOutputStream(imageUri)
    outputStream?.let {
      outputStream.write(multiPartFile.data.base64Binary)
      outputStream.flush()
      outputStream.close()
    }

    // TODO 更新相册系统库
    contentValues.clear()
    contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
    context.contentResolver.update(imageUri, contentValues, null, null)
  }
}

actual fun MediaPicture.Companion.create(
  saveLocation: String,
  desc: MultipartFieldDescription,
): MediaPicture = MediaPictureImpl(saveLocation, desc)

class MediaPictureImpl(saveLocation: String, desc: MultipartFieldDescription) :
  MediaPicture(saveLocation, desc) {
  private var outputStream: OutputStream? = null
  private val contentValues: ContentValues?
  private val imageUri: Uri?
  private val context = getAppContextUnsafe()

  init {
    contentValues = ContentValues().apply {
      put(MediaStore.MediaColumns.DISPLAY_NAME, desc.fileName)
      put(MediaStore.MediaColumns.MIME_TYPE, desc.contentType)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        put(
          MediaStore.MediaColumns.RELATIVE_PATH,
          Environment.DIRECTORY_DCIM + File.separator + saveLocation
        )
        put(MediaStore.Video.Media.IS_PENDING, 1)
      }
    }
    // 插入图片到系统图库

    imageUri =
      context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        ?.also { outputStream = context.contentResolver.openOutputStream(it) }
  }

  override suspend fun consumePictureChunk(chunk: ByteArray) {
    outputStream?.apply {
      write(chunk)
      flush()
    }
  }

  override suspend fun savePicture() {
    outputStream?.apply {
      close()
      contentValues?.apply {
        clear()
        put(MediaStore.Video.Media.IS_PENDING, 0)

        imageUri?.also {
          context.contentResolver.update(imageUri, contentValues, null, null)
        }
      }
    }
  }
}