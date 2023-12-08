package org.dweb_browser.sys.permission

import org.dweb_browser.core.std.permission.PermissionType

expect suspend fun requestPermission(type: PermissionType): Boolean
expect suspend fun requestPermissions(types: MutableList<PermissionType>): MutableMap<PermissionType, Boolean>