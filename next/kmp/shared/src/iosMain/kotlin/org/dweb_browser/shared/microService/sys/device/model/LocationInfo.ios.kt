package org.dweb_browser.shared.microService.sys.device.model

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dweb_browser.helper.ioAsyncExceptionHandler
import platform.CoreLocation.CLAuthorizationStatus
import platform.CoreLocation.CLGeocoder
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationCoordinate2D
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.CLPlacemark
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusDenied
import platform.CoreLocation.kCLAuthorizationStatusRestricted
import platform.CoreLocation.kCLDistanceFilterNone
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.Foundation.NSError
import platform.darwin.NSObject
import kotlin.coroutines.resumeWithException

actual class LocationInfo actual constructor()  {

    private val manager = CLLocationManager()

    init {
        manager.requestWhenInUseAuthorization()
        manager.desiredAccuracy = kCLLocationAccuracyBest
        manager.distanceFilter = kCLDistanceFilterNone
    }

    actual suspend fun getLocationInfo(): String = suspendCancellableCoroutine { continuation ->

        val delegate: CLLocationManagerDelegateProtocol = object : NSObject(), CLLocationManagerDelegateProtocol {

            override fun locationManager(
                manager: CLLocationManager,
                didChangeAuthorizationStatus: CLAuthorizationStatus
            ) {
                when (didChangeAuthorizationStatus) {
                    kCLAuthorizationStatusAuthorizedWhenInUse,
                    kCLAuthorizationStatusAuthorizedAlways -> {
                        manager.requestLocation()
                    }
                    kCLAuthorizationStatusDenied -> {
                        continuation.resumeWithException(Throwable(message = "User has rejected permission to access location"))
                    }
                    kCLAuthorizationStatusRestricted -> {
                        continuation.resumeWithException(Throwable(message = "User cannot give permission to access location due to parental restriction"))
                    }
                    else -> {
                        continuation.resumeWithException(Throwable(message = "User has not given permission to access location"))
                    }
                }
            }

            @OptIn(ExperimentalForeignApi::class)
            override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {

                didUpdateLocations.firstOrNull()?.let {
                    val location = it as CLLocation
                    location.coordinate.useContents {
                        val coor = this
                        CoroutineScope(Dispatchers.Default).async {
                            val locationData = getAddressFromLocation(coor)
                            val result = Json.encodeToString(locationData)
                            continuation.resume(
                                value = result,
                                onCancellation = { }
                            )
                        }
                    }
                }
            }

            override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
                print("error")
            }
        }

        manager.delegate = delegate
    }

    fun getLocationInfo(getLocationData: (String) -> Unit) {
        MainScope().launch(ioAsyncExceptionHandler) {
            getLocationData(getLocationInfo())
        }
    }

    private suspend fun getAddressFromLocation(coordinate: CLLocationCoordinate2D): LocationData = suspendCancellableCoroutine { continuation ->

        var locationData = LocationData()
        locationData.latitude = coordinate.latitude
        locationData.longitude = coordinate.longitude

        val geocoder = CLGeocoder()
        geocoder.reverseGeocodeLocation(
            location = CLLocation(coordinate.latitude,coordinate.longitude),
            completionHandler = { list, error ->
                if (error != null) {
                    continuation.resumeWithException(Exception(error.localizedDescription))
                } else {
                    list?.firstOrNull()?.let {
                        (it as CLPlacemark).run {
                            locationData.addressLine1 = thoroughfare
                            locationData.city = locality
                            locationData.postalCode = postalCode
                            locationData.state = administrativeArea
                            locationData.countryCode = ISOcountryCode
                        }
                    }
                    continuation.resume(
                        value = locationData,
                        onCancellation = {}
                    )
                }
            }
        )
    }
}