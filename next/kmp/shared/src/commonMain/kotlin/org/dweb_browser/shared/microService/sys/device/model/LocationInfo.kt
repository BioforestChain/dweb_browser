package org.dweb_browser.shared.microService.sys.device.model

import kotlinx.serialization.Serializable


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

expect class LocationInfo() {

    suspend fun getLocationInfo(): String

}