package org.dweb_browser.helper.platform.offscreenwebcanvas

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CompletableDeferred
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal class OffscreenWebCanvasCore {

  val channel = OffscreenWebCanvasMessageChannel()
  private val ridAcc = atomic(0)

  suspend fun runJsCodeWithResult(
    resultVoid: Boolean,
    jsonIfyResult: Boolean, jsCode: String,
  ): RunCommandResult {
    val rid = ridAcc.incrementAndGet()
    val evalResult = CompletableDeferred<RunCommandResult>()
    val off = channel.onMessage {
      if (!it.data.contains(""""rid":$rid""")) {
        return@onMessage
      }
      try {
        val runResult = Json.decodeFromString<RunCommandResult>(it.data)
        println("got message:${runResult}")
        if (runResult.rid == rid) {
          evalResult.complete(runResult)
        }
      } catch (e: Throwable) {
        evalResult.completeExceptionally(e)
        e.printStackTrace()
      }
    }
    channel.postMessage(Json.encodeToString(RunCommandReq(rid, resultVoid, jsonIfyResult, jsCode)))
    return evalResult.await().also {
      off()
    }
  }
}