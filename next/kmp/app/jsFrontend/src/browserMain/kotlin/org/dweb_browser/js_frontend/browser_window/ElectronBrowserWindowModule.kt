package org.dweb_browser.js_frontend.browser_window
import org.dweb_browser.js_frontend.view_model.ViewModelDataState

class ElectronBrowserWindowModule(
    val VMId: String,
) {
    val viewModelDataState =  ViewModelDataState(
        VMId = VMId,
    )
    val controller: ElectronBrowserWindowController = ElectronBrowserWindowController()
}
