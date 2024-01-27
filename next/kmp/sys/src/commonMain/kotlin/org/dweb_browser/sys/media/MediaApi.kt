package org.dweb_browser.sys.media

import org.dweb_browser.helper.platform.MultiPartFile
import org.dweb_browser.helper.platform.MultipartFieldDescription

expect suspend fun savePictures(saveLocation: String, files: List<MultiPartFile>)

expect fun MediaPicture.Companion.create(
  saveLocation: String, desc: MultipartFieldDescription
): MediaPicture

abstract class MediaPicture(saveLocation: String, desc: MultipartFieldDescription) {
  companion object {}

  abstract suspend fun consumePictureChunk(chunk: ByteArray)
  abstract suspend fun savePicture()
}
