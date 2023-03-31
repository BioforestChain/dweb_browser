package info.bagen.rust.plaoc.microService.sys.mwebview

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.MutableLiveData
import com.google.accompanist.web.WebContent
import com.google.accompanist.web.WebViewNavigator
import com.google.accompanist.web.WebViewState
import info.bagen.rust.plaoc.App
import info.bagen.rust.plaoc.microService.core.MicroModule
import info.bagen.rust.plaoc.microService.helper.*
import info.bagen.rust.plaoc.microService.ipc.Ipc
import info.bagen.rust.plaoc.microService.ipc.IpcEvent
import info.bagen.rust.plaoc.microService.sys.nativeui.NativeUiController
import info.bagen.rust.plaoc.microService.webview.DWebView
import io.ktor.util.collections.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import org.json.JSONObject
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.atomic.AtomicInteger
import kotlin.properties.Delegates

/**
 * MWebView 是为其它模块提供 GUI 的程序模块，所以这里需要传入两个模块：localeMM 与 remoteMM
 * remoteMM 只是一层抽象，未来如何需要可以通过网络成实现
 */
class MultiWebViewController(
    val mmid: Mmid,
    val localeMM: MultiWebViewNMM,
    val remoteMM: MicroModule,
) {
    companion object {
        private var webviewId_acc = AtomicInteger(1)
    }

    private var webViewList = mutableStateListOf<ViewItem>()

    @Composable
    fun eachView(action: @Composable (viewItem: ViewItem) -> Unit) =
        webViewList.forEachIndexed { _, viewItem ->
            action(viewItem)
        }

    fun isLastView(viewItem: ViewItem) = webViewList.lastOrNull() == viewItem
    fun isFistView(viewItem: ViewItem) = webViewList.firstOrNull() == viewItem
    val lastViewOrNull get() = webViewList.lastOrNull()

    private val mIpcMap = mutableMapOf<Mmid, Ipc>()

    data class MWebViewState(
        val webviewId: String,
        val isActivated: Boolean,
    )


    data class ViewItem(
        val webviewId: String,
        val webView: DWebView,
        val state: WebViewState,
        val navigator: WebViewNavigator,
        val coroutineScope: CoroutineScope,
        var hidden: Boolean = false
    ) {
        val nativeUiController by lazy {
            webView.activity?.let { NativeUiController(it) }
                ?: throw Exception("webview un attached to activity")
        }
    }

    private var activityTask = PromiseOut<MultiWebViewActivity>()
    suspend fun waitActivityCreated() = activityTask.waitPromise()

    var activity: MultiWebViewActivity? = null
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
    suspend fun openWebView(url: String) = appendWebViewAsItem(createDwebView(url)).also {
        debugMultiWebView("openWebView =>", it.webviewId)
        updateStateHook("openWebView")
    }

    suspend fun createDwebView(url: String): DWebView = withContext(mainAsyncExceptionHandler) {
        val currentActivity = activity ?: App.appContext
        val dWebView = DWebView(
            currentActivity, localeMM, remoteMM, DWebView.Options(
                url = url,
                /// 我们会完全控制页面将如何离开，所以这里兜底默认为留在页面
                onDetachedFromWindowStrategy = DWebView.Options.DetachedFromWindowStrategy.Ignore,
            ), activity
        )
        dWebView
    }

    @Synchronized
    fun appendWebViewAsItem(dWebView: DWebView) = runBlockingCatching(Dispatchers.Main) {
        val webviewId = "#w${webviewId_acc.getAndAdd(1)}"
        val state = WebViewState(WebContent.Url(dWebView.url ?: ""))
        val coroutineScope = CoroutineScope(CoroutineName(webviewId))
        val navigator = WebViewNavigator(coroutineScope)
        ViewItem(
            webviewId = webviewId,
            webView = dWebView,
            state = state,
            coroutineScope = coroutineScope,
            navigator = navigator,
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

    /**
     * 关闭WebView
     */
    suspend fun closeWebView(webviewId: String) =
        webViewList.find { it.webviewId == webviewId }?.let { viewItem ->
            debugMultiWebView("closeWebView =>", viewItem.webviewId)
            webViewList.remove(viewItem).also {
                if (it) updateStateHook("closeWebView")
            }
            withContext(Dispatchers.Main) {
                viewItem.webView.destroy()
            }
            webViewCloseSignal.emit(webviewId)
            return true
        } ?: false

    /**
     * 移除所有列表
     */
    fun destroyWebView() = webViewList.clear()

    /**
     * 将指定WebView移动到顶部显示
     */
    suspend fun moveToTopWebView(webviewId: String): Boolean {
        val viewItem = webViewList.find { it.webviewId == webviewId } ?: return false
        webViewList.remove(viewItem)
        webViewList.add(viewItem).also { add ->
            if (add) updateStateHook("moveToTopWebView")
        }
        return true
    }
    private suspend fun updateStateHook(handle: String) {
        debugMultiWebView(
            "updateStateHook $handle",
            "localeMM:${localeMM.mmid} mmid:$mmid ${webViewList.size}"
        )
        val currentState = JSONObject()
        webViewList.map {
            currentState.put(it.webviewId,gson.toJson(MWebViewState(it.webviewId, it.hidden)))
        }
        mIpcMap.getOrPut(mmid) {
            val (ipc) = localeMM.connect(mmid)
            ipc.onEvent {
                debugMultiWebView("event", "name=${it.event.name},data=${it.event.data}")
            }
            ipc
        }.also {ipc ->
            ipc.postMessage(IpcEvent.fromUtf8("state", currentState.toString()))
        }
    }


    private val webViewCloseSignal = Signal<String>()
    private val webViewOpenSignal = Signal<String>()

    fun onWebViewClose(cb: Callback<String>) = webViewCloseSignal.listen(cb)
    fun onWebViewOpen(cb: Callback<String>) = webViewOpenSignal.listen(cb)

}