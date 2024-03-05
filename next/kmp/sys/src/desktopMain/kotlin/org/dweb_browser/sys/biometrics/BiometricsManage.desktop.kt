package org.dweb_browser.sys.biometrics

import org.dweb_browser.core.help.types.MMID


/**
 * TODO 接入 Windows/WinBio 接口
 *
 * TODO MacOS和IOS 共享基于 LocalAuthentication 的实现
 */
actual object BiometricsManage {
  actual suspend fun checkSupportBiometrics() = BiometricCheckResult.BIOMETRIC_STATUS_UNKNOWN

  actual suspend fun biometricsResultContent(
    biometricsNMM: BiometricsNMM,
    remoteMMID: MMID,
    title: String?,
    subtitle: String?,
    input: ByteArray?,
    mode: InputMode
  ): BiometricsResult {
    return BiometricsResult(false, "no implement yet")
  }
}