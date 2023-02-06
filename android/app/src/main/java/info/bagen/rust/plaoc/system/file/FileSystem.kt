package info.bagen.rust.plaoc.system.file

import android.system.Os
import android.util.Log
import androidx.core.net.toUri
import com.king.mlkit.vision.camera.util.LogUtils
import info.bagen.libappmgr.utils.JsonUtil
import info.bagen.rust.plaoc.App
import info.bagen.rust.plaoc.ExportNative
import info.bagen.rust.plaoc.createBytesFactory
import info.bagen.rust.plaoc.webView.network.dWebView_host
import org.json.JSONObject
import java.io.*
import java.util.regex.PatternSyntaxException
import kotlin.io.path.Path
import kotlin.io.path.isSymbolicLink

class FileSystem {

    private fun getRootPath(): String {
        return "${App.appContext.dataDir}/system-app/$dWebView_host/home/"
    }

    private fun getFileByPath(path: String): File {
        return File(getRootPath() + File.separator + path)
    }

    private fun checkFilter(filers: Array<LsFilter>, file: File): Boolean {
        var rType = true
        var rName = false
        filers.map { filer ->
            filer.type.let {
                rType = when (it) {
                    "file" -> file.isFile
                    "directory" -> file.isDirectory
                    else -> false
                }
            }
            filer.name.forEach { regex ->
                if (regex.toRegex().containsMatchIn(file.name)) {
                    rName = true
                }
            }
//      Log.i("checkFilter ${file.name}: ", "文件类型=$rType，匹配规则=$rName")
            if (rType && rName) return true
        }
        return false
    }

    /**检查正则表达式*/
    private fun transformRegex(filers: Array<LsFilter>): String {
        var errTag = ""
        try {
            filers.map { filer ->
                filer.name.withIndex().map { (i, regex) ->
                    errTag = regex
                    var reg = regex
                    // 把*.ts的写法转换为正则 /$(\\.ts)/
                    val transFileEnd = """\*\.""".toRegex()
                    if (transFileEnd.containsMatchIn(reg)) {
                        reg = reg.replace(transFileEnd, """\.""")
                    }
                    filer.name[i] = reg
                    reg.toRegex()
                }
            }
        } catch (e: PatternSyntaxException) {
            val msg = "过滤表达式${errTag}语法错误"
            Log.e("transformRegex: ", e.toString())
            return msg
        }
        return "ok"
    }

    /**
     * filter：
     * recursive：是否递归遍历目录默认false
     * */
    fun ls(path: String, filter: Array<LsFilter>, recursive: Boolean = false) {
        val check = transformRegex(filter)
        if (check != "ok") {
            return createBytesFactory(ExportNative.FileSystemLs, check)
        }
        val rootPath = getRootPath()
        val file = File(rootPath + File.separator + path)
        val fileList = arrayListOf<String>()
        when (recursive) {
            true -> { // 遍历获取
                file.walk().iterator().forEach {
                    if (checkFilter(filter, it)) {
                        fileList.add(it.absolutePath.replace(rootPath, ""))
                    }
                }
            }
            false -> {
                file.listFiles()?.forEach {
                    if (checkFilter(filter, it)) {
                        fileList.add(it.absolutePath.replace(rootPath, ""))
                    }
                }
            }
        }
        createBytesFactory(ExportNative.FileSystemLs, JsonUtil.toJson(fileList))
    }

    /** 获取文件entry列表*/
    fun list(path: String) {
        val rootPath = getRootPath()
        val file = File(rootPath + File.separator + path)
        val fileList = arrayListOf<Fs>()
        file.listFiles()?.forEach {
//    Log.i("FileSystemList, $rootPath", it.absolutePath)
            val fs = Fs(
                it.name,
                it.extension,
                it.absolutePath.replace(rootPath, ""), // path
                it.parent.replace(rootPath, ""), // cwd
                if (it.isFile) "file" else "directory",
                Path(it.absolutePath).isSymbolicLink(), // 是否是超链接文件
                it.absolutePath.replace(rootPath, ""), // 相对地址
            )
            fileList.add(fs)
        }
        createBytesFactory(ExportNative.FileSystemList, JsonUtil.toJson(fileList))
    }

    fun mkdir(path: String, recursive: Boolean = false) {
        val file = getFileByPath(path)
        val bool = when (recursive) {
            true -> {
                file.mkdirs()
            }
            false -> {
                file.mkdir()
            }
        }
        createBytesFactory(ExportNative.FileSystemMkdir, bool.toString())
    }

