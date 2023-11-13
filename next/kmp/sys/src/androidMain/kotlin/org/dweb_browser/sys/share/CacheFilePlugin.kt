package org.dweb_browser.sys.share

import android.net.Uri
import java.io.File
import java.io.IOException
import java.io.InputStream

object CacheFilePlugin {

  fun writeFile(
    path: String, eFileType: EFileType?, data: InputStream, recursive: Boolean
  ): String {
    if (eFileType != null) {
      // 创建目录，因为它可能不存在
      val androidDir = FileSystemPlugin.getDirectory(eFileType)
      return if (androidDir.exists() || androidDir.mkdirs()) {
        // 路径也可能包括目录
        val fileObject = File(androidDir, path)
        val parentFile = fileObject.parentFile
        if (parentFile == null || parentFile.exists() || recursive && parentFile.mkdirs()) {
          saveFile(fileObject, data)
        } else {
          "Parent folder doesn't exist"
        }
      } else {
        debugShare("writeFile", "Not able to create '$eFileType'!")
        "NOT_CREATED_DIR"
      }
    } else {
      // check file:// or no scheme uris
      val u = Uri.parse(path)
      val uriPath = u.path
      return if ((u.scheme == null || u.scheme == "file") && uriPath !== null) {
        val fileObject = File(path)
        val parentFile = fileObject.parentFile
        if (parentFile == null || parentFile.exists() || recursive && parentFile.mkdirs()) {
          saveFile(fileObject, data)
        } else {
          "Parent folder doesn't exist"
        }
      } else {
        u.scheme + " scheme not supported"
      }
    }
  }

  private fun saveFile(file: File, data: InputStream, append: Boolean = false): String {
    return try {
      FileSystemPlugin.saveFile(file, data, append)
      debugShare("saveFile", "File '" + file.absolutePath + "' saved!")
      Uri.fromFile(file).toString()
    } catch (ex: IOException) {
      debugShare(
        "saveFile",
        "Creating file '" + file.getPath() + "' failed. Error: " + ex.message
      )
      "FILE_NOTCREATED"
    } catch (ex: IllegalArgumentException) {
      "The supplied data is not valid base64 content."
    }
  }

  /**
   * 如果给定的目录字符串是公共存储目录，用户或其他应用程序可以访问该目录，则为真。
   * @param directory the directory string.
   */
  private fun isPublicDirectory(directory: String): Boolean {
    return ("DOCUMENTS" == directory || "EXTERNAL_STORAGE" == directory)
  }
}