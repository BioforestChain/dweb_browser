package  viewModel

import org.dweb_browser.js_frontend.browser_window.BaseElectronWindowModel
import org.dweb_browser.js_frontend.view_model.ViewModelState

class ViewModel(state: ViewModelState = ViewModelState()) :
    BaseElectronWindowModel("js.backend.dweb", state)