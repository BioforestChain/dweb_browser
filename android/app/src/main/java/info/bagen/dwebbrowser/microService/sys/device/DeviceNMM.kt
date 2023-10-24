package info.bagen.dwebbrowser.microService.sys.device

import android.os.Build
import android.os.Environment
import info.bagen.dwebbrowser.microService.sys.permission.EPermission
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.PureResponse
import org.dweb_browser.core.http.PureStringBody
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.core.std.file.ext.store
import org.dweb_browser.helper.printDebug
import org.dweb_browser.helper.randomUUID
import org.dweb_browser.helper.toJsonElement
import org.dweb_browser.helper.toUtf8ByteArray
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
  val UUID_KEY = "DEVICE_UUID"
  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    routes(
      /** 获取设备唯一标识uuid*/
      "/uuid" bind HttpMethod.Get to defineJsonResponse {
        val uuid = store.getOrPut(UUID_KEY) {
          val grant = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            nativeFetch("file://permission.sys.dweb/query?permission=${EPermission.PERMISSION_STORAGE}").boolean()
          } else true
          if (grant) {
            getDeviceUUID() ?: kotlin.run { randomUUID() }
          } else {
            randomUUID()
          }
        }
        debugDevice("getUUID", uuid)
        UUIDResponse(uuid).toJsonElement()
      },
      /** 获取手机基本信息*/
      "/info" bind HttpMethod.Get to definePureResponse {
        val result = deviceInfo.getDeviceInfo()
        PureResponse(HttpStatusCode.OK, body = PureStringBody(result))
      },
      /** 获取APP基本信息*/
      "/appInfo" bind HttpMethod.Get to definePureResponse {
        val result = deviceInfo.getAppInfo()
        PureResponse(HttpStatusCode.OK, body = PureStringBody(result))
      },
      /**单独获取手机版本*/
      "/mobileVersion" bind HttpMethod.Get to definePureResponse {
        val result = deviceInfo.getDevice()
        PureResponse(HttpStatusCode.OK, body = PureStringBody(result))
      },
      /** 获取电池基本信息*/
      "/batteryInfo" bind HttpMethod.Get to definePureResponse {
        val result = deviceInfo.getBatteryInfo()
        PureResponse(HttpStatusCode.OK, body = PureStringBody(result))
      },
      /** 获取内存信息*/
      "/storage" bind HttpMethod.Get to definePureResponse {
        val result = deviceInfo.getStorage()
        PureResponse(HttpStatusCode.OK, body = PureStringBody(result))
      },
      /** 获取当前手机状态 看看是不是勿扰模式或者其他*/
      "/module" bind HttpMethod.Get to definePureResponse {
        PureResponse(HttpStatusCode.OK, body = PureStringBody(deviceInfo.deviceData.module))
      },
    )
  }

  @Serializable
  data class UUIDResponse(val uuid: String)

  override suspend fun _shutdown() {

  }
}

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
