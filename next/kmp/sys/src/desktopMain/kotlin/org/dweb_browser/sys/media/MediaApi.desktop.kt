package org.dweb_browser.sys.media

import io.ktor.http.ContentType
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.dweb_browser.helper.platform.MultiPartFile
import org.dweb_browser.helper.platform.MultiPartFileEncode
import org.dweb_browser.helper.platform.MultipartFieldDescription
import org.dweb_browser.platform.desktop.os.OsType
import org.dweb_browser.platform.desktop.os.OsType.MacOS
import org.dweb_browser.platform.desktop.os.OsType.Windows
import java.io.File

private fun getPictureDir(saveLocation: String) = when (OsType.current) {
  MacOS -> File(System.getProperty("user.home"), "Pictures/Photos Library.photoslibrary/")
  Windows -> File(System.getProperty("user.home"), "Pictures/")
  else -> File("~/Pictures/")
}.resolve(saveLocation).apply { mkdirs() }

actual suspend fun savePictures(
  saveLocation: String, files: List<MultiPartFile>
) {
  val saveDir = getPictureDir(saveLocation)

  for (file in files) {
    saveDir.resolve(file.name).apply {
      when (file.encoding) {
        MultiPartFileEncode.UTF8 -> writeText(file.data)
        MultiPartFileEncode.BASE64 -> writeBytes(file.binary)
      }
    }
  }
}

actual fun MediaPicture.Companion.create(
  saveLocation: String,
  desc: MultipartFieldDescription
): MediaPicture = MediaPictureImpl(saveLocation, desc)

class MediaPictureImpl(saveLocation: String, desc: MultipartFieldDescription) :
  MediaPicture(saveLocation, desc) {
  private var file = getPictureDir(saveLocation).resolve(
    desc.fileName
      ?: (
          // 文件名
          Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).toString()
              // 文件后缀
              + (desc.contentType?.let { ".${ContentType.parse(it).contentSubtype}" } ?: "")
              // 临时文件后缀
              + ".tmp"
          )
  )

  override suspend fun consumePictureChunk(chunk: ByteArray) {
    file.appendBytes(chunk)
  }

  override suspend fun savePicture() {
    /// 移除 临时文件后缀
    file.renameTo(file.parentFile.resolve(file.nameWithoutExtension))
  }
}