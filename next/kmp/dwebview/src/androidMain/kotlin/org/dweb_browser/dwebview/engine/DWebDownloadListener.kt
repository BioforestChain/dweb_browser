package org.dweb_browser.dwebview.engine

import android.webkit.DownloadListener
import io.ktor.http.ContentType
import io.ktor.http.Url
import io.ktor.http.fileExtensions
import kotlinx.coroutines.launch
import org.dweb_browser.dwebview.WebDownloadArgs
import org.dweb_browser.dwebview.debugDWebView
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.randomUUID

class DWebDownloadListener(val engine: DWebViewEngine) : DownloadListener {
  private val scope get() = engine.lifecycleScope
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
    contentDisposition: String?,
    mimetype: String,
    contentLength: Long
  ) {
    val suggestedFilename = contentDisposition?.substringAfter("filename=")?.ifEmpty { null }
    // 否则使用链接的最后一部分作为文件名
      ?: Url(url).segments.lastOrNull()
      // 否则使用随机文件名
      ?: (randomUUID() + (ContentType.parse(mimetype).fileExtensions().firstOrNull() ?: ""))

    scope.launch {
      downloadSignal.emit(
        WebDownloadArgs(
          userAgent, suggestedFilename, mimetype, contentLength, url
        )
      )
    }
    inners("onDownloadStart").forEach {
      it.onDownloadStart(url, userAgent, contentDisposition, mimetype, contentLength)
    }
  }
}