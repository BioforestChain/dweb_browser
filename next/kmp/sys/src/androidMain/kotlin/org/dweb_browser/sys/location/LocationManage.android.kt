package org.dweb_browser.sys.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import org.dweb_browser.core.module.getAppContext
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
  actual suspend fun getCurrentLocation(precise: Boolean): GeolocationPosition {
    // 请求单次更新
    val observer = createLocationObserver(false)
    observer.start(precise = precise)
    return observer.flow.take(1).first().also {
      observer.destroy()
    }
  }

  /**
   * 监听地址
   */
  actual suspend fun createLocationObserver(autoStart: Boolean): LocationObserver {
    val observer = AndroidLocationObserver()
    if (autoStart) {
      observer.start()
    }
    return observer
  }
}

