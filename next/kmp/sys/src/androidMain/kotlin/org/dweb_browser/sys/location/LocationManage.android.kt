package org.dweb_browser.sys.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.module.getAppContext
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.sys.permission.AndroidPermissionTask
import org.dweb_browser.sys.permission.PermissionActivity
import org.dweb_browser.sys.permission.SystemPermissionAdapterManager
import org.dweb_browser.sys.permission.SystemPermissionName

@SuppressLint("MissingPermission")
actual class LocationManage {
  private val context = getAppContext()
  private val ioScope = MainScope() + ioAsyncExceptionHandler

  init {
    SystemPermissionAdapterManager.append {
      if (task.name == SystemPermissionName.LOCATION) {
        PermissionActivity.launchAndroidSystemPermissionRequester(
          microModule,
          AndroidPermissionTask(
            permissions = listOf(
              Manifest.permission.ACCESS_COARSE_LOCATION,
              Manifest.permission.ACCESS_FINE_LOCATION,
            ),
            title = task.title,
            description = task.description
          ),
        ).values.firstOrNull()
      } else null
    }
  }

  private fun processLocation(lastLocation: android.location.Location): GeolocationPosition {
    val geolocationCoordinates = GeolocationCoordinates(
      accuracy = lastLocation.accuracy.toDouble(),
      latitude = lastLocation.latitude,
      longitude = lastLocation.longitude
    )
    return GeolocationPosition(
      state = GeolocationPositionState.Success,
      coords = geolocationCoordinates,
    )
  }

  private fun askLocationSettings() {
    context.startActivity(Intent().apply {
      action = android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    })
  }

  actual suspend fun getCurrentLocation(precise: Boolean): GeolocationPosition? {
    val manager = getAppContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
    if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
      !manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    ) {
      askLocationSettings()
      return null
    }

    val location = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
      ?: manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
    return location?.let { processLocation(location) }
  }

  private val locationListenerMaps = mutableMapOf<MMID, LocationObserverCallback>()

  private val locationListener = LocationListener { location ->
    ioScope.launch {
      val lastLocation = processLocation(location)
      locationListenerMaps.forEach { (_, callback) -> callback(lastLocation) }
    }
  }

  /**
   *
   */
  actual suspend fun observeLocation(
    mmid: MMID, fps: Long, precise: Boolean, callback: LocationObserverCallback
  ) {
    val manager = getAppContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
    if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
      !manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    ) {
      askLocationSettings()
      return
    }
    if (locationListenerMaps.isEmpty()) {
      manager.requestLocationUpdates(
        LocationManager.GPS_PROVIDER, 0L, 0f, locationListener, Looper.getMainLooper()
      )
      manager.requestLocationUpdates(
        LocationManager.NETWORK_PROVIDER, 0L, 0f, locationListener, Looper.getMainLooper()
      )
    }
    locationListenerMaps[mmid] = callback
  }

  actual fun removeLocationObserve(mmid: MMID) {
    locationListenerMaps.remove(mmid)
    if (locationListenerMaps.isEmpty()) {
      val manager = getAppContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
      manager.removeUpdates(locationListener)
    }
  }
}