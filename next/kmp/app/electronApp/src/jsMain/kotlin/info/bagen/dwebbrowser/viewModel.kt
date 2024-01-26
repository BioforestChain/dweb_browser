package info.bagen.dwebbrowser

import electron.BrowserWindowConstructorOptions
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import node.process.Process
import node.path.PlatformPath
import org.dweb_browser.js_backend.browser_window.BaseBrowserWindowModel
import org.dweb_browser.js_backend.browser_window.ElectronBrowserWindowOptions
import org.dweb_browser.js_backend.view_model.BaseViewModel
import web.cssom.atrule.width

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
class BrowserDemoReactAppViewModel: BaseBrowserWindowModel("js.backend.dweb") {
    // 测试数据
    override val state = mutableMapOf<dynamic, dynamic>("currentCount" to 10)
    override val electronBrowserWindowOptions: BrowserWindowConstructorOptions = ElectronBrowserWindowOptions.create()
    override val electronLoadUrlPath: String = "/demoReactApp/index.html"
    override val electronIsOpenDevtools: Boolean = true

    init {
        // 添加一个状态监听器
        onStateChangeByBrowser {
            console.log("接受到了UI发送过来的消息")
            // TODO: 需要删除 - 测试服务器端向客户端同步数据
            scope.launch {
                // 测试项 客户端同步 viewModel
                syncDataToUI(it[0], it[1] + 1)
            }
        }
    }
}

class BrowserDemoComposeAppViewModel: BaseBrowserWindowModel("js.backend.dweb") {
    // 测试数据
    override val state = mutableMapOf<dynamic, dynamic>("currentCount" to 1)
    override val electronBrowserWindowOptions: BrowserWindowConstructorOptions = ElectronBrowserWindowOptions.create()
    override val electronLoadUrlPath: String = "/demoComposeApp/index.html"
    override val electronIsOpenDevtools: Boolean = true

    init {
        // 添加一个状态监听器
        onStateChangeByBrowser {
            console.log("接受到了UI发送过来的消息", it[0])
            // TODO: 需要删除 - 测试服务器端向客户端同步数据
            scope.launch {
                // 测试项 客户端同步 viewModel
                syncDataToUI(it[0], it[1] + 1)
            }
        }
    }
}



