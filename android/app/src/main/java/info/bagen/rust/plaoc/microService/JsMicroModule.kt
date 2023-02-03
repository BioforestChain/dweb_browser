package info.bagen.rust.plaoc.microService


import android.content.MutableContextWrapper
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.*
import android.webkit.WebMessagePort.WebMessageCallback
import info.bagen.libappmgr.network.ApiService
import info.bagen.rust.plaoc.App
import io.ktor.http.*
import kotlinx.coroutines.*
import java.util.*


typealias workerOption = NativeOptions
// 我们隐匿地启动单例webview视图，用它来动态创建 WebWorker，来实现 JavascriptContext 的功能
var ctx: WebView = JavascriptContext().create()
class JsMicroModule : MicroModule() {
    // 该程序的来源
    override var mmid = "js.sys.dweb"
    private val routers: Router = mutableMapOf()

    //    private val _connecttingIpcs = mutableSetOf<Ipc>()
    private val javascriptContext = JavascriptContext()



    init {
        // 创建一个webWorker
        routers["create-process"] = put@{ args ->
            return@put createProcess(args as workerOption)
        }
        createMessageChannel()
    }

    override fun bootstrap(args: workerOption) {
        println("kotlin#JsMicroModule args==> ${args.mainCode}  ${args.origin}")
        // 注入messageChannel
        ctx.evaluateJavascript("") { }
        // 开始执行开发者自己的代码
        this.createProcess(args)
    }

    override fun ipc(): Ipc {
        TODO("Not yet implemented")
    }

    // 创建一个webWorker
    private fun createProcess(args: workerOption) {
        if (args.mainCode == "") return
        ctx.loadUrl("https://objectjson.waterbang.top")
        javascriptContext.hiJackWorkerCode(ctx, args.mainCode)
//        xx.postMessage(port2,[port2])
    }


    private fun createMessageChannel() {
        val channel = ctx.createWebMessageChannel()
        channel[0].setWebMessageCallback(   object : WebMessageCallback() {
            override fun onMessage(port: WebMessagePort, message: WebMessage) {
                println("kotlin#JsMicroModule port🍟==> $port ,message: ${message.data}")
            }
        })
//        channel[1].postMessage(WebMessage("My secure message"))
        ctx.postWebMessage(WebMessage("fetch-ipc-channel", arrayOf(channel[1])),Uri.EMPTY)
    }

}

class JavascriptContext {
    val ALL_PROCESS_MAP = mutableMapOf<Number, String>()
    var accProcessId = 0

    // 创建了一个后台运行的webView 用来运行webWorker
    fun create(): WebView {
        return WebView(MutableContextWrapper(App.appContext.applicationContext)).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.databaseEnabled = true
            webChromeClient = object : WebChromeClient(){
                override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {

                }

                override fun onReceivedTitle(view: WebView?, title: String?) {

                }
            }

            webViewClient = object : WebViewClient(){

            }
            loadUrl("https://objectjson.waterbang.top")
        }

    }

    // 为这个上下文安装启动代码
    @OptIn(DelicateCoroutinesApi::class)
    fun hiJackWorkerCode(ctx: WebView, mainUrl: String) {
        GlobalScope.launch {
            val workerHandle = "worker${Date().time}"
            println("kotlin#JsMicroModule workerHandle==> $workerHandle")
            val injectJs = getInjectWorkerCode("injectWorkerJs/injectWorker.js")
            val userCode = ApiService.instance.getNetWorker(mainUrl).replace("\"use strict\";", "")
            // 构建注入的代码
            val workerCode =
                "data:utf-8,((module,exports=module.exports)=>{$injectJs;return module.exports})({exports:{}}).installEnv();$userCode"
            withContext(Dispatchers.Main) {
                injectJs(ctx, workerCode, workerHandle)
            }
        }
    }

    //    注入webView
    private fun injectJs(ctx: WebView, workerCode: String, workerHandle: String) {
        val jsCode =
            "const $workerHandle = new Worker(`$workerCode`);\n" +
                    "let _BFS_port; \n" +
                    "onmessage = function (e) {\n" +
                    "    _BFS_port = e.ports[0];\n" +
                    "    _BFS_port.onmessage = function (event) {\n" +
                    "    console.log(\"backWebView#onmessage\",event.data)\n" +
                    "    $workerHandle.postMessage(event.data)\n" +
                    "    }\n" +
                    "}\n" +
                    "$workerHandle.onmessage = function (event) {\n" +
                    "  console.log('backWebView:onmessage', event.data);\n" +
                    " _BFS_port.postMessage(event.data);\n" +
                    "}"
//        println("kotlin:JsMicroModule injectJs==> $jsCode")
        // 转发功能
        ctx.evaluateJavascript(jsCode) {
            this.ALL_PROCESS_MAP[accProcessId] = workerHandle
            this.accProcessId++
        }
    }

    private fun getInjectWorkerCode(jsAssets: String): String {
        val inputStream = App.appContext.assets.open(jsAssets)
        val byteArray = inputStream.readBytes()
        return String(byteArray)
    }
}

