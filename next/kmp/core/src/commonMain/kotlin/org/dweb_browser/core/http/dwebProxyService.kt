package org.dweb_browser.core.http

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first

expect class DwebProxyService() {
  suspend fun start()
  suspend fun stop()
  val proxyUrl: StateFlow<String?>
}

suspend fun DwebProxyService.waitReady() {
  if (proxyUrl.value == null) {
    start()
  }
  proxyUrl.filterNotNull().first()
}

val dwebProxyService = DwebProxyService()