package org.dweb_browser.sys.location

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take

actual class LocationManage {
  /**
   * 获取当前的位置信息
   */
  actual suspend fun getCurrentLocation(precise: Boolean): GeolocationPosition {
    // 请求单次更新
    val observer = createLocationObserver(false)
    observer.start(precise = precise)
    return observer.flow.take(1).first().also {
      observer.destroy()
    }
  }

  /**
   * 创建一个监听器
   * 监听位置信息，位置信息变化及时通知
   * 返回的Boolean表示是否正常发送，如果发送遗产，关闭监听。
   */
  actual suspend fun createLocationObserver(autoStart: Boolean): LocationObserver {
    val observer = DesktopLocationObserver()
    if (autoStart) {
      observer.start()
    }
    return observer
  }

}

