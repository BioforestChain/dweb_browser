package org.dweb_browser.core.std.dns.ext

import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.dweb_browser.core.ipc.helper.IpcEvent
import org.dweb_browser.core.ipc.helper.IpcEventMessageArgs
import org.dweb_browser.core.module.MicroModule

/**
 * Activity的意义在于异步启动某些任务，而不是总在 bootstrap 的时候就全部启动
 * 1. 比如可以避免启动依赖造成的启动的堵塞
 * 1. 比如可以用来唤醒渲染窗口
 */
private const val ACTIVITY_EVENT_NAME = "activity"
fun IpcEvent.Companion.createActivity(data: String) = IpcEvent.fromUtf8(ACTIVITY_EVENT_NAME, data)
fun IpcEvent.isActivity() = name == ACTIVITY_EVENT_NAME
fun MicroModule.onActivity(cb: suspend (value: IpcEventMessageArgs) -> Unit) = onConnect { (ipc) ->
  ipc.eventFlow.onEach { args ->
    if (args.event.isActivity()) {
      cb(args)
    }
  }.launchIn(ioAsyncScope)
}