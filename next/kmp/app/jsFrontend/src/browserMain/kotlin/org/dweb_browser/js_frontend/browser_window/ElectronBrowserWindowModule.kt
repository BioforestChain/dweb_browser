package org.dweb_browser.js_frontend.browser_window

import org.dweb_browser.js_frontend.view_model.ViewModel
import org.dweb_browser.js_common.view_model.DecodeValueFromString
import org.dweb_browser.js_common.view_model.EncodeValueToString
import org.dweb_browser.js_frontend.view_model.ViewModelState

interface IElectronBrowserWindowModule {
    val moduleId: String
    val controller: ElectronBrowserWindowController
    val viewModel: ViewModel
}

class ElectronBrowserWindowModule(
    override val moduleId: String,/** example: demo.compose.app */
    encodeValueToString: EncodeValueToString,
    decodeValueFromString: DecodeValueFromString,
) : IElectronBrowserWindowModule {
    override val controller: ElectronBrowserWindowController = ElectronBrowserWindowController()
    override val viewModel: ViewModel = ViewModel(
        ViewModelState(mutableMapOf()),
        encodeValueToString = encodeValueToString,
        decodeValueFromString = decodeValueFromString
    )
}
