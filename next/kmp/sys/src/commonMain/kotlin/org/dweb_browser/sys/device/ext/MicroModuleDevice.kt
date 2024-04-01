package org.dweb_browser.sys.device.ext

import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.dns.nativeFetch

suspend fun MicroModule.Runtime.getDeviceAppVersion() =
  nativeFetch("file://device.sys.dweb/version").text()