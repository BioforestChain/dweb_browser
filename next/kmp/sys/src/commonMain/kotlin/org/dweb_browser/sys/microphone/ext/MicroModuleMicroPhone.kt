package org.dweb_browser.sys.microphone.ext

import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.dns.nativeFetch

/**
 * 打开录音功能，并返回路径
 */
suspend fun MicroModule.systemRecordSound() =
  nativeFetch("file://microphone.sys.dweb/recordSound").body.toPureString()
