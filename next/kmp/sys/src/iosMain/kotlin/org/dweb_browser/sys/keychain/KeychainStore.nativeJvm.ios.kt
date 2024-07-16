package org.dweb_browser.sys.keychain

import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.module.MicroModule

actual suspend fun openAuthView(
  runtime: MicroModule.Runtime,
  remoteMmid: MMID,
  title: String,
  description: String,
) {
}