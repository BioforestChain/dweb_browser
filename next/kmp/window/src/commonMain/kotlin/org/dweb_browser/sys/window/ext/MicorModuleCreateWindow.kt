package org.dweb_browser.sys.window.ext

import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.sys.window.core.windowInstancesManager

suspend fun MicroModule.openMainWindow() =
  windowInstancesManager.get(nativeFetch("file://window.sys.dweb/openMainWindow").text())
    ?.also { it.state.constants.microModule.value = this }
