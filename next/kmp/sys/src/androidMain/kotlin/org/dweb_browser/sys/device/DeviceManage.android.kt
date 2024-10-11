package org.dweb_browser.sys.device

import android.content.pm.PackageManager
import android.os.Environment
import org.dweb_browser.helper.SuspendOnce
import org.dweb_browser.helper.getAppContextUnsafe
import org.dweb_browser.helper.randomUUID
import org.dweb_browser.sys.device.model.DeviceData
import org.dweb_browser.sys.device.model.DeviceInfo
import java.io.File

data class AndroidHardwareInfo(
  val brand: String,
  val modelName: String,
  val hardware: String,
  val manufacturer: String,
  val supportAbis: String
)

actual object DeviceManage {
  private const val PREFIX = ".dweb_" // 文件夹起始内容
  private val rootFile by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
  }

  /**
   * 由于使用 MediaStore 存储，卸载apk后分区存储信息会被清空，导致重新安装无法后台直接获取文件内容（权限异常）
   *
   */
  private val initDeviceUUID = SuspendOnce {
    val uuid = rootFile.list()?.find { file ->
      file.startsWith(PREFIX)
    }?.substringAfter(PREFIX)
    debugDevice("deviceUUID", "uuid=$uuid")
    uuid ?: randomUUID().also { newUUID ->
      val mkdirs = File(rootFile, "$PREFIX$newUUID").mkdirs()
      debugDevice("deviceUUID", "randomUUID=$newUUID, create directory => $mkdirs")
    }
  }

  actual suspend fun deviceUUID(uuid: String?): String {
    // 如果传入的uuid不为空，理论上需要创建一个和uuid一样的文件夹。如果uuid为空的话，同样是直接返回 deviceUUID
    return uuid?.also { saveUUID ->
      // 创建之前，把目录下面所有前缀符合的文件夹删除
      rootFile.listFiles()?.iterator()?.forEach { file ->
        if (file.name.startsWith(PREFIX)) {
          file.deleteRecursively()
        }
      }
      val mkdirs = File(rootFile, "$PREFIX$saveUUID").mkdirs()
      debugDevice("deviceUUID", "uuid=$saveUUID, create directory => $mkdirs")
    } ?: initDeviceUUID()
  }

  actual fun deviceAppVersion(): String {
    val packageManager: PackageManager = getAppContextUnsafe().packageManager
    val packageName: String = getAppContextUnsafe().packageName
    return packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0.0"
  }
}