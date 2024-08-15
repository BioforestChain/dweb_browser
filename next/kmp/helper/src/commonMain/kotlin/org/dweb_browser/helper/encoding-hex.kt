package org.dweb_browser.helper

@OptIn(ExperimentalStdlibApi::class)
public val String.hexBinary: ByteArray get() = hexToByteArray()

@OptIn(ExperimentalStdlibApi::class)
public val ByteArray.hexString: String get() = toHexString()
