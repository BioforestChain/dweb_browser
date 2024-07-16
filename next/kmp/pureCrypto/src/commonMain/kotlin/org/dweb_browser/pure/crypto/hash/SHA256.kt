package org.dweb_browser.pure.crypto.hash

expect suspend fun sha256(data: ByteArray): ByteArray
expect suspend fun sha256(data: String): ByteArray
expect fun sha256Sync(data: ByteArray): ByteArray
