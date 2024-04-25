package org.dweb_browser.core.std.dns.ext

import org.dweb_browser.core.ipc.Ipc
import org.dweb_browser.core.ipc.helper.IpcEvent
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.helper.collectIn
import org.dweb_browser.helper.trueAlso

/**
 * Activity的意义在于异步启动某些任务，而不是总在 bootstrap 的时候就全部启动
 * 1. 比如可以避免启动依赖造成的启动的堵塞
 * 1. 比如可以用来唤醒渲染窗口
 */
private const val ACTIVITY_EVENT_NAME = "activity"
fun IpcEvent.Companion.createActivity(data: String) = IpcEvent.fromUtf8(ACTIVITY_EVENT_NAME, data)
fun IpcEvent.isActivity() = name == ACTIVITY_EVENT_NAME
suspend fun MicroModule.Runtime.onActivity(cb: suspend (value: Pair<IpcEvent, Ipc>) -> Unit) {
  scopeLaunch(cancelable = true) {
    val winIpc = connect("dns.std.dweb")
    winIpc.onEvent("onActivity").collectIn(getRuntimeScope()) { event ->
      event.consumeFilter { ipcEvent ->
        ipcEvent.isActivity().trueAlso {
          cb(Pair(event.consume(), winIpc))
        }
      }
    }
  }
}