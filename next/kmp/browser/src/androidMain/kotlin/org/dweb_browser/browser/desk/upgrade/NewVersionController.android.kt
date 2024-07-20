package org.dweb_browser.browser.desk.upgrade

import kotlinx.serialization.json.Json
import org.dweb_browser.browser.desk.LastVersionItem
import org.dweb_browser.browser.desk.NewVersionUrl
import org.dweb_browser.browser.desk.debugDesk
import org.dweb_browser.core.std.dns.httpFetch
import org.dweb_browser.pure.http.PureClientRequest
import org.dweb_browser.pure.http.PureMethod

actual suspend fun loadApplicationNewVersion(): NewVersionItem? {
  val loadNewVersion = try {
    val response = httpFetch.fetch(
      PureClientRequest(href = NewVersionUrl, method = PureMethod.GET)
    )
    Json.decodeFromString<LastVersionItem>(response.text())
  } catch (e: Exception) {
    debugDesk("NewVersion", "error => ${e.message}")
    null
  }
  return loadNewVersion?.createNewVersionItem()
}