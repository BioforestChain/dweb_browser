package info.bagen.libappmgr.utils

import android.net.Uri
import android.os.Build
import info.bagen.libappmgr.entity.AppInfo
import info.bagen.libappmgr.entity.DAppInfo
import info.bagen.libappmgr.system.media.MediaInfo
import info.bagen.libappmgr.system.media.MediaType
import info.bagen.libappmgr.system.media.loadThumbnail
import info.bagen.libappmgr.utils.FilesUtil.getFileType
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

enum class APP_DIR_TYPE(val rootName: String) {
    // 内置应用
    RecommendApp(rootName = "recommend-app"),

    // 下载应用
    SystemApp(rootName = "system-app"),

    // 客户应用
    UserApp(rootName = "user-app"),

    // Assets
    AssetsApp(rootName = ""),
}

/**
 * 主要用于文件的存储和读取操作，包括文件的解压操作
 */
object FilesUtil {
    const val TAG: String = "FilesUtil"
    val simpleDateFormat = SimpleDateFormat("yyyyMMddhhmmss")

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
            AppContextUtil.sInstance!!.dataDir.absolutePath
        } else {
            AppContextUtil.sInstance!!.filesDir.absolutePath
        }
    }

    /**
     * 获取应用的缓存路径
     */
    fun getAppCacheDirectory(): String {
        return AppContextUtil.sInstance!!.cacheDir.absolutePath
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
        return getAppUpdateDirectory(appInfo) + File.separator + simpleDateFormat.format(Date()) + ".json"
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
            var childrenMap: HashMap<String, String> = HashMap<String, String>()
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
    private fun traverseFileTree(fileName: String): List<String> {
        return traverseFileTree(File(fileName))
    }

    /**
     * 遍历当前目录及其子目录所有文件和文件夹
     */
    private fun traverseFileTree(file: File): List<String> {
        if (!file.exists()) {
            file.mkdirs()
        }
        var fileList = arrayListOf<String>()
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
    fun getFileContent(filename: String): String? {
        // Log.d("FilesUtil", "getFileContent filename->$filename")
        var file = File(filename)
        if (!file.exists()) {
            return null
        }
        return file.bufferedReader().use { it.readText() }
    }

    /**
     * 将content信息写入到文件中
     */
    fun writeFileContent(filename: String, content: String) {
        var file = File(filename)
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
        var file: File = File(filename)
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
        } catch (e: Exception) {
        }
    }

    fun deleteQuietly(path: String, recursively: Boolean = true) {
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
            var context = AppContextUtil.sInstance
            val fileNames = context!!.assets.list(oldPath) //获取assets目录下的所有文件及目录名，空目录不会存在
            if (fileNames != null) {
                if (fileNames.isNotEmpty()) { // 目录
                    val file = File(newPath);
                    file.mkdirs();//如果文件夹不存在，则递归
                    fileNames.forEach {
                        copyFilesFassets(
                            oldPath + File.separator + it, newPath + File.separator + it
                        )
                    }
                } else {// 文件
                    var inputStream = context!!.assets.open(oldPath)
                    var outputStream = FileOutputStream(newPath)
                    var read: Int = inputStream.read()
                    while (read != -1) {
                        outputStream.write(read)
                        read = inputStream.read()
                    }
                    outputStream.flush()
                    outputStream.close()
                    inputStream.close()
                }
            }
        } catch (e: Exception) {
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
        // Log.d(TAG, "$systemAppMap , $recommendAppMap")
        systemAppMap?.forEach {
            val appInfo = getFileContent(
                it.value + File.separator + DIR_BOOT + File.separator + FILE_LINK_JSON
            )?.let { it1 ->
                JsonUtil.getAppInfoFromLinkJson(it1, APP_DIR_TYPE.SystemApp)
            }
            // Log.d(TAG, "getAppInfoList system-app $appInfo")
            appInfo?.apply {
                getDAppInfo(appInfo)?.let { dAppInfo ->
                    dAppUrl = getAppDenoUrl(appInfo, dAppInfo)
                }
                appInfoList.add(appInfo)
                systemAppExist[it.key] = it.value
            }
        }
        // 将 system 目录下的所有app进行校验是否是 recommend 的
        appInfoList.forEach { appInfo ->
            recommendAppMap?.let { map ->
                if (map.containsKey(appInfo.bfsAppId)) {
                    appInfo.isRecommendApp = true
                }
            }
        }
        recommendAppMap?.forEach {
            if (!systemAppExist.containsKey(it.key)) {
                val appInfo = getFileContent(
                    it.value + File.separator + DIR_BOOT + File.separator + FILE_LINK_JSON
                )?.let { it1 ->
                    JsonUtil.getAppInfoFromLinkJson(it1, APP_DIR_TYPE.RecommendApp)
                }
                appInfo?.apply {
                    this.isRecommendApp = true
                    appInfoList.add(this)
                }
            }
        }
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
                AppContextUtil.sInstance!!.assets.open("${appInfo.bfsAppId}/$DIR_BOOT/$FILE_BFSA_META_JSON")
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
        var defaultPicture = "Pictures"
        if (!maps.containsKey(defaultPicture)) maps[defaultPicture] =
            arrayListOf() // 默认先创建一个图片目录，用于保存根目录存在的图片
        File(path).listFiles()?.forEach inLoop@{ file ->
            var name = file.name
            if (name.startsWith(".")) return@inLoop // 判断第一个字符如果是. 不执行当前文件，直接continue
            if (file.isFile) {
                // 判断文件是否符合要求，如果符合，添加到maps
                file.createMediaInfo()?.let { maps[defaultPicture]!!.add(it) }
            } else if (file.isDirectory) {
                var list = maps[name] ?: arrayListOf()
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
        } catch (e: Exception) {
        } finally {
            try {
                fis?.close()
            } catch (e: IOException) {
                //e.printStackTrace()
            }
            return null
        }
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


