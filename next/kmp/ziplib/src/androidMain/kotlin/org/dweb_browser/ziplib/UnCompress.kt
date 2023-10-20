package org.dweb_browser.ziplib

import android.annotation.SuppressLint

@SuppressLint("UnsafeDynamicallyLoadedCode")
object AndroidMiniz {
  init {
    System.loadLibrary("miniz")
  }

  external fun unzipFromJNI(zipFilePath: String, destPath: String): Int
}

actual fun unCompress(zipFilePath: String, destPath: String) = AndroidMiniz.unzipFromJNI(zipFilePath, destPath) == 0