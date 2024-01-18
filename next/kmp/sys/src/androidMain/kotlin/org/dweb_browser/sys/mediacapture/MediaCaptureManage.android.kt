package org.dweb_browser.sys.mediacapture

import android.Manifest
import org.dweb_browser.core.module.MicroModule
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

  actual suspend fun takePicture(microModule: MicroModule): String {
    return MediaCaptureActivity.launchAndroidTakePicture(microModule)?.toString() ?: ""
  }

  actual suspend fun captureVideo(microModule: MicroModule): String {
    return MediaCaptureActivity.launchAndroidCaptureVideo(microModule)?.toString() ?: ""
  }

  actual suspend fun recordSound(microModule: MicroModule): String {
    return MediaCaptureActivity.launchAndroidRecordSound(microModule)?.toString() ?: ""
  }
}