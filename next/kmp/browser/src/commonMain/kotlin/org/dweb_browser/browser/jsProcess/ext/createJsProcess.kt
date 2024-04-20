package org.dweb_browser.browser.jsProcess.ext

import io.ktor.http.URLBuilder
import org.dweb_browser.core.http.router.HandlerContext
import org.dweb_browser.core.http.router.HttpHandlerToolkit
import org.dweb_browser.core.http.router.HttpRouter
import org.dweb_browser.core.ipc.Ipc
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.helper.buildUnsafeString
import org.dweb_browser.helper.collectIn

suspend fun MicroModule.Runtime.createJsProcess(processName: String?): JsProcess {
  val mainIpc = this.connect("js.browser.dweb")
  val codeIpc = mainIpc.fork(autoStart = true)
  val fetchIpc = mainIpc.fork(autoStart = true)
  val handlerId = codeIpc.request(URLBuilder("file://js.browser.dweb/create-process").run {
    processName?.also { parameters["name"] = processName }
    parameters["fetch-ipc-pid"] = fetchIpc.pid.toString()
    buildUnsafeString()
  }).text()
  return JsProcess(handlerId, this, codeIpc, fetchIpc)
}

class JsProcess(
  private val handlerId: String,
  private val runtime: MicroModule.Runtime,
  val codeIpc: Ipc,
  val fetchIpc: Ipc,
) : HttpHandlerToolkit {
  private val httpRouter = HttpRouter(runtime, runtime.mmid)
  override fun getContextHttpRouter(): HttpRouter {
    return httpRouter
  }

  fun defineRoutes(definer: HttpHandlerToolkit.() -> Unit) {
    definer()
  }

  init {
    codeIpc.onRequest("js-process-http").collectIn(codeIpc.scope) {
      it.consumeFilter { ipcServerRequest ->
        val pureServerRequest = ipcServerRequest.toPure()
        val httpHandlerChain =
          httpRouter.withFilter(pureServerRequest) ?: return@consumeFilter false
        val pureResponse = httpHandlerChain(HandlerContext(pureServerRequest, codeIpc))
        codeIpc.postResponse(ipcServerRequest.reqId, pureResponse)
        return@consumeFilter true
      }
    }
  }
}