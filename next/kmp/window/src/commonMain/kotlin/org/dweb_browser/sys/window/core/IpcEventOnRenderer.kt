package org.dweb_browser.sys.window.core

import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.ipc.helper.IpcEvent
import org.dweb_browser.core.ipc.helper.IpcEventMessageArgs

/**
 * Renderer：窗口由 window.std.dweb 被创建后，要求窗口拥有着对内容进行渲染
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