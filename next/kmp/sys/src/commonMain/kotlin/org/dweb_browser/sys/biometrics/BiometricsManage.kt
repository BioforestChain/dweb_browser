package org.dweb_browser.sys.biometrics

import kotlinx.serialization.Serializable
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.helper.StringEnumSerializer

@Serializable
data class BiometricsResult(
  val success: Boolean,
  val message: String,
  val encoding: String = "UTF-8",
)

@Serializable
data class BiometricsData(
  val title: String? = "",
  val subtitle: String? = "",
  val description: String? = "",
  val useFallback: Boolean? = false,
  val negativeButtonText: String? = "",
)

enum class BiometricCheckResult(val value: Int) {
  /**用户无法进行身份验证，因为没有注册生物识别或设备凭据。 */
  BIOMETRIC_ERROR_NONE_ENROLLED(11),

  /**用户可以成功进行身份验证。 */
  BIOMETRIC_SUCCESS(0),

  /**无法确定用户是否可以进行身份验证。 */
  BIOMETRIC_STATUS_UNKNOWN(-1),

  /**用户无法进行身份验证，因为指定的选项与当前的 Android 版本不兼容。 */
  BIOMETRIC_ERROR_UNSUPPORTED(-2),

  /**由于硬件不可用，用户无法进行身份验证。 稍后再试。 */
  BIOMETRIC_ERROR_HW_UNAVAILABLE(1),

  /**用户无法进行身份验证，因为没有合适的硬件（例如没有生物识别传感器或没有键盘保护装置）。 */
  BIOMETRIC_ERROR_NO_HARDWARE(12),

  /**用户无法进行身份验证，因为发现一个或多个硬件传感器存在安全漏洞。 在安全更新解决该问题之前，受影响的传感器将不可用。 */
  BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED(15),
  ;

  companion object {
    val ALL_VALUES = entries.associateBy { it.value }

    fun fromValue(value: Int): BiometricCheckResult? = entries.firstOrNull { it.value == value }
  }
}

expect object BiometricsManage {

  suspend fun checkSupportBiometrics(): BiometricCheckResult

  suspend fun biometricsAuthInRuntime(
    mmRuntime: MicroModule.Runtime,
    title: String? = null,
    subtitle: String? = null,
    description: String? = null,
  ): BiometricsResult

  suspend fun biometricsAuthInGlobal(
    title: String? = null,
    subtitle: String? = null,
    description: String? = null,
  ): BiometricsResult
}


object InputMode_Serializer :
  StringEnumSerializer<InputMode>("DisplayMode", InputMode.ALL_VALUES, { mode })

@Serializable(with = InputMode_Serializer::class)
enum class InputMode(val mode: String) {
  None("none"),
  DECRYPT("decrypt"),
  ENCRYPT("encrypt"),
  ;

  companion object {
    val ALL_VALUES = entries.associateBy { it.mode }
  }
}