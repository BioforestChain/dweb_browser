package info.bagen.dwebbrowser.util

import android.net.Uri
import android.os.Build
import com.google.gson.*
import com.google.gson.annotations.JsonAdapter
import info.bagen.dwebbrowser.App
import info.bagen.dwebbrowser.microService.helper.Mmid
import org.dweb_browser.helper.*
import info.bagen.dwebbrowser.ui.entity.*
import info.bagen.dwebbrowser.util.FilesUtil.getFileType
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.lang.reflect.Type
import java.util.*

@JsonAdapter(APP_DIR_TYPE::class)
enum class APP_DIR_TYPE(val rootName: String) : JsonSerializer<APP_DIR_TYPE>,
  JsonDeserializer<APP_DIR_TYPE> {
  // 内置应用
  RecommendApp(rootName = "recommend-app"),

  // 下载应用
  SystemApp(rootName = "system-app"),

  // 客户应用
  UserApp(rootName = "user-app"),

  // Assets
  AssetsApp(rootName = "");

  override fun serialize(
    src: APP_DIR_TYPE,
    typeOfSrc: Type,
    context: JsonSerializationContext
  ) = JsonPrimitive(src.rootName)

  override fun deserialize(
    json: JsonElement,
    typeOfT: Type,
    context: JsonDeserializationContext
  ) = json.asString.let { rootName -> values().first { it.rootName == rootName } }

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
  private fun getAndroidRootDirectory(): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      App.appContext.dataDir.absolutePath
    } else {
      App.appContext.filesDir.absolutePath
    }
  }

  /**
   * 获取应用的缓存路径
   */
  fun getAppCacheDirectory(): String {
    return App.appContext.cacheDir.absolutePath
  }

  /**
   * 获取应用的根路径
   */
  fun getAppRootDirectory(appInfo: AppInfo): String {
    return getAndroidRootDirectory() + File.separator + appInfo.appDirType.rootName + File.separator + appInfo.bfsAppId
  }

  /**
   * 获取应用的缓存路径
   */
  fun getAppDownloadPath(path: String? = null): String {
    val fileName = path?.let { url ->
      val uri = Uri.parse(url)
      uri.lastPathSegment
    }
    var filePath =
      getAppCacheDirectory() + File.separator + fileName //simpleDateFormat.format(Date()) + ".bfsa"
    var count = 0
    while (File(filePath).exists()) {
      count++
      filePath = getAppCacheDirectory() + File.separator + "${count}_" + fileName
    }
    return filePath
  }

  /**
   * 获取应用的解压路径
   */
  fun getAppUnzipPath(appInfo: AppInfo? = null): String {
    return getAndroidRootDirectory() + File.separator + (appInfo?.appDirType
      ?: APP_DIR_TYPE.SystemApp).rootName + File.separator
  }

  /**
   * 获取程序运行路径
   */
  fun getAppLauncherPath(appInfo: AppInfo): String {
    return getAppRootDirectory(appInfo) + File.separator + DIR_SYS
  }

  /**
   * 获取程序运行路径
   */
  fun getAppDenoUrl(appInfo: AppInfo, dAppInfo: DAppInfo): String {
    return getAppRootDirectory(appInfo) + File.separator + dAppInfo.manifest.bfsaEntry
  }

  /**
   * 获取应用更新路径
   */
  fun getAppVersionSaveFile(appInfo: AppInfo): String {
    return getAppUpdateDirectory(appInfo) + File.separator + now() + ".json"
  }

  /**
   * 获取应用更新路径
   */
  private fun getAppUpdateDirectory(appInfo: AppInfo): String {
    return getAppRootDirectory(appInfo) + File.separator + DIR_AUTO_UPDATE
  }

  /**
   * 获取应用更新路径中最新文件
   */
  fun getLastUpdateContent(appInfo: AppInfo): String? {
    val directory = getAppUpdateDirectory(appInfo)
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
  private fun getChildrenDirectoryList(appDirType: APP_DIR_TYPE): Map<String, String>? {
    return getChildrenDirectoryList(
      File(getAndroidRootDirectory() + File.separator + appDirType.rootName)
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
  fun getAppIconPathName(appInfo: AppInfo): String {
    return getAppRootDirectory(appInfo) + File.separator + DIR_SYS + File.separator + appInfo.icon.parseFilePath()
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
    if (!file.parentFile.exists()) {
      file.parentFile.mkdirs()
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
  fun copyAssetsToRecommendAppDir() {
    val rootPath =
      getAndroidRootDirectory() + File.separator + APP_DIR_TYPE.RecommendApp.rootName
    val file = File(rootPath)
    file.deleteRecursively() // 第一次运行程序时，recommend-app
    copyFilesFassets(APP_DIR_TYPE.RecommendApp.rootName, rootPath)
  }

  /**
   *  从assets目录中复制整个文件夹内容
   *  @param  oldPath  String  原文件路径  如：/aa
   *  @param  newPath  String  复制后路径  如：xx:/bb/cc
   */
  private fun copyFilesFassets(oldPath: String, newPath: String) {
    try {
      val fileNames = App.appContext.assets.list(oldPath) ?: return //获取assets目录下的所有文件及目录名，空目录不会存在
      if (fileNames.isNotEmpty()) { // 目录
        val file = File(newPath);
        file.mkdirs();//如果文件夹不存在，则递归
        fileNames.forEach {
          copyFilesFassets(
            oldPath + File.separator + it, newPath + File.separator + it
          )
        }
      } else {// 文件
        val inputStream = App.appContext.assets.open(oldPath)
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

  /**
   * 获取system-app和remember-app目录下的所有appinfo
   */
  fun getAppInfoList(): List<AppInfo> {
    // 1.从system-app/boot/bfs-app-id/boot/link.json取值，将获取到内容保存到appInfo中
    // 2.将system-app中到bfs-app-id信息保存到map里面，用于后续交验
    // 3.从remember-app/boot/bfs-app-id/boot/link.json取值，补充到列表中
    val systemAppMap = getChildrenDirectoryList(APP_DIR_TYPE.SystemApp)
    val recommendAppMap = getChildrenDirectoryList(APP_DIR_TYPE.RecommendApp)
    val appInfoList = ArrayList<AppInfo>()
    val systemAppExist = HashMap<String, String>()

    systemAppMap?.forEach { (key, value) ->

      getFileContent(
        value + File.separator + DIR_BOOT + File.separator + FILE_LINK_JSON
      )?.let { content ->
        JsonUtil.getAppInfoFromLinkJson(content, APP_DIR_TYPE.SystemApp)
      }?.apply {
        /*getDAppInfo(appInfo)?.let { dAppInfo ->
            dAppUrl = getAppDenoUrl(appInfo, dAppInfo)
        }*/
        dAppUrl = key
        appInfoList.add(this)
        systemAppExist[key] = value
      }
    }
    // 将 system 目录下的所有app进行校验是否是 recommend 的
    appInfoList.forEach { appInfo ->
      if (recommendAppMap?.containsKey(appInfo.bfsAppId) == true) {
        appInfo.isRecommendApp = true
      }
    }
    recommendAppMap?.forEach { (key, value) ->
      if (!systemAppExist.containsKey(key)) {
        val appInfo = getFileContent(
          value + File.separator + DIR_BOOT + File.separator + FILE_LINK_JSON
        )?.let { content ->
          println("content:$content")
          JsonUtil.getAppInfoFromLinkJson(content, APP_DIR_TYPE.RecommendApp)
        }?.apply {
          this.isRecommendApp = true
          appInfoList.add(this)
        }
      }
    }
    println("systemAppMap: ${systemAppMap?.toList()?.joinToString(",")}")
    println("recommendAppMap: ${recommendAppMap?.toList()?.joinToString(",")}")
    println("appInfoList: ${appInfoList.joinToString(",")}")
    return appInfoList
  }

  /**
   * 获取需要轮询的应用列表
   */
  fun getScheduleAppList(): List<AppInfo> {
    return getAppInfoList()
  }

  /**
   * 根据bfsAppId获取DAppInfo的版本信息
   */
  fun getDAppInfo(appInfo: AppInfo): DAppInfo? {
    val content = when (appInfo.appDirType) {
      APP_DIR_TYPE.AssetsApp -> {
        App.appContext.assets.open("${appInfo.bfsAppId}/$DIR_BOOT/$FILE_BFSA_META_JSON")
          .bufferedReader().use { it.readText() }
      }
      else -> {
        val path =
          getAppRootDirectory(appInfo) + File.separator + DIR_BOOT + File.separator + FILE_BFSA_META_JSON
        getFileContent(path)
      }
    }
    return JsonUtil.getDAppInfoFromBFSA(content)
  }

  /**
   * 获取DAppInfo的版本信息
   */
  fun getDAppInfo(bfsAppId: String, appType: APP_DIR_TYPE = APP_DIR_TYPE.SystemApp): DAppInfo? {
    return getDAppInfo(
      AppInfo(version = "", bfsAppId = bfsAppId, name = "", appDirType = appType, icon = "")
    )
  }

  /**
   * 获取 DCIM和Picture下面的相册信息
   */
  fun getMediaInfoList(arrayList: ArrayList<File>): HashMap<String, ArrayList<MediaInfo>> {
    val maps = hashMapOf<String, ArrayList<MediaInfo>>()
    arrayList.forEach { path ->
      traverseDCIM(path.absolutePath, maps)
    }
    return maps
  }

  /**
   * 遍历当前目录及其子目录所有文件
   */
  private fun traverseDCIM(path: String, maps: HashMap<String, ArrayList<MediaInfo>>) {
    val defaultPicture = "Pictures"
    if (!maps.containsKey(defaultPicture)) maps[defaultPicture] =
      arrayListOf() // 默认先创建一个图片目录，用于保存根目录存在的图片
    File(path).listFiles()?.forEach inLoop@{ file ->
      val name = file.name
      if (name.startsWith(".")) return@inLoop // 判断第一个字符如果是. 不执行当前文件，直接continue
      if (file.isFile) {
        // 判断文件是否符合要求，如果符合，添加到maps
        file.createMediaInfo()?.let { maps[defaultPicture]!!.add(it) }
      } else if (file.isDirectory) {
        val list = maps[name] ?: arrayListOf()
        file.walk().iterator().forEach subLoop@{ subFile ->
          if (subFile.name.startsWith(".")) return@subLoop
          if (subFile.isFile) {
            // 判断文件是否符合要求，如果符合，添加到list
            subFile.createMediaInfo()?.let { list.add(it) }
          }
        }
        if (list.isNotEmpty()) { // 如果name已存在，添加所有，如果list不存在直接添加到节点
          maps[name]?.addAll(list) ?: run { maps[name] = list }
        }
      }
    }
  }

  fun getFileType(path: String): String? {
    val lastDot = path.lastIndexOf(".")
    val suffix =
      if (lastDot < 0) "" else path.substring(lastDot + 1).uppercase(Locale.getDefault())
    var first: String? = null
    when (suffix) {
      "MP4", "M4V", "3GP", "3GPP", "3G2", "3GPP2" -> {
        first = MediaType.Video.name
      }
      "JPG", "JPEG", "PNG", "BMP", "WBMP" -> {
        first = MediaType.Image.name
      }
      "SVG" -> {
        first = MediaType.Svg.name
      }
      "GIF" -> {
        first = MediaType.Gif.name
      }
      else -> {
        first = null
      }
    }
    return first
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

  fun uninstallApp(mmid: Mmid) {
    val path =
      getAndroidRootDirectory() + File.separator + APP_DIR_TYPE.SystemApp.rootName + File.separator + mmid
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

fun File.createMediaInfo(): MediaInfo? {
  var mediaInfo: MediaInfo? = null
  getFileType(this.absolutePath)?.let {
    mediaInfo = MediaInfo(type = it, path = absolutePath)
    mediaInfo?.time = (lastModified() / 1000).toInt() // 获取文件的最后修改时间
    mediaInfo?.loadThumbnail()
  }
  return mediaInfo
}


