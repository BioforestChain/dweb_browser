package info.bagen.rust.plaoc.microService.webview

import android.content.Context
import android.os.Message
import android.webkit.*
import info.bagen.rust.plaoc.microService.core.MicroModule
import info.bagen.rust.plaoc.microService.helper.*
import info.bagen.rust.plaoc.microService.sys.dns.nativeFetch
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Uri


/**
 * DWebView ,将 WebView 与 dweb 的 dwebHttpServer 设计进行兼容性绑定的模块
 * 该对象继承于WebView，所以需要在主线程去初始化它
 *
 * dwebHttpServer 底层提供了三个概念：
 * host/internal_origin/public_origin
 *
 * 其中 public_origin 是指标准的 http 协议链接，可以在标准的网络中被访问（包括本机的其它应用、以及本机所处的局域网），它的本质就是一个网关，所有的本机请求都会由它代理分发。
 * 而 host ，就是所谓网关分发的判定关键
 * 因此 internal_origin 是一个特殊的 http 链接协议，它非标准，只能在本应用（Dweb Browser）被特定的方法翻译后才能正常访问
 * 这个"翻译"方法的背后，本质上就是 host 这个值在其关键作用：
 * 1. 将 host 值放在 url 的 query 中: uri.query("X-Dweb-Host", HOST)
 * 2. 将 host 值放在 header 中: uri.header("X-Dweb-Host", HOST)
 * 3. 将 host 值放在 header.userAgent 中: uri.header("User-Agent", uri.header("User-Agent") + " dweb-host/$HOST")
 *
 * 以上三种方式，优先级依此降低，都可以将 host 携带给 public_origin 背后的服务让其进行网关路由，
 *
 * 比如说：
 * 1. public_origin 是 http://localhost:22600 ，它由 http.sys.dweb 模块进行监听
 * 2. 一个 mmid 为 desktop.bfs.dweb 的应用，它通过 http.sys.dweb 来获得监听网络的服务，因此得到的 host 便是 desktop.bfs.dweb:80
 * 3. 在 Android 平台中，我们将 http://desktop.bfs.dweb:80 这个链接作为 internal_origin，它并不能直接访问，但我们实现了一些方法、一些拦截，将这个请求转译成：
 *      http://localhost:22600?X-Dweb-Host=desktop.bfs.dweb%3A80
 * 4. 在看 Desktop 平台，我们将 http://desktop.bfs.dweb-80.localhost:22600 这个链接作为 internal_origin，
 *      同样使得 public_origin 可以通过 header.host 来翻译获得我们 host，只不过这个方法只在 Desktop-Dev 的 Chromium 引擎中有效，所以被我们采用，可以省去很多拦截工作
 *      未来可能为了确保全平台一致，我们就得实现相对一致的 internal_origin
 *
 * 所以说 internal_origin 是什么样的，其实并不重要，重要的是 host 能正确传递到位。
 *
 *
 * ## 糟心的局限性
 *
 * 在谈 Android/IOS 平台，因为它们的拦截接口无法拦截到 request.body，所以我们其实只能拦截 GET/HEAD 请求
 *
 * 所以我们对 userAgent 做了修改，使得开发者可以直接使用 public_origin 就能访问对应 host 的服务，
 * 但其实这是有风险的，因为所有服务都用同一个 public_origin，而 Web 是根据域名来存储数据的，因此你的数据可能会被其它应用盗取；而且 public_origin 是有可能变动的，所以你的数据可能会丢失
 * 因此开发者最好不好使用这个域名去存储东西（当然，我们会尽量使用平台接口去限制开发者使用相关的存储接口，比如禁用存储、或者修改存储的接口做数据隔离保护）
 *
 * 但在大部分情况下，我们都是使用 GET 请求，因此建议你直接使用 internal_origin 来访问网络，让请求拦截帮你做重定向请求。
 * 这样你就可以用 WebView 来打开 internal_origin，从而确保了存储服务的正确工作
 *
 * 至于 POST、PUT、DELETE 等可以携带 body 的请求，其实是需要修改 fetch、XMLHttpRequest 这些网络请求接口才能实现的。
 * 这方面 DWebView 会使用平台接口来实现响应的请求拦截，原理如上述一般：在 url 中注入 host 信息即可。
 * 某些情况可能是平台接口无法很好地覆盖的，这时候需要开发者手动进行修改，
 * DWebView 会提供基本的修改脚本，来方便开发者定制这些情况（比方说在一些 iframe、WebWorker 中，或者一些沙盒API中需要额外的定制化服务）
 */
