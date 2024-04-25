package org.dweb_browser.sys.clipboard

import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.helper.Debugger
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.pure.http.PureResponse
import org.dweb_browser.pure.http.PureStringBody

val debugClipboard = Debugger("Clipboard")

@Serializable
data class ClipboardWriteResponse(val success: Boolean, val errorManager: String = "")

@Serializable
data class ClipboardData(val value: String, val type: String)

/** 剪切板微模块*/
class ClipboardNMM : NativeMicroModule("clipboard.sys.dweb", "clipboard") {

  init {
    categories = listOf(
      MICRO_MODULE_CATEGORY.Service,
      MICRO_MODULE_CATEGORY.Process_Service
    );
  }

  inner class ClipboardRuntime(override val bootstrapContext: BootstrapContext) : NativeRuntime() {

    private val clipboardManage = ClipboardManage()

    override suspend fun _bootstrap() {
      routes(/** 读取剪切板*/
        "/read" bind PureMethod.GET by definePureResponse {
          val read = read()
          debugClipboard("/read", read)
          PureResponse(HttpStatusCode.OK, body = PureStringBody(read))
        },
        /**
         * 写入剪切板
         * fetch("file://clipboard.sys.dweb/write?xxx=xxx")
         * */
        "/write" bind PureMethod.GET by defineBooleanResponse {

          val string = request.queryOrNull("string")
          val image = request.queryOrNull("image")
          val url = request.queryOrNull("url")
          val label = request.queryOrNull("label")
          debugClipboard("/write", "string:${string},image:${image},url:${url},label:${label}")
          // 如果都是空
          if (image.isNullOrEmpty() && url.isNullOrEmpty() && url.isNullOrEmpty()) {
            PureResponse(HttpStatusCode.BadRequest)
          }
          write(string, image, url, labelValue = label) {
            PureResponse(HttpStatusCode.OK, body = PureStringBody(it))
          }
          true
        })
    }

    private fun write(
      data: String? = null,
      image: String? = null,
      url: String? = null,
      labelValue: String? = "OcrText",
      onErrorCallback: (String) -> Unit
    ) {
      val response: ClipboardWriteResponse = if (data != null) {
        clipboardManage.writeText(data, label = labelValue)
      } else if (image != null) {
        clipboardManage.writeImage(image, label = labelValue)
      } else if (url != null) {
        clipboardManage.writeUrl(url, label = labelValue)
      } else {
        onErrorCallback("No data provided")
        return
      }
      if (!response.success) {
        onErrorCallback(response.errorManager)
      }
    }

    fun read(): String {
      val clipboardData = clipboardManage.read()
      print(Json.encodeToString(clipboardData))
      return Json.encodeToString(clipboardData)
    }

    override suspend fun _shutdown() {

    }
  }

  override fun createRuntime(bootstrapContext: BootstrapContext) =
    ClipboardRuntime(bootstrapContext)
}

data class ClipboardWriteOption(
  val str: String? = null,
  val image: String? = null,
  val url: String? = null,
  val label: String? = null,
)