package info.bagen.rust.plaoc.microService.sys.plugin.share

import android.net.Uri
import info.bagen.rust.plaoc.microService.sys.plugin.fileSystem.FileSystemPlugin
import info.bagen.rust.plaoc.microService.sys.plugin.fileSystem.debugFileSystem
import java.io.File
import java.io.IOException
import java.io.InputStream

class CacheFilePlugin {

    val fileSystemPlugin = FileSystemPlugin()

    fun writeFile(path: String, directory: String?, data: InputStream, recursive: Boolean): String {
        if (directory != null) {
            // 创建目录，因为它可能不存在
            val androidDir = fileSystemPlugin.getDirectory(directory)
            if (androidDir != null) {
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
                    debugFileSystem("writeFile", "Not able to create '$directory'!")
                    "NOT_CREATED_DIR"
                }
            } else {
                debugFileSystem("writeFile", "Directory ID '$directory' is not supported by plugin")
                return "INVALID_DIR"
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
            fileSystemPlugin.saveFile(file, data, append)
            debugFileSystem("saveFile", "File '" + file.absolutePath + "' saved!")
            Uri.fromFile(file).toString()
        } catch (ex: IOException) {
            debugFileSystem(
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