package org.dweb_browser.core.sys.download

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dweb_browser.core.module.getAppContext
import org.dweb_browser.helper.Debugger
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.help.types.JmmAppInstallManifest
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.PureRequest
import org.dweb_browser.core.http.PureResponse
import org.dweb_browser.core.http.PureStream
import org.dweb_browser.core.http.PureStreamBody
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.ipc.Ipc
import org.dweb_browser.core.ipc.ReadableStreamIpc
import org.dweb_browser.core.ipc.helper.IpcEvent
import org.dweb_browser.core.ipc.helper.ReadableStream

internal val debugDownload = Debugger("Download")

/**
 * 下载模块
 */
class DownloadNMM :
  NativeMicroModule("download.browser.dweb", "Download Browser") {
  private val downloadModel = DownloadModel(this)

  init {
    short_name = "Download"
    categories = listOf(MICRO_MODULE_CATEGORY.Service)
  }

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {

    routes(
      "/download" bind HttpMethod.Post to definePureResponse {
        // TODO 判断当前map是否存在，如果存在，修改状态即可，如果不存在，就重新进行下载
        debugDownload("download", ipc.remote.mmid)
        val jmmAppInstallManifest =
          Json.decodeFromString<JmmAppInstallManifest>(request.body.toPureString())
        if (downloadModel.downloadApp(getAppContext(), jmmAppInstallManifest)) {
          return@definePureResponse PureResponse(HttpStatusCode.OK)
        } else {
          return@definePureResponse PureResponse(HttpStatusCode.ExpectationFailed).body("Download Process exists ${ipc.remote.mmid}")
        }
      },
      "/check" bind HttpMethod.Get to defineBooleanResponse {
        debugDownload("check", ipc.remote.mmid)
        // TODO 当前的应用在map中是否存在
        downloadModel.exists(ipc.remote.mmid)
      },
      "/pause" bind HttpMethod.Get to defineBooleanResponse {
        val mmid = request.queryOrNull("mmid") ?: ipc.remote.mmid
        debugDownload("pause", "ipc->${ipc.remote.mmid}, mmid->$mmid")
//        downloadModel.updateDownloadState(mmid, JmmDownloadController.PAUSE)
        true
      },
      /**继续下载*/
      "/resume" bind HttpMethod.Get to defineBooleanResponse {
        val mmid = request.queryOrNull("mmid") ?: ipc.remote.mmid
        debugDownload("resume", "ipc->${ipc.remote.mmid}, mmid->$mmid")
//        downloadModel.updateDownloadState(mmid, JmmDownloadController.RESUME)
        true
      },
      "/cancel" bind HttpMethod.Get to defineBooleanResponse {
        val mmid = request.queryOrNull("mmid") ?: ipc.remote.mmid
        debugDownload("cancel", "ipc->${ipc.remote.mmid}, mmid->$mmid")
//        downloadModel.updateDownloadState(mmid, JmmDownloadController.CANCEL)
        true
      },
      "/listen" bind HttpMethod.Get to definePureStreamHandler {
        listen(ipc, request)
      },
      "/observe" bind HttpMethod.Get to definePureResponse {
        val mmid = request.query("mmid")
        debugDownload("observe", "ipc->${ipc.remote.mmid}, mmid->$mmid")
        val inputStream = ReadableStream { controller ->
          val off = downloadModel.onDownload { downloadInfo ->
            try {
              val jsonData = Json.encodeToString(downloadInfo)
              controller.enqueue((jsonData + "\n").toByteArray())
            } catch (e: Exception) {
              controller.closeWrite()
              e.printStackTrace()
            }
          }
          ioAsyncScope.launch { controller.awaitClose { off() } }
          ipc.onClose { off(); controller.closeWrite() }
        }
        return@definePureResponse PureResponse(
          HttpStatusCode.OK, body = PureStreamBody(inputStream.stream)
        )
      },
    )
  }

  override suspend fun _shutdown() {}

  private fun listen(ipc: Ipc, message: PureRequest): PureStream {
    debugDownload("download_listen", ipc.remote.mmid)

    val streamIpc = ReadableStreamIpc(ipc.remote, "download/${ipc.remote.mmid}")
    /// 接收一个body，body在关闭的时候，fetchIpc也会一同关闭
    streamIpc.bindIncomeStream(message.body.toPureStream())
    /// 自己nmm销毁的时候，ipc也会被全部销毁
    this.addToIpcSet(streamIpc)
    downloadModel.onDownload {
      debugDownload("download_listen", "download -> $it")
      val downloadInfo = it
      if (ipc.remote.mmid == downloadInfo.id) {
        streamIpc.postMessage(IpcEvent.fromUtf8(downloadInfo.toEvent(), downloadInfo.toData()))
      }
    }
    return streamIpc.input.stream
  }
}