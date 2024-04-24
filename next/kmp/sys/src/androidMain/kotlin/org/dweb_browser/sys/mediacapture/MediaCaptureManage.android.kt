package org.dweb_browser.sys.mediacapture

import android.Manifest
import android.annotation.SuppressLint
import io.ktor.utils.io.jvm.javaio.toByteReadChannel
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.helper.getAppContextUnsafe
import org.dweb_browser.pure.http.PureStream
import org.dweb_browser.sys.permission.AndroidPermissionTask
import org.dweb_browser.sys.permission.PermissionActivity
import org.dweb_browser.sys.permission.SystemPermissionAdapterManager
import org.dweb_browser.sys.permission.SystemPermissionName

actual class MediaCaptureManage actual constructor() {
  init {
    SystemPermissionAdapterManager.append {
      when (task.name) {
        SystemPermissionName.CAMERA -> {
          PermissionActivity.launchAndroidSystemPermissionRequester(
            microModule,
            AndroidPermissionTask(listOf(Manifest.permission.CAMERA), task.title, task.description)
          ).values.firstOrNull()
        }

        SystemPermissionName.MICROPHONE -> {
          PermissionActivity.launchAndroidSystemPermissionRequester(
            microModule,
            AndroidPermissionTask(
              listOf(Manifest.permission.RECORD_AUDIO),
              task.title,
              task.description
            )
          ).values.firstOrNull()
        }

        else -> null
      }
    }
  }

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