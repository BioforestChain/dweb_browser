package info.bagen.dwebbrowser.microService.sys.fileSystem

import android.util.Log
import info.bagen.dwebbrowser.App
import info.bagen.dwebbrowser.microService.helper.iterator
import info.bagen.dwebbrowser.microService.helper.readByteArray
import info.bagen.dwebbrowser.microService.helper.readInt
import info.bagen.dwebbrowser.util.JsonUtil
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.*
import java.util.*
import java.util.regex.PatternSyntaxException
import kotlin.io.path.Path
import kotlin.io.path.isSymbolicLink

class VirtualFileSystem {
    val filePermissions = mutableMapOf<path, PermissionState>()
    fun getRootPath(): String {
        return "${App.appContext.dataDir}/system-app/home/"
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
    fun list(path: String): String {
        val rootPath = getRootPath()
        val file = getFileByPath(path)
        val fileList = arrayListOf<Fs>()
        file.listFiles()?.forEach {
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
        return JsonUtil.toJson(fileList)
    }

    fun mkdir(path: String, recursive: Boolean = false): String {
        val file = getFileByPath(path)
        return try {
            val bool = when (recursive) {
                true -> {
                    file.mkdirs()
                }
                false -> {
                    file.mkdir()
                }
            }
            bool.toString()
        } catch (e: Throwable) {
            e.message.toString()
        }
    }

    /**写文件利用BufferedWriter，可以保证内存不会溢出，而且会一直写入 */
    fun write(path: String, fileContent: InputStream, options: WriteOption): String {
        val file = getFileByPath(path)
        var fileWriter: FileOutputStream? = null
        try {
            // 判断文件是否创建且需要自动创建
            if (!file.exists() && options.autoCreate) {
                file.parentFile?.mkdirs()
            }
            fileWriter = FileOutputStream(file, options.append)
            for (byte in fileContent) {
                fileWriter.write(byte)
            }
            fileWriter.close()
        } catch (e: Throwable) {
            return e.message.toString()
        } finally {
            try {
                fileWriter?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return "true"
    }

    /**
     * 流写入
     * @param path String
     * @param content InputStream
     * @param options WriteOption
     */
    fun writeByteArray(path: String, content: InputStream, options: WriteOption): String {
        val file = getFileByPath(path)
        // append=true,是不断增加的
        var fileWriter: FileOutputStream? = null
        try {
            // 判断文件是否创建且需要自动创建
            if (!file.exists() && options.autoCreate) {
                file.parentFile?.mkdirs()
            }
            fileWriter = FileOutputStream(file, options.append)
            // 如果通道关闭并且没有剩余字节可供读取，则返回 true
            while (content.available() > 0) {
                val size = content.readInt()
                // 读取指定数量的字节并从中生成字节数据包。 如果通道已关闭且没有足够的可用字节，则失败
                val chunk = content.readByteArray(size)
                fileWriter.write(chunk)
            }
        } catch (e: Throwable) {
            return e.message.toString()
        } finally {
            fileWriter?.close()
            content.close()
        }
        return "true"
    }

    /** 读文件buffer*/
    @OptIn(DelicateCoroutinesApi::class)
    fun readBuffer(path: String, callStream: (byteArray: ByteArray, index: Int) -> Unit) {
        println("read buffer.toString -> $path")
        val file = getFileByPath(path)
        val buffer = FileInputStream(file)
        GlobalScope.launch(Dispatchers.IO) {
            do {
                var i = 0
                val buf = ByteArray(1024)
                try {
                    i = buffer.read(buf)
                } catch (e: IOException) {
                    e.printStackTrace()
                } finally {
                    buffer.close()
                }
                callStream(buf, i)
            } while (i != -1)
        }
    }


    /**
     * 读取大文件
     * @param filePath
     */
    @OptIn(DelicateCoroutinesApi::class)
    fun readFile(filePath: String?, callString: (str: String, hasNextLine: Boolean) -> Unit) {
        GlobalScope.launch(Dispatchers.IO) {
            var inputStream: FileInputStream? = null
            var sc: Scanner? = null
            try {
                inputStream = FileInputStream(filePath)
                sc = Scanner(inputStream, "UTF-8")
                while (sc.hasNextLine()) {
                    val line: String = sc.nextLine()
                    callString(line, sc.hasNextLine())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
                sc?.close()
            }
        }
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

    fun rm(path: String, deepDelete: Boolean = true): String {
        val file = getFileByPath(path)
        var bool = false
        try {
            bool = when (deepDelete) {
                true -> file.deleteRecursively()
                false -> file.delete()
            }
        } catch (e: Throwable) {
            return e.message.toString()
        }
        return bool.toString()
    }

}