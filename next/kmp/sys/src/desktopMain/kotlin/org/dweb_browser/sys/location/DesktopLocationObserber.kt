package org.dweb_browser.sys.location

import com.teamdev.jxbrowser.js.JsFunctionCallback
import com.teamdev.jxbrowser.js.JsObject
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.dweb_browser.helper.SuspendOnce
import org.dweb_browser.helper.SuspendOnce1
import org.dweb_browser.platform.desktop.webview.WebviewEngine

class DesktopLocationObserver() : LocationObserver() {
  companion object {
    val getWebGeolocation = SuspendOnce {
      WebviewEngine.offScreen.newBrowser().run {
        println("mainFrame()=${mainFrame()}")
//        navigation().loadUrl("data:text/html;charset=utf-8;base64,")
        mainFrame().get().executeJavaScript<JsObject>("navigator.geolocation")
          ?: throw Exception("No Support navigator.geolocation")
      }
    }
  }

  private val sharedFlow = MutableSharedFlow<GeolocationPosition>()
  override val flow get() = sharedFlow

  private val _watch = SuspendOnce1 { precise: Boolean ->
    coroutineScope {
      getWebGeolocation().call<Double>("watchPosition", JsFunctionCallback { arguments ->
        sharedFlow.tryEmit(jsGeolocationPositionToNative(arguments[0] as JsObject))
      }, JsFunctionCallback {
        launch {
          stop()
        }
      }, object {
        @Suppress("unused")
        val enableHighAccuracy = precise
      })!!
    }
  }

  override suspend fun start(precise: Boolean, minTimeMs: Long, minDistance: Double) {
    _watch(precise)
  }

  override suspend fun stop() {
    if (_watch.haveRun) {
      getWebGeolocation().call<Unit>("clearWatch", _watch.getResult().also {
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