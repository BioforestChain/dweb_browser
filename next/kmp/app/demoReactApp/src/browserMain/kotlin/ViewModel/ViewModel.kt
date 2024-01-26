package  viewModel

import org.dweb_browser.js_frontend.browser_window.BaseElectronWindowModel
import org.dweb_browser.js_frontend.view_model_state.ViewModelState

class ViewModel(override var state: ViewModelState = ViewModelState()) :
    BaseElectronWindowModel("js.backend.dweb") {
    init {
        console.log("ViewModel init")
        init()
    }
}