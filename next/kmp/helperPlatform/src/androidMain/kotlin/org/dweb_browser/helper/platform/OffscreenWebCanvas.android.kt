package org.dweb_browser.helper.platform

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dweb_browser.helper.JsonLoose
import org.dweb_browser.helper.mainAsyncExceptionHandler
import org.dweb_browser.helper.platform.offscreenwebcanvas.OffscreenWebCanvasMessageChannel
import org.dweb_browser.helper.platform.offscreenwebcanvas.RunCommandReq
import org.dweb_browser.helper.platform.offscreenwebcanvas.RunCommandResult

actual class OffscreenWebCanvas private actual constructor(width: Int, height: Int) {
  companion object {
    init {
      WebView.setWebContentsDebuggingEnabled(true)
    }
  }

  lateinit var webview: WebView

  @SuppressLint("SetJavaScriptEnabled")
  constructor(context: Context, width: Int = 128, height: Int = 128) : this(width, height) {
    webview = WebView(context)
    CoroutineScope(mainAsyncExceptionHandler).launch {
      webview.webViewClient = WebViewClient();
      webview.settings.javaScriptEnabled = true;
      webview.loadUrl(channel.getEntryUrl(width, height))
    }
  }

  private val channel = OffscreenWebCanvasMessageChannel()
  private val ridAcc = atomic(0)
  private var cacheWidth = width
  private var cacheHeight = height

  internal actual suspend fun runJsCodeWithResult(
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
        val runResult = JsonLoose.decodeFromString<RunCommandResult>(it.data)
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

  actual val width: Int
    get() = cacheWidth
  actual val height: Int
    get() = cacheHeight

}