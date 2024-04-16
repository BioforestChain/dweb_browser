package org.dweb_browser.browser.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import io.ktor.http.ContentType
import io.ktor.http.defaultForFile
import org.dweb_browser.browser.web.debugBrowser
import org.dweb_browser.core.module.getAppContext
import java.io.File

object InstallApkUtil {
  fun enableInstallApp(context: Context): Boolean {
    return context.packageManager.canRequestPackageInstalls()
  }

  fun openSystemInstallSetting(context: Context) {
    val uri = Uri.parse("package:${context.packageName}")
    val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, uri)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
  }

  fun installApp(context: Context, realPath: String): Boolean {
    if (realPath.substringAfterLast(".") != "apk") return false // 非apk无法安装
    val file = File(realPath)
    val packageName = context.packageName
    val intent = Intent(Intent.ACTION_VIEW)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      // Android7.0及以上版本
      debugBrowser("installApp", "realPath=$realPath")
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
    return true
  }

  fun openFile(realPath: String): Boolean {
    val intent = Intent(Intent.ACTION_VIEW)
    val context = getAppContext()
    val file = File(realPath)
    val uriForFile = FileProvider.getUriForFile(
      context, "${context.packageName}.file.opener.provider", file
    )
    intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION) //给目标文件临时授权
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) //系统会检查当前所有已创建的Task中是否有该要启动的Activity的Task
    intent.setDataAndType(uriForFile, ContentType.defaultForFile(file).toString())
    context.startActivity(intent)
    return true
  }
}