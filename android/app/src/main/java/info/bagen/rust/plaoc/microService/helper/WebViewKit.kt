package info.bagen.rust.plaoc.microService.helper

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

typealias AsyncChannel = Channel<Result<String>>;

/**
 * 这个类的使用，务必在 Main 线程中
 */
class WebViewAsyncEvalContext(
    val webView: WebView,
) {
    companion object {
        private var idAcc = 0
        const val JS_ASYNC_KIT = "native_async_callback_kit"
    }

    private val channelMap = mutableMapOf<Int, AsyncChannel>()

    @SuppressLint("JavascriptInterface")
    private fun doInitInterface() {
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
        doInitInterface()
    }

    suspend fun evaluateJavascriptSync(script: String) =
        withContext(Dispatchers.Main) {
            val po = PromiseOut<String>()
            webView.evaluateJavascript(script) {
                po.resolve(it)
            }
            po.waitPromise()
        }

    suspend fun evaluateJavascriptAsync(
        script: String,
        afterEval: suspend () -> Unit = {}
    ): String {

        val channel: AsyncChannel = Channel()
        val id = idAcc++
        channelMap[id] = channel
        GlobalScope.launch(Dispatchers.Main) {
            webView.evaluateJavascript(
                """
            void (async()=>{return ($script)})()
                .then(res=>$JS_ASYNC_KIT.resolve($id,JSON.stringify(res)))
                .catch(err=>$JS_ASYNC_KIT.reject($id,String(err)));
            """.trimMargin()
            ) {
            };
            afterEval()
        }
        return channel.receive().getOrThrow()
    }

}