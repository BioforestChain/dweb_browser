package org.dweb_browser.sys.location

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.std.permission.AuthorizationStatus
import org.dweb_browser.helper.PromiseOut
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.helper.platform.ios.DwebLocationRequestApi
import org.dweb_browser.helper.withMainContext
import org.dweb_browser.sys.permission.SystemPermissionAdapterManager
import org.dweb_browser.sys.permission.SystemPermissionName
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusDenied
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.Foundation.timeIntervalSince1970
import platform.darwin.NSObject


/**
 * TODO 参考详见：https://github.com/icerockdev/moko-geo/blob/master/geo/src/iosMain/kotlin/dev/icerock/moko/geo/LocationTracker.kt
 */
actual class LocationManage {
  private val ioAsyncScope = MainScope() + ioAsyncExceptionHandler

  private val manager = CLLocationManager()

  val result = CompletableDeferred<AuthorizationStatus>()

  init {
    SystemPermissionAdapterManager.append {
      if (task.name == SystemPermissionName.LOCATION) {
        locationAuthorizationStatus()
      } else null
    }
  }

  private suspend fun locationAuthorizationStatus() : AuthorizationStatus {
    val status = when (manager.authorizationStatus) {
      kCLAuthorizationStatusAuthorizedAlways,
      kCLAuthorizationStatusAuthorizedWhenInUse -> AuthorizationStatus.GRANTED
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

  private val delegate: CLLocationManagerDelegateProtocol = object : NSObject(),
    CLLocationManagerDelegateProtocol {
    override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
      when (manager.authorizationStatus) {
        kCLAuthorizationStatusAuthorizedAlways,
        kCLAuthorizationStatusAuthorizedWhenInUse -> result.complete(AuthorizationStatus.GRANTED)
        kCLAuthorizationStatusDenied -> result.complete(AuthorizationStatus.DENIED)
        else -> result.complete(AuthorizationStatus.UNKNOWN)
      }
    }
  }

  @OptIn(ExperimentalForeignApi::class)
  actual suspend fun getCurrentLocation(): GeolocationPosition {
    val promiseOut = PromiseOut<GeolocationPosition>()
    DwebLocationRequestApi().requestLocationWithCompleted { location, code, error ->
      val geolocation = location as platform.CoreLocation.CLLocation?
      val geo = iOSLocationConvertToGeoGeolocationPosition(geolocation, code.toInt(), error)
      promiseOut.resolve(geo)
    }
    return promiseOut.waitPromise()
  }

  // TODO 这里没有看到任何对于异常情况自动解除监听的行为
  @OptIn(ExperimentalForeignApi::class)
  actual suspend fun observeLocation(
    mmid: MMID,
    fps: Int,
    callback: suspend (GeolocationPosition) -> Boolean
  ) {
    DwebLocationRequestApi().requestTrack(mmid, fps.toLong()) { location, code, error ->
      val geolocation = location as platform.CoreLocation.CLLocation?
      val geo = iOSLocationConvertToGeoGeolocationPosition(geolocation, code.toInt(), error)
      ioAsyncScope.launch {
        val continueTrack = callback(geo)
        if (!continueTrack) {
          DwebLocationRequestApi().removeTrackWithMmid(mmid)
        }
      }
    }
  }

  @OptIn(ExperimentalForeignApi::class)
  actual fun removeLocationObserve(mmid: MMID) {
    DwebLocationRequestApi().removeTrackWithMmid(mmid)
  }

  @OptIn(ExperimentalForeignApi::class)
  fun iOSLocationConvertToGeoGeolocationPosition(
    location: CLLocation?,
    code: Int,
    msg: String?
  ): GeolocationPosition {

    val state = when (code) {
      0 -> GeolocationPositionState.Success
      1 -> GeolocationPositionState.PERMISSION_DENIED
      2 -> GeolocationPositionState.POSITION_UNAVAILABLE
      3 -> GeolocationPositionState.TIMEOUT
      else -> GeolocationPositionState.createOther(msg)
    }

    var coords = GeolocationCoordinates(0.0, 0.0, 0.0)
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
      coords = GeolocationCoordinates(accuracy, latitude, longitude, altitude, null, heading, speed)

      //时间戳
      time = it.timestamp.timeIntervalSince1970().toLong()
    }

    return GeolocationPosition(state, coords, time)
  }
}