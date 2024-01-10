package org.dweb_browser.sys.microphone

import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.permission.AuthorizationStatus
import org.dweb_browser.helper.WARNING
import org.dweb_browser.sys.permission.SystemPermissionAdapterManager
import org.dweb_browser.sys.permission.SystemPermissionName

actual class MicroPhoneManage {
  init {
    SystemPermissionAdapterManager.append {
      if (task.name == SystemPermissionName.MICROPHONE) {
        AuthorizationStatus.GRANTED
      } else null
    }
  }

  actual suspend fun recordSound(microModule: MicroModule): String {
    WARNING("Not yet Implements recordSound")
    return ""
  }
}