package org.dweb_browser.sys.share.ext

import kotlinx.serialization.json.Json
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.helper.buildUrlString
import org.dweb_browser.pure.http.IPureBody
import org.dweb_browser.pure.http.PureClientRequest
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.sys.share.ShareResult

suspend fun NativeMicroModule.NativeRuntime.postSystemShare(
  title: String, text: String? = null, url: String? = null, body: IPureBody? = null
): ShareResult {
  val requestUrl = buildUrlString("file://share.sys.dweb/share") {
    parameters["title"] = title
    text?.let { parameters["text"] = text }
    url?.let { parameters["url"] = url }
  }
  val result =  nativeFetch(
    PureClientRequest(href = requestUrl, method = PureMethod.POST, body = body ?: IPureBody.Empty)
  )
  return Json.decodeFromString<ShareResult>(result.body.toPureString())
}