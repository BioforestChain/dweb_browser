package org.dweb_browser.sys.location

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.helper.PromiseOut
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.helper.platform.ios.DwebLocationRequestApi
import platform.CoreLocation.CLLocation
import platform.Foundation.timeIntervalSince1970


/**
 * TODO 参考详见：https://github.com/icerockdev/moko-geo/blob/master/geo/src/iosMain/kotlin/dev/icerock/moko/geo/LocationTracker.kt
 */
actual class LocationApi {
  private val ioAsyncScope = MainScope() + ioAsyncExceptionHandler

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

  @OptIn(ExperimentalForeignApi::class)
  actual suspend fun observeLocation(mmid: MMID, fps: Int, callback: suspend (GeolocationPosition) -> Boolean) {
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
  fun iOSLocationConvertToGeoGeolocationPosition(location: CLLocation?, code: Int, msg: String?): GeolocationPosition {

    val state = when (code) {
      0-> GeolocationPositionState.Success
      1-> GeolocationPositionState.PERMISSION_DENIED
      2-> GeolocationPositionState.POSITION_UNAVAILABLE
      3-> GeolocationPositionState.TIMEOUT
      else -> GeolocationPositionState.createOther(msg)
    }

    var coords = GeolocationCoordinates(0.0, 0.0, 0.0)
    var time: Long = 0

    location?.let {
      val accuracy =  maxOf(location.horizontalAccuracy, location.verticalAccuracy)
      val latitude = location!!.coordinate.useContents {
        this.latitude
      }
      val longitude = location!!.coordinate.useContents {
        this.longitude
      }
      val altitude = location!!.altitude
      val heading = if (location!!.course < 0) null else location!!.course
      val speed = if (location!!.speed < 0) null else location!!.speed

      //坐标
      coords = GeolocationCoordinates(accuracy, latitude, longitude, altitude, null, heading, speed)

      //时间戳
      time = location!!.timestamp.timeIntervalSince1970().toLong()
    }

    return  GeolocationPosition(state, coords, time)
  }
}