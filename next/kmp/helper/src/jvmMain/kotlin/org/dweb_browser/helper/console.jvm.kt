package org.dweb_browser.helper

public actual fun eprintln(message: String): Unit = System.err.println(message)
