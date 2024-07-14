package org.dweb_browser.helper

@OptIn(ExperimentalStdlibApi::class)
val String.hexBinary get() = hexToByteArray()

@OptIn(ExperimentalStdlibApi::class)
val ByteArray.hexString get() = toHexString()
