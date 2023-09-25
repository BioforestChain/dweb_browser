package info.bagen.dwebbrowser.microService.sys.fileSystem

import info.bagen.dwebbrowser.App
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.toJsonElement
import org.dweb_browser.microservice.core.BootstrapContext
import org.dweb_browser.microservice.core.NativeMicroModule
import org.dweb_browser.microservice.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.microservice.http.PureResponse
import org.dweb_browser.microservice.http.PureStringBody
import org.dweb_browser.microservice.http.bind
import org.dweb_browser.microservice.http.receiveMultipart

val debugFileSystem = Debugger("FsNmm")

class FileSystemNMM : NativeMicroModule("file.sys.dweb", "file") {

  init {
    categories = listOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Protocol_Service);
  }

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    routes(
      "/writeFile" bind HttpMethod.Get to defineEmptyResponse {
//                val path = qurey_path(request)
//                val data = qurey_data(request)
//                val recursive = query_recursive(request)
//                val directory = qurey_optional_directory(request)
//                plugin.fileSystemPlugin.Filesystem(MultiWebViewNMM.getCurrentWebViewController(ipc.remote.mmid)?.activity)
//            return@defineHandler plugin.writeFile(path,directory,data,recursive)
      },
      "/getUri" bind HttpMethod.Get to defineEmptyResponse {
//                val path: String = qurey_path(request)
//                val directory: String = qurey_directory(request)
//                plugin.fileSystemPlugin.Filesystem(MultiWebViewNMM.getCurrentWebViewController(ipc.remote.mmid)?.activity)
//                val fileObject: File = plugin.fileSystemPlugin.getFileObject(path, directory)
//               return@defineHandler Uri.fromFile(fileObject).toString()
      },
      /** 流写入*/
      "/writeSteam" bind HttpMethod.Post to defineEmptyResponse {

      },
      /** 删除*/
      "/delete" bind HttpMethod.Delete to defineEmptyResponse {

      },

      "/mkdir" bind HttpMethod.Get to defineEmptyResponse {

      },
      "/rename" bind HttpMethod.Get to defineEmptyResponse {

      },
      "/savePictures" bind HttpMethod.Post to defineJsonResponse {
        var success = false
        try {
          if (requestPermissions()) {
            val multiPartData = request.receiveMultipart()
            multiPartData.forEachPart { partData ->
              when (partData) {
                is PartData.FileItem -> {
                  partData.originalFileName?.also { filename ->
                    success = FileSystemPlugin.saveToPictureDirectory(
                      filename, partData.streamProvider(),
                    )
                  }
                }
                else -> {}
              }
              partData.dispose()
            }
            return@defineJsonResponse DwebResult(success).toJsonElement()
          } else {
            return@defineJsonResponse DwebResult(false, "User denied permission！").toJsonElement()
          }
        } catch (e: Exception) {
          debugFileSystem("savePictures", "Error", e)
          return@defineJsonResponse DwebResult(false, e.message ?: "").toJsonElement()
        }
      }
    ).cors()
  }

  @Serializable
  data class DwebResult(val success: Boolean, val message: String = "")

  override suspend fun _shutdown() {
    TODO("Not yet implemented")
  }

  private suspend fun requestPermissions(): Boolean {
    App.startActivity(FileSystemActivity::class.java) {
      // 有啥参数？好像没有
    }
    return FileSystemController.controller.waitPermissionGrants()
  }
}
