package org.dweb_browser.shared.microService.sys.config

import platform.Foundation.NSUserDefaults
actual object ConfigStore {

    actual fun get(key: String): String {
        return (NSUserDefaults.standardUserDefaults.objectForKey(key) as? String).toString()
    }

    actual fun set(key: String, data: String) {
        NSUserDefaults.standardUserDefaults.setObject(data,key)
        NSUserDefaults.standardUserDefaults.synchronize()
    }

    actual val Config: String
        get() = "const"
}