package org.dweb_browser.sys.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.module.getAppContext
import org.dweb_browser.helper.SimpleSignal
import org.dweb_browser.helper.UUID
import org.dweb_browser.sys.permission.AndroidPermissionTask
import org.dweb_browser.sys.permission.PermissionActivity
import org.dweb_browser.sys.permission.SystemPermissionAdapterManager
import org.dweb_browser.sys.permission.SystemPermissionName

/**
 * minTimeMs 位置更新之间的最小时间间隔（以毫秒为单位）
 * minDistanceM 位置更新之间的最小距离（以米为单位）
 */
@SuppressLint("MissingPermission")
actual class LocationManage {
  companion object {
    private val locationObservers = mutableMapOf<UUID, AndroidLocationObserver>()
  }

  private val context = getAppContext()

  //  拿到位置控制器 （国内无法使用google play服务,因此不能使用LocationServices.API/FusedLocationProviderClient）
//  private val locationClient: FusedLocationProviderClient? = null
  private var manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager


  init {
    SystemPermissionAdapterManager.append {
      if (task.name == SystemPermissionName.LOCATION) {
        PermissionActivity.launchAndroidSystemPermissionRequester(
          microModule,
          AndroidPermissionTask(
            permissions = listOf(
              Manifest.permission.ACCESS_COARSE_LOCATION,
              Manifest.permission.ACCESS_FINE_LOCATION,
            ), title = task.title, description = task.description
          ),
        ).values.firstOrNull()
      } else null
    }
  }


  /**一次性获取位置*/
  actual suspend fun getCurrentLocation(mmid: MMID, precise: Boolean): GeolocationPosition {
    // 请求单次更新
    val observer = createLocalListener(precise = precise)
    return observer.flow.take(1).first().also {
      observer.destroy()
    }
  }

  /**
   * 监听地址
   */
  actual suspend fun observeLocation(
    minDistance: Double, precise: Boolean
  ): LocationObserver {
    return createLocalListener(precise = precise, minDistance = minDistance.toFloat())
  }

  /**移除对应的回调*/
  actual suspend fun removeLocationObserve(observerId: UUID) {
    locationObservers[observerId]?.destroy()
  }


  /**给每个模块创建回调对象*/
  private fun createLocalListener(
    precise: Boolean = true,
    minTimeMs: Long = 0,
    minDistance: Float = 0f,
  ): AndroidLocationObserver {

    val observer = AndroidLocationObserver()
    // 保存
    locationObservers[observer.id] = observer
    observer.onDestroy {
      locationObservers.remove(observer.id)
    }
    observer.start(precise, minTimeMs, minDistance)

    return observer
  }
}


//// 如果关闭了gps和network
//if (getLocationWay().isNullOrEmpty()) {
//  askLocationSettings()
//  return GeolocationPosition.createErrorObj(GeolocationPositionState.POSITION_UNAVAILABLE)
//}
///**获取当前获取位置的途径*/
//private fun getLocationWay(): String? {
//  if (manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
//    return LocationManager.NETWORK_PROVIDER
//  }
//  if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
//    return LocationManager.GPS_PROVIDER
//  }
//  return null
//}
//    if (Build.VERSION.SDK_INT >= 30) {
//      manager.getCurrentLocation(
//        selectPrecise(precise),
//        cancellationSignal,
//        context.mainExecutor
//      ) { location ->
//        if (location != null) {
//          result.complete(toGeolocationPosition(location))
//        } else {
//          val state = GeolocationPositionState.PERMISSION_DENIED
//          result.complete(GeolocationPosition.createErrorObj(state))
//        }
//        cancellationSignal.cancel()
//      }
//    } else {
//      // 创建监听回调 (TODO 这里是适配版本低的手机，未测试)
//      val locationListener = createLocalListener(mmid)
//      manager.requestSingleUpdate(
//        selectPrecise(precise),
//        locationListener,
//        Looper.myLooper()
//      );
//      manager.removeUpdates(locationListener)
//    }

