package org.dweb_browser.core.std.permission.ext

import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.core.std.permission.AuthorizationStatus
import org.dweb_browser.core.std.permission.PERMISSION_ID

suspend fun MicroModule.Runtime.queryPermissions(permissions: List<PERMISSION_ID>) =
  nativeFetch(
    "file://permission.std.dweb/query?permissions=" + permissions.joinToString(",")
  ).json<Map<PERMISSION_ID, Map<MMID /* applicantMmid */, AuthorizationStatus>>>()

suspend fun MicroModule.Runtime.queryPermission(permission: PERMISSION_ID) =
  queryPermissions(listOf(permission))[permission] ?: mapOf()

suspend fun MicroModule.Runtime.checkPermissions(permissions: List<PERMISSION_ID>) =
  nativeFetch(
    "file://permission.std.dweb/check?permissions=" + permissions.joinToString(",")
  ).json<Map<PERMISSION_ID, AuthorizationStatus>>()

suspend fun MicroModule.Runtime.checkPermission(permission: PERMISSION_ID) =
  checkPermissions(listOf(permission))[permission] ?: AuthorizationStatus.UNKNOWN

suspend fun MicroModule.Runtime.requestPermissions(permissions: List<PERMISSION_ID>) =
  nativeFetch(
    "file://permission.std.dweb/request?permissions=" + permissions.joinToString(",")
  ).json<Map<PERMISSION_ID, AuthorizationStatus>>()

suspend fun MicroModule.Runtime.requestPermission(permission: PERMISSION_ID) =
  requestPermissions(listOf(permission))[permission] ?: AuthorizationStatus.UNKNOWN

suspend fun MicroModule.Runtime.deletePermissions(mmid: MMID, permissions: List<PERMISSION_ID>) =
  nativeFetch(
    "file://permission.std.dweb/delete?mmid=$mmid&permissions=" + permissions.joinToString(",")
  ).json<Map<PERMISSION_ID, Boolean>>()

suspend fun MicroModule.Runtime.deletePermission(mmid: MMID, permission: PERMISSION_ID) =
  deletePermissions(mmid, listOf(permission))[permission] ?: false
