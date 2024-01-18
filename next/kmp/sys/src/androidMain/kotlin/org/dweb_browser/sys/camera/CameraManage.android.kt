package org.dweb_browser.sys.camera

import android.Manifest
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.sys.permission.AndroidPermissionTask
import org.dweb_browser.sys.permission.PermissionActivity
import org.dweb_browser.sys.permission.SystemPermissionAdapterManager
import org.dweb_browser.sys.permission.SystemPermissionName

actual class CameraManage actual constructor() {
  init {
    SystemPermissionAdapterManager.append {
      if (task.name == SystemPermissionName.CAMERA) {
        PermissionActivity.launchAndroidSystemPermissionRequester(
          microModule,
          AndroidPermissionTask(listOf(Manifest.permission.CAMERA), task.title, task.description)
        ).values.firstOrNull()
      } else null
    }
  }

  actual suspend fun takePicture(microModule: MicroModule): String {
    return CameraActivity.launchAndroidTakePicture(microModule)?.toString() ?: ""
  }

  actual suspend fun captureVideo(microModule: MicroModule): String {
    return CameraActivity.launchAndroidCaptureVideo(microModule)?.toString() ?: ""
  }

  actual suspend fun getPhoto(microModule: MicroModule, options: ImageOptions): Photo? {
    return CameraActivity.launchAndroidGetPhoto(microModule)?.let { uri ->
      Photo()
    }
  }
}