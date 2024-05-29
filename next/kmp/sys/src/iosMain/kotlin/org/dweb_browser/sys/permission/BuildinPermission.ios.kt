package org.dweb_browser.sys.permission

import org.dweb_browser.core.std.permission.AuthorizationStatus
import org.dweb_browser.sys.filechooser.FileChooserManage.Companion.phonePermission
import org.dweb_browser.sys.location.LocationManage.Companion.locationPermission
import org.dweb_browser.sys.mediacapture.MediaCaptureManage.Companion.cameraPermission
import org.dweb_browser.sys.mediacapture.MediaCaptureManage.Companion.microPhonePermission

actual object BuildinPermission {
  private val unRequiredPermission: RequestSystemPermission = {
    when (task.name) {
      SystemPermissionName.CONTACTS, SystemPermissionName.STORAGE, SystemPermissionName.CLIPBOARD, SystemPermissionName.Notification -> AuthorizationStatus.GRANTED
      else -> null
    }
  }

  actual fun start() {
    SystemPermissionAdapterManager.append(adapter = unRequiredPermission)
    SystemPermissionAdapterManager.append(adapter = locationPermission)
    SystemPermissionAdapterManager.append(adapter = phonePermission)
    SystemPermissionAdapterManager.append(adapter = cameraPermission)
    SystemPermissionAdapterManager.append(adapter = microPhonePermission)
  }
}

