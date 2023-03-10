package info.bagen.rust.plaoc.microService.webview

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import info.bagen.rust.plaoc.microService.helper.PromiseOut
import info.bagen.rust.plaoc.microService.helper.commonAsyncExceptionHandler
import info.bagen.rust.plaoc.microService.helper.runBlockingCatching
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

typealias AsyncChannel = Channel<Result<String>>;

/**
 * 代码执行器
 * 这个类的使用，务必在 Main 线程中
 */
class WebViewEvaluator(
    val webView: WebView,
) {
    companion object {
        private var idAcc = 0
        const val JS_ASYNC_KIT = "__native_async_callback_kit__"
    }

    private val channelMap = mutableMapOf<Int, AsyncChannel>()

    @SuppressLint("JavascriptInterface")
    private fun initKit() {
        webView.addJavascriptInterface(object {
            @JavascriptInterface
            fun resolve(id: Int, data: String) {
                GlobalScope.launch {
                    channelMap.remove(id)?.also {
                        it.send(Result.success(data))
                        it.close()
                    }
                }
            }

            @JavascriptInterface
            fun reject(id: Int, reason: String) {
                GlobalScope.launch {
                    channelMap.remove(id)?.also {
                        it.send(Result.failure(Exception(reason)))
                        it.close()
                    }
                }
            }
        }, JS_ASYNC_KIT)
    }

    init {
        initKit()
    }

    /**
     * 执行同步JS代码
     */
    suspend fun evaluateSyncJavascriptCode(script: String) = withContext(Dispatchers.Main) {
        val po = PromiseOut<String>()
        webView.evaluateJavascript(script) {
            po.resolve(it)
        }
        po.waitPromise()
    }

    /**
     * 执行异步JS代码，需要传入一个表达式
     */
    suspend fun evaluateAsyncJavascriptCode(
        script: String, afterEval: suspend () -> Unit = {}
    ): String {

        val channel: AsyncChannel = Channel()
        val id = idAcc++
        channelMap[id] = channel
        GlobalScope.launch(Dispatchers.Main + commonAsyncExceptionHandler) {
            webView.evaluateJavascript(
                """
            void (async()=>{return ($script)})()
                .then(res=>$JS_ASYNC_KIT.resolve($id,JSON.stringify(res)))
                .catch(err=>$JS_ASYNC_KIT.reject($id,String(err)));
            """.trimMargin()
            ) {
                runBlockingCatching {
                    afterEval()
                }.getOrNull()
            };

        }
        return channel.receive().getOrThrow()
    }

}