package org.dweb_browser.sys.clipboard

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.module.getAppContext

private val mClipboard =
  NativeMicroModule.getAppContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

/**
 *label – 剪辑数据的用户可见标签。
 * content——剪辑中的实际文本。
 * */
actual fun writeClipboard(
  label: String?,
  content: String?,
  type: ClipboardType
): ClipboardWriteResponse {

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

actual fun readClipboard(): ClipboardData {
  var value: CharSequence? = null
  if (mClipboard.hasPrimaryClip()) {
    value =
      if (mClipboard.primaryClipDescription?.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) == true) {
        val item = mClipboard.primaryClip?.getItemAt(0)
        item?.text
      } else {
        val item = mClipboard.primaryClip?.getItemAt(0)
        item?.coerceToText(NativeMicroModule.getAppContext()).toString()
      }
  }
  var type = "text/plain"
  if (value != null && value.toString().startsWith("data:")) {
    type = value.toString().split(";").toTypedArray()[0].split(":").toTypedArray()[1]
  }
  return ClipboardData(value.toString(), type)
}