package org.dweb_browser.sys.window.ext

import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.sys.window.core.windowInstancesManager

suspend fun MicroModule.openMainWindow() =
  nativeFetch("file://window.sys.dweb/openMainWindow").text().let { wid ->
    windowInstancesManager.get(wid)
      ?.also { it.state.constants.microModule.value = this }
      ?: throw Exception("fail to open window for $mmid, not an application")
  }

suspend fun MicroModule.getMainWindow() =
  nativeFetch("file://window.sys.dweb/mainWindow").text().let { wid ->
    windowInstancesManager.get(wid)
      ?.also { it.state.constants.microModule.value = this }
      ?: throw Exception("fail to got window for $mmid, not an application")
  }
