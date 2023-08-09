package info.bagen.dwebbrowser.microService.sys.window


import info.bagen.dwebbrowser.microService.core.WindowPropertyKeys
import info.bagen.dwebbrowser.microService.core.windowInstancesManager
import org.dweb_browser.helper.Observable
import org.dweb_browser.helper.printdebugln
import org.dweb_browser.microservice.core.BootstrapContext
import org.dweb_browser.microservice.core.NativeMicroModule
import org.dweb_browser.microservice.help.gson
import org.dweb_browser.microservice.ipc.helper.ReadableStream
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.lens.Query
import org.http4k.lens.string
import org.http4k.routing.bind
import org.http4k.routing.routes

fun debugWindow(tag: String, msg: Any? = "", err: Throwable? = null) =
  printdebugln("window", tag, msg, err)

/**
 * 标准化窗口管理模块
 *
 * 该模块暂时不会下放到 std 级别，std 级别通常属于非常底层的中立标准，比如通讯等与客观物理相关的，std是一个dweb平台的最小子集，未来可以基于该标准做认证平台。
 * 而sys级别拥有各异的实现，不同的厂商可以在这个级别做自己的操作系统标准化设计。
 * 这里的windows.sys.dweb属于当下这个时代的一种矩形窗口化设计，它不代表所有的窗口形态，它有自己的取舍。
 */
class WindowNMM :
  NativeMicroModule("window.sys.dweb", "Window Management") {
  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    val query_wid = Query.string().required("wid")
    fun getWindow(request: Request) = query_wid(request).let { wid ->
      windowInstancesManager.instances[wid]
        ?: throw Exception("No Found by window id: '$wid'")
    }

    apiRouting = routes(
      /** 窗口的状态监听 */
      "/observe" bind Method.GET to defineHandler { request, ipc ->
        val win = getWindow(request)

        debugWindow("/observe", "wid: ${win.id} ,mmid: ${ipc.remote.mmid}")
        val inputStream = ReadableStream(onStart = { controller ->
          val off = win.state.observable.onChange {
            try {
              controller.enqueue(gson.toJson(win.state) + "\n")
            } catch (e: Exception) {
              controller.close()
              e.printStackTrace()
            }
          }.also {
            it.emitSelf(
              Observable.Change(
                WindowPropertyKeys.Any,
                null,
                null
              )
            )
          }
          ipc.onClose {
            off()
            controller.close()
          }
        })
        return@defineHandler Response(Status.OK).body(inputStream)
      },
      "/focus" bind Method.GET to defineHandler { request -> getWindow(request).focus() },
      "/blur" bind Method.GET to defineHandler { request -> getWindow(request).blur() },
      "/maximize" bind Method.GET to defineHandler { request -> getWindow(request).maximize() },
      "/unMaximize" bind Method.GET to defineHandler { request -> getWindow(request).unMaximize() },
      "/minimize" bind Method.GET to defineHandler { request -> getWindow(request).minimize() },
      "/close" bind Method.GET to defineHandler { request -> getWindow(request).close() },
    )
  }

  override suspend fun _shutdown() {
  }

}