package org.dweb_browser.sys.device

import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.file.ext.store
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.toJsonElement
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.sys.permission.SystemPermissionName
import org.dweb_browser.sys.permission.ext.requestSystemPermission

val debugDevice = Debugger("Device")

class DeviceNMM : NativeMicroModule("device.sys.dweb", "Device Info") {
  init {
    short_name = "Device";
    categories =
      listOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Device_Management_Service);
  }

  val UUID_KEY = "DEVICE_UUID"
  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    routes(
      /** 获取设备唯一标识uuid*/
      "/uuid" bind PureMethod.GET by defineJsonResponse {
        if (requestSystemPermission(
            name = SystemPermissionName.STORAGE,
            title = "Request external storage permissions",
            description = "Request external storage permissions"
          )
        ) {
          val uuid = DeviceManage.deviceUUID(store.getOrNull(UUID_KEY))
          UUIDResponse(uuid).toJsonElement()
        } else {
          throwException(HttpStatusCode.Unauthorized, "permission dined")
        }
      },
      /** 获取设备当前安装的 DwebBrowser 版本 */
      "/version" bind PureMethod.GET by defineStringResponse {
        DeviceManage.deviceAppVersion()
      },
    )
  }

  @Serializable
  data class UUIDResponse(val uuid: String)

  override suspend fun _shutdown() {

  }
}
