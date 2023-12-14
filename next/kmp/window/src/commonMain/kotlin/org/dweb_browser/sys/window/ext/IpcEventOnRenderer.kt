package org.dweb_browser.sys.window.ext

import kotlinx.coroutines.CompletableDeferred
import org.dweb_browser.core.ipc.Ipc
import org.dweb_browser.core.ipc.helper.IpcEvent
import org.dweb_browser.core.ipc.helper.IpcEventMessageArgs
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.helper.SafeHashMap
import org.dweb_browser.helper.SimpleSignal
import org.dweb_browser.helper.WeakHashMap
import org.dweb_browser.helper.getOrPut

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

private val mainWindowIdWM = WeakHashMap<NativeMicroModule, CompletableDeferred<String>>()
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
      val context = RendererContext.get(args)
      getMainWindowIdWMDeferred(this@onRenderer).complete(context.wid)
      context.cb()
    } else if (args.event.isRendererDestroy()) {
      val context = RendererContext.get(args)
      mainWindowIdWM.remove(this@onRenderer)?.cancel()
      if (context.disposeSignal.isInitialized()) {
        context.disposeSignal.value.emitAndClear()
      }
    }
  }
}

class RendererContext(val wid: String, val ipc: Ipc) {
  companion object {
    private val windowRendererContexts = SafeHashMap<String, RendererContext>()
    fun get(args: IpcEventMessageArgs) = getWid(args).let { wid ->
      windowRendererContexts.getOrPut(wid) { RendererContext(wid, args.ipc) }
    }

    private inline fun getWid(args: IpcEventMessageArgs) = args.event.text
  }

  internal val disposeSignal = lazy { SimpleSignal() }
  val onDispose by lazy { disposeSignal.value.toListener() }
}