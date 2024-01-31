package org.dweb_browser.js_frontend.browser_window

import org.dweb_browser.js_frontend.view_model.BaseViewModel
import org.dweb_browser.js_frontend.view_model_state.ViewModelState

interface IElectronBrowserWindowModule {
    val moduleId: String
    val controller: ElectronBrowserWindowController
    val viewModel: BaseViewModel
}

class ElectronBrowserWindowModule(
    override val moduleId: String,
    /* example: demo.compose.app */
    // 编码value的方法
    val valueEncodeToString: (key: dynamic, value: dynamic) -> String,
    // 解码value的方法
    val valueDecodeFromString: (key: dynamic, value: String) -> dynamic,
) : IElectronBrowserWindowModule {
    override val controller: ElectronBrowserWindowController = ElectronBrowserWindowController()
    override val viewModel: BaseViewModel = BaseViewModel(
        ViewModelState(mutableMapOf<dynamic, dynamic>()),
        valueEncodeToString = valueEncodeToString,
        valueDecodeFromString = valueDecodeFromString
    )
}
