package org.dweb_browser.dwebview.engine

import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.sys.filechooser.debugFileChooser
import org.dweb_browser.sys.filechooser.ext.openSystemFileChooser
import org.dweb_browser.sys.mediacapture.MediaCaptureResult
import org.dweb_browser.sys.mediacapture.ext.mediaCapture

class DWebFileChooser(val remoteMM: MicroModule, val ioScope: CoroutineScope) : WebChromeClient() {
  override fun onShowFileChooser(
    webView: WebView,
    filePathCallback: ValueCallback<Array<Uri>>,
    fileChooserParams: FileChooserParams
  ): Boolean {
    val mimeTypes = fileChooserParams.acceptTypes.joinToString(",").ifEmpty { "*/*" }
    val captureEnabled = fileChooserParams.isCaptureEnabled
    debugFileChooser("onShowFileChooser", "mimeTypes=$mimeTypes, enable=$captureEnabled, multi=${fileChooserParams.mode}")
    if (captureEnabled && (mimeTypes.startsWith("video/") ||
          mimeTypes.startsWith("audio/") || mimeTypes.startsWith("image/"))
    ) {
      ioScope.launch {
        val mediaCaptureResult =
          Json.decodeFromString<MediaCaptureResult>(remoteMM.mediaCapture(mimeTypes))
        if (mediaCaptureResult.success) {
          filePathCallback.onReceiveValue(arrayOf(Uri.parse(mediaCaptureResult.data)))
        } else {
          filePathCallback.onReceiveValue(null)
        }
      }
      return true
    }

    ioScope.launch {
      try {
        if (fileChooserParams.mode == FileChooserParams.MODE_OPEN_MULTIPLE) {
          val list = remoteMM.openSystemFileChooser(mimeTypes, multiple = true, limit = 9)
          val uris = list.map { Uri.parse(it) }
          filePathCallback.onReceiveValue(uris.toTypedArray())
        } else {
          val list = remoteMM.openSystemFileChooser(mimeTypes)
          list.firstOrNull()?.let { uri ->
            filePathCallback.onReceiveValue(arrayOf(Uri.parse(uri)))
          } ?: filePathCallback.onReceiveValue(null)
        }
      } catch (e: Exception) {
        filePathCallback.onReceiveValue(null)
      }
    }
    return true
  }
}