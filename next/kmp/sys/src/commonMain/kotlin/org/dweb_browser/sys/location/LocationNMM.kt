package org.dweb_browser.sys.location

import io.ktor.http.HttpStatusCode
import org.dweb_browser.core.help.types.DwebPermission
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.http.router.byChannel
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.permission.AuthorizationStatus
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.toJsonElement
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.sys.permission.SystemPermissionName
import org.dweb_browser.sys.permission.SystemPermissionTask
import org.dweb_browser.sys.permission.ext.requestSystemPermissions

val debugLocation = Debugger("Location")

class LocationNMM : NativeMicroModule("geolocation.sys.dweb", "geolocation") {
  init {
    categories =
      listOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Device_Management_Service)
    dweb_permissions = listOf(
      DwebPermission(
        pid = "$mmid/location",
        routes = listOf("file://$mmid/location"),
        title = LocationI18nResource.apply_location_permission.text,
      )
    )
  }

  private val locationManage = LocationManage()
  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    routes(
      "/location" bind PureMethod.GET by defineJsonResponse {
        val precise = request.queryBoolean("precise")
        debugLocation("location/get", "enter")
        if (requestSystemPermission()) {
          locationManage.getCurrentLocation(precise)?.toJsonElement()
            ?: throwException(HttpStatusCode.NoContent)
        } else {
          throwException(HttpStatusCode.Unauthorized, LocationI18nResource.permission_denied.text)
        }
      },
      "/location" byChannel { ctx ->
        val remoteMmid = ipc.remote.mmid
        val fps = request.queryOrNull("fps")?.toLong() ?: 5L
        val precise = request.queryBoolean("precise")
        debugLocation("location/byChannel", "enter => $remoteMmid->$fps")
        if (requestSystemPermission()) {
          locationManage.observeLocation(remoteMmid, fps, precise) {
            try {
              ctx.sendJsonLine(it.toJsonElement())
              true // 这边表示正常，继续监听
            } catch (e: Exception) {
              debugLocation("observe", e.message ?: "close observe")
              close(cause = e)
              false // 这边表示异常，关闭监听
            }
          }
          onClose {
            locationManage.removeLocationObserve(remoteMmid)
          }
        }
      }
    )
  }

  override suspend fun _shutdown() {
  }

  private suspend fun requestSystemPermission(): Boolean {
    val permission = requestSystemPermissions(
      SystemPermissionTask(
        name = SystemPermissionName.LOCATION,
        title = LocationI18nResource.request_permission_title.text,
        description = LocationI18nResource.request_permission_message.text
      )
    )
    return permission.filterValues { it != AuthorizationStatus.GRANTED }.isEmpty()
  }
}