// 转换为web标准需要的结构
private fun toGeolocationPosition(lastLocation: Location): GeolocationPosition {
  val geolocationCoordinates = GeolocationCoordinates(
    accuracy = lastLocation.accuracy.toDouble(),
    latitude = lastLocation.latitude,
    longitude = lastLocation.longitude
  )
  return GeolocationPosition(
    state = GeolocationPositionState.Success,
    coords = geolocationCoordinates,
  )
}

/**
 * 询问是否设置网络权限
 */
private fun askLocationSettings() {
  debugLocation("getCurrentLocation=>", "askLocationSettings")
  getAppContext().startActivity(Intent().apply {
    action = android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS
    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
  })
}

/**
 * 选择定位精度
 */
private fun selectPrecise(precise: Boolean): String {
  return if (precise) {
    LocationManager.GPS_PROVIDER //GPS 定位的精准度比较高，但是非常耗电。
  } else {
    LocationManager.NETWORK_PROVIDER //网络定位的精准度稍差，但耗电量比较少。
  }
}

class AndroidLocationObserver(
  private val sharedFlow: MutableSharedFlow<GeolocationPosition> = MutableSharedFlow(),
) :
  LocationObserver(flow = sharedFlow) {
  val listener = object : LocationListener {
    override fun onLocationChanged(location: Location) {
      // 对位置进行处理
      val lastLocation = toGeolocationPosition(location)
      sharedFlow.tryEmit(lastLocation)
    }

    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {
      debugLocation("getCurrentLocation=>", "onProviderDisabled=>$provider,status=>$status")
      // 代表设备中的定位提供器不可用，例如 GPS 或网络定位被完全关闭。
      if (status == 0) { // LocationProvider.OUT_OF_SERVICE
        // 尝试询问用户是否在设置打开
        askLocationSettings()
        sharedFlow.tryEmit(GeolocationPosition.createErrorObj(GeolocationPositionState.POSITION_UNAVAILABLE))
      }
      // 定位提供器暂时不可用，但很快就会恢复。例如在进入隧道或者建筑物中时，GPS 可能暂时无法使用。
      if (status == 1) { // LocationProvider.TEMPORARILY_UNAVAILABLE
        sharedFlow.tryEmit(GeolocationPosition.createErrorObj(GeolocationPositionState.TIMEOUT))
      }
    }

    // 当提供器被启用时会调用这个方法
    override fun onProviderEnabled(provider: String) {
      debugLocation("getCurrentLocation=>", "onProviderDisabled=>$provider")
    }

    // 当提供器被禁用时会调用这个方法
    override fun onProviderDisabled(provider: String) {
      debugLocation("getCurrentLocation=>", "onProviderDisabled=>$provider")
      sharedFlow.tryEmit(GeolocationPosition.createErrorObj(GeolocationPositionState.PERMISSION_DENIED))
    }

  }

  companion object {
    private val manager =
      getAppContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager

  }

  @SuppressLint("MissingPermission")
  fun start(
    precise: Boolean = true,
    minTimeMs: Long = 0,
    minDistance: Float = 0f,
  ) {
    val context = getAppContext()

    // 请求多次更新，这会通过回调触发到onLocationChanged
    if (Build.VERSION.SDK_INT >= 30) {
      manager.requestLocationUpdates(
        selectPrecise(precise), minTimeMs, minDistance, context.mainExecutor, listener
      )
    } else {
      manager.requestLocationUpdates(
        selectPrecise(precise), minTimeMs, minDistance, listener, Looper.getMainLooper()
      )
    }
  }

  fun stop() {
    manager.removeUpdates(listener)
  }

  private val destroySignal = SimpleSignal()
  val onDestroy = destroySignal.toListener()
  suspend fun destroy() {
    destroySignal.emitAndClear()
    stop()
  }
}