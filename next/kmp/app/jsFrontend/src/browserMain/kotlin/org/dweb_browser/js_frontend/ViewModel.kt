package  org.dweb_browser.js_frontend

import kotlinx.browser.window
import org.dweb_browser.js_frontend.view_model.BaseViewModel
class ViewModel(override var state: MutableMap<dynamic, dynamic>) :
    BaseViewModel("js.backend.dweb", "ws://${window.location.host}")