package org.dweb_browser.sys.filechooser

import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.permission.AuthorizationStatus
import org.dweb_browser.sys.permission.SystemPermissionAdapterManager
import org.dweb_browser.sys.permission.SystemPermissionName

actual class FileChooserManage {
  init {
    SystemPermissionAdapterManager.append {
      if (task.name == SystemPermissionName.FILE_CHOOSER) {
        AuthorizationStatus.GRANTED
      } else null
    }
  }

  actual suspend fun openFileChooser(
    microModule: MicroModule, mimeTypes: String, multiple: Boolean, limit: Int
  ): List<String> {
    return FileChooserActivity.launchAndroidFileChooser(microModule, mimeTypes, multiple, limit)
  }
}
