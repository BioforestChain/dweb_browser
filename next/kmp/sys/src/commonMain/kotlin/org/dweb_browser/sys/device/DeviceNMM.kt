package org.dweb_browser.sys.device

import kotlinx.serialization.Serializable
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.file.ext.store
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.toJsonElement
import org.dweb_browser.pure.http.PureMethod

val debugDevice = Debugger("Device")

class DeviceNMM : NativeMicroModule("device.sys.dweb", "Device Info") {
  init {
    short_name = "Device";
    categories =
      listOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Device_Management_Service);
  }

  val UUID_KEY = "DEVICE_UUID"

  private val deviceApi = DeviceApi();
  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    routes(
      /** 获取设备唯一标识uuid*/
      "/uuid" bind PureMethod.GET by defineJsonResponse {
        val uuid = store.getOrPut(UUID_KEY) {
          deviceApi.deviceUUID()
        }
        UUIDResponse(uuid).toJsonElement()
      }
    )
  }

  @Serializable
  data class UUIDResponse(val uuid: String)

  override suspend fun _shutdown() {

  }
}
