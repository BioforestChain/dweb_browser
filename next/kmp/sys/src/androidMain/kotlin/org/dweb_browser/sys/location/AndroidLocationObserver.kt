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
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.helper.getAppContextUnsafe

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
  debugLocation("AndroidLocationObserver", "askLocationSettings")
  getAppContextUnsafe().startActivity(Intent().apply {
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
    LocationManager.NETWORK_PROVIDER // 网络定位的精准度稍差，但耗电量比较少。
  }
}

class AndroidLocationObserver(override val mm: MicroModule.Runtime) : LocationObserver() {
  private val sharedFlow = MutableSharedFlow<GeolocationPosition>(replay = 1) // 通道中只会保留最后一条记录
  override val flow get() = sharedFlow

  inner class AndroidLocationListener : LocationListener {
    override fun onLocationChanged(location: Location) {
      debugLocation("AndroidListener", "onLocationChanged=>$location")
      // 对位置进行处理
      val lastLocation = toGeolocationPosition(location)
      sharedFlow.tryEmit(lastLocation)
    }

    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {
      debugLocation("AndroidListener", "onStatusChanged=>$provider,status=>$status")
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
      debugLocation("AndroidListener", "onProviderDisabled=>$provider")
    }

    // 当提供器被禁用时会调用这个方法
    override fun onProviderDisabled(provider: String) {
      debugLocation("AndroidListener", "onProviderDisabled=>$provider")
      sharedFlow.tryEmit(GeolocationPosition.createErrorObj(GeolocationPositionState.PERMISSION_DENIED))
    }
  }

  val listener = AndroidLocationListener()

  companion object {
    //  拿到位置控制器 （国内无法使用google play服务,因此不能使用LocationServices.API/FusedLocationProviderClient）
    //  private val locationClient: FusedLocationProviderClient? = null
    private val locationManager =
      getAppContextUnsafe().getSystemService(Context.LOCATION_SERVICE) as LocationManager
  }

  @SuppressLint("MissingPermission")
  override suspend fun start(precise: Boolean, minTimeMs: Long, minDistance: Double) {
    val context = getAppContextUnsafe()

    // 请求多次更新，这会通过回调触发到onLocationChanged
    if (Build.VERSION.SDK_INT >= 30) {
      // 由于 Oppo 手机GPS定位存在问题，这边增加判断，不管是否要求精确，NETWORK_PROVIDER都注册监听
      if (precise) {
        locationManager.requestLocationUpdates(
          LocationManager.GPS_PROVIDER,
          minTimeMs,
          minDistance.toFloat(),
          context.mainExecutor,
          listener
        )
      }
      locationManager.requestLocationUpdates(
        LocationManager.NETWORK_PROVIDER,
        minTimeMs,
        minDistance.toFloat(),
        context.mainExecutor,
        listener
      )
    } else {
      if (precise) {
        locationManager.requestLocationUpdates(
          LocationManager.GPS_PROVIDER,
          minTimeMs,
          minDistance.toFloat(),
          listener,
          Looper.getMainLooper()
        )
      }
      locationManager.requestLocationUpdates(
        LocationManager.NETWORK_PROVIDER,
        minTimeMs,
        minDistance.toFloat(),
        listener,
        Looper.getMainLooper()
      )
    }
  }

  override suspend fun stop() {
    locationManager.removeUpdates(listener)
  }
}