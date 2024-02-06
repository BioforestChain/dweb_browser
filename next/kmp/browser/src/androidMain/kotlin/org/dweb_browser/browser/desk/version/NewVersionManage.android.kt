package org.dweb_browser.browser.desk.version

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.dweb_browser.browser.desk.debugDesk
import org.dweb_browser.core.module.getAppContext
import org.dweb_browser.core.std.dns.httpFetch
import org.dweb_browser.core.std.permission.AuthorizationStatus
import org.dweb_browser.pure.http.PureClientRequest
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.sys.permission.SystemPermissionAdapterManager
import org.dweb_browser.sys.permission.SystemPermissionName
import java.io.File

const val NewVersionUrl =
  "https://source.dwebdapp.com/dweb-browser-apps/dweb-browser/version.json" // 获取最新版本信息

@Serializable
data class LastVersionItem(
  val android: String,
  val version: String,
  val market: Map<String, Version>
) {
  @Serializable
  data class Version(val version: String)

  fun createNewVersionItem() = NewVersionItem(originUrl = android, versionName = version)
}

actual class NewVersionManage {
  init {
    SystemPermissionAdapterManager.append {
      when (task.name) {
        SystemPermissionName.InstallSystemApp -> {
          if (getAppContext().packageManager.canRequestPackageInstalls()) {
            AuthorizationStatus.GRANTED
          } else AuthorizationStatus.DENIED
        }

        else -> null
      }
    }
  }

  /**
   * 获取当前版本，存储的版本，以及在线加载最新版本
   */
  actual suspend fun loadNewVersion(): NewVersionItem? {
    val loadNewVersion = try {
      val response =
        httpFetch.fetch(PureClientRequest(href = NewVersionUrl, method = PureMethod.GET))
      Json.decodeFromString<LastVersionItem>(response.text())
    } catch (e: Exception) {
      debugDesk("NewVersion", "error => ${e.message}")
      null
    }
    return loadNewVersion?.createNewVersionItem()
  }

  actual fun openSystemInstallSetting() {
    val uri = Uri.parse("package:${getAppContext().packageName}")
    val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, uri)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    getAppContext().startActivity(intent)
  }

  actual fun installApk(realPath: String) {
    val file = File(realPath)
    val (context, packageName) = getAppContext().let { Pair(it, it.packageName) }
    val intent = Intent(Intent.ACTION_VIEW)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      // Android7.0及以上版本
      debugDesk("NewVersion", "installApk realPath=$realPath")
      val apkUri = FileProvider.getUriForFile(
        context, "$packageName.file.opener.provider", file
      )
      intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      intent.setDataAndType(apkUri, "application/vnd.android.package-archive")
    } else {
      // Android7.0以下版本
      intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive")
      intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    getAppContext().startActivity(intent)
  }
}
