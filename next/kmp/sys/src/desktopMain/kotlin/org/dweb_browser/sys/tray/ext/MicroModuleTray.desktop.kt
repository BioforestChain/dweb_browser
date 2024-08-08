package org.dweb_browser.sys.tray.ext

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.helper.buildUrlString
import org.dweb_browser.sys.tray.TrayItem

actual suspend fun MicroModule.Runtime.registryTray(item: TrayItem): String {
  return nativeFetch(buildUrlString("file://tray.sys.dweb/registry") {
//    parameters["title"] = "Js Process"
//    parameters["url"] = "file://js.browser.dweb/open-devTool"
    item.id?.also { parameters["id"] = it }
    parameters["title"] = item.title
    parameters["type"] = item.type.name
    item.parent?.also { parameters["parent"] = it }
    item.url?.also { parameters["url"] = it }
    item.group?.also { parameters["group"] = it }
    item.enabled?.also { parameters["enabled"] = it.toString() }
    item.mnemonic?.also { parameters["mnemonic"] = it.toString() }
    item.icon?.also { parameters["icon"] = it }
    item.shortcut?.also { parameters["shortcut"] = Json.encodeToString(it) }
  }).text()
}