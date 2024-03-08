package org.dweb_browser.sys.location

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
import org.dweb_browser.core.module.getAppContext

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

class AndroidLocationObserver : LocationObserver() {
  private val sharedFlow = MutableSharedFlow<GeolocationPosition>()
  override val flow get() = sharedFlow

  inner class AndroidLocationListener : LocationListener {
    override fun onLocationChanged(location: Location) {
      // 对位置进行处理
      val lastLocation = toGeolocationPosition(location)
      sharedFlow.tryEmit(lastLocation)
    }

    @Deprecated("Deprecated in Java")
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

  val listener = AndroidLocationListener()

  companion object {
    private val manager =
      getAppContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager

  }

  @SuppressLint("MissingPermission")
  override suspend fun start(precise: Boolean, minTimeMs: Long, minDistance: Double) {
    val context = getAppContext()

    // 请求多次更新，这会通过回调触发到onLocationChanged
    if (Build.VERSION.SDK_INT >= 30) {
      manager.requestLocationUpdates(
        selectPrecise(precise), minTimeMs, minDistance.toFloat(), context.mainExecutor, listener
      )
    } else {
      manager.requestLocationUpdates(
        selectPrecise(precise), minTimeMs, minDistance.toFloat(), listener, Looper.getMainLooper()
      )
    }
  }

  override suspend fun stop() {
    manager.removeUpdates(listener)
  }
}