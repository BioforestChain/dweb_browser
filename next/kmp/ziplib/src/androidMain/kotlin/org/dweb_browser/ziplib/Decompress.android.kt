package org.dweb_browser.ziplib

import android.annotation.SuppressLint
import org.dweb_browser.helper.ZipUtil

@SuppressLint("UnsafeDynamicallyLoadedCode")
object AndroidMiniz {
  init {
    System.loadLibrary("miniz")
  }

  external fun unzipFromJNI(zipFilePath: String, destPath: String): Int
}

//actual fun unCompress(zipFilePath: String, destPath: String) = AndroidMiniz.unzipFromJNI(zipFilePath, destPath) == 0
actual fun decompress(zipFilePath: String, destPath: String) = ZipUtil.ergodicDecompress(zipFilePath, destPath)
