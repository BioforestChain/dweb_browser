package org.dweb_browser.platform.desktop.os

import java.awt.Desktop
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.StringWriter

// 官方文档：https://learn.microsoft.com/en-us/previous-versions/windows/internet-explorer/ie-developer/platform-apis/aa767914(v=vs.85)?redirectedfrom=MSDN
// 参考：https://github.com/TwidereProject/TwidereX-Android/blob/da1602493091116c354602136d600783a9c648dd/common/src/desktopMain/kotlin/com/twidere/twiderex/utils/WindowsRegistry.kt
object WindowsRegistry {
  private fun registryUrlProtocol(protocol: String) {
    val root = File("${System.getProperty("user.home")}/.dweb")
    if (!root.exists()) {
      root.mkdirs()
    }
    val regFile = File("${root.absolutePath}/deeplink.reg")
    if (!regFile.exists()) {
      regFile.createNewFile()
      val reg = """
        Windows Registry Editor Version 5.00

        [HKEY_CLASSES_ROOT\$protocol]
        "URL Protocol"=""
        @="URL:$protocol"

        [HKEY_CLASSES_ROOT\$protocol\shell]

        [HKEY_CLASSES_ROOT\$protocol\shell\open]

        [HKEY_CLASSES_ROOT\$protocol\shell\open\command]
        @="\"${File("").absolutePath.replace("\\", "\\\\")}\\DwebBrowser.exe\" \"%1\""
      """.trimIndent()
      regFile.writeText(reg)
    }
    try {
      val process = ProcessBuilder("cmd", "/c", "regedit", "/s", regFile.canonicalPath).start()
      process.waitFor()
    } catch (e: Throwable) {
      try {
        // if process is not working use Desktop open file
        Desktop.getDesktop().open(regFile)
      } catch (e: Throwable) {
        e.printStackTrace()
      }
    }
  }

  private fun readRegistry(location: String, key: String): String? {
    return try {
      // Run reg query, then read output with StreamReader (internal class)
      val process = ProcessBuilder("reg", "query", location, "/v", key).start()
      val reader = StreamReader(process.inputStream)
      reader.start()
      process.waitFor()
      reader.join()
      reader.result
    } catch (e: Exception) {
      e.printStackTrace()
      null
    }
  }

  fun ensureWindowsRegistry(scheme: String) {
    val protocol = readRegistry("HKCR\\$scheme", "URL Protocol")
    if(protocol?.contains(scheme) == true) return
    registryUrlProtocol(scheme)
  }

  internal class StreamReader(private val input: InputStream) : Thread() {
    private val sw: StringWriter = StringWriter()
    override fun run() {
      try {
        var c: Int
        while ((input.read().also { c = it }) != -1) sw.write(c)
      } catch (e: IOException) {
        //
      }
    }

    val result: String
      get() = sw.toString()
  }
}