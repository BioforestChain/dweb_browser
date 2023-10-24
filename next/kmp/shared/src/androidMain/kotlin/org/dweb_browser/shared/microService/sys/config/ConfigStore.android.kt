package org.dweb_browser.shared.microService.sys.config

import android.content.Context
import androidx.core.app.ComponentActivity
import org.dweb_browser.browser.web.util.getString
import org.dweb_browser.browser.web.util.saveString
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.module.getAppContext

actual object ConfigStore {

    actual fun get(key: String): String {
        return NativeMicroModule.getAppContext().getString(key,"")
    }

    actual fun set(key: String, data: String) {
        NativeMicroModule.getAppContext().saveString(key,data)
    }

    actual val Config: String
        get() = "const"

}
