package org.dweb_browser.sys.permission

import org.dweb_browser.core.std.permission.PermissionType

actual suspend fun requestPermission(type: PermissionType): Boolean {
  return true
}

actual suspend fun requestPermissions(types: MutableList<PermissionType>): MutableMap<PermissionType, Boolean> {
  val map = mutableMapOf<PermissionType, Boolean>()
  types.forEach {
    map[it] = true
  }
  return map
}