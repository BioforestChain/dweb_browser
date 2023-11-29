package org.dweb_browser.sys.location

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.dweb_browser.helper.SimpleSignal

@Serializable
data class LatLng(
  val latitude: Double,
  val longitude: Double
)

@Serializable
data class Location(
  val coordinates: LatLng,
  val coordinatesAccuracyMeters: Double
) {
  @Transient
  val closeSignal = SimpleSignal()

  @Transient
  val onClose = closeSignal.toListener()
}

/**
 * 如果想优化，可以参考：https://github.com/icerockdev/moko-geo/blob/master/geo/src/commonMain/kotlin/dev/icerock/moko/geo/LocationTracker.kt
 */
expect class LocationApi() {
  /**
   * 获取当前的位置信息
   */
  suspend fun getCurrentLocation(): Location

  /**
   * 监听位置信息，位置信息变化及时通知
   * 返回的Boolean表示是否正常发送，如果发送遗产，关闭监听。
   */
  suspend fun observeLocation(callback: suspend (Location) -> Boolean)
}