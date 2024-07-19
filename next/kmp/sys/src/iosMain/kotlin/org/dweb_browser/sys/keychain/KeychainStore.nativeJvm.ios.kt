package org.dweb_browser.sys.keychain

import org.dweb_browser.core.help.types.MMID

actual suspend fun tryThrowUserRejectAuth(
  runtime: KeychainNMM.KeyChainRuntime,
  remoteMmid: MMID,
  title: String,
  description: String,
) {
}