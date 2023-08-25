package info.bagen.dwebbrowser.microService.sys.share

import android.content.Intent
import info.bagen.dwebbrowser.App
import info.bagen.dwebbrowser.microService.core.AndroidNativeMicroModule
import info.bagen.dwebbrowser.microService.sys.fileSystem.EFileDirectory
import info.bagen.dwebbrowser.microService.sys.share.ShareController.Companion.controller
import org.dweb_browser.helper.PromiseOut
import org.dweb_browser.helper.printDebug
import org.dweb_browser.microservice.core.BootstrapContext
import org.dweb_browser.microservice.help.MICRO_MODULE_CATEGORY
import org.dweb_browser.microservice.help.cors
import org.http4k.core.Method
import org.http4k.core.MultipartFormBody
import org.http4k.lens.Query
import org.http4k.lens.composite
import org.http4k.lens.string
import org.http4k.routing.bind
import org.http4k.routing.routes

data class ShareOptions(
  val title: String?,
  val text: String?,
  val url: String?,
)

fun debugShare(tag: String, msg: Any? = "", err: Throwable? = null) =
  printDebug("Share", tag, msg, err)

class ShareNMM : AndroidNativeMicroModule("share.sys.dweb", "share") {

  override val categories =
    mutableListOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Protocol_Service);

  private val plugin = CacheFilePlugin()
  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    val shareOption = Query.composite { spec ->
      ShareOptions(
        title = string().optional("title")(spec),
        text = string().optional("text")(spec),
        url = string().optional("url")(spec)
      )
    }
    apiRouting = routes(
      /** 分享*/
      "/share" bind Method.POST to defineHandler { request, ipc ->
        val files = mutableListOf<String>()
        val result = PromiseOut<String>()
        val ext = shareOption(request)
        try {
          val receivedForm = MultipartFormBody.from(request)
          val fileByteArray = receivedForm.files("files")
          // 写入缓存
          fileByteArray.map { file ->
            val url = plugin.writeFile(
              file.filename,
              EFileDirectory.Cache.location,
              file.content,
              false
            )
            files.add(url)
          }
        } catch (e: Exception) {
          debugShare("share catch", "e===>$e $files")
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
        return@defineHandler ShareResult(data == "OK", data)
      },
    ).cors()
  }

  data class ShareResult(val success: Boolean, val message: String)

  private fun openActivity() {
    val activity = getActivity()
    val intent = Intent(getActivity(), ShareActivity::class.java)
    intent.action = "${App.appContext.packageName}.share"
    intent.`package` = App.appContext.packageName
//         intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
//         intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
    activity?.startActivity(intent)
  }

  override suspend fun _shutdown() {
    TODO("Not yet implemented")
  }
}
