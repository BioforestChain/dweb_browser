package org.dweb_browser.sys.filechooser.ext

import kotlinx.serialization.json.Json
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.helper.buildUrlString
import org.dweb_browser.pure.http.PureClientRequest
import org.dweb_browser.pure.http.PureMethod

/**
 * 打开系统文件选择器,limit取值范围 1~16
 */
suspend fun MicroModule.Runtime.openSystemFileChooser(
  mimeType: String, multiple: Boolean = false, limit: Int = 1
): List<String> {
  val requestUrl = buildUrlString("file://fs-picker.sys.dweb/open-file") {
    parameters["accept"] = mimeType
    parameters["multiple"] = "$multiple"
    parameters["limit"] = "${limit.coerceIn(1, 16)}"
  }
  val result = nativeFetch(
    PureClientRequest(href = requestUrl, method = PureMethod.GET)
  )
  return Json.decodeFromString<List<String>>(result.body.toPureString())
}