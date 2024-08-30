package org.dweb_browser.core.http

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first

expect class DwebProxyService() {
  suspend fun start()
  suspend fun stop()
  val proxyUrl: StateFlow<String?>
}

internal const val DWEB_SSL_PEM = """-----BEGIN PRIVATE KEY-----
MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgLsUuVx2ebL0CUMSU
OvcyAnML50/UE7p3UA6wJ3INLEahRANCAAQUc8mXf0w2kjDGqNXcTfv5pFS0k3jP
VQeUNq11RDhxIlDhoW9+bGaTnO12twUwrhTyesuErl87Ei3G7hMq3HF2
-----END PRIVATE KEY-----
"""

suspend fun DwebProxyService.waitReady() {
  if (proxyUrl.value == null) {
    start()
  }
  proxyUrl.filterNotNull().first()
}

val dwebProxyService = DwebProxyService()