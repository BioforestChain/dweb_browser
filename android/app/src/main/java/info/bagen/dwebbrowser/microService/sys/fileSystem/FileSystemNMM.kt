package info.bagen.dwebbrowser.microService.sys.fileSystem

import info.bagen.dwebbrowser.App
import info.bagen.dwebbrowser.microService.sys.fileSystem.FileSystemController.Companion.controller
import info.bagen.dwebbrowser.microService.sys.share.debugShare
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dweb_browser.helper.printDebug
import org.dweb_browser.microservice.core.BootstrapContext
import org.dweb_browser.microservice.core.NativeMicroModule
import org.dweb_browser.microservice.help.cors
import org.dweb_browser.microservice.help.jsonBody
import org.dweb_browser.microservice.help.types.MICRO_MODULE_CATEGORY
import org.http4k.core.Method
import org.http4k.core.MultipartFormBody
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.bind
import org.http4k.routing.routes

fun debugFileSystem(tag: String, msg: Any? = "", err: Throwable? = null) =
  printDebug("FileSystem", tag, msg, err)

class FileSystemNMM : NativeMicroModule("file.sys.dweb", "file") {

  init {
    categories = listOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Protocol_Service);
  }

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
//    val qurey_path = Query.string().required("path")
//    val qurey_directory = Query.string().required("directory")
//    val qurey_optional_directory = Query.string().optional("directory")
//    val qurey_data = Query.string().required("data")
//    val query_recursive = Query.boolean().defaulted("recursive", false)
    apiRouting = routes(
      "/writeFile" bind Method.GET to defineHandler { request, ipc ->
//        val path = qurey_path(request)
//        val data = qurey_data(request)
//        val recursive = query_recursive(request)
//        val directory = qurey_optional_directory(request)
//        plugin.fileSystemPlugin.Filesystem(MultiWebViewNMM.getCurrentWebViewController(ipc.remote.mmid)?.activity)
//        return@defineHandler plugin.writeFile(path,directory,data,recursive)
      },
      "/getUri" bind Method.GET to defineHandler { request, ipc ->
//        val path: String = qurey_path(request)
//        val directory: String = qurey_directory(request)
//        plugin.fileSystemPlugin.Filesystem(MultiWebViewNMM.getCurrentWebViewController(ipc.remote.mmid)?.activity)
//        val fileObject: File = plugin.fileSystemPlugin.getFileObject(path, directory)
//        return@defineHandler Uri.fromFile(fileObject).toString()
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
      "/savePictures" bind Method.POST to defineHandler { request, ipc ->
        try {
          openActivity()

          if (controller.waitPermissionGrants()) {
            val receivedForm = MultipartFormBody.from(request)
            val fileByteArray = receivedForm.files("files")
            // 写入到Pictures目录
            fileByteArray.map { multipartFormFile ->
              FileSystemPlugin.saveToPictureDirectory(
                multipartFormFile.filename, multipartFormFile.content
              )
            }
          } else {
            return@defineHandler Response(Status.OK).jsonBody(
              Json.encodeToString(
                FilesResult(
                  false,
                  "User denied permission！"
                )
              )
            )
          }
        } catch (e: Exception) {
          debugShare("share catch", "e===>$e")
          return@defineHandler Response(Status.OK).jsonBody(
            Json.encodeToString(
              FilesResult(
                false,
                e.message ?: ""
              )
            )
          )
        }
        return@defineHandler Response(Status.OK).jsonBody(
          Json.encodeToString(
            FilesResult(
              true,
              "Saved successfully！"
            )
          )
        )
      }
    ).cors()
  }

  @Serializable
  data class FilesResult(val success: Boolean, val message: String)

  override suspend fun _shutdown() {
    TODO("Not yet implemented")
  }

  private fun openActivity() {
    App.startActivity(FileSystemActivity::class.java) {
      // 有啥参数？好像没有
    }
  }
}
