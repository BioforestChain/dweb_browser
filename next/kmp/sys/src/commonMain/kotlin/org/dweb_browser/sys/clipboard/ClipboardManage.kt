package org.dweb_browser.sys.clipboard

import org.dweb_browser.helper.base64UrlBinary

expect class ClipboardManage() {

  fun writeText(
    content: String,
    label: String? = null,
  ): ClipboardWriteResponse

  fun writeImage(
    base64DataUri: String,
    label: String? = null,
  ): ClipboardWriteResponse

  fun writeUrl(
    url: String,
    label: String? = null,
  ): ClipboardWriteResponse

  fun clear(): Boolean
  fun read(): ClipboardData
}

internal fun tryWriteClipboard(action: () -> Unit) = runCatching {
  action()
  ClipboardWriteResponse(true)
}.getOrElse {
  ClipboardWriteResponse(false, it.message ?: it.stackTraceToString())
}

internal fun splitBase64DataUriToFile(base64DataUri: String): Pair<ByteArray, String> {
  val (metadata, base64Content) = base64DataUri.split(",", limit = 2)
  val imageMime = metadata.replace(Regex(".*:(.*?);.*"), "$1")
  val imageData = base64Content.base64UrlBinary
  return Pair(imageData, imageMime)
}