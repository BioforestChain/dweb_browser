package org.dweb_browser.pure.crypto.hash

actual suspend fun sha256(data: ByteArray) = common_sha256(data)
actual suspend fun sha256(data: String) = common_sha256(data)