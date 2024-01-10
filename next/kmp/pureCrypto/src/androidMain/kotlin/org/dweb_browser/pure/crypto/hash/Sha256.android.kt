package org.dweb_browser.pure.crypto.hash

actual suspend fun sha256(data: ByteArray) = sha256Common(data)