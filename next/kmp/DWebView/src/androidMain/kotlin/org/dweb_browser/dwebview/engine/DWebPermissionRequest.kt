package org.dweb_browser.dwebview.engine

import android.Manifest
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.dweb_browser.dwebview.debugDWebView
import org.dweb_browser.helper.android.BaseActivity

class DWebPermissionRequest(val activity: BaseActivity?) : WebChromeClient() {
  override fun onPermissionRequest(request: PermissionRequest) {
    activity?.also { context ->
      debugDWebView(
        "onPermissionRequest",
        "activity:$context request.resources:${request.resources.joinToString { it }}"
      )
      context.lifecycleScope.launch {
        val requestPermissionsMap = mutableMapOf<String, String>();
        // 参考资料： https://developer.android.com/reference/android/webkit/PermissionRequest#constants.1
        for (res in request.resources) {
          when (res) {
            PermissionRequest.RESOURCE_VIDEO_CAPTURE -> {
              requestPermissionsMap[Manifest.permission.CAMERA] = res
            }

            PermissionRequest.RESOURCE_AUDIO_CAPTURE -> {
              requestPermissionsMap[Manifest.permission.RECORD_AUDIO] = res
            }

            PermissionRequest.RESOURCE_MIDI_SYSEX -> {
              requestPermissionsMap[Manifest.permission.BIND_MIDI_DEVICE_SERVICE] = res
            }

            PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID -> {
              // TODO android.webkit.resource.PROTECTED_MEDIA_ID
            }
          }
        }
        if (requestPermissionsMap.isEmpty()) {
          request.grant(arrayOf());
          return@launch
        }
        val responsePermissionsMap =
          context.requestMultiplePermissionsLauncher.launch(requestPermissionsMap.keys.toTypedArray());
        val grants = responsePermissionsMap.filterValues { value -> value };
        if (grants.isEmpty()) {
          request.deny()
        } else {
          request.grant(grants.keys.map { requestPermissionsMap[it] }.toTypedArray())
        }

      }
    } ?: request.deny()
  }
}