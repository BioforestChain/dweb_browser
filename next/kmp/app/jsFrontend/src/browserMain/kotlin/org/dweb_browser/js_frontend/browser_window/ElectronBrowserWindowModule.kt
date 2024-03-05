package org.dweb_browser.js_frontend.browser_window
import org.dweb_browser.js_common.view_model.DataState
import org.dweb_browser.js_frontend.browser_window.ElectronBrowserWindowController
import org.dweb_browser.js_frontend.view_model.OnSyncFromServer
import org.dweb_browser.js_frontend.view_model.ViewModelDataState

class ElectronBrowserWindowModule(
    val VMId: String,
    dataState: DataState,
    onSyncFromServer: OnSyncFromServer
) {
    val viewModelDataState =  ViewModelDataState(
        dataState = dataState,
        VMId = VMId,
        onSyncFromServer = onSyncFromServer
    )
    val controller: ElectronBrowserWindowController = ElectronBrowserWindowController()
}
