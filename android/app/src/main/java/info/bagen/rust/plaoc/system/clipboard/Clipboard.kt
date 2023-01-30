package info.bagen.rust.plaoc.system.clipboard

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import info.bagen.libappmgr.utils.JsonUtil
import info.bagen.rust.plaoc.App


data class ClipboardWriteResponse(val success: Boolean, val errorManager: String = "")
data class ClipboardData(val value: String, val type: String)

object Clipboard {

    fun write(
        strValue: String?,
        imageValue: String?,
        urlValue: String?,
        labelValue: String = "OcrText",
        onErrorCallback: (String) -> Unit
    ) {
        var response: ClipboardWriteResponse
        if (strValue != null) {
            response = writeClipboard(label = labelValue, strValue)
        } else if (imageValue != null) {
            response = writeClipboard(label = labelValue, imageValue)
        } else if (urlValue != null) {
            response = writeClipboard(label = labelValue, urlValue)
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

    private fun writeClipboard(label: String?, content: String?): ClipboardWriteResponse {
        val data = ClipData.newPlainText(label, content)
        return if (data != null) {
            try {
                mClipboard.setPrimaryClip(data)
            } catch (e: Exception) {
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
}

data class ClipboardWriteOption(
    val str: String? = null,
    val image: String? = null,
    val url: String? = null,
    val label: String? = null,
)
