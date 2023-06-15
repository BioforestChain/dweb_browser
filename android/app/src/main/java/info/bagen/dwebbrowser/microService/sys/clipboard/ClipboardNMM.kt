package info.bagen.dwebbrowser.microService.sys.clipboard

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import info.bagen.dwebbrowser.util.JsonUtil
import info.bagen.dwebbrowser.App
import org.dweb_browser.helper.*
import org.dweb_browser.microservice.core.BootstrapContext
import org.dweb_browser.microservice.core.NativeMicroModule
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.lens.Query
import org.http4k.lens.string
import org.http4k.routing.bind
import org.http4k.routing.routes

data class ClipboardWriteResponse(val success: Boolean, val errorManager: String = "")
data class ClipboardData(val value: String, val type: String)

fun debugClipboard(tag: String, msg: Any? = "", err: Throwable? = null) =
  printdebugln("Clipboard", tag, msg, err)

/** 剪切板微模块*/
class ClipboardNMM : NativeMicroModule("clipboard.sys.dweb") {
    override suspend fun _bootstrap(bootstrapContext: BootstrapContext)
 {
        apiRouting = routes(
            /** 读取剪切板*/
            "/read" bind Method.GET to defineHandler { request ->
                val read = read()
                debugClipboard("/read",read)
                Response(Status.OK).body(read)
            },
            /**
             * 写入剪切板
             * fetch("file://clipboard.sys.dweb/write?xxx=xxx")
             * */
            "/write" bind Method.GET to defineHandler { request ->
                val string = Query.string().optional("string")(request)
                val image = Query.string().optional("image")(request)
                val url = Query.string().optional("url")(request)
                val label = Query.string().optional("label")(request)
                debugClipboard("/write","string:${string},image:${image},url:${url},label:${label}")
                // 如果都是空
                if (image.isNullOrEmpty() && url.isNullOrEmpty() && url.isNullOrEmpty()) {
                    Response(Status.UNSATISFIABLE_PARAMETERS)
                }
                write(string,image,url,labelValue = label) {
                    Response(Status.OK).body(it)
                }
                true
            }
        )
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
        return JsonUtil.toJson(clipboardData)
    }

    private val mClipboard =
        App.appContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

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
                    item?.coerceToText(App.appContext).toString()
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
