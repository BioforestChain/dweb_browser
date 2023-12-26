package org.dweb_browser.pure.image.offscreenwebcanvas

internal suspend fun OffscreenWebCanvasCore.evalJavaScriptReturnString(jsCode: String) =
  runJsCodeWithResult(ReturnType.string, jsCode) as String

internal suspend fun OffscreenWebCanvasCore.evalJavaScriptReturnVoid(jsCode: String) =
  runJsCodeWithResult(ReturnType.void, jsCode) as Unit

internal suspend fun OffscreenWebCanvasCore.evalJavaScriptReturnByteArray(jsCode: String) =
  runJsCodeWithResult(ReturnType.binary, jsCode) as ByteArray

internal suspend fun OffscreenWebCanvasCore.evalJavaScriptReturnJson(jsCode: String) =
  runJsCodeWithResult(ReturnType.json, jsCode) as String