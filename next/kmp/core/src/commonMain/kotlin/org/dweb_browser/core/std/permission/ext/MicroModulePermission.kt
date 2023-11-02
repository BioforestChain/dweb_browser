package org.dweb_browser.core.std.permission.ext

import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.core.std.permission.AuthorizationStatus
import org.dweb_browser.core.std.permission.PERMISSION_ID
import org.dweb_browser.helper.encodeURIComponent

suspend fun NativeMicroModule.queryPermissions(vararg permissions: PERMISSION_ID) = nativeFetch(
  "file://permission.std.dweb/query?permissions=${
    permissions.joinToString(",").encodeURIComponent()
  }"
).json<Map<PERMISSION_ID, Map<MMID /* applicantMmid */, AuthorizationStatus>>>()

suspend fun NativeMicroModule.queryPermission(permission: PERMISSION_ID) =
  queryPermissions(permission)[permission] ?: mapOf()

suspend fun NativeMicroModule.checkPermissions(vararg permissions: PERMISSION_ID) = nativeFetch(
  "file://permission.std.dweb/check?permissions=${
    permissions.joinToString(",").encodeURIComponent()
  }"
).json<Map<PERMISSION_ID, AuthorizationStatus>>()

suspend fun NativeMicroModule.checkPermission(permission: PERMISSION_ID) =
  checkPermissions(permission)[permission] ?: AuthorizationStatus.UNKNOWN

suspend fun NativeMicroModule.requestPermissions(vararg permissions: PERMISSION_ID) = nativeFetch(
  "file://permission.std.dweb/request?permissions=${
    permissions.joinToString(",").encodeURIComponent()
  }"
).json<Map<PERMISSION_ID, AuthorizationStatus>>()

suspend fun NativeMicroModule.requestPermission(permission: PERMISSION_ID) =
  requestPermissions(permission)[permission] ?: AuthorizationStatus.UNKNOWN


suspend fun NativeMicroModule.deletePermissions(
  applicantMmid: MMID, vararg permissions: PERMISSION_ID
) = nativeFetch(
  "file://permission.std.dweb/delete?mmid=${applicantMmid.encodeURIComponent()}&permissions=${
    permissions.joinToString(",").encodeURIComponent()
  }"
).json<Map<PERMISSION_ID, Boolean>>()

suspend fun NativeMicroModule.deletePermission(applicantMmid: MMID, permission: PERMISSION_ID) =
  deletePermissions(applicantMmid, permission)[permission] ?: false
