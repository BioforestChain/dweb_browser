package org.dweb_browser.sys.device.model

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.Build.MODEL
import android.provider.Settings
import android.telephony.TelephonyManager
import androidx.compose.runtime.NoLiveLiterals
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dweb_browser.helper.getAppContextUnsafe
import kotlin.math.sqrt

@Serializable
data class DeviceData(
  var id: String = "",
  var board: String = "",
  var brand: String = "", // 品牌
  var manufacturer: String = "", // 产品/硬件 制造商
//  var deviceName: String = "", // 设备名称
  var deviceModel: String = "", // 设备型号
  var display: String = "", // 版本号
  var hardware: String = "", // 硬件
  var memory: MemoryData? = null, // 运行内存
  var storage: StorageSize? = null, // 存储
  var resolution: String = "", // 屏幕分辨率
  var density: String = "", // 屏幕像素密度
  var refreshRate: String = "", // 屏幕最高帧率
  var screenSizeInches: String = "", // 屏幕尺寸 单位/英寸
  var module: String = "default", // 手机模式(silentMode,doNotDisturb,default)
  var supportAbis: String = "", // 支持的Abis
  var radio: String = "", // 收音机
)

object DeviceInfo {

  fun getAppInfo(): String {
    return AppInfo().getAppInfo()
  }

  fun getBatteryInfo() = BatteryInfo().getBatteryInfo()

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
      val deviceData = DeviceData()
      deviceData.id = id
      deviceData.board = board
      deviceData.brand = brand
      deviceData.manufacturer = manufacturer
//        deviceData.deviceName = deviceName
      deviceData.deviceModel = deviceModel
      deviceData.resolution = resolution
      deviceData.screenSizeInches = screenSizeInches
      deviceData.display = display
      deviceData.hardware = hardware
      deviceData.module = module
      deviceData.supportAbis = supportAbis
      deviceData.radio = radio
      deviceData.density = density
      deviceData.refreshRate = refreshRate
      val memoryInfo = MemoryInfo()
      deviceData.memory = memoryInfo.memoryData
      deviceData.storage = memoryInfo.storageSize
      return deviceData
    }

  private lateinit var mTelephonyManager: TelephonyManager

  val id: String
    get() = Build.ID

  val board: String
    get() = Build.BOARD

  // 品牌
  val brand: String
    get() = Build.BRAND

  // 产品硬件制造商
  val manufacturer: String
    get() = Build.MANUFACTURER

  val deviceName: String
    @SuppressLint("MissingPermission") get() {
      mTelephonyManager =
        getAppContextUnsafe().getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
      // 获取 Global.DEVICE_NAME值不对，所以考虑用蓝牙名称，这二者理论上是一致的
      // 但是如果蓝牙是关闭的，修改设备名称后不会立刻同步到蓝牙，只有等蓝牙打开后才会同步名称
      return Settings.Secure.getString(getAppContextUnsafe().contentResolver, "bluetooth_name")
    }

  val deviceModel: String
    get() {
      //return Settings.Global.getString(App.appContext.contentResolver, Settings.Global.DEVICE_NAME)
      return MODEL
    }

  private val displayMetrics = getAppContextUnsafe().resources.displayMetrics

  val resolution: String
    get() {
      val screenWith = displayMetrics.widthPixels
      val screenHeight = displayMetrics.heightPixels/*var windowManager = App.appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
      val dm = DisplayMetrics()
      windowManager.defaultDisplay.getMetrics(dm)*/
      return "$screenHeight * $screenWith pixels"
    }

  val screenSizeInches: String
    @SuppressLint("DefaultLocale")
    get() {
      val screenWidthDp = displayMetrics.widthPixels / displayMetrics.density
      val screenHeightDp = displayMetrics.heightPixels / displayMetrics.density
      val screenDiagonalDp =
        sqrt((screenWidthDp * screenWidthDp + screenHeightDp * screenHeightDp).toDouble())
      val screenDiagonalInches = screenDiagonalDp / 160 // 1 inch = 160 dp
      return String.format("%.2f inches", screenDiagonalInches)
    }

  val density: String
    get() = "${displayMetrics.densityDpi} dpi"

  val refreshRate: String
    @SuppressLint("DefaultLocale")
    get() = "${(getAppContextUnsafe().getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager).defaultDisplay.refreshRate.toInt()} Hz"

  val display: String
    get() {
      return Build.DISPLAY
    }

  val sdkInt: Int
    get() = Build.VERSION.SDK_INT

  val osVersion: String
    get() = Build.VERSION.RELEASE

  val hardware: String
    get() {
      return Build.HARDWARE
    }

  val radio: String
    get() = Build.getRadioVersion()

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
      var zenMode = Settings.Global.getInt(getAppContextUnsafe().contentResolver, "zen_mode", 0)
      return zenMode == 1
    }

  /**
   * 获取当前声音模式
   * AudioManager.RINGER_MODE_NORMAL  响铃模式 2
   * AudioManager.RINGER_MODE_SILENT  静音模式 0
   * AudioManager.RINGER_MODE_VIBRATE 振动模式 1
   */
  @NoLiveLiterals
  val ringerMode: Int
    get() {
      AudioManager.RINGER_MODE_NORMAL
      AudioManager.RINGER_MODE_NORMAL
      val am = getAppContextUnsafe().getSystemService(Context.AUDIO_SERVICE) as AudioManager
      return am.ringerMode
    }

  /**
   * 获取设备支持的Abis
   */
  val supportAbis: String
    get() = Build.SUPPORTED_ABIS.joinToString()
}
