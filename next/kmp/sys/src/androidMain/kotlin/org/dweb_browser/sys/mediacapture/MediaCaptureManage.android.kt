package org.dweb_browser.sys.mediacapture

import android.annotation.SuppressLint
import io.ktor.utils.io.jvm.javaio.toByteReadChannel
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.helper.getAppContextUnsafe
import org.dweb_browser.pure.http.PureStream

actual class MediaCaptureManage actual constructor() {
  @SuppressLint("Recycle")
  actual suspend fun takePicture(microModule: MicroModule.Runtime): PureStream? =
    MediaCaptureActivity.launchAndroidTakePicture(microModule)?.let { uri ->
      val inputStream = getAppContextUnsafe().contentResolver.openInputStream(uri)
      inputStream?.let { PureStream(it.toByteReadChannel()) }
    }

  @SuppressLint("Recycle")
  actual suspend fun captureVideo(microModule: MicroModule.Runtime): PureStream? =
    MediaCaptureActivity.launchAndroidCaptureVideo(microModule)?.let { uri ->
      val inputStream = getAppContextUnsafe().contentResolver.openInputStream(uri)
      inputStream?.let { PureStream(it.toByteReadChannel()) }
    }

  @SuppressLint("Recycle")
  actual suspend fun recordSound(microModule: MicroModule.Runtime): PureStream? =
    MediaCaptureActivity.launchAndroidRecordSound(microModule)?.let { uri ->
      val inputStream = getAppContextUnsafe().contentResolver.openInputStream(uri)
      inputStream?.let { PureStream(it.toByteReadChannel()) }
    }
}