package org.dweb_browser.helper

public actual fun randomUUID(): String = java.util.UUID.randomUUID().toString()
