package info.bagen.rust.plaoc.microService.sys.mwebview

import androidx.compose.runtime.mutableStateListOf
import com.google.accompanist.web.WebContent
import com.google.accompanist.web.WebViewNavigator
import com.google.accompanist.web.WebViewState
import info.bagen.rust.plaoc.App
import info.bagen.rust.plaoc.microService.core.MicroModule
import info.bagen.rust.plaoc.microService.helper.*
import info.bagen.rust.plaoc.microService.webview.DWebView
import kotlinx.coroutines.*

class MutilWebViewController(
    val mmid: Mmid,
    val localeMM: MicroModule,
    val remoteMM: MicroModule,
) {

    companion object {
        private var webviewId_acc = 1
    }

    val webViewList = mutableStateListOf<ViewItem>()

    data class ViewItem(
        val webviewId: String,
        val webView: DWebView,
        val state: WebViewState,
        val navigator: WebViewNavigator,
        val coroutineScope: CoroutineScope,
        var hidden: Boolean = false
    )

    private var activityTask = PromiseOut<MutilWebViewActivity>()
    suspend fun waitActivityCreated() = activityTask.waitPromise()

    var activity: MutilWebViewActivity? = null
        set(value) {
            if (field == value) {
                return
            }
            field = value
            for (webview in webViewList) {
                webview.webView.activity = value
            }
            if (value == null) {
                activityTask = PromiseOut()
            } else {
                activityTask.resolve(value)
            }
        }


    /**
     * 打开WebView
     */
    suspend fun openWebView(url: String) = appendWebViewAsItem(createDwebView(url))

//    private val openDwebViewLock = Mutex()

    suspend fun createDwebView(url: String): DWebView = withContext(mainAsyncExceptionHandler) {
        val currentActivity = activity ?: App.appContext
        val dWebView = DWebView(
            currentActivity, localeMM, remoteMM, DWebView.Options(
                url = url,
                /// 我们会完全控制页面将如何离开，所以这里兜底默认为留在页面
                onDetachedFromWindowStrategy = DWebView.Options.DetachedFromWindowStrategy.Ignore,
            ), activity
        )
//        dWebView.onOpen { message ->
//            openDwebViewLock.withLock {
//
//                debugMultiWebView("opening")
//                /**
//                 * 此时url有webview内部在管理
//                 */
//                val subDwebView = createDwebView(localeMM, remoteMM, "")
//                val transport = message.obj;
//                if (transport is WebView.WebViewTransport) {
//                    transport.webView = subDwebView;
//                    message.sendToTarget();
//                    // 它是有内部链接的，所以等到它ok了再说
//                    val url = subDwebView.getUrlInMain()
//                    if (url?.isEmpty() != true) {
//                        val readyPo = PromiseOut<Unit>()
//                        subDwebView.onReady { readyPo.resolve(Unit) }
//                        readyPo.waitPromise()
//                    }
//
//                    debugMultiWebView("opened", subDwebView.getUrlInMain())
//                    appendWebViewAsItem(subDwebView)
//                }
//            }
//
//        }
        dWebView
    }


    @Synchronized
    fun appendWebViewAsItem(dWebView: DWebView) = runBlockingCatching(Dispatchers.Main) {
        val webviewId = "#w${webviewId_acc++}"
        val state = WebViewState(WebContent.Url(dWebView.url ?: ""))
        val coroutineScope = CoroutineScope(CoroutineName(webviewId))
        val navigator = WebViewNavigator(coroutineScope)
        ViewItem(
            webviewId = webviewId,
            webView = dWebView,
            state = state,
            coroutineScope = coroutineScope,
            navigator = navigator
        ).also {
            webViewList.add(it)
            dWebView.onCloseWindow {
                closeWebView(webviewId)
            }
            it.coroutineScope.launch {
                webViewOpenSignal.emit(webviewId)
            }
        }
    }.getOrThrow()


    fun getCurrentWebView(): ViewItem {
        return webViewList.last()
    }

    /**
     * 关闭WebView
     */
    @Synchronized
    fun closeWebView(webviewId: String): Boolean {
        return webViewList.removeIf {
            if (it.webviewId == webviewId) {
                it.webView.destroy()
                it.coroutineScope.launch {
                    webViewCloseSignal.emit(webviewId)
                }
                true
            } else {
                false
            }
        }
    }

    /**
     * 将指定WebView移动到顶部显示
     */
    fun moveToTopWebView(webviewId: String): Boolean {
        val viewItem = webViewList.find { it.webviewId == webviewId } ?: return false
        webViewList.remove(viewItem)
        webViewList.add(viewItem)
        return true
    }

    private val webViewCloseSignal = Signal<String>()
    private val webViewOpenSignal = Signal<String>()
    fun onWebViewClose(cb: Callback<String>) = webViewCloseSignal.listen(cb)
    fun onWebViewOpen(cb: Callback<String>) = webViewOpenSignal.listen(cb)
}