package org.dweb_browser.sys.device.model

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.Build.MODEL
import android.provider.Settings
import android.telephony.TelephonyManager
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dweb_browser.core.module.getAppContext

@Serializable
data class DeviceData(
  var deviceName: String = "", // 设备名称
  var deviceMode: String = "", // 设备型号
  var deviceVersion: String = "", // 版本号
  var processor: String = "", // 处理器
  var memory: MemoryData? = null, // 运行内存
  var storage: StorageSize? = null, // 存储
  var screen: String = "", // 屏幕
  var module: String = "default", // 手机模式(silentMode,doNotDisturb,default)
)

class DeviceInfo {

  fun getAppInfo(): String {
    return AppInfo().getAppInfo()
  }

  fun getDevice(): String {
    return deviceData.deviceVersion
  }

  fun getBatteryInfo(): String {
    return BatteryInfo().getBatteryInfo()
  }

  fun getDeviceInfo(): String {
    return Json.encodeToString(deviceData)
  }

  fun getMemory(): String {
    return MemoryInfo().getMemoryInfo()
  }

  fun getStorage(): String {
    return Json.encodeToString(deviceData.storage)
  }


  val deviceData: DeviceData
    get() {
      var deviceData = DeviceData()
      deviceData.deviceName = deviceName
      deviceData.deviceMode = deviceMode
      deviceData.screen = deviceScreen
      deviceData.deviceVersion = deviceVersion
      deviceData.processor = deviceProcessor
      deviceData.module = module
      var memoryInfo = MemoryInfo()
      deviceData.memory = memoryInfo.memoryData
      deviceData.storage = memoryInfo.storageSize
      return deviceData
    }

  private lateinit var mTelephonyManager: TelephonyManager

  val deviceName: String
    @SuppressLint("MissingPermission")
    get() {
      mTelephonyManager = getAppContext()
        .getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
      // 获取 Global.DEVICE_NAME值不对，所以考虑用蓝牙名称，这二者理论上是一致的
      // 但是如果蓝牙是关闭的，修改设备名称后不会立刻同步到蓝牙，只有等蓝牙打开后才会同步名称
      return Settings.Secure.getString(getAppContext().contentResolver, "bluetooth_name")
    }

  val deviceMode: String
    get() {
      //return Settings.Global.getString(App.appContext.contentResolver, Settings.Global.DEVICE_NAME)
      return MODEL
    }

  val deviceScreen: String
    get() {
      var displayMetrics = getAppContext().resources.displayMetrics
      var screenWith = displayMetrics.widthPixels
      var screenHeight = displayMetrics.heightPixels
      /*var windowManager = App.appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
      val dm = DisplayMetrics()
      windowManager.defaultDisplay.getMetrics(dm)*/
      return "$screenHeight * $screenWith"
    }

  val deviceVersion: String
    get() {
      return Build.DISPLAY
    }

  val deviceProcessor: String
    get() {
      return Build.HARDWARE
    }

  val module: String // silentMode,doNotDisturb,default
    get() {
      // 勿扰
      if (enableZenMode) {
        return "doNotDisturb"
      }
      // 静音
      if (ringerMode == AudioManager.RINGER_MODE_SILENT) {
        return "silentMode"
      }
      return "default"
    }

//  val devicePhone: String
//    @SuppressLint("MissingPermission")
//    get() {
//      return mTelephonyManager.line1Number
//    }

  /**
   * 获取勿扰模式状态，true为开，false为关
   */
  val enableZenMode: Boolean
    get() {
      var zenMode =
        Settings.Global.getInt(getAppContext().contentResolver, "zen_mode", 0)
      return zenMode == 1
    }

  /**
   * 获取当前声音模式
   * AudioManager.RINGER_MODE_NORMAL  响铃模式 2
   * AudioManager.RINGER_MODE_SILENT  静音模式 0
   * AudioManager.RINGER_MODE_VIBRATE 振动模式 1
   */
  val ringerMode: Int
    get() {
      AudioManager.RINGER_MODE_NORMAL
      AudioManager.RINGER_MODE_NORMAL
      val am = getAppContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager
      return am.ringerMode
    }
}
