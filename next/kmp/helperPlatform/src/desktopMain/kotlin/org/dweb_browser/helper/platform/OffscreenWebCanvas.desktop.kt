package org.dweb_browser.helper.platform

import org.dweb_browser.helper.platform.offscreenwebcanvas.RunCommandResult

actual class OffscreenWebCanvas actual constructor(width: Int, height: Int) {


  actual val width: Int
    get() = TODO("Not yet implemented")
  actual val height: Int
    get() = TODO("Not yet implemented")

  internal actual suspend fun runJsCodeWithResult(
    resultVoid: Boolean,
    jsonIfyResult: Boolean,
    jsCode: String
  ): RunCommandResult {
    TODO("Not yet implemented")
  }

}