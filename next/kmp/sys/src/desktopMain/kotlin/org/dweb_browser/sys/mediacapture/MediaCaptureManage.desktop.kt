package org.dweb_browser.sys.mediacapture

import io.ktor.utils.io.jvm.javaio.toByteReadChannel
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.pure.http.PureStream
import org.dweb_browser.sys.filechooser.FileChooserManage
import java.io.File

actual class MediaCaptureManage actual constructor() {
  private val fileChooser = FileChooserManage()
  private suspend fun openFileChooserAsPureStream(
    microModule: MicroModule.Runtime,
    accept: String
  ) =
    fileChooser.openFileChooser(microModule, accept, false).firstOrNull()?.let {
      val file = File(it)
      PureStream(file.inputStream().toByteReadChannel())
    }

  actual suspend fun takePicture(microModule: MicroModule.Runtime): PureStream? {
    return openFileChooserAsPureStream(microModule, "image/*")
  }

  actual suspend fun captureVideo(microModule: MicroModule.Runtime): PureStream? {
    return openFileChooserAsPureStream(microModule, "video/*")
  }

  actual suspend fun recordSound(microModule: MicroModule.Runtime): PureStream? {
    return openFileChooserAsPureStream(microModule, "audio/*")
  }
}