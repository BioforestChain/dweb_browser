package org.dweb_browser.browser.common

import android.annotation.SuppressLint
import android.os.Build
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import org.dweb_browser.browser.common.loading.LoadingView
import org.dweb_browser.browser.web.data.ConstUrl
import org.dweb_browser.common.webview.AccompanistWebChromeClient
import org.dweb_browser.common.webview.AccompanistWebViewClient
import org.dweb_browser.common.webview.LoadingState
import org.dweb_browser.common.webview.WebContent
import org.dweb_browser.common.webview.WebViewState
import org.dweb_browser.helper.compose.NativeBackHandler


@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CommonWebView(url: String, onGoBack: () -> Unit) {
  val state = remember(url) { WebViewState(WebContent.Url(url)) }

  val webViewClient = remember {
    object : AccompanistWebViewClient() {
      override fun onReceivedError(
        view: WebView,
        request: WebResourceRequest?,
        error: WebResourceError?,
      ) {
        super.onReceivedError(view, request, error)
        // android 6.0以下执行
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
          return
        }
        // 断网或者网络连接超时
        val errorCode = error?.errorCode
        if (errorCode == ERROR_HOST_LOOKUP || errorCode == ERROR_CONNECT || errorCode == ERROR_TIMEOUT) {
          view.loadUrl(ConstUrl.BLANK.url) // 避免出现默认的错误界面
          //view!!.loadUrl(mErrorUrl) // 加载自定义错误页面
        }
      }

      override fun onReceivedHttpError(
        view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?,
      ) {
        // Log.e("SplashActivity", "PrivacyView::onReceivedHttpError $errorResponse")
        super.onReceivedHttpError(view, request, errorResponse)
        // 这个方法在 android 6.0才出现
        val statusCode = errorResponse!!.statusCode
        if (404 == statusCode || 500 == statusCode) {
          view?.loadUrl(ConstUrl.BLANK.url) // 避免出现默认的错误界面
          // view!!.loadUrl(mErrorUrl) // 加载自定义错误页面
        }
      }
    }
  }

  val webChromeClient = remember {
    object : AccompanistWebChromeClient() {
      override fun onReceivedTitle(view: WebView, title: String?) {
        super.onReceivedTitle(view, title)
        // Log.e("SplashActivity", "SplashActivity::PrivacyView::onReceivedTitle $title")
        // android 6.0 以下通过title获取判断
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
          title?.let {
            if (it.contains("404") || it.contains("500") || it.contains("Error") || it.contains("找不到网页") || it.contains(
                "网页无法打开"
              )
            ) {
              view.loadUrl(ConstUrl.BLANK.url) // 避免出现默认的错误界面
              // view!!.loadUrl(mErrorUrl) // 加载自定义错误页面
            }
          }
        }
      }
    }
  }

  val showLoading = remember { mutableStateOf(false) }
  LaunchedEffect(state) {
    snapshotFlow { state.loadingState }.collect {
      when (it) {
        is LoadingState.Loading -> showLoading.value = true
        else -> showLoading.value = false
      }
    }
  }
  NativeBackHandler {
    onGoBack()
  };
  Scaffold(modifier = Modifier.fillMaxSize(), topBar = {
    TopAppBar(title = {
      Text(state.pageTitle ?: "")
    }, navigationIcon = {
      IconButton(onClick = onGoBack) {
        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Go Back")
      }
    })
  }) { paddingValues ->
    AccompanistWebView(state = state,
      modifier = Modifier.fillMaxSize().padding(paddingValues), // TODO 为了避免暗模式突然闪一下白屏
      client = webViewClient,
      chromeClient = webChromeClient,
      factory = {
        WebView(it).also { webView ->
          webView.settings.also { settings ->
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.safeBrowsingEnabled = true
            settings.loadWithOverviewMode = true
            settings.loadsImagesAutomatically = true
            settings.setSupportMultipleWindows(true)
            settings.allowFileAccess = true
            settings.javaScriptCanOpenWindowsAutomatically = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.allowContentAccess = true
          }
        }
      })
    LoadingView(showLoading)
  }
}
