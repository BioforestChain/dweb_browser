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

private const val UUID_KEY = "DEVICE_UUID"

class DeviceNMM : NativeMicroModule("device.sys.dweb", "Device Info") {
  init {
    short_name = "Device";
    categories =
      listOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Device_Management_Service);
  }

  @Serializable
  data class UUIDResponse(val uuid: String)

  inner class DeviceRuntime(override val bootstrapContext: BootstrapContext) : NativeRuntime() {
    override suspend fun _bootstrap() {
      // 由于android改造了，这边在初始化强制执行一次“文件夹”的创建操作。
      DeviceManage.deviceUUID(store.getOrNull(UUID_KEY))

      routes(
        /** 获取设备唯一标识uuid*/
        "/uuid" bind PureMethod.GET by defineJsonResponse {
          val uuid = store.getOrPut(UUID_KEY) { DeviceManage.deviceUUID() }
          UUIDResponse(uuid).toJsonElement()
        },
        /** 获取设备当前安装的 DwebBrowser 版本 */
        "/version" bind PureMethod.GET by defineStringResponse {
          DeviceManage.deviceAppVersion()
        },
      )
    }

    override suspend fun _shutdown() {

    }
  }

  override fun createRuntime(bootstrapContext: BootstrapContext) = DeviceRuntime(bootstrapContext)
}
