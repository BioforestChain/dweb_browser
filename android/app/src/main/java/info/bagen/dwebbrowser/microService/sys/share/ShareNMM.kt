package info.bagen.dwebbrowser.microService.sys.share

import android.content.Intent
import info.bagen.dwebbrowser.App
import info.bagen.dwebbrowser.microService.sys.fileSystem.EFileType
import info.bagen.dwebbrowser.microService.sys.share.ShareController.Companion.controller
import io.ktor.http.ContentDisposition
import io.ktor.http.HttpMethod
import io.ktor.http.cio.MultipartEvent
import io.ktor.http.cio.parseHeaders
import io.ktor.http.cio.parseMultipart
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.Serializable
import org.dweb_browser.helper.PromiseOut
import org.dweb_browser.helper.printDebug
import org.dweb_browser.helper.toJsonElement
import org.dweb_browser.microservice.core.AndroidNativeMicroModule
import org.dweb_browser.microservice.core.BootstrapContext
import org.dweb_browser.microservice.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.microservice.http.bind
import org.dweb_browser.microservice.http.routes

data class ShareOptions(
  val title: String?,
  val text: String?,
  val url: String?,
)

fun debugShare(tag: String, msg: Any? = "", err: Throwable? = null) =
  printDebug("Share", tag, msg, err)

class ShareNMM : AndroidNativeMicroModule("share.sys.dweb", "share") {

  init {
    categories = listOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Protocol_Service);
  }

  private val plugin = CacheFilePlugin()
  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
//    val shareOption = Query.composite { spec ->
//      ShareOptions(
//        title = string().optional("title")(spec),
//        text = string().optional("text")(spec),
//        url = string().optional("url")(spec)
//      )
//    }
    routes(
      /** 分享*/
      "/share" bind HttpMethod.Post to defineJsonResponse {
        val files = mutableListOf<String>()
        val result = PromiseOut<String>()
        val ext = ShareOptions(
          title = request.query("title"),
          text = request.query("text"),
          url = request.query("url")
        )
//        try {
        val readChannel = request.body.toPureStream().getReader("share /share")
        val httpHeadersMap = parseHeaders(readChannel)

        ioAsyncScope.parseMultipart(readChannel, httpHeadersMap).consumeEach {
          if (it is MultipartEvent.MultipartPart) {
            val headers = it.headers.await()
            val contentDisposition =
              headers["Content-Disposition"]?.let { ContentDisposition.parse(it.toString()) }
            val filename = contentDisposition?.parameter("filename")
//
            if (filename != null) {
              val url = plugin.writeFile(
                filename,
                EFileDirectory.Cache.location,
                it.body.toInputStream(),
                false
              )

              it.release()
              files.add(url)
            }
          }
        }

//          var a = PartData({
//            controller.activity.finish()
//          }, io.ktor.http.Headers(request.headers.toMap()))
//
//
//          MultipartEvent.Preamble(request.body.toPureStream().getReader().readPacket())
//          val receivedForm = MultipartFormBody.from(request)
//          val fileByteArray = receivedForm.files("files")
//          // 写入缓存
//          fileByteArray.map { file ->
//            val url = plugin.writeFile(
//              file.filename,
//              EFileDirectory.Cache.location,
//              file.content,
//              false
//            )
//            files.add(url)
//          }
//        } catch (e: Exception) {
//          debugShare("share catch", "e===>$e $files")
//        }
        openActivity()
        controller.waitActivityResultLauncherCreated()

        SharePlugin.share(controller, ext.title, ext.text, ext.url, files, result)

        // 等待结果回调
        controller.activity?.getShareData { it ->
          result.resolve(it)
        }

        val data = result.waitPromise()
        debugShare("share", "result => $data")
        if (data !== "OK") {
          controller.activity?.finish()
        }
        return@defineJsonResponse ShareResult(data == "OK", data).toJsonElement()
      },
    )
//      .cors()
  }

  @Serializable
  data class ShareResult(val success: Boolean, val message: String)

  private fun openActivity() {
    val context = getAppContext()
    val intent = Intent(context, ShareActivity::class.java)
    intent.action = "${App.appContext.packageName}.share"
    intent.`package` = App.appContext.packageName
//         intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
    context.startActivity(intent)
  }

  override suspend fun _shutdown() {
    TODO("Not yet implemented")
  }
}
