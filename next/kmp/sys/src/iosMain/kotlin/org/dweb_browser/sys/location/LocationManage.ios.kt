package org.dweb_browser.sys.location

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.std.permission.AuthorizationStatus
import org.dweb_browser.helper.withMainContext
import org.dweb_browser.platform.ios.DwebLocationRequestApi
import org.dweb_browser.sys.permission.SystemPermissionAdapterManager
import org.dweb_browser.sys.permission.SystemPermissionName
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusDenied
import platform.Foundation.timeIntervalSince1970
import platform.darwin.NSObject


/**
 * TODO 参考详见：https://github.com/icerockdev/moko-geo/blob/master/geo/src/iosMain/kotlin/dev/icerock/moko/geo/LocationTracker.kt
 */
actual class LocationManage {

  private val manager = CLLocationManager()

  val result = CompletableDeferred<AuthorizationStatus>()

  @OptIn(ExperimentalForeignApi::class)
  private val api = DwebLocationRequestApi()

  init {
    SystemPermissionAdapterManager.append {
      if (task.name == SystemPermissionName.LOCATION) {
        locationAuthorizationStatus()
      } else null
    }
  }

  private suspend fun locationAuthorizationStatus(): AuthorizationStatus {
    val status = when (manager.authorizationStatus) {
      kCLAuthorizationStatusAuthorizedAlways, kCLAuthorizationStatusAuthorizedWhenInUse -> AuthorizationStatus.GRANTED

      kCLAuthorizationStatusDenied -> AuthorizationStatus.DENIED
      else -> {
        withMainContext {
          manager.delegate = delegate
          manager.requestWhenInUseAuthorization()
          return@withMainContext result.await()
        }
      }
    }
    return status
  }

  private val delegate: CLLocationManagerDelegateProtocol =
    object : NSObject(), CLLocationManagerDelegateProtocol {
      override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
        when (manager.authorizationStatus) {
          kCLAuthorizationStatusAuthorizedAlways, kCLAuthorizationStatusAuthorizedWhenInUse -> result.complete(
            AuthorizationStatus.GRANTED
          )

          kCLAuthorizationStatusDenied -> result.complete(AuthorizationStatus.DENIED)
          else -> result.complete(AuthorizationStatus.UNKNOWN)
        }
      }
    }

  @OptIn(ExperimentalForeignApi::class)
  actual suspend fun getCurrentLocation(mmid: MMID, precise: Boolean): LocationFlow {
    return callbackFlow {
      api.requestLocationWithCompleted { location, code, error ->
        val geo = toGeolocationPosition(location, code.toInt(), error)
        trySend(geo)
      }

      awaitClose {
        api.removeTrackWithMmid(mmid)
      }
    }
  }

  // TODO 这里没有看到任何对于异常情况自动解除监听的行为
  @OptIn(ExperimentalForeignApi::class)
  actual suspend fun observeLocation(
    mmid: MMID, fps: Long, precise: Boolean
  ): LocationFlow {
    return callbackFlow {
      api.requestTrack(mmid, fps) { location, code, error ->
        val geo = toGeolocationPosition(location, code.toInt(), error)
        trySend(geo)
      }

      awaitClose {
        api.removeTrackWithMmid(mmid)
      }
    }
  }

  @OptIn(ExperimentalForeignApi::class)
  actual fun removeLocationObserve(mmid: MMID) {
    api.removeTrackWithMmid(mmid)
  }

  /**
   * 将ios位置转换为需要结构
   */
  @OptIn(ExperimentalForeignApi::class)
  private fun toGeolocationPosition(
    location: CLLocation?, code: Int, msg: String?
  ): GeolocationPosition {

    val state = when (code) {
      0 -> GeolocationPositionState.Success
      1 -> GeolocationPositionState.PERMISSION_DENIED
      2 -> GeolocationPositionState.POSITION_UNAVAILABLE
      3 -> GeolocationPositionState.TIMEOUT
      else -> GeolocationPositionState.createOther(msg)
    }

    var cords = GeolocationCoordinates(0.0, 0.0, 0.0)
    var time: Long = 0

    location?.let {
      val accuracy = maxOf(location.horizontalAccuracy, location.verticalAccuracy)
      val latitude = it.coordinate.useContents {
        this.latitude
      }
      val longitude = it.coordinate.useContents {
        this.longitude
      }
      val altitude = it.altitude
      val heading = if (it.course < 0) null else it.course
      val speed = if (it.speed < 0) null else it.speed

      //坐标
      cords = GeolocationCoordinates(accuracy, latitude, longitude, altitude, null, heading, speed)

      //时间戳
      time = it.timestamp.timeIntervalSince1970().toLong()
    }

    return GeolocationPosition(state, cords, time)
  }
}