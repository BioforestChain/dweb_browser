package org.dweb_browser.dwebview.engine

import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.permission.AuthorizationStatus
import org.dweb_browser.dwebview.DwebViewI18nResource
import org.dweb_browser.dwebview.debugDWebView
import org.dweb_browser.helper.getAppContextUnsafe
import org.dweb_browser.helper.withMainContext
import org.dweb_browser.sys.permission.SystemPermissionName
import org.dweb_browser.sys.permission.SystemPermissionTask
import org.dweb_browser.sys.permission.ext.requestSystemPermissions

class DWebPermissionRequest(
  val remoteMM: MicroModule.Runtime, val lifecycleScope: CoroutineScope
) : WebChromeClient() {

  private suspend fun requestPermission(name: SystemPermissionName, title: String, desc: String) =
    remoteMM.requestSystemPermissions(
      SystemPermissionTask(name = name, title = title, description = desc)
    ).filterValues { value -> value != AuthorizationStatus.GRANTED }.isEmpty()

  override fun onPermissionRequest(request: PermissionRequest) {
    val context = getAppContextUnsafe()
    debugDWebView(
      "onPermissionRequest",
      "activity:$context request.resources:${request.resources.joinToString { it }}"
    )

    lifecycleScope.launch {
      val responsePermissionsMap = mutableMapOf<String, Boolean>()
      // 参考资料： https://developer.android.com/reference/android/webkit/PermissionRequest#constants.1
      for (res in request.resources) {
        when (res) {
          PermissionRequest.RESOURCE_VIDEO_CAPTURE -> {
            responsePermissionsMap[res] = requestPermission(
              name = SystemPermissionName.CAMERA,
              title = DwebViewI18nResource.permission_tip_camera_title.text,
              desc = DwebViewI18nResource.permission_tip_camera_message.text
            )
          }

          PermissionRequest.RESOURCE_AUDIO_CAPTURE -> {
            responsePermissionsMap[res] = requestPermission(
              name = SystemPermissionName.MICROPHONE,
              title = DwebViewI18nResource.permission_tip_microphone_title.text,
              desc = DwebViewI18nResource.permission_tip_microphone_message.text
            )
          }

          PermissionRequest.RESOURCE_MIDI_SYSEX -> {
            // TODO 这是一个需要系统级别签名的权限，一般的第三方应用可能无法获得
            // requestPermissionsMap[Manifest.permission.BIND_MIDI_DEVICE_SERVICE] = res
          }

          PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID -> {
            // TODO android.webkit.resource.PROTECTED_MEDIA_ID
          }
        }
      }

      withMainContext {
        if (responsePermissionsMap.isEmpty()) {
          request.grant(arrayOf())
        } else {
          val grants = responsePermissionsMap.filterValues { value -> value }
          if (grants.isEmpty()) {
            request.deny()
          } else {
            request.grant(grants.keys.toTypedArray())
          }
        }
      }
    }
  }
}