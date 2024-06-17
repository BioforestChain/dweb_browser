package org.dweb_browser.dwebview.messagePort

import com.teamdev.jxbrowser.ObjectClosedException
import com.teamdev.jxbrowser.js.JsArray
import com.teamdev.jxbrowser.js.JsObject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.dweb_browser.core.ipc.helper.DWebMessage
import org.dweb_browser.core.ipc.helper.IWebMessagePort
import org.dweb_browser.dwebview.DWebView
import org.dweb_browser.helper.DeferredSignal
import org.dweb_browser.helper.Once
import org.dweb_browser.helper.WARNING
import kotlin.jvm.optionals.getOrNull

class DWebMessagePort(val port: /* MessagePort */JsObject, private val webview: DWebView) :
  IWebMessagePort {

  private val startOnce = Once(before = {
    if (closed) {
      throw Exception("already closed")
    }
  }) {
    val messageChannel = Channel<DWebMessage>(capacity = Channel.UNLIMITED)
    fun handleMessageEvent(event: JsObject) = runCatching {
      val ports = runCatching {
        event.property<JsArray>("ports").get().let { jsPorts ->
          mutableListOf<DWebMessagePort>().apply {
            for (index in 0..<jsPorts.length()) {
              add(DWebMessagePort(jsPorts.get<JsObject>(index)!!, webview))
            }
          }
        }
      }.getOrElse { emptyList() }

      val message = event.property<Any>("data").getOrNull()
      when (message) {
        is String -> DWebMessage.DWebMessageString(message, ports)
        is ByteArray -> DWebMessage.DWebMessageBytes(message, ports)
        else -> {
          // 如果是null，说明event被回收了，那么我们就要重新拿这个对象
          if (!event.hasProperty("type")) {
            return@runCatching false
          }
          null
        }
      }?.also { dwebMessage ->
        messageChannel.trySend(dwebMessage)
      }

      true
    }.getOrElse {
      false
    }

    val webMessagePortPicker = webview.viewEngine.createWebMessagePortPicker(port)

    val job = webview.lifecycleScope.launch {
      runCatching {
        webMessagePortPicker.collect {
          handleMessageEvent(it)
        }
        messageChannel.close()
      }.getOrElse {
        messageChannel.close(it)
        this@DWebMessagePort.close()
      }
    }
    messageChannel.invokeOnClose {
      job.cancel()
    }

    messageChannel
  }

  override suspend fun start() {
    startOnce()
  }

  private var closed = false
  override suspend fun close(cause: CancellationException?) {
    if (!closed) {
      closed = true
      if (startOnce.haveRun) {
        startOnce.getResult().close(cause)
      }
      try {
        port.call<Unit>("close")
      } catch (e: ObjectClosedException) {
        WARNING("messageChannel port 对象已经被释放！${cause}")
      } catch (e: Exception) {
        WARNING("port.call send message error：[${e.message}]")
      }
      unref()
    }
  }

  private val unRefDeferred = CompletableDeferred<Unit>()
  val onUnRef = DeferredSignal(unRefDeferred)
  override suspend fun unref() {
    unRefDeferred.complete(Unit)
    port.close()
  }

  override suspend fun postMessage(event: DWebMessage): Unit = runCatching {
    when (event) {
      is DWebMessage.DWebMessageBytes -> {
        val ports = event.ports.map {
          require(it is DWebMessagePort)
          it.port
        }

        port.call<Unit>("postMessage", event.binary, ports)
      }

      is DWebMessage.DWebMessageString -> {
        val ports = event.ports.map {
          require(it is DWebMessagePort)
          it.port
        }
        port.call<Unit>("postMessage", event.text, ports)
      }
    }
  }.getOrElse {
    // jsObject 可能已经被释放了，这种情况下，无需报错
    if (it !is ObjectClosedException) {
      // TODO 这里的错误应该走 messageerror 事件来抛出，而不是这里打印警告
      WARNING(it.stackTraceToString())
    }
  }

  override val onMessage by lazy {
    startOnce()
  }
}
