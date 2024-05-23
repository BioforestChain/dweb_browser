package org.dweb_browser.sys.location

import android.annotation.SuppressLint
import kotlinx.coroutines.flow.first
import org.dweb_browser.core.module.MicroModule

/**
 * minTimeMs 位置更新之间的最小时间间隔（以毫秒为单位）
 * minDistanceM 位置更新之间的最小距离（以米为单位）
 */
@SuppressLint("MissingPermission")
actual class LocationManage actual constructor(actual val mm: MicroModule.Runtime) {
  /**一次性获取位置*/
  actual suspend fun getCurrentLocation(precise: Boolean): GeolocationPosition {
    debugLocation("LocationManage", "getCurrentLocation => precise=$precise")
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
    debugLocation("LocationManage", "createLocationObserver => autoStart=$autoStart")
    val observer = AndroidLocationObserver(mm)
    if (autoStart) {
      observer.start()
    }
    return observer
  }
}

