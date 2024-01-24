package  viewModel

import org.dweb_browser.js_frontend.browser_window.BaseElectronWindowModel
class ViewModel(override var state: MutableMap<dynamic, dynamic>) :
    BaseElectronWindowModel("js.backend.dweb")