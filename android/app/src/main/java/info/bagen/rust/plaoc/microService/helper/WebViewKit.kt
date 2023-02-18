package info.bagen.rust.plaoc.microService.helper

import android.annotation.SuppressLint
import android.webkit.WebView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import okhttp3.internal.notify
import okhttp3.internal.wait

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
        val lock = Mutex()
        GlobalScope.launch(Dispatchers.Main) {
            webView.addJavascriptInterface(object {
                fun resolve(id: Int, data: String) {
                    runBlocking {
                        channelMap.remove(id)?.also {
                            it.send(Result.success(data))
                            it.close()
                        }
                    }
                }

                fun reject(id: Int, reason: String) {
                    runBlocking {
                        channelMap.remove(id)?.also {
                            it.send(Result.failure(Exception(reason)))
                            it.close()
                        }
                    }
                }
            }, "__native_async_callback_kit__")
            lock.notify()
        }
        lock.wait()
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
                .then(res=>__native_async_callback_kit__.resolve($id,JSON.stringify(res))
                .catch(err=>__native_async_callback_kit__.reject($id,String(err)));
            """.trimMargin()
            ) {
            };
        }
        return channel.receive().getOrThrow()
    }

}