class DWebView(context: Context, val mm: MicroModule, val options: Options) : WebView(context) {
    data class Options(
        /**
         * 初始化加载的页面
         */
        val loadUrl: String
    )

    private var readyPo = PromiseOut<Boolean>()

    suspend fun afterReady() = readyPo.waitPromise()

    private val evaluator = WebViewEvaluator(this)

    /**
     * 初始化设置 userAgent
     */
    private inline fun setUA() {
        val baseUserAgentString = settings.userAgentString
        val baseDwebHost = mm.mmid
        var dwebHost = baseDwebHost
        // 初始化设置 ua，这个是无法动态修改的
        val uri = Uri.of(options.loadUrl)
        if (uri.scheme == "http" && uri.host.endsWith(".dweb")) {
            dwebHost = uri.authority
        }
        // 加入默认端口
        if (!dwebHost.contains(":")) {
            dwebHost += ":80"
        }
        settings.userAgentString = "$baseUserAgentString dweb-host/${dwebHost}"
    }

    private inline fun hookWindowClose() {
        addJavascriptInterface(object {
            @JavascriptInterface
            fun close() {
                println("CCCCCLOSE!!")
            }

            @JavascriptInterface
            fun close2() {
                println("CCCCCLOSE!!")
            }
        }, "__hook_window__")
    }

    private val openSignal = Signal<Message>()
    fun onOpen(cb: Callback<Message>) = openSignal.listen(cb)
    private val closeSignal = SimpleSignal()
    fun onClose(cb: SimpleCallback) = closeSignal.listen(cb)

    init {

        setUA()
        hookWindowClose()
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        webViewClient = object : WebViewClient() {

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // TODO 这里需要注入脚本，对 fetch、XMLHttpRequest 这些网络请求进行拦截
                // loadUrl("javascript:hookFetch()");
                readyPo.resolve(true)
            }

            override fun shouldInterceptRequest(
                view: WebView, request: WebResourceRequest
            ): WebResourceResponse? {
                if (request.method == "GET" && request.url.host?.endsWith(".dweb") == true && request.url.scheme == "http") {
                    /// http://*.dweb 由 MicroModule 来处理请求
                    val response = runBlocking {
                        mm.nativeFetch(
                            Request(Method.GET, request.url.toString())
                                .headers(request.requestHeaders.toList())
                        )
                    }
                    val headersMap = response.headers.toMap().toMutableMap()
                    return WebResourceResponse(
                        null,
                        null,
                        response.status.code,
                        response.status.description,
                        headersMap,
                        response.body.stream
                    )
                }
                return super.shouldInterceptRequest(view, request)
            }
        }

        settings.setSupportMultipleWindows(true)
        webChromeClient = object : WebChromeClient() {
            override fun onCreateWindow(
                view: WebView,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message
            ): Boolean {
                println("open $isDialog $isUserGesture ${resultMsg?.data} ${resultMsg?.obj}")
                GlobalScope.launch {
                    openSignal.emit(resultMsg)
                }
                return true
            }

            override fun onCloseWindow(window: WebView?) {
                println("close")
                GlobalScope.launch {
                    closeSignal.emit()
                }
                super.onCloseWindow(window)
            }
        }
        if (options.loadUrl.isNotEmpty()) {
            /// 开始加载
            loadUrl(options.loadUrl)
        }

    }

    /**
     * 执行同步JS代码
     */
    suspend fun evaluateSyncJavascriptCode(script: String) =
        evaluator.evaluateSyncJavascriptCode(script)

    /**
     * 执行异步JS代码，需要传入一个表达式
     */
    suspend fun evaluateAsyncJavascriptCode(script: String, afterEval: suspend () -> Unit = {}) =
        evaluator.evaluateAsyncJavascriptCode(script, afterEval)
}