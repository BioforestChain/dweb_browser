package org.dweb_browser.pure.http

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

    suspend fun emitChange(newPort: UShort, by: String) {
      if (currentPort != newPort) {
        debugHttpPureServer("onPortChange/emit") { "${this@onPortChange}/${stateFlow} onPortChange $currentPort=>$newPort (task=$task by=$by)" }
        currentPort = newPort
        onPortChange(newPort)
      }
    }
    if (false) {
      launch {
        while (true) {
          delay(10000)
          debugHttpPureServer("onPortChange/loop/debug") { "${this@onPortChange}/${stateFlow} currentPort=$currentPort task=$task" }
        }
      }
      launch {
        while (true) {
          delay(1000)
          stateFlow.value.also { newPort ->
            if (newPort != null) {
              emitChange(newPort, "loop")
            }
          }
        }
      }
    }
    stateFlow.collect { newPort ->
      if (newPort != null) {
        emitChange(newPort, "collect")
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
