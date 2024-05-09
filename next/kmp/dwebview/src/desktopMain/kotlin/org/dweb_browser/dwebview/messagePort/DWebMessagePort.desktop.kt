package org.dweb_browser.dwebview.messagePort

import com.teamdev.jxbrowser.js.JsArray
import com.teamdev.jxbrowser.js.JsException
import com.teamdev.jxbrowser.js.JsFunctionCallback
import com.teamdev.jxbrowser.js.JsObject
import com.teamdev.jxbrowser.js.JsPromise
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.dweb_browser.core.ipc.helper.DWebMessage
import org.dweb_browser.core.ipc.helper.IWebMessagePort
import org.dweb_browser.dwebview.DWebView
import org.dweb_browser.dwebview.debugDWebView
import org.dweb_browser.dwebview.engine.await
import org.dweb_browser.dwebview.engine.window
import org.dweb_browser.helper.randomUUID
import org.dweb_browser.helper.runIf
import kotlin.jvm.optionals.getOrNull

class DWebMessagePort(val port: /* MessagePort */JsObject, private val webview: DWebView) :
  IWebMessagePort {
  private val _started = lazy {
    if (closed) {
      throw Exception("already closed")
    }
    val viewEngine = webview.viewEngine
    val mainFrame = viewEngine.mainFrame
    val messageChannel = Channel<DWebMessage>(capacity = Channel.UNLIMITED)
    fun handleMessageEvent(event: JsObject) = runCatching {

      val ports = event.property<JsArray>("ports").runIf { jsPorts ->
        mutableListOf<DWebMessagePort>().apply {
          for (index in 0..<jsPorts.length()) {
            add(DWebMessagePort(jsPorts.get<JsObject>(index)!!, webview))
          }
        }
      } ?: emptyList()

      val message = event.property<Any>("data").getOrNull()
      if (message is String) {
        debugDWebView(
          "message-in",
          when (val len = message.length) {
            in 0..100 -> message
            else -> message.slice(0..59) + "..." + message.slice(len - 50..<len)
          }
        )
      } else {
        debugDWebView(
          "message-in-2", message
        )
      }
      when (message) {
        is String -> DWebMessage.DWebMessageString(message, ports)
        is ByteArray -> DWebMessage.DWebMessageBytes(message, ports)
        else -> {
          // 如果是null，说明event被回收了，那么我们就要重新拿这个对象
          if (!event.hasProperty("type")) {
            event.close()
            return@runCatching false
          }
          null
        }
      }?.also { dwebMessage ->
        messageChannel.trySend(dwebMessage)
      }

      // release jsObject
      event.close()
      true
    }.getOrElse {
      false
    }

//    val cb = JsFunctionCallback {
//      handleMessageEvent(it[0] as JsObject)
//    }
//
//    port.call<Unit>("addEventListener", "message", cb)
//    port.call<Unit>("start")
//    messageChannel.invokeOnClose {
//      port.call<Unit>("removeEventListener", "message", cb)
//    }

    /// 这是一个简单的方案，但速度比下面全异步的方案慢了一倍
    /// 并且因为是同步方案，close也会有一些问题，
    if (false) {
      val randomName = randomUUID()
      mainFrame.executeJavaScript<Unit>(
        """
        window["$randomName"] = (port) => {
          port.addEventListener("message", (event)=>{
            while(true){
              if(port["$randomName-message"](event)){
                break
              }
            }
          })
          const port_close = port.close;
          port.close = ()=>{
            port_close.call(port)
            port["$randomName-close"]()
          }
        }
      """.trimIndent()
      )
      val jsWindow = mainFrame.window()
      jsWindow.call<Unit>(randomName, port)
      jsWindow.removeProperty(randomName)
      port.putProperty("$randomName-message", JsFunctionCallback {
        handleMessageEvent(it[0] as JsObject)
      })
      jsWindow.removeProperty(randomName)
      port.putProperty("$randomName-close", JsFunctionCallback {
        messageChannel.close()
      })
      port.call<Unit>("start")
    }

    /// 全异步方案
    if (true) {
      val randomName = randomUUID()
      mainFrame.executeJavaScript<Unit>(
        """window["$randomName"] = (port) => {
  const withResolvers = () => {
    const out = {
      is_po: true,
    };
    out.promise = new Promise((resolve_, reject_) => {
      out.resolve = resolve_;
      out.reject = reject_;
    });
    return out;
  };
  const messages = {
    length: 0,
    push: (item) => {
      messages[messages.length++] = item;
    },
  };
  const waiters = [];
  /// 允许重新获取，但是不可 seek
  port.pickEvent = (i) => {
    delete messages[i - 1];
    const event = messages[i];
    if (event !== undefined) {
      return event;
    }
    const po = withResolvers();
    waiters.push(po);
    return po.promise;
  };
  const listener = (event) => {
    messages.push(event);

    const waiter = waiters.shift();
    if (waiter) {
      waiter.resolve(event);
    }
  };
  port.addEventListener("message", listener);
  port.start();
  const port_close = port.close;
  let closed = false;
  port.close = () => {
    closed = true;
    // 这里会同时关闭读取与写入，所以即便消息还没被接收完成，也意味着将会被丢弃
    port_close.call(port);
    messages.push("close for send");
    port.removeEventListener("message", listener);
    for (po of waiters) {
      if (po.is_po) {
        po.resolve("close for send");
      } else {
        break;
      }
    }
    waiters.length = 0;
  };
};
    void 0;
    """.trimIndent()
      )

      val jsWindow = mainFrame.window()
      jsWindow.call<Unit>(randomName, port)
      jsWindow.removeProperty(randomName)

      webview.ioScope.launch {
        runCatching {
          var i = 0L;
          while (true) {
            val eventOrPo = port.call<Any>("pickEvent", i)
            val event = when (eventOrPo) {
              is JsPromise -> {
                eventOrPo.await<Any>()
              }

              else -> eventOrPo
            }
            if (event is String) {
              throw JsException(event)
            } else if (event is JsObject) {
              if (handleMessageEvent(event)) {
                // 如果成功处理，光标前移
                i++
              }
              if (event != eventOrPo && eventOrPo is JsObject) {
                eventOrPo.close()
              }
            }
          }
        }.getOrElse {
          if (it.message?.contains("close for send") == true) {
            messageChannel.close()
          } else {
            messageChannel.close(it)
          }
        }
      }
    }

    messageChannel
  }

  override suspend fun start() {
    _started.value
  }

  private var closed = false
  override suspend fun close(cause: CancellationException?) {
    if (!closed) {
      closed = true
//      if (_started.isInitialized()) {
//        _started.value.close(cause)
//      }
      port.call<Unit>("close")
    }
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
    /// jsObject 可能已经被释放了

  }

  override val onMessage by lazy {
    _started.value
  }
}
