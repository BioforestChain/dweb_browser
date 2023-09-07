package org.dweb_browser.helper.platform

import org.dweb_browser.helper.platform.offscreenwebcanvas.RunCommandResult


expect class OffscreenWebCanvas private constructor(width: Int, height: Int) {

  internal suspend fun runJsCodeWithResult(
    resultVoid: Boolean,
    jsonIfyResult: Boolean, jsCode: String,
  ): RunCommandResult

  val width: Int
  val height: Int
}


