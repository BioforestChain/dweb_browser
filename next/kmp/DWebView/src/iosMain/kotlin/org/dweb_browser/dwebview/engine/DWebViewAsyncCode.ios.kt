package org.dweb_browser.dwebview.engine

import kotlinx.coroutines.launch
import platform.Foundation.NSArray
import platform.Foundation.NSNumber
import platform.WebKit.WKScriptMessage
import platform.WebKit.WKScriptMessageHandlerProtocol
import platform.WebKit.WKUserContentController
import platform.darwin.NSObject

class DWebViewAsyncCode(private val engine: DWebViewEngine) : NSObject(),
  WKScriptMessageHandlerProtocol {

  internal val asyncCodePrepareCode = """
      ${WebViewEvaluator.JS_ASYNC_KIT} = {
          resolve(id,res){
              webkit.messageHandlers.asyncCode.postMessage([1,id,res])
          },
          reject(id,err){
              console.error(err);
              webkit.messageHandlers.asyncCode.postMessage([0,id,"QQQQ:"+(err instanceof Error?(err.message+"\n"+err.stack):String(err))])
          }
      };
      void 0;
    """.trimMargin()

  override fun userContentController(
    userContentController: WKUserContentController,
    didReceiveScriptMessage: WKScriptMessage
  ) {
    val message = didReceiveScriptMessage.body as NSArray
    val isSuccess = (message.objectAtIndex(0u) as NSNumber).boolValue
    val id = (message.objectAtIndex(1u) as NSNumber).intValue
    val channel = engine.evaluator.channelMap.remove(id)

    if (channel != null) {
      val result = message.objectAtIndex(2u) as String
      if (isSuccess) {
        engine.mainScope.launch {
          channel.send(Result.success(result))
        }
      } else {
        engine.mainScope.launch {
          channel.send(Result.failure(Throwable(result)))
        }
      }
    }
  }
}