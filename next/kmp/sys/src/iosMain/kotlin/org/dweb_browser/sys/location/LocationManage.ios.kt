package org.dweb_browser.sys.location

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.permission.AuthorizationStatus
import org.dweb_browser.helper.withMainContext
import org.dweb_browser.sys.permission.RequestSystemPermission
import org.dweb_browser.sys.permission.SystemPermissionName
import platform.CoreLocation.CLAuthorizationStatus
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusDenied
import platform.CoreLocation.kCLAuthorizationStatusRestricted

actual class LocationManage actual constructor(actual val mm: MicroModule.Runtime) {

  companion object {
    // 每个模块对应申请的控制器
    private val clLocationManager by lazy { CLLocationManager() }
    private val locationDelegate by lazy { LocationDelegate(clLocationManager) }

    private var authorizationStatus: AuthorizationStatus? = null
    internal val locationPermission: RequestSystemPermission = {
      if (task.name == SystemPermissionName.LOCATION) {
        authorizationStatus ?: run {
          val status = matchAuthorizationStatus(clLocationManager.authorizationStatus)
          if (status == AuthorizationStatus.UNKNOWN) {
            val deferred = CompletableDeferred<AuthorizationStatus>()
            locationDelegate.authorizationStatusListeners.add {
              deferred.complete(matchAuthorizationStatus(it))
            }
            clLocationManager.requestWhenInUseAuthorization()
            deferred.await().also {
              locationDelegate.authorizationStatusListeners.clear()
            }
          } else status
        }.also { authorizationStatus = it }
      } else null
    }

    private fun matchAuthorizationStatus(clAuthorizationStatus: CLAuthorizationStatus) =
      when (clAuthorizationStatus) {
        /**
         * kCLAuthorizationStatusAuthorizedWhenInUse: 仅仅只能在应用前台运行的时候使用
         */
        kCLAuthorizationStatusAuthorizedAlways, kCLAuthorizationStatusAuthorizedWhenInUse -> AuthorizationStatus.GRANTED
        /**
         * kCLAuthorizationStatusRestricted: 可能是由于存在家长控制等主动限制
         */

        kCLAuthorizationStatusDenied, kCLAuthorizationStatusRestricted -> AuthorizationStatus.DENIED
        /**
         * 包含 kCLAuthorizationStatusNotDetermined
         */
        else -> AuthorizationStatus.UNKNOWN
      }

  }


  /**一次性获取位置*/
  actual suspend fun getCurrentLocation(precise: Boolean): GeolocationPosition {
    // 请求单次更新
    val observer = createLocationObserver(false)
    observer.start(precise = precise)
    return observer.flow.first().also {
      observer.destroy()
    }
  }

  /**
   * 监听地址
   */
  actual suspend fun createLocationObserver(autoStart: Boolean): LocationObserver {
    val observer = withMainContext { IosLocationObserver(mm) }
    if (autoStart) {
      observer.start()
    }
    return observer
  }
}
