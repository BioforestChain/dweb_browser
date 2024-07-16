package org.dweb_browser.sys.biometrics

import kotlinx.cinterop.ExperimentalForeignApi
import org.dweb_browser.core.module.MicroModule
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthentication
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

actual object BiometricsManage {

  @OptIn(ExperimentalForeignApi::class)
  actual suspend fun checkSupportBiometrics() =
    when {
      LAContext().canEvaluatePolicy(
        LAPolicyDeviceOwnerAuthentication,
        null
      ) -> BiometricCheckResult.BIOMETRIC_SUCCESS

      else -> BiometricCheckResult.BIOMETRIC_STATUS_UNKNOWN
    }


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
    return startLocalAuthentication(reason)
  }

  suspend fun startLocalAuthentication(reason: String) = suspendCoroutine { continuation ->
    LAContext().evaluatePolicy(
      LAPolicyDeviceOwnerAuthentication, reason
    ) { success, error ->
      var message = ""
      if (!success) {
        message = error?.localizedDescription ?: BiometricsI18nResource.authentication_failed.text
      }

      continuation.resume(BiometricsResult(success, message))
    }
  }
}