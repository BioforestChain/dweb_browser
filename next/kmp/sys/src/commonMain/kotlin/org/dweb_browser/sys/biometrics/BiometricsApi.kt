package org.dweb_browser.sys.biometrics

import kotlinx.serialization.Serializable

@Serializable
data class BiometricsResult(val success: Boolean, val message: String)

@Serializable
data class BiometricsData(
  val title: String?,
  val subtitle: String?,
  val description: String?,
  val useFallback: Boolean?,
  val negativeButtonText: String?,
)

expect object BiometricsApi {

  suspend fun isSupportBiometrics(
    biometricsData: BiometricsData, biometricsNMM: BiometricsNMM
  ): Boolean

  suspend fun biometricsResultContent(biometricsNMM: BiometricsNMM): BiometricsResult
}

