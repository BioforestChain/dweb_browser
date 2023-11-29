package org.dweb_browser.sys.location

/**
 * TODO 参考详见：https://github.com/icerockdev/moko-geo/blob/master/geo/src/iosMain/kotlin/dev/icerock/moko/geo/LocationTracker.kt
 */
actual class LocationApi {
  actual suspend fun getCurrentLocation(): Location {
    TODO("Not yet implemented")
  }

  actual suspend fun observeLocation(callback: suspend (Location) -> Boolean) {
    TODO("Not yet implemented")
  }
}