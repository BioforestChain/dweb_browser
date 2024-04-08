package org.dweb_browser.sys.device

import android.Manifest
import android.content.pm.PackageManager
import android.os.Environment
import kotlinx.coroutines.runBlocking
import org.dweb_browser.core.module.getAppContext
import org.dweb_browser.helper.randomUUID
import org.dweb_browser.sys.permission.AndroidPermissionTask
import org.dweb_browser.sys.permission.PermissionActivity
import org.dweb_browser.sys.permission.SystemPermissionAdapterManager
import org.dweb_browser.sys.permission.SystemPermissionName
import java.io.File

actual object DeviceManage {
  init {
    SystemPermissionAdapterManager.append {
      if (task.name == SystemPermissionName.PHONE) {
        PermissionActivity.launchAndroidSystemPermissionRequester(
          microModule, AndroidPermissionTask(
            listOf(Manifest.permission.READ_PHONE_STATE), task.title, task.description
          )
        ).values.firstOrNull()
      } else null
    }
  }

  private const val PREFIX = ".dweb_" // 文件夹起始内容
  private var alreadyCreateDirectory = false // 标志是否存储过
  private val rootFile by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
  }

  /**
   * 由于使用 MediaStore 存储，卸载apk后分区存储信息会被清空，导致重新安装无法后台直接获取文件内容（权限异常）
   *
   */
  private val deviceUUID by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    runBlocking {
      val uuid = rootFile.list()?.find { file ->
        file.startsWith(PREFIX)
      }?.substringAfter(PREFIX)
      debugDevice("deviceUUID", "uuid=$uuid")
      uuid ?: randomUUID().also { newUUID ->
        val mkdirs = File(rootFile, "$PREFIX$newUUID").mkdirs()
        debugDevice("deviceUUID", "randomUUID=$newUUID, create directory => $mkdirs")
      }
    }
  }

  actual fun deviceUUID(uuid: String?): String {
    // 如果传入的uuid不为空，理论上需要创建一个和uuid一样的文件夹。如果uuid为空的话，同样是直接返回 deviceUUID
    return uuid?.also { saveUUID ->
      if (!alreadyCreateDirectory) {
        // 创建之前，把目录下面所有前缀符合的文件夹删除
        rootFile.listFiles()?.iterator()?.forEach { file ->
          if (file.name.startsWith(PREFIX)) { file.deleteRecursively() }
        }
        val mkdirs = File(rootFile, "$PREFIX$saveUUID").mkdirs()
        debugDevice(
          "deviceUUID",
          "already=$alreadyCreateDirectory, uuid=$saveUUID, create directory => $mkdirs"
        )
        alreadyCreateDirectory = true
      }
    } ?: deviceUUID
  }

  actual fun deviceAppVersion(): String {
    val packageManager: PackageManager = getAppContext().packageManager
    val packageName: String = getAppContext().packageName
    return packageManager.getPackageInfo(packageName, 0).versionName
  }
}