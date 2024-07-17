package org.dweb_browser.sys.keychain

import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.helper.withIoContext
import org.dweb_browser.sys.biometrics.BiometricsManage

actual suspend fun tryThrowUserRejectAuth(
  runtime: MicroModule.Runtime,
  remoteMmid: MMID,
  title: String,
  description: String,
) = withIoContext {
  val subtitle =
    runtime.bootstrapContext.dns.query(remoteMmid)?.name?.let { "$it($remoteMmid)" } ?: remoteMmid
  val result = BiometricsManage.biometricsAuthInGlobal(title, subtitle, description)
  println("QAQ tryThrowUserRejectAuth ${result.success} ${result.message}")
  if (!result.success) {
    throw Exception(result.message)
  }
}