package info.bagen.dwebbrowser

import node.process.Process
import node.path.PlatformPath

import org.dweb_browser.js_backend.view_model.BaseViewModel

@JsModule("node:path")
@JsNonModule()
external val path: PlatformPath

@JsModule("node:process")
@JsNonModule()
external val process: Process



class Options(
    @JsName("width")
    val width: Double = 1000.0,
    @JsName("height")
    val height: Double = 1000.0

){}




/**
 * example
 */
//class BrowserDemoReactAppViewModel: BaseBrowserWindowModel(
//    "demo.react.app",
//    viewModelMutableMapOf("currentCount" to 10) // 测试数据
//) {
//    override val electronBrowserWindowOptions: BrowserWindowConstructorOptions = ElectronBrowserWindowOptions.create()
//    override val electronLoadUrlPath: String = "/demoReactApp/index.html"
//    override val electronIsOpenDevtools: Boolean = true
//
//    init {
//        // 添加一个状态监听器
//        onUpdateByClient{key: dynamic, value: dynamic ->
//            this[key] = value + 1
//        }
//    }
//}

class BrowserDemoComposeAppViewModel: BaseViewModel(
    frontendViewModelId = "demo.compose.app",
    initVieModelMutableMap =  mutableMapOf<dynamic, dynamic>("currentCount" to 10)
) {
//    // 测试数据
//    override val electronBrowserWindowOptions: BrowserWindowConstructorOptions = ElectronBrowserWindowOptions.create()
//    // TODO: 这里pat还有问题
//    override val electronLoadUrlPath: String = "/demoComposeApp/index.html"
//    override val electronIsOpenDevtools: Boolean = true

    init {
//        // 添加一个状态监听器
//        onUpdateByClient{key: dynamic, value: dynamic ->
//            console.log("接受到了UI发送过来的消息", key,value)
//            this[key] = value + 1
//        }
    }
}



