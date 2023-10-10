package org.dweb_browser.window.core

import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.ipc.helper.IpcEvent
import org.dweb_browser.core.ipc.helper.IpcEventMessageArgs

/**
 * Activity的意义在于异步启动某些任务，而不是总在 bootstrap 的时候就全部启动
 * 1. 比如可以避免启动依赖造成的启动的堵塞
 * 1. 比如可以用来唤醒渲染窗口
 */
private const val RENDERER_EVENT_NAME = "renderer"
fun IpcEvent.Companion.createRenderer(data: String) = IpcEvent.fromUtf8(RENDERER_EVENT_NAME, data)
fun IpcEvent.isRenderer() = name == RENDERER_EVENT_NAME
fun MicroModule.onRenderer(cb: suspend RendererContext.() -> Unit) = onConnect { (ipc) ->
  ipc.onEvent { args ->
    if (args.event.isRenderer()) {
      RendererContext(args).cb()
    }
  }
}

@JvmInline
value class RendererContext(private val eventMessage: IpcEventMessageArgs) {
  val wid get() = eventMessage.event.text
  val ipc get() = eventMessage.ipc
}