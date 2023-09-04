package org.dweb_browser.helper

actual fun randomUUID(): String = java.util.UUID.randomUUID().toString()
