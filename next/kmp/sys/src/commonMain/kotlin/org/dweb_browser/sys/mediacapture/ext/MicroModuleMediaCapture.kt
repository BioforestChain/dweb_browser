package org.dweb_browser.sys.mediacapture.ext

import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.helper.buildUrlString

suspend fun MicroModule.Runtime.mediaCapture(mime: String) = nativeFetch(
  buildUrlString("file://media-capture.sys.dweb/capture") { parameters["mime"] = mime }
).body.toPureString()