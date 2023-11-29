package org.dweb_browser.sys.window.ext

import kotlinx.coroutines.CompletableDeferred
import org.dweb_browser.core.ipc.helper.IpcEvent
import org.dweb_browser.core.ipc.helper.IpcEventMessageArgs
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.helper.UUID
import org.dweb_browser.helper.WeakHashMap
import org.dweb_browser.helper.getOrPut
import kotlin.jvm.JvmInline

/**
 * Renderer：窗口由 window.sys.dweb 被创建后，要求窗口拥有着对内容进行渲染
 */
private const val RENDERER_EVENT_NAME = "renderer"
private const val RENDERER_DESTROY_EVENT_NAME = "renderer-destroy"
fun IpcEvent.Companion.createRenderer(wid: String) = fromUtf8(RENDERER_EVENT_NAME, wid)
fun IpcEvent.Companion.createRendererDestroy(wid: String) =
  fromUtf8(RENDERER_DESTROY_EVENT_NAME, wid)

fun IpcEvent.isRenderer() = name == RENDERER_EVENT_NAME
fun IpcEvent.isRendererDestroy() = name == RENDERER_DESTROY_EVENT_NAME

private val mainWindowIdWM = WeakHashMap<NativeMicroModule, CompletableDeferred<UUID>>()
private fun getMainWindowIdWMDeferred(mm: NativeMicroModule) =
  mainWindowIdWM.getOrPut(mm) { CompletableDeferred() }

suspend fun NativeMicroModule.getMainWindowId() = getMainWindowIdWMDeferred(this).await()
suspend fun NativeMicroModule.getOrOpenMainWindowId() =
  if (!hasMainWindow) openMainWindow().id else getMainWindowId()

val NativeMicroModule.hasMainWindow
  get() = getMainWindowIdWMDeferred(this).isCompleted

fun NativeMicroModule.onRenderer(cb: suspend RendererContext.() -> Unit) = onConnect { (ipc) ->
  ipc.onEvent { args ->
    if (args.event.isRenderer()) {
      val context = RendererContext(args)
      getMainWindowIdWMDeferred(this@onRenderer).complete(context.wid)
      context.cb()
    } else if (args.event.isRendererDestroy()) {
      mainWindowIdWM.remove(this@onRenderer)?.cancel()
    }
  }
}

@JvmInline
value class RendererContext(private val eventMessage: IpcEventMessageArgs) {
  val wid get() = eventMessage.event.text
  val ipc get() = eventMessage.ipc
}