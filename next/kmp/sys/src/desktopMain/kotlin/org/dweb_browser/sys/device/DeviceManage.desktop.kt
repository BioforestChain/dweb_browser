package org.dweb_browser.sys.device

import org.dweb_browser.core.std.file.FileNMM
import org.dweb_browser.core.std.file.getApplicationRootDir
import org.dweb_browser.core.std.permission.AuthorizationStatus
import org.dweb_browser.helper.randomUUID
import org.dweb_browser.platform.desktop.os.OsType
import org.dweb_browser.sys.permission.SystemPermissionAdapterManager
import org.dweb_browser.sys.permission.SystemPermissionName

actual object DeviceManage {
  private val runtime by lazy { Runtime.getRuntime() }
  private val deviceUUID by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    runCatching {
      when (OsType.current) {
        OsType.MacOS-> {
          val cmd = arrayOf(
            "/bin/sh",
            "-c",
            "system_profiler SPHardwareDataType | awk '/UUID/ { print $3; }'"
          )
          runtime.exec(cmd)
        }

        OsType.Windows -> runtime.exec("wmic csproduct get UUID")
        else -> runtime.exec("cat /sys/class/dmi/id/product_uuid")
      }.inputStream.readAllBytes().decodeToString()
    }.getOrElse {
      FileNMM.getApplicationRootDir().resolve(".os").toFile().apply { mkdirs() }.resolve("uuid")
        .run {
          if (exists()) readText()
          else randomUUID().also { writeText(it) }
        }
    }
  }

  actual fun deviceUUID(uuid: String?): String {
    return uuid ?: deviceUUID // 由于安卓的改造，这边如果有传入uuid，直接返回即可
  }

  actual fun deviceAppVersion(): String {
    return System.getProperty("dwebbrowser.version") ?: javaClass.`package`?.implementationVersion ?: "0.0.0-dev.0"
  }
}