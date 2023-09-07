package org.dweb_browser.helper.platform.offscreenwebcanvas

import org.dweb_browser.helper.platform.OffscreenWebCanvas

suspend fun OffscreenWebCanvas.evalJavaScriptWithResult(
  jsCode: String,
  jsonIfyResult: Boolean = false,
): Result<String> {
  return try {
    val evalResult = runJsCodeWithResult(resultVoid = false, jsonIfyResult, jsCode)
    if (evalResult.error != null) {
      Result.failure(Throwable(evalResult.error))
    } else {
      Result.success(evalResult.success ?: "")
    }
  } catch (e: Throwable) {
    Result.failure(e)
  }
}

suspend fun OffscreenWebCanvas.evalJavaScriptWithVoid(jsCode: String): Result<Unit> {
  return try {
    val evalResult = runJsCodeWithResult(resultVoid = true, jsonIfyResult = false, jsCode)
    if (evalResult.error != null) {
      Result.failure(Throwable(evalResult.error))
    } else {
      Result.success(Unit)
    }
  } catch (e: Throwable) {
    Result.failure(e)
  }
}