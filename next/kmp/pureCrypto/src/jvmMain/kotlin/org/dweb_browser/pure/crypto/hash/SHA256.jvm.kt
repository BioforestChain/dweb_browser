package org.dweb_browser.pure.crypto.hash

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.security.MessageDigest

private val shaLock = Mutex()

actual suspend fun sha256(data: ByteArray) = shaLock.withLock { common_sha256(data) }
actual suspend fun sha256(data: String) = shaLock.withLock { common_sha256(data) }

fun jvmSha256(data: ByteArray): ByteArray {
  val sha256Digest = MessageDigest.getInstance("SHA-256")
  sha256Digest.update(data)
  return sha256Digest.digest()
}
