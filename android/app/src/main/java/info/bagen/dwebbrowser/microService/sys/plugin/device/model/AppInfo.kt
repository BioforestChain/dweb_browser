package info.bagen.dwebbrowser.microService.sys.plugin.device.model

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import info.bagen.dwebbrowser.App
import info.bagen.dwebbrowser.microService.helper.gson

data class AppData(
    var versionCode: Int = 0,
    var versionName: String = "",
    var packageName: String = "",
    var appName: String = ""
)

class AppInfo {
    private val NOT_FOUND_VAL = "unknown"

    fun getAppInfo(): String {
        return gson.toJson(appData)
    }

    val appData: AppData get() = AppData(versionCode, versionName, packageName, appName)

    val versionName: String
        get() {
            val pInfo: PackageInfo
            val context = App.appContext
            return try {
                pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                pInfo.versionName
            } catch (e: Throwable) {
                "1.0.0"
            }
        }

    val versionCode: Int
        get() {
            val pInfo: PackageInfo
            val context = App.appContext
            return try {
                pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                pInfo.versionCode
            } catch (e: Throwable) {
                0
            }
        }

    val packageName: String get() = App.appContext.packageName

    val appName: String
        get() {
            val context = App.appContext
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
