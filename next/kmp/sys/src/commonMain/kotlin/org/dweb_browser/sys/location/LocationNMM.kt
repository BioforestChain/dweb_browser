package org.dweb_browser.sys.location

import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.buffer
import org.dweb_browser.core.help.types.DwebPermission
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.http.router.byChannel
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.permission.AuthorizationStatus
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.helper.collectIn
import org.dweb_browser.helper.toJsonElement
import org.dweb_browser.helper.withMainContext
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.sys.permission.SystemPermissionName
import org.dweb_browser.sys.permission.SystemPermissionTask
import org.dweb_browser.sys.permission.ext.requestSystemPermissions

val debugLocation = Debugger("Location")

class LocationNMM : NativeMicroModule("geolocation.sys.dweb", LocationI18nResource.name.text) {
  init {
    short_name = LocationI18nResource.name.text
    categories =
      listOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Device_Management_Service)
    icons =
      listOf(ImageResource(src = "file:///sys/sys-icons/$mmid.svg", type = "image/svg+xml"))
    dweb_permissions = listOf(
      DwebPermission(
        pid = "$mmid/location",
        routes = listOf("file://$mmid/location"),
        title = LocationI18nResource.apply_location_permission.text,
      )
    )
  }
  inner class LocationRuntime(override val bootstrapContext: BootstrapContext) : NativeRuntime() {


    override suspend fun _bootstrap() {
      routes(
        "/location" byChannel { ctx ->
          val locationManage = withMainContext { LocationManage(this@LocationRuntime) }
          val remoteMmid = ipc.remote.mmid
          val minDistance = request.queryOrNull("minDistance")?.toDouble() ?: 1.0
          val precise = request.queryBoolean("precise")
          debugLocation("locationChannel=>", "enter => $remoteMmid->$precise")
          if (requestSystemPermission()) {
            try {
              val observer = locationManage.createLocationObserver(false)
              observer.start(precise = precise, minDistance = minDistance)
              // 缓存通道
              val flowJob = observer.flow.buffer().collectIn(mmScope) { position ->
                debugLocation("locationChannel=>", "loop:$position")
                ctx.sendJsonLine(position.toJsonElement())
              }
              onClose {
                debugLocation("locationChannel=>", "onClose：$remoteMmid")
                scopeLaunch(cancelable = false) {
                  observer.destroy()
                }
                flowJob.cancel()
              }
            } catch (e: Exception) {
              debugLocation("locationChannel=>", e.message ?: "close observe")
              close(cause = e)
            }
          }
        },
        "/location" bind PureMethod.GET by defineJsonResponse {
          val locationManage = withMainContext { LocationManage(this@LocationRuntime) }
          val precise = request.queryBoolean("precise")
          val isPermission = requestSystemPermission()
          debugLocation("location/get", "isPermission=>$isPermission")
          if (isPermission) {
            locationManage.getCurrentLocation(precise).toJsonElement()
          } else {
            throwException(HttpStatusCode.Unauthorized, LocationI18nResource.permission_denied.text)
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
  
  override fun createRuntime(bootstrapContext: BootstrapContext) = LocationRuntime(bootstrapContext)
}