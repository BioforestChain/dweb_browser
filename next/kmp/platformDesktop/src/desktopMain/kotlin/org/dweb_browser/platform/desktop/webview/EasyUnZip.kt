package org.dweb_browser.platform.desktop.webview

import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipFile

fun easyUnZip(file: File, outputDir: File) {
  ZipFile(file).use { zipFile ->
    //创建输出目录
    outputDir.mkdirs()
    val enums = zipFile.entries()
    while (enums.hasMoreElements()) {
      val entry = enums.nextElement()
      val name = entry.name
      if (entry.isDirectory) {
        //创建空目录
        outputDir.resolve(name).mkdirs()
      } else { //是文件
        outputDir.resolve(name).apply {
          parentFile?.mkdirs()
          createNewFile()
          zipFile.getInputStream(entry).use { inputStream ->
            inputStream.pipeTo(this.outputStream())
          }
        }
      }
    }
  }
}

fun InputStream.pipeTo(out: OutputStream) {
  var length: Int
  val b = ByteArray(1024)
  while (this.read(b).also { length = it } != -1) {
    out.write(b, 0, length)
  }
  out.flush()
}