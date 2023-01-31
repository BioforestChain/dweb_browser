package info.bagen.rust.plaoc.microService


class JsMicroModule : NativeMicroModule() {
    // 该程序的来源
    var origin = "https://objectjson.waterbang.top/";
    override var mmid = "sys.dweb"
    private val boostrapCode = "";

    override fun bootstrap(args: WindowOptions) {
/// 我们隐匿地启动单例webview视图，用它来动态创建 WebWorker，来实现 JavascriptContext 的功能
//        const ctx = JavascriptContext.create(args.processId);
//        ctx.evalJavascript();// 为这个上下文安装启动代码
//        ctx.evalJavascript(args.main_js);// 开始执行开发者自己的代码
    }

    override fun ipc(): Ipc {
        TODO("Not yet implemented")
    }
}

class JavascriptContext {

    fun create() {

    }
}

