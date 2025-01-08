package org.dweb_browser.sys.clipboard.ext

import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.helper.encodeURIComponent


suspend fun MicroModule.Runtime.clipboardWriteText(text: String) =
  nativeFetch("file://clipboard.sys.dweb/write?string=${text.encodeURIComponent()}")

suspend fun MicroModule.Runtime.clipboardReadText() =
  nativeFetch("file://clipboard.sys.dweb/read").text()