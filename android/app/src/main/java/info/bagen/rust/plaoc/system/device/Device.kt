package info.bagen.rust.plaoc.system.device

import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import android.webkit.WebView
import info.bagen.libappmgr.utils.JsonUtil
import info.bagen.rust.plaoc.App
import java.util.*


object Device {

    fun getId(): String {
        return getUuid()
    }

    fun getInfo(): String {
        val deviceInfo = StructDeviceInfo(
            memUsed = getMemUsed(),
            diskFree = getDiskFree(),
            diskTotal = getDiskTotal(),
            realDiskFree = getRealDiskFree(),
            realDiskTotal = getRealDiskTotal(),
            model = Build.MODEL,
            operatingSystem = "android",
            osVersion = Build.VERSION.RELEASE,
            platform = getPlatform(),
            manufacturer = Build.MANUFACTURER,
            isVirtual = isVirtual(),
            name = getName(),
            webViewVersion = getWebViewVersion(),
        )
        return JsonUtil.toJson(deviceInfo)
    }

    fun getBatteryInfo(): String {
        val batteryInfo = StructBatteryInfo(
            batteryLevel = getBatteryLevel(), isCharging = isCharging()
        )
        return JsonUtil.toJson(batteryInfo)
    }

    fun getLanguageCode(): String {
        return Locale.getDefault().language
    }

    fun getLanguageTag(): String {
        return Locale.getDefault().toLanguageTag()
    }

    @SuppressLint("HardwareIds")
    private fun getUuid(): String {
        return Settings.Secure.getString(
            App.appContext.contentResolver,
            Settings.Secure.ANDROID_ID
        )
    }

    private fun getBatteryLevel(): Float {
        val iFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = App.appContext.registerReceiver(null, iFilter)
        var level = -1
        var scale = -1
        batteryStatus?.let { battery ->
            level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        }
        return level / scale.toFloat()
    }

    private fun isCharging(): Boolean {
        val iFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = App.appContext.registerReceiver(null, iFilter)
        if (batteryStatus != null) {
            val status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            return status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        }
        return false
    }

    private fun getMemUsed(): Long {
        val runtime = Runtime.getRuntime()
        return runtime.totalMemory() - runtime.freeMemory()
    }

    private fun getDiskFree(): Long {
        val statFs = StatFs(Environment.getRootDirectory().absolutePath)
        return statFs.availableBlocksLong * statFs.blockSizeLong
    }

    private fun getDiskTotal(): Long {
        val statFs = StatFs(Environment.getRootDirectory().absolutePath)
        return statFs.blockCountLong * statFs.blockSizeLong
    }

    private fun getRealDiskFree(): Long {
        val statFs = StatFs(Environment.getDataDirectory().absolutePath)
        return statFs.availableBlocksLong * statFs.blockSizeLong
    }

    private fun getRealDiskTotal(): Long {
        val statFs = StatFs(Environment.getDataDirectory().absolutePath)
        return statFs.blockCountLong * statFs.blockSizeLong
    }

    private fun getPlatform(): String {
        return "android"
    }

    private fun isVirtual(): Boolean {
        return Build.FINGERPRINT.contains("generic") || Build.PRODUCT.contains("sdk")
    }

    private fun getName(): String {
        return Settings.Global.getString(
            App.appContext.contentResolver,
            Settings.Global.DEVICE_NAME
        )
    }

    private fun getWebViewVersion(): String {
        return WebView.getCurrentWebViewPackage()?.versionName ?: Build.VERSION.RELEASE
        /*var info: PackageInfo? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          info = WebView.getCurrentWebViewPackage()
        } else {
          var webViewPackage = "com.google.android.webview"
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            webViewPackage = "com.android.chrome"
          }
          val pm: PackageManager = App.appContext.getPackageManager()
          try {
            info = pm.getPackageInfo(webViewPackage, 0)
          } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
          }
        }
        return if (info != null) {
          info.versionName
        } else Build.VERSION.RELEASE*/
    }
}

private data class StructBatteryInfo(
    val batteryLevel: Float,
    val isCharging: Boolean,
)

private data class StructDeviceInfo(
    val memUsed: Long,
    val diskFree: Long,
    val diskTotal: Long,
    val realDiskFree: Long,
    val realDiskTotal: Long,
    val model: String,
    val operatingSystem: String,
    val osVersion: String,
    val platform: String,
    val manufacturer: String,
    val isVirtual: Boolean,
    val name: String,
    val webViewVersion: String,
)
