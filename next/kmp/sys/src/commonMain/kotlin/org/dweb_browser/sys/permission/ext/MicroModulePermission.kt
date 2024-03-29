package org.dweb_browser.sys.permission.ext

import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.core.std.permission.AuthorizationStatus
import org.dweb_browser.pure.http.PureClientRequest
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.sys.permission.SystemPermissionName
import org.dweb_browser.sys.permission.SystemPermissionTask

suspend fun MicroModule.Runtime.requestSystemPermissions(vararg permissionsTasks: SystemPermissionTask) =
  nativeFetch(
    PureClientRequest.fromJson(
      href = "file://permission.sys.dweb/request", method = PureMethod.POST, body = permissionsTasks
    )
  ).json<Map<SystemPermissionName, AuthorizationStatus>>()

suspend fun MicroModule.Runtime.requestSystemPermission(permissionsTask: SystemPermissionTask) =
  requestSystemPermissions(permissionsTask)[permissionsTask.name] == AuthorizationStatus.GRANTED

suspend fun MicroModule.Runtime.requestSystemPermission(
  name: SystemPermissionName, title: String, description: String
) = requestSystemPermission(SystemPermissionTask(name, title, description))