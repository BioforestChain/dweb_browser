package org.dweb_browser.browserUI.util

import android.content.Context
import android.net.Uri
import android.os.Build
import org.dweb_browser.helper.MMID
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

enum class APP_DIR_TYPE(val rootName: String) {
  // 内置应用
  RecommendApp(rootName = "recommend-app"),

  // 下载应用
  SystemApp(rootName = "system-app"),

  // 客户应用
  UserApp(rootName = "user-app"),

  // Assets
  AssetsApp(rootName = "");
}

/**
 * 主要用于文件的存储和读取操作，包括文件的解压操作
 */
object FilesUtil {
  const val TAG: String = "FilesUtil"

  const val DIR_BOOT: String = "boot" // 存放 link.json 数据
  const val DIR_SYS: String = "sys" // system-app/bfs-id-xxx/sys 是运行程序路径
  const val DIR_HOME: String = "home" // system-app/bfs-id-xxx/sys 是运行程序路径
  private val DIR_AUTO_UPDATE: String = "tmp" + File.separator + "autoUpdate" // 存放最新版本的路径
  private val FILE_LINK_JSON: String = "link.json"
  private val FILE_BFSA_META_JSON: String = "bfsa-metadata.json"

  /**
   * 获取app的根目录
   */
  private fun getAndroidRootDirectory(context: Context): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      context.dataDir.absolutePath
    } else {
      context.filesDir.absolutePath
    }
  }

  /**
   * 获取应用的缓存路径
   */
  fun getAppCacheDirectory(context: Context): String {
    return context.cacheDir.absolutePath
  }

  /**
   * 获取应用的根路径
   */
  fun getAppRootDirectory(context: Context, type: APP_DIR_TYPE, id: String): String {
    return getAndroidRootDirectory(context) + File.separator + type.rootName + File.separator + id
  }

  /**
   * 获取应用的缓存路径
   */
  fun getAppDownloadPath(context: Context, path: String? = null): String {
    val fileName = path?.let { url ->
      val uri = Uri.parse(url)
      uri.lastPathSegment
    }
    var filePath = getAppCacheDirectory(context) + File.separator + fileName
    var count = 0
    while (File(filePath).exists()) {
      count++
      filePath = getAppCacheDirectory(context) + File.separator + "${count}_" + fileName
    }
    return filePath
  }

  /**
   * 获取应用的解压路径
   */
  fun getAppUnzipPath(context: Context, type: APP_DIR_TYPE = APP_DIR_TYPE.SystemApp): String {
    return getAndroidRootDirectory(context) + File.separator + type.rootName + File.separator
  }

  /**
   * 获取程序运行路径
   */
  fun getAppLauncherPath(context: Context, type: APP_DIR_TYPE, id: String): String {
    return getAppRootDirectory(context, type, id) + File.separator + DIR_SYS
  }

  /**
   * 获取应用更新路径
   */
  private fun getAppUpdateDirectory(
    context: Context, type: APP_DIR_TYPE, id: String
  ): String {
    return getAppRootDirectory(context, type, id) + File.separator + DIR_AUTO_UPDATE
  }

  /**
   * 获取应用更新路径中最新文件
   */
  fun getLastUpdateContent(context: Context, type: APP_DIR_TYPE, id: String): String? {
    val directory = getAppUpdateDirectory(context, type, id)
    val file = File(directory)
    if (file.exists()) {
      val files = file.listFiles()
      return if (files != null && files.isNotEmpty()) {
        getFileContent(files.last().absolutePath)
      } else {
        null
      }
    }
    return null
  }

  /**
   * 获取当前目录子目录列表
   */
  private fun getChildrenDirectoryList(context: Context, type: APP_DIR_TYPE): Map<String, String>? {
    return getChildrenDirectoryList(
      File(getAndroidRootDirectory(context) + File.separator + type.rootName)
    )
  }

  /**
   * 获取当前目录子目录列表
   */
  private fun getChildrenDirectoryList(file: File): Map<String, String>? {
    if (file.exists()) {
      /*if (!file.exists()) {
        file.mkdirs()
      }*/
      val childrenMap: HashMap<String, String> = HashMap<String, String>()
      file.listFiles()?.forEach {
        if (it.isDirectory) {
          // Log.d(TAG, "name=${it.name}, absolutePath=${it.absolutePath}")
          childrenMap[it.name] = it.absolutePath
        }
      }
      if (childrenMap.isNotEmpty()) {
        return childrenMap
      }
    }
    return null
  }

  /**
   * 获取相应应用的图标, link.json中的icon路径进行修改，直接默认要求默认在sys目录
   */
  fun getAppIconPathName(context: Context, type: APP_DIR_TYPE, id: String, icon: String): String {
    return getAppRootDirectory(context, type, id) + File.separator + DIR_SYS + File.separator + icon.parseFilePath()
  }

  /**
   * 遍历当前目录及其子目录所有文件和文件夹
   */
  fun traverseFileTree(fileName: String): List<String> {
    return traverseFileTree(File(fileName))
  }

  /**
   * 遍历当前目录及其子目录所有文件和文件夹
   */
  private fun traverseFileTree(file: File): List<String> {
    if (!file.exists()) {
      file.mkdirs()
    }
    val fileList = arrayListOf<String>()
    val fileTreeWalk = file.walk() // 遍历目录及其子目录所有文件和目录
    fileTreeWalk.iterator().forEach {
      fileList.add(it.absolutePath)
    }
    return fileList
  }

  /**
   * 获取文件全部内容字符串
   * @param filename
   */
  private fun getFileContent(filename: String): String? {
    // Log.d("FilesUtil", "getFileContent filename->$filename")
    val file = File(filename)
    if (!file.exists()) {
      return null
    }
    return file.bufferedReader().use { it.readText() }
  }

  /**
   * 将content信息写入到文件中
   */
  fun writeFileContent(filename: String, content: String) {
    val file = File(filename)
    if (file.parentFile?.exists() == false) {
      file.parentFile?.mkdirs()
    }
    if (!file.exists()) {
      file.createNewFile()
    }
    file.bufferedWriter().use { out -> out.write(content) }
  }

  /**
   * 末尾追加
   */
  private fun appendFileContent(filename: String, content: String) {
    val file: File = File(filename)
    if (!file.exists()) {
      file.createNewFile()
    }
    file.bufferedWriter().append(content)
  }

  fun deleteQuietly(file: File, recursively: Boolean = true) {
    try {
      if (recursively) {
        file.deleteRecursively()
      } else {
        file.delete()
      }
    } catch (_: Throwable) {
    }
  }

  private fun deleteQuietly(path: String, recursively: Boolean = true) {
    deleteQuietly(File(path), recursively)
  }

  /**
   * 将assert目录下的文件拷贝到app目录remember-app目录下
   */
  fun copyAssetsToRecommendAppDir(context: Context) {
    val rootPath =
      getAndroidRootDirectory(context) + File.separator + APP_DIR_TYPE.RecommendApp.rootName
    val file = File(rootPath)
    file.deleteRecursively() // 第一次运行程序时，recommend-app
    copyFilesFassets(context, APP_DIR_TYPE.RecommendApp.rootName, rootPath)
  }

  /**
   *  从assets目录中复制整个文件夹内容
   *  @param  oldPath  String  原文件路径  如：/aa
   *  @param  newPath  String  复制后路径  如：xx:/bb/cc
   */
  private fun copyFilesFassets(context: Context, oldPath: String, newPath: String) {
    try {
      val fileNames = context.assets.list(oldPath) ?: return //获取assets目录下的所有文件及目录名，空目录不会存在
      if (fileNames.isNotEmpty()) { // 目录
        val file = File(newPath);
        file.mkdirs();//如果文件夹不存在，则递归
        fileNames.forEach {
          copyFilesFassets(
            context, oldPath + File.separator + it, newPath + File.separator + it
          )
        }
      } else {// 文件
        val inputStream = context.assets.open(oldPath)
        val outputStream = FileOutputStream(newPath)
        inputStream.copyTo(outputStream, inputStream.available())
        outputStream.flush()
        outputStream.close()
        inputStream.close()
      }
    } catch (e: Throwable) {
      e.printStackTrace()
    }
  }

  fun fileToBase64(path: String): String? {
    var fis: FileInputStream? = null
    try {
      fis = File(path).inputStream()

      val byte = ByteArray(fis.available())
      fis.read(byte)

      return android.util.Base64.encodeToString(byte, android.util.Base64.DEFAULT)
    } catch (e: Throwable) {
    } finally {
      try {
        fis?.close()
      } catch (e: IOException) {
        //e.printStackTrace()
      }
      return null
    }
  }

  fun uninstallApp(context: Context, mmid: MMID) {
    val path =
      getAndroidRootDirectory(context) + File.separator + APP_DIR_TYPE.SystemApp.rootName + File.separator + mmid
    deleteQuietly(path)
  }
}

fun String.parseFilePath(): String {
  if (this.lowercase().startsWith("file://")) {
    // return this.replace("file://", "")
    return this.substring(7, this.length)
  } else if (!this.startsWith("./") && !this.startsWith("/")) {
    return "/$this"
  }
  return this
}