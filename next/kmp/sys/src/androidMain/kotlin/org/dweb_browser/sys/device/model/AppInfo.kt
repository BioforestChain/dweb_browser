package org.dweb_browser.sys.device.model

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dweb_browser.helper.getAppContextUnsafe

@Serializable
data class AppData(
  var versionCode: Int = 0,
  var versionName: String = "",
  var packageName: String = "",
  var appName: String = ""
)

class AppInfo {
  private val NOT_FOUND_VAL = "unknown"
  val context = getAppContextUnsafe()

  fun getAppInfo(): String {
    return Json.encodeToString(appData)
  }

  val appData: AppData get() = AppData(versionCode, versionName, packageName, appName)

  val versionName: String
    get() {
      val pInfo: PackageInfo
      return try {
        pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          context.packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
          context.packageManager.getPackageInfo(packageName, 0)
        }
        pInfo.versionName ?: "0.0.0-dev.0"
      } catch (e: Throwable) {
        "1.0.0"
      }
    }

  val versionCode: Int
    get() {
      val pInfo: PackageInfo

      return try {
        pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        pInfo.versionCode
      } catch (e: Throwable) {
        0
      }
    }

  val packageName: String get() = context.packageName

  val appName: String
    get() {
      val packageManager = context.packageManager
      var applicationInfo: ApplicationInfo? = null
      try {
        applicationInfo =
          packageManager.getApplicationInfo(context.applicationInfo.packageName, 0)
      } catch (e: PackageManager.NameNotFoundException) {
      }

      return (if (applicationInfo != null) packageManager.getApplicationLabel(applicationInfo) else NOT_FOUND_VAL) as String
    }
}
