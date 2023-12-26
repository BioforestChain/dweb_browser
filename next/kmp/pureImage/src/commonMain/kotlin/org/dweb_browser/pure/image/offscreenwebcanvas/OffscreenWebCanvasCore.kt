package org.dweb_browser.pure.image.offscreenwebcanvas

import kotlinx.coroutines.CompletableDeferred
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dweb_browser.helper.SafeInt

internal class OffscreenWebCanvasCore {

  val channel = OffscreenWebCanvasMessageChannel()
  private var ridAcc by SafeInt(1)

  suspend fun runJsCodeWithResult(
    returnType: ReturnType, jsCode: String,
  ): Any? {
    val rid = ridAcc++
    val evalResult = CompletableDeferred<Any?>()
    val resultPrefix = "$rid:"
    val errorPrefix = "throw:"
    val returnVoidPrefix = "void"
    val returnAnyPrefix = "return:"
    var waitingReturn = false
    val off = channel.onMessage {
      if (waitingReturn) {
        evalResult.complete(it.text ?: it.binary)
        return@onMessage
      }
      if (it.text?.startsWith(resultPrefix) != true) {
        return@onMessage
      }
      val resultMetadata = it.text.substring(resultPrefix.length)
      if (resultMetadata.startsWith(errorPrefix)) {
        evalResult.completeExceptionally(Throwable(resultMetadata.substring(errorPrefix.length)))
      } else if (resultMetadata == returnVoidPrefix) {
        evalResult.complete(Unit)
      } else if (resultMetadata == returnAnyPrefix) {
        waitingReturn = true
      } else {
        evalResult.completeExceptionally(Throwable("Invalid response: $resultMetadata"))
      }
    }
    channel.postMessage(Json.encodeToString(RunCommandReq(rid, returnType, jsCode)))
    return evalResult.await().also {
      off()
    }
  }
}