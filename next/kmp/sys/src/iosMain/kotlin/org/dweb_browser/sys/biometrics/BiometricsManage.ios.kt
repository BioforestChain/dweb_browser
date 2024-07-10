package org.dweb_browser.sys.biometrics

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CompletableDeferred
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.module.MicroModule
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthentication

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


  actual suspend fun biometricsResultContent(
    mmRuntime: MicroModule.Runtime,
    remoteMMID: MMID,
    title: String?,
    subtitle: String?,
  ): BiometricsResult {
    val result = CompletableDeferred<BiometricsResult>()
    val safeTitle = title ?: BiometricsI18nResource.default_title.text
    val safeSubtitle = subtitle ?: BiometricsI18nResource.default_subtitle.text
    LAContext().evaluatePolicy(
      LAPolicyDeviceOwnerAuthentication, "$safeTitle\n $safeSubtitle\n $remoteMMID"
    ) { success, error ->
      var message = ""
      if (!success) {
        message = error?.localizedDescription ?: BiometricsI18nResource.authentication_failed.text
      }

      result.complete(BiometricsResult(success, message))
    }
    return result.await()
  }
}