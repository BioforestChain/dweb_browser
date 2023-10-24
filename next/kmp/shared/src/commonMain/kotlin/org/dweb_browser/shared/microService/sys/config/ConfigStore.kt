package org.dweb_browser.shared.microService.sys.config

expect object ConfigStore {

    val Config: String

    fun get(key: String): String

    fun set(key: String, data: String)
}