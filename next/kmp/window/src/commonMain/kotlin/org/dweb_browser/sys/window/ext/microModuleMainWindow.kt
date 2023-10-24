package org.dweb_browser.sys.window.ext

import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.sys.window.core.windowInstancesManager

suspend fun MicroModule.openMainWindow() =
  windowInstancesManager.get(nativeFetch("file://window.sys.dweb/openMainWindow").text())
    ?.also { it.state.constants.microModule.value = this }
    ?: throw Exception("fail to got window for $mmid, not an application")

suspend fun MicroModule.getMainWindow() =
  windowInstancesManager.get(nativeFetch("file://window.sys.dweb/mainWindow").text())
    ?.also { it.state.constants.microModule.value = this }
    ?: throw Exception("fail to got window for $mmid, not an application")
