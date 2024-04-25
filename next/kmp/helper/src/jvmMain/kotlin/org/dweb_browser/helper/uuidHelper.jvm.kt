package org.dweb_browser.helper

actual fun randomUUID() = java.util.UUID.randomUUID().toString()
