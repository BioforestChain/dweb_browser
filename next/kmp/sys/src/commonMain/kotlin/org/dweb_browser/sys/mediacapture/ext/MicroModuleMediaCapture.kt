package org.dweb_browser.sys.mediacapture.ext

import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.dns.nativeFetch

suspend fun MicroModule.Runtime.mediaCapture(mime: String)  =
  nativeFetch("file://media-capture.sys.dweb/capture?mime=$mime").body.toPureString()