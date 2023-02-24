package info.bagen.rust.plaoc.microService.helper

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

typealias AsyncChannel = Channel<Result<String>>;

class WebViewAsyncEvalContext(
    val webView: WebView,
) {
    companion object {
        private var idAcc = 0
    }

    private val channelMap = mutableMapOf<Int, AsyncChannel>()

    @SuppressLint("JavascriptInterface")
    private val doInitInterface = suspendOnce {
        val lock = Mutex(true)
        GlobalScope.launch(Dispatchers.Main) {
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
            }, "__native_async_callback_kit__")
            lock.unlock()
        }
        lock.lock()
    }

    suspend fun evaluateJavascriptAsync(script: String): String {
        doInitInterface()
        val channel: AsyncChannel = Channel()
        val id = idAcc++
        channelMap[id] = channel
        GlobalScope.launch(Dispatchers.Main) {
            webView.evaluateJavascript(
                """
            void (async()=>{return ($script)})()
                .then(res=>__native_async_callback_kit__.resolve($id,JSON.stringify(res)))
                .catch(err=>__native_async_callback_kit__.reject($id,String(err)));
            """.trimMargin()
            ) {
            };
        }
        return channel.receive().getOrThrow()
    }

}