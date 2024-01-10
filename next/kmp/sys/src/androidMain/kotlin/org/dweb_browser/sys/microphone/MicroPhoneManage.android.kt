package org.dweb_browser.sys.microphone

import android.Manifest
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.sys.permission.AndroidPermissionTask
import org.dweb_browser.sys.permission.PermissionActivity
import org.dweb_browser.sys.permission.SystemPermissionAdapterManager
import org.dweb_browser.sys.permission.SystemPermissionName

actual class MicroPhoneManage {
  init {
    SystemPermissionAdapterManager.append {
      if (task.name == SystemPermissionName.MICROPHONE) {
        PermissionActivity.launchAndroidSystemPermissionRequester(
          microModule,
          AndroidPermissionTask(
            listOf(Manifest.permission.RECORD_AUDIO), task.title, task.description
          )
        ).values.firstOrNull()
      } else null
    }
  }

  actual suspend fun recordSound(microModule: MicroModule): String {
    return MicroPhoneActivity.launchAndroidRecordSound(microModule)
  }
}