package info.bagen.rust.plaoc.microService.sys.plugin.fileSystem

import android.media.MediaScannerConnection
import android.net.Uri
import info.bagen.rust.plaoc.microService.core.BootstrapContext
import info.bagen.rust.plaoc.microService.core.NativeMicroModule
import info.bagen.rust.plaoc.microService.helper.printdebugln
import info.bagen.rust.plaoc.microService.sys.mwebview.MultiWebViewNMM
import kotlinx.coroutines.currentCoroutineContext
import org.http4k.core.*
import org.http4k.lens.*
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.json.JSONObject
import java.io.File
import java.io.IOException


inline fun debugFileSystem(tag: String, msg: Any? = "", err: Throwable? = null) =
    printdebugln("FileSystem", tag, msg, err)

class FileSystemNMM : NativeMicroModule("file.sys.dweb") {

    val plugin = FileSystemPlugin()

    override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
        val qurey_path = Query.string().required("path")
        val qurey_directory = Query.string().required("directory")
        val qurey_optional_directory = Query.string().optional("directory")
        val qurey_data = Query.string().required("data")
        val query_recursive = Query.boolean().defaulted("recursive",false)
        apiRouting = routes(
            "/writeFile" bind Method.GET to defineHandler { request,ipc ->
                val path = qurey_path(request)
                val data = qurey_data(request)
                val recursive = query_recursive(request)
                val directory = qurey_optional_directory(request)
                plugin.Filesystem(MultiWebViewNMM.getCurrentWebViewController(ipc.remote.mmid)?.activity)
            return@defineHandler writeFile(path,directory,data,recursive)
            },
            "/getUri" bind Method.GET to defineHandler { request,ipc ->
                val path: String = qurey_path(request)
                val directory: String = qurey_directory(request)
                plugin.Filesystem(MultiWebViewNMM.getCurrentWebViewController(ipc.remote.mmid)?.activity)
                val fileObject: File = plugin.getFileObject(path, directory)
               return@defineHandler Uri.fromFile(fileObject).toString()
            },
            /** 流写入*/
            "/writeSteam" bind Method.POST to defineHandler { request ->

            },

            /** 删除*/
            "/delete" bind Method.DELETE to defineHandler { request ->

            },

            "/mkdir" bind Method.GET to defineHandler { request ->

            },
            "/rename" bind Method.GET to defineHandler { request ->

            },
        )
    }

    fun writeFile(path:String,directory:String?,data:String,recursive:Boolean): Any {
        if (directory != null) {
            // create directory because it might not exist
            val androidDir = plugin.getDirectory(directory)
            if (androidDir != null) {
                if (androidDir.exists() || androidDir.mkdirs()) {
                    // path might include directories as well
                    val fileObject = File(androidDir, path)
                    if (fileObject.parentFile.exists() || recursive && fileObject.parentFile.mkdirs()) {
                      return saveFile(fileObject, data)
                    } else {
                        return  "Parent folder doesn't exist"
                    }
                } else {
                    debugFileSystem("writeFile","Not able to create '$directory'!")
                    return "NOT_CREATED_DIR"
                }
            } else {
                debugFileSystem("writeFile","Directory ID '$directory' is not supported by plugin")
                return  "INVALID_DIR"
            }
        } else {
            // check file:// or no scheme uris
            val u = Uri.parse(path)
            if (u.scheme == null || u.scheme == "file") {
                val fileObject = File(u.path)
                if (fileObject.parentFile == null ||
                    fileObject.parentFile.exists() || recursive && fileObject.parentFile.mkdirs()
                ) {
                  return saveFile(fileObject, data)
                } else {
                    return "Parent folder doesn't exist"
                }
            } else {
                return  u.scheme + " scheme not supported"
            }
        }
        return "writeFile null"
    }

    private fun saveFile(file:File, data: String, encoding:String? = null, append:Boolean = false):Any {
        val charset = plugin.getEncoding(encoding)
        if (encoding != null && charset == null) {
            return "Unsupported encoding provided: $encoding"
        }

        try {
            plugin.saveFile(file, data, charset, append)
            debugFileSystem("saveFile","File '" + file.absolutePath + "' saved!")
            val result = JSONObject()
            result.put("uri", Uri.fromFile(file).toString())
            return result
        } catch (ex: IOException) {
            debugFileSystem("saveFile","Creating file '" + file.getPath() + "' with charset '" + charset + "' failed. Error: " + ex.message)
            return  "FILE_NOTCREATED"
        } catch (ex: IllegalArgumentException) {
            return  "The supplied data is not valid base64 content."
        }
    }


    /**
     * 如果给定的目录字符串是公共存储目录，用户或其他应用程序可以访问该目录，则为真。
     * @param directory the directory string.
     */
    private fun isPublicDirectory(directory: String): Boolean {
        return ("DOCUMENTS" == directory || "EXTERNAL_STORAGE" == directory)
    }


    override suspend fun _shutdown() {
        TODO("Not yet implemented")
    }

}
