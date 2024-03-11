package org.dweb_browser.pure.crypto.hash

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private val shaLock = Mutex()

actual suspend fun sha256(data: ByteArray) = shaLock.withLock { common_sha256(data) }
actual suspend fun sha256(data: String) = shaLock.withLock { common_sha256(data) }