package org.dweb_browser.sys.device

import org.dweb_browser.core.std.file.FileNMM
import org.dweb_browser.core.std.file.getApplicationRootDir
import org.dweb_browser.helper.randomUUID
import org.dweb_browser.platform.desktop.os.OsType

actual object DeviceManage {
  val runtime by lazy { Runtime.getRuntime() }
  val uuid by lazy {
    runCatching {
      when (OsType.current) {
        OsType.MacOS, OsType.M2 -> {
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

  actual fun deviceUUID(): String {
    return uuid
  }

  actual fun deviceAppVersion(): String {
    return DeviceManage::class.java.`package`?.implementationVersion ?: "0.0.0-dev.0"
  }
}