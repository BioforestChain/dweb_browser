package org.dweb_browser.browser.desk.version

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.FileProvider
import io.ktor.utils.io.cancel
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dweb_browser.browser.desk.debugDesk
import org.dweb_browser.core.module.getAppContext
import org.dweb_browser.core.std.dns.httpFetch
import org.dweb_browser.helper.NewVersionUrl
import org.dweb_browser.helper.consumeEachArrayRange
import org.dweb_browser.helper.datetimeNow
import org.dweb_browser.helper.getString
import org.dweb_browser.helper.isGreaterThan
import org.dweb_browser.helper.saveString
import org.dweb_browser.pure.http.PureClientRequest
import org.dweb_browser.pure.http.PureMethod
import java.io.File

enum class VersionType {
  Hide, NewVersion, Download, Install
  ;
}

@Serializable
class NewVersionItem(
  val android: String, val version: String, val market: Map<String, Market>
) {
  @Serializable
  data class Market(val version: String)
}

object NewVersionModel {
  private const val SP_KEY_File = "local-file"
  private const val SP_KEY_VERSION = "version"
  var preVersionType: VersionType = VersionType.Hide
  val versionType: MutableState<VersionType> = mutableStateOf(VersionType.Hide)
  var versionItem: MutableState<NewVersionItem?> = mutableStateOf(null)
  var apkFile: File? = null

  fun updateVersionType(versionType: VersionType, recordPre: Boolean = true) {
    this.preVersionType = if (recordPre) this.versionType.value else VersionType.Hide // 保留上一次的状态
    this.versionType.value = versionType
  }

  private fun downloadCompleted(file: File) {
    apkFile = file
    getAppContext().saveString(SP_KEY_File, file.absolutePath)
    updateVersionType(VersionType.Install)
  }

  suspend fun loadNewVersionItem() {
    try {
      val (context, packageManager, packageName) = getAppContext().let { context ->
        Triple(context, context.packageManager, context.packageName)
      }
      val currentVersion = packageManager.getPackageInfo(packageName, 0).versionName
      val spVersion = context.getString(SP_KEY_VERSION, "")
      val spVersionItem =
        if (spVersion.isNotEmpty()) Json.decodeFromString<NewVersionItem>(spVersion) else null
      debugDesk("loadNewVersionItem", "sp=>$spVersion, $spVersionItem")
      spVersionItem?.let {
        val spFilePath = context.getString(SP_KEY_File, "")
        if (spVersionItem.version.isGreaterThan(currentVersion)) {
          val response = httpFetch(PureClientRequest(spVersionItem.android, PureMethod.GET))
          val contentLength = response.headers.get("Content-Length")?.toLong() ?: 0L
          versionItem.value = spVersionItem
          apkFile = File(spFilePath)
          val fileLength = apkFile?.length()
          debugDesk("loadNewVersionItem", "contentLength=>$contentLength, fileLength=$fileLength")
          if (fileLength == contentLength) {
            updateVersionType(VersionType.Install)
            return
          } else {
            updateVersionType(VersionType.NewVersion)
          }
        } else {
          context.saveString(SP_KEY_File, "")
          context.saveString(SP_KEY_VERSION, "")
          File(spFilePath).deleteRecursively()
        }
      }
      // TODO 下面重新请求下最新版本
      val response =
        httpFetch.fetch(PureClientRequest(href = NewVersionUrl, method = PureMethod.GET))
      debugDesk("loadNewVersionItem", "newVersion => ${response.text().replace("\n", "")}")
      val newVersionItem = Json.decodeFromString<NewVersionItem>(response.text())
      if (newVersionItem.version.isGreaterThan(currentVersion)) {
        context.saveString(SP_KEY_VERSION, Json.encodeToString(newVersionItem))
        versionItem.value = newVersionItem
        updateVersionType(VersionType.NewVersion)
      }
    } catch (e: Exception) {
      debugDesk("loadNewVersionItem", "Fail -> ${e.message}")
    }
  }

  fun download(url: String, callback: (Long, Long) -> Unit) {
    MainScope().launch {
      val uri = Uri.parse(url)
      val context = getAppContext()
      val file = File(
        context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
        uri.lastPathSegment ?: "${datetimeNow()}.apk"
      )
      try {
        val response = httpFetch.fetch(PureClientRequest(url, PureMethod.GET))
        val contentLength = response.headers.get("Content-Length")?.toLong() ?: 1L
        // 先判断下文件是否存在，并且大小是否一致，如果一致就默认为已下载
        if (file.exists() && file.length() == contentLength) {
          downloadCompleted(file)
          return@launch
        }
        val fos = file.outputStream()
        val input = response.stream().getReader("Download New Version")
        var current = 0L
        input.consumeEachArrayRange { byteArray, last ->
          if (last) {
            // 关闭文件
            input.cancel()
            fos.flush()
            fos.close()
            apkFile = file
            downloadCompleted(file)
          } else {
            current += byteArray.size
            callback(current, contentLength)
            fos.write(byteArray)
          }
        }
      } catch (e: Exception) {
        debugDesk("DownloadApk", "Fail -> ${e.message}")
      }
    }
  }

  fun checkInstallPermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      getAppContext().packageManager.canRequestPackageInstalls()
    } else true
  }

  fun openInstallPermissionSetting() {
    val uri = Uri.parse("package:${getAppContext().packageName}")
    val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, uri)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    getAppContext().startActivity(intent)
  }

  // 打开安装界面
  fun installApk() {
    val file = apkFile ?: return
    val (context, packageName) = getAppContext().let { Pair(it, it.packageName) }
    val intent = Intent(Intent.ACTION_VIEW)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      // Android7.0及以上版本
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