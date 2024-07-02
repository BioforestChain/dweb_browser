package org.dweb_browser.helper.platform

import org.dweb_browser.platform.desktop.os.OsType
import java.io.BufferedReader
import java.io.InputStreamReader

fun execCommand(command: String): String {
  val process = when (OsType.current) {
    OsType.Windows -> ProcessBuilder("powershell.exe", "-Command", command)
    else -> ProcessBuilder("/bin/sh", "-c", command)
  }
    .redirectErrorStream(true)
    .start()
  val reader = BufferedReader(InputStreamReader(process.inputStream))
  val result = reader.readText().trim()

  process.waitFor()
  reader.close()

  return result
}