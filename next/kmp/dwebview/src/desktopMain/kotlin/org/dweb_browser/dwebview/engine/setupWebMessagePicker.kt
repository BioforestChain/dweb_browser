package org.dweb_browser.dwebview.engine

import com.teamdev.jxbrowser.js.JsObject
import com.teamdev.jxbrowser.js.JsPromise
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import org.dweb_browser.dwebview.polyfill.DwebViewDesktopPolyfill
import org.dweb_browser.helper.WARNING

class WebMessagePortPicker(val port: JsObject, val engine: DWebViewEngine) {

  private var retry = 0
  private var warned = false

  /**
   * 事件游标
   */
  private var i = 0L

  init {
    engine.mainFrame.jsWindow().call<Unit>("__pick_web_message_port__", port)
  }

  /**
   * 收集所有数据，直到返回结束信号，或者异常抛出
   */
  suspend fun collect(handleMessageEvent: (JsObject) -> Boolean): String {
    val ctx = currentCoroutineContext()
    while (ctx.isActive) {
      runCatching {
        val eventOrPo = port.call<Any>("pickEvent", i)
        retry = 0
        warned = false
        runCatching {
          val event = when (eventOrPo) {
            is JsPromise -> {
              eventOrPo.await<Any>()
            }

            else -> eventOrPo
          }
          if (event is String) {
            return event
          } else if (event is JsObject) {
            if (handleMessageEvent(event)) {
              // 如果成功处理，光标前移
              i++
            }
            /// release jsObject
            if (eventOrPo is JsObject) eventOrPo.close()
            if (event != eventOrPo) event.close()
          }
        }.getOrElse {
          if (eventOrPo is JsObject) {
            eventOrPo.close()
          }
        }
      }.getOrElse {
        retry++
        if (retry > 100 && !warned) {
          warned = true
          WARNING("fail to call pickEvent, retry over $retry times.")
          // throw JsException("fail to call pickEvent, retry over $retry times.")
        }
      }
    }
    return "abort collect"
  }
}

fun setupWebMessagePicker(engine: DWebViewEngine) = { port: JsObject ->
  WebMessagePortPicker(port, engine)
}.also {
  engine.addDocumentStartJavaScript(DwebViewDesktopPolyfill.WebMessagePort)
}