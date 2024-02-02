package org.dweb_browser.sys.biometrics

import kotlinx.serialization.Serializable
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.helper.StringEnumSerializer

@Serializable
data class BiometricsResult(
  val success: Boolean,
  val message: String,
  val encoding: String = "UTF-8"
)

@Serializable
data class BiometricsData(
  val title: String? = "",
  val subtitle: String? = "",
  val description: String? = "",
  val useFallback: Boolean? = false,
  val negativeButtonText: String? = "",
)

expect object BiometricsManage {

  suspend fun checkSupportBiometrics(): Int

  suspend fun biometricsResultContent(
    biometricsNMM: BiometricsNMM,
    remoteMMID: MMID,
    title: String?,
    subtitle: String?,
    input: ByteArray? = null,
    mode: InputMode = InputMode.None
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