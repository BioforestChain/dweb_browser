package info.bagen.rust.plaoc.system.device.model

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import android.util.Log
import info.bagen.libappmgr.utils.JsonUtil
import info.bagen.rust.plaoc.App
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*

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
    @SuppressLint("MissingPermission") // 先忽略权限
    fun getLocationInfo(): String {
        return JsonUtil.toJson(locationData)
    }

    fun getLocationInfo(GetLocationData: (String) -> Unit) {
        GlobalScope.launch {
            GetLocationData(JsonUtil.toJson(locationData))
        }
    }

    val locationData: LocationData
        get() {
            val latLong = locationLatLong
            return getAddressFromLocation(latLong[0]!!, latLong[1]!!)
        }

    private fun getAddressFromLocation(latitude: Double, longitude: Double): LocationData {

        val geocoder = Geocoder(App.appContext, Locale.getDefault())
        val locationData = LocationData()
        try {
            val addressList = geocoder.getFromLocation(latitude, longitude, 1)
            addressList?.forEach { address ->
                locationData.addressLine1 = address.getAddressLine(0)
                locationData.city = address.locality
                locationData.postalCode = address.postalCode
                locationData.state = address.adminArea
                locationData.countryCode = address.countryCode
                locationData.latitude = latitude
                locationData.longitude = longitude
                return locationData
            }
        } catch (e: Exception) {
            Log.e("", "Unable connect to Geocoder", e)
        } finally {
            return locationData
        }
    }

    val locationLatLong: Array<Double?>
        @SuppressLint("MissingPermission")
        get() {
            val latlong = arrayOfNulls<Double>(2)
            latlong[0] = 0.0
            latlong[1] = 0.0

            try {
                val locationM =
                    App.appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                locationM.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    MIN_TIME_BW_UPDATES,
                    MIN_DISTANCE_CHANGE_FOR_UPDATES.toFloat(), this, Looper.getMainLooper()
                )

                val locationGPS = locationM.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (locationGPS != null) {
                    latlong[0] = locationGPS.latitude
                    latlong[1] = locationGPS.longitude
                    locationM.removeUpdates(this)
                    return latlong
                }

                locationM.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    MIN_TIME_BW_UPDATES,
                    MIN_DISTANCE_CHANGE_FOR_UPDATES.toFloat(), this, Looper.getMainLooper()
                )
                val locationNet = locationM.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                if (locationNet != null) {
                    latlong[0] = locationNet.latitude
                    latlong[1] = locationNet.longitude
                    locationM.removeUpdates(this)
                    return latlong
                }

                val locationPassive =
                    locationM.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
                if (locationPassive != null) {
                    latlong[0] = locationPassive.latitude
                    latlong[1] = locationPassive.longitude
                    locationM.removeUpdates(this)
                    return latlong
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return latlong
        }

    companion object {
        private const val MIN_DISTANCE_CHANGE_FOR_UPDATES: Long = 0
        private const val MIN_TIME_BW_UPDATES: Long = 0
    }

    override fun onLocationChanged(location: Location) {
        TODO("Not yet implemented")
    }
}
