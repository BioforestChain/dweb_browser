package org.dweb_browser.browser.util

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import coil3.annotation.InternalCoilApi
import org.dweb_browser.browser.desk.debugDesk
import org.dweb_browser.core.module.getAppContext
import java.io.File
import java.util.Locale


object InstallApkUtil {

  private fun openSystemInstallSetting() {
    val uri = Uri.parse("package:${getAppContext().packageName}")
    val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, uri)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    getAppContext().startActivity(intent)
  }

  private fun installApp(realPath: String): Boolean {
    if (realPath.substringAfterLast(".") != "apk") return false // 非apk无法安装
    val file = File(realPath)
    val (context, packageName) = getAppContext().let { Pair(it, it.packageName) }
    val intent = Intent(Intent.ACTION_VIEW)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      // Android7.0及以上版本
      debugDesk("NewVersion", "installApp realPath=$realPath")
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

  private fun openOrShareFile(realPath: String) {
    val intent = Intent(Intent.ACTION_VIEW)
    val context = getAppContext()
    val file = File(realPath)
    val uriForFile = FileProvider.getUriForFile(
      context, "${context.packageName}.file.opener.provider", file
    )
    intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION) //给目标文件临时授权
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) //系统会检查当前所有已创建的Task中是否有该要启动的Activity的Task
    intent.setDataAndType(uriForFile, getMimeTypeFromFile(file.name))
    context.startActivity(intent)
  }

  @OptIn(InternalCoilApi::class)
  private fun getMimeTypeFromFile(fileName: String): String {
    //获取后缀名前的分隔符"."在fName中的位置。
    val dotIndex = fileName.lastIndexOf(".")
    return if (dotIndex > 0) {
      // 获取文件后缀名
      val end = fileName.substring(dotIndex + 1, fileName.length).lowercase(Locale.getDefault())
      // 在MIME和文件类型的匹配表中找到对应的MIME类型
      coil3.util.MimeTypeMap.getMimeTypeFromExtension(end) ?: "*/*"
    } else "*/*"
  }
}