package org.dweb_browser.sys.location

import android.annotation.SuppressLint
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.module.getAppContext
import org.dweb_browser.helper.ChangeableList
import org.dweb_browser.helper.PromiseOut
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.ioAsyncExceptionHandler

@SuppressLint("MissingPermission")
actual class LocationApi : LocationCallback() {
  private val context = NativeMicroModule.getAppContext()
  private var fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
  private val locationRequest = LocationRequest.create().also {
    it.interval = 10000 // 更新间隔10s
    it.fastestInterval = 5000L // 最快更新5s
    it.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
  }

  private val ioAsyncScope = MainScope() + ioAsyncExceptionHandler
  private val list: ChangeableList<PromiseOut<Boolean>> = ChangeableList()
  private var starting = atomic(false)
  private val locationChanged = Signal<Location>()
  private val onLocationChanged = locationChanged.toListener()

  init {
    ioAsyncScope.launch {
      list.onChange {
        debugLocation("init", "onChange ${list.size}, ${starting.value}")
        if (list.isEmpty()) {
          starting.value = false
          fusedLocationClient.removeLocationUpdates(this@LocationApi)
        } else if (!starting.value) {
          starting.value = true
          fusedLocationClient.requestLocationUpdates(
            locationRequest, this@LocationApi, Looper.getMainLooper()
          )
        }
      }
    }
  }

  override fun onLocationResult(locationResult: LocationResult) {
    debugLocation("LocationResult", "onLocationResult->${locationResult.lastLocation}")
    super.onLocationResult(locationResult)
    val lastLocation = locationResult.lastLocation ?: kotlin.run {
      return
    }
    ioAsyncScope.launch {
      val location = processLocation(lastLocation)
      locationChanged.emit(location)
    }
  }

  private fun processLocation(lastLocation: android.location.Location): Location {
    val latLng = LatLng(
      lastLocation.latitude,
      lastLocation.longitude
    )
    return Location(
      coordinates = latLng,
      coordinatesAccuracyMeters = lastLocation.accuracy.toDouble()
    )
  }

  @SuppressLint("MissingPermission")
  actual suspend fun getCurrentLocation(): Location {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    val promiseOut = PromiseOut<Location>()
    fusedLocationClient.lastLocation.addOnSuccessListener { lastLocation ->
      val location = processLocation(lastLocation)
      debugLocation("LocationResult", "addOnSuccessListener->$location")
      promiseOut.resolve(location)
    }
    return promiseOut.waitPromise()
  }

  actual suspend fun observeLocation(callback: suspend (Location) -> Boolean) {
    val promiseOut = PromiseOut<Boolean>()
    list.add(promiseOut)
    val off = onLocationChanged { location ->
      debugLocation("LocationResult", "observeLocation->$location")
      if (!callback(location)) promiseOut.resolve(false)
    }
    promiseOut.waitPromise()
    list.remove(promiseOut)
    off()
  }
}