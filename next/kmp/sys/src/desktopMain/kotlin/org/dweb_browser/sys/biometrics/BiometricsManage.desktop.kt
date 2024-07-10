package org.dweb_browser.sys.biometrics

import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.module.MicroModule


/**
 * TODO 接入 Windows/WinBio 接口
 *
 * TODO MacOS和IOS 共享基于 LocalAuthentication 的实现
 */
actual object BiometricsManage {
  actual suspend fun checkSupportBiometrics() =
    BiometricCheckResult.fromValue(biometrics.checkSupportBiometrics().toInt())
      ?: BiometricCheckResult.BIOMETRIC_STATUS_UNKNOWN

  actual suspend fun biometricsResultContent(
    mmRuntime: MicroModule.Runtime,
    remoteMMID: MMID,
    title: String?,
    subtitle: String?,
  ): BiometricsResult {
    val result = biometrics.biometricsResultContent(
      title ?: subtitle ?: BiometricsI18nResource.default_subtitle.text
    )

    if (!result.success && result.message.isBlank()) {
      return BiometricsResult(result.success, BiometricsI18nResource.authentication_failed.text)
    }

    return BiometricsResult(result.success, result.message)
  }
}