package org.dweb_browser.sys.device.model

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dweb_browser.helper.getAppContextUnsafe
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.helper.printError
import java.util.Locale

@Serializable
data class LocationData(
  var latitude: Double? = null,
  var longitude: Double? = null,
  var addressLine1: String? = null,
  var city: String? = null,
  var state: String? = null,
  var countryCode: String? = null,
  var postalCode: String? = null
)

class LocationInfo : LocationListener {
  val context = getAppContextUnsafe()

  @SuppressLint("MissingPermission") // 先忽略权限
  fun getLocationInfo(): String {
    return Json.encodeToString(locationData)
  }

  fun getLocationInfo(getLocationData: (String) -> Unit) {
    MainScope().launch(ioAsyncExceptionHandler) {
      getLocationData(getLocationInfo())
    }
  }

  private val locationData: LocationData
    get() {
      val latLong = locationLatLong
      return getAddressFromLocation(latLong[0]!!, latLong[1]!!)
    }

  private fun getAddressFromLocation(latitude: Double, longitude: Double): LocationData {
    val geocoder = Geocoder(context, Locale.getDefault())
    val locationData = LocationData()
    try {
      val addressList = geocoder.getFromLocation(latitude, longitude, 1)
      addressList?.first { address ->
        locationData.addressLine1 = address.getAddressLine(0)
        locationData.city = address.locality
        locationData.postalCode = address.postalCode
        locationData.state = address.adminArea
        locationData.countryCode = address.countryCode
        locationData.latitude = latitude
        locationData.longitude = longitude
        true
      }
    } catch (e: Throwable) {
      printError("", "Unable connect to Geocoder", e)
    }
    return locationData
  }

  val locationLatLong: Array<Double?>
    @SuppressLint("MissingPermission")
    get() {
      val latlong = arrayOfNulls<Double>(2)
      latlong[0] = 0.0
      latlong[1] = 0.0

      try {
        val manager = getAppContextUnsafe().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        manager.requestLocationUpdates(
          LocationManager.GPS_PROVIDER,
          MIN_TIME_BW_UPDATES,
          MIN_DISTANCE_CHANGE_FOR_UPDATES.toFloat(), this, Looper.getMainLooper()
        )

        val locationGPS = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        if (locationGPS != null) {
          latlong[0] = locationGPS.latitude
          latlong[1] = locationGPS.longitude
          manager.removeUpdates(this)
          return latlong
        }

        manager.requestLocationUpdates(
          LocationManager.NETWORK_PROVIDER,
          MIN_TIME_BW_UPDATES,
          MIN_DISTANCE_CHANGE_FOR_UPDATES.toFloat(), this, Looper.getMainLooper()
        )
        val locationNet = manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        if (locationNet != null) {
          latlong[0] = locationNet.latitude
          latlong[1] = locationNet.longitude
          manager.removeUpdates(this)
          return latlong
        }

        val locationPassive =
          manager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
        if (locationPassive != null) {
          latlong[0] = locationPassive.latitude
          latlong[1] = locationPassive.longitude
          manager.removeUpdates(this)
          return latlong
        }
      } catch (e: Throwable) {
        e.printStackTrace()
      }

      return latlong
    }

  companion object {
    private const val MIN_DISTANCE_CHANGE_FOR_UPDATES: Long = 0
    private const val MIN_TIME_BW_UPDATES: Long = 0
  }

  /** 当最后一次获取的位置与当前位置不匹配时，会触发该函数*/
  override fun onLocationChanged(location: Location) {
    locationLatLong[0] = location.latitude
    locationLatLong[1] = location.longitude
  }
}
