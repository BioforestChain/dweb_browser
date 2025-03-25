package org.dweb_browser.sys.biometrics

import org.dweb_browser.core.module.MicroModule


/**
 * TODO 接入 Windows/WinBio 接口
 *
 * TODO MacOS和IOS 共享基于 LocalAuthentication 的实现
 */
actual object BiometricsManage {
  actual suspend fun checkSupportBiometrics() =
    BiometricCheckResult.fromValue(org.dweb_browser.biometrics.checkSupportBiometrics().toInt())
      ?: BiometricCheckResult.BIOMETRIC_STATUS_UNKNOWN

  actual suspend fun biometricsAuthInRuntime(
    mmRuntime: MicroModule.Runtime,
    title: String?,
    subtitle: String?,
    description: String?,
  ): BiometricsResult {
    return biometricsAuthInGlobal(title, subtitle, description)
  }

  actual suspend fun biometricsAuthInGlobal(
    title: String?,
    subtitle: String?,
    description: String?,
  ): BiometricsResult {
    val safeTitle = title ?: BiometricsI18nResource.default_title.text
    val safeSubtitle = subtitle ?: BiometricsI18nResource.default_subtitle.text
    val reason = listOfNotNull(safeTitle, safeSubtitle, description).joinToString("\n")
    val result = org.dweb_browser.biometrics.biometricsResultContent(reason)

    return BiometricsResult(
      result.success, when {
        result.message.isBlank() -> when {
          result.success -> BiometricsI18nResource.authentication_success.text
          else -> BiometricsI18nResource.authentication_failed.text
        }

        else -> result.message
      }
    )
  }
}