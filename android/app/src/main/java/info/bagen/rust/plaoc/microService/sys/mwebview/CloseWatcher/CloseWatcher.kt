package info.bagen.rust.plaoc.microService.sys.mwebview.CloseWatcher

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import info.bagen.rust.plaoc.microService.helper.commonAsyncExceptionHandler
import info.bagen.rust.plaoc.microService.sys.mwebview.MultiWebViewController
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger


@SuppressLint("JavascriptInterface")
class CloseWatcher(
    val viewItem: MultiWebViewController.ViewItem,
) {

    companion object {
        val acc_id = AtomicInteger(1)
        const val JS_POLYFILL_KIT = "__native_close_watcher_kit__"
        const val CREATE_CLOSE_WATCHER_PREFIX = "create-close-watcher/"
    }

    val consuming = mutableSetOf<String>()


    init {
        viewItem.webView.addJavascriptInterface(object {
            /**
             * js 创建 CloseWatcher
             */
            @JavascriptInterface
            fun create(): String {
                val consumeToken = java.util.Random().nextInt().toString()
                consuming.add(consumeToken)
                viewItem.webView.evaluateJavascript(
                    """
                        open("data:text/html,dweb-internal/${CREATE_CLOSE_WATCHER_PREFIX}$consumeToken");
                        const watchers = (${JS_POLYFILL_KIT}._watchers||(${JS_POLYFILL_KIT}._watchers = new Map()));
                        let resolve;
                        const promise = new Promise(r => {
                            resolve = r
                        });
                        watchers.set("$consumeToken", (id) => {
                            watchers.set("$consumeToken", id);
                            watchers.set(id, new EventTarget());
                        });
                        """.trimIndent()
                ) {};
                return consumeToken
            }

            /**
             * js主动关闭 CloseWatcher
             */
            @JavascriptInterface
            fun tryClose(id: String) =
                watchers.find { watcher -> watcher.id == id }?.also {
                    GlobalScope.launch(commonAsyncExceptionHandler) {
                        close(it)
                    }
                }

        }, JS_POLYFILL_KIT)
    }

    private val watchers = mutableListOf<Watcher>()

    inner class Watcher {
        val id = acc_id.getAndAdd(1).toString()
        private var _destroy = AtomicBoolean(false)
        private val closeMutex = Mutex()
        suspend fun tryClose(): Boolean = closeMutex.withLock {
            if (_destroy.get()) {
                return false
            }
            val defaultPrevented = false;
            /// 目前，不实现cancel，因为没有合适的API来完美模拟实现
//           viewItem.webView.evaluateAsyncJavascriptCode(
//                """
//                new Promise((resolve,reject)=>{
//                    try{
//                        const watchers = ${JS_POLYFILL_KIT}._watchers;
//                        const watcher = watchers?.get("$id");
//                        if (watcher) {
//                            const event = new CustomEvent('cancel',{cancelable:true})
//                            watcher.dispatchEvent(event);
//                            resolve(event.defaultPrevented)
//                            watchers.delete("$id");
//                        }
//                        resolve(false)
//                    }catch(err){resolve(false)}
//                })
//                """.trimIndent()
//            ) {}.toBoolean();
            if (!defaultPrevented) {
                return destroy()
            }
            return false
        }

        fun destroy() = !_destroy.getAndSet(true)
    }

    /**
     * 申请一个 CloseWatcher
     */
    fun apply(isUserGesture: Boolean): Watcher {
        if (isUserGesture || watchers.size == 0) {
            watchers.add(Watcher())
        }
        return watchers.last()
    }

    fun resolveToken(consumeToken: String, watcher: Watcher) {
        viewItem.webView.evaluateJavascript(
            """
                ${JS_POLYFILL_KIT}._watchers?.get("$consumeToken")("${watcher.id}");
                """.trimIndent()
        ) {};
    }

    /**
     * 现在是否有 CloseWatcher 在等待被关闭
     */
    val canClose get() = watchers.size > 0

    /**
     * 关闭指定的 CloseWatcher
     */
    suspend fun close(watcher: Watcher = watchers.last()): Boolean {
        if (watcher.tryClose()) {
            return watchers.remove(watcher)
        }
        return false
    }

    /**
     * 关闭指定的 CloseWatcher
     */
    fun cancel() {

    }
}