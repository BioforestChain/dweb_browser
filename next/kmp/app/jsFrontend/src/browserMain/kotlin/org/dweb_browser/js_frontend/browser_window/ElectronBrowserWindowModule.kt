package org.dweb_browser.js_frontend.browser_window

import org.dweb_browser.js_frontend.view_model.BaseViewModel
import org.dweb_browser.js_frontend.view_model_state.ViewModelState

interface IElectronBrowserWindowModule{
    val moduleId: String
    val controller: ElectronBrowserWindowController
    val viewModel: BaseViewModel
}


class ElectronBrowserWindowModule(
    override val moduleId: String, /* example: demo.compose.app */
): IElectronBrowserWindowModule{
    override val controller: ElectronBrowserWindowController = ElectronBrowserWindowController(moduleId)
    override val viewModel: BaseViewModel =  BaseViewModel(moduleId, ViewModelState(mutableMapOf<dynamic,dynamic>()))
}
