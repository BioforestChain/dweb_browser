package info.bagen.rust.plaoc.microService.core

import info.bagen.rust.plaoc.microService.helper.Mmid
import info.bagen.rust.plaoc.microService.helper.int
import info.bagen.rust.plaoc.microService.helper.rand
import info.bagen.rust.plaoc.microService.helper.stream
import info.bagen.rust.plaoc.microService.ipc.IPC_ROLE
import info.bagen.rust.plaoc.microService.ipc.Ipc
import info.bagen.rust.plaoc.microService.ipc.ipcWeb.Native2JsIpc
import info.bagen.rust.plaoc.microService.ipc.ipcWeb.ReadableStreamIpc
import info.bagen.rust.plaoc.microService.sys.dns.nativeFetch
import org.http4k.core.*

data class JmmMetadata(val main_url: String)

open class JsMicroModule(override val mmid: Mmid, val metadata: JmmMetadata) : MicroModule() {

    /**
     * 和 dweb 的 port 一样，pid 是我们自己定义的，它跟我们的 mmid 关联在一起
     * 所以不会和其它程序所使用的 pid 冲突
     */
    private var processId: Int? = null
    override suspend fun _bootstrap() {
        println("启动成功了 $mmid/$metadata")
        val pid = rand(1, 1000)
        processId = pid
        val streamIpc = ReadableStreamIpc(this, IPC_ROLE.CLIENT)
        streamIpc.onRequest { (request) ->
            when (request.uri.path) {
                "/index.js" -> nativeFetch(metadata.main_url)
                else -> Response(Status.NOT_FOUND)
            }
        }
        streamIpc.bindIncomeStream(
            nativeFetch(
                Request(
                    Method.POST,
                    Uri.of("file://js.sys.dweb/create-process")
                        .query("main_pathname", "/index.js")
                        .query("process_id", pid.toString())
                ).body(streamIpc.stream)
            ).stream()
        )

        _connectingIpcSet.add(streamIpc);
    }


    override suspend fun _connect(from: MicroModule): Ipc {
        val pid = processId ?: throw Exception("$mmid process_id no found, should bootstrap first")

        val portId = nativeFetch(
            "file://js.sys.dweb/create-ipc?process_id=$pid"
        ).int();
        val outerIpc = Native2JsIpc(portId, this);
        _connectingIpcSet.add(outerIpc)
        return outerIpc
    }

    private val _connectingIpcSet = mutableSetOf<Ipc>()


    override suspend fun _shutdown() {
        for (outerIpc in _connectingIpcSet) {
            outerIpc.close()
        }
        _connectingIpcSet.clear()

        /// TODO 发送指令，关停js进程
        processId = null
    }
}
//
//class JsProcess : NativeMicroModule("js.sys.dweb") {
//    // 存储每个worker的port 以此来建立每个worker的通信
//    private val ALL_PROCESS_MAP = mutableMapOf<Number, WebMessagePort>()
//    private var accProcessId = 0
//
//    // 创建了一个后台运行的webView 用来运行webWorker
//    private var webView: WebView? = null
//
//    override suspend fun _bootstrap() {
//        webView = WebView(App.appContext).also { view ->
//            WebView.setWebContentsDebuggingEnabled(true)
//            val settings = view.settings
//            settings.javaScriptEnabled = true
//            settings.domStorageEnabled = true
//            settings.useWideViewPort = true
//            settings.loadWithOverviewMode = true
//            settings.databaseEnabled = true
//        }
//    }
//
//
////    /** 处理ipc 请求的工厂 然后会转发到nativeFetch */
////    fun ipcFactory(webMessagePort: WebMessagePort, ipcString: String) {
////        mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true) //允许出现特殊字符和转义符
////        mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true) //允许使用单引号
////        val ipcRequest = mapper.readValue(ipcString, IpcRequest::class.java)
////        println("JavascriptContext#ipcFactory url: ${ipcRequest.url}")
////        // 处理请求
////        val response = nativeFetch(ipcRequest.url)
////        println("JavascriptContext#ipcFactory body: ${response.bodyString()}")
////        tranResponseWorker(
////            webMessagePort,
////            IpcResponse.fromResponse(
////                ipcRequest.req_id,
////                response,
////                ipc,
////            )
////        )
////    }
//
//    /** 这里负责返回每个webWorker里的返回值
//     * 注意每个worker的post都是不同的 */
//    private fun tranResponseWorker(webMessagePort: WebMessagePort, res: IpcResponse) {
//        val jsonMessage = gson.toJson(res)
//        println("JavascriptContext#tranResponseWorker: $jsonMessage")
//        webMessagePort.postMessage(WebMessage(jsonMessage))
//    }
//
//
//    /** 为这个上下文安装启动代码 */
//    @OptIn(DelicateCoroutinesApi::class)
//    fun hiJackWorkerCode(mainUrl: String): String {
//        val workerPort = this.accProcessId
//        GlobalScope.launch {
//            val workerHandle = "worker${Date().time}"
//            println("kotlin#JsMicroModule workerHandle==> $workerHandle")
//            val injectJs = getInjectWorkerCode("injectWorkerJs/injectWorker.js")
//            val userCode = ApiService.instance.getNetWorker(mainUrl).replace("\"use strict\";", "")
//            // 构建注入的代码
//            val workerCode = "data:utf-8," +
//                    "((module,exports=module.exports)=>{$injectJs;return module.exports})({exports:{}}).installEnv();$userCode"
//
//            withContext(Dispatchers.Main) {
//                injectJs(workerCode, workerHandle)
//            }
//        }
//        return workerPort.toString()
//    }
//
//    //    注入webView
//    private fun injectJs(workerCode: String, workerHandle: String) {
//        val view = webView ?: return;
//        // 为每一个webWorker都创建一个通道
//        val channel = view.createWebMessageChannel()
//        channel[0].setWebMessageCallback(object :
//            WebMessagePort.WebMessageCallback() {
//            override fun onMessage(port: WebMessagePort, message: WebMessage) {
//                Log.i("JsProcess", "kotlin#JsMicroModuleport🍟message: ${message.data}")
////                ipcFactory(channel[0], message.data)
//            }
//        })
//        view.evaluateJavascript(
//            "const $workerHandle = new Worker(`$workerCode`); \n" +
//                    "onmessage = function (e) {\n" +
//                    "$workerHandle.postMessage([\"ipc-channel\", e.ports[0]], [e.ports[0]])\n" +
//                    "}\n"
//        ) {
//            println("worker创建完成")
//        }
//        // 发送post1到worker层
//        view.postWebMessage(WebMessage("fetch-ipc-channel", arrayOf(channel[1])), Uri.EMPTY)
//
//        this.ALL_PROCESS_MAP[accProcessId] = channel[0]
//        this.accProcessId++
//    }
//
//
//    override suspend fun _shutdown() {
//        webView?.destroy()
//        webView = null
//    }
//}
//
///**读取本地资源文件，并把内容转换为String */
//fun getInjectWorkerCode(jsAssets: String): String {
//    val inputStream = App.appContext.assets.open(jsAssets)
//    val byteArray = inputStream.readBytes()
//    return String(byteArray)
//}
