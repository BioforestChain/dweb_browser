package org.dweb_browser.helper

val ByteArray.utf8String get() = decodeToString()
val String.utf8Binary get() = encodeToByteArray()