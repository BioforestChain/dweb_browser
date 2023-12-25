package org.dweb_browser.sys.clipboard

import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.PureResponse
import org.dweb_browser.core.http.PureStringBody
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.ipc.helper.IpcMethod
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.helper.printDebug

@Serializable
data class ClipboardWriteResponse(val success: Boolean, val errorManager: String = "")

@Serializable
data class ClipboardData(val value: String, val type: String)

enum class ClipboardType {
  STRING,
  IMAGE,
  URL
}

fun debugClipboard(tag: String, msg: Any? = "", err: Throwable? = null) =
  printDebug("Clipboard", tag, msg, err)

/** 剪切板微模块*/
class ClipboardNMM : NativeMicroModule("clipboard.sys.dweb", "clipboard") {

  init {
    categories = listOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Process_Service);
  }

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    routes(/** 读取剪切板*/
      "/read" bind IpcMethod.GET by definePureResponse {
        val read = read()
        debugClipboard("/read", read)
        PureResponse(HttpStatusCode.OK, body = PureStringBody(read))
      },
      /**
       * 写入剪切板
       * fetch("file://clipboard.sys.dweb/write?xxx=xxx")
       * */
      "/write" bind IpcMethod.GET by defineBooleanResponse {

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
    string: String? = null,
    image: String? = null,
    url: String? = null,
    labelValue: String? = "OcrText",
    onErrorCallback: (String) -> Unit
  ) {
    val response: ClipboardWriteResponse = if (string != null) {
      writeClipboard(label = labelValue, string, type = ClipboardType.STRING)
    } else if (image != null) {
      writeClipboard(label = labelValue, image, type = ClipboardType.IMAGE)
    } else if (url != null) {
      writeClipboard(label = labelValue, url, type = ClipboardType.URL)
    } else {
      onErrorCallback("No data provided")
      return
    }
    if (!response.success) {
      onErrorCallback(response.errorManager)
    }
  }

  fun read(): String {
    val clipboardData = readClipboard()
    print(Json.encodeToString(clipboardData))
    return Json.encodeToString(clipboardData)
  }

  override suspend fun _shutdown() {

  }
}

data class ClipboardWriteOption(
  val str: String? = null,
  val image: String? = null,
  val url: String? = null,
  val label: String? = null,
)