package org.dweb_browser.microservice.core

import org.dweb_browser.microservice.ipc.helper.IpcEvent

enum class InternalIpcEvent(val ipcEventName: String) {
  /**
   * Activity的意义在于异步启动某些任务，而不是总在 bootstrap 的时候就全部启动
   * 1. 比如可以避免启动依赖造成的启动的堵塞
   * 1. 比如可以用来唤醒渲染窗口
   */
  Activity("activity"),
  ;

  fun create(data: String) = IpcEvent.fromUtf8(ipcEventName, data)

}

infix fun IpcEvent.isTypeof(type: InternalIpcEvent) = name == type.ipcEventName
