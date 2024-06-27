package org.dweb_browser.sys.location

import com.teamdev.jxbrowser.js.JsFunctionCallback
import com.teamdev.jxbrowser.js.JsObject
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.file.ext.createDir
import org.dweb_browser.helper.SuspendOnce1
import org.dweb_browser.helper.platform.getOrCreateProfile
import org.dweb_browser.helper.platform.webViewEngine

class DesktopLocationObserver(override val mm: MicroModule.Runtime) : LocationObserver() {
  companion object {
    val browser = SuspendOnce1 { mm: MicroModule.Runtime ->
      webViewEngine.offScreenEngine.profiles().getOrCreateProfile(mm.mmid).newBrowser()
    }
    val getWebGeolocation = SuspendOnce1 { mm: MicroModule.Runtime ->
      mm.createDir("/data/sys-location")
      browser(mm)
        .run {
          println("mainFrame()=${mainFrame()}")
//        navigation().loadUrl("data:text/html;charset=utf-8;base64,")
          mainFrame().get().executeJavaScript<JsObject>("navigator.geolocation")
            ?: throw Exception("No Support navigator.geolocation")
        }
    }

    suspend fun getWebGeolocationOptions(mm: MicroModule.Runtime, enableHighAccuracy: Boolean) =
      browser(mm).run {
        mainFrame().get()
          .executeJavaScript<JsObject>("{'enableHighAccuracy': ${enableHighAccuracy}}")
      }
  }

  private val sharedFlow = MutableSharedFlow<GeolocationPosition>()
  override val flow get() = sharedFlow

  private val _watch = SuspendOnce1 { precise: Boolean ->
    coroutineScope {
      getWebGeolocation(mm).call<Double>("watchPosition", JsFunctionCallback { arguments ->
        sharedFlow.tryEmit(jsGeolocationPositionToNative(arguments[0] as JsObject))
      }, JsFunctionCallback {
        launch {
          stop()
        }
      }, getWebGeolocationOptions(mm, precise))!!
    }
  }

  override suspend fun start(precise: Boolean, minTimeMs: Long, minDistance: Double) {
    _watch(precise)
  }

  override suspend fun stop() {
    if (_watch.haveRun) {
      getWebGeolocation(mm).call<Unit>("clearWatch", _watch.getResult().also {
        _watch.reset()
      })
    }
  }

}

internal fun jsGeolocationPositionToNative(jsObject: JsObject) = runCatching {
  GeolocationPosition(
    GeolocationPositionState.Success, jsObject.property<JsObject>("coords").get().run {
      GeolocationCoordinates(
        accuracy = property<Double>("accuracy").get(),
        latitude = property<Double>("latitude").get(),
        longitude = property<Double>("longitude").get(),
        altitude = property<Double>("altitude").get(),
        altitudeAccuracy = property<Double>("altitudeAccuracy").get(),
        heading = property<Double>("heading").get(),
        speed = property<Double>("speed").get(),
      )
    }, jsObject.property<Double>("timestamp").get().toLong()
  )
}.getOrElse { GeolocationPosition.createErrorObj(GeolocationPositionState.POSITION_UNAVAILABLE) }