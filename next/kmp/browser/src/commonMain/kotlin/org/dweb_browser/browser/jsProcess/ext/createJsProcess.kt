package org.dweb_browser.browser.jsProcess.ext

import org.dweb_browser.core.http.router.HandlerContext
import org.dweb_browser.core.http.router.HttpHandlerToolkit
import org.dweb_browser.core.http.router.HttpRouter
import org.dweb_browser.core.ipc.Ipc
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.helper.collectIn

suspend fun MicroModule.Runtime.createJsProcess(): JsProcess {
  val mainIpc = this.connect("js.browser.dweb")
  val jsProcessIpc = mainIpc.fork()
  val handlerId = jsProcessIpc.request("file://js.browser.dweb/create-process").text()
  return JsProcess(handlerId, this, jsProcessIpc)
}

class JsProcess(
  private val handlerId: String,
  private val runtime: MicroModule.Runtime,
  private val jsProcessIpc: Ipc,
) : HttpHandlerToolkit {
  private val httpRouter = HttpRouter(runtime, runtime.mmid)
  override fun getContextHttpRouter(): HttpRouter {
    return httpRouter
  }

  fun defineRoutes(definer: HttpHandlerToolkit.() -> Unit) {
    definer()
  }

  init {
    jsProcessIpc.onRequest("js-process-http").collectIn(jsProcessIpc.scope) {
      it.consumeFilter { ipcServerRequest ->
        val pureServerRequest = ipcServerRequest.toPure()
        val httpHandlerChain =
          httpRouter.withFilter(pureServerRequest) ?: return@consumeFilter false
        val pureResponse = httpHandlerChain(HandlerContext(pureServerRequest, jsProcessIpc))
        jsProcessIpc.postResponse(ipcServerRequest.reqId, pureResponse)
        return@consumeFilter true
      }
    }
  }
}