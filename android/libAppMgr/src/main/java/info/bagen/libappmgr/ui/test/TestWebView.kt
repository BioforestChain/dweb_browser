package info.bagen.libappmgr.ui.view

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import info.bagen.libappmgr.ui.test.TestViewModel
import kotlinx.coroutines.launch

@Composable
fun TestWebView(
    viewModel: TestViewModel,
    //自己处理返回事件
    onBack: (webView: WebView?) -> Unit,
    //自己选择是否接收，网页地址加载进度回调
    onProgressChange: (progress: Int) -> Unit = {},
    //自己选择是否设置自己的WebSettings配置
    initSettings: (webSettings: WebSettings?) -> Unit = {},
    //自己选择是否处理onReceivedError回调事件
    onReceivedError: (error: WebResourceError?) -> Unit = {}
) {
    var webView: WebView? = null
    val coroutineScope = rememberCoroutineScope()
    if (viewModel.showWebView.value) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            onProgressChange(-1)
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            onProgressChange(100)
                            // viewModel.showWebView.value = true
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            if (null == request?.url) return false
                            val showOverrideUrl = request.url.toString()
                            try {
                                if (!showOverrideUrl.startsWith("http://")
                                    && !showOverrideUrl.startsWith("https://")
                                ) {
                                    //处理非http和https开头的链接地址
                                    Intent(Intent.ACTION_VIEW, Uri.parse(showOverrideUrl)).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        view?.context?.applicationContext?.startActivity(this)
                                    }
                                    return true
                                }
                            } catch (e: Exception) {
                                //没有安装和找到能打开(「xxxx://openlink.cc....」、「weixin://xxxxx」等)协议的应用
                                return true
                            }
                            return super.shouldOverrideUrlLoading(view, request)
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            super.onReceivedError(view, request, error)
                            onReceivedError(error)
                        }
                    }
                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            super.onProgressChanged(view, newProgress)
                            onProgressChange(newProgress)
                        }
                    }

                    initSettings(this.settings)
                    webView = this
                    loadUrl(viewModel.url.value)
                }
            },
            update = {
                it.loadUrl(viewModel.url.value)
                viewModel.bottomNavController.forEach { navController ->
                    navController.clickable.value = true
                }
            })
        BackHandler {
            coroutineScope.launch {
                //自行控制点击了返回按键之后，关闭页面还是返回上一级网页
                onBack(webView)
            }
        }
    } else {
        if (webView != null) {
            webView?.loadUrl("")
        }
    }
}
