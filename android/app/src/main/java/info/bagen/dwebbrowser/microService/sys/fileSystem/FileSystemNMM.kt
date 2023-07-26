package info.bagen.dwebbrowser.microService.sys.fileSystem

import org.dweb_browser.helper.*
import org.dweb_browser.microservice.core.BootstrapContext
import org.dweb_browser.microservice.core.NativeMicroModule
import org.http4k.core.*
import org.http4k.routing.bind
import org.http4k.routing.routes

fun debugFileSystem(tag: String, msg: Any? = "", err: Throwable? = null) =
  printdebugln("FileSystem", tag, msg, err)

class FileSystemNMM : NativeMicroModule("file.sys.dweb","file") {

  override val categories = mutableListOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Protocol_Service);

//    val plugin = CacheFilePlugin()

    override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
//        val qurey_path = Query.string().required("path")
//        val qurey_directory = Query.string().required("directory")
//        val qurey_optional_directory = Query.string().optional("directory")
//        val qurey_data = Query.string().required("data")
//        val query_recursive = Query.boolean().defaulted("recursive",false)
        apiRouting = routes(
            "/writeFile" bind Method.GET to defineHandler { request,ipc ->
//                val path = qurey_path(request)
//                val data = qurey_data(request)
//                val recursive = query_recursive(request)
//                val directory = qurey_optional_directory(request)
//                plugin.fileSystemPlugin.Filesystem(MultiWebViewNMM.getCurrentWebViewController(ipc.remote.mmid)?.activity)
//            return@defineHandler plugin.writeFile(path,directory,data,recursive)
            },
            "/getUri" bind Method.GET to defineHandler { request,ipc ->
//                val path: String = qurey_path(request)
//                val directory: String = qurey_directory(request)
//                plugin.fileSystemPlugin.Filesystem(MultiWebViewNMM.getCurrentWebViewController(ipc.remote.mmid)?.activity)
//                val fileObject: File = plugin.fileSystemPlugin.getFileObject(path, directory)
//               return@defineHandler Uri.fromFile(fileObject).toString()
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


    override suspend fun _shutdown() {
        TODO("Not yet implemented")
    }

}
