package org.dweb_browser.browser.common


import android.content.Context
import android.os.Bundle
import android.view.ViewGroup.LayoutParams
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.dweb_browser.common.webview.AccompanistWebChromeClient
import org.dweb_browser.common.webview.AccompanistWebViewClient
import org.dweb_browser.common.webview.WebContent
import org.dweb_browser.common.webview.WebViewNavigator
import org.dweb_browser.common.webview.WebViewState
import org.dweb_browser.common.webview.rememberWebViewNavigator

/**
 * A wrapper around the Android View WebView to provide a basic WebView composable.
 *
 * If you require more customisation you are most likely better rolling your own and using this
 * wrapper as an example.
 *
 * The WebView attempts to set the layoutParams based on the Compose modifier passed in. If it
 * is incorrectly sizing, use the layoutParams composable function instead.
 *
 * @param state The webview state holder where the Uri to load is defined.
 * @param modifier A compose modifier
 * @param captureBackPresses Set to true to have this Composable capture back presses and navigate
 * the WebView back.
 * @param navigator An optional navigator object that can be used to control the WebView's
 * navigation from outside the composable.
 * @param onCreated Called when the WebView is first created, this can be used to set additional
 * settings on the WebView. WebChromeClient and WebViewClient should not be set here as they will be
 * subsequently overwritten after this lambda is called.
 * @param onDispose Called when the WebView is destroyed. Provides a bundle which can be saved
 * if you need to save and restore state in this WebView.
 * @param client Provides access to WebViewClient via subclassing
 * @param chromeClient Provides access to WebChromeClient via subclassing
 * @param factory An optional WebView factory for using a custom subclass of WebView
 * @sample com.google.accompanist.sample.webview.BasicWebViewSample
 */
@Composable
public fun AccompanistWebView(
  state: WebViewState,
  modifier: Modifier = Modifier,
  captureBackPresses: Boolean = true,
  navigator: WebViewNavigator = rememberWebViewNavigator(),
  onCreated: (WebView) -> Unit = {},
  onDispose: (WebView) -> Unit = {},
  client: AccompanistWebViewClient = remember { AccompanistWebViewClient() },
  chromeClient: AccompanistWebChromeClient = remember { AccompanistWebChromeClient() },
  factory: ((Context) -> WebView)? = null,
) {
  BoxWithConstraints(modifier) {
    // WebView changes it's layout strategy based on
    // it's layoutParams. We convert from Compose Modifier to
    // layout params here.
    val width = if (constraints.hasFixedWidth) LayoutParams.MATCH_PARENT
    else LayoutParams.WRAP_CONTENT
    val height = if (constraints.hasFixedHeight) LayoutParams.MATCH_PARENT
    else LayoutParams.WRAP_CONTENT

    val layoutParams = FrameLayout.LayoutParams(
      width,
      height,
    )

    AccompanistWebView(
      state,
      layoutParams,
      Modifier,
      captureBackPresses,
      navigator,
      onCreated,
      onDispose,
      client,
      chromeClient,
      factory
    )
  }
}

/**
 * A wrapper around the Android View WebView to provide a basic WebView composable.
 *
 * If you require more customisation you are most likely better rolling your own and using this
 * wrapper as an example.
 *
 * The WebView attempts to set the layoutParams based on the Compose modifier passed in. If it
 * is incorrectly sizing, use the layoutParams composable function instead.
 *
 * @param state The webview state holder where the Uri to load is defined.
 * @param layoutParams A FrameLayout.LayoutParams object to custom size the underlying WebView.
 * @param modifier A compose modifier
 * @param captureBackPresses Set to true to have this Composable capture back presses and navigate
 * the WebView back.
 * @param navigator An optional navigator object that can be used to control the WebView's
 * navigation from outside the composable.
 * @param onCreated Called when the WebView is first created, this can be used to set additional
 * settings on the WebView. WebChromeClient and WebViewClient should not be set here as they will be
 * subsequently overwritten after this lambda is called.
 * @param onDispose Called when the WebView is destroyed. Provides a bundle which can be saved
 * if you need to save and restore state in this WebView.
 * @param client Provides access to WebViewClient via subclassing
 * @param chromeClient Provides access to WebChromeClient via subclassing
 * @param factory An optional WebView factory for using a custom subclass of WebView
 */
@Composable
public fun AccompanistWebView(
  state: WebViewState,
  layoutParams: FrameLayout.LayoutParams,
  modifier: Modifier = Modifier,
  captureBackPresses: Boolean = true,
  navigator: WebViewNavigator = rememberWebViewNavigator(),
  onCreated: (WebView) -> Unit = {},
  onDispose: (WebView) -> Unit = {},
  client: AccompanistWebViewClient = remember { AccompanistWebViewClient() },
  chromeClient: AccompanistWebChromeClient = remember { AccompanistWebChromeClient() },
  factory: ((Context) -> WebView)? = null,
) {
  val webView = state.webView

  BackHandler(captureBackPresses && navigator.canGoBack) {
    webView?.goBack()
  }

  webView?.let { wv ->
    LaunchedEffect(wv, navigator) {
      with(navigator) {
        wv.handleNavigationEvents()
      }
    }

    LaunchedEffect(wv, state) {
      snapshotFlow { state.content }.collect { content ->
        when (content) {
          is WebContent.Url -> {
            wv.loadUrl(content.url, content.additionalHttpHeaders)
          }

          is WebContent.Data -> {
            wv.loadDataWithBaseURL(
              content.baseUrl, content.data, content.mimeType, content.encoding, content.historyUrl
            )
          }

          is WebContent.Post -> {
            wv.postUrl(
              content.url, content.postData
            )
          }

          is WebContent.NavigatorOnly -> {
            // NO-OP
          }
        }
      }
    }
  }

  // Set the state of the client and chrome client
  // This is done internally to ensure they always are the same instance as the
  // parent Web composable
  client.state = state
  client.navigator = navigator
  chromeClient.state = state

  AndroidView(
    factory = { context ->
      (factory?.invoke(context) ?: WebView(context)).also { webview ->
        onCreated(webview)
        webview.layoutParams = layoutParams
        state.viewState?.also { viewState ->
          webview.restoreState(viewState)
          state.viewState = null
        }
        webview.webChromeClient = chromeClient
        webview.webViewClient = client
      }.also { state.webView = it }
    },
    modifier = modifier,
    update = { webview ->
      state.viewState?.also { viewState ->
        webview.restoreState(viewState)
        state.viewState = null
      }
    },
    onRelease = { webview ->
      Bundle().also { viewState ->
        webview.saveState(viewState)
        state.viewState = viewState
      }
      onDispose(webview)
    },
  )
}
