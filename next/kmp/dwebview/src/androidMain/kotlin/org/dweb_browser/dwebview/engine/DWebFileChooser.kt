package org.dweb_browser.dwebview.engine

import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.dwebview.DwebViewI18nResource
import org.dweb_browser.helper.android.BaseActivity
import org.dweb_browser.helper.withMainContext
import org.dweb_browser.sys.filechooser.debugFileChooser
import org.dweb_browser.sys.permission.SystemPermissionName
import org.dweb_browser.sys.permission.SystemPermissionTask
import org.dweb_browser.sys.permission.ext.requestSystemPermission
import java.io.File

class DWebFileChooser(
  private val remoteMM: MicroModule.Runtime,
  private val lifecycleScope: CoroutineScope,
  private val activity: BaseActivity?
) : WebChromeClient() {
  override fun onShowFileChooser(
    webView: WebView,
    filePathCallback: ValueCallback<Array<Uri>>,
    fileChooserParams: FileChooserParams
  ): Boolean = activity?.let { context ->
    val mimeTypes = fileChooserParams.acceptTypes.joinToString(",").ifEmpty { "*/*" }
    val captureEnabled = fileChooserParams.isCaptureEnabled
    debugFileChooser(
      tag = "onShowFileChooser",
      msg = "mimeTypes=$mimeTypes, enable=$captureEnabled, multi=${fileChooserParams.mode}"
    )
    if (captureEnabled) {
      if (mimeTypes.startsWith("video/")) {
        lifecycleScope.launch {
          val uriList = captureVideo()
          withMainContext {
            filePathCallback.onReceiveValue(uriList)
          }
        }
        return true
      } else if (mimeTypes.startsWith("image/")) {
        // filePathCallback.onReceiveValue 需要在主线程调用，否则会卡住，导致应用crash
        lifecycleScope.launch {
          val uriList = takePicture()
          withMainContext {
            filePathCallback.onReceiveValue(uriList)
          }
        }
        return true
      } else if (mimeTypes.startsWith("audio/")) {
        lifecycleScope.launch {
          val uriList = recordAudio()
          withMainContext {
            filePathCallback.onReceiveValue(uriList)
          }
        }
        return true
      }
    }

    lifecycleScope.launch {
      try {
        if (fileChooserParams.mode == FileChooserParams.MODE_OPEN_MULTIPLE) {
          val uris = context.getMultipleContentsLauncher.launch(mimeTypes)
          withMainContext {
            filePathCallback.onReceiveValue(uris.toTypedArray())
          }
        } else {
          context.getContentLauncher.launch(mimeTypes)?.let { uri ->
            withMainContext {
              filePathCallback.onReceiveValue(arrayOf(uri))
            }
          } ?: withMainContext { filePathCallback.onReceiveValue(null) }
        }
      } catch (e: Exception) {
        withMainContext { filePathCallback.onReceiveValue(null) }
      }
    }
    return true
  } ?: super.onShowFileChooser(webView, filePathCallback, fileChooserParams)

  private suspend fun captureVideo(): Array<Uri>? = activity?.let { context ->
    if (!remoteMM.requestSystemPermission(
        SystemPermissionTask(
          SystemPermissionName.CAMERA,
          DwebViewI18nResource.permission_tip_camera_title.text,
          DwebViewI18nResource.permission_tip_camera_message.text
        )
      )
    ) return null
    val tmpFile = File.createTempFile("temp_capture", ".mp4", context.cacheDir);
    val tmpUri = FileProvider.getUriForFile(
      context, "${context.packageName}.file.opener.provider", tmpFile
    )
    if (context.captureVideoLauncher.launch(tmpUri)) arrayOf(tmpUri) else null
  }

  private suspend fun takePicture(): Array<Uri>? = activity?.let { context ->
    if (!remoteMM.requestSystemPermission(
        SystemPermissionTask(
          SystemPermissionName.CAMERA,
          DwebViewI18nResource.permission_tip_camera_title.text,
          DwebViewI18nResource.permission_tip_camera_message.text
        )
      )
    ) return null
    val tmpFile = File.createTempFile("temp_capture", ".jpg", context.cacheDir);
    val tmpUri = FileProvider.getUriForFile(
      context, "${context.packageName}.file.opener.provider", tmpFile
    )
    if (context.takePictureLauncher.launch(tmpUri)) arrayOf(tmpUri) else null
  }

  private suspend fun recordAudio(): Array<Uri>? = activity?.let { context ->
    if (!remoteMM.requestSystemPermission(
        SystemPermissionTask(
          SystemPermissionName.MICROPHONE,
          DwebViewI18nResource.permission_tip_microphone_title.text,
          DwebViewI18nResource.permission_tip_microphone_message.text
        )
      )
    ) return null
    val tmpFile = File.createTempFile("temp_capture", ".ogg", context.cacheDir);
    val tmpUri = FileProvider.getUriForFile(
      context, "${context.packageName}.file.opener.provider", tmpFile
    )
    if (context.recordSoundLauncher.launch(tmpUri)) arrayOf(tmpUri) else null
  }
}