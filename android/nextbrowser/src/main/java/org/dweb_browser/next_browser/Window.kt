package org.dweb_browser.next_browser

import android.content.Context
import android.webkit.MimeTypeMap
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import com.google.accompanist.web.AccompanistWebChromeClient
import com.google.accompanist.web.AccompanistWebViewClient
import com.google.accompanist.web.WebContent
import com.google.accompanist.web.WebViewNavigator
import com.google.accompanist.web.WebViewState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream

class Window(context: Context, url: String, coroutineScope: CoroutineScope) {
  val state = WebViewState(
    WebContent.Url(
      url = url, additionalHttpHeaders = emptyMap()
    )
  )
  val pageIconHref = mutableStateOf("")
  val navigator = WebViewNavigator(coroutineScope)
  val webChromeClient = object : AccompanistWebChromeClient() {
  }
  val webViewClient = object : AccompanistWebViewClient() {
  }


  val webView = runBlocking {
    withContext(Dispatchers.Main) {

      object : WebView(context) {
        init {
          settings.allowFileAccess = true
          settings.allowContentAccess = true
          webChromeClient = this@Window.webChromeClient
          webViewClient = this@Window.webViewClient
        }

        private var _curUrl: String = ""
        override fun loadUrl(url: String, additionalHttpHeaders: Map<String, String>) {
          if (_curUrl != url) {
            _curUrl = url
            super.loadUrl(url, additionalHttpHeaders)
          }
        }
      }
    }
  }

  val x = mutableFloatStateOf(20f);
  val y = mutableFloatStateOf(60f);
  val width = mutableFloatStateOf(100f);
  val height = mutableFloatStateOf(200f);
  val round = mutableFloatStateOf(0f);
  val visible = mutableStateOf(true);
  val zIndex = mutableIntStateOf(1);
}


