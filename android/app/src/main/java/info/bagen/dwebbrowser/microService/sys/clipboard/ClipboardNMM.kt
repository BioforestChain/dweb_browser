package info.bagen.dwebbrowser.microService.sys.clipboard

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dweb_browser.core.getAppContext
import org.dweb_browser.helper.printDebug
import org.dweb_browser.microservice.core.BootstrapContext
import org.dweb_browser.microservice.core.NativeMicroModule
import org.dweb_browser.microservice.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.microservice.http.PureResponse
import org.dweb_browser.microservice.http.PureStringBody
import org.dweb_browser.microservice.http.bind

data class ClipboardWriteResponse(val success: Boolean, val errorManager: String = "")
data class ClipboardData(val value: String, val type: String)

fun debugClipboard(tag: String, msg: Any? = "", err: Throwable? = null) =
  printDebug("Clipboard", tag, msg, err)

/** 剪切板微模块*/
class ClipboardNMM : NativeMicroModule("clipboard.sys.dweb", "clipboard") {

  init {
    categories = listOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Process_Service);
  }

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    routes(/** 读取剪切板*/
      "/read" bind HttpMethod.Get to definePureResponse {
        val read = read()
        debugClipboard("/read", read)
        PureResponse(HttpStatusCode.OK, body = PureStringBody(read))
      },
      /**
       * 写入剪切板
       * fetch("file://clipboard.sys.dweb/write?xxx=xxx")
       * */
      "/write" bind HttpMethod.Get to defineBooleanResponse {

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

  fun write(
    string: String? = null,
    image: String? = null,
    url: String? = null,
    labelValue: String? = "OcrText",
    onErrorCallback: (String) -> Unit
  ) {
    val response: ClipboardWriteResponse
    if (string != null) {
      response = writeClipboard(label = labelValue, string)
    } else if (image != null) {
      response = writeClipboard(label = labelValue, image)
    } else if (url != null) {
      response = writeClipboard(label = labelValue, url)
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
    return Json.encodeToString(clipboardData)
  }

  private val mClipboard =
    getAppContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

  /**
   *label – 剪辑数据的用户可见标签。
   * content——剪辑中的实际文本。
   * */
  private fun writeClipboard(label: String?, content: String?): ClipboardWriteResponse {
    val data = ClipData.newPlainText(label, content)
    return if (data != null) {
      try {
        mClipboard.setPrimaryClip(data)
      } catch (e: Throwable) {
        return ClipboardWriteResponse(false, "Writing to the clipboard failed")
      }
      ClipboardWriteResponse(true)
    } else {
      ClipboardWriteResponse(false, "Problem formatting data")
    }
  }

  private fun readClipboard(): ClipboardData {
    var value: CharSequence? = null
    if (mClipboard.hasPrimaryClip()) {
      value =
        if (mClipboard.primaryClipDescription?.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) == true) {
          val item = mClipboard.primaryClip?.getItemAt(0)
          item?.text
        } else {
          val item = mClipboard.primaryClip?.getItemAt(0)
          item?.coerceToText(getAppContext()).toString()
        }
    }
    var type = "text/plain"
    if (value != null && value.toString().startsWith("data:")) {
      type = value.toString().split(";").toTypedArray()[0].split(":").toTypedArray()[1]
    }
    return ClipboardData(value.toString(), type)
  }

  override suspend fun _shutdown() {
    TODO("Not yet implemented")
  }
}

data class ClipboardWriteOption(
  val str: String? = null,
  val image: String? = null,
  val url: String? = null,
  val label: String? = null,
)
