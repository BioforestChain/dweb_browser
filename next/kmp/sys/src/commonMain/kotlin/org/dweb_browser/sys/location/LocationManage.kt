package org.dweb_browser.sys.location

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.helper.SimpleSignal
import org.dweb_browser.helper.datetimeNow

@Serializable
data class GeolocationPositionState(val code: Int, val message: String?) {
  companion object {
    val Success = GeolocationPositionState(0, "success")
    /// https://developer.mozilla.org/en-US/docs/Web/API/GeolocationPositionError

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

abstract class LocationObserver() {
  abstract val mm: MicroModule.Runtime
  abstract val flow: Flow<GeolocationPosition>

  /**
   * @param precise 是否使用高精度定位（需要传感器）
   * @param minTimeMs 位置更新之间的最小时间间隔（以毫秒为单位）
   * @param minDistance 位置更新之间的最小距离（以米为单位）
   */
  abstract suspend fun start(
    precise: Boolean = true,
    minTimeMs: Long = 0,
    minDistance: Double = 0.0,
  )

  abstract suspend fun stop()

  private val destroySignal = SimpleSignal()
  val onDestroy = destroySignal.toListener()
  open suspend fun destroy() {
    destroySignal.emitAndClear()
    stop()
  }
}

expect class LocationManage(mm: MicroModule.Runtime) {
  val mm: MicroModule.Runtime

  /**
   * 获取当前的位置信息
   */
  suspend fun getCurrentLocation(precise: Boolean): GeolocationPosition

  /**
   * 创建一个监听器
   * 监听位置信息，位置信息变化及时通知
   * 返回的Boolean表示是否正常发送，如果发送遗产，关闭监听。
   */
  suspend fun createLocationObserver(autoStart: Boolean = true): LocationObserver
}