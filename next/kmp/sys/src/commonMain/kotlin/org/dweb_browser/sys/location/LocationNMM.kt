package org.dweb_browser.sys.location

import org.dweb_browser.core.help.types.DwebPermission
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.http.router.byChannel
import org.dweb_browser.core.ipc.helper.IpcMethod
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.permission.PermissionType
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.toJsonElement

val debugLocation = Debugger("Location")

class LocationNMM : NativeMicroModule("geolocation.sys.dweb", "geolocation") {
  init {
    categories =
      listOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Device_Management_Service)
    dweb_permissions = listOf(
      DwebPermission(
        pid = "$mmid/permission",
        routes = listOf("file://$mmid/location", "file://$mmid/observe"),
        title = "申请定位权限",
        permissionType = listOf(PermissionType.LOCATION)
      )
    )
  }

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    val locationApi = LocationApi()
    routes(
      "/location" bind IpcMethod.GET by defineJsonResponse {
        debugLocation("location", "enter")
        locationApi.getCurrentLocation().toJsonElement()
      },
      "/observe" byChannel { ctx ->
        debugLocation("observe", "enter")
        val remoteMmid = ipc.remote.mmid
        val fps = try {
          request.query("fps").toInt()
        } catch (e: NumberFormatException) {
          debugLocation("observe", "fps error")
          5
        }
        locationApi.observeLocation(remoteMmid, fps) {
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
          locationApi.removeLocationObserve(remoteMmid)
        }
      },
      "/removeObserve" bind IpcMethod.GET by defineEmptyResponse {
        debugLocation("removeObserve", "enter")
        val remoteMmid = ipc.remote.mmid
        locationApi.removeLocationObserve(remoteMmid)
      }
    )
  }

  override suspend fun _shutdown() {
  }
}