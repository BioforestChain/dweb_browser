@file:OptIn(ExperimentalJsExport::class)

package org.dweb_browser.wasm_backend

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.await
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.w3c.dom.MessageChannel
import org.w3c.dom.MessageEvent
import org.w3c.dom.MessagePort
import kotlin.random.Random

suspend fun main() {
  println("hi wasm backend !!ZZZZ")
  val channel = MessageChannel()
  val port1 = channel.port1
  val port2 = channel.port2
  startPureViewController(port2)
  coroutineScope {
    val viewModal = PureViewModal(this, port1)
    val mockData = listOf("hi ~ 1", "hi ~ 2", "hi ~ 3", "hi ~ 5")
    launch {
      while (true) {
        delay(5000)
        viewModal.title.emit(mockData[Random.nextInt(mockData.size)])
      }
    }
  }
}

external fun startPureViewController(viewModal: MessagePort)

class PureViewModal(
  private val coroutineScope: CoroutineScope,
  private val messagePort: MessagePort
) {
  val port = MutableStateFlow(0)
  val title = MutableStateFlow("xixix")

  init {
    coroutineScope.launch {
      var httpServerPort = 0
      println("createHttpServer: ${::createHttpServer}")
      httpServerPort = createHttpServer {
        println("onReq: ${it.url}")
        coroutineScope.launch {
          if (it.url.endsWith(".html")) {
            it.setHeader("Content-Type".toJsString(), "text/html".toJsString())
            it.write("<h1>Hi</h1>".toJsString())
          } else {
            it.write("$httpServerPort".toJsString())
          }
          it.end()
        }
      }.await<JsNumber>().toInt()
      println("PureViewModal/createHttpServer: $httpServerPort")
      port.emit(httpServerPort)
    }

    messagePort.addEventListener("message") {
      require(it is MessageEvent)
      println("PureViewModal/got message/data: ${it.data}")
    }
    messagePort.start()

    launchSub(port, "port")
    launchSub(title, "title")
  }

  private fun <T> launchSub(flow: StateFlow<T>, name: String) = flow.map {
    println("$name changed => $it")
    messagePort.postMessage(
      Json.encodeToString(mapOf("key" to name, "value" to it.toString())).toJsString()
    )
  }.launchIn(coroutineScope)
}

@JsExport
fun echo(word: String): String {
  return "echo: $word"
}