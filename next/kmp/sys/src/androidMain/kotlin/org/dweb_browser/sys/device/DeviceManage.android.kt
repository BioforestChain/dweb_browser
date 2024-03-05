package org.dweb_browser.sys.device

import android.Manifest
import android.os.Environment
import org.dweb_browser.core.module.getAppContext
import org.dweb_browser.helper.randomUUID
import org.dweb_browser.sys.permission.AndroidPermissionTask
import org.dweb_browser.sys.permission.PermissionActivity
import org.dweb_browser.sys.permission.SystemPermissionAdapterManager
import org.dweb_browser.sys.permission.SystemPermissionName
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader

actual object DeviceManage {

  init {
    SystemPermissionAdapterManager.append {
      if (task.name == SystemPermissionName.PHONE) {
        PermissionActivity.launchAndroidSystemPermissionRequester(
          microModule,
          AndroidPermissionTask(
            listOf(Manifest.permission.READ_PHONE_STATE),
            task.title,
            task.description
          )
        ).values.firstOrNull()
      } else null
    }
  }

  actual fun deviceUUID(): String {
    return getDeviceUUID()
  }

  private fun getDeviceUUID(): String {
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
        file.outputStream().write(uuid.encodeToByteArray())
        return uuid
      }
    } catch (e: Exception) {
      debugDevice("uuid", "${e.message}")
    }
    return randomUUID()
  }

  actual fun deviceAppVersion(): String {
    val (packageManager, packageName) = getAppContext().let { context ->
      Pair(context.packageManager, context.packageName)
    }
    return packageManager.getPackageInfo(packageName, 0).versionName
  }
}