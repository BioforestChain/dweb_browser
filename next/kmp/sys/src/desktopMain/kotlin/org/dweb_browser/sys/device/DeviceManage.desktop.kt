package org.dweb_browser.sys.device

import org.dweb_browser.core.std.file.FileNMM
import org.dweb_browser.core.std.file.getApplicationRootDir
import org.dweb_browser.helper.platform.execCommand
import org.dweb_browser.helper.randomUUID
import org.dweb_browser.platform.desktop.os.OsType
import org.dweb_browser.sys.device.model.MacHardwareInfo
import org.dweb_browser.sys.device.model.WinHardwareInfo

actual object DeviceManage {
  private val runtime by lazy { Runtime.getRuntime() }
  private val initDeviceUUID by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    runCatching {
      when (OsType.current) {
        OsType.MacOS -> {
          val uuid = execCommand("system_profiler SPHardwareDataType | grep UUID | sed 's/.* //'")
          if (uuid.isNotEmpty()) {
            return@lazy uuid
          } else {
            throw java.io.IOException()
          }
        }

        OsType.Windows -> {
          try {
            return@lazy WinHardwareInfo.uuid
          } catch (_: Exception) {
            throw java.io.IOException()
          }
        }

        else -> runtime.exec("cat /sys/class/dmi/id/product_uuid")
      }.inputStream.readAllBytes().decodeToString()
    }.getOrElse {
      FileNMM.getApplicationRootDir().resolve(".os").toFile().apply {
        if (!exists()) {
          mkdirs()
        }
      }
        .resolve("uuid")
        .run {
          if (exists()) readText()
          else randomUUID().also {
            createNewFile()
            writeText(it)
          }
        }
    }
  }

  actual suspend fun deviceUUID(uuid: String?): String {
    return uuid ?: initDeviceUUID // 由于安卓的改造，这边如果有传入uuid，直接返回即可
  }

  actual fun deviceAppVersion(): String {
    return System.getProperty("dwebbrowser.version") ?: javaClass.`package`?.implementationVersion
    ?: "0.0.0-dev.0"
  }


  fun getMacHardwareInfo() = MacHardwareInfo.getHardwareInfo()

  fun getWinHardwareInfo() = WinHardwareInfo.getHardwareInfo()
}