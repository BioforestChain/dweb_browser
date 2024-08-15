package org.dweb_browser.helper

public val ByteArray.utf8String: String get() = decodeToString()
public val String.utf8Binary: ByteArray get() = encodeToByteArray()