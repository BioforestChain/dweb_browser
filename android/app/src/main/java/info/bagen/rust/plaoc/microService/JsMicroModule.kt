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

class JsMicroModule : MicroModule() {
    // 该程序的来源
    override var mmid = "js.sys.dweb"
    private val routers: Router = mutableMapOf()

    // 我们隐匿地启动单例webview视图，用它来动态创建 WebWorker，来实现 JavascriptContext 的功能
    private val javascriptContext = JavascriptContext()
    //private val _connecttingIpcs = mutableSetOf<Ipc>()

    init {
        // 创建一个webWorker
        routers["create-process"] = put@{ args ->
            return@put createProcess(args as workerOption)
        }
    }

    override fun bootstrap(args: workerOption) {
        println("kotlin#JsMicroModule args==> ${args.mainCode}  ${args.origin}")
        // 开始执行开发者自己的代码
        this.createProcess(args)
    }

    override fun ipc(): Ipc {
        TODO("Not yet implemented")
    }

    // 创建一个webWorker
    private fun createProcess(args: workerOption) {
        if (args.mainCode == "") return
        javascriptContext.hiJackWorkerCode(args.mainCode)
//        xx.postMessage(port2,[port2])
    }

}

class JavascriptContext {
    val ALL_PROCESS_MAP = mutableMapOf<Number, String>()
    var accProcessId = 0


    // 创建了一个后台运行的webView 用来运行webWorker
    var view: WebView = WebView(App.appContext)


    // 为这个上下文安装启动代码
    @OptIn(DelicateCoroutinesApi::class)
    fun hiJackWorkerCode(mainUrl: String) {
        GlobalScope.launch {
            val workerHandle = "worker${Date().time}"
            println("kotlin#JsMicroModule workerHandle==> $workerHandle")
            val injectJs = getInjectWorkerCode("injectWorkerJs/injectWorker.js")
            val userCode = ApiService.instance.getNetWorker(mainUrl).replace("\"use strict\";", "")
            // 构建注入的代码
            val workerCode =
                "data:utf-8,((module,exports=module.exports)=>{$injectJs;return module.exports})({exports:{}}).installEnv();$userCode"
            withContext(Dispatchers.Main) {
                injectJs(workerCode, workerHandle)
            }
        }
    }

    //    注入webView
    private fun injectJs(workerCode: String, workerHandle: String) {
        val channel = view.createWebMessageChannel()
        channel[0].setWebMessageCallback(object :
            WebMessagePort.WebMessageCallback() {
            override fun onMessage(port: WebMessagePort, message: WebMessage) {
                println("kotlin#JsMicroModuleport🍟message: ${message.data}")
            }
        })
        view.evaluateJavascript("const $workerHandle = new Worker(`$workerCode`); \n" ) {
            println("worker创建完成")
        }
        view.evaluateJavascript("$workerHandle.postMessage([\"ipc-channel\", ${channel[1]}], [${channel[1]}])\n"){
            println("worker监听注册完成")
        }
        view.postWebMessage(WebMessage("fetch-ipc-channel", arrayOf(channel[1])), Uri.EMPTY)

        //  println("kotlin:JsMicroModule injectJs accProcessId==> $accProcessId, $it")
        this.ALL_PROCESS_MAP[accProcessId] = workerHandle
        this.accProcessId++

    }

    private fun getInjectWorkerCode(jsAssets: String): String {
        val inputStream = App.appContext.assets.open(jsAssets)
        val byteArray = inputStream.readBytes()
        return String(byteArray)
    }
}

