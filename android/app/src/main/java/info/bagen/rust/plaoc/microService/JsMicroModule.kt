package info.bagen.rust.plaoc.microService
import android.webkit.WebView
import info.bagen.rust.plaoc.App


typealias workerOption = NativeOptions

class JsMicroModule : MicroModule() {
    // 该程序的来源
    override var mmid = "js.sys.dweb"
    private val javascriptContext = JavascriptContext()

    override fun bootstrap(args: workerOption) {
/// 我们隐匿地启动单例webview视图，用它来动态创建 WebWorker，来实现 JavascriptContext 的功能
        val ctx = javascriptContext.create()
        ctx.evaluateJavascript(args.origin){ } // 为这个上下文安装启动代码
        ctx.evaluateJavascript(args.mainJs){ } // 开始执行开发者自己的代码
    }

    override fun ipc(): Ipc {
        TODO("Not yet implemented")
    }
}

class JavascriptContext {
    fun create(): WebView {
        return WebView(App.appContext)
    }
}

