package org.dweb_browser.sys.tray.ext

import io.ktor.http.URLBuilder
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.helper.buildUnsafeString
import org.dweb_browser.sys.tray.TrayNMM

suspend fun MicroModule.Runtime.registryTray(
  id: String? = null,
  title: String,
  url: String,
  group: String? = null,
  enabled: Boolean? = null,
  mnemonic: Char? = null,
  icon: String? = null,
  shortcut: TrayNMM.TrayAction.KeyShortcut? = null,
): String {
  return nativeFetch(URLBuilder("file://tray.sys.dweb/registry").apply {
    parameters["title"] = "Js Process"
    parameters["url"] = "file://js.browser.dweb/open-devTool"
    id?.also { parameters["id"] = id }
    parameters["title"] = title
    parameters["url"] = url
    group?.also { parameters["group"] = group }
    enabled?.also { parameters["enabled"] = enabled.toString() }
    mnemonic?.also { parameters["mnemonic"] = mnemonic.toString() }
    icon?.also { parameters["icon"] = icon }
    shortcut?.also { parameters["shortcut"] = Json.encodeToString(shortcut) }
  }.buildUnsafeString()).text()
}