package org.dweb_browser.sys.location

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.helper.SimpleSignal
import org.dweb_browser.helper.datetimeNow

@Serializable
data class GeolocationPositionState(val code: Int, val message: String?) {
  companion object {
    val Success = GeolocationPositionState(0, "Success")
    val PERMISSION_DENIED = GeolocationPositionState(1, "permission denied")
    val POSITION_UNAVAILABLE = GeolocationPositionState(2, "position unavailable")
    val TIMEOUT = GeolocationPositionState(3, "timeout")
    fun createOther(message: String?) = GeolocationPositionState(4, message)
  }
}

@Serializable
data class GeolocationCoordinates(
  val accuracy: Double,
  val latitude: Double,
  val longitude: Double,
  val altitude: Double? = null,
  val altitudeAccuracy: Double? = null,
  val heading: Double? = null,
  val speed: Double? = null,
)

@Serializable
data class GeolocationPosition(
  val state: GeolocationPositionState,
  val coords: GeolocationCoordinates,
  val timestamp: Long = datetimeNow(),
) {
  @Transient
  val closeSignal = SimpleSignal()

  @Transient
  val onClose = closeSignal.toListener()

  companion object {
    fun createErrorObj(state: GeolocationPositionState): GeolocationPosition {
      return GeolocationPosition(state, GeolocationCoordinates(0.0, 0.0, 0.0))
    }
  }
}

typealias LocationObserverCallback = suspend (GeolocationPosition) -> Unit

/**
 * 如果想优化，可以参考：https://github.com/icerockdev/moko-geo/blob/master/geo/src/commonMain/kotlin/dev/icerock/moko/geo/LocationTracker.kt
 */
expect class LocationManage() {
  /**
   * 获取当前的位置信息
   */
  suspend fun getCurrentLocation(precise: Boolean): GeolocationPosition?

  /**
   * 监听位置信息，位置信息变化及时通知
   * 返回的Boolean表示是否正常发送，如果发送遗产，关闭监听。
   */
  suspend fun observeLocation(
    mmid: MMID, fps: Long, precise: Boolean, callback: LocationObserverCallback
  )

  /**
   * 移除定位监听
   */
  fun removeLocationObserve(mmid: MMID)
}