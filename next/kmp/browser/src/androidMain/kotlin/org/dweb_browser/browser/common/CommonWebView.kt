package org.dweb_browser.browser.common

import android.annotation.SuppressLint
import android.os.Build
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import com.google.accompanist.web.AccompanistWebChromeClient
import com.google.accompanist.web.AccompanistWebViewClient
import com.google.accompanist.web.LoadingState
import com.google.accompanist.web.WebContent
import com.google.accompanist.web.WebView
import com.google.accompanist.web.WebViewState
import org.dweb_browser.browser.web.model.ConstUrl
import org.dweb_browser.browser.common.loading.LoadingView
import org.dweb_browser.helper.compose.LocalCommonUrl
import org.dweb_browser.helper.compose.clickableWithNoEffect

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CommonWebView() {
  val localPrivacy = LocalCommonUrl.current
  if (localPrivacy.value.isNotEmpty()) {
    BackHandler { localPrivacy.value = "" }
    val state = remember { WebViewState(WebContent.Url(localPrivacy.value)) }

    val webViewClient = remember {
      object : AccompanistWebViewClient() {
        override fun onReceivedError(
          view: WebView,
          request: WebResourceRequest?,
          error: WebResourceError?
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
          view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?
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
              if (it.contains("404") || it.contains("500") || it.contains("Error") ||
                it.contains("找不到网页") || it.contains("网页无法打开")
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
    Box(
      modifier = Modifier
        .clickableWithNoEffect { }
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
    ) {
      val background = MaterialTheme.colorScheme.background
      WebView(
        state = state,
        modifier = Modifier
          .fillMaxSize()
          .background(background), // TODO 为了避免暗模式突然闪一下白屏
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
        }
      )
      LoadingView(showLoading)
    }
  }
}