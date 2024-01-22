package info.bagen.dwebbrowser

import kotlinx.coroutines.launch
import node.process.Process
import node.path.PlatformPath
import org.dweb_browser.js_backend.view_model.BaseViewModel

@JsModule("node:path")
@JsNonModule()
external val path: PlatformPath

@JsModule("node:process")
@JsNonModule()
external val process: Process


/**
 * example
 */
class BrowserViewModel(frontendViewModelId: String) : BaseViewModel(frontendViewModelId) {
    // 测试数据
    override val state = mutableMapOf<dynamic, dynamic>("currentCount" to 1)
    init {
        // 添加一个状态监听器
        onStateChangeByBrowser {
            // TODO: 需要删除 - 测试服务器端向客户端同步数据
            scope.launch {
                // 测试项 客户端同步 viewModel
                syncDataToUI(it[0], it[1] + 1)
                console.log("重这里发出的消息", it[0], it[1])
            }
        }
    }
}



