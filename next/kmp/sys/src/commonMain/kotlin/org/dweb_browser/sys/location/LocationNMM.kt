package org.dweb_browser.sys.location

import io.ktor.http.HttpMethod
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.toJsonElement

val debugLocation = Debugger("Location")

class LocationNMM : NativeMicroModule("geolocation.sys.dweb", "geolocation") {
  init {
    categories =
      listOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Device_Management_Service)
  }

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    val locationApi = LocationApi()
    routes(
      "/location" bind HttpMethod.Get by defineJsonResponse {
        debugLocation("location", "enter")
        locationApi.getCurrentLocation().toJsonElement()
      },
      "/observe" bind HttpMethod.Get by defineJsonLineResponse {
        debugLocation("observe", "enter")
        locationApi.observeLocation {
          try {
            emit(it.toJsonElement())
            true // 这边表示正常，继续监听
          } catch (e: Exception) {
            debugLocation("observe", e.message ?: "close observe")
            end()
            false // 这边表示异常，关闭监听
          }
        }
      }
    )
  }

  override suspend fun _shutdown() {
  }
}