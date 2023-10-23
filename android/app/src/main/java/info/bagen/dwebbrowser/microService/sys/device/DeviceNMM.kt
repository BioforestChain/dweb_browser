package info.bagen.dwebbrowser.microService.sys.device

import android.os.Build
import android.os.Environment
import org.dweb_browser.helper.printDebug
import org.dweb_browser.helper.randomUUID
import org.dweb_browser.helper.toUtf8ByteArray
import org.dweb_browser.microservice.core.BootstrapContext
import org.dweb_browser.microservice.core.NativeMicroModule
import org.dweb_browser.microservice.help.boolean
import org.dweb_browser.microservice.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.microservice.sys.dns.nativeFetch
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.bind
import org.http4k.routing.routes
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader

fun debugDevice(tag: String, msg: Any? = "", err: Throwable? = null) =
  printDebug("Device", tag, msg, err)

class DeviceNMM : NativeMicroModule("device.sys.dweb", "Device Info") {
  init {
    short_name = "Device";
    categories =
      listOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Device_Management_Service);
  }

  val deviceInfo = DeviceInfo()
  val UUID_KEY = "DeviceUUID"

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    apiRouting = routes(
      /** 获取设备唯一标识uuid*/
      "/uuid" bind Method.GET to defineHandler { _ ->
        // 先请求权限，看是否有外部权限，
        // 然后获取 device.ini文件，看是否有数据
        // 没有就通过 randomUUID 产生随机的 UUID， 并保存到 device.ini
        val permission = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
          nativeFetch("file://permission.sys.dweb/query?permission=PERMISSION_STORAGE").boolean()
        } else true
        debugDevice("uuid", "permission=$permission")
        if (permission) {
          getDeviceUUID()?.let { uuid ->
            debugDevice("getUUID", uuid)
            return@defineHandler UUIDResponse(uuid)
          } ?: run {
            return@defineHandler Response(Status.EXPECTATION_FAILED, "no found uuid")
          }
        } else {
          return@defineHandler Response(Status.EXPECTATION_FAILED, "permission denied")
        }
      },
      /** 获取手机基本信息*/
      "/info" bind Method.GET to defineHandler { request ->
        val result = deviceInfo.getDeviceInfo()
        Response(Status.OK).body(result)
      },
      /** 获取APP基本信息*/
      "/appInfo" bind Method.GET to defineHandler { request ->
        val result = deviceInfo.getAppInfo()
        Response(Status.OK).body(result)
      },
      /**单独获取手机版本*/
      "/mobileVersion" bind Method.GET to defineHandler { request ->
        val result = deviceInfo.getDevice()
        Response(Status.OK).body(result)
      },
      /** 获取电池基本信息*/
      "/batteryInfo" bind Method.GET to defineHandler { request ->
        val result = deviceInfo.getBatteryInfo()
        Response(Status.OK).body(result)
      },
      /** 获取内存信息*/
      "/storage" bind Method.GET to defineHandler { request ->
        val result = deviceInfo.getStorage()
        Response(Status.OK).body(result)
      },
      /** 获取当前手机状态 看看是不是勿扰模式或者其他*/
      "/module" bind Method.GET to defineHandler { request ->
        Response(Status.OK).body(deviceInfo.deviceData.module)
      },

      )
  }

  data class UUIDResponse(val uuid: String)

  override suspend fun _shutdown() {}
}

/*fun getDeviceUUID(): String {
  val devIDShort = "35" + Build.BRAND.length % 10 +
      Build.BOARD.length % 10 + Build.DEVICE.length % 10 + Build.DISPLAY.length % 10 +
      Build.HOST.length % 10 + Build.ID.length % 10 + Build.MANUFACTURER.length % 10 +
      Build.MODEL.length % 10 + Build.PRODUCT.length % 10 + Build.TAGS.length % 10 +
      Build.TYPE.length % 10 + Build.USER.length % 10 + Build.FINGERPRINT.length % 10 // 13位
  debugDevice("uuid", "devIDShort=$devIDShort, ${devIDShort.hashCode()}")
  return UUID.nameUUIDFromBytes(devIDShort.toUtf8ByteArray()).toString()
  // return UUID(devIDShort.hashCode().toLong(), Build.MANUFACTURER.hashCode().toLong()).toString()
}*/

fun getDeviceUUID(): String? {
  val fileName = "dweb-browser.ini"
  val root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
  val file = File(root, fileName)
  try {
    if (file.exists()) {
      return InputStreamReader(FileInputStream(file)).readText()
    }
    file.parentFile?.let { parentFile ->
      if (!parentFile.exists()) parentFile.mkdirs()
    }
    if (file.createNewFile()) {
      val uuid = randomUUID()
      file.outputStream().write(uuid.toUtf8ByteArray())
      return uuid
    }
  } catch (e: Exception) {
    debugDevice("uuid", "${e.message}")
  }
  return null
}