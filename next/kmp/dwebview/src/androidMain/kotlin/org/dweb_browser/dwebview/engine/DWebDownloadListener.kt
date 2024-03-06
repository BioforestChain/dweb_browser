package org.dweb_browser.dwebview.engine

import android.webkit.DownloadListener
import kotlinx.coroutines.launch
import org.dweb_browser.dwebview.WebDownloadArgs
import org.dweb_browser.dwebview.debugDWebView
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.one

class DWebDownloadListener(val engine: DWebViewEngine) : DownloadListener {
  private val scope get() = engine.ioScope
  private val extends = Extends<DownloadListener>()
  fun addDownloadListener(
    listener: DownloadListener, config: Extends.Config = Extends.Config()
  ) = extends.add(listener, config)

  fun removeDownloadListener(listener: DownloadListener) = extends.remove(listener)

  private fun inners(methodName: String, noise: Boolean = true) =
    extends.hasMethod(methodName).also {
      if (it.isNotEmpty() && noise) {
        debugDWebView("DWebDownloadListener") { "calling method: $methodName" }
      }
    }

  internal val downloadSignal = Signal<WebDownloadArgs>()

  override fun onDownloadStart(
    url: String,
    userAgent: String,
    contentDisposition: String,
    mimetype: String,
    contentLength: Long
  ) {
    scope.launch {
      downloadSignal.emit(
        WebDownloadArgs(userAgent, contentDisposition, mimetype, contentLength, url)
      )
    }
    inners("onDownloadStart").one {
      it.onDownloadStart(url, userAgent, contentDisposition, mimetype, contentLength)
    }
  }
}