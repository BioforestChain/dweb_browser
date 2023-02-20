package info.bagen.rust.plaoc.microService.sys.plugin.clipboard

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import info.bagen.libappmgr.utils.JsonUtil
import info.bagen.rust.plaoc.App
import info.bagen.rust.plaoc.microService.core.NativeMicroModule
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.queries
import org.http4k.routing.bind
import org.http4k.routing.routes


data class ClipboardWriteResponse(val success: Boolean, val errorManager: String = "")
data class ClipboardData(val value: String, val type: String)

enum class EWriteOptions(val value:String) {
    EString("string"),
    EImage("image"),
    EUrl("url")
}

object ClipboardNMM : NativeMicroModule("clipboard.sys.dweb") {
    override suspend fun _bootstrap() {
        apiRouting = routes(
            "/read" bind Method.GET to defineHandler { request ->
                println("Clipboard#apiRouting read===>$mmid  ${request.uri.path} ")
                val read = read()
                Response(Status.OK,read)
            },
            "/write" bind Method.GET to defineHandler { request ->
                println("Clipboard#apiRouting write===>$mmid  ${request.uri.path} ${request.uri.queries()}")
                for ((key,value) in request.uri.queries()) {
                    // fetch("file://clipboard.sys.dweb/write?xxx=xxx")
                    when(key) {
                        EWriteOptions.EString.value -> write(string = value){
                            Response(Status.OK,it)
                        }
                        EWriteOptions.EImage.value -> write(image = value){
                            Response(Status.OK,it)
                        }
                        EWriteOptions.EUrl.value -> write(url = value){
                            Response(Status.OK,it)
                        }
                        else -> {
                            Response(Status.UNSATISFIABLE_PARAMETERS)
                        }
                    }
                }
                true
            }
        )
    }

    fun write(
        string: String? = null,
        image: String?= null,
        url: String?= null,
        labelValue: String = "OcrText",
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
