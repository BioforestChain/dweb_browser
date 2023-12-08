package org.dweb_browser.sys.share

import io.ktor.http.HttpMethod
import kotlinx.serialization.Serializable
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.receiveMultipart
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.helper.WARNING
import org.dweb_browser.helper.printDebug
import org.dweb_browser.helper.toJsonElement

fun debugShare(tag: String, msg: Any? = "", err: Throwable? = null) =
  printDebug("Share", tag, msg, err)

@Serializable
data class ShareResult(val success: Boolean, val message: String)

@Serializable
data class ShareOptions(
  val title: String?,
  val text: String?,
  val url: String?,
)

class ShareNMM : NativeMicroModule("share.sys.dweb", "share") {
  init {
    categories = listOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Protocol_Service);
  }

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    routes(
      /** 分享*/
      "/share" bind HttpMethod.Post by defineJsonResponse {
        val shareOptions = ShareOptions(
          title = request.queryOrNull("title"),
          text = request.queryOrNull("text"),
          url = request.queryOrNull("url")
        )
        val multiPartData = try {
          request.receiveMultipart()
        } catch (e: Exception) {
          debugShare("/share", "receiveMultipart error -> ${e.message}")
          null
        }

        val result = share(shareOptions, multiPartData)
        debugShare("/share", "result => $result")
        return@defineJsonResponse ShareResult(result == "OK", result).toJsonElement()
      },
    ).cors()
  }

  override suspend fun _shutdown() {
    WARNING("Not yet implemented")
  }

}