    fun write(
        path: String,
        content: String,
        options: WriteOption
    ) {
        val file = getFileByPath(path)
        if (!file.exists() && options.autoCreate) {
            file.parentFile?.mkdirs()
        }
//    LogUtils.d("write  file->${file.absolutePath}，content->$content，append->$append，autoCreate->$autoCreate")
        try {
            val fileWriter = FileWriter(file, options.append)
            val bufferedWriter = BufferedWriter(fileWriter)
            bufferedWriter.write(content)
            bufferedWriter.close()
        } catch (e: Exception) {
            LogUtils.d("write fail -> ${e.message}")
            return createBytesFactory(ExportNative.FileSystemWrite, e.message.toString())
        }
        createBytesFactory(ExportNative.FileSystemWrite, true.toString())
    }

    fun read(path: String) {
        val file = getFileByPath(path)
        val buffer = StringBuffer()
        try {
            file.bufferedReader().forEachLine {
                buffer.append(it)
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            createBytesFactory(ExportNative.FileSystemReadBuffer, e.message.toString())
        } catch (e: IOException) {
            e.printStackTrace()
            createBytesFactory(ExportNative.FileSystemReadBuffer, e.message.toString())
        }
        LogUtils.d("read buffer.toString -> $buffer")
        createBytesFactory(ExportNative.FileSystemRead, buffer.toString())
    }

    fun readBuffer(path: String) {
        val file = getFileByPath(path)
        val buffer = StringBuffer()
        try {
            file.bufferedReader().readLine()?.forEach {
                buffer.append(it)
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            createBytesFactory(ExportNative.FileSystemReadBuffer, e.message.toString())
        } catch (e: IOException) {
            e.printStackTrace()
            createBytesFactory(ExportNative.FileSystemReadBuffer, e.message.toString())
        }

        createBytesFactory(ExportNative.FileSystemReadBuffer, buffer.toString())
    }

    fun rename(path: String, newFilePath: String) {
        println("rename：$path,$newFilePath")
        val ordFile = getFileByPath(path)
        val newFile = getFileByPath(newFilePath)
        if (!ordFile.exists()) {
            createBytesFactory(ExportNative.FileSystemRename, "重命名文件不存在")
        }
        if (newFile.exists()) {
            createBytesFactory(ExportNative.FileSystemRename, "重命名文件冲突，文件已存在")
        }
        try {
            val bool = ordFile.renameTo(newFile)
            createBytesFactory(ExportNative.FileSystemRename, bool.toString())
        } catch (e: IOException) {
            e.printStackTrace()
            createBytesFactory(ExportNative.FileSystemRename, e.message.toString())
        }
    }

    fun rm(path: String, deepDelete: Boolean = true) {
        val file = getFileByPath(path)
        val bool = when (deepDelete) {
            true -> file.deleteRecursively()
            false -> file.delete()
        }
        createBytesFactory(ExportNative.FileSystemRm, bool.toString())
    }

    fun stat(path: String) {
        val file = getFileByPath(path)
        val statData = Os.stat(file.toString())
        var data = JSONObject()

        data.put("type", if (file.isDirectory) "directory" else "file")
        data.put("size", statData.st_size)
        data.put("mtime", statData.st_mtime)
        data.put("uri", File(path).toUri().toString())
        data.put("ctime", statData.st_ctime)
        data.put("atime", statData.st_atime)
        data.put("blksize", statData.st_blksize)
        data.put("blocks", statData.st_blocks)
        data.put("dev", statData.st_dev)
        data.put("gid", statData.st_gid)
        data.put("rdev", statData.st_rdev)
        data.put("mode", statData.st_mode)
        data.put("ino", statData.st_ino)
        data.put("uid", statData.st_uid)
        data.put("nlink", statData.st_nlink)

        createBytesFactory(ExportNative.FileSystemStat, data.toString())
    }
}

data class Fs(
    val name: String = "",
    val extname: String = "",
    val path: String = "",
    val cwd: String = "",
    val type: String = "",
    val isLink: Boolean = false,
    val relativePath: String = "",
)

data class FileLs(
    val path: String = "",
    val option: LsOption = LsOption()
)

data class LsOption(
    val filter: Array<LsFilter> = arrayOf(),
    val recursive: Boolean = false
)

enum class FileType(var value: String) { FILE(value = "file"), DIRECTORY(value = "directory") }

data class LsFilter(
    val type: String = "",
    val name: Array<String> = arrayOf()
)

data class FileRead(
    val path: String = ""
)

data class FileWrite(
    val path: String = "",
    val content: String = "",
    val option: WriteOption = WriteOption()
)

data class WriteOption(
    val append: Boolean = false,
    val autoCreate: Boolean = true,
)

data class FileRm(
    val path: String = "",
    val option: RmOption = RmOption()
)

data class FileRename(
    val path: String = "",
    val newPath: String = "",
)

data class RmOption(
    val deepDelete: Boolean = true
)

data class FileStat(
    val path: String = ""
)
