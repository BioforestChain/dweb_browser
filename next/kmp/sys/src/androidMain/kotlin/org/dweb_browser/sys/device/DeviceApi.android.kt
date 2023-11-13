package org.dweb_browser.sys.device

import android.os.Environment
import org.dweb_browser.helper.randomUUID
import org.dweb_browser.helper.toUtf8ByteArray
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader

actual class DeviceApi actual constructor() {

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
        file.outputStream().write(uuid.toUtf8ByteArray())
        return uuid
      }
    } catch (e: Exception) {
      debugDevice("uuid", "${e.message}")
    }
    return randomUUID()
  }
}