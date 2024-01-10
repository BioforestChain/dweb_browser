package org.dweb_browser.dwebview.engine

import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.permission.AuthorizationStatus
import org.dweb_browser.dwebview.DwebViewI18nResource
import org.dweb_browser.sys.camera.ext.captureSystemVideo
import org.dweb_browser.sys.camera.ext.takeSystemPicture
import org.dweb_browser.sys.filechooser.debugFileChooser
import org.dweb_browser.sys.filechooser.ext.openSystemFileChooser
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
          if (!requestPermission(
              SystemPermissionName.CAMERA,
              DwebViewI18nResource.permission_tip_camera_title.text,
              DwebViewI18nResource.permission_tip_camera_message.text
            )
          ) {
            filePathCallback.onReceiveValue(null)
            return@launch
          }
          /*val tmpFile = File.createTempFile("temp_capture", ".mp4", context.cacheDir);
          val tmpUri = FileProvider.getUriForFile(
            context, "${context.packageName}.file.opener.provider", tmpFile
          )
          if (context.captureVideoLauncher.launch(tmpUri)) {
            filePathCallback.onReceiveValue(arrayOf(tmpUri))
          } else {
            filePathCallback.onReceiveValue(null)
          }*/
          val video = remoteMM.captureSystemVideo()
          if (video.isNotEmpty()) {
            filePathCallback.onReceiveValue(arrayOf(Uri.parse(video)))
          } else {
            filePathCallback.onReceiveValue(null)
          }
        }
        return true
      } else if (mimeTypes.startsWith("image/")) {
        ioScope.launch {
          if (!requestPermission(
              SystemPermissionName.CAMERA,
              DwebViewI18nResource.permission_tip_camera_title.text,
              DwebViewI18nResource.permission_tip_camera_message.text
            )
          ) {
            filePathCallback.onReceiveValue(null)
            return@launch
          }
          /*val tmpFile = File.createTempFile("temp_capture", ".jpg", context.cacheDir);
          val tmpUri = FileProvider.getUriForFile(
            context, "${context.packageName}.file.opener.provider", tmpFile
          )
          if (context.takePictureLauncher.launch(tmpUri)) {
            filePathCallback.onReceiveValue(arrayOf(tmpUri))
          } else {
            filePathCallback.onReceiveValue(null)
          }*/
          val picture = remoteMM.takeSystemPicture()
          if (picture.isNotEmpty()) {
            val uri = Uri.parse(picture)
            filePathCallback.onReceiveValue(arrayOf(uri))
          } else {
            filePathCallback.onReceiveValue(null)
          }
        }
        return true
      } else if (mimeTypes.startsWith("audio/")) {
        ioScope.launch {
          if (!requestPermission(
              SystemPermissionName.MICROPHONE,
              DwebViewI18nResource.permission_tip_microphone_title.text,
              DwebViewI18nResource.permission_tip_microphone_message.text
            )
          ) {
            filePathCallback.onReceiveValue(null)
            return@launch
          }
          /*val tmpFile = File.createTempFile("temp_capture", ".ogg", context.cacheDir);
          val tmpUri = FileProvider.getUriForFile(
            context, "${context.packageName}.file.opener.provider", tmpFile
          )

          if (context.recordSoundLauncher.launch(tmpUri)) {
            filePathCallback.onReceiveValue(arrayOf(tmpUri))
          } else {
            filePathCallback.onReceiveValue(null)
          }*/
          val audio = remoteMM.systemRecordSound()
          if (audio.isNotEmpty()) {
            filePathCallback.onReceiveValue(arrayOf(Uri.parse(audio)))
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
          /*val uris = context.getMultipleContentsLauncher.launch(mimeTypes)
          filePathCallback.onReceiveValue(uris.toTypedArray())*/
          val list = remoteMM.openSystemFileChooser(mimeTypes, multiple = true, limit = 9)
          val uris = list.map { Uri.parse(it) }
          filePathCallback.onReceiveValue(uris.toTypedArray())
        } else {
          /*val uri = context.getContentLauncher.launch(mimeTypes)
          if (uri != null) {
            filePathCallback.onReceiveValue(arrayOf(uri))
          } else {
            filePathCallback.onReceiveValue(null)
          }*/
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