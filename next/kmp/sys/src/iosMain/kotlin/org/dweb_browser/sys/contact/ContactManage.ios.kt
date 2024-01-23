package org.dweb_browser.sys.contact

import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.permission.AuthorizationStatus
import org.dweb_browser.helper.WARNING
import org.dweb_browser.sys.permission.SystemPermissionAdapterManager
import org.dweb_browser.sys.permission.SystemPermissionName

actual class ContactManage {
  init {
    SystemPermissionAdapterManager.append {
      when (task.name) {
        SystemPermissionName.CONTACTS -> AuthorizationStatus.GRANTED
        else -> null
      }
    }
  }

  actual suspend fun pickContact(microModule: MicroModule): ContactInfo? {
    WARNING("Not yet implemented")
    return null
  }
}