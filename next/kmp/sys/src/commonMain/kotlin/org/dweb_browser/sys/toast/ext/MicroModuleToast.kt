package org.dweb_browser.sys.toast.ext

import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.helper.buildUrlString
import org.dweb_browser.sys.toast.ToastDurationType
import org.dweb_browser.sys.toast.ToastPositionType

suspend fun MicroModule.Runtime.showToast(
  message: String, duration: ToastDurationType? = null, position: ToastPositionType? = null
) =
  nativeFetch(buildUrlString("file://toast.sys.dweb/show") {
    parameters["message"] = message
    duration?.also {
      parameters["duration"] = it.name
    }
    position?.also {
      parameters["position"] = it.position
    }
  })