package org.dweb_browser.sys.location

import com.teamdev.jxbrowser.js.JsFunctionCallback
import com.teamdev.jxbrowser.js.JsObject
import kotlinx.coroutines.CompletableDeferred
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.helper.runIf

actual class LocationManage actual constructor(actual val mm: MicroModule.Runtime) {
  /**
   * 获取当前的位置信息
   */
  actual suspend fun getCurrentLocation(precise: Boolean): GeolocationPosition {
    val result = CompletableDeferred<GeolocationPosition>()
    runCatching {
      DesktopLocationObserver.getWebGeolocation(mm)
        .call<Unit>("getCurrentPosition", JsFunctionCallback {
          result.complete(jsGeolocationPositionToNative(it[0] as JsObject))
        }, JsFunctionCallback { arguments ->
          val error = arguments[0] as JsObject
          val errorState = if (error.hasProperty("code") && error.hasProperty("message")) {
            error.property<Double>("code").runIf {
              when (val code = it.toInt()) {
                1 -> GeolocationPositionState.PERMISSION_DENIED
                2 -> GeolocationPositionState.POSITION_UNAVAILABLE
                3 -> GeolocationPositionState.TIMEOUT
                else -> error.property<String>("message").runIf { message ->
                  GeolocationPositionState(code, message)
                }
              }
            }
          } else null
          result.complete(
            GeolocationPosition.createErrorObj(
              errorState ?: GeolocationPositionState.POSITION_UNAVAILABLE
            )
          )
        }, DesktopLocationObserver.getWebGeolocationOptions(mm, precise))
    }.getOrElse {
      result.complete(
        GeolocationPosition.createErrorObj(
          GeolocationPositionState.POSITION_UNAVAILABLE
        )
      )
    }
    return result.await()
  }

  /**
   * 创建一个监听器
   * 监听位置信息，位置信息变化及时通知
   * 返回的Boolean表示是否正常发送，如果发送遗产，关闭监听。
   */
  actual suspend fun createLocationObserver(autoStart: Boolean): LocationObserver {
    val observer = DesktopLocationObserver(mm)
    if (autoStart) {
      observer.start()
    }
    return observer
  }
}
