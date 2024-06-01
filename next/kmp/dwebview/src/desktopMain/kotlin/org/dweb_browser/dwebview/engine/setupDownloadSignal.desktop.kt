package org.dweb_browser.dwebview.engine

import com.teamdev.jxbrowser.browser.callback.StartDownloadCallback
import kotlinx.coroutines.launch
import org.dweb_browser.dwebview.WebDownloadArgs
import org.dweb_browser.helper.Signal

fun setupDownloadSignal(engine: DWebViewEngine) = Signal<WebDownloadArgs>().also { signal ->
  engine.browser.set(StartDownloadCallback::class.java, StartDownloadCallback { params, tell ->
    if (signal.isNotEmpty()) {
      engine.lifecycleScope.launch {
        val downloadTarget = params.download().target()
        signal.emit(
          WebDownloadArgs(
            userAgent = engine.browser.userAgent(),
            suggestedFilename = downloadTarget.suggestedFileName(),
            mimetype = downloadTarget.mimeType().value(),
            contentLength = null,
            url = downloadTarget.url()
          )
        )
      }
    }
    tell.cancel()
  })
}