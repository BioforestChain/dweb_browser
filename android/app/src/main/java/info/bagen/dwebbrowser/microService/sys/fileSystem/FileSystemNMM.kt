package info.bagen.dwebbrowser.microService.sys.fileSystem

import info.bagen.dwebbrowser.App
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dweb_browser.helper.Debugger
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
      "/savePictures" bind HttpMethod.Post to definePureResponse {
        try {
          if (requestPermissions()) {
            val multiPartData = request.receiveMultipart()
            val files = mutableListOf<String>()
            multiPartData.forEachPart { partData ->
              when (partData) {
                is PartData.FileItem -> {
                  partData.originalFileName?.also { filename ->
                    val savedFilePath = FileSystemPlugin.saveToPictureDirectory(
                      filename, partData.streamProvider(),
                    )
                    files.add(savedFilePath)
                  }
                }
                else -> {}
              }
              partData.dispose()
            }
            return@definePureResponse PureResponse(body = PureStringBody(Json.encodeToString(files)))

          } else {
            return@definePureResponse PureResponse(HttpStatusCode.Forbidden)
          }
        } catch (e: Exception) {
          debugFileSystem("savePictures", "Error", e)
          return@definePureResponse PureResponse(
            HttpStatusCode.ExpectationFailed,
            body = PureStringBody(e.message ?: e.stackTraceToString())
          )
        }
      }
    ).cors()
  }

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
