package org.dweb_browser.sys.keychain

import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.sys.biometrics.BiometricsManage

actual suspend fun openAuthView(
  runtime: MicroModule.Runtime,
  remoteMmid: MMID,
  title: String,
  description: String,
) {
  val subtitle =
    runtime.bootstrapContext.dns.query(remoteMmid)?.name?.let { "$it($remoteMmid)" } ?: remoteMmid
  BiometricsManage.biometricsAuthInGlobal(title, subtitle, description)
}