package info.bagen.dwebbrowser.microService.sys.share

import android.content.Intent
import info.bagen.dwebbrowser.App
import info.bagen.dwebbrowser.microService.sys.fileSystem.EFileType
import info.bagen.dwebbrowser.microService.sys.share.ShareController.Companion.controller
import io.ktor.http.HttpMethod
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import kotlinx.serialization.Serializable
import org.dweb_browser.core.module.getAppContext
import org.dweb_browser.helper.PromiseOut
import org.dweb_browser.helper.printDebug
import org.dweb_browser.helper.toJsonElement
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.http.receiveMultipart

data class ShareOptions(
  val title: String?,
  val text: String?,
  val url: String?,
)

fun debugShare(tag: String, msg: Any? = "", err: Throwable? = null) =
  printDebug("Share", tag, msg, err)

class ShareNMM : NativeMicroModule("share.sys.dweb", "share") {

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
          title = request.queryOrNull("title"),
          text = request.queryOrNull("text"),
          url = request.queryOrNull("url")
        )
        val multiPartData = request.receiveMultipart()
        multiPartData.forEachPart { partData ->
          when (partData) {
            is PartData.FileItem -> {
              partData.originalFileName?.also { filename ->
                val url = plugin.writeFile(
                  filename, EFileType.Cache, partData.streamProvider(), false
                )
                files.add(url)
              }
            }

            else -> {}
          }
          partData.dispose()
        }

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
    ).cors()
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
