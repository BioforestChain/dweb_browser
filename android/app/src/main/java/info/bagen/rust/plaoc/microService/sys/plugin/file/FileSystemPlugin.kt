package info.bagen.rust.plaoc.microService.sys.file

import android.util.Log
import com.king.mlkit.vision.camera.util.LogUtils
import info.bagen.libappmgr.utils.JsonUtil
import info.bagen.rust.plaoc.App
import info.bagen.rust.plaoc.microService.sys.plugin.file.Fs
import info.bagen.rust.plaoc.microService.sys.plugin.file.LsFilter
import info.bagen.rust.plaoc.microService.sys.plugin.file.WriteOption
import info.bagen.rust.plaoc.webView.network.dWebView_host
import java.io.*
import java.nio.charset.StandardCharsets
import java.util.regex.PatternSyntaxException
import kotlin.io.path.Path
import kotlin.io.path.isSymbolicLink

typealias path = String

class FileSystemPlugin {
    val filePermissions = mutableMapOf<path, PermissionState>()
    fun getRootPath(): String {
        return "${App.appContext.dataDir}/system-app/$dWebView_host/home/"
    }

    fun getFileByPath(path: String): File {
        val pathName = getRootPath() + File.separator + path
        return File(pathName)
    }

    fun checkFilter(filers: Array<LsFilter>, file: File): Boolean {
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
    fun transformRegex(filers: Array<LsFilter>): String {
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

    fun addPermission(path: String, state: Int) {
        this.filePermissions[path] = PermissionState.valueOf(state.toString())
    }

    fun checkoutPermission(path: String): Boolean {
        return this.filePermissions.contains(path)
    }

    /**
     * filter：
     * recursive：是否递归遍历目录默认false
     * */
    fun ls(path: String, filter: Array<LsFilter>, recursive: Boolean = false): String {
        val check = transformRegex(filter)
        if (check != "ok") {
            return "Error for LsFilter rule"
        }
        val rootPath = getRootPath()
        val file = getFileByPath(path)
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
        return JsonUtil.toJson(fileList)
    }

    /** 获取文件entry列表*/
    fun list(path: String) {
        val rootPath = getRootPath()
        val file = getFileByPath(path)
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
        // TODO return createBytesFactory(ExportNative.FileSystemList, JsonUtil.toJson(fileList))
    }

    fun mkdir(path: String, recursive: Boolean = false): Boolean {
        val file = getFileByPath(path)
        val bool = when (recursive) {
            true -> {
                file.mkdirs()
            }
            false -> {
                file.mkdir()
            }
        }
        return bool
    }

    fun write(path: String, content: String, options: WriteOption): String {
        val file = getFileByPath(path)
        if (!file.exists() && options.autoCreate) {
            file.parentFile?.mkdirs()
        }
//    println("write  file->${file.absolutePath}，content->$content，append->$append，autoCreate->$autoCreate")
        try {
            val fileWriter = FileWriter(file, options.append)
            val bufferedWriter = BufferedWriter(fileWriter)
            bufferedWriter.write(content)
            bufferedWriter.close()
        } catch (e: Throwable) {
            LogUtils.d("write fail -> ${e.message}")
            return e.message.toString()
        }
        return "true"
    }

    fun read(path: String, callStream: (byteArray:ByteArray,index:Int) -> Unit) {
        println("read buffer.toString -> $path")
        val file = getFileByPath(path)
        val buffer = FileInputStream(file)
        do {
            var i = 0
            val buf = ByteArray(1024)
            try {
                i = buffer.read(buf)
            } catch (e: IOException) {
                e.printStackTrace()
            }
            callStream(buf,i)
        } while (i != -1)

    }

    fun rename(path: String, newFilePath: String): String {
        println("rename：$path,$newFilePath")
        val ordFile = getFileByPath(path)
        val newFile = getFileByPath(newFilePath)
        if (!ordFile.exists()) {
            return "The renamed file does not exist"
        }
        if (newFile.exists()) {
            return "Rename file conflict, file already exists"
        }
        return try {
            val bool = ordFile.renameTo(newFile)
            bool.toString()
        } catch (e: IOException) {
            e.printStackTrace()
            e.message.toString()
        }
    }

    fun rm(path: String, deepDelete: Boolean = true): Boolean {
        val file = getFileByPath(path)
        val bool = when (deepDelete) {
            true -> file.deleteRecursively()
            false -> file.delete()
        }
        return bool
    }

}

/**
 * 权限状态
 */
enum class PermissionState(val state: Int) {
    /**无权限 */
    NULL(0),

    /**  可读*/
    READ(1 shl 1),

    /**  可写*/
    WRITE(1 shl 2),

    /** 可执行*/
    EXECUTE(1 shl 3),

    READ_WRITE(READ.state or WRITE.state),

    READ_EXECUTE(READ.state or EXECUTE.state),

    READ_WRITE_EXECUTE(READ.state or WRITE.state or EXECUTE.state);

}
