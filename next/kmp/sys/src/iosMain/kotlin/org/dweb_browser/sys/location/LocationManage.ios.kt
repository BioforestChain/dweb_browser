package org.dweb_browser.sys.location

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.std.permission.AuthorizationStatus
import org.dweb_browser.sys.permission.SystemPermissionAdapterManager
import org.dweb_browser.sys.permission.SystemPermissionName
import platform.CoreLocation.CLAuthorizationStatus
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationAccuracy
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusDenied
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLAuthorizationStatusRestricted
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.CoreLocation.kCLLocationAccuracyKilometer
import platform.Foundation.timeIntervalSince1970
import platform.darwin.NSObject
import kotlin.properties.Delegates

actual class LocationManage {
  companion object {
    private val authorizationStatusMap = mutableMapOf<MMID, AuthorizationStatus>()
  }

  // 每个模块对应申请的控制器
  private val clLocationManager = CLLocationManager()
  private val locationDelegate = LocationDelegate(clLocationManager)

  private class LocationDelegate(clLocationManager: CLLocationManager) : NSObject(),
    CLLocationManagerDelegateProtocol {
    private var clAuthorizationStatus: CLAuthorizationStatus by Delegates.observable(
      clLocationManager.authorizationStatus
    ) { _, _, newValue ->
      authorizationStatusListeners.forEach { it(newValue) }
    }

    val authorizationStatusListeners = mutableListOf<(CLAuthorizationStatus) -> Unit>()

    private var clLocation: CLLocation? by Delegates.observable(clLocationManager.location) { _, _, newValue ->
      clLocationListeners.forEach { it(newValue) }
    }
    val clLocationListeners = mutableListOf<(CLLocation?) -> Unit>()
    override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
      clLocation = didUpdateLocations.lastOrNull() as CLLocation?
    }

    override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
      clAuthorizationStatus = manager.authorizationStatus
    }
  }


  init {
    clLocationManager.delegate = locationDelegate
    SystemPermissionAdapterManager.append {
      if (task.name == SystemPermissionName.LOCATION) {
        getLocationAuthorizationStatus(this.microModule.mmid)
      } else null
    }
  }

  private fun matchAuthorizationStatus(clAuthorizationStatus: CLAuthorizationStatus) =
    when (clAuthorizationStatus) {
      kCLAuthorizationStatusAuthorizedAlways, kCLAuthorizationStatusAuthorizedWhenInUse -> AuthorizationStatus.GRANTED

      kCLAuthorizationStatusDenied, kCLAuthorizationStatusRestricted -> AuthorizationStatus.DENIED

      kCLAuthorizationStatusNotDetermined -> AuthorizationStatus.UNKNOWN

      else -> AuthorizationStatus.UNKNOWN
    }

  /**获取权限状态*/
  private suspend fun getLocationAuthorizationStatus(mmid: MMID): AuthorizationStatus {
    val authorizationStatus = authorizationStatusMap.getOrPut(mmid) {
      matchAuthorizationStatus(clLocationManager.authorizationStatus)
    }

    return if (authorizationStatus == AuthorizationStatus.UNKNOWN) {
      val deferred = CompletableDeferred<AuthorizationStatus>()
      locationDelegate.authorizationStatusListeners.add {
        deferred.complete(matchAuthorizationStatus(it))
      }
      clLocationManager.requestWhenInUseAuthorization()
      deferred.await().also {
        locationDelegate.authorizationStatusListeners.clear()
        authorizationStatusMap[mmid] = it
      }
    } else {
      authorizationStatus
    }
  }

  // 单次
  actual suspend fun getCurrentLocation(mmid: MMID, precise: Boolean): LocationFlow {
    return createLocalManager(precise)
  }

  actual suspend fun observeLocation(
    mmid: MMID, minDistance: Double, precise: Boolean
  ): LocationFlow {
    return createLocalManager(precise, minDistance)
  }

  //移除监听
  actual fun removeLocationObserve(mmid: MMID) {
    clLocationManager.stopUpdatingLocation()
    authorizationStatusMap.remove(mmid)
  }

  /**给每个模块创建控制器*/
  private fun createLocalManager(
    precise: Boolean = true,
    minDistance: Double = 0.0,
  ): LocationFlow {
    return callbackFlow {
      clLocationManager.desiredAccuracy = selectPrecise(precise)
      clLocationManager.distanceFilter = minDistance // 移动多少米才会更新

      locationDelegate.clLocationListeners.add {
        it?.also { trySend(toGeolocationPosition(it)) }
      }

      clLocationManager.startUpdatingLocation()

      awaitClose {
        clLocationManager.stopUpdatingLocation()
        locationDelegate.clLocationListeners.clear()
      }
    }
  }

  // 选择精度
  private fun selectPrecise(precise: Boolean): CLLocationAccuracy {
    return if (precise) {
      kCLLocationAccuracyBest //准确度相当高，可以用于需要较高精度的场景。
    } else {
      kCLLocationAccuracyKilometer //精度为最近的1公里，一般只用于粗略定位。
    }
  }

  /**
   * 将ios位置转换为需要结构
   */
  @OptIn(ExperimentalForeignApi::class)
  private fun toGeolocationPosition(
    location: CLLocation
  ): GeolocationPosition {
    // 构造水平和垂直经度
    val accuracy = maxOf(location.horizontalAccuracy, location.verticalAccuracy)
    // 纬度
    val latitude = location.coordinate.useContents {
      this.latitude
    }
    //经度
    val longitude = location.coordinate.useContents {
      this.longitude
    }
    // 高度
    val altitude = location.altitude
    // 航向
    val heading = if (location.course < 0) null else location.course
    // 速度
    val speed = if (location.speed < 0) null else location.speed
    //坐标
    val cords =
      GeolocationCoordinates(accuracy, latitude, longitude, altitude, null, heading, speed)
    //时间戳
    val time = location.timestamp.timeIntervalSince1970().toLong()
    return GeolocationPosition(GeolocationPositionState.Success, cords, time)
  }
}

//delegate: 这是一个委托，处理位置更新和其他相关事件。
//desiredAccuracy: 预期的定位精度，单位是米。
//distanceFilter: 设备必须移动多远才会再次更新其位置。
//activityType: 告诉定位服务你的应用是怎样使用位置更新的(比如是跑步、骑车还是其他)。
//pausesLocationUpdatesAutomatically: 指定当用户停止移动一段时间后，是否自动暂停位置更新。
//allowsBackgroundLocationUpdates: 指定是否允许在后台更新位置信息。
//headingFilter: 设备必须改变多少度的方向才会再次更新其方向。
//headingOrientation: 指定设备的方向是如何被用来计算真北和磁北的。
//maximumRegionMonitoringDistance: 设备可以监控的最大距离。

//kCLLocationAccuracyBestForNavigation: 最准确的定位信息，一般用于导航应用。
//kCLLocationAccuracyBest: 准确度相当高，可以用于需要较高精度的场景。
//kCLLocationAccuracyNearestTenMeters: 精度为最近的10米，适合一般定位需求，耗电也相对较低。
//kCLLocationAccuracyHundredMeters: 精度为最近的100米，适用于精度要求不高、对电池要求较高的场景。
//kCLLocationAccuracyKilometer: 精度为最近的1公里，一般只用于粗略定位。
//kCLLocationAccuracyThreeKilometers: 精度为最近的3公里，耗电最少，精度也最低。