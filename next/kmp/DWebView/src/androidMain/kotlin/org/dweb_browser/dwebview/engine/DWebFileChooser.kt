package org.dweb_browser.dwebview.engine

import android.Manifest
import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.dweb_browser.helper.android.BaseActivity
import java.io.File

class DWebFileChooser(val activity: org.dweb_browser.helper.android.BaseActivity?) : WebChromeClient() {
  override fun onShowFileChooser(
    webView: WebView,
    filePathCallback: ValueCallback<Array<Uri>>,
    fileChooserParams: FileChooserParams
  ) = activity?.let { context ->
    val mimeTypes = fileChooserParams.acceptTypes.joinToString(",").ifEmpty { "*/*" }
    val captureEnabled = fileChooserParams.isCaptureEnabled
    if (captureEnabled) {
      if (mimeTypes.startsWith("video/")) {
        context.lifecycleScope.launch {
          if (!context.requestSelfPermission(Manifest.permission.CAMERA)) {
            filePathCallback.onReceiveValue(null)
            return@launch
          }
          val tmpFile = File.createTempFile("temp_capture", ".mp4", context.cacheDir);
          val tmpUri = FileProvider.getUriForFile(
            context, "${context.packageName}.file.opener.provider", tmpFile
          )

          if (context.captureVideoLauncher.launch(tmpUri)) {
            filePathCallback.onReceiveValue(arrayOf(tmpUri))
          } else {
            filePathCallback.onReceiveValue(null)
          }
        }
        return true;
      } else if (mimeTypes.startsWith("image/")) {
        context.lifecycleScope.launch {
          if (!context.requestSelfPermission(Manifest.permission.CAMERA)) {
            filePathCallback.onReceiveValue(null)
            return@launch
          }

          val tmpFile = File.createTempFile("temp_capture", ".jpg", context.cacheDir);
          val tmpUri = FileProvider.getUriForFile(
            context, "${context.packageName}.file.opener.provider", tmpFile
          )

          if (context.takePictureLauncher.launch(tmpUri)) {
            filePathCallback.onReceiveValue(arrayOf(tmpUri))
          } else {
            filePathCallback.onReceiveValue(null)
          }
        }
        return true;
      } else if (mimeTypes.startsWith("audio/")) {
        context.lifecycleScope.launch {
          if (!context.requestSelfPermission(Manifest.permission.RECORD_AUDIO)) {
            filePathCallback.onReceiveValue(null)
            return@launch
          }

          val tmpFile = File.createTempFile("temp_capture", ".ogg", context.cacheDir);
          val tmpUri = FileProvider.getUriForFile(
            context, "${context.packageName}.file.opener.provider", tmpFile
          )

          if (context.recordSoundLauncher.launch(tmpUri)) {
            filePathCallback.onReceiveValue(arrayOf(tmpUri))
          } else {
            filePathCallback.onReceiveValue(null)
          }
        }
        return true;
      }
    }

    context.lifecycleScope.launch {
      try {
        if (fileChooserParams.mode == FileChooserParams.MODE_OPEN_MULTIPLE) {
          val uris = context.getMultipleContentsLauncher.launch(mimeTypes)
          filePathCallback.onReceiveValue(uris.toTypedArray())
        } else {
          val uri = context.getContentLauncher.launch(mimeTypes)
          if (uri != null) {
            filePathCallback.onReceiveValue(arrayOf(uri))
          } else {
            filePathCallback.onReceiveValue(null)
          }
        }
      } catch (e: Exception) {
        filePathCallback.onReceiveValue(null)
      }
    }
    return true
  } ?: super.onShowFileChooser(webView, filePathCallback, fileChooserParams)


}