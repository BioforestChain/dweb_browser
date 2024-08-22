package org.dweb_browser.pure.http

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.globalDefaultScope

val debugHttpPureServer = Debugger("httpPureServer")

typealias HttpPureServerOnRequest = suspend (PureServerRequest) -> PureResponse?

expect class HttpPureServer(onRequest: HttpPureServerOnRequest) {
  val onRequest: HttpPureServerOnRequest
  val stateFlow: StateFlow<UShort?>
  suspend fun start(port: UShort): UShort
  suspend fun close()
  suspend fun getDebugInfo(): String
}

fun HttpPureServer.onPortChange(
  task: String,
  emitOnInit: Boolean = true,
  onPortChange: suspend (UShort) -> Unit,
): Job =
  globalDefaultScope.launch {
    var currentPort = when {
      emitOnInit -> null
      else -> stateFlow.value
    }

    stateFlow.collect { newPort ->
      if (newPort != null && currentPort != newPort) {
        debugHttpPureServer("onPortChange/emit") { "onPortChange $currentPort=>$newPort task=$task (in ${this@onPortChange})" }
        currentPort = newPort
        onPortChange(newPort)
      }
    }
  }


internal val allHttpPureServerInstances = mutableSetOf<HttpPureServer>()
internal suspend fun tryDoHttpPureServerResponse(pureServerRequest: PureServerRequest): PureResponse? {
  for (server in allHttpPureServerInstances) {
    val response = server.onRequest(pureServerRequest)
    if (response != null) {
      return response
    }
  }
  return null
}
