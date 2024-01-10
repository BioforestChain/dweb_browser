package org.dweb_browser.dwebview.engine

import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.permission.AuthorizationStatus
import org.dweb_browser.sys.camera.CameraResult
import org.dweb_browser.sys.camera.ext.captureSystemVideo
import org.dweb_browser.sys.camera.ext.takeSystemPicture
import org.dweb_browser.sys.filechooser.debugFileChooser
import org.dweb_browser.sys.filechooser.ext.openSystemFileChooser
import org.dweb_browser.sys.microphone.MicroPhoneResult
import org.dweb_browser.sys.microphone.ext.systemRecordSound
import org.dweb_browser.sys.permission.SystemPermissionName
import org.dweb_browser.sys.permission.SystemPermissionTask
import org.dweb_browser.sys.permission.ext.requestSystemPermissions

class DWebFileChooser(val remoteMM: MicroModule, val ioScope: CoroutineScope) : WebChromeClient() {

  private suspend fun requestPermission(name: SystemPermissionName, title: String, desc: String) =
    remoteMM.requestSystemPermissions(
      SystemPermissionTask(name = name, title = title, description = desc)
    ).filterValues { value -> value != AuthorizationStatus.GRANTED }.isEmpty()

  override fun onShowFileChooser(
    webView: WebView,
    filePathCallback: ValueCallback<Array<Uri>>,
    fileChooserParams: FileChooserParams
  ): Boolean {
    val mimeTypes = fileChooserParams.acceptTypes.joinToString(",").ifEmpty { "*/*" }
    val captureEnabled = fileChooserParams.isCaptureEnabled
    debugFileChooser("onShowFileChooser", "mimeTypes=$mimeTypes, enable=$captureEnabled")
    if (captureEnabled) {
      if (mimeTypes.startsWith("video/")) {
        ioScope.launch {
          val cameraResult = Json.decodeFromString<CameraResult>(remoteMM.captureSystemVideo())
          if (cameraResult.success) {
            filePathCallback.onReceiveValue(arrayOf(Uri.parse(cameraResult.data)))
          } else {
            filePathCallback.onReceiveValue(null)
          }
        }
        return true
      } else if (mimeTypes.startsWith("image/")) {
        ioScope.launch {
          val cameraResult = Json.decodeFromString<CameraResult>(remoteMM.takeSystemPicture())
          if (cameraResult.success) {
            filePathCallback.onReceiveValue(arrayOf(Uri.parse(cameraResult.data)))
          } else {
            filePathCallback.onReceiveValue(null)
          }
        }
        return true
      } else if (mimeTypes.startsWith("audio/")) {
        ioScope.launch {
          val recordSound = Json.decodeFromString<MicroPhoneResult>(remoteMM.systemRecordSound())
          if (recordSound.success) {
            filePathCallback.onReceiveValue(arrayOf(Uri.parse(recordSound.data)))
          } else {
            filePathCallback.onReceiveValue(null)
          }
        }
        return true
      }
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