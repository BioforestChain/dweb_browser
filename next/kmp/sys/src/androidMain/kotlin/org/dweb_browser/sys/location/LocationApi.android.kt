package org.dweb_browser.sys.location

import android.Manifest
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
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.module.getAppContext
import org.dweb_browser.helper.ChangeableList
import org.dweb_browser.helper.PromiseOut
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.sys.permission.AndroidPermissionTask
import org.dweb_browser.sys.permission.PermissionActivity
import org.dweb_browser.sys.permission.SystemPermissionAdapterManager
import org.dweb_browser.sys.permission.SystemPermissionName

@SuppressLint("MissingPermission")
actual class LocationManage : LocationCallback() {
  private val context = getAppContext()
  private var fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
  private val locationRequest = LocationRequest.create().also {
    it.interval = 10000 // 更新间隔10s
    it.fastestInterval = 5000L // 最快更新5s
    it.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
  }

  private val ioAsyncScope = MainScope() + ioAsyncExceptionHandler
  private val list: ChangeableList<PromiseOut<Boolean>> = ChangeableList()
  private var starting = atomic(false)
  private val locationChanged = Signal<GeolocationPosition>()
  private val onLocationChanged = locationChanged.toListener()

  init {
    SystemPermissionAdapterManager.append {
      if (task.name == SystemPermissionName.LOCATION) {
        PermissionActivity.launchAndroidSystemPermissionRequester(
          microModule,
          AndroidPermissionTask(
            permissions = listOf(
              Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION
            ),
            title = task.title,
            description = task.description
          ),
        ).values.firstOrNull()
      } else null
    }

    ioAsyncScope.launch {
      list.onChange {
        debugLocation("init", "onChange ${list.size}, ${starting.value}")
        if (list.isEmpty()) {
          starting.value = false
          fusedLocationClient.removeLocationUpdates(this@LocationManage)
        } else if (!starting.value) {
          starting.value = true
          fusedLocationClient.requestLocationUpdates(
            locationRequest, this@LocationManage, Looper.getMainLooper()
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

  @SuppressLint("MissingPermission")
  actual suspend fun getCurrentLocation(): GeolocationPosition {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    val promiseOut = PromiseOut<GeolocationPosition>()
    fusedLocationClient.lastLocation.addOnSuccessListener { lastLocation ->
      val location = processLocation(lastLocation)
      debugLocation("LocationResult", "addOnSuccessListener->$location")
      promiseOut.resolve(location)
    }
    return promiseOut.waitPromise()
  }

  actual suspend fun observeLocation(
    mmid: MMID,
    fps: Int,
    callback: suspend (GeolocationPosition) -> Boolean
  ) {
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

  actual fun removeLocationObserve(mmid: MMID) {
    TODO("Not yet implemented")
  }
}