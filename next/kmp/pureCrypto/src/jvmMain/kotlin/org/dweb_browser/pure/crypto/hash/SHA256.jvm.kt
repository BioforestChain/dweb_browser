package org.dweb_browser.pure.crypto.hash

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.security.MessageDigest

private val shaLock = Mutex()

public actual suspend fun sha256(data: ByteArray): ByteArray = shaLock.withLock { common_sha256(data) }
public actual suspend fun sha256(data: String): ByteArray = shaLock.withLock { common_sha256(data) }
public actual fun sha256Sync(data: ByteArray): ByteArray = jvmSha256(data)

public fun jvmSha256(data: ByteArray): ByteArray {
  val sha256Digest = MessageDigest.getInstance("SHA-256")
  sha256Digest.update(data)
  return sha256Digest.digest()
}
