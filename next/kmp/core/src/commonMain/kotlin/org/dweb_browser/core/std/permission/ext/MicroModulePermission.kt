package org.dweb_browser.core.std.permission.ext

import io.ktor.http.HttpStatusCode
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.core.std.permission.AuthorizationStatus
import org.dweb_browser.core.std.permission.PERMISSION_ID
import org.dweb_browser.helper.buildUrlString
import org.dweb_browser.pure.http.PureResponse

suspend fun MicroModule.Runtime.queryPermissions(permissions: List<PERMISSION_ID>) = nativeFetch(
  buildUrlString("file://permission.std.dweb/query") {
    parameters["permissions"] = permissions.joinToString(",")
  }
).json<Map<PERMISSION_ID, Map<MMID /* applicantMmid */, AuthorizationStatus>>>()

suspend fun MicroModule.Runtime.queryPermission(permission: PERMISSION_ID) =
  queryPermissions(listOf(permission))[permission] ?: mapOf()

suspend fun MicroModule.Runtime.checkPermissions(permissions: List<PERMISSION_ID>) = nativeFetch(
  buildUrlString("file://permission.std.dweb/check") {
    parameters["permissions"] = permissions.joinToString(",")
  }
).json<Map<PERMISSION_ID, AuthorizationStatus>>()

suspend fun MicroModule.Runtime.checkPermission(permission: PERMISSION_ID) =
  checkPermissions(listOf(permission))[permission] ?: AuthorizationStatus.UNKNOWN

suspend fun MicroModule.Runtime.requestPermissions(permissions: List<PERMISSION_ID>) = nativeFetch(
  buildUrlString("file://permission.std.dweb/request") {
    parameters["permissions"] = permissions.joinToString(",")
  }
).json<Map<PERMISSION_ID, AuthorizationStatus>>()

suspend fun MicroModule.Runtime.requestPermission(permission: PERMISSION_ID) =
  requestPermissions(listOf(permission))[permission] ?: AuthorizationStatus.UNKNOWN

suspend fun MicroModule.Runtime.deletePermissions(mmid: MMID, permissions: List<PERMISSION_ID>) =
  nativeFetch(
    buildUrlString("file://permission.std.dweb/delete") {
      parameters["mmid"] = mmid
      parameters["permissions"] = permissions.joinToString(",")
    }
  ).json<Map<PERMISSION_ID, Boolean>>()

suspend fun MicroModule.Runtime.deletePermission(mmid: MMID, permission: PERMISSION_ID) =
  deletePermissions(mmid, listOf(permission))[permission] ?: false

suspend inline fun MicroModule.Runtime.doRequestWithPermissions(doRequest: () -> PureResponse): PureResponse {
  var response = doRequest()
  if (response.status == HttpStatusCode.Unauthorized) {
    val permissions = response.body.toPureString()
    /// 尝试进行授权请求
    if (requestPermissions(permissions.split(",").toList()).all {
        it.value == AuthorizationStatus.GRANTED
      }) {
      /// 如果授权完全成功，那么重新进行请求
      response = doRequest()
    }
  }
  return response
}
