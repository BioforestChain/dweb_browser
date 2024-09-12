package org.dweb_browser.core.http

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import org.dweb_browser.helper.hexBinary

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
internal val DWEB_SSL_PUBLIC_KEY =
  "3059301306072a8648ce3d020106082a8648ce3d030107034200041473c9977f4c369230c6a8d5dc4dfbf9a454b49378cf55079436ad754438712250e1a16f7e6c66939ced76b70530ae14f27acb84ae5f3b122dc6ee132adc7176".hexBinary
internal const val DWEB_SSL_ISSUER_NAME = "CN=rcgen self signed cert"
suspend fun DwebProxyService.waitReady() {
  if (proxyUrl.value == null) {
    start()
  }
  proxyUrl.filterNotNull().first()
}

val dwebProxyService = DwebProxyService()